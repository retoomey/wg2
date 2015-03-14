package org.wdssii.gui.charts;

import com.sun.opengl.util.BufferUtil;
import java.nio.ByteBuffer;
import javax.media.opengl.GL;
import org.wdssii.core.StopWatch;
import org.wdssii.datatypes.DataType;
import org.wdssii.geom.Location;
import org.wdssii.geom.V3;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.GLWorld;
import static org.wdssii.gui.charts.VSliceChart.myNumCols;
import static org.wdssii.gui.charts.VSliceChart.myNumRows;
import org.wdssii.gui.products.filters.DataFilter;
import org.wdssii.gui.products.volumes.VolumeValue;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * VSlice 2D Renderer in a GLDrawable
 *
 * @author Robert Toomey
 */
public class VSlice2DGLEventListener extends LLHGLEventListener {

    private final static Logger LOG = LoggerFactory.getLogger(VSlice2DGLEventListener.class);
    private int myBottomM = 0;
    private int myTopM = 20000;

    public void setBottomMeters(int b) {
        myBottomM = b;
    }

    public void setTopMeters(int t) {
        myTopM = t;
    }

    public void updateBufferForTexture() {
        final int total = myNumCols * myNumRows;
        final boolean useFilters = false;

        //StopWatch watch = new StopWatch();
        //watch.start();
        ByteBuffer buffer;
        if (myBuffer != null) {  // FIXME: AND THE SIZE WE NEED
            buffer = myBuffer;  // AT THE MOMENT ASSUMING NEVER CHANGES
        } else {
            buffer = BufferUtil.newByteBuffer(total * 4);
        }

        if ((myVolume != null) && (myLLHArea != null)) {
            VolumeValue v = myVolume.getVolumeValue(myCurrentVolumeValueName);
            if (v != null) {
                myCurrentVolumeValueName = v.getName();
            }
            //sourceGrid = myLLHArea.getSegmentInfo(null, 0, myNumRows, myNumCols);
            sourceGrid = myLLHArea.getSegmentInfo(null, 0, myNumRows, myNumCols);
            if (myList != null) {
                myList.prepForVolume(myVolume);
            }

            DataFilter.DataValueRecord rec = myVolume.getNewDataValueRecord();

            // Buffers are reused from our geometry object
            my2DSlice.setValid(false);
            my2DSlice.setDimensions(sourceGrid.rows, sourceGrid.cols);
            //  int[] color2DVertices = my2DSlice.getColor2dFloatArray(sourceGrid.rows * sourceGrid.cols);
            float[] value2DVertices = my2DSlice.getValue2dFloatArray(sourceGrid.rows * sourceGrid.cols);

            //final double startHeight = sourceGrid.getStartHeight();
            final double startHeight = myTopM;
            // final double deltaHeight = sourceGrid.getDeltaHeight();
            final double deltaHeight = (myTopM - myBottomM) / (1.0 * sourceGrid.rows);
            final double deltaLat = sourceGrid.getDeltaLat();
            final double deltaLon = sourceGrid.getDeltaLon();

            ColorMap.ColorMapOutput data = new ColorMap.ColorMapOutput();

            // Shift to 'center' for each square so we get data value at the center of the grid square
            double currentHeight = startHeight - (deltaHeight / 2.0);
            double currentLat = sourceGrid.startLat + (deltaLat / 2.0);
            double currentLon = sourceGrid.startLon + (deltaLon / 2.0);

            int cp2d = 0;
            int cpv2d = 0;
            boolean warning = false;
            String message = "";
            Location b = new Location(0, 0, 0);

            myVolume.prepForValueAt();

            for (int row = 0; row < sourceGrid.rows; row++) {
                currentLat = sourceGrid.startLat;
                currentLon = sourceGrid.startLon;
                for (int col = 0; col < sourceGrid.cols; col++) {
                    // Add color for 2D table....
                    try {
                        b.init(currentLat, currentLon, currentHeight / 1000.0f);
                        myVolume.getValueAt(b, data, rec, myList, useFilters, v);
                    } catch (Exception e) {
                        warning = true;
                        message = e.toString();
                        data.setColor(0, 0, 0, 255);
                        data.filteredValue = DataType.MissingData;
                    } finally {
                        buffer.put((byte) (data.redI()));     // Red component
                        buffer.put((byte) (data.greenI()));   // Green component
                        buffer.put((byte) (data.blueI()));    // Blue component
                        buffer.put((byte) 255);
                        value2DVertices[cpv2d++] = data.filteredValue;
                    }
                    currentLat += deltaLat;
                    currentLon += deltaLon;
                }
                currentHeight -= deltaHeight;
            }
            if (warning) {
                LOG.error("Exception during 2D VSlice grid generation " + message);
            } else {
                my2DSlice.setValid(true);
            }

            // Copy?  Bleh.....
              /*  int[] data2 = my2DSlice.getColor2dFloatArray(0);

             int counter = 0;
             for (int i = 0; i < total; i++) {
             int pixel = data2[counter++];
             buffer.put((byte) ((pixel >>> 16) & 0xFF));     // Red component
             buffer.put((byte) ((pixel >>> 8) & 0xFF));      // Green component
             buffer.put((byte) (pixel & 0xFF));               // Blue component
             // buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
             buffer.put((byte) 255);
             }
             */

        } else {
            for (int i = 0; i < total; i++) {
                buffer.put((byte) 255);
                buffer.put((byte) 0);
                buffer.put((byte) 0);
                buffer.put((byte) 255);
            }
        }
        buffer.flip();
        myBuffer = buffer;
        // watch.stop();
        // LOG.debug("VSlice GENERATION TIME IS " + watch);
    }

    void drawGLWorld(GLWorld w) {

        // FIXME: possible sync errors...
        if (sourceGrid != null) {
            // Try to just draw a line between locations....
            final double deltaLat = sourceGrid.getDeltaLat();
            final double deltaLon = sourceGrid.getDeltaLon();
            // sourceGrid = myLLHArea.getSegmentInfo(null, 0, myNumRows, myNumCols);
            double currentLat = sourceGrid.startLat + (deltaLat / 2.0);
            double currentLon = sourceGrid.startLon + (deltaLon / 2.0);
            double height = sourceGrid.bottomHeight;
            GL gl = w.gl;
            gl.glColor4d(1.0, 0.0, 0.0, 1.0);
            gl.glLineWidth(2);
            gl.glDisable(GL.GL_DEPTH_TEST);
            gl.glBegin(gl.GL_LINE_STRIP);
            for (int c = 0; c < myNumCols; c++) {
                V3 at = w.projectLLH(currentLat, currentLon, height);
                gl.glVertex3d(at.x, at.y, at.z);
                currentLat += deltaLat;
                currentLon += deltaLon;
            }
            gl.glEnd();
            gl.glLineWidth(1);
            gl.glEnable(GL.GL_DEPTH_TEST); // FIXME: Should push/pop


        }
    }
}