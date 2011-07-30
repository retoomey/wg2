package org.wdssii.datatypes.builders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.core.PrototypeFactory;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.DataType;
import org.wdssii.index.IndexRecord;

/**
 * @author lakshman
 * 
 * BuilderFactory maps a set of builder names to the objects that know how to
 * build them.  For example, if an IndexRecord refers to a netcdf file, we
 * pass the information to the netcdfbuilder to build the datatype.
 * 
 */
public abstract class BuilderFactory {

    private static Log log = LogFactory.getLog(BuilderFactory.class);
    /** name to Builder */
    private static final PrototypeFactory<Builder> factory;

    /** Create the factory from Builder.xml in the xml, OR use
     * a stock set of built in defaults.  This rarely changes, so this
     * allows overriding without breaking if w2config is missing.
     */
    static {
        factory = new PrototypeFactory<Builder>(
                "java/Builder.xml");
        factory.addDefault("netcdf", "org.wdssii.datatypes.builders.NetcdfBuilder");
        factory.addDefault("W2ALGS", "org.wdssii.datatypes.builders.W2algsBuilder");
        factory.addDefault("test", "org.wdssii.datatypes.builders.TestBuilder");

    }
    
    /** Get the builder for a given builder name */
    public static Builder getBuilder(String builderName) {
        Builder builder = factory.getPrototypeMaster(builderName);
        return builder;
    }

    /** The single thread do all the work call.  This blocks until DataType is completely loaded and ready.
     * This is what you want for algorithms probably.
     * @param rec
     * @return
     * @throws DataUnavailableException
     */
    public static DataType createDataType(IndexRecord rec)
            throws DataUnavailableException {

        String builderName = rec.getBuilderName();
        Builder builder = factory.getPrototypeMaster(builderName);
        if (builder == null) {
            log.error("ERROR: no such builder: " + builderName);
            return null;
        }
        return builder.createDataType(rec, null);
    }

    /** The background job version.  This doesn't block and returns a DataRequest which is a future
     * that holds a pointer to a future DataType.  See DataRequest for example 
     */
    public static DataRequest createDataRequest(IndexRecord rec) {
        String builderName = rec.getBuilderName();
        Builder builder = factory.getPrototypeMaster(builderName);
        if (builder == null) {
            log.error("ERROR: no such builder: " + builderName);
            return null;
        }
        return builder.createDataRequest(rec);
    }
}
