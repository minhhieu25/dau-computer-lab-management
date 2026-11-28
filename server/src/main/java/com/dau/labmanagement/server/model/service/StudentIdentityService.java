package com.dau.labmanagement.server.model.service;

import com.dau.labmanagement.server.model.dao.StudentSessionDao;
import com.dau.labmanagement.server.model.dao.UserDao;
import com.dau.labmanagement.server.model.entity.AccountStatus;
import com.dau.labmanagement.server.model.entity.UserRole;
import com.dau.labmanagement.server.model.entity.UserRow;
import com.dau.labmanagement.shared.protocol.message.LoginAckMessage;
import com.dau.labmanagement.shared.protocol.message.LoginStatus;
import com.dau.labmanagement.shared.security.PasswordProof;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

/** Authenticates student accounts and binds successful logins to a machine session. */
public final class StudentIdentityService {

    private static final Logger LOG = LoggerFactory.getLogger(StudentIdentityService.class);
    private static final int DECOY_ITERATIONS = 210_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Jdbi jdbi;

    public StudentIdentityService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /** Creates a one-time challenge without revealing whether an account exists. */
    public Challenge beginLogin(String studentId) {
        byte[] nonce = randomBytes(32);
        UserRow eligibleStudent = null;
        PasswordHash hash = null;

        if (studentId != null && !studentId.isBlank() && studentId.length() <= 50) {
            Optional<UserRow> account = jdbi.withExtension(
                    UserDao.class, dao -> dao.findActiveByUsername(studentId));
            if (account.isPresent()
                    && AccountStatus.ACTIVE.toDb().equals(account.get().status())
                    && UserRole.STUDENT.toDb().equals(account.get().role())) {
                try {
                    hash = PasswordHash.parse(account.get().passwordHash());
                    eligibleStudent = account.get();
                } catch (IllegalArgumentException exception) {
                    LOG.error("Student account has an invalid password hash format", exception);
                }
            }
        }

        if (eligibleStudent == null) {
            byte[] salt = randomBytes(16);
            return new Challenge(studentId, null, null, null, nonce,
                    salt, DECOY_ITERATIONS);
        }
        Challenge challenge = new Challenge(studentId, eligibleStudent.userId(),
                eligibleStudent.passwordHash(), hash.key(), nonce,
                hash.salt(), hash.iterations());
        java.util.Arrays.fill(hash.key(), (byte) 0);
        return challenge;
    }

    /** Verifies a one-time password proof and opens a persisted student session. */
    public Optional<StudentIdentity> completeLogin(
            Challenge challenge, String studentId, String nonce, String proof, long machineId) {
        if (challenge == null || challenge.userId == null
                || !challenge.studentId.equals(studentId)
                || !challenge.nonce.equals(nonce)
                || !challenge.matchesProof(proof)) {
            return Optional.empty();
        }

        return jdbi.inTransaction(handle -> createStudentSession(handle, challenge, machineId));
    }

    private Optional<StudentIdentity> createStudentSession(
            Handle handle, Challenge challenge, long machineId) {
        Optional<UserRow> currentAccount = handle.attach(UserDao.class)
                .findActiveByUsernameForUpdate(challenge.studentId);
        if (currentAccount.isEmpty()) {
            return Optional.empty();
        }
        UserRow user = currentAccount.get();
        if (!user.userId().equals(challenge.userId)
                || !AccountStatus.ACTIVE.toDb().equals(user.status())
                || !UserRole.STUDENT.toDb().equals(user.role())
                || !challenge.passwordHash.equals(user.passwordHash())) {
            return Optional.empty();
        }

        OffsetDateTime now = OffsetDateTime.now();
        StudentSessionDao sessions = handle.attach(StudentSessionDao.class);
        long sessionId = sessions.insert(user.userId(), machineId);
        return Optional.of(new StudentIdentity(
                sessionId, user.username(), user.fullName()));
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /** The verifier state is held only for the lifetime of a single TCP challenge. */
    public static final class Challenge {
        private final String studentId;
        private final Long userId;
        private final String passwordHash;
        private final byte[] verifier;
        private final String nonce;
        private final String salt;
        private final int iterations;

        private Challenge(String studentId, Long userId, String passwordHash,
                          byte[] verifier, byte[] nonce, byte[] salt, int iterations) {
            this.studentId = studentId;
            this.userId = userId;
            this.passwordHash = passwordHash;
            this.verifier = verifier == null ? null : verifier.clone();
            this.nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
            this.salt = Base64.getEncoder().encodeToString(salt);
            this.iterations = iterations;
        }

        public LoginAckMessage acknowledgment() {
            return new LoginAckMessage(
                    LoginStatus.CHALLENGE, nonce, salt, iterations,
                    null, null, null, null);
        }

        private boolean matchesProof(String proof) {
            if (verifier == null || proof == null) {
                return false;
            }
            try {
                byte[] expected = Base64.getDecoder().decode(
                        PasswordProof.createProof(verifier, nonce));
                byte[] actual = Base64.getDecoder().decode(proof);
                return MessageDigest.isEqual(expected, actual);
            } catch (IllegalArgumentException | GeneralSecurityException exception) {
                return false;
            }
        }

        public void clear() {
            if (verifier != null) {
                java.util.Arrays.fill(verifier, (byte) 0);
            }
        }
    }

    /** Public identity returned to the Client after a successful login. */
    public record StudentIdentity(long studentSessionId, String studentId, String fullName) {
    }

    private record PasswordHash(byte[] salt, int iterations, byte[] key) {

        private static PasswordHash parse(String encoded) {
            if (encoded == null) {
                throw new IllegalArgumentException("Password hash is missing");
            }
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) {
                throw new IllegalArgumentException("Unsupported password hash format");
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] key = Base64.getDecoder().decode(parts[3]);
                if (iterations < 1 || iterations > 1_000_000
                        || salt.length != 16 || key.length != 32) {
                    throw new IllegalArgumentException("Invalid password hash parameters");
                }
                return new PasswordHash(salt, iterations, key);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid password hash iteration count", exception);
            }
        }
    }
}
