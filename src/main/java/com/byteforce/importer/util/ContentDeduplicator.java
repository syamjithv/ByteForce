package com.byteforce.importer.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance deduplication engine for placement questions.
 * Employs normalized textual fingerprinting and SHA-256 content hashing.
 */
public class ContentDeduplicator {

    private final Set<String> seenHashes = ConcurrentHashMap.newKeySet();

    /**
     * Normalizes text by lowercasing, stripping special punctuation, and collapsing whitespace.
     */
    public static String normalizeText(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return input.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Computes a deterministic SHA-256 hash of the normalized question text.
     */
    public static String computeHash(String text) {
        String normalized = normalizeText(text);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Pre-seeds existing hashes from the database so that re-runs or overlapping batches
     * are strictly idempotent.
     */
    public void registerExistingHash(String hash) {
        if (hash != null && !hash.isBlank()) {
            seenHashes.add(hash.trim().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Checks if a question hash is unique. If unique, registers it and returns true.
     * If already seen, returns false (duplicate).
     */
    public boolean checkAndRegister(String hash) {
        if (hash == null || hash.isBlank()) {
            return false;
        }
        return seenHashes.add(hash.trim().toLowerCase(Locale.ROOT));
    }

    public int size() {
        return seenHashes.size();
    }
}
