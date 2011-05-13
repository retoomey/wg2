package org.wdssii.gui.products;

import java.awt.Point;
import javax.media.opengl.GL;

import org.wdssii.datatypes.Table2DView.LocationType;
import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.GridVisibleArea;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;

/** ProductRenderer is a helper class of Product.  It draws the DataType in the 3D world
 * 
 * @author Robert Toomey
 *
 */
public abstract class ProductRenderer {

    /** The product we render */
    private Product myProduct = null;

    // Get the color map for this product (FIXME: more general product info
    // instead)
    // public ColorMap getColorMap();
    // Create anything needed to draw this product in the current dc
    public void initToProduct(DrawContext dc, Product aProduct) {
        myProduct = aProduct;
    }

    // Draw the product in the current dc
    public abstract void draw(DrawContext dc);

    /** Return the product we draw */
    public Product getProduct() {
        return myProduct;
    }

    /** Get the product readout for a given screenpoint in the drawcontext */
    public ProductReadout getProductReadout(Point p, DrawContext dc) {
        return new ProductReadout();
    }

    /** The default product outline in the 3D world.  This assumes that
     * the Product2DTable implementation of the product smoothly maps to
     * 3D (Which is true for LatLonGrid, RadialSet, WindField...)
     * @param dc
     */
    public void drawProductOutline(DrawContext dc) {
        // FIXME: Humm...right now this is based upon the visible grid of
        // the DataTable view...which only is showing top product..
        // We should probably store a current grid within the product or
        // renderer for multi-product support....
        // This will eventually track with mouse....

        GridVisibleArea a = CommandManager.getInstance().getVisibleGrid();
        drawGridOutline(dc, a);
    }

    public void drawReadoutCellOutline(DrawContext dc, ProductReadout pr) {
        // Subclasses should outline the data given in the ProductReadout
    }

    /** Based on a table grid visible area, draw the corresponding outline in the 3D world window */
    public void drawGridOutline(DrawContext dc, GridVisibleArea a) {
        Product aProduct = getProduct();
        if ((aProduct != null) && (a != null)) {
            Globe myGlobe = dc.getGlobe();
            GL gl = dc.getGL();

            gl.glPushAttrib(GL.GL_LINE_BIT);
            gl.glLineWidth(3);
            gl.glColor4d(1d, 1d, 1d, 1d);
            Location location;
            Vec4 p;
            int counter = 0;
            location = new Location(1.0, 1.0, 1.0);

            Product2DTable table = aProduct.get2DTable();

            int lastColumn = a.lastFullColumn;
            int lastRow = a.lastFullRow;
            int startColumn = a.startCol;
            int startRow = a.startRow;

            // Check integrity or we can crash the video driver lol
            boolean goodToDraw = ((lastRow - startRow >= 0)
                    && (lastColumn - startColumn >= 0));

            if (goodToDraw) {

                // Outline the last column that's partly visible in table.  Make this the full column to
                // only show cells that are fully drawn.
                int r = startRow;
                int c = startColumn;
                int dfaState = 0;
                int lastState = 3;
                boolean validPoint = false;
                // Using some old compiler theory to keep from having four separate loops for each side
                // and a lot of redundant code. Use a DFA to loop around the data
                gl.glLineWidth(5.0f);
                gl.glBegin(GL.GL_LINE_LOOP);
                /*
                while (dfaState <= lastState) {
                
                // Get point for the bzscan outline
                switch (dfaState) { // Going clockwise around the data
                case 0: // top, marching left until last column to draw
                validPoint = table.getLocation(LocationType.TOP_LEFT, r, c, location);
                c++;
                if (c > lastColumn) {
                dfaState++;
                c--;
                }
                break;
                case 1: // right side, increasing row, keeping column the same
                validPoint = table.getLocation(LocationType.TOP_RIGHT, r, c, location);
                r++;
                if (r > lastRow) {
                dfaState++;
                r--;
                }
                break;
                case 2: // bottom side, decreasing column, keeping row the same
                validPoint = table.getLocation(LocationType.BOTTOM_RIGHT, r, c, location);
                c--;
                if (c < startColumn) {
                dfaState++;
                c++;
                }
                break;
                case 3: // left side, decreasing row, keeping column the same
                validPoint = table.getLocation(LocationType.BOTTOM_LEFT, r, c, location);
                r--;
                if (r < startRow) {
                dfaState++;
                r++;
                }
                break;
                default:
                dfaState = lastState;
                break;  // Should be unreachable
                }
                // Post vertex from last run of DFA (if point was valid)
                if (validPoint) {
                p = myGlobe.computePointFromPosition(
                Angle.fromDegrees(location.getLatitude()),
                Angle.fromDegrees(location.getLongitude()),
                location.getHeightKms() * 1000);
                gl.glVertex3d(p.x, p.y, p.z);
                validPoint = false;
                counter++;
                if (counter > 1000) {
                System.out.println("Limiting readout boundary to " + counter + " points");
                break;
                }
                }
                }
                 */
                gl.glEnd();
            }
            gl.glPopAttrib();
        }
    }
}