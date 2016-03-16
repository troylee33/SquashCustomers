package se.osdsquash.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.UUID;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import se.osdsquash.common.SquashUtil;
import se.osdsquash.xml.XmlRepository;
import se.osdsquash.xml.jaxb.CustomerInfoType;
import se.osdsquash.xml.jaxb.CustomerType;
import se.osdsquash.xml.jaxb.InvoiceType;
import se.osdsquash.xml.jaxb.InvoicesType;
import se.osdsquash.xml.jaxb.SubscriptionType;
import se.osdsquash.xml.jaxb.SubscriptionsType;

/**
 * A Panel holding customer details
 */
public class CustomerDetailsPanel extends JPanel {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -5061672333810391510L;

    private XmlRepository xmlRepository;

    // The current customer shown in the panel:
    private UUID customerUUID;

    // Original CustomerNr, before an edit is made
    private String customerNr;

    // All components in the Panel:
    private JLabel kundNrLabel;
    private JTextField kundNrTextField;

    private JCheckBox foretagCheckbox;
    private JLabel emptyFillerLabel;

    private JLabel fornamnLabel;
    private JTextField fornamnTextField;
    private JLabel efternamnLabel;
    private JTextField efternamnTextField;
    private JLabel gatuAdressLabel;
    private JTextField gatuAdressTextField;
    private JLabel postNrLabel;
    private JTextField postNrTextField;
    private JLabel ortLabel;
    private JTextField ortTextField;
    private JLabel telefonLabel;
    private JTextField telefonTextField;
    private JLabel eMailLabel;
    private JTextField eMailTextField;

    // Tables holding the subscriptions and invoices
    private SubscriptionsTable subscriptionsTable;
    private InvoicesTable invoicesTable;

    private JButton saveButton;

    // True if there are unsaved changes made to the customer
    private boolean customerDirty;

    /**
     * Constructor initializing an empty and disabled customer details panel
     * 
     * @param subscriptionsTable Reference to the subscriptions
     * @param invoicesTable Reference to the invoices
     * @param mainGUI The parent "owner" panel
     */
    protected CustomerDetailsPanel(
        SubscriptionsTable subscriptionsTable,
        InvoicesTable invoicesTable) {

        super();
        this.xmlRepository = XmlRepository.getInstance();
        this.subscriptionsTable = subscriptionsTable;
        this.invoicesTable = invoicesTable;
        this.initPanel();
    }

