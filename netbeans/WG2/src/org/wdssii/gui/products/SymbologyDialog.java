package org.wdssii.gui.products;

import com.jidesoft.swing.JideSplitPane;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.products.renderers.DataTableRenderer;
import org.wdssii.gui.renderers.SymbolFactory;
import org.wdssii.gui.renderers.SymbolGUI;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Dialog for changing symbology of our products Not sure where this belongs.
 *
 * (alpha)
 *
 * @author Robert Toomey
 */
public class SymbologyDialog extends JDialog {

    private JPanel myPanel = null;
    private JButton myOKButton;
    private JPanel myGUIHolder;
    private SymbolGUI myCurrentGUI = null;
    private JButton mySymbolButton;
    private Product myProduct = null;

    // Because Java is brain-dead with JDialog/JFrame silliness
    public SymbologyDialog(Product prod, JFrame owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(prod, location, myMessage);
    }

    public SymbologyDialog(Product prod, JDialog owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(prod, location, myMessage);
    }

    private void init(Product prod, Component location, String myMessage) {

        myProduct = prod;
        setTitle("Symbology");
        Container content = getContentPane();

        // Root panel, containing split pane top and buttons bottom
        JPanel p;
        myPanel = p = new JPanel();
        p.setLayout(new MigLayout(new LC().fill().insetsAll("10"), null, null));

        // The extra information panel...
        myGUIHolder = new JPanel();
        //myGUIHolder.setSize(200, 50);
        //p.add(myGUIHolder, new CC().growX().growY().pushY().span().wrap());

        JPanel buttonPanel = new JPanel();
        String listData[] = {
            "Single Symbol",
            "Categories:Unique Values",
        };
        JList tree = new JList(listData);
        mySymbolButton = new JButton("Edit Symbol");

        JideSplitPane s = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        s.setProportionalLayout(true);
        s.setShowGripper(true);
        s.add(new JScrollPane(tree));
        s.add(new JScrollPane(mySymbolButton));

        //p.add(mySymbolButton);

        p.add(s, new CC().growX().growY()); // Fill with split pane

        // The OK button...we allow GUIPlugInPanels to hook into this
        myOKButton = new JButton("OK");
        buttonPanel.add(myOKButton, new CC());

        // The cancel button
        //myCancelButton = new JButton("Cancel");
        //buttonPanel.add(myCancelButton);
        p.add(buttonPanel, new CC().dockSouth());

        content.setLayout(new MigLayout(new LC().fill().insetsAll("10"), null, null));
        content.add(myPanel, new CC().growX().growY());
        pack();
        setLocationRelativeTo(this);

        mySymbolButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SymbolDialog myDialog = new SymbolDialog(myProduct, SymbologyDialog.this, SymbologyDialog.this, true, "Symbology");
            }
        });
        // myCancelButton.addActionListener(new java.awt.event.ActionListener() {
        //     @Override
        //     public void actionPerformed(java.awt.event.ActionEvent evt) {
        //         dispose();
        //     }
        // });
        myOKButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dispose();
            }
        });

        setLocationRelativeTo(location);
        setVisible(true);
    }
}
