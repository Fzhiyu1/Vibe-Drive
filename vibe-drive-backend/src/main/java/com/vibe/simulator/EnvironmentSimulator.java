package com.vibe.simulator;

import com.vibe.model.Environment;
import com.vibe.model.enums.*;
import org.springframework.stereotype.Component;

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
}
