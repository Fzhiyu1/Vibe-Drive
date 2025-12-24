package com.vibe.model;

import com.vibe.model.enums.*;
import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * 车载环境感知数据
 * 包含地理位置、天气、车速、用户状态等信息
 */
@Description("车载环境感知数据，包含地理位置、天气、车速、用户状态等信息")
public record Environment(
    @NotNull(message = "GPS tag cannot be null")
    @Description("地理标签，表示当前所在位置类型：HIGHWAY/TUNNEL/BRIDGE/URBAN/SUBURBAN/MOUNTAIN/COASTAL/PARKING")
    GpsTag gpsTag,

    @NotNull(message = "Weather cannot be null")
    @Description("当前天气状况：SUNNY/CLOUDY/RAINY/SNOWY/FOGGY")
    Weather weather,

    @Min(value = 0, message = "Speed must be at least 0")
    @Max(value = 200, message = "Speed must be at most 200 km/h")
    @Description("当前车速，单位 km/h，范围 0-200")
    double speed,

    @NotNull(message = "User mood cannot be null")
    @Description("用户情绪状态：HAPPY/CALM/TIRED/STRESSED/EXCITED")
    UserMood userMood,

    @NotNull(message = "Time of day cannot be null")
    @Description("时段：DAWN/MORNING/NOON/AFTERNOON/EVENING/NIGHT/MIDNIGHT")
    TimeOfDay timeOfDay,

    @Min(value = 1, message = "Passenger count must be at least 1")
    @Max(value = 7, message = "Passenger count must be at most 7")
    @Description("车内乘客数量，范围 1-7")
    int passengerCount,

    @NotNull(message = "Route type cannot be null")
    @Description("路线类型：HIGHWAY/URBAN/MOUNTAIN/COASTAL/TUNNEL")
    RouteType routeType,

    @Description("驾驶员生理数据，包含心率、压力、疲劳等指标")
    DriverBiometrics biometrics,

    @Description("精确位置信息，包含经纬度、地名和附近兴趣点")
    LocationInfo location,

    @Description("数据采集时间戳")
    Instant timestamp
) {
    /**
     * 紧凑构造器：校验和默认值
     */
    public Environment {
        if (speed < 0 || speed > 200) {
            throw new IllegalArgumentException("Speed must be between 0 and 200 km/h");
        }
        if (passengerCount < 1 || passengerCount > 7) {
            throw new IllegalArgumentException("Passenger count must be between 1 and 7");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * 根据车速获取当前安全模式
     */
    public SafetyMode getSafetyMode() {
        return SafetyMode.fromSpeed(speed);
    }

    /**
     * 判断是否为高速行驶场景
     */
    public boolean isHighSpeedScenario() {
        return speed >= 100 || gpsTag.isHighSpeedScenario();
    }

    /**
     * 判断是否为恶劣天气
     */
    public boolean isSevereWeather() {
        return weather.isSevereWeather();
    }

    /**
     * 判断是否为深夜场景
     */
    public boolean isLateNight() {
        return timeOfDay.isLateNight();
    }

    /**
     * 判断是否为独自驾驶
     */
    public boolean isSoloDriving() {
        return passengerCount == 1;
    }

    /**
     * 判断是否需要舒缓氛围
     */
    public boolean needsSoothingAmbience() {
        return userMood.needsSoothingAmbience() || isLateNight() || isSevereWeather();
    }

    /**
     * 判断是否适合活力氛围
     */
    public boolean suitableForEnergeticAmbience() {
        return userMood.needsEnergeticAmbience() && !isHighSpeedScenario() && !isSevereWeather();
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Environment Builder
     */
    public static class Builder {
        private GpsTag gpsTag = GpsTag.URBAN;
        private Weather weather = Weather.SUNNY;
        private double speed = 0;
        private UserMood userMood = UserMood.CALM;
        private TimeOfDay timeOfDay = TimeOfDay.MORNING;
        private int passengerCount = 1;
        private RouteType routeType = RouteType.URBAN;
        private DriverBiometrics biometrics;
        private LocationInfo location;
        private Instant timestamp;

        public Builder gpsTag(GpsTag gpsTag) {
            this.gpsTag = gpsTag;
            return this;
        }

        public Builder weather(Weather weather) {
            this.weather = weather;
            return this;
        }

        public Builder speed(double speed) {
            this.speed = speed;
            return this;
        }

        public Builder userMood(UserMood userMood) {
            this.userMood = userMood;
            return this;
        }

        public Builder timeOfDay(TimeOfDay timeOfDay) {
            this.timeOfDay = timeOfDay;
            return this;
        }

        public Builder passengerCount(int passengerCount) {
            this.passengerCount = passengerCount;
            return this;
        }

        public Builder routeType(RouteType routeType) {
            this.routeType = routeType;
            return this;
        }

        public Builder biometrics(DriverBiometrics biometrics) {
            this.biometrics = biometrics;
            return this;
        }

        public Builder location(LocationInfo location) {
            this.location = location;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Environment build() {
            return new Environment(gpsTag, weather, speed, userMood, timeOfDay, passengerCount, routeType, biometrics, location, timestamp);
        }
    }
}
