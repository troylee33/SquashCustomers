package se.osdsquash.test;

import java.io.File;
import java.util.UUID;

/**
 * Test class
 */
public class UuidGenerator {

    public static void main(String[] args) {

        System.out.println("Generated UUIDs:");
        for (int i = 0; i < 5; i++) {
            System.out.println(UUID.randomUUID().toString());
        }

        String testPath = "/Test/sokvag/jarfilen.jar";
        System.out.println(testPath.substring(0, testPath.lastIndexOf(File.separator)));
    }
}
