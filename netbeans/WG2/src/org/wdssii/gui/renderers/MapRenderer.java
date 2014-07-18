package org.wdssii.gui.renderers;

import org.wdssii.gui.features.Feature3DRenderer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.nio.FloatBuffer;
import javax.media.opengl.GL;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.gui.GLWorld;
import org.wdssii.geom.V3;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.MapFeature.MapMemento;
import org.wdssii.storage.Array1D;
import org.wdssii.storage.Array1DOpenGL;
import org.wdssii.storage.GrowList;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.MapFeature;

/**
 *
 * @author Robert Toomey
 *
 * Uses geotools to create/render a shapefile map in a GLWorld window.
 * Currently only looking at JTS MultiPolygon (Polygon) objects and creating
 * line strips from them.
 * @todo Add other types, clean up code, etc...
 *
 * FIXME: combine common code from PolarGridRenderer into a background class
 * that hides most of the job stuff....
 */
public class MapRenderer extends Feature3DRenderer {

    private final static Logger LOG = LoggerFactory.getLogger(MapRenderer.class);
    /**
     * Lock for changing offsets/polygonData
     */
    private final Object drawLock = new Object(); // Lock 2 (second)
    /**
     * Offset array, one per Polygon
     */
    private GrowList<Integer> myOffsets;
    /**
     * Verts for the map. Single for now (monster maps may fail)
     */
    protected Array1D<Float> polygonData;
    /**
     * Current memento settings
     */
    private MapMemento current;
    /**
     * The feature we use from GeoTools
     */
    private SimpleFeatureSource mySource;
    private boolean tryToLoad = true;

    public MapRenderer() {
    }

    // public MapRenderer(SimpleFeatureSource f) {
    //     mySource = f;
    //}
    @Override
    public void initToFeature(Feature m) {
        if (m instanceof MapFeature) {
            MapFeature mf = (MapFeature) (m);
            mySource = mf.getFeatureSource();
        }
    }
    /**
     * The worker job if we are threading
     */
    private BackgroundMapMaker myWorker = null;
    private final Object workerLock = new Object(); // Lock 1 (first)
    private boolean myCreated = false;

    /**
     * Is this map fully created?
     */
    public boolean isCreated() {
        return myCreated;
    }

    /**
     * Get if this map if fully created
     */
    public void setIsCreated(boolean flag) {
        myCreated = flag;
    }

    @Override
    public void pick(GLWorld w, Point p, FeatureMemento m) {
    }

    @Override
    public void preRender(GLWorld w, FeatureMemento m) {
    }

    /**
     * Job for creating in the background any rendering
     */
    public static class BackgroundMapMaker extends WdssiiJob {

        public GLWorld w;
        private MapMemento m;
        private MapRenderer myMapRenderer;
        // Humm.. reload map each time we regenerate?
        // Current maps will load once probably...
        public SimpleFeatureSource source;

        public BackgroundMapMaker(String jobName, GLWorld aW, SimpleFeatureSource s, MapRenderer r, MapMemento map) {
            super(jobName);
            w = aW;
            myMapRenderer = r;
            source = s;
            m = map;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
            return create(w, m, myMapRenderer, source, monitor);
        }

