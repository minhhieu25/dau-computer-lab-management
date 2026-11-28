package com.dau.labmanagement.shared.security;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Objects;

/** Helpers for the nonce-based password proof shared by Client and Server. */
public final class PasswordProof {

    private PasswordProof() {
    }

    /**
     * Derives the PBKDF2 key used for a login challenge.
     *
     * @param password the password characters
     * @param saltBase64 the challenge salt
     * @param iterations the PBKDF2 work factor
     * @return the derived 256-bit key
     */
    public static byte[] deriveKey(char[] password, String saltBase64, int iterations)
            throws GeneralSecurityException {
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(saltBase64, "saltBase64");
        if (password.length == 0 || iterations < 1 || iterations > 1_000_000) {
            throw new IllegalArgumentException("Invalid password proof parameters");
        }

        byte[] salt = Base64.getDecoder().decode(saltBase64);
        if (salt.length != 16) {
            throw new IllegalArgumentException("Password challenge salt must be 16 bytes");
        }
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
        } finally {
            keySpec.clearPassword();
        }
    }

    /**
     * Computes the HMAC proof for a server-generated nonce.
     *
     * @param derivedKey the PBKDF2 key
     * @param nonceBase64 the base64url-encoded nonce
     * @return the base64-encoded proof
     */
    public static String createProof(byte[] derivedKey, String nonceBase64)
            throws GeneralSecurityException {
        Objects.requireNonNull(derivedKey, "derivedKey");
        Objects.requireNonNull(nonceBase64, "nonceBase64");
        if (derivedKey.length != 32) {
            throw new IllegalArgumentException("Password proof key must be 32 bytes");
        }

        byte[] nonce = Base64.getUrlDecoder().decode(nonceBase64);
        if (nonce.length != 32) {
            throw new IllegalArgumentException("Password proof nonce must be 32 bytes");
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(derivedKey, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(nonce));
    }
}
