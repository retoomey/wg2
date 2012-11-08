package org.wdssii.datatypes.builders.netcdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.datatypes.PPIRadialSet.PPIRadialSetMemento;
import org.wdssii.datatypes.Radial;
import org.wdssii.datatypes.builders.NetcdfBuilder;
import org.wdssii.datatypes.builders.NetcdfBuilder.NetcdfFileInfo;
import org.wdssii.geom.CPoint;
import org.wdssii.geom.CVector;
import org.wdssii.storage.Array1Dfloat;
import org.wdssii.storage.Array2Dfloat;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Create a RadialSet from a Netcdf file
 * 
 * This is the original RadialSet which handles PPI, or 
 * Plan Position Indicator radial sets, which have fixed elevation and
 * rotating azimuth.
 * 
 * @author Robert Toomey
 */
public class PPIRadialSetNetcdf extends DataTypeNetcdf {

    /** The log for errors */
    private static Logger log = LoggerFactory.getLogger(PPIRadialSetNetcdf.class);

    /** Try to create a RadialSet by reflection.  This is called from NetcdfBuilder by reflection	
     * @param ncfile	the Netcdf file to read from
     * @param sparse 	did we come from a "SparseRadialSet"?
     */
    @Override
    public DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse) {

        PPIRadialSetMemento m = new PPIRadialSetMemento();
        m.datametric = PPIRadialSet.createDataMetric();
        this.fillFromNetcdf(m, ncfile, sparse);
        return new PPIRadialSet(m);
    }

    @Override
    public void fillNetcdfFileInfo(NetcdfFile ncfile, NetcdfFileInfo info){
        super.fillNetcdfFileInfo(ncfile, info);
        try{
            float elev = ncfile.findGlobalAttribute("Elevation").getNumericValue().floatValue();
            info.Choice = Float.toString(elev);
        }catch(Exception e){
            info.Choice="Missing";
        }
    }
    
    /** Fill a memento from Netcdf data. */
    @Override
    public void fillFromNetcdf(DataTypeMemento m, NetcdfFile ncfile, boolean sparse) {

        /** Let super fill in the defaults */
        super.fillFromNetcdf(m, ncfile, sparse);

        if (m instanceof PPIRadialSetMemento) {
            PPIRadialSetMemento r = (PPIRadialSetMemento) (m);
            try {
                
                Variable v_az = ncfile.findVariable("Azimuth");
                Variable v_bw = ncfile.findVariable("BeamWidth");
                Variable v_as = ncfile.findVariable("AzimuthalSpacing");
                Variable v_gw = ncfile.findVariable("GateWidth");
                Variable v_ny = ncfile.findVariable("NyquistVelocity");
                float elev = ncfile.findGlobalAttribute("Elevation").getNumericValue().floatValue();
                float distToFirstGate = ncfile.findGlobalAttribute("RangeToFirstGate").getNumericValue().floatValue();
                float nyquist = DataType.MissingData;

                if (r.attriNameToValue != null) {
                    if (r.attriNameToValue.containsKey("Nyquist_Vel")) {
                        nyquist = Float.parseFloat(r.attriNameToValue.get("Nyquist_Vel"));
                    }
                }
                Array az_values = v_az.read();
                Array bw_values = v_bw.read();
                Array gw_values = v_gw.read();

                // optional
                Array as_values = null;
                if (v_as != null) {
                    as_values = v_as.read();
                }
                Array ny_values = null;
                if (v_ny != null) {
                    ny_values = v_ny.read();
                }

                // Valid for all info but the radials
                r.fixedAngleDegs = elev;
                r.rangeToFirstGate = distToFirstGate / 1000;
                // set up the co-ordinate system
                r.radarLocation = r.originLocation.getCPoint();
                r.myUz = r.radarLocation.minus(new CPoint(0, 0, 0)).unit();
                r.myUx = new CVector(0, 0, 1).crossProduct(r.myUz).unit();
                r.myUy = r.myUz.crossProduct(r.myUx);

                Array2Dfloat values = null;
                int num_radials = 0;
                try {
                    values = sparse ? NetcdfBuilder.readSparseArray2Dfloat(ncfile, r.typeName, r.datametric)
                            : NetcdfBuilder.readArray2Dfloat(ncfile, r.typeName, r.datametric);
                    num_radials = values.getX();
                } catch (Exception e) {
                    // If we can't get the values for any reason, 
                    // just make a zero size radial set (nice recovery)
                    log.warn("Couldn't create radials of radial set, leaving as empty");
                }

                r.radials = new Radial[num_radials];
                for (int i = 0; i < num_radials; ++i) {
                    float az = az_values.getFloat(i);
                    float bw = bw_values.getFloat(i);
                    float as = (as_values == null) ? bw : as_values.getFloat(i);
                    float gw = gw_values.getFloat(i) / 1000; // meters to kms
                    float ny = (ny_values == null) ? nyquist : ny_values.getFloat(i);
                    
                    // This wraps around the column of the 2D array, _not_ a copy
                    Array1Dfloat col = values.getCol(i);
                    if (col != null) {
                        if (r.maxGateNumber < col.size()) {
                            r.maxGateNumber = col.size();
                        }
                    }
                    r.radials[i] = new Radial(az, bw, as, gw, ny, col, i);
                }
            } catch (Exception e) { // FIXME: what to do if anything?
                log.warn("Couldn't create radial set from netcdf file");
            }
        }
    }
}