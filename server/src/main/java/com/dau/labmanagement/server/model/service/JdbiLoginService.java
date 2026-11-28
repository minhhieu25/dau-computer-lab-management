package com.dau.labmanagement.server.model.service;

import com.dau.labmanagement.server.model.dao.UserDao;
import com.dau.labmanagement.server.model.entity.AccountStatus;
import com.dau.labmanagement.server.model.entity.UserRole;
import com.dau.labmanagement.server.model.entity.UserRow;
import com.dau.labmanagement.shared.security.PasswordHasher;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Database-backed {@link LoginService} for the Server login screen (SC_001).
 *
 * <p>Checks, in order: input presence, account existence (soft-deleted accounts
 * are invisible to the DAO), the PBKDF2 password hash, the account status and
 * the role. Only {@code admin} and {@code teacher} may use the Server;
 * students log in on the Client (SC_002, BR-09).</p>
 *
 * <p>The password is verified before the status is reported so a wrong password
 * never reveals that an account exists and is locked. An unknown username still
 * runs a full PBKDF2 derivation ({@link PasswordHasher#burn(char[])}) so the
 * response time does not disclose account existence.</p>
 */
public final class JdbiLoginService implements LoginService {

    private static final Logger LOG = LoggerFactory.getLogger(JdbiLoginService.class);

    private final Jdbi jdbi;
    private final LoginSessionRegistry sessions;

    /**
     * @param jdbi     the shared JDBI instance
     * @param sessions registry that stores the opened Server sessions
     */
    public JdbiLoginService(Jdbi jdbi, LoginSessionRegistry sessions) {
        this.jdbi = jdbi;
        this.sessions = sessions;
    }

    @Override
    public LoginResult login(String username, char[] password) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isEmpty() || password == null || password.length == 0) {
            return LoginResult.rejected(LoginFailureReason.EMPTY_INPUT);
        }
        if (normalizedUsername.length() > 50) {
            PasswordHasher.burn(password);
            return LoginResult.rejected(LoginFailureReason.INVALID_CREDENTIALS);
        }

        Optional<UserRow> account;
        try {
            account = jdbi.withExtension(UserDao.class,
                    dao -> dao.findActiveByUsername(normalizedUsername));
        } catch (RuntimeException exception) {
            LOG.error("Login lookup failed for user '{}'", normalizedUsername, exception);
            return LoginResult.rejected(LoginFailureReason.SYSTEM_UNAVAILABLE);
        }

        if (account.isEmpty()) {
            PasswordHasher.burn(password);
            LOG.info("Rejected Server login for unknown account '{}'", normalizedUsername);
            return LoginResult.rejected(LoginFailureReason.INVALID_CREDENTIALS);
        }

        UserRow user = account.get();
        if (!PasswordHasher.verify(password, user.passwordHash())) {
            LOG.info("Rejected Server login for '{}': wrong password", normalizedUsername);
            return LoginResult.rejected(LoginFailureReason.INVALID_CREDENTIALS);
        }
        if (!AccountStatus.ACTIVE.toDb().equals(user.status())) {
            LOG.info("Rejected Server login for '{}': account status {}", normalizedUsername, user.status());
            return LoginResult.rejected(LoginFailureReason.ACCOUNT_LOCKED);
        }

        UserRole role = parseRole(user);
        if (role == null || role == UserRole.STUDENT) {
            LOG.info("Rejected Server login for '{}': role {} is not allowed on the Server",
                    normalizedUsername, user.role());
            return LoginResult.rejected(LoginFailureReason.ROLE_NOT_ALLOWED);
        }

        LoginSession session = sessions.open(user.userId(), user.username(), user.fullName(), role);
        LOG.info("Opened Server login session {} for {} ({})",
                session.sessionId(), user.username(), role.toDb());
        return LoginResult.success(session);
    }

    private static UserRole parseRole(UserRow user) {
        try {
            return UserRole.fromDb(user.role());
        } catch (IllegalArgumentException exception) {
            LOG.error("Account '{}' has an unsupported role '{}'", user.username(), user.role());
            return null;
        }
    }
}
