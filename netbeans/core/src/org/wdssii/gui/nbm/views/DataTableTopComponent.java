package org.wdssii.gui.nbm.views;

import net.miginfocom.swing.MigLayout;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.wdssii.gui.views.TableProductView;

/**
 * DataTableTopComponent shows the 2D tracking table of any product
 * capable of displaying its data in an excel format.
 * @author Robert Toomey
 */
@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//DataTable//EN",
autostore = false)
@TopComponent.Description(preferredID = "DataTableTopComponent",
iconBase = "org/wdssii/gui/nbm/views/color_swatch.png",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "explorer", openAtStartup = false)
@ActionID(category = "Window", id = "org.wdssii.gui.nbm.views.DataTableTopComponent")
@ActionReference(path = "Menu/Window/WDSSII" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_DataTableAction",
preferredID = "DataTableTopComponent")
public final class DataTableTopComponent extends TopComponent {

    private TableProductView myPanel;
      
    public DataTableTopComponent() {
      //  initComponents();
        
        setLayout(new MigLayout("fill, inset 0", "", ""));
        myPanel = new TableProductView();
        add(myPanel, "grow");
        
        setName(NbBundle.getMessage(DataTableTopComponent.class, "CTL_DataTableTopComponent"));
        setToolTipText(NbBundle.getMessage(DataTableTopComponent.class, "HINT_DataTableTopComponent"));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
