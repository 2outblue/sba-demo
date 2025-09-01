package com.novara_demo.admin.config;


import com.novara_demo.admin.util.CustomAuthProvider;
import de.codecentric.boot.admin.server.web.client.HttpHeadersProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

    @Bean
    public HttpHeadersProvider authProvider() {
        return new CustomAuthProvider();
    }
}
