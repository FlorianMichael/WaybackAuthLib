/*
 * This file is part of WaybackAuthLib - https://github.com/FlorianMichael/WaybackAuthLib
 * Copyright (C) 2023-2025 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.florianmichael.waybackauthlib;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;

import java.net.Proxy;
import java.net.URI;
import java.util.*;

public class WaybackAuthLib {

    public static final String YGG_PROD = "https://authserver.mojang.com/";

    private static final String ROUTE_AUTHENTICATE = "authenticate";
    private static final String ROUTE_REFRESH = "refresh";
    private static final String ROUTE_INVALIDATE = "invalidate";
    private static final String ROUTE_VALIDATE = "validate";

    private final URI baseURI;
    private final String clientToken;
    private final MinecraftClient client;

    private String username;
    private String password;

    private String accessToken;

    private String userId;

    private boolean loggedIn;
    private GameProfile currentProfile;

    private List<Property> properties = new ArrayList<>();
    private List<GameProfile> profiles = new ArrayList<>();

    /**
     * Creates a new instance of the authentication service.
     */
    public WaybackAuthLib() {
        this(YGG_PROD, "");
    }

    /**
     * Creates a new instance of the authentication service.
     *
     * @param authHost The host of the authentication service. (e.g. <a href="https://authserver.mojang.com/">Mojang AuthServer</a>)
     */
    public WaybackAuthLib(final String authHost) {
        this(authHost, "");
    }

    /**
     * Creates a new instance of the authentication service.
     *
     * @param authHost    The host of the authentication service. (e.g. <a href="https://authserver.mojang.com/">Mojang AuthServer</a>)
     * @param clientToken The client token. (e.g. "")
     */
    public WaybackAuthLib(final String authHost, final String clientToken) {
        this(authHost, clientToken, Proxy.NO_PROXY);
    }

    /**
     * Creates a new instance of the authentication service.
     *
     * @param authHost    The host of the authentication service. (e.g. <a href="https://authserver.mojang.com/">Mojang AuthServer</a>)
     * @param clientToken The client token. (e.g. "")
     * @param proxy       The proxy to use for the requests.
     */
    public WaybackAuthLib(String authHost, final String clientToken, final Proxy proxy) {
        if (authHost == null) throw new IllegalArgumentException("Authentication is null");
        if (!authHost.endsWith("/")) authHost += "/";

        this.baseURI = URI.create(authHost);

        if (clientToken == null) throw new IllegalArgumentException("ClientToken is null");
        this.clientToken = clientToken;

        this.client = MinecraftClient.unauthenticated(Objects.requireNonNullElse(proxy, Proxy.NO_PROXY));
    }

    /**
     * Logs in the user with the provided credentials. If the user is already logged in, it will refresh the access token.
     *
     * @throws InvalidCredentialsException If the provided credentials are invalid.
     * @throws Exception                   If the server didn't send a response or the client token doesn't match.
     */
    public void logIn() throws Exception {
        if (this.username == null || this.username.isEmpty()) {
            throw new InvalidCredentialsException("Invalid username.");
        }

        final var refreshAccessToken = this.accessToken != null && !this.accessToken.isEmpty();
        final var newAuthentication = this.password != null && !this.password.isEmpty();

        if (!refreshAccessToken && !newAuthentication) {
            throw new InvalidCredentialsException("Invalid password or access token.");
        }

        AuthenticateRefreshResponse response;
        if (refreshAccessToken) {
            response = client.post(this.baseURI.resolve(ROUTE_REFRESH).toURL(), new RefreshRequest(this.clientToken, this.accessToken, null), AuthenticateRefreshResponse.class);
        } else {
            response = client.post(this.baseURI.resolve(ROUTE_AUTHENTICATE).toURL(), new AuthenticationRequest(Agent.MINECRAFT, this.username, this.password, this.clientToken), AuthenticateRefreshResponse.class);
        }

        if (response == null) {
            throw new InvalidRequestException("Server didn't sent a response.");
        }
        if (!response.clientToken.equals(this.clientToken)) {
            throw new InvalidRequestException("Server token and provided token doesn't match.");
        }

        // AuthLib tracks this field, so we do it too in case someone wants to use this library as a drop-in replacement.
        if (response.user != null && response.user.id != null) {
            userId = response.user.id;
        } else {
            userId = getUsername();
        }

        this.accessToken = response.accessToken;
        this.profiles = response.availableProfiles != null ? Arrays.asList(response.availableProfiles) : Collections.emptyList();
        this.currentProfile = response.selectedProfile;

        this.properties.clear();
        if (response.user != null && response.user.properties != null) {
            this.properties.addAll(response.user.properties);
        }

        this.loggedIn = true;
    }

    public boolean checkTokenValidity() {
        final var request = new ValidateRequest(accessToken, clientToken);

        try {
            client.post(this.baseURI.resolve(ROUTE_VALIDATE).toURL(), request, Response.class);
            return true;
        } catch (final Exception ignored) {
            return false;
        }
    }

    /**
     * Logs out the user and invalidates the access token. If the user is not logged in, it will do nothing.
     *
     * @throws Exception If the server didn't send a response or the client token doesn't match.
     */
    public void logOut() throws Exception {
        final var request = new InvalidateRequest(this.clientToken, this.accessToken);
        final var response = client.post(this.baseURI.resolve(ROUTE_INVALIDATE).toURL(), request, Response.class); // Mojang doesn't send this request, but it seems useful to invalidate the token.

        if (!this.loggedIn) {
            throw new IllegalStateException("Cannot log out while not logged in.");
        }

        if (response != null && response.error != null && !response.error.isEmpty()) {
            throw new InvalidRequestException(response.error, response.errorMessage, response.cause);
        }

        this.accessToken = null;

        this.loggedIn = false;
        this.currentProfile = null;

        this.properties = new ArrayList<>();
        this.profiles = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    /**
     * Sets the username. If the user is logged in, it will throw an exception.
     *
     * @param username The username.
     */
    public void setUsername(String username) {
        if (this.loggedIn && this.currentProfile != null) {
            throw new IllegalStateException("Cannot change username whilst logged in & online");
        }

        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Sets the password. If the user is logged in, it will throw an exception.
     *
     * @param password The password.
     */
    public void setPassword(String password) {
        if (this.loggedIn && this.currentProfile != null) {
            throw new IllegalStateException("Cannot set password whilst logged in & online");
        }

        this.password = password;
    }

    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token. If the user is logged in, it will throw an exception.
     *
     * @param accessToken The access token.
     */
    public void setAccessToken(String accessToken) {
        if (this.loggedIn && this.currentProfile != null) {
            throw new IllegalStateException("Cannot set access token whilst logged in & online");
        }

        this.accessToken = accessToken;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public GameProfile getCurrentProfile() {
        return currentProfile;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<GameProfile> getProfiles() {
        return profiles;
    }

    @Override
    public String toString() {
        return "WaybackAuthLib{" +
                "baseURI=" + baseURI +
                ", clientToken='" + clientToken + '\'' +
                ", client=" + client +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", userId='" + userId + '\'' +
                ", loggedIn=" + loggedIn +
                ", currentProfile=" + currentProfile +
                ", properties=" + properties +
                ", profiles=" + profiles +
                '}';
    }

    /**
     * These classes are only internal and are being used to serialize the requests and responses.
     */

    private static class Agent {
        public static final Agent MINECRAFT = new Agent("Minecraft", 1);

        public String name;
        public int version;

        protected Agent(String name, int version) {
            this.name = name;
            this.version = version;
        }
    }

    private static class User {

        public String id;
        public List<Property> properties;
    }

    private static class AuthenticationRequest {

        public Agent agent;
        public String username;
        public String password;
        public String clientToken;
        private boolean requestUser;

        protected AuthenticationRequest(final Agent agent, final String username, final String password, final String clientToken) {
            this.agent = agent;
            this.username = username;
            this.password = password;
            this.clientToken = clientToken;
            this.requestUser = true; // Only pass this field to fill the authentication header
        }
    }

    private static class RefreshRequest {

        public String clientToken;
        public String accessToken;
        public GameProfile selectedProfile;
        public boolean requestUser;

        protected RefreshRequest(String clientToken, String accessToken, GameProfile selectedProfile) {
            this.clientToken = clientToken;
            this.accessToken = accessToken;
            this.selectedProfile = selectedProfile;
            this.requestUser = true;
        }
    }

    private static class ValidateRequest {

        public String clientToken;
        public String accessToken;

        public ValidateRequest(final String accessToken, final String clientToken) {
            this.clientToken = clientToken;
            this.accessToken = accessToken;
        }
    }

    private static class InvalidateRequest {

        public String clientToken;
        public String accessToken;

        protected InvalidateRequest(String clientToken, String accessToken) {
            this.clientToken = clientToken;
            this.accessToken = accessToken;
        }
    }

    private static class Response {

        public String error;
        public String errorMessage;
        public String cause;
    }

    private static class AuthenticateRefreshResponse extends Response {

        public String accessToken;
        public String clientToken;
        public GameProfile selectedProfile;
        public GameProfile[] availableProfiles;
        public User user;
    }

}
