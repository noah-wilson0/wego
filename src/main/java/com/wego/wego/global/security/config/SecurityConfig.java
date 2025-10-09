package com.wego.wego.global.security.config;

import com.wego.wego.global.security.JwtAuthenticationFilter;
import com.wego.wego.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtProvider jwtProvider;
    private final RedisTemplate redisTemplate;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults())
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() //로그아웃 문제
                        .requestMatchers("/auth/sign-in","/members/sign-up",
                                "/auth/me",
                                "/chemis/result", "/chemis/all", "/images/chemi/**",
                                "/places/search",
                                "/draft-plans/slug/*",
                                "/draft-plans/*/meta",
                                "/draft-plans/*/dates","/draft-plans/*/times",
                                "/draft-plans/*/*/paged",
                                "/draft-plans/*/places", "/draft-plans/*/accommodations",
                                "/draft-plans/*/*/route",
//                                "/draft-plans/*/auto-schedule", //테스트 할떄만
                                "/travel-plans/*", //공유 일정 조회
                                "/travel-share-plans/*/settlements/result", //공유자의 정산하기는 비회원도 가능
                                "/feeds/all/paged",
                                "/feeds/{feed_id}/comments",
                                "/api/v1/ai/places","/api/v2/ai/places").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/draft-plans/*").permitAll()
                        .requestMatchers("/auth/sign-out",
                                "/me/check-password", "/me/change-password",
                                "/me/change-info", "/me/chemi",
                                "/me/feeds", "/me/travel-plans","/me/travel-plans/imminent",
                                "/travel-plans/*/me",
                                "/travel-plans/*/share",
                                "/chemis/similar","/chemis/labels",

                                "/feeds/init/**",
                                "/travel-plans/*/settlements/result",
                                "/draft-plans/*/auto-schedule").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/me").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/me",
                                "/travel-plans/*/settlements","/travel-share-plans/*/settlements",
                                "/feeds/*","/feeds/*/comments").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/draft-plans/*",
                                "/feeds", "/feeds/*/comments",
                                "/travel-plans/*/settlements",
                                "/travel-share-plans/*/settlements",
                                "/travel-plans/*/settlements/items",
                                "/travel-share-plans/*/settlements/items").hasRole("USER")
                        .requestMatchers(HttpMethod.PATCH,
                                "/travel-plans/*/settlements",
                                "/travel-share-plans/*/settlements").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider,redisTemplate), UsernamePasswordAuthenticationFilter.class).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt Encoder 사용
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5174","http://localhost:5173")); // 프론트 주소
        config.setAllowedMethods(List.of("GET", "POST", "PATCH","PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 쿠키 포함 허용 시 true

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 모든 경로에 적용
        return source;
    }
}

