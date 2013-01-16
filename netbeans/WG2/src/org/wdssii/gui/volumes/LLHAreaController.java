package org.wdssii.gui.volumes;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.worldwind.LLHAreaLayer;

/**
 * This handles the mouse/key events for an LLHAreaLayer. There is only one of
 * these for all of our LLHAreas This is probably going to become the general
 * state listener for all the 'modes' of a 3d world window...
 *
 * @author Robert Toomey
 */
public class LLHAreaController implements KeyListener, MouseListener, MouseMotionListener {

    private static Logger log = LoggerFactory.getLogger(LLHAreaController.class);
    private ActionMode activeAction = ActionMode.NO_ACTION;
    /**
     * The WorldWindow we have a layer in
     */
    private final WorldWindow wwd;
    /**
     * The volume drawing layer we are linked to
     */
    private final LLHAreaLayer volumeLayer;
    // Current selection and device state.
    private Point mousePoint;
    private LLHAreaFeature activeLLHArea;
    private LLHAreaControlPoint activeControlPoint;
    private DFAMode myDFAState = DFAMode.NO_ACTION;
    private Position origin;
    private ArrayList<LatLon> originList;
    private boolean consumeMouseClick;

    // Current action we are doing with mouse
    public enum ActionMode {

        NO_ACTION(),
        MOVE_AIRSPACE_LATERALLY(Cursor.MOVE_CURSOR),
        MOVE_AIRSPACE_VERTICALLY(Cursor.N_RESIZE_CURSOR),
        RESIZE_AIRSPACE(Cursor.N_RESIZE_CURSOR),
        ADD_CONTROL_POINT(Cursor.CROSSHAIR_CURSOR),
        REMOVE_CONTROL_POINT(Cursor.CROSSHAIR_CURSOR),
        MOVE_CONTROL_POINT(Cursor.HAND_CURSOR);
        private Cursor actionCursor;

        private ActionMode() {
            actionCursor = null;
        }

        private ActionMode(int c) {
            actionCursor = Cursor.getPredefinedCursor(c);
        }
    }

    // Really this should belong to the earth ball itself I think... FIXME
    public enum ToolbarMode {

        INACTIVE, // Ignore events completely

        NORMAL, // Selection and drag when over control points
        ADD_REMOVE         // Add with click, remove with shift-click
    }

    // The state of the DFA...where we are at (nodes)
    public enum DFAMode {

        NO_ACTION, // The 'ready' state, or stopped
        DRAGGING_CONTROL_POINT, // Dragging a control point
        DRAGGING_ENTIRE_SET, // Dragging everything
        DRAGGING_NOTHING // Dragging nothing but we keep mouse
    }

    // The action of the DFA...where to go (arrows)
    public enum DFAAction {

        START_DRAGGING,
        CREATE_CONTROL_POINT, // START_DRAGGING
        DELETE_CONTROL_POINT,
        DRAGGING,
        STOP_DRAGGING
    }
    private ToolbarMode myMode = ToolbarMode.NORMAL;

    public LLHAreaController(WorldWindow w, LLHAreaLayer volume) {
        this.wwd = w;
        this.volumeLayer = volume;
        //this.setupActionCursorMap();
        InputHandler h = this.wwd.getInputHandler();
        setupListeners();
    }

    public final void setupListeners() {
        if (this.wwd != null) {
            InputHandler h = this.wwd.getInputHandler();
            h.addKeyListener(this);
            h.addMouseListener(this);
            h.addMouseMotionListener(this);
        }
    }

    public final void removeListeners() {
        if (this.wwd != null) {
            InputHandler h = this.wwd.getInputHandler();
            h.removeKeyListener(this);
            h.removeMouseListener(this);
            h.removeMouseMotionListener(this);
        }
    }

    /**
     * Get the action we are currently doing...
     */
    public ActionMode getActiveAction() {
        return activeAction;
    }

    /**
     * Set the action we are currently doing...
     */
    protected void setActiveAction(ActionMode action) {
        this.activeAction = action;
    }

    public LLHAreaLayer getLLHAreaLayer() {
        return this.volumeLayer;
    }

    public WorldWindow getWorldWindow() {
        return this.wwd;
    }

    protected Point getMousePoint() {
        return this.mousePoint;
    }

    protected void setMousePoint(Point point) {
        this.mousePoint = point;
    }

