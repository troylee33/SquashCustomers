package se.osdsquash;

import java.awt.EventQueue;

import se.osdsquash.gui.MainGUI;

/**
 * Main class that initializes and starts the program.
 * This class is specified in the runnable jar package.
 */
public class SquashKunder {

    public static void main(String[] args) {

        // Creates the JFrame GUI and displays it
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                MainGUI guiClient = MainGUI.getInstance();
                guiClient.setVisible(true);
            }
        });
    }
}
