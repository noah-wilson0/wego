package com.wego.wego.external.tourapi.place.entity;


import com.wego.wego.external.tourapi.location.entity.CityCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "place")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long id; //대체키(기본키)

    @Column(name = "content_id", nullable = false , unique = true)
    private String contentId; //비즈니스 키

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_code_id", nullable = false)
    private CityCode cityCode;

    private String placeType;

    @Column(nullable = false)
    private String addr1;

    private String addr2;

    private String image;

    @Column(nullable = false)
    private String longitude; //경도

    @Column(nullable = false)
    private String latitude;  //위도

    private String tel;

    @Column(name = "average_rating", nullable = false)
    @Builder.Default
    private Double averageRating=0.0;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Long likeCount=0L;

    public void changeContentId(String contentId) {
        this.contentId = contentId;
    }

    public void changeTitle(String title) {
        this.title = title;
    }
    public void changeCityCode(CityCode cityCode) {
        this.cityCode = cityCode;
    }

    public void changeAddr1(String addr1) {
        this.addr1 = addr1;
    }

    public void changeAddr2(String addr2) {
        this.addr2 = addr2;
    }

    public void changeImage(String image) {
        this.image = image;
    }

    public void changeLongitude(String longitude) {
        this.longitude = longitude;
    }

    public void changeLatitude(String latitude) {
        this.latitude = latitude;
    }

    public void changeTel(String tel) {
        this.tel = tel;
    }

    public Place changeExceptAverageRatingAndLikeCount(
            String contentId,
            String title,
            CityCode cityCode,
            String placeType,
            String addr1,
            String addr2,
            String image,
            String longitude,
            String latitude,
            String tel
    ) {
        this.contentId = contentId;
        this.title = title;
        this.cityCode = cityCode;
        this.placeType=placeType;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.image = image;
        this.longitude = longitude;
        this.latitude = latitude;
        this.tel = tel;
        return this;
    }



}

