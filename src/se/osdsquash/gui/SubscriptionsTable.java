package se.osdsquash.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import se.osdsquash.common.SquashUtil;
import se.osdsquash.xml.jaxb.SubscriptionType;
import se.osdsquash.xml.jaxb.WeekdayType;

/**
 * Custom Table that handles Subscriptions
 *
 */
public class SubscriptionsTable extends JTable {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 143262883719395737L;

    private TableCellEditor trackEditor;
    private TableCellEditor weekdayEditor;
    private TableCellEditor startTimeEditor;

    /**
     * Constructor, taking existing subscriptions, null is OK as well
     * @param subscriptions Existing subscriptions, or null
     */
    protected SubscriptionsTable(List<SubscriptionType> subscriptions) {

        super(new SubscriptionsTableModel(subscriptions));

        super.setCellSelectionEnabled(true);
        super.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        TableColumn trackColumn = super.getColumnModel().getColumn(TableColumnEnum.TRACK_NR.index);
        trackColumn.setMaxWidth(60);
        TableColumn weekdayColumn = super.getColumnModel().getColumn(TableColumnEnum.WEEKDAY.index);
        weekdayColumn.setMaxWidth(90);
        TableColumn startTimeColumn = super.getColumnModel()
            .getColumn(TableColumnEnum.START_TIME.index);
        startTimeColumn.setMaxWidth(80);

        // All cell values are selected through combo boxes, define those editors:
        {
            final Vector<String> trackOptions = new Vector<String>();
            for (Integer trackNr : SquashUtil.getAllTracks()) {
                trackOptions.addElement(String.valueOf(trackNr));
            }
            JComboBox<String> tracksComboBox = new JComboBox<>(trackOptions);
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
        }

        {
            final Vector<String> startTimeOptions = new Vector<String>();
            for (String startTime : SquashUtil.getAllStartTimes()) {
                startTimeOptions.addElement(startTime);
            }
            JComboBox<String> startTimesComboBox = new JComboBox<>(startTimeOptions);
            startTimesComboBox.setMaximumRowCount(18);
            this.startTimeEditor = new DefaultCellEditor(startTimesComboBox);
        }
    }

    protected void addSubscription(SubscriptionType subscriptionType) {
        this.getSubscriptionsTableModel().addSubscription(subscriptionType);
    }

    protected void clearSubscriptions() {
        this.getSubscriptionsTableModel().clearSubscriptions();
    }

    protected List<SubscriptionType> getSubscriptions() {
        return this.getSubscriptionsTableModel().getSubscriptions();
    }

    // Cntrol which editor to use for which column
    @Override
    public TableCellEditor getCellEditor(int row, int column) {

        int modelColumn = this.convertColumnIndexToModel(column);

        if (modelColumn == TableColumnEnum.TRACK_NR.index) {
            return this.trackEditor;
        } else if (modelColumn == TableColumnEnum.WEEKDAY.index) {
            return this.weekdayEditor;
        } else if (modelColumn == TableColumnEnum.START_TIME.index) {
            return this.startTimeEditor;
        } else {
            // This will default to a simple string editor
            return super.getCellEditor(row, column);
        }
    }

    // Returns our custom table model
    protected SubscriptionsTableModel getSubscriptionsTableModel() {
        return (SubscriptionsTableModel) super.getModel();
    }

    protected static class SubscriptionsTableModel extends AbstractTableModel {

        /**
         * Serial UID
         */
        private static final long serialVersionUID = -6302430124070466242L;

        private List<SubscriptionType> subscriptions;

        private String[] columnNames = TableColumnEnum.getNames();

        private static final Class<?>[] COLUMN_CLASSES = new Class[]{
            String.class,
            String.class,
            String.class};

        /**
         * Constructor taking the subscriptions, if any
         * @param subscriptions Subscriptions, null is accepted as well
         */
        protected SubscriptionsTableModel(List<SubscriptionType> subscriptions) {
            super();
            if (subscriptions == null) {
                this.subscriptions = new ArrayList<>(0);
            } else {
                this.subscriptions = subscriptions;
            }
        }

        protected void addSubscription(SubscriptionType subscription) {
            this.subscriptions.add(subscription);
            int rowIndex = this.subscriptions.size() - 1;
            super.fireTableRowsInserted(rowIndex, rowIndex);
        }

        protected void removeSubscription(int rowIndexToRemove) {
            this.subscriptions.remove(rowIndexToRemove);
            super.fireTableRowsDeleted(rowIndexToRemove, rowIndexToRemove);
        }

        protected void clearSubscriptions() {
            this.subscriptions.clear();
        }

        protected List<SubscriptionType> getSubscriptions() {
            return this.subscriptions;
        }

        // All cells can be edited
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

            // Get the actual object to update, e.g. the row
            SubscriptionType subscription = this.subscriptions.get(rowIndex);

            // Set value depending on what column we're at
            if (columnIndex == TableColumnEnum.TRACK_NR.index) {
                int trackNr = Integer.parseInt((String) aValue);
                subscription.setTrackNumber(trackNr);
            } else if (columnIndex == TableColumnEnum.WEEKDAY.index) {
                subscription.setWeekday(SquashUtil.weekdayStringToType((String) aValue));
            } else if (columnIndex == TableColumnEnum.START_TIME.index) {
                subscription.setStartTime((SquashUtil.getCalendarFromTrackTime((String) aValue)));
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
            return this.subscriptions.size();
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

            SubscriptionType subscription = this.subscriptions.get(row);
            TableColumnEnum tableColumn = TableColumnEnum.fromIndex(col);
            switch (tableColumn) {
                case TRACK_NR :
                    return String.valueOf(subscription.getTrackNumber());
                case WEEKDAY :
                    return SquashUtil.weekdayTypeToString(subscription.getWeekday());
                case START_TIME :
                    // Display as HH:mm
                    return SquashUtil.getTrackTimeFromCalendar(subscription.getStartTime());
                default :
                    return null;
            }
        }
    }

    // Table column definitions, with index and display name
    private static enum TableColumnEnum {

        TRACK_NR(0, "Bana"), WEEKDAY(1, "Dag"), START_TIME(2, "Tid");

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
}