    protected LLHAreaControlPoint getActiveControlPoint() {
        return this.activeControlPoint;
    }

    protected void setActiveControlPoint(LLHAreaControlPoint controlPoint) {
        this.activeControlPoint = controlPoint;
    }

    protected LLHAreaFeature getActiveAirspace() {
        return activeLLHArea;
    }

    protected void setActiveAirspace(LLHAreaFeature airspace) {
        this.activeLLHArea = airspace;
    }

    protected void setOrigin(Position p, ArrayList<LatLon> l) {
        this.origin = p;
        this.originList = l;
    }

    protected Position getOrigin() {
        return this.origin;
    }

    protected ArrayList<LatLon> getOriginList() {
        return this.originList;
    }

    /**
     * Picked object as an LLHArea
     */
    protected LLHAreaFeature asLLHAreaFeature(Object obj) {
        if (!(obj instanceof LLHArea)) {
            return null;
        }

        // Bleh! hack this for moment
        List<Feature> fl = FeatureList.theFeatures.getFeatures();
        Iterator<Feature> iter = fl.iterator();
        while (iter.hasNext()) {
            Feature f = iter.next();
            if (f instanceof LLHAreaFeature) {
                LLHAreaFeature a = (LLHAreaFeature) (f);
                LLHArea area = a.getLLHArea();
                if (area == obj) {
                    return a;
                }
            }
        }
        return null;
    }

    /**
     * Picked object as a LLHAreaControlPoint
     */
    protected LLHAreaControlPoint asLLHAreaControlPoint(Object obj) {
        if (!(obj instanceof LLHAreaControlPoint)) {
            return null;
        }
        return (LLHAreaControlPoint) obj;
    }

    private static class pickedInfo {

        LLHAreaFeature pickedArea = null;
        LLHAreaControlPoint pickedPoint = null;
    }

    private pickedInfo getTopPickedObject() {
        pickedInfo i = new pickedInfo();

        PickedObjectList pickedObjects = this.getWorldWindow().getObjectsAtCurrentPosition();
        if (pickedObjects != null) {
            for (PickedObject p : pickedObjects) {
                Object o = p.getObject();
                // Get first LLHAreaFeature clicked...
                if (i.pickedArea == null) {
                    LLHAreaFeature l = asLLHAreaFeature(o);
                    if (l != null) {
                        i.pickedArea = l;
                    }
                }
                // Get first control point clicked
                if (i.pickedPoint == null) {
                    LLHAreaControlPoint point = asLLHAreaControlPoint(o);
                    if (point != null) {
                        i.pickedPoint = point;
                    }
                }
            }
        }
        return i;
    }

    //**************************************************************//
    //********************  Key Events  ****************************//
    //**************************************************************//
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        log.debug("LLH key pressed");
        if (e == null) {
            return;
        }

//		this.updateCursor(e);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        log.debug("LLH key released");
        if (e == null) {
            return;
        }

//		this.updateCursor(e);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    //**************************************************************//
    //********************  Mouse Events  **************************//
    //**************************************************************//
    /**
     * Linux window managers steal cntl and alt keys now. 3D window manager
     * takes the alt click to move entire window...so we switch to a mouse mode
     */
    public void setMouseMode(ToolbarMode aMode) {
        myMode = aMode;
    }

    public ToolbarMode getMouseMode() {
        return myMode;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        log.debug("Mouse clicked LLH");
        if (consumeMouseClick) {
            e.consume();
            consumeMouseClick = false;
        }
        // processDFA(e.getPoint(), DFAAction.MOUSE_CLICKED);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        log.debug("Mouse pressed LLH");
        if (e == null) {
            return;
        }
        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            return;
        }

        // Really want add_remove, button to be one piece of code including cursor...
        // FIXME:  How to do that?
        boolean b1 = (e.getButton() == MouseEvent.BUTTON1);
        boolean shift = e.isShiftDown();

