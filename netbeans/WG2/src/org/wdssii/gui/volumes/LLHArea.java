package org.wdssii.gui.volumes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.wdssii.gui.products.ProductHandlerList;

import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Cylinder;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sphere;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import gov.nasa.worldwind.util.GeometryBuilder;
import gov.nasa.worldwind.util.Logging;
import javax.swing.JComponent;

/** A root class for all of our volumes.  Common functionality will go here.
 * 
 * @author Robert Toomey
 * */
public class LLHArea extends AVListImpl implements Movable {

    /** Change to pass onto the LLHArea.  All fields common to
     LLHArea are here
    */
    public static class LLHAreaMemento {
        public boolean visible;
        public boolean useVisible = false;  // Hummm either flags or set on creation
        public boolean only;
        public boolean useOnly = false;
        public double maxHeight;
        public boolean useMaxHeight = false;
    }
    
    // Hummm
    private boolean visible = true;
    private boolean only = false; 
    
    /** Every LLHArea can follow a particular product */
    protected String myProductFollow = ProductHandlerList.TOP_PRODUCT;
    
    /** Do we use virtual volume or regular one? */
    protected boolean myUseVirtualVolume = false;

    protected static final String SUBDIVISIONS = "Subdivisions";
    protected static final String VERTICAL_EXAGGERATION = "VerticalExaggeration";

    private AirspaceAttributes attributes;
    
    protected double lowerAltitude = 0.0;
    protected double upperAltitude = 1.0;
    
    private LatLon groundReference;

    // Geometry computation and rendering support.
    private LLHAreaRenderer renderer = new LLHAreaRenderer();
    private GeometryBuilder geometryBuilder = new GeometryBuilder();

    public LLHArea() {
        this(new BasicAirspaceAttributes());
    }

    /** Get the memento for this class */
    public LLHAreaMemento getMemento(){
        LLHAreaMemento m = new LLHAreaMemento();
        m.visible = visible;
        m.only = only;
        m.maxHeight = upperAltitude;
        return m;
    }
    
    public void setFromMemento(LLHAreaMemento l){
        if (l.useVisible){ setVisible(l.visible); }
        if (l.useOnly) { setOnly(l.only); }
        if (l.useMaxHeight){ setAltitudes(lowerAltitude, l.maxHeight);}
    }
    
    // Airspaces perform about 5% better if their extent is cached, so do that here.
    protected static class ExtentInfo {
        // The extent depends on the state of the globe used to compute it, and the vertical exaggeration.

        protected Extent extent;
        protected double verticalExaggeration;
        protected Object globeStateKey;

        public ExtentInfo(Extent extent, DrawContext dc) {
            this.extent = extent;
            this.verticalExaggeration = dc.getVerticalExaggeration();
            this.globeStateKey = dc.getGlobe().getStateKey(dc);
        }

        protected boolean isValid(DrawContext dc) {
            return this.verticalExaggeration == dc.getVerticalExaggeration()
                    && globeStateKey.equals(dc.getGlobe().getStateKey(dc));
        }
    }
    protected HashMap<Globe, ExtentInfo> extents = new HashMap<Globe, ExtentInfo>(2); // usually only 1, but few at most

