package com.novara_demo.admin.model;

import java.time.Instant;

public class TokenResponse {
    private AccessToken accessToken;
    private RefreshToken refreshToken;

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public TokenResponse setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public TokenResponse setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public static class AccessToken {
        private String tokenValue;
        private Instant issuedAt;
        private Instant expiresAt;

        public boolean isValid() {
            return tokenValue != null && expiresAt != null && Instant.now().isBefore(expiresAt.minusSeconds(30));
        }

        public String getTokenValue() {
            return tokenValue;
        }

        public AccessToken setTokenValue(String tokenValue) {
            this.tokenValue = tokenValue;
            return this;
        }

        public Instant getIssuedAt() {
            return issuedAt;
        }

        public AccessToken setIssuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public AccessToken setExpiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
    }

    public static class RefreshToken {
        private String tokenValue;

        public String getTokenValue() {
            return tokenValue;
        }

        public RefreshToken setTokenValue(String tokenValue) {
            this.tokenValue = tokenValue;
            return this;
        }
    }
}
