package com.wego.wego.domain.plan.controller.draft.auto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import com.wego.wego.domain.plan.dto.TempTravelDateRequest;
import com.wego.wego.domain.plan.dto.TempTravelTimeRequest;
import com.wego.wego.global.util.RedisKeyUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * DraftPlanAutoController 통합 테스트
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8080",            // 스프링을 8080에 실제로 띄움
                "auto.base-url=http://localhost:7070" // FastAPI 주소
        }
)
@Transactional
@AutoConfigureMockMvc
class DraftPlanAutoControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MemberRepository memberRepository;

    private final String uuid = "test-uuid-1234";

    @BeforeEach
    void setUp() throws Exception {
        // ---- Redis에 더미 데이터 저장 ----
        String slugJson = objectMapper.writeValueAsString(
                new DummySlug("seoul")
        );
        String dateJson = objectMapper.writeValueAsString(
                new TempTravelDateRequest(LocalDate.parse("2025-10-24"), LocalDate.parse("2025-10-25"))
        );
        String timeJson = objectMapper.writeValueAsString(
                List.of(
                        new TempTravelTimeRequest.TravelDayTimes(LocalDate.parse("2025-10-24"), LocalTime.parse("10:00"), LocalTime.parse("22:00")),
                        new TempTravelTimeRequest.TravelDayTimes(LocalDate.parse("2025-10-25"), LocalTime.parse("10:00"), LocalTime.parse("22:00"))
                )
        );

        redisTemplate.opsForValue().set(RedisKeyUtils.slugKey(uuid), slugJson);
        redisTemplate.opsForValue().set(RedisKeyUtils.dateKey(uuid), dateJson);
        redisTemplate.opsForValue().set(RedisKeyUtils.timeKey(uuid), timeJson);

        // ---- SecurityContextHolder에 Member 삽입 ----
        Member member = memberRepository.findById(3L).get();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(member, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        // ---- Redis 정리 ----
        redisTemplate.delete(RedisKeyUtils.slugKey(uuid));
        redisTemplate.delete(RedisKeyUtils.dateKey(uuid));
        redisTemplate.delete(RedisKeyUtils.timeKey(uuid));

        // ---- SecurityContext clear ----
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("auto-schedule API 호출 성공")
    void autoSchedule_success() throws Exception {
        mvc.perform(post("/draft-plans/{uuid}/auto-schedule", uuid))
                .andExpect(status().isOk());
    }

    @Test
    void autoTempTravelPlan() {
    }

    // ===== 더미 DTO =====
    record DummySlug(String slug) {}


}