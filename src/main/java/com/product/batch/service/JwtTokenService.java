package com.product.batch.service;

import com.product.batch.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final long TOKEN_EXPIRY_SECONDS = 3600;

    private final JwtEncoder jwtEncoder;

    public AuthResponse generateToken(Authentication authentication) {

        Instant now = Instant.now();

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replace("ROLE_", ""))
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("product-csv-batch")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(TOKEN_EXPIRY_SECONDS))
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        return new AuthResponse(token, "Bearer", TOKEN_EXPIRY_SECONDS);
    }
}