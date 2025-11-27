package com.wego.wego.domain.plan.dto.edit.route;

import com.wego.wego.external.tourapi.place.entity.Place;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class EditPlanRoutingRequest{
    @Builder.Default
    private List<DailyRouteRequest> days = new ArrayList<>();
    @Builder
    @Getter
    public static class DailyRouteRequest{
        LocalDate date;
        List<Place> places;
        LocalTime start_time;
        LocalTime end_time;
    }
}

