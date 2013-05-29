package org.wdssii.gui.products.renderers;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.AttributeTable.AttributeColumn;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.symbology.SymbolFactory;
import org.wdssii.gui.renderers.SymbolRenderer;
import org.wdssii.gui.renderers.SymbolRenderer.SymbolRectangle;
import org.wdssii.storage.Interval;
import org.wdssii.storage.IntervalTree;
import org.wdssii.xml.iconSetConfig.Category;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Renders points in lat/lon using our symbology and attribute table
 *
 * For now this will do: SingleSymbol Category:UniqueValues Might break up more
 * later
 *
 * FIXME: Need to clean this up...all experimental code right now so it's a mess
 *
 * @author Robert Toomey
 */
public class PointRenderer {

    private final static Logger LOG = LoggerFactory.getLogger(PointRenderer.class);
    private List<SymbolRenderer> myRenderers = null;
    private SymbolRenderer myFailSafe = null;
    private SymbolRenderer mySingle = null;
    private AttributeColumn myColumn = null; // Column if category, null if single
    private final Object myRefreshLock = new Object();
    private boolean myRefresh = false;
    private Symbology mySymbology = null;
    private TreeMap<String, SymbolRenderer> myCategoryLookup;
    private TreeMap<String, List<CatLookup>> myGroups;
    /**
     * Toggle from single for all to categories...
     */
    private boolean tryCategories = false;

    public void refreshRenderers() {
        synchronized (myRefreshLock) {
            myRefresh = true;
        }
    }

    public static class CatLookup {
        int row;
        Vec4 the3D;
        Vec4 the2D;
    }
    
    public static class Node {

        // FIXME: Use a row 'list' for all merged objects..
        // ArrayList<Integer> theRows;
        public Node(Vec4 p, int row, int count) {
            thePoint = p;
            theRow = row;
            myCount = count;
        }

        public Node(Vec4 p, int row) {
            thePoint = p;
            theRow = row;
        }
        public Vec4 thePoint;
        public int theRow;
        /**
         * How many merged nodes make this rectangle?
         */
        public int myCount = 1;
        public SymbolRectangle myCoverage;
    }

