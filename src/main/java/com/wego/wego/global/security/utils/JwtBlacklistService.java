package com.wego.wego.global.security.utils;

import com.wego.wego.global.security.JwtProvider;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProvider jwtProvider;

    public void addChangePWBlacklistToken(String token, String username) {
        long atExpiration = jwtProvider.getExpiryTime(token); // 만료시간(ms)
        long now = System.currentTimeMillis();
        long atTtl = atExpiration - now;
        if (atTtl > 0) { // 만료된 토큰이 아닐 때만
            redisTemplate.opsForValue().set(
                    RedisKeyUtils.changePasswordBlackListKey(username), token, atTtl, TimeUnit.MILLISECONDS
            );
        }
    }
    public void addLogoutBlacklistToken(String token, String username) {
        long atExpiration = jwtProvider.getExpiryTime(token); // 만료시간(ms)
        long now = System.currentTimeMillis();
        long atTtl = atExpiration - now;
        if (atTtl > 0) { // 만료된 토큰이 아닐 때만
            redisTemplate.opsForValue().set(
                    RedisKeyUtils.logoutBlackListKey(username), token, atTtl, TimeUnit.MILLISECONDS
            );
        }
    }
    public void addBlacklistToken(String token, String username) {
        long atExpiration = jwtProvider.getExpiryTime(token); // 만료시간(ms)
        long now = System.currentTimeMillis();
        long atTtl = atExpiration - now;
        if (atTtl > 0) { // 만료된 토큰이 아닐 때만
            redisTemplate.opsForValue().set(
                    RedisKeyUtils.blackListKey(username), token, atTtl, TimeUnit.MILLISECONDS
            );
        }
    }
}
