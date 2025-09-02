package com.wego.wego.global.security;

import com.wego.wego.domain.plan.service.GeminiRequestService;
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
    private String token=null;

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
                path.startsWith("/members/sign-in") ||
                path.startsWith("/members/signup") ||

                path.startsWith("/chemi/all") ||
                path.startsWith("/chemi/result") ||

                path.startsWith("/images/chemi") ||

                path.startsWith("/travel_plan/slug") ||
                path.startsWith("/travel_plan/date") ||
                path.startsWith("/travel_plan/place") ||
                path.startsWith("/travel_plan/route") ||
                path.equals("/travel_plan/recommend") ||
                path.startsWith("/travel_plan/recommend") ||
                path.startsWith("/travel_plan/temp/schedule") ||
                path.startsWith("/feed/all/paged")
        ) {
            log.info("permitAll한 요청");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        } else {

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
                if (redisTemplate.hasKey(RedisKeyUtils.blackListKey(jwtProvider.getAuthentication(token).getName()))) {
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
