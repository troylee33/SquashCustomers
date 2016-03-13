package se.osdsquash.gui;

import java.awt.Color;
import java.util.regex.Pattern;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import se.osdsquash.common.SquashUtil;

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
                this.messageOutput.setText("Fel: " + this.labelName + " måste anges");
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
     * Input validator for a mandatory integer/numerical-only text field.
     */
    protected static final class NumericalJTextFieldVerifier extends InputVerifier {

        private String labelName;
        private JLabel messageOutput;

        /**
         * Creates the verifier
         * @param labelName The display name of the property/input field to require a value for
         * @param messageOutput A field to write the validation error to, if error
         */
        protected NumericalJTextFieldVerifier(String labelName, JLabel messageOutput) {
            this.labelName = labelName;
            this.messageOutput = messageOutput;
        }

        @Override
        public boolean verify(JComponent input) {

            JTextField textField = (JTextField) input;
            String inputText = textField.getText();

            // Check validity by try parsing to an integer
            try {
                Integer.parseInt(inputText);
                this.messageOutput.setText("");
                input.setBackground(DEFAULT_COLOR);
                return true;

            } catch (NumberFormatException e) {
                input.setBackground(INVALID_COLOR);
                this.messageOutput
                    .setText("Fel: " + this.labelName + " måste anges med endast siffror");
                return false;
            }
        }
    }
}
