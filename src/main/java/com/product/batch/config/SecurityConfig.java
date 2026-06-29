package com.product.batch.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .headers(headers -> headers
                                                .frameOptions(frameOptions -> frameOptions.sameOrigin()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PathRequest.toH2Console()).permitAll()
                                                .requestMatchers("/h2-console/**").permitAll()

                                                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                                                .requestMatchers(HttpMethod.POST, "/api/batch/process-products")
                                                .hasAnyRole("ADMIN", "BATCH_OPERATOR")

                                                .requestMatchers(HttpMethod.GET, "/api/batch/status/**")
                                                .hasAnyRole("ADMIN", "BATCH_OPERATOR", "VIEWER")

                                                .requestMatchers(HttpMethod.GET, "/api/batch/errors/**")
                                                .hasAnyRole("ADMIN", "BATCH_OPERATOR")

                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable);

                return http.build();
        }

        @Bean
        public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {

                UserDetails admin = User.builder()
                                .username("admin")
                                .password(passwordEncoder.encode("admin"))
                                .roles("ADMIN")
                                .build();

                UserDetails operator = User.builder()
                                .username("operator")
                                .password(passwordEncoder.encode("operator"))
                                .roles("BATCH_OPERATOR")
                                .build();

                UserDetails viewer = User.builder()
                                .username("viewer")
                                .password(passwordEncoder.encode("viewer"))
                                .roles("VIEWER")
                                .build();

                return new InMemoryUserDetailsManager(admin, operator, viewer);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration authenticationConfiguration) throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {

                JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

                grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
                grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

                JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

                jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

                return jwtAuthenticationConverter;
        }
}