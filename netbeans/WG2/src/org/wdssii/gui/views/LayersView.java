package org.wdssii.gui.views;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.gui.worldwind.WWCategoryLayer;

/**
 *
 * @author Robert Toomey
 */
public class LayersView extends JThreadPanel implements WdssiiView {

    private final LayerTableModel myModel;
    private final JTable myTable;
    private javax.swing.JScrollPane jLayersScrollPane;

    /** Our factory, called by reflection to populate menus, etc...*/
    public static class Factory extends WdssiiDockedViewFactory {

        public Factory() {
            super("Layers", "layers.png");
        }

        public Component getNewComponent() {
            return new LayersView();
        }
    }

    /** Our custom renderer for our layer view table */
    private static class LayerTableCellRenderer extends WG2TableCellRenderer {

        /** A shared JCheckBox for rendering every check box in the list */
        private JCheckBox checkbox = new JCheckBox();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info = "";
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof LayerTableEntry) {
                LayerTableEntry e = (LayerTableEntry) value;

                switch (trueCol) {
                    case LayerTableModel.COL_VISIBLE:
                        return getJCheckBox(table, e.enabled, isSelected, cellHasFocus, row, col);
                    case LayerTableModel.COL_LAYER_NAME:
                        info = e.name;
                        break;
                    case LayerTableModel.COL_CATEGORY_NAME:
                        info = e.category;
                        break;
                }

                // For text...
                setText(info);
            } else {
                setText((String) (value));
            }
            return this;
        }
    }

    /** Each row in our table will have a LayerTableEntry */
    public static class LayerTableEntry {

        String name;
        boolean enabled;
        String category;
    }

    /** We have a custom model that stores a single LayerTableEntry
     * for each row of the table
     */
    private static class LayerTableModel extends RowEntryTableModel<LayerTableEntry> {

        private static final int COL_VISIBLE = 0;
        private static final int COL_LAYER_NAME = 1;
        private static final int COL_CATEGORY_NAME = 2;

        public LayerTableModel() {
            super(LayerTableEntry.class, new String[]{
                        "Visible", "Layer", "Category"});
        }
    }

    @Override
    public void updateInSwingThread(Object command) {
    }

    public LayersView() {
        initComponents();
        final JTable t = new javax.swing.JTable();
        myTable = t;
        final LayerTableModel m = new LayerTableModel();
        myModel = m;
        t.setModel(m);
        t.setFillsViewportHeight(
                true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set renderer for all LayerTableEntry cells
        LayerTableCellRenderer l = new LayerTableCellRenderer();
        t.setDefaultRenderer(LayerTableEntry.class, l);

        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();


        int count = t.getColumnCount();
        TableColumnModel cm = t.getColumnModel();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            col.setHeaderRenderer(new IconHeaderRenderer());
            switch (i) {
                case 0:
                    IconHeaderInfo info = new IconHeaderInfo("layervisible.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                    break;
            }
        }

        jLayersScrollPane.setViewportView(t);

        setUpSortingColumns();

        /** Add the mouse listener that handles clicking in any cell of our
         * custom Layer table
         */
        t.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // You actually want the single AND the double clicks so
                // that you always toggle even if they are clicking fast,
                // so we don't check click count.
                if (e.getComponent().isEnabled()
                        && e.getButton() == MouseEvent.BUTTON2) {
                    updateLayerList();
                    return;
                }
                if (e.getComponent().isEnabled()
                        && e.getButton() == MouseEvent.BUTTON1/* && e.getClickCount() == 1*/) {
                    Point p = e.getPoint();
                    int row = t.rowAtPoint(p);
                    int column = t.columnAtPoint(p);

                    if ((row > -1) && (column > -1)) {
                        int orgColumn = myTable.convertColumnIndexToModel(column);
                        int orgRow = myTable.convertRowIndexToModel(row);
                        Object stuff = myModel.getValueAt(orgRow, orgColumn);
                        if (stuff instanceof LayerTableEntry) {
                            LayerTableEntry entry = (LayerTableEntry) (stuff);


                            /** a click on visible checkbox toggles layer visibility */
                            if (orgColumn == LayerTableModel.COL_VISIBLE) {
                                LayerList list = getLayerList();
                                if (list != null) {
                                    Layer l = list.getLayerByName(entry.name);
                                    l.setEnabled(!l.isEnabled());
                                }
                                updateLayerList();
                            }
                        }
                    }

                }
            }
        });

        // FIXME: Earth ball currently has to be created first or this
        // can't get the layer list...
        updateLayerList();
    }

    private void initComponents() {

        jLayersScrollPane = new javax.swing.JScrollPane();

        jLayersScrollPane.setBorder(null);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLayersScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLayersScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE));
    }// </editor-fold>     

    /** Set up sorting columns if wanted */
    private void setUpSortingColumns() {

        /** Set the sorters for each column */
        TableRowSorter<LayerTableModel> sorter =
                new TableRowSorter<LayerTableModel>(myModel);
        myTable.setRowSorter(sorter);

        // This sort is strange, since toggle makes the item 'jump'  Seems
        // to be worker though.  Might be nice to disable the checkbox
        // columns from sorting....
        sorter.setComparator(LayerTableModel.COL_VISIBLE, new Comparator<LayerTableEntry>() {

            @Override
            public int compare(LayerTableEntry o1, LayerTableEntry o2) {
                return Boolean.valueOf(o1.enabled).compareTo(Boolean.valueOf(o2.enabled));
            }
        });
        sorter.setComparator(LayerTableModel.COL_LAYER_NAME, new Comparator<LayerTableEntry>() {

            @Override
            public int compare(LayerTableEntry o1, LayerTableEntry o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });
        sorter.setComparator(LayerTableModel.COL_CATEGORY_NAME, new Comparator<LayerTableEntry>() {

            @Override
            public int compare(LayerTableEntry o1, LayerTableEntry o2) {
                return o1.category.compareToIgnoreCase(o2.category);
            }
        });
        myTable.getRowSorter().toggleSortOrder(LayerTableModel.COL_CATEGORY_NAME);
        myTable.getRowSorter().toggleSortOrder(LayerTableModel.COL_CATEGORY_NAME);
    }

    /** Return the layer list we use
    FIXME: Make this interface/superclass so others could use this
    view if wanted in their own code
     */
    public LayerList getLayerList() {
        LayerList layers = CommandManager.getInstance().getLayerList();
        return layers;
    }

    /** Update the layer list from information pulled from a WorldWind
     * LayerList
     */
    public final void updateLayerList() {
        ArrayList<LayerTableEntry> e = new ArrayList<LayerTableEntry>();

        // Try to save old selection.  We'll assume if the number of layers
        // is the same that the old row is the same row...
        int oldRow = myTable.getSelectedRow();

        LayerList layers = getLayerList();
        if (layers != null) {
            for (Layer layer : layers) {
                LayerTableEntry n = new LayerTableEntry();
                layer.getName();
                n.name = layer.getName();
                n.enabled = layer.isEnabled();
                if (layer instanceof WWCategoryLayer) {
                    n.category = ((WWCategoryLayer) layer).getCategory();
                } else {
                    n.category = "NASA WorldWind Layer";
                }
                e.add(n);
            }
        }
        myModel.setDataTypes(e);
        myModel.fireTableDataChanged();
        if (oldRow > -1) {
            myTable.setRowSelectionInterval(oldRow, oldRow);
        }
    }
}