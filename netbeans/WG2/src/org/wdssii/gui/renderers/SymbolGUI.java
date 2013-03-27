package org.wdssii.gui.renderers;

import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.wdssii.gui.GUIPlugInPanel;
import org.wdssii.gui.properties.PropertyGUI;
import org.wdssii.properties.Memento;

/**
 * Root GUI for editing a Symbol
 * FIXME: Exact copy of FeatureGUI?
 * 
 * @author Robert Toomey
 */
public class SymbolGUI extends JPanel implements GUIPlugInPanel {

    /**
     * Stock PropertyGUI items to update
     */
    protected ArrayList<PropertyGUI> myStockGUIItems;
    /**
     * The root component, this is added/removed on the fly
     */
    protected JComponent myRoot = null;

    public void add(PropertyGUI g) {
        if (myStockGUIItems == null) {
            myStockGUIItems = new ArrayList<PropertyGUI>();
        }
        myStockGUIItems.add(g);
        layout(g);
    }

    /**
     * Layout a newly added propety gui item
     */
    public void layout(PropertyGUI g) {
        /* Default layout is mig */
        g.addToMigLayout(this);
    }

    /**
     * Called with proper memento to update all of our stock propertygui items
     */
    public void updateToMemento(Memento m) {
        if (myStockGUIItems != null) {
            for (PropertyGUI p : myStockGUIItems) {
                p.update(m);
            }
        }
    }

    /**
     * Set the root container component
     */
    public void setRootComponent(JComponent r) {
        myRoot = r;
    }

    @Override
    public void updateGUI() {
    }

    @Override
    public void activateGUI(JComponent parent) {
        if (myRoot != null) {
            parent.setLayout(new java.awt.BorderLayout());
            parent.add(myRoot, java.awt.BorderLayout.CENTER);
            doLayout();
        }
    }

    @Override
    public void deactivateGUI() {
        
    }
}
