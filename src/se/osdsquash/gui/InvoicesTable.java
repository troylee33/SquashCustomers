package se.osdsquash.gui;

import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import se.osdsquash.common.SquashUtil;
import se.osdsquash.xml.jaxb.InvoiceType;

/**
 * Custom Table that handles Invoices
 *
 */
public class InvoicesTable extends JTable {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 6855240729945090702L;

    private TableCellEditor invoiceNrEditor;
    private TableCellEditor filenameEditor;

    /**
     * Constructor, taking existing invoice references, null is OK as well
     * @param invoices Existing invoices, or null
     */
    protected InvoicesTable(List<InvoiceType> invoices) {

        super(new InvoiceTableModel(invoices));

        super.setCellSelectionEnabled(true);
        super.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableColumn invoiceNrColumn = super.getColumnModel()
            .getColumn(TableColumnEnum.INVOICE_NR.index);
        invoiceNrColumn.setMaxWidth(80);
        invoiceNrColumn.setCellRenderer(new SmallerFontRenderer());

        TableColumn filenameColumn = super.getColumnModel()
            .getColumn(TableColumnEnum.FILENAME.index);
        filenameColumn.setMaxWidth(278);
        filenameColumn.setCellRenderer(new SmallerFontRenderer());

        // TODO OLLE, what to display and edit ???

        // All cell values are selected through combo boxes, define those editors:
        /*{
            final Vector<Integer> trackOptions = new Vector<Integer>();
            for (Integer trackNr : SquashUtil.getAllTracks()) {
                trackOptions.addElement(trackNr);
            }
            JComboBox<Integer> tracksComboBox = new JComboBox<>(trackOptions);
            this.trackEditor = new DefaultCellEditor(tracksComboBox);
        }
        
        {
            final Vector<String> weekdayOptions = new Vector<String>();
            for (WeekdayType weekdayType : WeekdayType.values()) {
                String weekdayString = SquashUtil.weekdayTypeToString(weekdayType);
                weekdayOptions.addElement(weekdayString);
            }
            JComboBox<String> weekdaysComboBox = new JComboBox<>(weekdayOptions);
            this.weekdayEditor = new DefaultCellEditor(weekdaysComboBox);
        }*/
    }

    protected void addInvoice(InvoiceType invoiceType) {
        this.getInvoicesTableModel().addInvoice(invoiceType);
    }

    protected void clearInvoices() {
        this.getInvoicesTableModel().clearInvoices();
    }

    protected List<InvoiceType> getInvoices() {
        return this.getInvoicesTableModel().getInvoices();
    }

    // Cntrol which editor to use for which column
    @Override
    public TableCellEditor getCellEditor(int row, int column) {

        int modelColumn = this.convertColumnIndexToModel(column);

        if (modelColumn == TableColumnEnum.INVOICE_NR.index) {
            return this.invoiceNrEditor;
        } else if (modelColumn == TableColumnEnum.FILENAME.index) {
            return this.filenameEditor;
        } else {
            // This will default to a simple string editor
            return super.getCellEditor(row, column);
        }
    }

    // Returns the custom table model
    protected InvoiceTableModel getInvoicesTableModel() {
        return (InvoiceTableModel) super.getModel();
    }

    protected static class InvoiceTableModel extends AbstractTableModel {

        /**
         * Serial UID
         */
        private static final long serialVersionUID = -6729184383176033090L;

        private List<InvoiceType> invoices;

        private String[] columnNames = TableColumnEnum.getNames();

        private static final Class<?>[] COLUMN_CLASSES = new Class[]{Integer.class, String.class};

        /**
         * Constructor taking the invoices, if any
         * @param invoices Invoices, null is accepted as well
         */
        protected InvoiceTableModel(List<InvoiceType> invoices) {
            super();
            if (invoices == null) {
                this.invoices = new ArrayList<>(0);
            } else {
                this.invoices = invoices;
            }
        }

        protected void addInvoice(InvoiceType invoice) {
            this.invoices.add(invoice);
            int rowIndex = this.invoices.size() - 1;
            super.fireTableRowsInserted(rowIndex, rowIndex);
        }

        protected void removeInvoice(int rowIndexToRemove) {
            this.invoices.remove(rowIndexToRemove);
            super.fireTableRowsDeleted(rowIndexToRemove, rowIndexToRemove);
        }

        protected void clearInvoices() {
            this.invoices.clear();
        }

        protected List<InvoiceType> getInvoices() {
            return this.invoices;
        }

        // No cells can be edited
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

            // Get the actual object to update, e.g. the row
            InvoiceType invoice = this.invoices.get(rowIndex);

            // Set value depending on what column we're at
            if (columnIndex == TableColumnEnum.INVOICE_NR.index) {
                invoice.setInvoiceNumber((Integer) aValue);
            } else if (columnIndex == TableColumnEnum.FILENAME.index) {
                // No edit allowed!
            } else {
                // Something is wrong...
                throw new RuntimeException("Unknown rowIndex:" + rowIndex);
            }

            // Fire event
            this.fireTableCellUpdated(rowIndex, columnIndex);
        }

        // Returns a constant columns number for this model
        @Override
        public int getColumnCount() {
            return COLUMN_CLASSES.length;
        }

        // Returns the number of rows
        @Override
        public int getRowCount() {
            return this.invoices.size();
        }

        // Returns the name of the given column index
        @Override
        public String getColumnName(int col) {
            return this.columnNames[col];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return COLUMN_CLASSES[col];
        }

        // Returns the value of each cell
        @Override
        public Object getValueAt(int row, int col) {

            InvoiceType invoice = this.invoices.get(row);
            TableColumnEnum tableColumn = TableColumnEnum.fromIndex(col);
            switch (tableColumn) {
                case INVOICE_NR :
                    return Integer.valueOf(invoice.getInvoiceNumber());
                case FILENAME :
                    return SquashUtil.getFilenameFromPath(invoice.getRelativeFilePath());
                default :
                    return null;
            }
        }
    }

    // Table column definitions, with index and display name
    private static enum TableColumnEnum {

        INVOICE_NR(0, "FakturaNr"), FILENAME(1, "Filnamn");

        private TableColumnEnum(int index, String name) {
            this.index = index;
            this.name = name;
        }

        private int index;
        private String name;

        private static final Map<Integer, TableColumnEnum> COLUMN_INDEX_NAME_MAP = new HashMap<>();
        private static final List<String> NAMES = new ArrayList<>();

        static {
            for (TableColumnEnum column : TableColumnEnum.values()) {
                COLUMN_INDEX_NAME_MAP.put(column.index, column);
                NAMES.add(column.name);
            }
        }

        public static TableColumnEnum fromIndex(int colIndex) {
            TableColumnEnum columnName = COLUMN_INDEX_NAME_MAP.get(colIndex);
            return (columnName != null) ? columnName : null;
        }

        public static String[] getNames() {
            return NAMES.toArray(new String[NAMES.size()]);
        }
    }

    private static class SmallerFontRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {

            if (value == null) {
                return super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);
            }

            JLabel label = new JLabel(value.toString());
            label.setFont(new Font(null, Font.PLAIN, 10));
            return label;
        }
    };
}
