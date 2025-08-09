package com.wego.wego.global.security;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;


import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtProvider {
    private final Key key;
    private final MemberRepository memberRepository;

    public JwtProvider(@Value("${jwt.secret}") String secretKey, MemberRepository memberRepository) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.memberRepository = memberRepository;
    }

    public JwtToken generateToken(Authentication authentication) {
        String accessToken = generateAccessToken(authentication, 86400000);
        String refreshToken = generateRefreshToken(authentication, 86400000);
        return new JwtToken(accessToken, refreshToken);
    }

    private String generateAccessToken(Authentication authentication, long expire) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .setExpiration(new Date(now + expire))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    private String generateRefreshToken(Authentication authentication, long expire) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(authentication.getName())
                .setExpiration(new Date(now + expire))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * TODO RT로 AT 재발급
     */
    public String reissueAccessToken(String refreshToken) {
        return null;
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        Member member = memberRepository.findByUsername(claims.getSubject())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return new UsernamePasswordAuthenticationToken(member, "", authorities);

    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        } catch (SignatureException E) {
            log.info("유조된 jwt 서명 에러");
        }
        return false;
    }

    public Claims parseClaims(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }



}
