package se.osdsquash.common;

import se.osdsquash.xml.XmlRepository;

/**
 * Class that can return various runtime information
 */
public abstract class SquashRuntimeInfo {

    private static final String DATA_DIR_PATH;
    static {

        // Find out where this class is executing from and build some paths
        String pathToJar = XmlRepository.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath();
        String currentDir = pathToJar.substring(0, pathToJar.lastIndexOf("/"));

        // Hack to avoid creating data dir inside a packaged Mac OS app,
        // which looks like this xxxxx/SquashKunder.app/Contents/Java/SquashCustomers.jar
        // In this case, place the data dir above the .app directory.
        if (currentDir.endsWith("Contents/Java")) {
            currentDir = pathToJar.substring(0, pathToJar.lastIndexOf("/Contents/Java"));
            currentDir = currentDir + "/..";
        }

        DATA_DIR_PATH = currentDir + "/squashdata";
    }

    /**
     * Returns the full path to the data folder where all Squash data is stored inside.
     * <p>
     * This folder is always based from where the program executes from.
     * </p>
     * 
     * @return Directory path to data dir
     */
    public static String getDataDirPath() {
        return DATA_DIR_PATH;
    }
}
