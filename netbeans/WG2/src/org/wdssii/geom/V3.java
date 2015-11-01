package org.wdssii.geom;

/**
 * Our generic point/vector object for the display.
 * 
 * Going to move to this to reduce some of the library coupling.
 * This type of object is created in pretty much every library out there.
 * 
 * Note not using templates because Float more memory than float.
 * 
 * @author Robert Toomey
 */
public class V3 {
    public final float x;
    public final float y;
    public final float z;
    
    public V3(float xi, float yi, float zi){
        x = xi;
        y = yi;
        z = zi;
    }
    
     public V3(double xi, double yi, double zi){
        x = (float)xi;
        y = (float)yi;
        z = (float)zi;
    }
     
     public V3 offset(float dx, float dy, float dz){
    	 return new V3(x+dx, y+dy, z+dz);
     }
     
     public V3 offset(double dx, double dy, double dz){
    	 return new V3(x+dx, y+dy, z+dz);
     }
}
