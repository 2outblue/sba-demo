package com.novara_demo.admin.util;

import com.novara_demo.admin.model.LoginRequest;
import com.novara_demo.admin.model.RefreshRequest;
import com.novara_demo.admin.model.TokenResponse;
import com.novara_demo.admin.model.exception.FailedRefreshException;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.web.client.HttpHeadersProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class CustomAuthHeaderProvider implements HttpHeadersProvider {
    private final ReentrantLock refreshLock = new ReentrantLock();
    private final AtomicReference<TokenResponse> responseCache = new AtomicReference<>(new TokenResponse());
    private final RestTemplate restTemplate = new RestTemplate();
    private final RestClient restClient = RestClient.create();

    @Value("${app.url.login}")
    private String loginUrl;
    @Value("${app.url.refresh}")
    private String refreshUrl;
    @Value("${app.credentials.username}")
    private String username;
    @Value("${app.credentials.password}")
    private String password;


    @Override
    public HttpHeaders getHeaders(Instance instance) {
        HttpHeaders header = new HttpHeaders();
        header.setBearerAuth(getJwtToken());
        return header;
    }

    private String getJwtToken() {
        TokenResponse tokens = responseCache.get();
        if (tokens.getAccessToken() == null || !tokens.getAccessToken().isValid()) {
            handleInvalidJwt();
        }
        return responseCache.get().getAccessToken().getTokenValue();
    }

    private void handleInvalidJwt() {
        refreshLock.lock();

        if (responseCache.get().getAccessToken() != null && responseCache.get().getAccessToken().isValid()) {
            refreshLock.unlock();
            return;
        }
        boolean refreshed = attemptRefresh();
        if (!refreshed) {
            try {
                attemptLogin();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                refreshLock.unlock();
            }
        } else {
            refreshLock.unlock();
        }

    }

    public boolean attemptRefresh() {

        TokenResponse currentTokens = responseCache.get();
        if (currentTokens.getRefreshToken() == null) {return false;}
        RefreshRequest refreshRequest = new RefreshRequest(currentTokens.getRefreshToken().getTokenValue());
//        HttpHeaders loginHeaders = new HttpHeaders();
//        loginHeaders.setContentType(MediaType.APPLICATION_JSON);

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
                responseCache.set(responseBody);
                return true;
            }
            return false;
        } catch (FailedRefreshException e) {
            System.out.println(e.getMessage());
            return false;
        }

//        int statusCode = tokenResponse.getStatusCode().value();
//        String statusText = tokenResponse.getStatusCode().toString();
//        HttpHeaders responseHeaders = tokenResponse.getHeaders();
//        throw new WebClientResponseException(statusCode, statusText, responseHeaders, null, StandardCharsets.UTF_8);
    }

    private boolean attemptLogin() {
        LoginRequest loginRequest = new LoginRequest(username, password);
        ResponseEntity<TokenResponse> response = restClient.post()
                .uri(loginUrl)
                .body(loginRequest)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    int statusCode = res.getStatusCode().value();
                    String statusText = res.getStatusCode().toString();
                    HttpHeaders responseHeaders = res.getHeaders();
                    throw new WebClientResponseException(statusCode, statusText, responseHeaders, null, StandardCharsets.UTF_8);
                })
                .toEntity(TokenResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            responseCache.set(response.getBody());
            return true;
        }
        int statusCode = response.getStatusCode().value();
        String statusText = response.getStatusCode().toString();
        HttpHeaders responseHeaders = response.getHeaders();

        throw new WebClientResponseException(statusCode, statusText, responseHeaders, null, StandardCharsets.UTF_8);
    }
}
