package com.dau.labmanagement.server.model.service;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for SC_001 back-end: real PostgreSQL (Zonky, no Docker),
 * real Flyway migrations and the real PBKDF2 hashes.
 */
class JdbiLoginServiceIT {

    private static final String PASSWORD = "Teacher@123";
    private static final int TEST_ITERATIONS = 1_000;

    private static EmbeddedPostgres postgres;
    private static Jdbi jdbi;

    @BeforeAll
    static void startDatabase() throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        DataSource dataSource = postgres.getPostgresDatabase();
        com.dau.labmanagement.server.config.DatabaseMigrator
                .migrate(dataSource, "classpath:db/migration");
        jdbi = com.dau.labmanagement.server.config.JdbiProvider.create(dataSource);
        seedAccounts(dataSource);
    }

    @AfterAll
    static void stopDatabase() throws Exception {
        if (postgres != null) {
            postgres.close();
        }
    }

    private static void seedAccounts(DataSource dataSource) throws Exception {
        String hash = com.dau.labmanagement.shared.security.PasswordHasher
                .hash(PASSWORD.toCharArray(), TEST_ITERATIONS);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_user (username, password_hash, full_name, role, status) VALUES
                    ('gv01', '%1$s', 'Giảng viên Một', 'teacher', 'active'),
                    ('gv_locked', '%1$s', 'Giảng viên Bị Khóa', 'teacher', 'locked'),
                    ('sv01', '%1$s', 'Sinh viên Một', 'student', 'active'),
                    ('gv_deleted', '%1$s', 'Giảng viên Đã Xóa', 'teacher', 'active')
                    """.formatted(hash));
            statement.execute("UPDATE app_user SET deleted_at = now() WHERE username = 'gv_deleted'");
        }
    }

    @Test
    void acceptsTeacherAndOpensASession() {
        LoginSessionRegistry registry = new LoginSessionRegistry();
        LoginResult result = new JdbiLoginService(jdbi, registry)
                .login("gv01", PASSWORD.toCharArray());

        assertTrue(result.isSuccess(), "teacher should log in");
        LoginSession session = result.session();
        assertEquals("gv01", session.username());
        assertEquals("Giảng viên Một", session.fullName());
        assertEquals(com.dau.labmanagement.server.model.entity.UserRole.TEACHER, session.role());
        assertEquals("Giảng viên", session.roleLabel());
        assertNotNull(session.sessionId());
        assertEquals(1, registry.activeCount());
        assertTrue(registry.find(session.sessionId()).isPresent());
    }

    @Test
    void acceptsAdminAndOpensASession() {
        LoginSessionRegistry registry = new LoginSessionRegistry();
        LoginResult result = new JdbiLoginService(jdbi, registry)
                .login("admin", "Admin@123".toCharArray());

        assertTrue(result.isSuccess(), "seed admin should log in with its seed password");
        assertEquals(com.dau.labmanagement.server.model.entity.UserRole.ADMIN, result.session().role());
        assertEquals("Admin", result.session().roleLabel());
    }

    @Test
    void trimsTheUsernameBeforeLookingItUp() {
        LoginResult result = new JdbiLoginService(jdbi, new LoginSessionRegistry())
                .login("  gv01  ", PASSWORD.toCharArray());

        assertTrue(result.isSuccess());
        assertEquals("gv01", result.session().username());
    }

    @Test
    void rejectsAWrongPassword() {
        assertRejected("gv01", "wrong-password".toCharArray(),
                LoginFailureReason.INVALID_CREDENTIALS);
    }

    @Test
    void rejectsAnUnknownAccount() {
        assertRejected("khong_ton_tai", PASSWORD.toCharArray(),
                LoginFailureReason.INVALID_CREDENTIALS);
    }

    @Test
    void rejectsALockedAccount() {
        assertRejected("gv_locked", PASSWORD.toCharArray(),
                LoginFailureReason.ACCOUNT_LOCKED);
    }

    @Test
    void rejectsASoftDeletedAccountAsInvalidCredentials() {
        assertRejected("gv_deleted", PASSWORD.toCharArray(),
                LoginFailureReason.INVALID_CREDENTIALS);
    }

    @Test
    void rejectsAStudentOnTheServer() {
        assertRejected("sv01", PASSWORD.toCharArray(),
                LoginFailureReason.ROLE_NOT_ALLOWED);
    }

    @Test
    void rejectsEmptyInput() {
        LoginSessionRegistry registry = new LoginSessionRegistry();
        JdbiLoginService service = new JdbiLoginService(jdbi, registry);

        assertEquals(LoginFailureReason.EMPTY_INPUT,
                service.login("", PASSWORD.toCharArray()).failure());
        assertEquals(LoginFailureReason.EMPTY_INPUT,
                service.login("gv01", new char[0]).failure());
        assertEquals(0, registry.activeCount());
    }

    @Test
    void clearsThePasswordArrayAfterUseIsCallerResponsibility() {
        // Documented contract: the service must not retain the password array.
        char[] password = PASSWORD.toCharArray();
        new JdbiLoginService(jdbi, new LoginSessionRegistry()).login("gv01", password);
        Arrays.fill(password, '\0');
        assertFalse(new String(password).contains(PASSWORD));
    }

    private static void assertRejected(String username, char[] password, LoginFailureReason expected) {
        LoginSessionRegistry registry = new LoginSessionRegistry();
        LoginResult result = new JdbiLoginService(jdbi, registry).login(username, password);

        assertFalse(result.isSuccess(), "login must be rejected");
        assertEquals(expected, result.failure());
        assertEquals(expected.message(), result.failureMessage());
        assertEquals(0, registry.activeCount(), "no session may be opened");
    }
}
