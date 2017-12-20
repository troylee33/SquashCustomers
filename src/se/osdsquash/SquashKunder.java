package se.osdsquash;

import java.awt.EventQueue;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import se.osdsquash.gui.MainGUI;
import se.osdsquash.logger.SquashLogger;
import se.osdsquash.mail.MailHandler;

/**
 * Main class that initializes and starts the program.
 * This class is specified in the runnable jar package.
 */
public class SquashKunder {

    public static void main(String[] args) {

        final SquashLogger logger = SquashLogger.getInstance();

        // Set generic error handling, showing all errors
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable error) {

                error.printStackTrace();

                logger.log("Oväntat fel i programmet, felmeddelande: " + error.getMessage(), error);

                JOptionPane.showMessageDialog(
                    null,
                    "Ett oväntat fel uppstod i programmet. Felmeddelande: "
                        + "\n"
                        + error.getMessage(),
                    "Fel",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        // Try setting the 'Nimbus' Look & Feel, if present
        try {
            for (LookAndFeelInfo lfInfo : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equalsIgnoreCase(lfInfo.getName())) {
                    UIManager.setLookAndFeel(lfInfo.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            // Ok, fallback to default one...
        }

        // Check for older temp files to clean up
        MailHandler.deleteMailTempFiles();

        // Creates the Main GUI JFrame and display it
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                MainGUI guiClient = MainGUI.getInstance();
                guiClient.setVisible(true);
            }
        });
    }
}
