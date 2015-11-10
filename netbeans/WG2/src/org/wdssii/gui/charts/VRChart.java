package org.wdssii.gui.charts;

import java.awt.Container;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GLEventListener;
import javax.swing.Icon;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.wdssii.gui.ProductManager;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.products.VolumeSliceInput;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.volumes.LLHAreaSet;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

import com.jidesoft.swing.JideToggleButton;

/**
 * CAPPI is the horizontal vertical slice
 *
 * @author Robert Toomey
 */
public class VRChart extends LLHAreaChartGL {

    private final static Logger LOG = LoggerFactory.getLogger(VRChart.class);
    private final static int TEXTURE_TARGET = GL.GL_TEXTURE_2D;
    private JSlider mySlider = null;
    private JSlider myXSlider = null;
    private JSlider myYSlider = null;
    /**
     * Keep volume value setting per chart
     */
    // public String myCurrentVolumeValue = "";
    /**
     * The number of rows or altitudes of the VSlice
     */
    public static final int myNumRows = 150;  //50
    /**
     * The number of cols or change in Lat/Lon
     */
    public static final int myNumCols = 300; //100
    /**
     * Holder for the slice GIS 'state'
     */
    private VolumeSliceInput myCurrentGrid =
            new VolumeSliceInput(myNumRows, myNumCols, 0, 0,
            0, 0, 0, 50);
    /**
     * The opengl draw listener
     */
    private VRGLEventListener myGLListener = new VRGLEventListener();
    private JideToggleButton myB1, myB2, myB3;

    public GLEventListener getGLEventListener() {
        return myGLListener;
    }

    /**
     * Static method to create, called by reflection. Humm couldn't we just call
     * basic constructor lol a method directly?
     */
    public static VRChart create() {

        return new VRChart();

    }

