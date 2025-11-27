package com.wego.wego.domain.plan.dto.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wego.wego.domain.plan.dto.edit.TravelPlanNormalizeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevisePlanNormalize {

    private String label;
    private LocalDate start_date;
    private LocalDate end_date;
    private List<DaySchedule> days;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DaySchedule {
        private LocalDate date;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        private LocalTime start_time;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        private LocalTime end_time;

        private List<PlaceItem> places;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlaceItem {
        private String contentId;
        private String placeType;
        private String title;
        private String image;
        private int sequence;
        private double longitude;
        private double latitude;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        private LocalTime startTime;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        private LocalTime endTime;
    }

    /**
     * ✅ 일반 편집용 TravelPlanNormalizeResponse → AI revise용 RevisePlanNormalize 변환 메서드
     *  - routes 정보는 무시하고, days/places만 복사
     */
    public static RevisePlanNormalize from(TravelPlanNormalizeResponse src) {
        if (src == null) {
            return null;
        }

        List<DaySchedule> daySchedules = null;
        if (src.days() != null) {
            daySchedules = src.days().stream()
                    .map(day -> {
                        List<PlaceItem> placeItems = null;
                        if (day.places() != null) {
                            placeItems = day.places().stream()
                                    .map(p -> PlaceItem.builder()
                                            .contentId(p.content_id())
                                            .placeType(p.placeType())
                                            .title(p.title())
                                            .image(p.image())
                                            .sequence(p.sequence())
                                            .longitude(p.longitude())
                                            .latitude(p.latitude())
                                            .startTime(p.start_time())
                                            .endTime(p.end_time())
                                            .build()
                                    )
                                    .collect(Collectors.toList());
                        }

                        return DaySchedule.builder()
                                .date(day.date())
                                .start_time(day.start_time())
                                .end_time(day.end_time())
                                .places(placeItems)
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return RevisePlanNormalize.builder()
                .label(src.label())
                .start_date(src.start_date())
                .end_date(src.end_date())
                .days(daySchedules)
                .build();
    }
}
