package se.osdsquash.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.xml.datatype.XMLGregorianCalendar;

import se.osdsquash.common.SquashUtil;
import se.osdsquash.mail.MailHandler;
import se.osdsquash.xml.jaxb.CustomerInfoType;
import se.osdsquash.xml.jaxb.InvoiceStatusType;
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

    private TableCellEditor invoiceStatusEditor;

    private CustomerInfoType customerInfo;

    /**
     * Constructor, taking existing invoice references, null is OK as well
     * @param invoices Existing invoices, or null
     */
    protected InvoicesTable(List<InvoiceType> invoices) {

        super(new InvoiceTableModel(invoices));

        super.setCellSelectionEnabled(true);
        super.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        TableColumn statusColumn = super.getColumnModel().getColumn(TableColumnEnum.STATUS.index);
        statusColumn.setMaxWidth(70);
        statusColumn.setCellRenderer(new SmallerFontRenderer());

        TableColumn invoiceNrColumn = super.getColumnModel()
            .getColumn(TableColumnEnum.INVOICE_NR.index);
        invoiceNrColumn.setMaxWidth(70);
        invoiceNrColumn.setCellRenderer(new SmallerFontRenderer());

        TableColumn dueDateColumn = super.getColumnModel()
            .getColumn(TableColumnEnum.DUE_DATE.index);
        dueDateColumn.setMaxWidth(80);
        dueDateColumn.setCellRenderer(new SmallerFontRenderer());

        TableColumn filenameColumn = super.getColumnModel()
            .getColumn(TableColumnEnum.FILENAME.index);
        filenameColumn.setMaxWidth(410);
        filenameColumn.setCellRenderer(new SmallerFontRenderer());

        TableColumn mailColumn = super.getColumnModel().getColumn(TableColumnEnum.MAIL.index);
        mailColumn.setMaxWidth(40);
        mailColumn.setCellRenderer(new SmallerFontRenderer());

        // Use a combobox editor when editing the invoice status
        {
            final Vector<String> statusOptions = new Vector<String>();
            for (InvoiceStatusType statusType : InvoiceStatusType.values()) {
                String statusString = SquashUtil.invoiceStatusTypeToString(statusType);
                statusOptions.addElement(statusString);
            }
            JComboBox<String> statusesComboBox = new JComboBox<>(statusOptions);
            statusesComboBox.setFont(new Font(null, Font.PLAIN, 10));
            this.invoiceStatusEditor = new DefaultCellEditor(statusesComboBox);
        }

        // The filename & mail cells are clickable
        super.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getClickCount() > 0) {
                    int selectedRow = InvoicesTable.this.getSelectedRow();
                    int selectedColumn = InvoicesTable.this.getSelectedColumn();
                    if (selectedRow >= 0) {

                        // If a filename cell has been clicked, open the file
                        if (selectedColumn == TableColumnEnum.FILENAME.index) {
                            InvoiceType invoice = ((InvoiceTableModel) InvoicesTable.this
                                .getModel()).getInvoices().get(selectedRow);
                            if (invoice != null) {
                                InvoicesTable.this.openFile(invoice.getRelativeFilePath());
                            } else {
                                MainGUI.getInstance().printInfoText(
                                    "Fakturan kan inte mailas: Inga fakturauppgifter hittades för denna rad",
                                    true,
                                    true);
                            }

                            // Create a new mail with the file as attachment
                        } else if (selectedColumn == TableColumnEnum.MAIL.index) {
                            InvoiceType invoice = ((InvoiceTableModel) InvoicesTable.this
                                .getModel()).getInvoices().get(selectedRow);

                            if (!SquashUtil.isSet(InvoicesTable.this.customerInfo.getEmail())) {
                                MainGUI.getInstance().printInfoText(
                                    "Det finns ingen E-post angiven",
                                    true,
                                    true);
                                return;
                            }

                            if (invoice != null) {
                                MainGUI.getInstance().printInfoText(
                                    "Mail-programmet startar...",
                                    false,
                                    true);
                                new MailHandler()
                                    .createMailDraft("adress", invoice.getRelativeFilePath(), true);
                            } else {
                                MainGUI.getInstance().printInfoText(
                                    "Fakturan kan inte mailas: Inga fakturauppgifter hittades för denna rad",
                                    true,
                                    true);
                            }
                        }
                    }
                }
            }
        });
    }

    // This works as a mouse over event for cells
    @Override
    protected void processMouseMotionEvent(MouseEvent e) {

        // Set hand cursor for filename and mail links, otherwise standard cursor
        Point point = e.getPoint();
        int column = InvoicesTable.this.columnAtPoint(point);
        int row = InvoicesTable.this.rowAtPoint(point);
        if ((TableColumnEnum.FILENAME.index == column || TableColumnEnum.MAIL.index == column)
            && row >= 0) {
            InvoicesTable.this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            JLabel labelRenderer = (JLabel) InvoicesTable.this.getCellRenderer(row, column);
            labelRenderer.setText("<html><u>)" + labelRenderer.getText() + "</u></html>");
        } else {
            InvoicesTable.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        super.processMouseMotionEvent(e);
    }

    protected void setInvoices(CustomerInfoType customerInfo, List<InvoiceType> invoices) {
        this.customerInfo = customerInfo;
        this.getInvoicesTableModel().setInvoices(invoices);
    }

    protected void clearInvoices() {
        this.customerInfo = null;
        this.getInvoicesTableModel().clearInvoices();
    }

    protected List<InvoiceType> getInvoices() {
        return this.getInvoicesTableModel().getInvoices();
    }

    protected void refreshInvoiceTable() {
        this.getInvoicesTableModel().fireTableDataChanged();
    }

    // Cntrol which editor to use for which column
    @Override
    public TableCellEditor getCellEditor(int row, int column) {

        int modelColumn = this.convertColumnIndexToModel(column);

        if (modelColumn == TableColumnEnum.STATUS.index) {
            return this.invoiceStatusEditor;
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

        private static final Class<?>[] COLUMN_CLASSES = new Class[]{
            String.class,
            Integer.class,
            XMLGregorianCalendar.class,
            String.class,
            String.class};

        /**
         * Constructor taking the invoices, if any
         * @param invoices Invoices, null is accepted as well
         */
        protected InvoiceTableModel(List<InvoiceType> invoices) {
            super();
            if (invoices == null) {
                this.invoices = new ArrayList<>();
            } else {
                this.invoices = invoices;
                Collections.sort(this.invoices, new InvoiceComparator());
            }
        }

        protected void setInvoices(List<InvoiceType> invoices) {
            this.invoices.clear();
            this.invoices.addAll(invoices);
            Collections.sort(this.invoices, new InvoiceComparator());
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

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Status can be edited:
            return TableColumnEnum.STATUS.index == columnIndex;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

            // Get the actual object to update, e.g. the row
            InvoiceType invoice = this.invoices.get(rowIndex);

            // Set value depending on what column we're at.
            // Only Status may be changed, only handle that:
            if (columnIndex == TableColumnEnum.INVOICE_NR.index) {
                // No edit allowed!
            } else if (columnIndex == TableColumnEnum.FILENAME.index) {
                // No edit allowed!
            } else if (columnIndex == TableColumnEnum.MAIL.index) {
                // No edit allowed!
            } else if (columnIndex == TableColumnEnum.DUE_DATE.index) {
                // No edit allowed!
            } else if (columnIndex == TableColumnEnum.STATUS.index) {
                // Status can be edited:
                invoice.setInvoiceStatus(SquashUtil.invoiceStatusStringToType((String) aValue));
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
                case DUE_DATE :
                    return SquashUtil.getDayFormat(invoice.getDueDate());
                case FILENAME :
                    return SquashUtil.getFilenameFromPath(invoice.getRelativeFilePath());
                case MAIL :
                    return "Mail";
                case STATUS :
                    return SquashUtil.invoiceStatusTypeToString(invoice.getInvoiceStatus());
                default :
                    return null;
            }
        }
    }

    // Table column definitions, with index and display name
    private static enum TableColumnEnum {

        STATUS(0, "Status"), INVOICE_NR(1, "FakturaNr"), DUE_DATE(2, "Förfaller"), FILENAME(3,
                "Filnamn"), MAIL(4, "Mail");

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

    private void openFile(String filePath) {

        String errorMessage = null;

        File file = new File(filePath);
        if (!file.isFile()) {
            errorMessage = "Filen "
                + filePath
                + " kunde inte öppnas."
                + "\n"
                + "Filen kanske är borttagen?";
        }

        if (errorMessage == null) {
            if (!Desktop.isDesktopSupported()) {
                errorMessage = "Kan inte initiera filöppning på denna plattform, hittar ingen Desktop.";
            }
        }

        if (errorMessage == null) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.open(file);

            } catch (UnsupportedOperationException uoException) {
                errorMessage = "Kan inte initiera filöppning på denna plattform. Felmeddelande: "
                    + uoException.getMessage();
            } catch (IOException ioException) {
                errorMessage = "Kan inte öppna filen."
                    + " Kontrollera att det finns ett program som hanterar Excel-filer. Felmeddelande: "
                    + ioException.getMessage();
            } catch (Exception exception) {
                errorMessage = "Kan inte öppna filen, ett okänt fel inträffade. Felmeddelande: "
                    + exception.getMessage();
            }
        }

        if (errorMessage != null) {
            JOptionPane.showMessageDialog(null, errorMessage, "Fel", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Cell renderer using a smaller, size 10 font
    private class SmallerFontRenderer extends DefaultTableCellRenderer {

        /**
         * Serial UID
         */
        private static final long serialVersionUID = -8063226768847279359L;

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

            if (column == TableColumnEnum.DUE_DATE.index) {
                InvoiceType invoice = ((InvoiceTableModel) table.getModel()).getInvoices().get(row);
                if (invoice != null && SquashUtil.isOverdue(invoice)) {
                    label.setForeground(Color.RED);
                    label.setToolTipText("Fakturan verkar ha förfallit obetald");
                }
            } else if (column == TableColumnEnum.FILENAME.index) {
                InvoiceType invoice = ((InvoiceTableModel) table.getModel()).getInvoices().get(row);
                if (invoice != null) {
                    // Set the font style as a link
                    String filename = SquashUtil.getFilenameFromPath(invoice.getRelativeFilePath());
                    label.setText("<html><u>" + filename + "</u></html>");
                    label.setToolTipText(filename);
                    label.setForeground(Color.BLUE);
                    return label;
                }
            } else if (column == TableColumnEnum.MAIL.index) {

                if (SquashUtil.isSet(InvoicesTable.this.customerInfo.getEmail())) {
                    InvoiceType invoice = ((InvoiceTableModel) table.getModel())
                        .getInvoices()
                        .get(row);
                    if (invoice != null) {
                        // Set the font style as a link to create a new mail
                        label.setEnabled(true);
                        label.setText("<html><u>Mail</u></html>");
                        label.setToolTipText("Maila fakturan till kunden");
                        label.setForeground(Color.BLUE);
                        return label;
                    }
                } else {
                    label.setEnabled(false);
                    label.setText("Mail");
                    label.setToolTipText("Kundens E-mail måste anges");
                    label.setForeground(Color.LIGHT_GRAY);
                }

            }

            return label;
        }
    };

    // Sorts newest created invoice first
    private static final class InvoiceComparator implements Comparator<InvoiceType>, Serializable {

        /**
         * Serial UID
         */
        private static final long serialVersionUID = -1835375418064528868L;

        @Override
        public int compare(InvoiceType o1, InvoiceType o2) {
            return o2.getCreatedDate().toGregorianCalendar().compareTo(
                o1.getCreatedDate().toGregorianCalendar());
        }
    }
}