        public WdssiiJobStatus create(GLWorld w, MapMemento m,
                MapRenderer r, SimpleFeatureSource source, WdssiiJobMonitor monitor) {

            //Globe myGlobe = dc.getGlobe();
            GrowList<Integer> workOffsets = new GrowList<Integer>();
            workOffsets.add(0);  // Just one for now
            int idx = 0;
            MultiPolygon mp;
            SimpleFeatureCollection stuff;
            MathTransform transform = null;

            // Try to load the feature
            try {
                stuff = source.getFeatures();

                // Look at CRS.  Our original wdssi shapefiles
                // were all simple lat/lon
                SimpleFeatureType schema = source.getSchema();
                CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
                if (crs == null) {
                    // Should warn that we have no .prj and 'guess' or
                    // allow user to set it...
                    LOG.error("shapefile is missing projection information (.prj), assuming it's lat/lon data...map could be wrong.");
                } else {

                    // If we have .prj, we want it in a projection system with
                    // a latitude/longitude...
                    CoordinateReferenceSystem world = DefaultGeographicCRS.WGS84;
                    boolean lenient = true;
                    try {
                        transform = CRS.findMathTransform(crs, world, lenient);
                    } catch (FactoryException ex) {
                        LOG.debug("Couldn't get tranformation to world coordinate system");
                    }
                }
            } catch (IOException ex) {
                LOG.error("Can't crete map " + ex.toString());
                return WdssiiJobStatus.OK_STATUS;
            }

            // Counter pass.  See how much memory we need....
            int multiPolyPointCount = 0;
            SimpleFeatureIterator i = stuff.features();
            while (i.hasNext()) {
                SimpleFeature f = i.next();
                Object geo = f.getDefaultGeometry();

                // JTS polygon format
                if (geo instanceof MultiPolygon) {
                    mp = (MultiPolygon) (geo);
                    int subGeometryCount = mp.getNumGeometries();
                    for (int sg = 0; sg < subGeometryCount; sg++) {
                        Object o1 = mp.getGeometryN(sg);

                        // Render each polygon of a multipoly as its
                        // own line strip or polygon in opengl.
                        if (o1 instanceof Polygon) {
                            Polygon poly = (Polygon) (o1);
                            poly.getNumPoints();
                            multiPolyPointCount += mp.getNumPoints();
                        }
                    }
                }
            }

            // Allocate memory...
            Array1D<Float> workPolygons = new Array1DOpenGL(multiPolyPointCount * 3, 0.0f);

            // Creation pass
            i = stuff.features();
            int actualCount = 0;

            //ElevationModel e = myGlobe.getElevationModel();
            workPolygons.begin();
            while (i.hasNext()) {
                SimpleFeature f = i.next();
                Geometry geo1 = (Geometry) f.getDefaultGeometry();
                Geometry geo;
                try {
                    if (transform != null) {
                        geo = JTS.transform(geo1, transform);
                    } else {
                        // We assume coordinates are WGS...map might
                        // be wrongly projected, but we did warn
                        geo = geo1;
                    }
                } catch (MismatchedDimensionException ex) {
                    LOG.error("Couldn't tranform map, mismatched dimensions");
                    continue;
                } catch (TransformException ex) {
                    LOG.error("Transform exception with map");
                    continue;
                }
                if (geo instanceof MultiPolygon) {
                    mp = (MultiPolygon) (geo);

                    int subGeometryCount = mp.getNumGeometries();
                    for (int sg = 0; sg < subGeometryCount; sg++) {
                        Object o1 = mp.getGeometryN(sg);

                        // Render each polygon of a multipoly as its
                        // own line strip or polygon in opengl.
                        if (o1 instanceof Polygon) {
                            Polygon poly = (Polygon) (o1);


                            Coordinate[] coorArray = poly.getCoordinates();
                            for (int p = 0; p < coorArray.length; p++) {
                                Coordinate C = coorArray[p];

                                double lat = C.y;
                                double lon = C.x;
                                V3 point = w.projectLLH(lat, lon, w.getElevation(lat, lon) + 100.0);
                                workPolygons.set(idx++, (float) point.x);
                                workPolygons.set(idx++, (float) point.y);
                                workPolygons.set(idx++, (float) point.z);
                                actualCount++;
                            }
                            workOffsets.add(idx);
                            // Update our renderer once a ring...abort if we're stale
                            if (!r.updateData(this, workOffsets, workPolygons, false)) {
                                return WdssiiJobStatus.OK_STATUS;
                            }

                        }
                    }

                }
            }
            workPolygons.end();
            LOG.debug("Map points allocated/actual" + multiPolyPointCount + ", " + actualCount);

            // Update our renderer once a ring...abort if we're stale
            r.updateData(this, workOffsets, workPolygons, false);
            return WdssiiJobStatus.OK_STATUS;
        }
    }
    /*
     * Tell if this changes requires a new background job. Some changes,
     * like line thickness are done by the renderer on the fly
     */

