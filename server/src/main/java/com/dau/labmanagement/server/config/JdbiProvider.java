package com.dau.labmanagement.server.config;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.CaseInsensitiveColumnNameMatcher;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
import org.jdbi.v3.core.mapper.reflect.SnakeCaseColumnNameMatcher;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.sql.DataSource;

import java.util.List;

/**
 * Creates the single JDBI instance shared by all DAOs.
 *
 * <p>Global mapping rule for the whole project: snake_case columns map to
 * camelCase record components (see {@code model.entity}). DAOs are SQL object
 * interfaces ({@code model.dao}); use them with
 * {@code jdbi.withExtension(UserDao.class, dao -> dao.findActiveUsers())}.</p>
 */
public final class JdbiProvider {

    private JdbiProvider() {
        // Utility class, no instances.
    }

    /**
     * Creates a configured JDBI instance.
     *
     * @param dataSource the pooled data source
     * @return the JDBI entry point for DAO extensions
     */
    public static Jdbi create(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        // Snake_case first, then the default case-insensitive fallback.
        jdbi.getConfig(ReflectionMappers.class).setColumnNameMatchers(
                List.of(new SnakeCaseColumnNameMatcher(), new CaseInsensitiveColumnNameMatcher()));
        return jdbi;
    }
}