        if (myMode == ToolbarMode.ADD_REMOVE) {
            if (b1) {
                if (shift) {
                    processDFA(e.getPoint(), DFAAction.DELETE_CONTROL_POINT);
                } else {
                    processDFA(e.getPoint(), DFAAction.CREATE_CONTROL_POINT);
                    // DFA currently switching to drag automatically
                }
                e.consume(); // Don't pass on mouse down to others
            }
        } else if (myMode == ToolbarMode.NORMAL) {
            if (b1) {
                processDFA(e.getPoint(), DFAAction.START_DRAGGING);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        log.debug("Mouse released LLH");
        if (e == null) {
            return;
        }

        boolean b1 = (e.getButton() == MouseEvent.BUTTON1);
        if (b1) {
            processDFA(e.getPoint(), DFAAction.STOP_DRAGGING);
            e.consume(); // Don't pass on mouse down to others
        }

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (e == null) {
            return;
        }

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (e == null) {
            return;
        }

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
        }
    }

    //**************************************************************//
    //********************  Mouse Motion Events  *******************//
    //**************************************************************//
    @Override
    public void mouseDragged(MouseEvent e) {
        log.debug("LLH AREA Mouse DRAGGED ");
        if (e == null) {
            return;
        }

        //  this.updateCursor(e, MouseAction.MOUSE_DRAGGED);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            return;
        }

        processDFA(e.getPoint(), DFAAction.DRAGGING);
        // Anything but NO_ACTION state on drag is consumed...
        if (myDFAState != DFAMode.NO_ACTION) {
            log.debug("Mouse drag consume.. " + myDFAState);
            e.consume();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        log.debug("LLH AREA Mouse moved " + myDFAState);
        if (e == null) {
            return;
        }

        this.setMousePoint(new Point(e.getPoint())); // copy to insulate us from changes by the caller
        //  this.updateCursor(e, MouseAction.MOUSE_MOVED);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    protected ActionMode updateCursor(InputEvent e, DFAAction m) {
        // Include this test to ensure any derived implementation performs it.
        if (e == null || e.getComponent() == null) {
            return ActionMode.NO_ACTION;
        }

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            return ActionMode.NO_ACTION;
        }

        Cursor cursor;
        ActionMode action = getPotentialActionFor(e, m);

        // The cursor part
        cursor = action.actionCursor;
        e.getComponent().setCursor(cursor);
        e.getComponent().repaint();

