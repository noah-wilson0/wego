package com.wego.wego.domain.plan.dto.edit;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class EditLanggraphRequest {
    String prompt;
    CompactTravelPlan travelPlan;

    public EditLanggraphRequest(String prompt, TravelPlanNormalizeResponse normalize) {
        this.prompt = prompt;
        this.travelPlan = CompactTravelPlan.from(normalize);
    }

    @Getter
    @Builder
    public static class CompactTravelPlan {
        String label;
        LocalDate startDate;
        LocalDate endDate;
        @Builder.Default
        List<Day> days = new ArrayList<>();

        @Getter
        @Builder
        public static class Day {
            String date;
            String startTime;
            String endTime;
            @Builder.Default
            List<Place> places = new ArrayList<>();

            @Getter
            @Setter
            @Builder
            public static class Place {
                String title;
                int sequence;
            }
        }

        public static CompactTravelPlan from(TravelPlanNormalizeResponse travelPlanNormalizeResponse) {
            CompactTravelPlan compactTravelPlan = CompactTravelPlan.builder()
                    .label(travelPlanNormalizeResponse.label())
                    .startDate(travelPlanNormalizeResponse.start_date())
                    .endDate(travelPlanNormalizeResponse.end_date())
                    .build();


            for (TravelPlanNormalizeResponse.DaySchedule day : travelPlanNormalizeResponse.days()) {
                CompactTravelPlan.Day newDay =  Day.builder()
                        .date(day.date().toString())
                        .startTime(day.start_time().toString())
                        .endTime(day.end_time().toString())
                        .build();

                for (TravelPlanNormalizeResponse.PlaceItem placeItem : day.places()) {
                    newDay.places.add(new CompactTravelPlan.Day.Place(placeItem.title(), placeItem.sequence()));
                }

                compactTravelPlan.days.add(newDay);
            }
            return compactTravelPlan;
        }

    }





}
