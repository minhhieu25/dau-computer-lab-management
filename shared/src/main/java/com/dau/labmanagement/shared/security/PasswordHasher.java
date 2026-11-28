package com.dau.labmanagement.shared.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * PBKDF2-SHA256 password hashing shared by the Server login screen (SC_001),
 * the Client identity check (SC_002) and account management (SC_005).
 *
 * <p>Stored format, see {@code V2__seed.sql}:
 * {@code pbkdf2-sha256$<iterations>$<salt-base64>$<key-base64>} with a 16-byte
 * salt and a 256-bit key. Passwords are never stored or logged in clear text
 * (Docs/coding_convention.md, section 3).</p>
 */
public final class PasswordHasher {

    /** Prefix identifying the hashing scheme in the {@code password_hash} column. */
    public static final String ALGORITHM = "pbkdf2-sha256";

    /** Work factor used for new hashes; matches the seed admin account. */
    public static final int DEFAULT_ITERATIONS = 210_000;

    /** Salt length in bytes. */
    public static final int SALT_BYTES = 16;

    /** Derived key length in bits. */
    public static final int KEY_BITS = 256;

    private static final int MIN_ITERATIONS = 1;
    private static final int MAX_ITERATIONS = 1_000_000;
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final byte[] DECOY_SALT = new byte[SALT_BYTES];
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Utility class, no instances.
    }

    /**
     * Hashes a password with a fresh random salt and the default work factor.
     *
     * @param password the password characters
     * @return the encoded hash in the project storage format
     */
    public static String hash(char[] password) {
        return hash(password, DEFAULT_ITERATIONS);
    }

    /**
     * Hashes a password with a fresh random salt.
     *
     * @param password   the password characters
     * @param iterations the PBKDF2 work factor
     * @return the encoded hash in the project storage format
     * @throws IllegalArgumentException if the password is empty or the work factor is out of range
     */
    public static String hash(char[] password, int iterations) {
        Objects.requireNonNull(password, "password");
        requireIterations(iterations);
        if (password.length == 0) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password, salt, iterations);
        return ALGORITHM + "$" + iterations + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(key);
    }

    /**
     * Verifies a password against an encoded hash. Malformed hashes and empty
     * passwords are treated as a mismatch instead of an error.
     *
     * @param password the password characters
     * @param encoded  the stored hash
     * @return true when the password matches
     */
    public static boolean verify(char[] password, String encoded) {
        if (password == null || password.length == 0 || encoded == null) {
            return false;
        }
        ParsedHash parsed;
        try {
            parsed = parse(encoded);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        byte[] actual = derive(password, parsed.salt(), parsed.iterations());
        return MessageDigest.isEqual(parsed.key(), actual);
    }

    /**
     * Runs the same derivation work as {@link #verify(char[], String)} without
     * comparing anything, so the login response time does not reveal whether a
     * username exists.
     *
     * @param password the password characters
     */
    public static void burn(char[] password) {
        if (password == null || password.length == 0) {
            return;
        }
        derive(password, DECOY_SALT, DEFAULT_ITERATIONS);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(KEY_ALGORITHM)
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(KEY_ALGORITHM + " is not available", exception);
        } finally {
            keySpec.clearPassword();
        }
    }

    private static ParsedHash parse(String encoded) {
        String[] parts = encoded.split("\\$", -1);
        if (parts.length != 4 || !ALGORITHM.equals(parts[0])) {
            throw new IllegalArgumentException("Unsupported password hash format");
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid password hash iteration count", exception);
        }
        requireIterations(iterations);
        byte[] salt;
        byte[] key;
        try {
            salt = Base64.getDecoder().decode(parts[2]);
            key = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid base64 in password hash", exception);
        }
        if (salt.length != SALT_BYTES || key.length != KEY_BITS / Byte.SIZE) {
            throw new IllegalArgumentException("Invalid password hash parameters");
        }
        return new ParsedHash(salt, iterations, key);
    }

    private static void requireIterations(int iterations) {
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            throw new IllegalArgumentException("Password hash iterations must be between "
                    + MIN_ITERATIONS + " and " + MAX_ITERATIONS);
        }
    }

    private record ParsedHash(byte[] salt, int iterations, byte[] key) {
    }
}
