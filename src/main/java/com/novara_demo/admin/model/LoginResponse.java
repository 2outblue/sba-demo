package com.novara_demo.admin.model;

public class LoginResponse {
    private AccessToken accessToken;
    private RefreshToken refreshToken;

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public LoginResponse setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public LoginResponse setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public static class AccessToken {
        private String tokenValue;

        public String getTokenValue() {
            return tokenValue;
        }

        public AccessToken setTokenValue(String tokenValue) {
            this.tokenValue = tokenValue;
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
