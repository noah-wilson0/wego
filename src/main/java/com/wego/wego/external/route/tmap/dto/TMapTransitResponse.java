package com.wego.wego.external.route.tmap.dto;

import lombok.ToString;

import java.util.List;

public record TMapTransitResponse (
        MetaData metaData
) {
    public record MetaData(
            RequestParameter requestParameters,
            Plan plan
    ) {
        public record RequestParameter(
                String endY,
                String endX,
                String startY,
                String startX,
                String reqDttm
        ) {
        }

        public record Plan(List<Itineraries> itineraries) {

            public record Itineraries(
                    Fare fare,
                    int pathType,
                    int totalTime,
                    int totalWalkTime,
                    int transferCount,
                    int totalDistance,
                    int totalWalkDistance

            ) {
                public record Fare(
                        Regular regular
                ) {
                    public record Regular(
                            int totalFare,
                            Currency currency
                    ) {
                        public record Currency(
                                String symbol,
                                String currency,
                                String currencyCode
                        ) {
                        }
                    }

                }
            }
        }
    }

}
