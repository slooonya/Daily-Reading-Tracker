package com.cpt202.dailyreadingtracker.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class SecretKeyGenerator {
    
    private static final int DEFAULT_KEY_LENGTH = 32;

    public static String generateSecretKey() {
        return generateSecretKey(DEFAULT_KEY_LENGTH);
    }

    public static String generateSecretKey(int keyLength){
        if (keyLength <= 0)
            throw new IllegalArgumentException("Key length must be positive");

        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] keyBytes = new byte[keyLength];
            random.nextBytes(keyBytes);

            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (Exception e) {
            throw new SecurityException("Failed to generate secret key", e);
        }
    }
}
