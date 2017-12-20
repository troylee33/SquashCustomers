package se.osdsquash.gui;

import java.awt.Color;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import se.osdsquash.common.SquashUtil;
import se.osdsquash.xml.XmlRepository;
import se.osdsquash.xml.jaxb.CustomerType;
import se.osdsquash.xml.jaxb.SubscriptionType;
import se.osdsquash.xml.jaxb.SubscriptionsType;

/**
 * Holds different validator classes and helper methods
 */
public abstract class ValidatorHelper {

    // RegExp to validate the track start time format (HH:mm)
    private static final Pattern START_TIME_VALIDATION_PATTERN = Pattern
        .compile("^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$");

    private static final Color INVALID_COLOR = new Color(255, 186, 186);
    private static final Color DEFAULT_COLOR = Color.WHITE;

    /**
     * Checks if given subscription(s) are not already taken by any existing 
     * customer's subscription, e.g. the time and track is taken.
     * 
     * @param customerNr The customer who's subscriptions to check
     * @param subscriptions Subscriptions to check
     * @param existingCustomers All existing customers and their subscriptions
     * 
     * @return An error message if subscription is already taken, null if all ok
     */
    public static String validateSubscriptions(
        int customerNr,
        List<SubscriptionType> subscriptions,
        List<CustomerType> existingCustomers) {

        for (SubscriptionType subscription : subscriptions) {
            for (CustomerType customer : existingCustomers) {

                // Don't compare with ourselves
                if (customerNr == customer.getCustomerInfo().getCustomerNumber()) {
                    continue;
                }

                SubscriptionsType existingSubscriptions = customer.getSubscriptions();
                if (existingSubscriptions != null) {
                    for (SubscriptionType existingSubscription : existingSubscriptions
                        .getSubscription()) {

                        // Flextimes are not validated
                        if (Boolean.TRUE.equals(existingSubscription.isFlexTime())) {
                            continue;
                        }

                        if (existingSubscription.getWeekday().equals(subscription.getWeekday())
                            && existingSubscription.getTrackNumber() == subscription
                                .getTrackNumber()
                            && existingSubscription
                                .getStartTime()
                                .equals(subscription.getStartTime())) {

                            return "Abonnemangstiden "
                                + SquashUtil.weekdayTypeToString(existingSubscription.getWeekday())
                                + " "
                                + SquashUtil
                                    .getTrackTimeFromCalendar(existingSubscription.getStartTime())
                                + " på bana "
                                + existingSubscription.getTrackNumber()
                                + " är redan upptagen av "
                                + customer.getCustomerInfo().getCustomerNumber()
                                + " ("
                                + customer.getCustomerInfo().getFirstname()
                                + ")";
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Simple validator that requires a non-whitespace value for a text field
     */
    protected static final class ValueMandatoryJTextFieldVerifier extends InputVerifier {

        private String labelName;
        private JLabel messageOutput;

        /**
         * Creates the verifier
         * @param labelName The display name of the property/input field to require a value for
         * @param messageOutput A field to write the validation error to, if error
         */
        protected ValueMandatoryJTextFieldVerifier(String labelName, JLabel messageOutput) {
            this.labelName = labelName;
            this.messageOutput = messageOutput;
        }

        @Override
        public boolean verify(JComponent input) {

            JTextField textField = (JTextField) input;
            String inputText = textField.getText();

            if (SquashUtil.isSet(inputText)) {
                this.messageOutput.setText("");
                input.setBackground(DEFAULT_COLOR);
                return true;
            } else {
                this.messageOutput.setForeground(Color.red);
                this.messageOutput.setText(this.labelName + " saknas");
                input.setBackground(INVALID_COLOR);
                return false;
            }
        }
    }

    /**
     * Input validator for a mandatory track start time text field
     */
    protected static final class StartTimeJTextFieldVerifier extends InputVerifier {

        private JLabel messageOutput;

        /**
         * Creates the verifier
         * @param messageOutput A field to write the validation error to, if error
         */
        protected StartTimeJTextFieldVerifier(JLabel messageOutput) {
            this.messageOutput = messageOutput;
        }

        @Override
        public boolean verify(JComponent input) {

            JTextField textField = (JTextField) input;
            String inputText = textField.getText();

            if (!SquashUtil.isSet(inputText)) {
                this.messageOutput.setText("Fel: En starttid måste anges i formatet HH:mm");
                input.setBackground(INVALID_COLOR);
                return false;
            } else if (START_TIME_VALIDATION_PATTERN.matcher(inputText).matches()) {
                this.messageOutput.setText("");
                input.setBackground(DEFAULT_COLOR);
                return true;
            } else {
                this.messageOutput.setText("Fel: Starttiden måste anges i formatet HH:mm");
                input.setBackground(INVALID_COLOR);
                return false;
            }
        }
    }

    /**
     * Input validator for an OPTIONAL integer/numerical-only text field.
     */
    protected static final class OptionalIntegerJTextFieldVerifier extends InputVerifier {

        private String labelName;
        private JLabel messageOutput;

        /**
         * Creates the verifier
         * @param labelName The display name of the property/input field
         * @param messageOutput A field to write the validation error to, if error
         */
        protected OptionalIntegerJTextFieldVerifier(String labelName, JLabel messageOutput) {
            this.labelName = labelName;
            this.messageOutput = messageOutput;
        }

        @Override
        public boolean verify(JComponent input) {

            JTextField textField = (JTextField) input;
            String inputText = textField.getText();

            // Check validity - only if we have a value - by try parsing to an integer
            if (inputText != null && inputText.trim().length() > 0) {
                try {
                    Integer.parseInt(inputText);

                    // All ok here, the parsing went fine
                    this.messageOutput.setText("");
                    input.setBackground(DEFAULT_COLOR);
                    return true;

                } catch (NumberFormatException e) {
                    input.setBackground(INVALID_COLOR);
                    this.messageOutput
                        .setText("Fel: " + this.labelName + " måste anges med endast siffror");
                    return false;
                }
            } else {
                // All ok, since there is no value
                this.messageOutput.setText("");
                input.setBackground(DEFAULT_COLOR);
                return true;
            }
        }
    }

    /**
     * Input validator specifially for the mandatory, unique customer number.
     */
    protected static final class CustomerNrJTextFieldVerifier extends InputVerifier {

        private final String labelName = "KundNr";
        private String originalCustomerNr;
        private JLabel messageOutput;

        /**
         * Creates the verifier
         * @param originalCustomerNr Customer nr before any changes are made to it
         * @param messageOutput A field to write the validation error to, if error
         */
        protected CustomerNrJTextFieldVerifier(String originalCustomerNr, JLabel messageOutput) {
            this.originalCustomerNr = originalCustomerNr;
            this.messageOutput = messageOutput;
        }

        @Override
        public boolean verify(JComponent input) {

            JTextField textField = (JTextField) input;
            String inputText = textField.getText();

            // Check validity by try parsing to an integer first
            try {
                int customerNr = Integer.parseInt(inputText);

                // Verify that the number is not already taken,
                // but don't compare with ourselves!
                if (this.originalCustomerNr != null && this.originalCustomerNr.equals(inputText)) {
                    this.messageOutput.setText("");
                    input.setBackground(DEFAULT_COLOR);
                    return true;
                }

                int currentCustomerNr = XmlRepository.getInstance().getCurrentCustomerNr();

                if (customerNr > currentCustomerNr) {
                    this.messageOutput.setText("");
                    input.setBackground(DEFAULT_COLOR);
                    return true;
                } else {
                    input.setBackground(INVALID_COLOR);
                    this.messageOutput.setText(
                        "Fel: "
                            + this.labelName
                            + " "
                            + String.valueOf(customerNr)
                            + " är upptaget, det första lediga numret är "
                            + String.valueOf(currentCustomerNr + 1));
                    return false;
                }

            } catch (NumberFormatException e) {
                input.setBackground(INVALID_COLOR);
                this.messageOutput
                    .setText("Fel: " + this.labelName + " måste anges med endast siffror");
                return false;
            }
        }
    }
}
