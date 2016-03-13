package se.osdsquash.test;

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
    }
}
