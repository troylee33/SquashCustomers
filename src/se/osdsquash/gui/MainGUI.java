package se.osdsquash.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import se.osdsquash.common.SquashUtil;
import se.osdsquash.common.SubscriptionPeriod;
import se.osdsquash.xml.XmlRepository;
import se.osdsquash.xml.jaxb.CustomerInfoType;
import se.osdsquash.xml.jaxb.CustomerType;

/**
 * The GUI class that presents/interacts - called by the main <code>Runner</code> class.
 * 
 * <p>
 * The design is a singleton instance
 * </p>
 */
public class MainGUI extends JFrame {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 1418443841032535316L;

    // Singleton reference to the repository:
    private XmlRepository xmlRepository;

    private static final int WINDOW_PIXEL_WIDTH = 1020;
    private static final int WINDOW_PIXEL_HEIGTH = 960;

    private final JLabel validationErrorLabel = new JLabel(" ");
    private final JLabel infoLabel = new JLabel(" ");

    // The list of customers data model and list:
    private DefaultListModel<CustomerType> customerListModel;
    private JList<CustomerType> customerList;

    // The customer master panel. Inside this panel, there are sub-panels
    private CustomerMasterPanel customerMasterPanel;

    private final JButton newCustomerButton = new JButton("Ny Kund");
    private final JButton deleteCustomerButton = new JButton("Radera Kund");
    private final JButton generateInvoicesButton = new JButton("Skapa Fakturor");

    private static final MainGUI INSTANCE = new MainGUI();

    /**
     * Returns the one and only instance of the main GUI panel
     * @return The instance
     */
    public static MainGUI getInstance() {
        return INSTANCE;
    }

    // Private constructor...creates and shows the GUI
    private MainGUI() {
        this.xmlRepository = XmlRepository.getInstance();
        this.initGUI();
    }

