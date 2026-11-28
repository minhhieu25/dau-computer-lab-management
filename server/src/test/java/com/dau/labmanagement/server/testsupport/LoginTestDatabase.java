package com.dau.labmanagement.server.testsupport;

import com.dau.labmanagement.server.config.DatabaseMigrator;
import com.dau.labmanagement.server.config.JdbiProvider;
import com.dau.labmanagement.shared.security.PasswordHasher;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.jdbi.v3.core.Jdbi;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * A throw-away PostgreSQL instance with the real V1/V2 migrations and a fixed
 * set of SC_001 test accounts. Used by the UI flow test and the demo launcher
 * so nobody needs a local database, a password or a Docker daemon to try login.
 */
public final class LoginTestDatabase implements AutoCloseable {

    /** Password shared by every non-admin test account. */
    public static final String TEACHER_PASSWORD = "Teacher@123";

    /** Password of the seeded {@code admin} account (V2__seed.sql). */
    public static final String ADMIN_PASSWORD = "Admin@123";

    private final EmbeddedPostgres postgres;
    private final Jdbi jdbi;

    private LoginTestDatabase(EmbeddedPostgres postgres, Jdbi jdbi) {
        this.postgres = postgres;
        this.jdbi = jdbi;
    }

    /** Starts the database, applies migrations and seeds the demo accounts. */
    public static LoginTestDatabase start() throws Exception {
        EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
        try {
            DataSource dataSource = postgres.getPostgresDatabase();
            DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
            seed(dataSource);
            return new LoginTestDatabase(postgres, JdbiProvider.create(dataSource));
        } catch (Exception | Error failure) {
            postgres.close();
            throw failure;
        }
    }

    public Jdbi jdbi() {
        return jdbi;
    }

    private static void seed(DataSource dataSource) throws Exception {
        String hash = PasswordHasher.hash(TEACHER_PASSWORD.toCharArray(), 2_000);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_user (username, password_hash, full_name, role, status) VALUES
                    ('gv01', '%1$s', 'Nguyễn Văn Giảng', 'teacher', 'active'),
                    ('gv02', '%1$s', 'Trần Thị Dạy', 'teacher', 'active'),
                    ('gv_locked', '%1$s', 'Giảng Viên Bị Khóa', 'teacher', 'locked'),
                    ('sv01', '%1$s', 'Sinh Viên Một', 'student', 'active'),
                    ('sv02', '%1$s', 'Sinh Viên Hai', 'student', 'active')
                    """.formatted(hash));
        }
    }

    @Override
    public void close() throws Exception {
        postgres.close();
    }
}
