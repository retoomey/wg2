package org.wdssii.gui.worldwind;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.Position;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.products.readouts.ProductReadout;

/**
 * @author Robert Toomey
 * 
 * The status overlay for our window.
 * Copied and modified from the original worldwind status bar
 * 
 */
public class ReadoutStatusBar extends JPanel implements PositionListener,
        RenderingListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    // Units constants
    public final static String UNIT_METRIC = "gov.nasa.worldwind.StatusBar.Metric";
    public final static String UNIT_IMPERIAL = "gov.nasa.worldwind.StatusBar.Imperial";
    private final static double METER_TO_FEET = 3.280839895;
    private final static double METER_TO_MILE = 0.000621371192;
    private static final int MAX_ALPHA = 254;
    private WorldWindow eventSource;
    //protected final JLabel latDisplay = new JLabel("");
    protected final JLabel locDisplay = new JLabel("");
   // protected final JLabel lonDisplay = new JLabel("Off globe");
    protected final JLabel altDisplay = new JLabel("");
  //  protected final JLabel eleDisplay = new JLabel("");
    protected final JLabel dataDisplay = new JLabel("");
    private boolean showNetworkStatus = true;
    private String elevationUnit = UNIT_METRIC;
    private int myLastX = 0;
    private int myLastY = 0;
    private ProductReadout myProductReadout = null;
    public Position lastPosition = null;

    public ReadoutStatusBar() {
       // super(new GridLayout(1, 0));
        super(new MigLayout(new LC().fill().insetsAll("0"), null, null));

        this.setBackground(Color.BLACK);

        final JLabel heartBeat = new JLabel("Downloading");

        altDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        locDisplay.setHorizontalAlignment(SwingConstants.LEFT);
        dataDisplay.setHorizontalAlignment(SwingConstants.CENTER);
       // eleDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        
        this.add(locDisplay);
        this.add(altDisplay);
        this.add(dataDisplay);
      //  this.add(eleDisplay);
        this.add(heartBeat);

        heartBeat.setHorizontalAlignment(SwingConstants.CENTER);
        heartBeat.setForeground(new java.awt.Color(255, 0, 0, 0));

        Timer downloadTimer = new Timer(100, new ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                if (!showNetworkStatus) {
                    if (heartBeat.getText().length() > 0) {
                        heartBeat.setText("");
                    }
                    return;
                }

                if (WorldWind.getNetworkStatus().isNetworkUnavailable()) {
                    heartBeat.setText("No Network");
                    heartBeat.setForeground(new java.awt.Color(255, 0, 0, 255));
                    return;
                }

                java.awt.Color color = heartBeat.getForeground();
                int alpha = color.getAlpha();
                if (WorldWind.getRetrievalService().hasActiveTasks()) {
                    heartBeat.setText("Downloading");
                    if (alpha >= MAX_ALPHA) {
                        alpha = MAX_ALPHA;
                    } else {
                        alpha = alpha < 16 ? 16 : Math.min(MAX_ALPHA,
                                alpha + 20);
                    }
                } else {
                    alpha = Math.max(0, alpha - 20);
                }
                heartBeat.setForeground(new java.awt.Color(255, 0, 0, alpha));
            }
        });
        downloadTimer.start();
    }

    public void setEventSource(WorldWindow newEventSource) {
        if (this.eventSource != null) {
            this.eventSource.removePositionListener(this);
            this.eventSource.removeRenderingListener(this);
        }

        if (newEventSource != null) {
            newEventSource.addPositionListener(this);
            newEventSource.addRenderingListener(this);
        }

        this.eventSource = newEventSource;
    }

    public boolean isShowNetworkStatus() {
        return showNetworkStatus;
    }

    public void setShowNetworkStatus(boolean showNetworkStatus) {
        this.showNetworkStatus = showNetworkStatus;
    }

    @Override
    public void moved(PositionEvent event) {
        this.handleCursorPositionChange(event);
    }

    public WorldWindow getEventSource() {
        return this.eventSource;
    }

    public String getElevationUnit() {
        return this.elevationUnit;
    }

    public void setElevationUnit(String unit) {
        if (unit == null) {
            // String message = Logging.getMessage("nullValue.StringIsNull");
            // Logging.logger().severe(message);
            // throw new IllegalArgumentException(message);
        }

        this.elevationUnit = unit;
    }

    /*
    protected String makeCursorElevationDescription(double metersElevation) {
        String s;
        if (UNIT_IMPERIAL.equals(elevationUnit)) {
            s = String.format("(TElev %,7d feet)",
                    (int) (metersElevation * METER_TO_FEET));
        } else // Default to metric units.
        {
            s = String.format("(TElev %,7d meters)", (int) metersElevation);
        }
        return s;
    }
*/
    protected String makeEyeAltitudeDescription(double metersAltitude) {
        String s;
        if (UNIT_IMPERIAL.equals(elevationUnit)) {
            s = String.format("(H %,7d mi)", (int) Math.round(metersAltitude * METER_TO_MILE));
        } else // Default to metric units.
        {
            s = String.format("(H %,7d km)", (int) Math.round(metersAltitude / 1e3));
        }
        return s;
    }

    public int getLastX() {
        return myLastX;
    }

    public int getLastY() {
        return myLastY;
    }

    private void handleCursorPositionChange(PositionEvent event) {
        Position newPos = event.getPosition();
        if (newPos != null) {
            lastPosition = newPos;
            String loc = String.format("(%7.4f\u00B0,%7.4f\u00B0,%,7.4f Meters)", newPos.getLatitude().getDegrees(),
                    newPos.getLongitude().getDegrees(), newPos.getAltitude());
            //	String las = String.format("Lat %7.4f\u00B0", newPos.getLatitude()
            //			.getDegrees());
            //	String los = String.format("Lon %7.4f\u00B0", newPos.getLongitude()
            //			.getDegrees());

           // String els = makeCursorElevationDescription(eventSource.getModel().getGlobe().getElevation(newPos.getLatitude(),
           //         newPos.getLongitude()));
            locDisplay.setText(loc);
            // FIXME: should probably go to EarthView
            Point p = event.getScreenPoint();
            myLastX = p.x;
            myLastY = p.y;
            if (myProductReadout != null){
                dataDisplay.setText(myProductReadout.getReadoutString());
            }
           // eleDisplay.setText(els);
        } else {
            locDisplay.setText("");
            dataDisplay.setText("");
            //eleDisplay.setText("");
        }
    }

    @Override
    public void stageChanged(RenderingEvent event) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (eventSource.getView() != null
                        && eventSource.getView().getEyePosition() != null) {
                    altDisplay.setText(makeEyeAltitudeDescription(eventSource.getView().getEyePosition().getElevation()));
                } else {
                    altDisplay.setText("Altitude");
                }
            }
        });   
    }

    public void setProductReadout(ProductReadout pr) {       
        myProductReadout = pr;
    }
}
