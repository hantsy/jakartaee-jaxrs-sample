/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.example.Constants.REMEMBERME_VALIDITY_SECONDS;
import static com.example.Constants.TOKEN_VALIDITY_SECONDS;
import static java.util.stream.Collectors.joining;

/**
 * @author hantsy
 */
@ApplicationScoped
public class TokenProvider {

    @Inject
    Logger LOGGER;

    private static final String AUTHORITIES_KEY = "auth";
    private static final String DEFAULT_SECRET_KEY = "rzxlszyykpbgqcflzxsqcysyhljt";

    private Key secretKey;

    private long tokenValidity;

    private long tokenValidityForRememberMe;

    @PostConstruct
    public void init() {
        byte[] secret = Base64.getEncoder().encode(DEFAULT_SECRET_KEY.getBytes());
        this.secretKey = Keys.hmacShaKeyFor(secret);
        this.tokenValidity = TimeUnit.SECONDS.toMillis(TOKEN_VALIDITY_SECONDS);
        this.tokenValidityForRememberMe = TimeUnit.SECONDS.toMillis(REMEMBERME_VALIDITY_SECONDS);
    }

    public String createToken(String username, Set<String> authorities, Boolean rememberMe) {
        Date now = new Date();
        long validity = rememberMe ? tokenValidityForRememberMe : tokenValidity;
        Date expiration = new Date(now.getTime() + validity);

        Claims claims = Jwts.claims().setSubject(username);
        if (!authorities.isEmpty()) {
            claims.put(AUTHORITIES_KEY, authorities.stream().collect(joining(",")));
        }

        return Jwts.builder()
                .setClaims(claims)
                .signWith(this.secretKey, SignatureAlgorithm.HS512)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .compact();
    }

    public JwtCredential getCredential(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build()
                .parseClaimsJws(token)
                .getBody();

        Set<String> authorities
                = claims.get(AUTHORITIES_KEY) == null ?
                Collections.emptySet()
                : Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(",")).collect(Collectors.toSet());

        return new JwtCredential(claims.getSubject(), authorities);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parse(authToken);
            return true;
        } catch (SecurityException e) {
            LOGGER.log(Level.INFO, "Invalid JWT signature: {0}", e.getMessage());
            return false;
        }
    }
}