    @Override
    public void updateChart(boolean force) {
        // This is called a lot...during point drag, etc.  We should check to see
        // if anything has CHANGED that needs us to regenerate.
// REGENERATE the slice....
        // With texture, I think we can use it in the 3D window as well as the 2D,
        // just have to generate the binding points for each...
        // if (myGLListener != null) {
        //     //myGLListener.setData(llhArea, volume, aList);
        //myGLListener.setTitle(titleKey);
        //     if (mySlider != null) {
        //         myGLListener.setTopMeters(mySlider.getValue());
        //     }
        //     myGLListener.updateBufferForTexture();
        //     repaintChart();
        //     return;
        // }
        // The LLHArea is the geometry in the 3d window we are
        // matching our coordinates to.  It can be valid without
        // any product/volume information.
        //myPlot.setCurrentVolumeValueName(myCurrentVolumeValue);
        LLHAreaSet llhArea = getLLHAreaToPlot();
        if (llhArea == null) {
            // If there isn't a 3D slice LLHArea object geometry to follow,
            // clear us...
            myGLListener.setData(null, null, null);
            myGLListener.setTitle("No slice in 3d window");

            myGLListener.updateBufferForTexture();
            repaintChart();
            return;
        }   // No slice to follow, return..

        /**
         * Get the GIS key
         */
        String gisKey = llhArea.getGISKey();

        // Sync the height/range axis to the GIS vslice range when updated, this resets
        // any 'zoom' in the chart...but only if GIS key has CHANGED.  This
        // way users can toggle products and keep zoom level for comparison,
        // but if they drag the vslice we reset to full slice.
        if (!getGISKey().equals(gisKey)) {
            // Get the full non-subgrid area and make the distance line axis correct,
            // even if we are missing everything else
            VolumeSliceInput info = llhArea.getSegmentInfo(myCurrentGrid, 0, myNumRows, myNumCols);
            if (info != null) {
                String t = getGISLabel(info.startLat, info.startLon,
                        info.endLat, info.endLon);
                myGLListener.setLLHLabel(t);
                //myDistanceAxis.setFixedRange(new Range(0, llhArea.getRangeKms(0, 1) / 1000.0));
                //myHeightAxis.setFixedRange(new Range(llhArea.getBottomHeightKms() / 1000.0, llhArea.getTopHeightKms() / 1000.0));
            }
        }
        setGISKey(gisKey);

        ProductVolume volume = ProductManager.getCurrentVolumeProduct(getUseProductKey(), getUseVirtualVolume());

        FilterList aList = null;
        String useKey = getUseProductKey();
        String titleKey;
        /**
         * Get the filter list of the product we are following
         */
        ProductFeature tph = ProductManager.getInstance().getProductFeature(useKey);
        Product p = null;
        if (tph != null) {
            aList = tph.getFList();
            p = tph.getProduct();
        }
        if (p != null) {
            titleKey = p.getProductInfoString(false);
        } else {
            titleKey = "No product";
        }

        if ((volume == null) || (aList == null)) {
            // If there isn't a valid data source, clear us out...
            // clear us...
            myVolume = null;
            // CLEAR everything (Blank the slice)
            myGLListener.setData(null, null, null);
            myGLListener.setTitle("No volume data");
            myGLListener.updateBufferForTexture();
            repaintChart();
            return;
        }

        /**
         * Physical key of the Lat/Lon/Height location
         */
        String key = gisKey;

        /**
         * Add volume key
         */
        key += volume.getKey();

        /**
         * Add filter key
         */
        key += aList.getFilterKey(getUseProductFilters());

        // Only different part is here..right?
        /**
         * Add the slider keys
         */
        if (mySlider != null) {
            key += mySlider.getValue();
        }

        boolean keyDifferent = false;
        if (!key.equals(getFullKey())) {
            keyDifferent = true;
        }
        setFullKey(key);

        if (!force && !keyDifferent) {
            return;
        }

        myVolume = volume;

        // REGENERATE the slice....
        // With texture, I think we can use it in the 3D window as well as the 2D,
        // just have to generate the binding points for each...
        if (myGLListener != null) {
            myGLListener.setData(llhArea, volume, aList);
            myGLListener.setTitle(titleKey);
            if (mySlider != null) {
                myGLListener.setTopMeters(mySlider.getValue());
                myGLListener.setXYRotation(myXSlider.getValue(), myYSlider.getValue());
            }
            if (myB3 != null) {
                myGLListener.drawBox = myB1.isSelected();
                myGLListener.drawCube = !myB2.isSelected();
                myGLListener.drawSlices = myB2.isSelected();
                myGLListener.drawClip = myB3.isSelected();
            }
            myGLListener.updateBufferForTexture();
            repaintChart();
        }
    }

    @Override
    public void setUpControls(Container parent) {
        JSlider j = new JSlider(0, 20000, 2000);
        j.setPaintLabels(true);
        j.setOrientation(JSlider.VERTICAL);
        ((Container) parent).add(j, java.awt.BorderLayout.EAST);
        j.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateChart(true);
            }
        });
        mySlider = j;

        j = new JSlider(0, 360, 0);
        j.setPaintLabels(true);
        j.setOrientation(JSlider.VERTICAL);
        ((Container) parent).add(j, java.awt.BorderLayout.WEST);
        j.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateChart(true);
            }
        });

        myXSlider = j;

        j = new JSlider(0, 360, 0);
        j.setPaintLabels(true);
        j.setOrientation(JSlider.HORIZONTAL);
        ((Container) parent).add(j, java.awt.BorderLayout.SOUTH);
        j.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateChart(true);
            }
        });

        myYSlider = j;
    }

    /**
     * Get extra menu items for the chart
     */
    public void addCustomTitleBarComponents(List<Object> addTo) {
        Icon i = SwingIconFactory.getIconByName("stock-tool-move-16.png");
        JideToggleButton b = new JideToggleButton(i);
        b.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateChart(true);
            }
        });
        myB1 = b;
        addTo.add(b);
        b = new JideToggleButton(i);
        b.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateChart(true);
            }
        });
        myB2 = b;

        addTo.add(b);
        b = new JideToggleButton(i);
        b.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateChart(true);
            }
        });
        myB3 = b;

        addTo.add(b);

    }
}
