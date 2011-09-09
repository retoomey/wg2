package org.wdssii.geom;

/**
 * @author lakshman
 * 
 */
public class Location {

    private double lat;
    private double lon;
    private double ht;
    /** in kilometers. */
    public static final double EarthRadius = 6380;

    /** lat, lon in degrees and height in kilometers. */
    public Location(double lat, double lon, double ht) {
        init(lat, lon, ht);
    }

    public void init(double lat, double lon, double ht) {
        this.lat = lat;
        this.lon = lon;
        this.ht = ht;

        // Contours has location objects with height 0
        //if (lat < -90 || lat > 90 || lon < -180 || lon > 180 || ht < -0.01)
       // if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
       //     throw new IndexOutOfBoundsException("Invalid earth location" + this);
       // }

    }

    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lon;
    }

    public double getHeightKms() {
        return ht;
    }

    @Override
    public String toString() {
        return "[ " + lat + ", " + lon + ", " + ht + "]";
    }

    public CPoint getCPoint() {
        double r = EarthRadius + ht; // kms
        double phi = lon * Math.PI / 180.0; // radians();
        double beta = lat * Math.PI / 180.0; // .radians();
        double x = r * Math.cos(phi) * Math.cos(beta);
        double y = r * Math.sin(phi) * Math.cos(beta);
        double z = r * Math.sin(beta);

        return new CPoint(x, y, z);
    }

    public boolean equals(Location l) {
        return ((lat == l.getLatitude()) && (lon == l.getLongitude())
                && (ht == l.getHeightKms()));
    }
}
