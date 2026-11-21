package com.dau.labmanagement;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddedPostgresTest {

    @Test
    void testEmbeddedPostgresConnection() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start();
             Connection connection = postgres.getPostgresDatabase().getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1")) {

            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
            assertFalse(connection.isClosed());
        }
    }
}