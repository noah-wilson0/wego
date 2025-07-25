package com.wego.wego.domain.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.*;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 케미 적용 후 프롬프트를 케미 정보가 들어가게 수정해야함
 */
@Slf4j
@Service
public class GeminiRequestService {
    private final ChatClient chatClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;


    public GeminiRequestService(ChatClient.Builder builder, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String getGeminiTravelPlan(String uuid) {
        TravelPlanForGeminiRequest travelPlanForGeminiRequest = getTravelDateTime(uuid);
        String request;
        String response;

        try {
            // 요청 JSON 직렬화
            request = objectMapper.writeValueAsString(travelPlanForGeminiRequest);

            response = createResponse(travelPlanForGeminiRequest);

//            log.info("🧪 Gemini 예시 응답: {}", response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }

        String prompt = """
너는 여행 플래너야.
사용자가 아래와 같은 여행 조건을 제공했어:

%s

이 조건을 기반으로 서울 여행 일정을 추천해줘.
각 날짜(day)에는 다음 항목들을 반드시 포함해야 해:

- 여행 장소(places): `name`, `addr`, `tel` key만 사용
- 숙소(accommodations): `name`, `addr`, `tel` key만 사용

❗️**중요한 형식 규칙**:
1. `places`, `accommodations` 항목은 오직 `title`, `addr`, `tel` key만 사용해야 해.
2. key 이름은 절대 바꾸지 마. ( `location`, `description` 등 금지)
3. JSON 이외 텍스트는 포함하지 마. JSON만 깔끔히 출력해.
4. 장소 정보는 다음 기준을 지켜줘:
    - name: 한국관광공사 등록 공식 명칭
    - addr: 도로명 주소 전체
    - tel: 가능하면 정확한 전화번호 제공 (없으면 null)
5. 형식은 아래 예시를 그대로 따라야 해:

%s

🎯 **일정 작성 시 추가 요구사항**: 
- 하루에 4~6개의 여행 장소를 포함해서 일정을 촘촘하게 구성해줘.
- 여행 시간 범위(`start_time`~`end_time`) 내에서 충분히 이동 가능하고 알차게 다닐 수 있도록 추천해.
- 다양한 장소 유형(자연/문화/음식 등)을 조화롭게 포함해.
- 유명 관광지를 우선 고려하되, 너무 혼잡하지 않은 명소도 함께 제안해줘.
- 숙소는 매일 1곳으로 제한해줘.

""".formatted(request, response);

        String content = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();

        String cleanedContent = sanitizeJson(content);
        return cleanedContent;
    }


    public String retryGeminiTravelPlan(String previous, RetryFailedDay retryFailedDay) {

        String failedSummary = """
📅 날짜: %s
⏰ 시간: %s ~ %s
🧭 문제된 장소:
- %s (%s)
""".formatted(
                retryFailedDay.date(),
                retryFailedDay.startTime(),
                retryFailedDay.endTime(),
                retryFailedDay.failedPlaces().title(),
                retryFailedDay.failedPlaces().addr()
        );

        String prompt = """
            너는 여행 플래너야.
            
            다음은 사용자가 요청한 전체 여행 일정이야:
            %s
            
            하지만 일정 중 일부 장소가 실제로 존재하지 않거나, 폐업했거나, 위치 정보가 불명확했어.
            아래 날짜의 문제된 장소들만 대체해줘:
            
            %s
            
            🎯 요청 사항:
            - 기존 일정 전체는 그대로 두고, 위에 나열된 장소만 비슷한 다른 장소로 바꿔줘.
            - 일정의 **흐름이나 동선**, 장소의 **성격(맛집, 자연, 역사 등)** 을 최대한 유지해줘.
            - 추천하는 장소는 동일 날짜 내에 이동 가능한 거리와 시간 안에서 선택해.
            - 장소 개수는 문제 장소 개수만큼만 추천해.
            
            📌 출력 규칙:
            - 오직 JSON만 반환해. 절대 텍스트 설명 붙이지 마.
            - JSON 배열로 장소들을 반환하고, 각 객체는 아래 키를 반드시 포함해:
                - name: 한국관광공사 등록 공식 명칭
                - addr: 도로명 주소 전체
                - tel: 가능하면 정확한 전화번호 제공 (없으면 null)
            
            예시:
              {
                "title": "추천 장소 1",
                "addr": "정확한 도로명 주소",
                "tel": "전화번호 또는 null"
              }
            """.formatted(previous, failedSummary);

        String content = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();

        return sanitizeJson(content);
    }

    private String createResponse(TravelPlanForGeminiRequest travelPlanForGeminiRequest) throws JsonProcessingException {
        String response;

        // 예시 응답 JSON 직렬화
        TempTravelPlanGeminiResponse.Days.Places place1 = new TempTravelPlanGeminiResponse.Days.Places(
                "성산 일출봉",
                "제주특별자치도 서귀포시 성산읍",
                "064-123-4567"
        );
        TempTravelPlanGeminiResponse.Days.Places place2 = new TempTravelPlanGeminiResponse.Days.Places(
                "섭지코지",
                "제주특별자치도 서귀포시 성산읍 고성리",
                "064-987-6543"
        );
        TempTravelPlanGeminiResponse.Days.Accommodation accommodation = new TempTravelPlanGeminiResponse.Days.Accommodation(
                "라마다 제주 호텔",
                "제주특별자치도 제주시 연동",
                "064-000-1111"
        );
        TempTravelPlanGeminiResponse.Days day = new TempTravelPlanGeminiResponse.Days(
                "2025-07-15",
                "09:00",
                "18:00",
                List.of(place1, place2),
                List.of(accommodation)
        );
        TempTravelPlanGeminiResponse exampleResponse = new TempTravelPlanGeminiResponse(
                travelPlanForGeminiRequest.start_date(),
                travelPlanForGeminiRequest.end_date(),
                List.of(day)
        );

        response = objectMapper.writeValueAsString(exampleResponse);
        return response;
    }

    private TravelPlanForGeminiRequest getTravelDateTime(String uuid) {
        String travelDate = getTravelDate(uuid);
        String travelTime = getTravelTime(uuid);
        TempTravelDateRequest tempTravelDateRequest;
        TempTravelTimeRequest tempTravelTimeRequest;
        try {
            tempTravelDateRequest = objectMapper.readValue(travelDate, TempTravelDateRequest.class);
            tempTravelTimeRequest = objectMapper.readValue(travelTime, TempTravelTimeRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        List<TravelPlanForGeminiRequest.TravelDayForAi> travelDayForAiList = tempTravelTimeRequest.travelDayTimes().stream()
                .map(day -> new TravelPlanForGeminiRequest.TravelDayForAi(
                        day.date().toString(),
                        day.startTime().toString(),
                        day.endTime().toString()
                ))
                .toList();
        return new TravelPlanForGeminiRequest(
                tempTravelDateRequest.startDate(),
                tempTravelDateRequest.endDate(),
                travelDayForAiList
        );


    }

    private String getTravelDate(String uuid) {
        return redisTemplate.opsForValue().get(RedisKeyUtils.dateKey(uuid));
    }

    private String getTravelTime(String uuid) {
        return redisTemplate.opsForValue().get(RedisKeyUtils.timeKey(uuid));
    }
    private String sanitizeJson(String raw) {
        return raw.replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
    }



}