    private void initPanel() {

        this.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.setMinimumSize(new Dimension(580, 400));
        this.setMaximumSize(new Dimension(580, 400));

        SpringLayout springLayout = new SpringLayout();
        this.setLayout(springLayout);

        // Prepare all input components, two of them per row.
        this.kundNrLabel = new JLabel("Kundnr: ");
        this.add(this.kundNrLabel);
        this.kundNrTextField = new CustomerTextField("");
        this.add(this.kundNrTextField);

        this.foretagCheckbox = new JCheckBox("Företagskund", false);
        this.add(this.foretagCheckbox);
        this.emptyFillerLabel = new JLabel(" ");
        this.add(this.emptyFillerLabel);

        this.fornamnLabel = new JLabel("Förnamn: ");
        this.add(this.fornamnLabel);
        this.fornamnTextField = new CustomerTextField("");
        this.add(this.fornamnTextField);

        this.efternamnLabel = new JLabel("Efternamn: ");
        this.add(this.efternamnLabel);
        this.efternamnTextField = new CustomerTextField("");
        this.add(this.efternamnTextField);

        this.gatuAdressLabel = new JLabel("Gatuadress: ");
        this.add(this.gatuAdressLabel);
        this.gatuAdressTextField = new CustomerTextField("");
        this.add(this.gatuAdressTextField);

        this.postNrLabel = new JLabel("Postnr: ");
        this.add(this.postNrLabel);
        this.postNrTextField = new CustomerTextField("");
        this.add(this.postNrTextField);

        this.ortLabel = new JLabel("Ort: ");
        this.add(this.ortLabel);
        this.ortTextField = new CustomerTextField("");
        this.add(this.ortTextField);

        this.telefonLabel = new JLabel("Telefon: ");
        this.add(this.telefonLabel);
        this.telefonTextField = new CustomerTextField("");
        this.add(this.telefonTextField);

        this.eMailLabel = new JLabel("E-mail: ");
        this.add(this.eMailLabel);
        this.eMailTextField = new CustomerTextField("");
        this.add(this.eMailTextField);

        // Empty "separator" row:
        this.add(new JLabel(" "));
        this.add(new JLabel(" "));

        // The validate and save button:
        this.saveButton = new JButton("Spara uppgifter");
        this.saveButton.setMinimumSize(new Dimension(120, 22));
        this.saveButton.setMaximumSize(new Dimension(120, 22));
        this.add(this.saveButton);

        this.saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                // Check if we have any validation errors and abort the save
                String errorMessage = MainGUI.getInstance().getValidationErrorTextLabel().getText();

                if (SquashUtil.isSet(errorMessage)) {
                    JOptionPane.showMessageDialog(
                        CustomerDetailsPanel.this,
                        "Kunduppgifterna kan inte sparas eftersom det finns felaktiga uppgifter:"
                            + "\n\n"
                            + errorMessage,
                        "Ogiltiga uppgifter",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // If no UUID, this is a new customer, otherwise it should exist
                CustomerType customer;
                boolean newCustomer;

                if (CustomerDetailsPanel.this.customerUUID == null) {
                    newCustomer = true;

                    // One may slip through validation if nothing was ever entered, because that
                    // never triggers any validation in the fields. Do a basic validation check.
                    if ((!CustomerDetailsPanel.this.kundNrTextField
                        .getInputVerifier()
                        .shouldYieldFocus(CustomerDetailsPanel.this.kundNrTextField))
                        || (!CustomerDetailsPanel.this.fornamnTextField
                            .getInputVerifier()
                            .shouldYieldFocus(CustomerDetailsPanel.this.fornamnTextField))) {

                        JOptionPane.showMessageDialog(
                            CustomerDetailsPanel.this,
                            "Kunden kan inte skapas eftersom det saknas obligatoriska uppgifter!"
                                + "\n",
                            "Ogiltiga uppgifter",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    customer = CustomerDetailsPanel.this.xmlRepository.getNewCustomer();
                    CustomerDetailsPanel.this.customerUUID = UUID.randomUUID();
                    customer.getCustomerInfo().setCustomerUUID(
                        CustomerDetailsPanel.this.customerUUID.toString());

                } else {
                    newCustomer = false;
                    customer = CustomerDetailsPanel.this.xmlRepository
                        .getCustomer(CustomerDetailsPanel.this.customerUUID);
                }

                // Read all input fields and set values to the customer object
                CustomerInfoType customerInfo = customer.getCustomerInfo();
                customerInfo.setCity(CustomerDetailsPanel.this.ortTextField.getText());
                customerInfo.setCompany(CustomerDetailsPanel.this.foretagCheckbox.isSelected());
                customerInfo.setCustomerNumber(
                    Integer.valueOf(CustomerDetailsPanel.this.kundNrTextField.getText()));
                customerInfo.setEmail(CustomerDetailsPanel.this.eMailTextField.getText());
                customerInfo.setFirstname(CustomerDetailsPanel.this.fornamnTextField.getText());
                customerInfo.setLastname(CustomerDetailsPanel.this.efternamnTextField.getText());
                customerInfo.setPostalCode(CustomerDetailsPanel.this.postNrTextField.getText());
                customerInfo.setStreet(CustomerDetailsPanel.this.gatuAdressTextField.getText());
                customerInfo.setTelephone(CustomerDetailsPanel.this.telefonTextField.getText());

                // Simply take what's in the subscriptions table and set that
                List<SubscriptionType> subscriptionsFromTable = CustomerDetailsPanel.this.subscriptionsTable
                    .getSubscriptions();
                CustomerDetailsPanel.this.xmlRepository
                    .setSubscriptionsToCustomer(customer, subscriptionsFromTable);

                // Simply take what's in the invoices table and set that
                List<InvoiceType> invoicesFromTable = CustomerDetailsPanel.this.invoicesTable
                    .getInvoices();
                CustomerDetailsPanel.this.xmlRepository
                    .setInvoicesToCustomer(customer, invoicesFromTable);

                // Save the customer object
                CustomerDetailsPanel.this.xmlRepository.saveCustomer(customer);
                String saveMessage;

                // We must add the new customer to the customer list...
                if (newCustomer) {
                    MainGUI.getInstance().addCustomerToList(customer);
                    saveMessage = "Ny kund sparad";
                } else {
                    // ...or just repaint the list
                    MainGUI.getInstance().repaintCustomerList();
                    saveMessage = "Kunduppgifter sparade";
                }

                MainGUI.getInstance().printInfoText(saveMessage, false, true);

                CustomerDetailsPanel.this.customerDirty = false;
            }
        });

        this.add(new JLabel(" "));

        // By default, all fields are empty and everything is disabled
        this.toggleFields(true, false);

        // NOTE: The above components much be even with the nr of rows and columns here:
        SpringLayoutUtil.makeGrid(
            this,
            11, // Rows
            2, // Cols
            10, // X location
            10, // Y location
            4, // X padding
            2); // Y padding

        this.add(new JLabel(" "));

        // Add dirty listeners for all input components, so we know when something has been modified
        this.registerDirtyListeners();

        // Start with customer not dirty
        this.customerDirty = false;
    }

    private void registerDirtyListeners() {

        // Add data change listeners for all our different input components
        for (Component component : super.getComponents()) {
            if (component instanceof JTextField) {
                ((JTextField) component).getDocument().addDocumentListener(new DirtyListener());
            } else if (component instanceof JCheckBox) {
                ((JCheckBox) component).addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        CustomerDetailsPanel.this.customerDirty = true;
                    }
                });
            }
        }

        // The subscriptions table
        this.subscriptionsTable
            .getSubscriptionsTableModel()
            .addTableModelListener(new TableModelListener() {

                @Override
                public void tableChanged(TableModelEvent evt) {
                    CustomerDetailsPanel.this.customerDirty = true;
                }
            });

        // The invoices table
        this.invoicesTable.getInvoicesTableModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent evt) {
                CustomerDetailsPanel.this.customerDirty = true;
            }
        });
    }

    /**
     * Clears customer data from the panel, if any
     */
    protected void clearCustomer() {

        this.customerUUID = null;
        this.customerNr = null;

        // Clear all inputs and disable them
        this.toggleFields(true, false);

        // Make customer not dirty after everything is set
        this.customerDirty = false;
    }

    /**
     * Sets given customer data to the panel
     * @param customerType An existing customer
     */
    protected void setCustomer(CustomerType customerType) {

        CustomerInfoType customerInfo = customerType.getCustomerInfo();
        this.customerUUID = UUID.fromString(customerInfo.getCustomerUUID());
        this.customerNr = String.valueOf(customerInfo.getCustomerNumber());

        // Enable all inputs
        this.toggleFields(false, true);

        // Set all input values
        this.kundNrTextField.setText(String.valueOf(customerInfo.getCustomerNumber()));

        this.foretagCheckbox.setSelected(customerInfo.isCompany());

        this.fornamnTextField.setText(customerInfo.getFirstname());
        this.efternamnTextField.setText(customerInfo.getLastname());
        this.gatuAdressTextField.setText(customerInfo.getStreet());
        this.postNrTextField.setText(String.valueOf(customerInfo.getPostalCode()));
        this.ortTextField.setText(customerInfo.getCity());
        this.telefonTextField.setText(customerInfo.getTelephone());
        this.eMailTextField.setText(customerInfo.getEmail());

        this.kundNrTextField.requestFocus();

        // Set all subscriptions
        this.subscriptionsTable.clearSubscriptions();

        SubscriptionsType subscriptionsType = customerType.getSubscriptions();
        if (subscriptionsType != null) {
            for (SubscriptionType subscriptionType : subscriptionsType.getSubscription()) {
                this.subscriptionsTable.addSubscription(subscriptionType);
            }
        }

        // Set all invoices
        InvoicesType invoicesType = customerType.getInvoices();
        if (invoicesType != null) {
            this.invoicesTable.setInvoices(invoicesType.getInvoice());
        }

        // Sets all input field validators
        this.initValidators();

        // We must re-evaluate the validators to get the correct status
        for (Component component : this.getComponents()) {
            if (component instanceof JTextField) {
                JTextField textField = ((JTextField) component);
                InputVerifier inputVerifier = textField.getInputVerifier();
                if (inputVerifier != null) {
                    inputVerifier.shouldYieldFocus(textField);
                }
            }
        }

        // Repaint now that things are populated and done:
        this.subscriptionsTable.repaint();
        this.invoicesTable.repaint();

        // Make customer not dirty after everything is set
        this.customerDirty = false;
    }

    /**
     * Clears the panel and prepares for a new customer to be entered
     * @param customerNr A new, unique customer number to use as default nr
     */
    protected void prepareNewCustomer(int customerNr) {

        // Clear all inputs and enable them
        this.toggleFields(true, true);

        this.customerUUID = null;
        this.customerNr = null;
        this.kundNrTextField.setText(String.valueOf(customerNr));

        // Sets all input field validators
        this.initValidators();

        this.kundNrTextField.requestFocus();

        // Make customer not dirty after everything is set
        this.customerDirty = false;
    }

    /**
     * Returns true if there is a customer set and it's details has
     * been modified and NOT saved
     * 
     * @return True if there are unsaved changes to the customer
     */
    protected boolean isCustomerDirty() {
        return this.customerDirty;
    }

    // Optional clearing of all input fields and enable or disable them
    private void toggleFields(boolean clearValues, boolean enableFields) {

        for (Component component : super.getComponents()) {
            if (component instanceof JLabel) {
                component.setEnabled(enableFields);
            } else if (component instanceof JButton) {
                component.setEnabled(enableFields);
            } else if (component instanceof JTextField) {
                if (clearValues) {
                    ((JTextField) component).setText("");
                }
                ((JTextField) component).setEnabled(enableFields);
            } else if (component instanceof JCheckBox) {
                if (clearValues) {
                    ((JCheckBox) component).setSelected(false);
                }
                ((JCheckBox) component).setEnabled(enableFields);
            }
        }

        // The tables are not "added" to this panel, we have references to them:
        if (clearValues) {
            this.subscriptionsTable.clearSubscriptions();
            this.invoicesTable.clearInvoices();
        }
        this.subscriptionsTable.setEnabled(enableFields);
        this.invoicesTable.setEnabled(enableFields);

        // Repaint the tables to make sure they are updated
        this.subscriptionsTable.repaint();
        this.invoicesTable.repaint();
    }

    // Resets/creates all input field validators
    private void initValidators() {

        // Validation errors are written to a main panel field
        MainGUI mainGui = MainGUI.getInstance();

        this.kundNrTextField.setInputVerifier(
            new ValidatorHelper.CustomerNrJTextFieldVerifier(
                this.customerNr,
                mainGui.getValidationErrorTextLabel()));

        this.fornamnTextField.setInputVerifier(
            new ValidatorHelper.ValueMandatoryJTextFieldVerifier(
                "Förnamn",
                mainGui.getValidationErrorTextLabel()));

        this.gatuAdressTextField.setInputVerifier(
            new ValidatorHelper.ValueMandatoryJTextFieldVerifier(
                "Gatuadress",
                mainGui.getValidationErrorTextLabel()));

        this.postNrTextField.setInputVerifier(
            new ValidatorHelper.ValueMandatoryJTextFieldVerifier(
                "Postnr",
                mainGui.getValidationErrorTextLabel()));

        this.ortTextField.setInputVerifier(
            new ValidatorHelper.ValueMandatoryJTextFieldVerifier(
                "Ort",
                mainGui.getValidationErrorTextLabel()));
    }

    // Default sizes text field
    private static final class CustomerTextField extends JTextField {

        /**
         * Serial UID
         */
        private static final long serialVersionUID = -4963309556324245707L;

        private static final int defaultNrOfColumns = 20;

        protected CustomerTextField(String textValue) {
            super(textValue, defaultNrOfColumns);
        }
    }

    // Change listener that marks the customer "dirty", e.g. modified
    private final class DirtyListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            CustomerDetailsPanel.this.customerDirty = true;
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            CustomerDetailsPanel.this.customerDirty = true;
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            CustomerDetailsPanel.this.customerDirty = true;
        }
    }
}
