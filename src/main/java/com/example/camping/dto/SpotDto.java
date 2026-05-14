package com.example.camping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.bind.annotation.JsonbProperty;

public class SpotDto {
    @JsonProperty("spot_id")
    @JsonbProperty("spot_id")
    private String spotId;

    @JsonProperty("spot_name")
    @JsonbProperty("spot_name")
    private String spotName;

    private String name;
    private String describe;
    private int price;

    @JsonProperty("spot_img_url")
    @JsonbProperty("spot_img_url")
    private String spotImgUrl;

    @JsonProperty("img_url")
    @JsonbProperty("img_url")
    private String imgUrl;

    public SpotDto() {
    }

    public SpotDto(String spotId, String spotName, String name, String describe, int price, String spotImgUrl, String imgUrl) {
        this.spotId = spotId;
        this.spotName = spotName;
        this.name = name;
        this.describe = describe;
        this.price = price;
        this.spotImgUrl = spotImgUrl;
        this.imgUrl = imgUrl;
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public String getSpotName() {
        return spotName;
    }

    public void setSpotName(String spotName) {
        this.spotName = spotName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getSpotImgUrl() {
        return spotImgUrl;
    }

    public void setSpotImgUrl(String spotImgUrl) {
        this.spotImgUrl = spotImgUrl;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}
