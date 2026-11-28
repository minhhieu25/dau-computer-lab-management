/**
 * Password hashing helpers shared by Server and Client.
 *
 * <p>Implements the PBKDF2WithHmacSHA256 format fixed in V2__seed.sql
 * ({@code pbkdf2-sha256$<iterations>$<salt-base64>$<key-base64>}).</p>
 */
package com.dau.labmanagement.shared.security;
