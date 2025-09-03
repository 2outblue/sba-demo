package com.novara_demo.admin.util;

import com.novara_demo.admin.model.LoginRequest;
import com.novara_demo.admin.model.TokenResponse;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.web.client.HttpHeadersProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

//@Component
public class CustomAuthProvider implements HttpHeadersProvider {

    private final RestTemplate restTemplate = new RestTemplate();
    private volatile String jwtToken;
    private final String loginUrl = "http://localhost:8095/auth/login";


    @Override
    public HttpHeaders getHeaders(Instance instance) {
        HttpHeaders header = new HttpHeaders();
        header.setBearerAuth(getJwtToken());
        return header;
    }

    private String getJwtToken() {
        if (jwtToken == null) {
            jwtToken = fetchJwtToken();
        }
        return jwtToken;
    }


    private String fetchJwtToken() {

        try {
            LoginRequest loginRequest = new LoginRequest("elon.vacation@example.com", "SunnyMarsTrip42!");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            TokenResponse response = restTemplate.postForObject(loginUrl, request, TokenResponse.class);
            System.out.println(response != null && response.getAccessToken() != null ? response.getAccessToken().getTokenValue() : null);
            return response != null && response.getAccessToken() != null
                ? response.getAccessToken().getTokenValue()
                : null;
        } catch (HttpClientErrorException e) {
            System.err.println("Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return null;
        }

//        return response != null && response.getAccessToken() != null
//                ? response.getAccessToken().getTokenValue()
//                : null;
    }
}