    public LLHArea(AirspaceAttributes attributes) {
        if (attributes == null) {
            String message = "nullValue.AirspaceAttributesIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.attributes = attributes;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isOnly() {
        return this.only;
    }

    public void setOnly(boolean only) {
        this.only = only;
    }

    public AirspaceAttributes getAttributes() {
        return this.attributes;
    }

    public void setAttributes(AirspaceAttributes attributes) {
        if (attributes == null) {
            String message = "nullValue.AirspaceAttributesIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.attributes = attributes;
    }

    public double[] getAltitudes() {
        double[] array = new double[2];
        array[0] = this.lowerAltitude;
        array[1] = this.upperAltitude;
        return array;
    }

    protected double[] getAltitudes(double verticalExaggeration) {
        double[] array = this.getAltitudes();
        array[0] = array[0] * verticalExaggeration;
        array[1] = array[1] * verticalExaggeration;
        return array;
    }

    public void setAltitudes(double lowerAltitude, double upperAltitude) {
        this.lowerAltitude = lowerAltitude;
        this.upperAltitude = upperAltitude;
        this.setExtentOutOfDate();
    }

    public void setAltitude(double altitude) {
        this.setAltitudes(altitude, altitude);
    }

    public LatLon getGroundReference() {
        return this.groundReference;
    }

    public void setGroundReference(LatLon groundReference) {
        this.groundReference = groundReference;
    }
    
    public boolean isAirspaceVisible(DrawContext dc) {
        Extent extent = this.getExtent(dc);
        return extent != null && extent.intersects(dc.getView().getFrustumInModelCoordinates());
    }

    public Extent getExtent(DrawContext dc) {
        ExtentInfo extentInfo = this.extents.get(dc.getGlobe());
        if (extentInfo != null && extentInfo.isValid(dc)) {
            return extentInfo.extent;
        }

        extentInfo = new ExtentInfo(this.doComputeExtent(dc), dc);
        this.extents.put(dc.getGlobe(), extentInfo);
        return extentInfo.extent;
    }

    protected void setExtentOutOfDate() {
        this.extents.clear(); // Doesn't hurt to remove all cached extents because re-creation is cheap
    }

    public LLHAreaRenderer getRenderer() {
        return this.renderer;
    }

    protected void setRenderer(LLHAreaRenderer renderer) {
        this.renderer = renderer;
    }

    public void render(DrawContext dc) {
        if (!this.isVisible()) {
            return;
        }

        if (!this.isAirspaceVisible(dc)) {
            return;
        }

        this.doRender(dc);
    }

    public void renderGeometry(DrawContext dc, String drawStyle) {
        if (drawStyle == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.doRenderGeometry(dc, drawStyle);
    }

    public void renderExtent(DrawContext dc) {
        this.doRenderExtent(dc);
    }

    @Override
    public void move(Position position) {
        this.moveTo(this.getReferencePosition().add(position));
    }

    @Override
    public void moveTo(Position position) {
        Position oldRef = this.getReferencePosition();
        this.doMoveTo(oldRef, position);
    }

    protected void doMoveTo(Position oldRef, Position newRef) {
        double[] altitudes = this.getAltitudes();
        double elevDelta = newRef.getElevation() - oldRef.getElevation();
        this.setAltitudes(altitudes[0] + elevDelta, altitudes[1] + elevDelta);
    }

    protected Position computeReferencePosition(List<? extends LatLon> locations, double[] altitudes) {
        int count = locations.size();
        if (count == 0) {
            return null;
        }

        LatLon ll;
        if (count < 3) {
            ll = locations.get(0);
        } else {
            ll = locations.get(count / 2);
        }

        return new Position(ll, altitudes[0]);
    }

    //**************************************************************//
    //********************  Geometry Rendering  ********************//
    //**************************************************************//
    // TODO: utility method for transforming list of LatLons into equivalent list comparable of crossing the dateline
    // (a) for computing a bounding sector, then bounding cylinder
    // (b) for computing tessellations of the list as 2D points
    // These lists of LatLons (Polygon) need to be capable of passing over
    // (a) the dateline
    // (b) either pole
    protected Extent doComputeExtent(DrawContext dc) {
        return null;
    }

    protected void doRenderGeometry(DrawContext dc, String drawStyle) {
    }

    protected GeometryBuilder getGeometryBuilder() {
        return this.geometryBuilder;
    }

    protected void setGeometryBuilder(GeometryBuilder gb) {
        if (gb == null) {
            String message = "nullValue.GeometryBuilderIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.geometryBuilder = gb;
    }

    protected void doRender(DrawContext dc) {
        renderer.renderNow(dc, Arrays.asList(this));
    }

    protected void doRenderExtent(DrawContext dc) {
        Extent extent = this.getExtent(dc);
        if (extent != null && extent instanceof Renderable) {
            ((Renderable) extent).render(dc);
        }
    }

    protected Cylinder computeBoundingCylinder(DrawContext dc, Iterable<? extends LatLon> locations) {
        Globe globe = dc.getGlobe();
        double verticalExaggeration = dc.getVerticalExaggeration();
        double[] altitudes = this.getAltitudes();

        // Get the points corresponding to the given locations at the lower and upper altitudes.
        ArrayList<Vec4> points = new ArrayList<Vec4>();
        for (int a = 0; a < 2; a++) {
            for (LatLon ll : locations) {
                points.add(globe.computePointFromPosition(ll.getLatitude(), ll.getLongitude(),
                        verticalExaggeration * altitudes[a]));
            }
        }
        if (points.isEmpty()) {
            return null;
        }

        // Compute the average point.
        Vec4 centerPoint = Vec4.computeAveragePoint(points);
        if (centerPoint == null) {
            return null;
        }

        // Get the center point and the lower and upper altitudes.
        LatLon centerLocation = globe.computePositionFromPoint(centerPoint);
        points.add(globe.computePointFromPosition(centerLocation.getLatitude(), centerLocation.getLongitude(),
                verticalExaggeration * altitudes[0]));
        points.add(globe.computePointFromPosition(centerLocation.getLatitude(), centerLocation.getLongitude(),
                verticalExaggeration * altitudes[1]));

        // Compute the surface normal at the center point. This will be the axis or up direction of the extent.
        Vec4 axis = globe.computeSurfaceNormalAtPoint(centerPoint);

        // Compute the extrema parallel and perpendicular projections of each point on the axis.
        double min_parallel = 0.0;
        double max_parallel = 0.0;
        double max_perp = 0.0;
        for (Vec4 vec : points) {
            Vec4 p = vec.subtract3(centerPoint);
            double parallel_proj = p.dot3(axis);
            Vec4 parallel = axis.multiply3(parallel_proj);
            Vec4 perpendicular = p.subtract3(parallel);
            double perpendicular_proj = perpendicular.getLength3();

            if (parallel_proj < min_parallel) {
                min_parallel = parallel_proj;
            } else if (parallel_proj > max_parallel) {
                max_parallel = parallel_proj;
            }

            if (perpendicular_proj > max_perp) {
                max_perp = perpendicular_proj;
            }
        }
        
        // The bottom and top of the cylinder are the extrema parallel projections from the center point along the axis.
        Vec4 bottomPoint = axis.multiply3(min_parallel).add3(centerPoint);
        Vec4 topPoint = axis.multiply3(max_parallel).add3(centerPoint);
        // The radius of the cylinder is the extrama perpendicular projection.
        double radius = max_perp;

        return new Cylinder(bottomPoint, topPoint, radius);
    }

    protected Extent computeBoundingExtent(DrawContext dc, Iterable<? extends Airspace> airspaces) {
        Vec4 center = null;
        double radius = 0;
        int count = 0;

        // Add the center point of all airspace extents. This is the first step in computing the mean center point.
        for (Airspace airspace : airspaces) {
            Extent extent = airspace.getExtent(dc);
            if (extent != null) {
                center = (center != null) ? extent.getCenter().add3(center) : extent.getCenter();
                count++;
            }
        }

        // If there's no mean center point, then we cannot compute an enclosing extent, so just return null.
        if (center == null) {
            return null;
        }

        // Divide by the number of contributing extents to compute the mean center point.
        center = center.divide3(count);

        // Compute the maximum distance from the mean center point to the outermost point on an airspace extent. This
        // will be the radius of the enclosing extent.
        for (Airspace airspace : airspaces) {
            Extent extent = airspace.getExtent(dc);
            if (extent != null) {
                double distance = extent.getCenter().distanceTo3(center) + extent.getRadius();
                if (radius < distance) {
                    radius = distance;
                }
            }
        }

        return new Sphere(center, radius);
    }

    @Override
    public Position getReferencePosition() {
        return null;
    }

    /** Set the product that we follow in the display */
    public void setProductFollow(String f) {
        myProductFollow = f;
    }

    /** Get the product that we follow in the display */
    public String getProductFollow() {
        return myProductFollow;
    }

    /** Set if we use a virtual or regular volume */
    public void setUseVirtualVolume(boolean current) {
        myUseVirtualVolume = current;
    }

    /** Get if we use a virtual or regular volume */
    public boolean getUseVirtualVolume() {
        return myUseVirtualVolume;
    }
    
    /** Activate our GUI within the given JComponent.  The JComponent is
     * assumed empty.  We should assign the layout we want to it.  The
     * caller is trusting us to handle this properly. 
     * 
     * @param source 
     */
    public void activateGUI(JComponent source){
        
    }
    
    /** Update our current GUI controls */
    public void updateGUI(){
        
    }
}