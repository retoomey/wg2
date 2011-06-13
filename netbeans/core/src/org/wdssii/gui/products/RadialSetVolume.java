package org.wdssii.gui.products;

import java.util.ArrayList;
import java.util.Iterator;

import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.RadialSet;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.products.filters.DataFilter;
import org.wdssii.gui.products.filters.DataFilter.DataValueRecord;

/** A ProductVolume consisting entirely of RadialSetProducts
 * 
 * @author Robert Toomey
 *
 */
public class RadialSetVolume extends ProductVolume {

    // Synchronize access to these...
    private Object myRadialLock = new Object();
    /** The set of radials */
    private ArrayList<Product> myRadials = new ArrayList<Product>();

    /** Ok, looks like this will be called every time a product is requested???
     * Gonna have to do some sync work I'm thinking...
     * Chart and VSlice may call this at same time...
     */
    @Override
    public void initVirtual(Product init, boolean virtual) {

        // Create an array list of products and sort them...
        // This list can change over time due to autoupdates.
        // If these are NEW products, they will start background thread loading.
        StringBuilder newKey = new StringBuilder("");
        ArrayList<Product> newRadials = new ArrayList<Product>();
        Product first = init;
        ArrayList<Product> p = first.loadVolumeProducts(virtual);
        Iterator<Product> iter = p.iterator();
        while (iter.hasNext()) {
            Product product = iter.next();
            // Only add products that are loaded to the volume....
            if (product.getRawDataType() != null) {
                newRadials.add(product);
                newKey.append(product.getCacheKey());
            }
        }
        first.sortVolumeProducts(newRadials);

        // Need to lock changing out products for new ones.  This is because
        // Someone might be calling getValueAt below
        synchronized (myRadialLock) {
            myKey = newKey.toString();
            myRadials = newRadials;
            //System.out.println("Init volume called... we have "+myRadials.size()+" products ready ");
            //myRecords = newRecords;
        }
    }

    /** Generate a key that uniquely determines this volume based on product data */
    @Override
    public String getKey() {
        synchronized (myRadialLock) {
            return myKey;
        }
    }

    /** Prepare filters for a batch value grab. */
    public void prepFilters(ArrayList<DataFilter> list) {
        if (list != null) {
            for (DataFilter d : list) {
                d.prepFilterForVolume(this);
            }
        }
    }
    public static boolean myExperiment = false;

    /** Get filtered value of from a volume location, store into ColorMapOutput.
     * This function needs to be callable by multiple threads at once, example: 3D vslice render by GL thread, 2D table render by VSliceChart.
     * So synchronize if you 'share' any memory here
     */
    @Override
    public boolean getValueAt(double lat, double lon, double heightM, ColorMapOutput output, DataValueRecord out,
            FilterList list, boolean useFilters) {

        // Maybe this could be a filter in the color map...  You could clip anything by height heh heh..
        // It would make sense for it to be a filter
        if (heightM < 0) {
            output.setColor(255, 255, 255, 255);
            //output.red = output.green = output.blue = output.alpha = 255;
            output.filteredValue = 0.0f;
            return false;
        }

        output.location.init(lat, lon, heightM / 1000.0);

        // Smooth in the vertical direction....?? how
        // We would need a weight based on range
        float value = DataType.MissingData;
        float weightAtValue;

        RadialSet.RadialSetQuery q = new RadialSet.RadialSetQuery();
        q.inLocation = output.location;
        // Testing interpolation ability (alpha experiment)
        // This tells query to return a distance from closest point...
        // for each of the Lat, Lon, Height 'axis'
        if (myExperiment) {
            q.inNeedInterpolationWeight = true;
        }

        q.outDataValue = DataType.MissingData;

        RadialSet.SphericalLocation buffer = new RadialSet.SphericalLocation();

        // Poor man's vslice..just grab the first thing NOT missing lol...
        // This is actually slowest when there isn't any data...
        // Notice with 'overlap' the first radial dominates without any smoothing...
        // FIXME: could binary search the radial volume I think...
        int radialSetIndex = 0;
        //Iterator<RadialSetProduct> iter = myRadials.iterator();

        // Make sure the reading of data values is sync locked with updating in initProduct...
        synchronized (myRadialLock) {
            Iterator<Product> iter = myRadials.iterator();
            while (iter.hasNext()) {

                // For each radial in the radial set....
                //RadialSet r = iter.next().getRadialSet();
                DataType dt = iter.next().getRawDataType();
                RadialSet r = null;
                if (dt != null) {
                    r = (RadialSet) (dt);
                }
                if (r != null) {
                    // First time, get the location in object spherical coordinates.  This doesn't
                    // change for any of the radials in the set.
                    if (radialSetIndex == 0) {
                        r.locationToSphere(output.location, buffer);
                        q.inSphere = buffer;
                    }
                    r.queryData(q);

                    if (myExperiment) {
                        value = DataType.MissingData;
                        weightAtValue = q.outDistanceHeight;

                        // Interpolate in true height...
                        if (true) {
                            // So it's a 'hit' if within .5 kilometer height of beam.
                            if (Math.abs(weightAtValue) < .5){
                                value = q.outDataValue;
                                break;
                                /*
                                
                                // We're above a radial..now we can height
                                // interpolate with one right above....bleh...
                                if (iter.hasNext()){
                                DataType dt2 = iter.next().getRawDataType();
                                RadialSet r2 = null;
                                if (dt2 != null) {
                                r2 = (RadialSet) (dt2);
                                if (r2 != null) {
                                r.queryData(q);
                                float v2= q.outDataValue;
                                float w2 = q.outDistanceHeight;
                                if (w2 < 0){
                                // woo hooo!
                                value = 100;
                                break;
                                }
                                }
                                }
                                }
                                 * 
                                 */
                            }// else {
                            // value = DataType.MissingData;
                            //   break;
                            //break;
                            //  break;
                            // Continue until over a first radial.
                            // }
                        } else {
                            // Break on first 'hit' when not interpolating...
                            if (DataType.isRealDataValue(value)) {

                                // Ok, check 'next' radial for a hit..this means beamwidths are overlapping..
                                //if (iter.hasNext()){
                                //float value2 = iter.next().getRadialSet().getValueAtLocation(locationBuffer);
                                //}
                                break;
                            }
                        }
                    } else {
                        // Cheapest...first 'hit' gives us value
                        value = q.outDataValue;
                        if (DataType.isRealDataValue(value)) {
                            break;
                        }
                    }
                }
                radialSetIndex++;
            }
        }
        q.inDataValue = value;
        q.outDataValue = value;
        q.outRadialSetNumber = radialSetIndex;
        out.hWeight = q.outDistanceHeight;
        list.fillColor(output, q, useFilters);

        // Find a location value in our radial set collection...
        return true;
    }

    /** Get the Storm Relative Motion deltas for each product in our set. */
    public ArrayList<ArrayList<Float>> getSRMDeltas(float speed, float degrees) {

        ArrayList<ArrayList<Float>> list = new ArrayList<ArrayList<Float>>();;
        synchronized (myRadialLock) {
            for (Product r : myRadials) {
                DataType d = r.getRawDataType();
                if (d != null) {
                    if (d instanceof RadialSet) {
                        RadialSet radial = (RadialSet) (d);
                        ArrayList<Float> values = radial.createSRMDeltas(speed, degrees);
                        list.add(values);
                    }
                } else {
                    list.add(null); // filler
                }
            }
        }
        return list;
    }
}
