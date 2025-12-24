package com.vibe.simulator;

import com.vibe.model.DriverBiometrics;
import com.vibe.model.Environment;
import com.vibe.model.LocationInfo;
import com.vibe.model.PoiInfo;
import com.vibe.model.enums.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 环境模拟器
 * 用于生成模拟的车载环境数据
 */
@Component
public class EnvironmentSimulator {

    private final Random random = new Random();

    /**
     * 根据场景类型生成环境数据
     */
    public Environment generateScenario(ScenarioType type) {
        return switch (type) {
            case LATE_NIGHT_RETURN -> generateLateNightReturn();
            case WEEKEND_FAMILY_TRIP -> generateWeekendFamilyTrip();
            case MORNING_COMMUTE -> generateMorningCommute();
            case RANDOM -> generateRandom();
        };
    }

    /**
     * 深夜归途场景
     */
    private Environment generateLateNightReturn() {
        return Environment.builder()
            .gpsTag(GpsTag.HIGHWAY)
            .weather(Weather.SUNNY)
            .timeOfDay(TimeOfDay.NIGHT)
            .passengerCount(1)
            .speed(80 + random.nextInt(20))
            .userMood(UserMood.TIRED)
            .biometrics(generateTiredBiometrics())
            .location(generateHighwayLocation())
            .build();
    }

    /**
     * 周末家庭出游场景
     */
    private Environment generateWeekendFamilyTrip() {
        return Environment.builder()
            .gpsTag(GpsTag.COASTAL)
            .weather(Weather.SUNNY)
            .timeOfDay(TimeOfDay.MORNING)
            .passengerCount(3 + random.nextInt(2))
            .speed(40 + random.nextInt(30))
            .userMood(UserMood.HAPPY)
            .biometrics(generateNormalBiometrics())
            .location(generateCoastalLocation())
            .build();
    }

    /**
     * 通勤早高峰场景
     */
    private Environment generateMorningCommute() {
        return Environment.builder()
            .gpsTag(GpsTag.URBAN)
            .weather(randomWeather())
            .timeOfDay(TimeOfDay.MORNING)
            .passengerCount(1)
            .speed(20 + random.nextInt(40))
            .userMood(UserMood.CALM)
            .biometrics(generateStressedBiometrics())
            .location(generateUrbanLocation())
            .build();
    }

    /**
     * 随机场景
     */
    private Environment generateRandom() {
        return Environment.builder()
            .gpsTag(randomGpsTag())
            .weather(randomWeather())
            .timeOfDay(randomTimeOfDay())
            .passengerCount(1 + random.nextInt(4))
            .speed(random.nextInt(120))
            .userMood(randomMood())
            .biometrics(generateRandomBiometrics())
            .location(generateRandomLocation())
            .build();
    }

    /**
     * 环境渐变（模拟真实驾驶）
     */
    public Environment evolve(Environment current, int deltaSeconds) {
        int speedDelta = random.nextInt(11) - 5; // -5 to +5
        double newSpeed = Math.max(0, Math.min(150, current.speed() + speedDelta));

        return Environment.builder()
            .gpsTag(current.gpsTag())
            .weather(current.weather())
            .timeOfDay(current.timeOfDay())
            .passengerCount(current.passengerCount())
            .speed(newSpeed)
            .userMood(current.userMood())
            .biometrics(current.biometrics())
            .location(current.location())
            .build();
    }

    private GpsTag randomGpsTag() {
        GpsTag[] values = GpsTag.values();
        return values[random.nextInt(values.length)];
    }

    private Weather randomWeather() {
        Weather[] values = Weather.values();
        return values[random.nextInt(values.length)];
    }

    private TimeOfDay randomTimeOfDay() {
        TimeOfDay[] values = TimeOfDay.values();
        return values[random.nextInt(values.length)];
    }

    private UserMood randomMood() {
        UserMood[] values = UserMood.values();
        return values[random.nextInt(values.length)];
    }

    // ========== 生理数据生成方法 ==========

