package com.wego.wego.global.security;

import com.wego.wego.global.util.RedisKeyUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        log.info("filter init");
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        String path = httpRequest.getRequestURI();
        log.info("path: {}", path);

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            log.info("OPTIONS 요청 - 필터 우회");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (
                path.startsWith("/auth/sign-in") ||
                path.startsWith("/auth/signup") ||

                path.startsWith("/chemis/all") ||
                path.startsWith("/chemis/result") ||

                path.startsWith("/images/chemi") ||

                path.startsWith("/draft-plans/slug")  ||
                path.startsWith("/places/search")  ||
                path.startsWith("/draft-plans/") &&path.endsWith("/meta") ||
                path.startsWith("/draft-plans") &&path.endsWith("/dates") ||
                path.startsWith("/draft-plans") &&path.endsWith("/times") ||
//                path.startsWith("/draft-plans") &&path.endsWith("/auto-schedule") || //테스트할떄만
                path.startsWith("/draft-plans") &&path.endsWith("/places") ||
                path.startsWith("/draft-plans") &&path.endsWith("/accommodations") ||
                path.startsWith("/draft-plans") &&path.endsWith("/route") ||

                path.startsWith("/draft-plans") && ("GET".equalsIgnoreCase(httpRequest.getMethod())) ||
                path.startsWith("/travel-share-plans") && path.endsWith("/settlements/result") ||

                path.startsWith("/feeds/all/paged") ||
                path.startsWith("/ai/places")
        ) {
            log.info("permitAll한 요청");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        } else {
            String token = null;
            //토큰 가져오기
            if (httpRequest.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : httpRequest.getCookies()) {
                    if ("accessToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                    }
                }
            } else {
                log.info("토큰 없음");
                HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
                SecurityContextHolder.clearContext();
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json; charset=UTF-8");
                httpResponse.getWriter().write("{\"error\": \"토큰이 없습니다.\"}");
                return;
            }

            // AT 블랙리스트검사
            if (token != null && jwtProvider.validateToken(token)) {
                if (redisTemplate.hasKey(RedisKeyUtils.blackListKey(jwtProvider.getAuthentication(token).getName()))||
                        redisTemplate.hasKey(RedisKeyUtils.logoutBlackListKey(jwtProvider.getAuthentication(token).getName()))) {
                    log.info("blacklist 확인 실행");
                    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
                    SecurityContextHolder.clearContext();
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.setContentType("application/json; charset=UTF-8");
                    httpResponse.getWriter().write("{\"error\": \"블랙리스트에 등록된 토큰입니다. 재로그인 필요\"}");
                    return;
                } else {
                    log.info("유효성 검사");
                    Authentication authentication = jwtProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}
