package com.custom.provider;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.custom.entity.User;
import com.custom.entity.UserAdapter;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CustomUserStorageProvider implements UserStorageProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserStorageProvider.class);

    private ComponentModel componentModel;
    private KeycloakSession keycloakSession;
    private Connection connection;

//    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void setModel(ComponentModel componentModel) {
        this.componentModel = componentModel;
    }

    public void setSession(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void close() {
        logger.info("Closing database connection.");
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed successfully.");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }

    @Override
    public UserModel getUserByUsername(RealmModel realmModel, String username) {
        logger.info("Attempting to find user by username: {}", username);
        String query = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                logger.info("User found with username: {}", username);
                User user = mapRowToUser(rs);
                return new UserAdapter(keycloakSession, realmModel, componentModel, user, connection);
            }else {
                logger.warn("No user found with username: {}", username);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username", e);
        }
        return null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realmModel, String email) {
        logger.info("Attempting to find user by email: {}", email);
        String query = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                logger.info("User found with email: {}", email);
                User user = mapRowToUser(rs);
                return new UserAdapter(keycloakSession, realmModel, componentModel, user, connection);
            }else {
                logger.warn("No user found with email: {}", email);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by email", e);
        }
        return null;
    }

    @Override
    public UserModel getUserById(RealmModel realmModel, String id) {
        logger.info("Attempting to find user by ID: {}", id);
        long persistenceId;
        try {
            persistenceId = Long.parseLong(StorageId.externalId(id));
        } catch (NumberFormatException e) {
            logger.error("Invalid ID format: {}", id, e);
            return null;
        }

        String query = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, persistenceId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                logger.info("User found with ID: {}", id);
                User user = mapRowToUser(rs);
                return new UserAdapter(keycloakSession, realmModel, componentModel, user, connection);
            } else {
                logger.warn("No user found with ID: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by ID", e);
        }
        return null;
    }

    @Override
    public UserModel addUser(RealmModel realmModel, String username) {
        logger.info("Attempting to add user with username: {}", username);
        String query = "INSERT INTO users (username) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    logger.info("User successfully added with ID: {} and username: {}", id, username);

                    User user = new User();
                    user.setId(id);
                    user.setUsername(username);

                    return new UserAdapter(keycloakSession, realmModel, componentModel, user, connection);
                } else {
                    logger.error("Failed to retrieve generated ID for user: {}", username);
                }
            }
        } catch (SQLException e) {
            logger.error("Error adding user", e);
        }
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realmModel, UserModel userModel) {
        logger.info("Attempting to remove user with ID: {}", userModel.getId());
        String query = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            long persistenceId;
            try {
                persistenceId = Long.parseLong(StorageId.externalId(userModel.getId()));
            } catch (NumberFormatException e) {
                logger.error("Invalid ID format: {}", userModel.getId(), e);
                return false;
            }

            stmt.setLong(1, persistenceId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("User successfully removed with ID: {}", userModel.getId());
                return true;
            } else {
                logger.warn("No user found with ID: {} to remove", userModel.getId());
            }
        } catch (SQLException e) {
            logger.error("Error removing user", e);
        }
        return false;
    }

//    @Override
//    public boolean supportsCredentialType(String credentialType) {
//        return "password".equals(credentialType);
//    }
//
//    @Override
//    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
//        return supportsCredentialType(credentialType);
//    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        logger.info("Validating credentials for user: {}", user.getUsername());
        if (!supportsCredentialType(credentialInput.getType())) {
            logger.warn("Unsupported credential type: {}", credentialInput.getType());
            return false;
        }
        String query = "SELECT password FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                logger.info("storedPassword: ", storedPassword);
//                boolean isValid = passwordEncoder.matches(credentialInput.getChallengeResponse(), storedPassword);
//                logger.info("Credential validation result for user {}: {}", user.getUsername(), isValid);
                return true;
            } else {
                logger.warn("No password found for user: {}", user.getUsername());
            }
        } catch (SQLException e) {
            logger.error("Error validating credentials for user", e);
        }
        return false;
    }


    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realmModel, Map<String, String> map, Integer firstResult, Integer maxResults) {
        String searchParam = map.getOrDefault("email", map.getOrDefault("username", ""));
        logger.info("Searching for users with parameter: {}", searchParam);

        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE email LIKE ? OR username LIKE ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, "%" + searchParam + "%");
            stmt.setString(2, "%" + searchParam + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
            logger.info("Found {} users matching search parameter: {}", users.size(), searchParam);
        } catch (SQLException e) {
            logger.error("Error searching for users", e);
        }

        return users.stream().map(user -> new UserAdapter(keycloakSession, realmModel, componentModel, user, connection));
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        logger.info("Mapping ResultSet to User object");
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFirstName(rs.getString("firstname"));
        user.setLastName(rs.getString("lastname"));
        user.setPassword(rs.getString("password"));
        logger.info("Mapped User: {}", user.getUsername());
        return user;
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realmModel, GroupModel groupModel, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realmModel, String attribute, String value) {
        return Stream.empty();
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return CredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (input instanceof UserCredentialModel && input.getType().equals(CredentialModel.PASSWORD)) {
            String newPassword = ((UserCredentialModel) input).getValue();
//            String hashedPassword = passwordEncoder.encode(newPassword);

            String query = "UPDATE users SET password = ? WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
//                stmt.setString(1, hashedPassword);
                stmt.setString(2, user.getUsername());
                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    logger.info("Password updated successfully for user: {}", user.getUsername());
                    return true;
                } else {
                    logger.warn("Failed to update password for user: {}", user.getUsername());
                    return false;
                }
            } catch (SQLException e) {
                logger.error("Error updating password for user", e);
                return false;
            }
        } else {
            logger.warn("Unsupported credential type for update: {}", input.getType());
            throw new IllegalArgumentException("Unsupported credential type: " + input.getType());
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (CredentialModel.PASSWORD.equals(credentialType)) {
            logger.warn("Disabling credential type 'password' is not supported.");
            throw new UnsupportedOperationException("Disabling password credentials is not supported.");
        }
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }
}