    /**
     * Given DrawContext and collection of points and symbology, draw these
     * points
     */
    public void draw(DrawContext dc, Symbology s, List<Vec4> points, AttributeColumn theColumn) {

        myColumn = theColumn;
        // This is set with a copy from GUI on change, so sync shouldn't be an issue...
        boolean changed = (s != mySymbology);

        // Create a fall back default if symbology fails...
        if (myFailSafe == null) {
            Symbol p;
            PolygonSymbol ps = new PolygonSymbol();
            ps.pointsize = 12;
            ps.toSquare();
            p = ps;
            SymbolRenderer renderer = SymbolFactory.getSymbolRenderer(p);
            renderer.setSymbol(p);
            myFailSafe = renderer;
            mySingle = myFailSafe;
        }

        if (changed) {

            mySymbology = s;

            if (s != null) {
                // Read single from Symbology
                List<Symbol> aList = mySymbology.getSingle().symbols;
                if ((aList != null) && (!aList.isEmpty())) {
                    Symbol sym = aList.get(0);
                    SymbolRenderer renderer = SymbolFactory.getSymbolRenderer(sym);
                    renderer.setSymbol(sym);
                    mySingle = renderer;
                }
            } else {
                mySingle = myFailSafe;
            }

            // Create a lookup table from key to renderer
            if (s.use == Symbology.CATEGORY_UNIQUE_VALUES) {
                if (myCategoryLookup == null) {
                    myCategoryLookup = new TreeMap<String, SymbolRenderer>();
                }
                // Creating every single time....bleh....
                // Should create a map lookup String -> renderer when
                // symbology changes.....
                List<Category> cats = s.getCategories().getCategoryList();
                String key;
                for (Category c : cats) {
                    key = c.value;
                    SymbolRenderer r = null;
                    Symbol aSymbol = c.symbols.get(0);  // bleh
                    if (aSymbol != null) {
                        r = SymbolFactory.getSymbolRenderer(aSymbol);
                        r.setSymbol(aSymbol);
                    }
                    myCategoryLookup.put(key, r);
                }
            }
        }

        // Create render groups...we will group by common symbol renderer
        int row = 0;
        myGroups = new TreeMap<String, List<CatLookup>>();
        for (Vec4 at3D : points) {
            String catKey = "";
            catKey = getCategory(row, mySymbology);

            // Add this cat key to the collection...
            List<CatLookup> list = myGroups.get(catKey);
            if (list == null) {
                list = new ArrayList<CatLookup>();
                myGroups.put(catKey, list);
            }
            CatLookup c = new CatLookup();
            c.the3D = at3D;
            c.row = row;
            list.add(c);
            
            row++;
        }

// Pre non-opengl first...
        TextRenderer aText = null;
        Font font = new Font("Arial", Font.PLAIN, 14);
        if (aText == null) {
            aText = new TextRenderer(font, true, true);
        }
        // Draw using the symbols...
        GL gl = dc.getGL();
        View v = dc.getView();
        GLUtil.pushOrtho2D(dc);
        gl.glDisable(GL.GL_DEPTH_TEST);
        int MAX = 1000;

        // Render the normal uncompressed squares ---------------------------
        // Render pass....
        row = 0;
        if (false) {
            SymbolRenderer.COLOR1 = false;
            for (Vec4 at3D : points) {
                // Project 3D world coordinates to 2D view (whenever eye changes)
                // Could maybe cache these...check for view changing somehow...
                // FIXME: Possible speed up here
                Vec4 at2D = v.project(at3D);

                // LOG.debug("translate "+at2D.x, ", "+at2D.y);

                gl.glTranslated(at2D.x, at2D.y, 0);
                SymbolRenderer item = getRenderer(row, s);
                SymbolRectangle r = item.getSymbolRectangle((int) at2D.x, (int) at2D.y);
                LOG.debug("Rectangle " + row + " " + r.x + ", " + r.x2 + ", " + r.y + ", " + r.y2);
                item.render(gl);


                gl.glTranslated(-at2D.x, -at2D.y, 0);

                aText.begin3DRendering();
                GLUtil.cheezyOutline(aText, Integer.toString(row), Color.WHITE, Color.BLACK, (int) at2D.x + 20, (int) at2D.y - 20);
                aText.end3DRendering();

                row++;
                if (row > MAX) {
                    break;
                }
            }
        }
        SymbolRenderer.COLOR1 = true;

        // Merge tree algorithm.


        for (String category : myGroups.keySet()) {
            List<CatLookup> theLookup = myGroups.get(category);
            // This monster is for a 2 dimension interval tree..
            IntervalTree<IntervalTree<Node>> theMergeTree = new IntervalTree<IntervalTree<Node>>();
           // row = 0;
            for(CatLookup cat : theLookup){
            //for (Vec4 at3D : thePoints) {

                // Project into GL to get the rectangle of the actual drawn symbol
                Vec4 at2D = v.project(cat.the3D);
                SymbolRenderer item = getRenderer(cat.row, s);
                SymbolRectangle r = item.getSymbolRectangle((int) at2D.x, (int) at2D.y);

                // Initial node information
                Interval xRange = new Interval(r.x, r.x2);
                Interval yRange = new Interval(r.y, r.y2);
                Node new1 = new Node(at2D, cat.row, 1);
                new1.myCoverage = r;

                boolean hittingRectangles = true;
                while (hittingRectangles) {  // Gobbling up rectangles
                    hittingRectangles = false;

                    List<Interval> xOverlapSet = theMergeTree.getOverlappingIntervals(xRange);
                    if (xOverlapSet.size() > 0) {
                        // Eat the old ranges into the new rectangle...
                        for (Interval x : xOverlapSet) {

                            IntervalTree<Node> ySet = theMergeTree.get(x);
                            List<Interval> yOverlapSet = ySet.getOverlappingIntervals(yRange);

                            // Hit x range AND y range
                            if (yOverlapSet.size() > 0) {
                                hittingRectangles = true;

                                // Expand rectangle to cover this x range plus ourselves...
                                r.x = Math.min(r.x, x.getStart());
                                r.x2 = Math.max(r.x2, x.getEnd());

                                // For each hit rectangle, increase the hit count...
                                // Mark the old rectangle for removal.
                                List<Interval> toDelete = new ArrayList<Interval>();
                                for (Interval y : yOverlapSet) {
                                    Node data = ySet.get(y);
                                    // New rectangle represents all the data that came before...
                                    new1.myCount += data.myCount;
                                    r.y = Math.min(r.y, y.getStart());
                                    r.y2 = Math.max(r.y2, y.getEnd());
                                    toDelete.add(y);
                                }
                                // and then remove old rectangles...
                                for (Interval i : toDelete) {
                                    ySet.delete(i);
                                }
                            }
                        }
                    }

                    if (hittingRectangles) {
                        xRange = new Interval(r.x, r.x2);
                        yRange = new Interval(r.y, r.y2);
                        at2D = new Vec4((r.x2 + r.x) / 2, (r.y2 + r.y) / 2);

                        new1.thePoint = at2D;
                        new1.theRow = cat.row;
                        r.centerx = (int) at2D.x;
                        r.centery = (int) at2D.y;
                    }
                }


                // Insert the final rectangle....
                IntervalTree<Node> existing = theMergeTree.get(xRange);
                if (existing == null) {
                    existing = new IntervalTree<Node>();
                    theMergeTree.insert(xRange, existing);
                }
                existing.insert(yRange, new1);
                //  LOG.debug("while done");
               // row++;
               // if (row > MAX) {
                //    break;
               // }
            }


            // Render pass with interval tree...
            Iterable<IntervalTree<Node>> XSets = theMergeTree.getValues();
            int xcount = 0;
            int ycount = 0;
            for (IntervalTree<Node> x : XSets) {
                Iterable<Node> YSet = x.getValues();
                for (Node y : YSet) {
                    ycount++;
                    gl.glTranslated(y.thePoint.x, y.thePoint.y, 0);
                    SymbolRenderer sr = getRenderer(y.theRow, s);
                    sr.render(gl);

                    gl.glTranslated(-y.thePoint.x, -y.thePoint.y, 0);
                    if (y.myCount > 1) {
                        sr.renderSymbolRectangle(gl, y.myCoverage);
                    }
                }
                xcount++;
            }

            // Separate label pass if wanted....
            aText.begin3DRendering();
            for (IntervalTree<Node> x : XSets) {
                Iterable<Node> YSet = x.getValues();
                for (Node y : YSet) {
                    if (y.myCount > 1) {
                        int tx = y.myCoverage.x2;
                        int ty = y.myCoverage.y;
                        GLUtil.cheezyOutline(aText, Integer.toString(y.myCount), Color.WHITE, Color.BLACK, tx, ty);

                    }
                }
            }

            aText.end3DRendering();

        }
        GLUtil.popOrtho2D(dc);
    }
// FIXME: should check rectangle with screen and clip out non-visibles...
// Get the X range rectangles matching this one...
          /*  First attempt raw addition of rectangles....
     * IntervalTree<Node> allSameX = theMergeTree.get(new Interval(r.x, r.x2));
     if (allSameX == null) {
     // If we're first, add us....
     IntervalTree<Node> newYSet = new IntervalTree<Node>();
     newYSet.insert(new Interval(r.y, r.y2), new Node(at2D, row));
     theMergeTree.insert(new Interval(r.x, r.x2), newYSet);
     } else {

     // otherwise add this Y interval to set....
     Node existing = allSameX.get(new Interval(r.y, r.y2));
     if (existing != null){
     exist++;
     }
     // FIXME: currently lose the old one here with overlapping rect
     allSameX.insert(new Interval(r.y, r.y2), new Node(at2D, row));
     }*/
// Render pass....
//int row = 0;
       /* for (Vec4 at3D : points) {
     // Project 3D world coordinates to 2D view (whenever eye changes)
     // Could maybe cache these...check for view changing somehow...
     // FIXME: Possible speed up here
     Vec4 at2D = v.project(at3D);

     // LOG.debug("translate "+at2D.x, ", "+at2D.y);
     gl.glTranslated(at2D.x, at2D.y, 0);
     SymbolRenderer item = getRenderer(row, s);
     item.render(gl);
     gl.glTranslated(-at2D.x, -at2D.y, 0);
     row++;

     }*/

    public String getCategory(int row, Symbology s) {
        String category = "";

        if (s != null) {

            // Simple single mode...
            if (s.use == Symbology.SINGLE) {
                category = "";

                // Category 
            } else if (s.use == Symbology.CATEGORY_UNIQUE_VALUES) {

                if (myColumn != null) {
                    category = myColumn.getValue(row);
                    SymbolRenderer rr = myCategoryLookup.get(category);  // Could be null, it's ok
                    if (rr == null) {
                        category = "";
                    }
                }
            }
        }
        return category;

    }

    public SymbolRenderer getRenderer(int row, Symbology s) {
        SymbolRenderer rr = null;

        if (s != null) {

            // Simple single mode...
            if (s.use == Symbology.SINGLE) {
                rr = mySingle;

                // Category 
            } else if (s.use == Symbology.CATEGORY_UNIQUE_VALUES) {

                if (myColumn != null) {
                    String text = myColumn.getValue(row);
                    rr = myCategoryLookup.get(text);  // Could be null, it's ok
                    if (rr == null) { // no category found...
                        rr = mySingle; // then try the single one...
                    }
                }
            }
        }
        if (rr == null) {
            rr = myFailSafe; // Fall back renderer
        }
        return rr;
    }
}
