package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <contour>
 * (1) <datatype>
 * (1) <locationdata>
 *       <array length="n">
 *         (n) <location>
 * </contour>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_contour extends Tag {

    public final static String TAG = "contour";
    
    /** The <datatype> tag within us */
    public Tag_datatype datatype = new Tag_datatype();
   
    /** The <locationdata> tag within us. */
    public Tag_locationdata locationdata = new Tag_locationdata();
    
    @Override
    public String tag() {
        return TAG;
    }

    /** Process all child tabs within our tag */
    @Override
    public void processChildren(XMLStreamReader p) {
       datatype.processTag(p);
       locationdata.processTag(p);
    }
}