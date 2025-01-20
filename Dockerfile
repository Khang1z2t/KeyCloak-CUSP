# Use the official Keycloak image
FROM quay.io/keycloak/keycloak:latest

# Switch to root user to copy the JAR file and configuration files
USER root

# Copy the custom provider JAR file into the container
COPY target/userprovider-1.0-SNAPSHOT.jar /opt/keycloak/providers/

# Copy keycloak.conf
COPY config/keycloak.conf /opt/keycloak/conf/keycloak.conf
COPY config/quarkus.properties /opt/keycloak/conf/

# Ensure the "keycloak" user has access to the "providers" directory and configuration files
RUN chown -R keycloak:keycloak /opt/keycloak/providers/ /opt/keycloak/conf/

# Switch back to the "keycloak" user to run Keycloak
USER keycloak
ENV KEYCLOAK_USER admin
ENV KEYCLOAK_PASSWORD admin


# Start Keycloak
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]