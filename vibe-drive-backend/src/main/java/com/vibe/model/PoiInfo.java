package com.vibe.model;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 兴趣点信息
 * 表示附近的地点信息
 */
@Description("兴趣点信息，表示附近的地点")
public record PoiInfo(
    @NotBlank(message = "POI name cannot be blank")
    @Description("地点名称")
    String name,

    @NotBlank(message = "POI category cannot be blank")
    @Description("地点类别：restaurant/scenic/mall/gas_station/parking/hotel/hospital")
    String category,

    @Min(value = 0, message = "Distance must be non-negative")
    @Description("距离当前位置的距离，单位米")
    int distanceMeters
) {
    /**
     * 判断是否为餐饮类
     */
    public boolean isRestaurant() {
        return "restaurant".equalsIgnoreCase(category) || "cafe".equalsIgnoreCase(category);
    }

    /**
     * 判断是否为景点
     */
    public boolean isScenic() {
        return "scenic".equalsIgnoreCase(category) || "park".equalsIgnoreCase(category);
    }

    /**
     * 判断是否在步行范围内（500米）
     */
    public boolean isWalkable() {
        return distanceMeters <= 500;
    }
}