    private DriverBiometrics generateNormalBiometrics() {
        return new DriverBiometrics(
            70 + random.nextInt(15),  // 心率 70-85
            0.2 + random.nextDouble() * 0.2,  // 压力 0.2-0.4
            0.1 + random.nextDouble() * 0.2,  // 疲劳 0.1-0.3
            36.3 + random.nextDouble() * 0.4  // 体温 36.3-36.7
        );
    }

    private DriverBiometrics generateTiredBiometrics() {
        return new DriverBiometrics(
            65 + random.nextInt(10),  // 心率偏低 65-75
            0.4 + random.nextDouble() * 0.2,  // 压力中等 0.4-0.6
            0.6 + random.nextDouble() * 0.3,  // 疲劳较高 0.6-0.9
            36.2 + random.nextDouble() * 0.3  // 体温略低
        );
    }

    private DriverBiometrics generateStressedBiometrics() {
        return new DriverBiometrics(
            85 + random.nextInt(20),  // 心率偏高 85-105
            0.7 + random.nextDouble() * 0.2,  // 压力较高 0.7-0.9
            0.3 + random.nextDouble() * 0.2,  // 疲劳中等 0.3-0.5
            36.5 + random.nextDouble() * 0.5  // 体温正常偏高
        );
    }

    private DriverBiometrics generateRandomBiometrics() {
        return new DriverBiometrics(
            60 + random.nextInt(40),  // 心率 60-100
            random.nextDouble(),  // 压力 0-1
            random.nextDouble() * 0.8,  // 疲劳 0-0.8
            36.0 + random.nextDouble()  // 体温 36-37
        );
    }

    // ========== 位置信息生成方法 ==========

    private LocationInfo generateHighwayLocation() {
        return LocationInfo.builder()
            .latitude(31.2304 + random.nextDouble() * 0.5)
            .longitude(121.4737 + random.nextDouble() * 0.5)
            .cityName("上海市")
            .districtName("浦东新区")
            .roadName("沪杭高速")
            .nearbyPois(List.of(
                new PoiInfo("沪杭高速服务区", "gas_station", 5000),
                new PoiInfo("嘉兴服务区", "restaurant", 15000)
            ))
            .build();
    }

    private LocationInfo generateCoastalLocation() {
        return LocationInfo.builder()
            .latitude(30.2741 + random.nextDouble() * 0.1)
            .longitude(120.1551 + random.nextDouble() * 0.1)
            .cityName("杭州市")
            .districtName("西湖区")
            .roadName("西湖景区道路")
            .nearbyPois(List.of(
                new PoiInfo("西湖风景区", "scenic", 500),
                new PoiInfo("楼外楼", "restaurant", 800),
                new PoiInfo("雷峰塔", "scenic", 1200)
            ))
            .build();
    }

    private LocationInfo generateUrbanLocation() {
        return LocationInfo.builder()
            .latitude(31.2304 + random.nextDouble() * 0.05)
            .longitude(121.4737 + random.nextDouble() * 0.05)
            .cityName("上海市")
            .districtName("黄浦区")
            .roadName("南京东路")
            .nearbyPois(List.of(
                new PoiInfo("南京路步行街", "mall", 200),
                new PoiInfo("外滩", "scenic", 800),
                new PoiInfo("星巴克", "cafe", 100)
            ))
            .build();
    }

    private LocationInfo generateRandomLocation() {
        String[] cities = {"上海市", "北京市", "杭州市", "深圳市", "广州市"};
        String[] districts = {"市中心", "郊区", "新区", "开发区"};
        String city = cities[random.nextInt(cities.length)];
        String district = districts[random.nextInt(districts.length)];

        return LocationInfo.builder()
            .latitude(30 + random.nextDouble() * 10)
            .longitude(115 + random.nextDouble() * 10)
            .cityName(city)
            .districtName(district)
            .roadName("主干道")
            .nearbyPois(List.of(
                new PoiInfo("加油站", "gas_station", 500 + random.nextInt(2000))
            ))
            .build();
    }
}
