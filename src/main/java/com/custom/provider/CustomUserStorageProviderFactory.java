package com.custom.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider> {
    private Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(CustomUserStorageProviderFactory.class);
    private static final String URL = "jdbc:postgresql://host.docker.internal:2345/provider_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "12345";

    @Override
    public CustomUserStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        try {
            CustomUserStorageProvider customUserProvider = new CustomUserStorageProvider();
            customUserProvider.setModel(componentModel);
            customUserProvider.setSession(keycloakSession);
            customUserProvider.setConnection(getConnection());
            return customUserProvider;
        } catch (Exception e) {
            logger.error("Error creating CustomerStorageProvider", e);
            throw new RuntimeException("Failed to create CustomerStorageProvider", e);
        }
    }

    private synchronized Connection getConnection() {
        if (connection == null || !isConnectionValid()) {
            int attempts = 0;
            SQLException lastException = null;
            while (attempts < 3) {
                try {
                    connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    logger.info("Successfully connected to the PostgreSQL database.");
                    return connection;
                } catch (SQLException e) {
                    attempts++;
                    lastException = e;
                    logger.error("Failed to connect to the database. Attempt: {}", attempts);
                }
            }
            throw new RuntimeException("Unable to connect to the database after 3 attempts", lastException);
        }
        return connection;
    }

    private boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public String getId() {
        return "custom-user-provider";
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection", e);
        }
    }
}
