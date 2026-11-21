package com.dau.labmanagement.shared.auth;

/** Defines the internal authentication boundary implemented by a local or network adapter. */
public interface AuthenticationGateway {
    /**
     * Authenticates credentials and returns the account/session established by the adapter.
     * @param request the credentials for one authentication attempt
     * @return the authentication outcome
     * @throws AuthenticationException when the adapter cannot complete the request
     */
    AuthenticationResponse authenticate(AuthenticationRequest request) throws AuthenticationException;

    /**
     * Ends the server-side session associated with the supplied session identifier.
     * @param sessionId the authenticated session identifier
     * @throws AuthenticationException when the adapter cannot complete the request
     */
    void logout(String sessionId) throws AuthenticationException;
}