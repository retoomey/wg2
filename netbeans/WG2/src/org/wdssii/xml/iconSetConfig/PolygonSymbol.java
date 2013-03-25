package org.wdssii.xml.iconSetConfig;

import java.awt.Color;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.wdssii.xml.Util;

/**
 * Polygon symbol A solid polygon shape based on equal angle rotation. This can
 * create squares, diamonds, circles...
 *
 * @author Robert Toomey
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "polygon")
public class PolygonSymbol extends Symbol {

    /**
     * Base color of the symbol
     */
    @XmlJavaTypeAdapter(Util.ColorAdapter.class) // Use ColorAdapter
    @XmlAttribute(name = "color")
    public Color color = Color.BLUE;
    public boolean useOutline = true;
    /**
     * Outline color of the symbol
     */
    @XmlJavaTypeAdapter(Util.ColorAdapter.class) // Use ColorAdapter
    @XmlAttribute(name = "ocolor")
    public Color ocolor = Color.BLACK;
    public int osize = 2;
    
    public int numpoints = 4;

    /**
     * Set up as a square
     */
    public void toSquare() {
        phaseangle = 45; // rotate 45 degrees
        numpoints = 4;  // 4 points
    }

    /**
     * Set up as a diamond
     */
    public void toDiamond() {
        phaseangle = 0;
        numpoints = 4;  // 4 points
    }

    /**
     * Set up as a circle
     */
    public void toCircle() {
        phaseangle = 0;
        numpoints = 20;  // 20 points
    }
    
    /** Set up as a triangle
     * 
     */
    public void toTriangle(){
        phaseangle = 0;
        numpoints = 3;
    }
}