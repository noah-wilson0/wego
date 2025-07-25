package com.wego.wego.external.tourapi.place.dto;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
public class PlaceResponse {

    private Response response;
    @Getter
    public static class Response {
        private Body body;
    }

    @Getter
    public static class Body{
        private Items items;
    }

    @Getter
    public static class Items{
        private List<PlaceItem> item;
    }

    @Getter
    @ToString
    public static class PlaceItem {
        private String addr1;
        private String addr2;
        private String areacode;
        private String cat3;
        private String contentid;
        private String firstimage;
        private String mapx; //경도
        private String mapy; //위도
        private String sigungucode;
        private String tel;
        private String title;
        private String contenttypeid;

        public void changeTel(String tel) {
            this.tel = tel;
        }

    }
}
