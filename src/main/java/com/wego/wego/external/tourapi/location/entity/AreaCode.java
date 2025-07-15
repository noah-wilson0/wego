package com.wego.wego.external.tourapi.location.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "area_code")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AreaCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "area_code_id")
    private Long areaCodeId; //대체키(기본키)

    @OneToMany(mappedBy = "areaCode", fetch = FetchType.LAZY)
    private List<CityCode> cityCodes=new ArrayList<CityCode>();

    @Column(nullable = false, unique = true)
    private String name; //비즈니스 키

    @Column(name = "area_code", nullable = false, unique = true)
    private String areaCode;

    public void changeCode(String newCode) {
        this.areaCode = newCode;
    }

    public AreaCode changeAll(String name, String areaCode) {
        this.name = name;
        this.areaCode = areaCode;

        return this;
    }

    public AreaCode createAreaCode(String name, String areaCode) {
        AreaCode newAreaCode = new AreaCode();
        newAreaCode.name=name;
        newAreaCode.areaCode=areaCode;
        return newAreaCode;
    }
    public void addCityCode(CityCode cityCode) {
        cityCodes.add(cityCode);
        cityCode.connectToAreaCode(this);
    }

}

