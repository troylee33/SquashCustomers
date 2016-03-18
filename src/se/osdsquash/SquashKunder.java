package se.osdsquash;

import java.awt.EventQueue;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import se.osdsquash.gui.MainGUI;

/**
 * Main class that initializes and starts the program.
 * This class is specified in the runnable jar package.
 */
public class SquashKunder {

    public static void main(String[] args) {

        // Set generic error handling, showing all errors
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable error) {

                System.err.println("Oväntat fel i programmet:");
                error.printStackTrace();

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