        return action;
    }

    /**
     * Get the action we would do for a given event...
     */
    protected ActionMode getPotentialActionFor(InputEvent e, DFAAction m) {
        pickedInfo i = getTopPickedObject();
        LLHAreaFeature area = i.pickedArea;
        LLHAreaControlPoint topControlPoint = i.pickedPoint;

        log.debug("TOP POINT AND AREA " + topControlPoint + ", " + area);
        switch (myMode) {
            case INACTIVE:
            default:
                return null;

            case ADD_REMOVE:
                // If no point under us, we'll add a point
                if (topControlPoint == null) {
                    return ActionMode.ADD_CONTROL_POINT;
                } else {
                    // Point under us with shift, delete a point...
                    if (e.isShiftDown()) {
                        return ActionMode.REMOVE_CONTROL_POINT;
                    } else {
                        // Move this point....
                        return ActionMode.MOVE_CONTROL_POINT;
                    }
                }
            // Do nothing on mouse up or drag 

            case NORMAL:
                if (e.isShiftDown()) {
                    if (topControlPoint != null) {
                        return ActionMode.RESIZE_AIRSPACE;
                    } else if (area != null) {
                        return ActionMode.MOVE_AIRSPACE_VERTICALLY;
                    }
                } else { // shift not down...
                    if (topControlPoint != null) {
                        return ActionMode.MOVE_CONTROL_POINT;
                    } else if (area != null) {
                        return ActionMode.MOVE_AIRSPACE_LATERALLY;
                    }

                }
        }
        return ActionMode.NO_ACTION;
    }

    private static class DFAInfo {

        LLHAreaFeature area;

        DFAInfo(MouseEvent e, DFAAction m) {
        }
    }

    /**
     * Handle a mouse event
     */
    protected void processDFA(Point e, DFAAction m) {
        DFAState d = new DFAState(this, e, m);
        processDFA(d);
    }

    public static class DFAState {

        public DFAState(LLHAreaController c, Point at2, DFAAction m2) {
            pickedInfo i = c.getTopPickedObject();
            pickedArea = i.pickedArea;
            pickedPoint = i.pickedPoint;
            activePoint = c.getActiveControlPoint();
            activeArea = c.getActiveAirspace();
            at = at2;
            m = m2;

            // Keep last mouse point and next always...hummm
            lastMousePoint = c.getMousePoint();
            c.setMousePoint(new Point(at2)); // copy to insulate us from changes by the caller
            //shift = e.isShiftDown();
            // b1 = (e.getButton() == MouseEvent.BUTTON1);
            consume = false;

            l = c.getLLHAreaLayer();
            w = c.getWorldWindow();
        }
        Point at;
        DFAAction m;
        pickedInfo i;
        LLHAreaFeature pickedArea;
        LLHAreaControlPoint pickedPoint;
        LLHAreaControlPoint activePoint;
        LLHAreaFeature activeArea;
        Point lastMousePoint;
        boolean shift;
        boolean b1;
        boolean consume;
        LLHAreaLayer l;
        WorldWindow w;
    }

    /**
     * From DFA state, create a new control point and make it active
     */
    protected void createNewPoint(DFAState d) {
        log.debug("Create point called");
        // Don't add on top of an existing point...
        if (d.pickedPoint == null) {
            d.pickedPoint = d.l.addControlPoint(d.w, d.l.getAirspace(), d.at);
        }
        d.w.redraw();

    }

    protected void deletePoint(DFAState d) {
        if (d.pickedPoint != null) {
            d.l.doRemoveControlPoint(d.pickedPoint);
        }
        d.w.redraw();
    }

    /**
     * Handle all mouse action with a DFA state machine
     */
    protected void processDFA(DFAState d) {

        switch (myDFAState) {
            case NO_ACTION:
                log.debug("STATE NO_ACTION");
                if (d.m == DFAAction.CREATE_CONTROL_POINT) {
                    LLHAreaControlPoint selected;
                    if (d.pickedPoint == null) {
                        selected = d.l.addControlPoint(d.w, d.l.getAirspace(), d.at);
                        consumeMouseClick = true;
                        setActiveControlPoint(selected);
                    } else {
                        selected = d.pickedPoint;
                    }
                    setActiveControlPoint(selected);
                    myDFAState = DFAMode.DRAGGING_CONTROL_POINT;
                } else if (d.m == DFAAction.DELETE_CONTROL_POINT) {
                    d.l.doRemoveControlPoint(d.pickedPoint);
                    consumeMouseClick = true;
                    setActiveControlPoint(null);
                    myDFAState = DFAMode.DRAGGING_NOTHING; //wait for 'up'
                } else if (d.m == DFAAction.START_DRAGGING) {

                    // Look for control point first, then area....
                    if (d.pickedPoint != null) {
                        setActiveControlPoint(d.pickedPoint);
                        d.activePoint = d.pickedPoint;
                        myDFAState = DFAMode.DRAGGING_CONTROL_POINT;
                    } else if (d.pickedArea != null) {
                        setActiveAirspace(d.pickedArea);
                        setOrigin(d.l.airspaceOrigin(d.w, d.l.getAirspace(), d.at),
                                d.l.originList(d.w, d.l.getAirspace(), d.at));

                        myDFAState = DFAMode.DRAGGING_ENTIRE_SET;
                    }
                }
                break;
            case DRAGGING_NOTHING:
                if (d.m == DFAAction.STOP_DRAGGING) {
                    log.debug("Drag nothing to no action");
                    myDFAState = DFAMode.NO_ACTION;
                    setOrigin(null, null); // free memory
                    log.debug("CONSUME SET TO TRUE");
                    consumeMouseClick = true;  // After a drag, ignore the mouseclicked
                }
                break;
            case DRAGGING_CONTROL_POINT:
                log.debug("STATE DRAGGIN_CONTROL_POINT");

                if (d.m == DFAAction.STOP_DRAGGING) {
                    myDFAState = DFAMode.NO_ACTION;
                } else if (d.m == DFAAction.DRAGGING) {
                    d.l.doMoveControlPoint(d.w, d.activePoint, d.at);
                }
                break;
            case DRAGGING_ENTIRE_SET:
                log.debug("STATE DRAGGING ENTIRE_SET");

                if (d.m == DFAAction.STOP_DRAGGING) {
                    myDFAState = DFAMode.NO_ACTION;
                } else if (d.m == DFAAction.DRAGGING) {
                    // d.l.doMoveControlPoint(d.w, d.activePoint, d.at);
                    if (d.activeArea != null) {
                        d.l.doMoveAirspaceLaterally(d.w, d.activeArea.getLLHArea(), getOriginList(), getOrigin(), d.at);
                    }

                }
                break;
        }
    }
}
