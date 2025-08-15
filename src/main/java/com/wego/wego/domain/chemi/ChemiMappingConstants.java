package com.wego.wego.domain.chemi;

import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor
public final class ChemiMappingConstants {

    public static final Map<Integer, Map<String, String>> CHEMI_MAP = Map.ofEntries(
            Map.entry(1, Map.of("1", "프로 계획러", "2", "슬로우 트레블러", "3", "로컬 레이더")),
            Map.entry(2, Map.of("1", "푸드 파이터", "2", "감성 여행가", "3", "가성비 여행러")),
            Map.entry(3, Map.of("1", "대장님과 조수", "2", "로컬 적응 여행가", "3", "솔로 여행가")),
            Map.entry(4, Map.of("1", "핫플에이트", "2", "스포티 크루", "3", "문화 탐험가")),
            Map.entry(5, Map.of("1", "즉흥 여행가", "2", "자연광", "3", "여행 플렉서")),
            Map.entry(6, Map.of("1", "릴렉서러", "2", "포토그래퍼", "3", "오지 탐험가")),
            Map.entry(7, Map.of("1", "페스타 러버", "2", "프로 계획러", "3", "슬로우 트레블러")),
            Map.entry(8, Map.of("1", "로컬 레이더", "2", "푸드 파이터", "3", "감성 여행가")),
            Map.entry(9, Map.of("1", "가성비 여행러", "2", "대장님과 조수", "3", "로컬 적응 여행가")),
            Map.entry(10, Map.of("1", "솔로 여행가", "2", "핫플에이트", "3", "스포티 크루")),
            Map.entry(11, Map.of("1", "문화 탐험가", "2", "즉흥 여행가", "3", "자연광")),
            Map.entry(12, Map.of("1", "여행 플렉서", "2", "릴렉서러", "3", "포토그래퍼")),
            Map.entry(13, Map.of("1", "오지 탐험가", "2", "페스타 러버", "3", "프로 계획러")),
            Map.entry(14, Map.of("1", "슬로우 트레블러", "2", "로컬 레이더", "3", "푸드 파이터")),
            Map.entry(15, Map.of("1", "감성 여행가", "2", "가성비 여행러", "3", "대장님과 조수")),
            Map.entry(16, Map.of("1", "로컬 적응 여행가", "2", "솔로 여행가", "3", "핫플에이트")),
            Map.entry(17, Map.of("1", "스포티 크루", "2", "문화 탐험가", "3", "즉흥 여행가")),
            Map.entry(18, Map.of("1", "자연광", "2", "여행 플렉서", "3", "릴렉서러")),
            Map.entry(19, Map.of("1", "포토그래퍼", "2", "오지 탐험가", "3", "페스타 러버"))
    );

    public static final Map<String, Integer> TEMPLATE;
    static {
        TEMPLATE = new LinkedHashMap<>();
        CHEMI_MAP.values().forEach(innerMap ->
                innerMap.values().forEach(chemi ->
                        TEMPLATE.putIfAbsent(chemi, 0)
                )
        );
    }

    public static Map<String, Integer> newScoreTable() {
        return new LinkedHashMap<>(TEMPLATE);
    }
}
