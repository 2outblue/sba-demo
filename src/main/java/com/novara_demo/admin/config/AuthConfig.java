package com.novara_demo.admin.config;

import com.novara_demo.admin.util.CustomAuthHeaderProvider;
import de.codecentric.boot.admin.server.web.client.InstanceExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class AuthConfig {

    @Bean
    public CustomAuthHeaderProvider getAuthHeaderProvider() {
        return new CustomAuthHeaderProvider();
    }

    @Bean
    public InstanceExchangeFilterFunction unauthorizedResponseConverterFilter() {
        return (instance, request, next) -> next.exchange(request)
                .flatMap(response -> {
                    if (response.statusCode().is4xxClientError()) {
                        return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE, response.strategies())
                                .headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
                                .body(response.bodyToFlux(DataBuffer.class))
                                .build());
                    }
                    return Mono.just(response);
                });
    }


    @Bean
    public InstanceExchangeFilterFunction addAuthHeader(CustomAuthHeaderProvider authHeaderProvider) {
        return (instance, request, next) -> {
            String url = request.url().toString();
            if (url.contains("/actuator/") || url.contains("/manage/")) {
                HttpHeaders existingHeaders = new HttpHeaders(request.headers());
                HttpHeaders updatedHeaders = authHeaderProvider.addBearerAuthHeader(existingHeaders);
                ClientRequest updatedRequest = ClientRequest.from(request)
                        .headers(headers -> headers.putAll(updatedHeaders))
                        .build();
                return next.exchange(updatedRequest);

            }
            return next.exchange(request);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User
                .withUsername("user")
                .password(passwordEncoder.encode("pass"))
                .build();
        return new MapReactiveUserDetailsService(user);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/logout").permitAll()
                        .anyExchange().authenticated()
                )
                .httpBasic(withDefaults())
                .formLogin(withDefaults())
                .logout(l -> l
                        .logoutUrl("/logout"))
                .csrf(c -> c.disable());

        return http.build();
    }
}
