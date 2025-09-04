package com.novara_demo.admin.util;

import com.novara_demo.admin.model.LoginRequest;
import com.novara_demo.admin.model.RefreshRequest;
import com.novara_demo.admin.model.TokenResponse;
import com.novara_demo.admin.model.exception.FailedRefreshException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class CustomAuthHeaderProvider {
    private final ReentrantLock refreshLock = new ReentrantLock();
    private final AtomicReference<TokenResponse> tokensCache = new AtomicReference<>(new TokenResponse());
    private final AtomicReference<Instant> coolOffPeriod = new AtomicReference<>(Instant.now().minusSeconds(1));
    private final RestClient restClient = RestClient.create();

    @Value("${app.url.login}")
    private String loginUrl;
    @Value("${app.url.refresh}")
    private String refreshUrl;
    @Value("${app.credentials.username}")
    private String username;
    @Value("${app.credentials.password}")
    private String password;
    @Value("${app.login-cool-off-seconds}")
    private long coolOffSeconds;

    public HttpHeaders addBearerAuthHeader(HttpHeaders existingHeaders) {

        String jwtToken = getJwtToken();
        if (jwtToken != null) {
            existingHeaders.setBearerAuth(jwtToken);
        }
        return existingHeaders;
    }

    private String getJwtToken() {
        TokenResponse tokens = tokensCache.get();
        if (tokens.getAccessToken() == null || !tokens.getAccessToken().isValid()) {
            handleInvalidJwt();
        }

        if (tokensCache.get().getAccessToken() != null && tokensCache.get().getAccessToken().isValid()) {
            return tokensCache.get().getAccessToken().getTokenValue();
        }
        return "invalid-token";
    }

    // NOTE: Yes this can be done with Mono without using a ReentrantLock, which would be better.
    private void handleInvalidJwt() {
        refreshLock.lock();
        // When the attemptLogin() method is reached, a cool off period is set
        // to prevent multiple login attempts (successful or not).
        if (coolOffPeriod.get().isAfter(Instant.now())) {
            refreshLock.unlock();
            return;
        }

        // Token may have been successfully refreshed by another thread at this point
        if (tokensCache.get().getAccessToken() != null && tokensCache.get().getAccessToken().isValid()) {
            refreshLock.unlock();
            return;
        }

        try {
            boolean refreshed = attemptRefresh(); // Refresh token may have expired
            if (!refreshed) {
                attemptLogin();
            }
        } catch (RuntimeException ignored) {
            // SBA will try to authenticate with the wrong/no token if there is a problem here
            //  so just let it try and it will back off requests if there is some prolonged
            //  issue with the client. If the client was down for a few minutes - SBA will try
            //  again after a certain amount of time and if client is up then, it will succeed.
            // TODO: Add logging here
        } finally {
            refreshLock.unlock();
        }

    }

    // Attempt to refresh the JWT with the refresh token.
    public boolean attemptRefresh() {
        TokenResponse currentTokens = tokensCache.get();
        if (currentTokens.getRefreshToken() == null) {return false;}
        RefreshRequest refreshRequest = new RefreshRequest(currentTokens.getRefreshToken().getTokenValue());

        try {
            ResponseEntity<TokenResponse> tokenResponse = restClient.post()
                    .uri(refreshUrl)
                    .body(refreshRequest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new FailedRefreshException("Could not refresh token");
                    })
                    .toEntity(TokenResponse.class);

            if (tokenResponse.hasBody() && !tokenResponse.getStatusCode().isError()) {
                TokenResponse responseBody = tokenResponse.getBody();
                tokensCache.set(responseBody);
                return true;
            }
            return false;
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private void attemptLogin() {
        LoginRequest loginRequest = new LoginRequest(username, password);
        try {
            ResponseEntity<TokenResponse> response = restClient.post()
                    .uri(loginUrl)
                    .body(loginRequest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(TokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                tokensCache.set(response.getBody());
            }
        } catch (RuntimeException ignored) {
        } finally {
            coolOffPeriod.set(Instant.now().plusSeconds(coolOffSeconds));
        }
    }
}
