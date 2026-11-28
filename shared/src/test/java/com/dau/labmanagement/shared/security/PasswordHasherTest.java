package com.dau.labmanagement.shared.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashRoundTripsAndUsesTheProjectFormat() {
        String encoded = PasswordHasher.hash("Teacher@123".toCharArray());

        assertTrue(encoded.startsWith(PasswordHasher.ALGORITHM + "$"
                + PasswordHasher.DEFAULT_ITERATIONS + "$"));
        assertTrue(PasswordHasher.verify("Teacher@123".toCharArray(), encoded));
        assertFalse(PasswordHasher.verify("teacher@123".toCharArray(), encoded));
    }

    @Test
    void eachHashUsesAFreshSalt() {
        String first = PasswordHasher.hash("same-password".toCharArray(), 1_000);
        String second = PasswordHasher.hash("same-password".toCharArray(), 1_000);

        assertNotEquals(first, second);
        assertTrue(PasswordHasher.verify("same-password".toCharArray(), first));
        assertTrue(PasswordHasher.verify("same-password".toCharArray(), second));
    }

    @Test
    void rejectsEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(new char[0]));
        assertFalse(PasswordHasher.verify(new char[0], PasswordHasher.hash("x".toCharArray())));
    }

    @Test
    void treatsMalformedHashesAsMismatch() {
        assertFalse(PasswordHasher.verify("x".toCharArray(), "plain-text"));
        assertFalse(PasswordHasher.verify("x".toCharArray(), "pbkdf2-sha256$210000$bad"));
        assertFalse(PasswordHasher.verify("x".toCharArray(), "md5$1$c2FsdA==$a2V5"));
        assertFalse(PasswordHasher.verify("x".toCharArray(), null));
    }

    @Test
    void validatesIterationBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordHasher.hash("x".toCharArray(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordHasher.hash("x".toCharArray(), 1_000_001));
    }
}
