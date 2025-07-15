package com.wego.wego.external.tourapi.location.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "city_code")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CityCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "city_code_id")
    private Long cityCodeId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_code_id",nullable = false)
    @ToString.Exclude
    private AreaCode areaCode;

    @Column(name = "city_code",nullable = false)
    private String cityCode;

//    @OneToMany(mappedBy = "cityCode",fetch = FetchType.LAZY)
//    @ToString.Exclude
//    private List<Place> places=new ArrayList<Place>();

    @Column(nullable = false,unique = true)
    private String name;

    public void changeCityCode(String newCityCode) {
        this.cityCode = newCityCode;
    }

    public CityCode changeAll(String name, String cityCode) {
        this.name = name;
        this.cityCode = cityCode;

        return this;
    }

    public static CityCode createCityCode(String cityCode, String name, AreaCode areaCode) {
        CityCode newCityCode = new CityCode();
        newCityCode.cityCode = cityCode;
        newCityCode.name = name;
        newCityCode.areaCode = areaCode;
        return newCityCode;
    }


    public void connectToAreaCode(AreaCode areaCode) {
        this.areaCode = areaCode;
        areaCode.getCityCodes().add(this);
    }


}

