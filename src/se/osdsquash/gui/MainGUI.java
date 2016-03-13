package se.osdsquash.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

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

    // Execution thread for long running tasks
    private Thread executionThread;

    // Singleton reference to the repository:
    private XmlRepository xmlRepository;

    private static final int WINDOW_PIXEL_WIDTH = 1320;
    private static final int WINDOW_PIXEL_HEIGTH = 800;

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

        components.add(this.createEmptyRow());

        // Load the customer list, which is a list box with single selection mode
        this.customerListModel = new DefaultListModel<>();
        for (CustomerType customerType : this.xmlRepository.getAllCustomers()) {
            this.customerListModel.addElement(customerType);
        }
        this.customerList = new JList<>(this.customerListModel);

        // Use a custom row renderer, to display a correct customer text
        this.customerList.setCellRenderer(new CustomerCellRenderer());

        this.customerList.setFixedCellWidth(198);
        this.customerList.setPreferredSize(new Dimension(200, 300));
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

                    CustomerType customerType = MainGUI.this.customerList.getSelectedValue();
                    MainGUI.this.customerMasterPanel.setCustomer(customerType);
                }
            }
        });

        // The list goes into a scrollable container
        JScrollPane customerListScroller = new JScrollPane(this.customerList);
        customerListScroller.setPreferredSize(new Dimension(220, 340));
        customerListScroller.setMaximumSize(new Dimension(220, 340));

        // Create the customer master panel, showing a selected customer
        this.customerMasterPanel = new CustomerMasterPanel(this.xmlRepository);

        // The customer list and details panel goes into a single component,
        // showing the two components side-by-side:
        JPanel customerAreaPanel = new JPanel(new FlowLayout());
        customerAreaPanel.setSize(new Dimension(1300, 600));
        customerAreaPanel.add(customerListScroller);
        customerAreaPanel.add(this.customerMasterPanel);

        components.add(customerAreaPanel);

        // ------------------------------  CREATE GENERIC COMPONENTS  ---------------------------
        // --------------------------------------------------------------------------------------

        components.add(this.createEmptyRow());

        // This label is a text field for displaying
        // validation error messages to the user.
        this.validationErrorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.validationErrorLabel.setForeground(Color.RED);
        this.validationErrorLabel.setSize(630, 22); // X, Y
        this.validationErrorLabel.setMinimumSize(new Dimension(630, 22));
        this.validationErrorLabel.setMaximumSize(new Dimension(630, 22));
        components.add(this.validationErrorLabel);

        // This label is a generic text field for displaying
        // any kind of information, of error or info type.
        this.infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.infoLabel.setForeground(Color.BLUE);
        this.infoLabel.setSize(630, 22); // X, Y
        this.infoLabel.setMinimumSize(new Dimension(630, 22));
        this.infoLabel.setMaximumSize(new Dimension(630, 22));
        components.add(this.infoLabel);

        components.add(this.createEmptyRow());

        // Action buttons for the customers
        this.newCustomerButton.setMinimumSize(new Dimension(130, 22));
        this.newCustomerButton.setMaximumSize(new Dimension(130, 22));
        components.add(this.newCustomerButton);

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

        this.deleteCustomerButton.setMinimumSize(new Dimension(130, 22));
        this.deleteCustomerButton.setMaximumSize(new Dimension(130, 22));
        components.add(this.deleteCustomerButton);

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
                        "Vill du verkligen radera denna kund?",
                        "Radera?",
                        JOptionPane.YES_NO_OPTION);

                    if (dialogResult != JOptionPane.YES_OPTION) {
                        return;
                    }

                    // Remove customer from repository and GUI list
                    CustomerType customerType = MainGUI.this.customerList.getSelectedValue();
                    MainGUI.this.customerListModel
                        .remove(MainGUI.this.customerList.getSelectedIndex());
                    MainGUI.this.xmlRepository.deleteCustomer(
                        UUID.fromString(customerType.getCustomerInfo().getCustomerUUID()));

                    MainGUI.this.printInfoText("Kund raderad", false, true);
                }
            }
        });

        components.add(this.createEmptyRow());

        this.generateInvoicesButton.setMinimumSize(new Dimension(130, 22));
        this.generateInvoicesButton.setMaximumSize(new Dimension(130, 22));
        components.add(this.generateInvoicesButton);

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

                    // Start new invoice creator thread
                    InvoiceCreatorRunnable invoiceCreator = new InvoiceCreatorRunnable();
                    MainGUI.this.executionThread = new Thread(invoiceCreator);
                    MainGUI.this.executionThread.start();

                    // Start a progress bar thread until the invoice thread is done
                    new ProgressDrawerRunnable(invoiceCreator).run();
                }
            }
        });

        // Draw all components to the panel
        this.drawLayout(components);
    }

    // Creates a basic layout panel and add all given components to it
    private void drawLayout(List<JComponent> components) {

        // The topPanel contains all the customer components
        JPanel topPanel = new JPanel();
        topPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        for (JComponent component : components) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            topPanel.add(component);
        }

        // Add exit and help buttons at a bottom panel:
        JPanel bottomPanel = new JPanel();
        bottomPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 16, 16, 16));
        bottomPanel.setSize(1140, 50);
        bottomPanel.setMinimumSize(new Dimension(1140, 50));
        bottomPanel.setMaximumSize(new Dimension(1140, 50));

        final JButton quitButton = new JButton("Avsluta");
        quitButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bottomPanel.add(quitButton);
        quitButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                MainGUI.this.handleProgramExitEvent();
            }
        });

        bottomPanel.add(this.createEmptyRow());

        final JButton infoButton = new JButton("Info");
        infoButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bottomPanel.add(infoButton);
        final String infoMessage = "Program för att hantera Östersunds Squashförenings abonnemang och fakturor.";

        infoButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                JOptionPane
                    .showMessageDialog(null, infoMessage, "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Finally "draw" the panels on the JFrame's ContentPane
        Container contentPane = this.getContentPane();
        contentPane.add(topPanel, BorderLayout.NORTH);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
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

    // Thread that executes the invoice file creation task
    protected class InvoiceCreatorRunnable implements Runnable {

        private StringBuilder filesResult;

        @Override
        public void run() {

            // Loop all customers and generate invoices as Excel-files
            this.filesResult = new StringBuilder(128);
            this.filesResult.append("Följande fakturafiler har skapats:\n\n");

            for (String filename : MainGUI.this.xmlRepository.generateAndStoreInvoices()) {
                this.filesResult.append(filename + "\n");
            }

            return;
        }

        // Returns the invoice creation results
        protected String getResultMessage() {
            return this.filesResult.toString();
        }
    }

    // Thread that shows a progress bar until the execution thread is done
    protected class ProgressDrawerRunnable implements Runnable {

        // Reference to the running invoice creation task
        private InvoiceCreatorRunnable invoiceCreator;

        protected ProgressDrawerRunnable(InvoiceCreatorRunnable invoiceCreator) {
            this.invoiceCreator = invoiceCreator;
        }

        @Override
        public void run() {

            final JProgressBar progressBar = new JProgressBar(0, 10);

            final JDialog waitingDialog = new JDialog(MainGUI.this, "Skapar fakturor...", true);
            waitingDialog.add(BorderLayout.CENTER, progressBar);
            waitingDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            waitingDialog.setSize(300, 100);
            waitingDialog.setLocationRelativeTo(MainGUI.this);

            // Poll for the execution thread to finish
            int progressValue = 1;
            while (MainGUI.this.executionThread.isAlive()) {

                // We must show the dialog in a new thread,
                // or it will just block the current thread until closed.
                Thread showProgressThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        waitingDialog.setVisible(true);
                    }
                });
                showProgressThread.start();

                try {
                    progressBar.setValue(progressValue);

                    MainGUI.this.executionThread.join(500);

                    // Check if finished, then clear and quit this progress bar
                    if (!MainGUI.this.executionThread.isAlive()) {

                        waitingDialog.setVisible(false);

                        // Show the result from the execution thread
                        JOptionPane.showMessageDialog(
                            MainGUI.this,
                            this.invoiceCreator.getResultMessage(),
                            "Resultat",
                            JOptionPane.PLAIN_MESSAGE);

                        return;
                    }

                    progressValue++;

                } catch (Exception exception) {
                    // Try to just continue...
                    System.err.println(
                        "Varning: Exception i progress-indikatorn, försöker fortsätta ändå...");
                    exception.printStackTrace();
                }
            }
        }
    }

    // Sets an info text, either as error marked or just info.
    // The text can be shown as a short notice, or permanent.
    protected void printInfoText(String text, boolean errorText, boolean shortNotice) {
        if (errorText) {
            this.infoLabel.setForeground(Color.RED);
        } else {
            this.infoLabel.setForeground(Color.BLUE);
        }

        this.infoLabel.setText(text);

        // If notice, clear the text in 5 seconds...
        if (shortNotice) {
            javax.swing.Timer timer = new javax.swing.Timer(5000, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    MainGUI.this.infoLabel.setText(" ");
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

    // List renderer that displays the customer text
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
            CustomerInfoType customerInfoType = ((CustomerType) value).getCustomerInfo();
            String customerText = String.valueOf(customerInfoType.getCustomerNumber())
                + " ("
                + (customerInfoType.getFirstname() == null ? "" : customerInfoType.getFirstname())
                + (customerInfoType.getLastname() == null
                    ? ""
                    : " " + customerInfoType.getLastname())
                + ")";
            label.setText(customerText);

            return label;
        }
    }

}
