package com.wego.wego.domain.placeStorage.dto;

import com.querydsl.core.annotations.QueryProjection;
import com.wego.wego.external.tourapi.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
//@AllArgsConstructor
@NoArgsConstructor
public class PlaceStorageItemDto {
    private int sequence;

    private String contentId;
    private String title;
    private String image;
    private String placeType;
    private String addr;
    private double longitude;
    private double latitude;
    private Double averageRating;
    private Long likeCount;

    @QueryProjection
    public PlaceStorageItemDto(int sequence, String contentId, String title, String image,
                                  String placeType, String addr,
                                  double longitude, double latitude,
                                  Double averageRating, Long likeCount) {
        this.sequence = sequence;
        this.contentId = contentId;
        this.title = title;
        this.image = image;
        this.placeType = placeType;
        this.addr = addr;
        this.longitude = longitude;
        this.latitude = latitude;
        this.averageRating = averageRating;
        this.likeCount = likeCount;
    }


//    public PlaceStorageItemDto(int sequence, Place place) {
//        this.sequence = sequence;
//        this.contentId = place.getContentId();
//        this.title = place.getTitle();
//        this.image = place.getImage();
//        this.placeType = place.getPlaceType();
//        this.addr = place.getAddr1();
//        this.longitude = Double.parseDouble(place.getLongitude());
//        this.latitude = Double.parseDouble(place.getLatitude());
//        this.averageRating = place.getAverageRating();
//        this.likeCount = place.getLikeCount();
//    }

}
