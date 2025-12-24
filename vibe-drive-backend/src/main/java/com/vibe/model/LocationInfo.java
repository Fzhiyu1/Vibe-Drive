package com.vibe.model;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 精确位置信息
 * 包含经纬度、地名和附近兴趣点
 */
@Description("精确位置信息，包含经纬度、地名和附近兴趣点")
public record LocationInfo(
    @Description("纬度")
    double latitude,

    @Description("经度")
    double longitude,

    @Description("城市名称")
    String cityName,

    @Description("区/县名称")
    String districtName,

    @Description("道路名称")
    String roadName,

    @Valid
    @Description("附近兴趣点列表")
    List<PoiInfo> nearbyPois
) {
    /**
     * 紧凑构造器
     */
    public LocationInfo {
        if (nearbyPois != null) {
            nearbyPois = List.copyOf(nearbyPois);
        }
    }

    /**
     * 获取完整地址
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (cityName != null) sb.append(cityName);
        if (districtName != null) sb.append(districtName);
        if (roadName != null) sb.append(roadName);
        return sb.toString();
    }

    /**
     * 判断是否有附近兴趣点
     */
    public boolean hasNearbyPois() {
        return nearbyPois != null && !nearbyPois.isEmpty();
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double latitude;
        private double longitude;
        private String cityName;
        private String districtName;
        private String roadName;
        private List<PoiInfo> nearbyPois;

        public Builder latitude(double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder cityName(String cityName) {
            this.cityName = cityName;
            return this;
        }

        public Builder districtName(String districtName) {
            this.districtName = districtName;
            return this;
        }

        public Builder roadName(String roadName) {
            this.roadName = roadName;
            return this;
        }

        public Builder nearbyPois(List<PoiInfo> nearbyPois) {
            this.nearbyPois = nearbyPois;
            return this;
        }

        public LocationInfo build() {
            return new LocationInfo(latitude, longitude, cityName,
                districtName, roadName, nearbyPois);
        }
    }
}
