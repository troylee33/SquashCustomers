package se.osdsquash.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.xml.datatype.XMLGregorianCalendar;

import se.osdsquash.common.SquashProperties;
import se.osdsquash.common.SquashUtil;
import se.osdsquash.xml.XmlRepository;
import se.osdsquash.xml.jaxb.CustomerType;
import se.osdsquash.xml.jaxb.SubscriptionType;
import se.osdsquash.xml.jaxb.WeekdayType;

/**
 * Panel grouping all customer sub-components for one customer:
 * The customer details, it's subscriptions and invoices
 */
public class CustomerMasterPanel extends JPanel {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -782397964994908639L;

    private CustomerDetailsPanel customerDetailsPanel;
    private SubscriptionsTable subscriptionsTable;
    private InvoicesTable invoicesTable;

    private XmlRepository xmlRepository;

    private JButton addSubscriptionButton;
    private JButton deleteSubscriptionButton;

    protected CustomerMasterPanel(XmlRepository xmlRepository) {

        super(new FlowLayout());
        this.setSize(new Dimension(600, 420));

        this.xmlRepository = XmlRepository.getInstance();

        Border border = BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK),
            "  Kunduppgifter  ",
            TitledBorder.LEFT,
            TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.PLAIN, 14),
            Color.DARK_GRAY);
        this.setBorder(border);

        // The subscriptions and invoices are presented in tables:

        this.subscriptionsTable = new SubscriptionsTable(null);
        this.subscriptionsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        this.invoicesTable = new InvoicesTable(null);
        this.invoicesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Add customer details first, aligned to left
        this.customerDetailsPanel = new CustomerDetailsPanel(
            this.subscriptionsTable,
            this.invoicesTable);
        this.customerDetailsPanel.setMinimumSize(new Dimension(600, 300));
        this.customerDetailsPanel.setMaximumSize(new Dimension(600, 400));
        this.add(this.customerDetailsPanel);

        // Hack to get some horizontal space
        this.add(new JLabel("    "));

        // Create a panel holding subscriptions first, then invoices below
        JPanel subscriptionsAndInvoicesOuterPanel = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 10, 10));
        subscriptionsAndInvoicesOuterPanel.setPreferredSize(new Dimension(400, 460));
        subscriptionsAndInvoicesOuterPanel.setMinimumSize(new Dimension(400, 460));
        subscriptionsAndInvoicesOuterPanel.setMaximumSize(new Dimension(400, 460));

        // Now add subscriptions inside a border:ed scroller, aligned to the right.
        // We must use a scroller to get the headers in the table correct!
        JScrollPane subscriptionsScrollPane = new JScrollPane(this.subscriptionsTable);

        Border subscriptionsBorder = BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY),
            " Abonnemang ",
            TitledBorder.LEFT,
            TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.PLAIN, 12),
            Color.DARK_GRAY);
        subscriptionsScrollPane.setBorder(subscriptionsBorder);
        subscriptionsScrollPane.setPreferredSize(new Dimension(240, 150));
        subscriptionsScrollPane.setMinimumSize(new Dimension(240, 150));
        subscriptionsScrollPane.setMaximumSize(new Dimension(240, 150));

        subscriptionsAndInvoicesOuterPanel.add(subscriptionsScrollPane);

        // Hack to cause a line break:
        subscriptionsAndInvoicesOuterPanel.add(new JLabel("              "));

        // Create add/remove buttons below
        this.addSubscriptionButton = new JButton("+");
        this.addSubscriptionButton.setEnabled(false);
        this.addSubscriptionButton.setMinimumSize(new Dimension(80, 22));
        this.addSubscriptionButton.setMaximumSize(new Dimension(80, 22));

        this.addSubscriptionButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                SubscriptionType newSubscription = CustomerMasterPanel.this.xmlRepository
                    .getNewSubscription();
                XMLGregorianCalendar firstTime = SquashUtil
                    .getCalendarFromTrackTime(SquashUtil.getAllStartTimes().get(0));
                newSubscription.setStartTime(firstTime);
                newSubscription.setTrackNumber(SquashProperties.FIRST_TRACK_NR.intValue());
                newSubscription.setWeekday(WeekdayType.MONDAY);

                CustomerMasterPanel.this.subscriptionsTable
                    .getSubscriptionsTableModel()
                    .addSubscription(newSubscription);

                CustomerMasterPanel.this.subscriptionsTable.repaint();
            }
        });

        subscriptionsAndInvoicesOuterPanel.add(this.addSubscriptionButton);

        this.deleteSubscriptionButton = new JButton("-");
        this.deleteSubscriptionButton.setEnabled(false);
        this.deleteSubscriptionButton.setMinimumSize(new Dimension(80, 22));
        this.deleteSubscriptionButton.setMaximumSize(new Dimension(80, 22));

        this.deleteSubscriptionButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                int selectedIndex = CustomerMasterPanel.this.subscriptionsTable.getSelectedRow();
                if (selectedIndex >= 0) {

                    int dialogResult = JOptionPane.showConfirmDialog(
                        CustomerMasterPanel.this,
                        "Vill du verkligen radera abonnemanget?",
                        "Radera?",
                        JOptionPane.YES_NO_OPTION);

                    if (dialogResult != JOptionPane.YES_OPTION) {
                        return;
                    }

                    CustomerMasterPanel.this.subscriptionsTable
                        .getSubscriptionsTableModel()
                        .removeSubscription(selectedIndex);

                } else {
                    MainGUI
                        .getInstance()
                        .printInfoText("Ingen abonnemangsrad markerad", true, true);
                }
            }
        });

        subscriptionsAndInvoicesOuterPanel.add(this.deleteSubscriptionButton);

        subscriptionsAndInvoicesOuterPanel
            .add(new JLabel("___________________________________________________"));
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(390, 2));
        subscriptionsAndInvoicesOuterPanel.add(separator);
        subscriptionsAndInvoicesOuterPanel
            .add(new JLabel("                                         "));

        // Now add the invoices table, shown below the subscriptions
        JScrollPane invoicesScrollPane = new JScrollPane(
            this.invoicesTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        invoicesScrollPane.setAlignmentX(Component.RIGHT_ALIGNMENT);

        Border invoicesBorder = BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY),
            " Fakturor ",
            TitledBorder.LEFT,
            TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.PLAIN, 12),
            Color.DARK_GRAY);
        invoicesScrollPane.setBorder(invoicesBorder);
        invoicesScrollPane.setPreferredSize(new Dimension(370, 150));
        invoicesScrollPane.setMinimumSize(new Dimension(370, 150));
        invoicesScrollPane.setMaximumSize(new Dimension(370, 150));

        subscriptionsAndInvoicesOuterPanel.add(invoicesScrollPane);

        // Draw the subscriptions and invoices as one panel to the right
        this.add(subscriptionsAndInvoicesOuterPanel);

        // Hack to get some horizontal space
        this.add(new JLabel("  "));
    }

    /**
     * Toggles enable/disable of this panel's components
     * @param enable True if to enable, false to disable
     */
    protected void toggleEnabled(boolean enable) {

        this.subscriptionsTable.setEnabled(enable);
        this.invoicesTable.setEnabled(enable);
        this.addSubscriptionButton.setEnabled(enable);
        this.deleteSubscriptionButton.setEnabled(enable);
    }

    // Below are some delegate methods, to the underlying panels
    protected boolean isCustomerDirty() {
        return this.customerDetailsPanel.isCustomerDirty();
    }

    protected void clearCustomer() {
        this.customerDetailsPanel.clearCustomer();
    }

    protected void setCustomer(CustomerType customerType) {
        this.customerDetailsPanel.setCustomer(customerType);
    }

    protected void prepareNewCustomer(int customerNr) {
        this.customerDetailsPanel.prepareNewCustomer(customerNr);
    }
}
