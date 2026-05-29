package com.example.camping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FavoriteRequest {
    @JsonProperty("spot_id")
    public String spotId;

    @JsonProperty("spot_name")
    public String spotName;

    @JsonProperty("is_favorite")
    public boolean isFavorite;
}