    // Init and draws the GUI
    private void initGUI() {

        // --------------------------------  GENERIC PREPARATIONS --------------------------------
        // ---------------------------------------------------------------------------------------

        // Set some default button texts to Swedish:
        UIManager.put("OptionPane.cancelButtonText", "Avbryt");
        UIManager.put("OptionPane.noButtonText", "Nej");
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.yesButtonText", "Ja");

        // Present a small loading dialog, while starting up the whole program
        final JDialog waitForInitDialog = new JDialog(MainGUI.this, "Startar programmet...", true);
        waitForInitDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        waitForInitDialog.setResizable(false);
        waitForInitDialog.setSize(260, 140);
        waitForInitDialog.add(new JLabel("Startar programmet..."));
        waitForInitDialog.setLocationRelativeTo(null);
        waitForInitDialog.setBackground(Color.LIGHT_GRAY);
        Thread initDialogStarter = new Thread() {

            @Override
            public void run() {
                waitForInitDialog.setVisible(true);
            }
        };
        initDialogStarter.start();

        this.setTitle("Östersunds Squashförening - Kunder och Fakturor");
        this.setSize(WINDOW_PIXEL_WIDTH, WINDOW_PIXEL_HEIGTH); // X, Y size for the program window
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        // We manually control the window close event ourselves
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        this.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent winEvt) {

                MainGUI.this.handleProgramExitEvent();
            }
        });

        // All GUI components, in order of their presentation
        List<JComponent> components = new ArrayList<JComponent>();

        // -----------------------------  CREATE CUSTOMER COMPONENTS  ---------------------------
        // --------------------------------------------------------------------------------------

        // The customer list label
        final JLabel procJavaUrlLabel = new JLabel("Kunder");
        components.add(procJavaUrlLabel);

        // Load the customer list, which is a list box with single selection mode
        this.customerListModel = new DefaultListModel<>();
        for (CustomerType customer : this.xmlRepository.getAllCustomers()) {
            this.customerListModel.addElement(customer);
        }
        this.customerList = new JList<>(this.customerListModel);

        // Use a custom row renderer, to display a correct customer text
        this.customerList.setCellRenderer(new CustomerCellRenderer());

        this.customerList.setFixedCellWidth(198);
        this.customerList.setMaximumSize(new Dimension(200, 300));
        this.customerList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        this.customerList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        this.customerList.setVisibleRowCount(32);

        // Implement a list selection listener, so we can abort selection changes
        this.customerList.setSelectionModel(new DefaultListSelectionModel() {

            /**
             * Serial UID
             */
            private static final long serialVersionUID = -7270919403637316285L;

            @Override
            public void setSelectionInterval(int index0, int index1) {

                // Warn if leaving a modified, unsaved customer
                if (MainGUI.this.customerMasterPanel.isCustomerDirty()) {
                    int dialogResult = JOptionPane.showConfirmDialog(
                        MainGUI.this.customerMasterPanel,
                        "Det finns osparade kunduppgifter! Vill du verkligen fortsätta?",
                        "Varning",
                        JOptionPane.YES_NO_OPTION);

                    if (dialogResult != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                // If we get here, go ahead and change the selection
                super.setSelectionInterval(index0, index1);

                // Set data according to the selection
                if (MainGUI.this.customerList.isSelectionEmpty()) {
                    MainGUI.this.deleteCustomerButton.setEnabled(false);
                    MainGUI.this.customerMasterPanel.toggleEnabled(false);

                    MainGUI.this.customerMasterPanel.clearCustomer();

                } else {
                    MainGUI.this.deleteCustomerButton.setEnabled(true);
                    MainGUI.this.customerMasterPanel.toggleEnabled(true);

                    CustomerType customer = MainGUI.this.customerList.getSelectedValue();
                    MainGUI.this.customerMasterPanel.setCustomer(customer);
                }
            }
        });

        // The list goes into a scrollable container
        JScrollPane customerListScroller = new JScrollPane(this.customerList);
        customerListScroller.setAlignmentY(SwingConstants.NORTH);
        customerListScroller.setPreferredSize(new Dimension(220, 400));
        customerListScroller.setMaximumSize(new Dimension(220, 400));

        InvoicesTable invoicesTable = new InvoicesTable(null);

        // Create the customer master panel, showing a selected customer
        this.customerMasterPanel = new CustomerMasterPanel(invoicesTable);

        // The customer list and details panel goes into a single component,
        // showing the two components side-by-side:
        JPanel customerAreaPanel = new JPanel();
        customerAreaPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 16));
        customerAreaPanel.setAlignmentY(SwingConstants.NORTH);
        customerAreaPanel.setSize(new Dimension(540, 420));
        customerAreaPanel.add(customerListScroller);
        customerAreaPanel.add(this.customerMasterPanel);

        components.add(customerAreaPanel);

        JPanel invoicesSouthPanel = new JPanel();
        invoicesSouthPanel.setAlignmentY(SwingConstants.SOUTH_EAST);
        invoicesSouthPanel.setPreferredSize(new Dimension(640, 280));

        // Now prepare all graphics for the invoice table, draw it below the customer details
        JScrollPane invoicesScrollPane = new JScrollPane(
            invoicesTable,
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
        invoicesScrollPane.setPreferredSize(new Dimension(640, 256));

        invoicesSouthPanel.add(invoicesScrollPane);
        components.add(invoicesSouthPanel);

        // ------------------------------  CREATE GENERIC COMPONENTS  ---------------------------
        // --------------------------------------------------------------------------------------

        // This label is a text field for displaying
        // validation error messages to the user.
        this.validationErrorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.validationErrorLabel.setForeground(Color.RED);
        this.validationErrorLabel.setSize(400, 32); // X, Y
        this.validationErrorLabel.setMinimumSize(new Dimension(400, 32));
        this.validationErrorLabel.setMaximumSize(new Dimension(400, 32));
        this.validationErrorLabel.setPreferredSize(new Dimension(400, 32));
        components.add(this.validationErrorLabel);

        // This label is a generic text field for displaying
        // any kind of information, of error or info type.
        this.infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.infoLabel.setForeground(Color.BLUE);
        this.infoLabel.setSize(400, 32); // X, Y
        this.infoLabel.setMinimumSize(new Dimension(400, 32));
        this.infoLabel.setMaximumSize(new Dimension(400, 32));
        this.infoLabel.setPreferredSize(new Dimension(400, 32));
        components.add(this.infoLabel);

        components.add(this.createEmptyRow());

        // The function buttons is X aligned into one panel
        JPanel customerButtonsPanel = new JPanel();
        customerButtonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        customerButtonsPanel.setLayout(new BoxLayout(customerButtonsPanel, BoxLayout.X_AXIS));
        customerButtonsPanel.setBorder(BorderFactory.createEmptyBorder(2, 16, 2, 16));
        customerButtonsPanel.setSize(500, 32);
        customerButtonsPanel.setMinimumSize(new Dimension(500, 32));
        customerButtonsPanel.setMaximumSize(new Dimension(500, 32));

        // Action buttons for the customers
        this.newCustomerButton.setMinimumSize(new Dimension(130, 22));
        this.newCustomerButton.setMaximumSize(new Dimension(130, 22));
        customerButtonsPanel.add(this.newCustomerButton);

        this.newCustomerButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                if (MainGUI.this.customerMasterPanel.isCustomerDirty()) {
                    int dialogResult = JOptionPane.showConfirmDialog(
                        MainGUI.this.customerMasterPanel,
                        "Det finns osparade kunduppgifter! Vill du verkligen fortsätta?",
                        "Varning",
                        JOptionPane.YES_NO_OPTION);

                    if (dialogResult != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                // Unselect any previous selected customer
                MainGUI.this.customerList.clearSelection();

                MainGUI.this.customerMasterPanel
                    .prepareNewCustomer(MainGUI.this.xmlRepository.getNewCustomerNr());
            }
        });

        customerButtonsPanel.add(this.createEmptyRow());

        this.deleteCustomerButton.setMinimumSize(new Dimension(130, 22));
        this.deleteCustomerButton.setMaximumSize(new Dimension(130, 22));
        customerButtonsPanel.add(this.deleteCustomerButton);

        // Disable this until a customer is selected
        MainGUI.this.deleteCustomerButton.setEnabled(false);

        this.deleteCustomerButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                if (MainGUI.this.customerList.isSelectionEmpty()) {
                    MainGUI.this.printInfoText("Du måste välja en kund att radera", true, true);
                } else {
                    int dialogResult = JOptionPane.showConfirmDialog(
                        MainGUI.this.customerMasterPanel,
                        "Vill du verkligen radera kunden?",
                        "Radera?",
                        JOptionPane.YES_NO_OPTION);

                    if (dialogResult != JOptionPane.YES_OPTION) {
                        return;
                    }

                    // Remove customer from repository and GUI list
                    CustomerType customer = MainGUI.this.customerList.getSelectedValue();
                    MainGUI.this.customerListModel
                        .remove(MainGUI.this.customerList.getSelectedIndex());
                    MainGUI.this.xmlRepository.deleteCustomer(
                        UUID.fromString(customer.getCustomerInfo().getCustomerUUID()));

                    MainGUI.this.printInfoText("Kund raderad", false, true);
                }
            }
        });

        customerButtonsPanel.add(this.createEmptyRow());

        this.generateInvoicesButton.setMinimumSize(new Dimension(130, 22));
        this.generateInvoicesButton.setMaximumSize(new Dimension(130, 22));
        customerButtonsPanel.add(this.generateInvoicesButton);

        this.generateInvoicesButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                if (MainGUI.this.customerMasterPanel.isCustomerDirty()) {
                    int dialogResult = JOptionPane.showConfirmDialog(
                        MainGUI.this.customerMasterPanel,
                        "Det finns osparade kunduppgifter! Vill du verkligen fortsätta?",
                        "Varning",
                        JOptionPane.YES_NO_OPTION);

                    if (dialogResult != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                if (MainGUI.this.customerListModel.isEmpty()) {
                    MainGUI.this.printInfoText("Det finns inga kunder!", true, true);
                } else {

                    String periodString = new SubscriptionPeriod().getPeriodString();

                    int dialogResult = JOptionPane.showConfirmDialog(
                        MainGUI.this.customerMasterPanel,
                        "Vill du generera fakturafiler för alla kunder"
                            + "\n"
                            + "för perioden "
                            + periodString
                            + "?",
                        "Skapa fakturor",
                        JOptionPane.YES_NO_OPTION);

                    if (dialogResult != JOptionPane.YES_OPTION) {
                        return;
                    }

                    // Execute the invoice creator in a new thread
                    // and with a waiting indicator:
                    MainGUI.this.runInvoiceCreatorWithProgressBar();
                }
            }
        });

        // Add the buttons panel
        components.add(customerButtonsPanel);

        // Draw all components to the panel
        this.drawLayout(components);

        waitForInitDialog.setVisible(false);
    }

    // Creates a basic layout panel and add all given components to it
    private void drawLayout(List<JComponent> components) {

        // The topPanel contains all the customer components
        JPanel topPanel = new JPanel();
        topPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        for (JComponent component : components) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            topPanel.add(component);
        }

        // Add common exit and help buttons at a bottom panel:
        JPanel bottomPanel = new JPanel();
        bottomPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 40));
        bottomPanel.setSize(1000, 32);
        bottomPanel.setMinimumSize(new Dimension(1000, 32));
        bottomPanel.setMaximumSize(new Dimension(1000, 32));

        final JButton quitButton = new JButton("Avsluta");
        quitButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bottomPanel.add(quitButton);
        quitButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                MainGUI.this.handleProgramExitEvent();
            }
        });

        final StringBuilder infoMessage = new StringBuilder(1024);
        infoMessage.append("<html>");
        infoMessage.append("<br/>");
        infoMessage.append(
            "<b>Program för att hantera Östersunds Squashförenings abonnemang och fakturor.</b>");
        infoMessage.append("<br/><br/>");
        infoMessage.append(
            "Då det närmar sig ny abonnemangsperiod skall du skapa fakturor för alla kunder. Gå igenom,<br/>");
        infoMessage.append(
            "verifiera och skicka iväg alla fakturorna. Uppdatera varje kunds faktura med status 'Skickad'.<br/>");
        infoMessage.append("<br/>");
        infoMessage.append(
            "Bevaka betalningar och ändra status på fakturorna vartefter, för att hålla koll på allt.<br/>");
        infoMessage.append(
            "Om en kund i listan rödflaggas av programmet så finns det en 'aktiv' faktura som förfallit.<br/>");
        infoMessage.append(
            "Kontrollera detta och sätt status 'Skyldig' på fakturan om den visar sig vara obetald.<br/>");
        infoMessage.append("<br/>");
        infoMessage.append(
            "Om en kund avslutar sitt abonnemang så kan du sätta status 'Avbruten' på fakturan och den<br/>");
        infoMessage.append(
            "kan då aldrig rödflaggas. När en ny abonnemangsperiod infaller så rödflaggas heller inte<br/>");
        infoMessage.append("gamla fakturor från tidigare period.<br/>");
        infoMessage.append("<br/>");
        infoMessage.append("</html>");

        final JButton infoButton = new JButton("Hjälp");
        infoButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bottomPanel.add(infoButton);
        infoButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                JOptionPane.showMessageDialog(
                    null,
                    infoMessage.toString(),
                    "Hjälp",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Finally "draw" the panels on the JFrame's ContentPane
        Container contentPane = this.getContentPane();
        contentPane.add(topPanel, BorderLayout.NORTH);
        contentPane.add(bottomPanel, BorderLayout.EAST);
    }

    // Creates a filler component, e.g. empty space
    private JComponent createEmptyRow() {
        return new Box.Filler(new Dimension(20, 10), new Dimension(20, 10), new Dimension(20, 10));
    }

    // Adds a customer to the list
    protected void addCustomerToList(CustomerType customer) {
        this.customerListModel.addElement(customer);
    }

    // Repaints the customer list
    protected void repaintCustomerList() {
        this.customerList.repaint();
    }

    // Handles the whole invoice creation execution
    private void runInvoiceCreatorWithProgressBar() {

        // Prepare a progress indicator dialog
        final JDialog waitingDialog = new JDialog(MainGUI.this, "Skapar fakturor...", true);

        waitingDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        waitingDialog.setSize(300, 160);
        waitingDialog.setBackground(Color.WHITE);

        URL spinnerImageUrl = this
            .getClass()
            .getClassLoader()
            .getResource("se/osdsquash/gui/Spinner.gif");
        JLabel imageLabel = new JLabel(new ImageIcon(spinnerImageUrl));
        imageLabel.setBackground(Color.WHITE);
        imageLabel.setOpaque(true);
        imageLabel.setMinimumSize(new Dimension(70, 70));
        imageLabel.setMaximumSize(new Dimension(70, 70));

        waitingDialog.add(BorderLayout.CENTER, imageLabel);

        waitingDialog.setResizable(false);
        waitingDialog.setLocationRelativeTo(MainGUI.this);

        // Start new invoice creator worker, e.g. in a new thread
        InvoiceCreatorRunnable invoiceCreator = new InvoiceCreatorRunnable(waitingDialog);
        invoiceCreator.execute();

        // Important to display the (blocking) progress bar after work is started.
        // The worker thread will close it when done.
        waitingDialog.setVisible(true);
    }

    // Sets an info text, either as error marked or just info.
    // The text can be shown as a short notice, or permanent.
    protected void printInfoText(String text, boolean errorText, boolean shortNotice) {
        if (errorText) {
            this.infoLabel.setForeground(Color.RED);
        } else {
            this.infoLabel.setForeground(Color.BLUE);
        }

        if (!SquashUtil.isSet(text)) {
            text = "   ";
        }

        this.infoLabel.setText(text);

        // If notice, clear the text in 5 seconds...
        if (shortNotice) {
            javax.swing.Timer timer = new javax.swing.Timer(5000, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    MainGUI.this.infoLabel.setText("   ");
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    // Returns the label where all info messages are printed to
    protected JLabel getInfoTextLabel() {
        return this.infoLabel;
    }

    // Sets an error text
    protected void printValidationErrorText(String text) {

        // Always have something in the text, to make it take up space
        if (!SquashUtil.isSet(text)) {
            text = "   ";
        }
        this.validationErrorLabel.setText(text);
    }

    // Returns the label where all validation error messages are printed to
    protected JLabel getValidationErrorTextLabel() {
        return this.validationErrorLabel;
    }

    // Handles program exit and warns if there is unsaved data
    private void handleProgramExitEvent() {

        if (MainGUI.this.customerMasterPanel.isCustomerDirty()) {

            int dialogResult = JOptionPane.showConfirmDialog(
                MainGUI.this.customerMasterPanel,
                "Det finns osparade kunduppgifter! Vill du verkligen avsluta?",
                "Varning",
                JOptionPane.YES_NO_OPTION);

            if (dialogResult == JOptionPane.YES_OPTION) {
                System.exit(0);
            } else {
                // Abort exit
                return;
            }
        }

        // Nothing dirty here, just exit
        System.exit(0);
    }

    // Swing thread that executes the invoice file creation task
    protected class InvoiceCreatorRunnable extends SwingWorker<String, String> {

        private StringBuilder filesResult;
        private final JDialog waitingDialog;

        protected InvoiceCreatorRunnable(JDialog waitingDialog) {
            this.waitingDialog = waitingDialog;
        }

        // Returns the invoice creation results
        protected String getResultMessage() {
            return this.filesResult.toString();
        }

        @Override
        protected String doInBackground() throws Exception {

            // Loop all customers and generate invoices as Excel-files
            this.filesResult = new StringBuilder(128);
            this.filesResult.append("Följande fakturafiler har skapats:\n\n");

            for (String filename : MainGUI.this.xmlRepository.generateAndStoreInvoices()) {
                this.filesResult.append(filename + "\n");
            }

            return this.filesResult.toString();
        }

        @Override
        protected void done() {

            try {
                this.waitingDialog.setVisible(false);

                // If there is a customer showing, refresh it so that the
                // new invoice file list will display the new file(s).
                if (!MainGUI.this.customerList.isSelectionEmpty()) {
                    CustomerType customer = MainGUI.this.customerList.getSelectedValue();
                    MainGUI.this.customerMasterPanel.setCustomer(customer);
                }

                // Show the results from the execution
                JOptionPane.showMessageDialog(
                    MainGUI.this,
                    this.filesResult.toString(),
                    "Resultat",
                    JOptionPane.PLAIN_MESSAGE);

            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    // List renderer that displays the customer text on a list's row
    private static class CustomerCellRenderer extends DefaultListCellRenderer {

        /**
         * Serial UID
         */
        private static final long serialVersionUID = 4089774932762553899L;

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

            // We know this to be a JLabel
            JLabel label = (JLabel) super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus);

            // We know the list value is a Customer object
            CustomerType customerType = ((CustomerType) value);
            CustomerInfoType customerInfoType = customerType.getCustomerInfo();
            boolean paymentOverdue = SquashUtil.hasOverdueInvoices(customerType);

            String customerText = String.valueOf(customerInfoType.getCustomerNumber())
                + " ("
                + (customerInfoType.getFirstname() == null ? "" : customerInfoType.getFirstname())
                + (customerInfoType.getLastname() == null
                    ? ""
                    : " " + customerInfoType.getLastname())
                + ")";
            label.setText(customerText);

            if (paymentOverdue) {
                label.setForeground(Color.RED);
                label.setToolTipText("Det finns obetalda fakturor");
            } else {
                label.setToolTipText(null);
            }

            return label;
        }
    }

}