    public boolean changeNeedsUpdate(MapMemento new1, MapMemento old) {
        boolean needsUpdate = false;
        if (current == null) { // No settings yet...definitely update
            return true;
        }
        // Never change again for the moment
        return false;
    }

    /**
     * Draw the product in the current dc
     */
    @Override
    public void draw(GLWorld w, FeatureMemento mf) {

        MapMemento m = (MapMemento) (mf);
        // Regenerate if memento is different...
        if (changeNeedsUpdate(m, current)) {
            // Have to make copy of information for background job
            current = new MapMemento(m); // Keep new settings...
            MapMemento aCopy = new MapMemento(m);

            // Change out workers only in workerLock
            synchronized (workerLock) {
                if (myWorker != null) {
                    myWorker.cancel(); // doesn't matter really
                }
                myWorker = new BackgroundMapMaker("Job", w, mySource, this, aCopy);
                myWorker.schedule();
            }

        }

        synchronized (drawLock) {

            if (isCreated() && (polygonData != null)) {
                final GL gl = w.gl;
                Color line = m.getPropertyValue(MapMemento.LINE_COLOR);
                final float r = line.getRed() / 255.0f;
                final float g = line.getGreen() / 255.0f;
                final float b = line.getBlue() / 255.0f;
                final float a = line.getAlpha() / 255.0f;
                boolean attribsPushed = false;
                try {
                    Object lock1 = polygonData.getBufferLock();
                    synchronized (lock1) {

                        gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_LIGHTING_BIT
                                | GL.GL_COLOR_BUFFER_BIT
                                | GL.GL_ENABLE_BIT
                                | GL.GL_TEXTURE_BIT | GL.GL_TRANSFORM_BIT
                                | GL.GL_VIEWPORT_BIT | GL.GL_CURRENT_BIT
                                | GL.GL_LINE_BIT);

                        gl.glDisable(GL.GL_LIGHTING);
                        gl.glDisable(GL.GL_TEXTURE_2D);

                        // We WANT depth test, we use the hidden stipple for things behind...
                        gl.glEnable(GL.GL_DEPTH_TEST);

                        gl.glShadeModel(GL.GL_FLAT);
                        gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT
                                | GL.GL_CLIENT_PIXEL_STORE_BIT);
                        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
                        // gl.glEnableClientState(GL.GL_COLOR_ARRAY);
                        attribsPushed = true;
                        FloatBuffer z = polygonData.getRawBuffer();
                        gl.glColor4f(r, g, b, a);
                        Integer t = m.getPropertyValue(MapMemento.LINE_THICKNESS);
                        gl.glLineWidth(t);
                        GLUtil.renderArrays(w.gl, z, myOffsets, GL.GL_LINE_LOOP);

                        // Try the hidden stipple
                        GLUtil.pushHiddenStipple(w.gl);
                        GLUtil.renderArrays(w.gl, z, myOffsets, GL.GL_LINE_LOOP);
                        GLUtil.popHiddenStipple(w.gl);

                    }
                } finally {
                    if (attribsPushed) {
                        gl.glPopClientAttrib();
                        gl.glPopAttrib();
                    }
                }
            }
        }
    }

    /**
     * Update our data to the data of a worker. Note because of threads for
     * brief time periods more than one worker might be going. (Fast changing of
     * settings). The worker will stop on false
     */
    public boolean updateData(BackgroundMapMaker worker, GrowList<Integer> off, Array1D<Float> poly,
            boolean done) {

        // WorkerLock --> drawLock.  Never switch order
        boolean keepWorking;
        synchronized (workerLock) {
            // See if worker changed..if so 
            if (worker == myWorker) {

                synchronized (drawLock) {
                    myOffsets = off;
                    polygonData = poly;
                    setIsCreated(true);
                }
                keepWorking = true;
            } else {
                // Old worker, stop...
                keepWorking = false;
            }
        }
        FeatureList.theFeatures.updateOnMinTime();
        return keepWorking;
    }
}