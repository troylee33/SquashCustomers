package se.osdsquash.logger;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import se.osdsquash.common.SquashRuntimeInfo;

/**
 * Singleton class for the Squash file logger
 */
public class SquashLogger {

    private static final SquashLogger INSTANCE = new SquashLogger();

    private final Logger logger = Logger.getLogger(SquashLogger.class.getName());
    private final FileHandler fileHandler;

    private SquashLogger() {

        // Always make sure we have the data folder first:
        File dataDir = new File(SquashRuntimeInfo.getDataDirPath());
        if (!dataDir.exists()) {
            if (!dataDir.mkdir()) {
                throw new RuntimeException(
                    "FEL när data-katalogen skulle skapas, kontrollera att det gör att skriva till lagringsytan!");
            }
        }

        final String logsDirPath = SquashRuntimeInfo.getDataDirPath() + "/logs";

        File logsDir = new File(logsDirPath);
        if (!logsDir.exists()) {
            if (!logsDir.mkdir()) {
                System.out.println(
                    "FEL när logs-katalogen skulle skapas, kontrollera att det gör att skriva till lagringsytan!");
                throw new RuntimeException("Faltalt fel, logg-katalogen kunde ej skapas");
            }
        }

        // Use an appending log file, max 5 Mb per file, keeping up to 5 files
        try {
            this.fileHandler = new FileHandler(logsDirPath + "/SquashKunder.log", 5000000, 5, true);

        } catch (Exception e) {
            System.out.println("Fel vid initiering av loggfil. Felmeddelande: " + e.getMessage());
            throw new RuntimeException("Faltalt fel, logg-filen kunde ej skapas");
        }

        this.fileHandler.setFormatter(new SimpleFormatter());
        this.logger.addHandler(this.fileHandler);

        // This removes parallell logging to the Standard System out:
        this.logger.setUseParentHandlers(false);

        this.logger.log(
            Level.INFO,
            "Startar SquashKunder. Sökväg till data-katalogen är: "
                + SquashRuntimeInfo.getDataDirPath());
    }

    /**
     * Returns the singleton logger instance
     * @return The logger
     */
    public static SquashLogger getInstance() {
        return INSTANCE;
    }

    /**
     * Logs a message to file
     * @param msg Message to log
     * @param error True if error level, otherwise info
     */
    public void log(String msg, boolean error) {
        this.logger.log((error ? Level.SEVERE : Level.INFO), msg);
    }

    /**
     * Logs an exception and message to file
     * @param msg Error message to log
     * @param throwable Cause of the error
     */
    public void log(String msg, Throwable throwable) {
        this.logger.log(Level.SEVERE, msg, throwable);
    }
}
