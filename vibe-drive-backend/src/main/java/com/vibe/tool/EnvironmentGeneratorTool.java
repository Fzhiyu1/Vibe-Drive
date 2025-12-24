package com.vibe.tool;

import com.vibe.model.DriverBiometrics;
import com.vibe.model.Environment;
import com.vibe.model.LocationInfo;
import com.vibe.model.enums.*;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 环境生成工具
 * 供 AI Agent 调用，根据场景描述生成完整的环境数据
 */
@Component
public class EnvironmentGeneratorTool {

    @Tool("""
        生成车载环境数据。根据用户描述的驾驶场景，设置所有环境参数。
        必须根据场景语义合理推断各项参数：
        - 深夜场景：timeOfDay 设为 night/midnight，疲劳水平较高
        - 高速场景：gpsTag 设为 highway，车速 80-120
        - 堵车场景：车速 0-30，压力水平较高
        - 家庭出游：乘客数 3-5，情绪 happy
        """)
    public Environment generateEnvironment(
            @P("位置类型: highway/tunnel/bridge/urban/suburban/mountain/coastal/parking") String gpsTag,
            @P("天气: sunny/cloudy/rainy/snowy/foggy") String weather,
            @P("车速 km/h (0-200)") int speed,
            @P("用户情绪: happy/calm/tired/stressed/excited") String userMood,
            @P("时段: dawn/morning/noon/afternoon/evening/night/midnight") String timeOfDay,
            @P("乘客数量 (1-7)") int passengerCount,
            @P("城市名称，如：上海市、北京市") String cityName,
            @P("道路名称，如：沪杭高速、南京路") String roadName,
            @P("心率 bpm (60-120)") int heartRate,
            @P("压力水平 (0.0-1.0)") double stressLevel,
            @P("疲劳水平 (0.0-1.0)") double fatigueLevel
    ) {
        // 解析枚举值
        GpsTag gpsTagEnum = parseGpsTag(gpsTag);
        Weather weatherEnum = parseWeather(weather);
        UserMood userMoodEnum = parseUserMood(userMood);
        TimeOfDay timeOfDayEnum = parseTimeOfDay(timeOfDay);

        // 构建生理数据
        DriverBiometrics biometrics = new DriverBiometrics(
                clamp(heartRate, 40, 200),
                clamp(stressLevel, 0.0, 1.0),
                clamp(fatigueLevel, 0.0, 1.0),
                36.5 // 默认体温
        );

        // 构建位置信息
        LocationInfo location = LocationInfo.builder()
                .latitude(31.2304)
                .longitude(121.4737)
                .cityName(cityName != null ? cityName : "未知城市")
                .roadName(roadName != null ? roadName : "未知道路")
                .build();

        // 构建环境对象
        return Environment.builder()
                .gpsTag(gpsTagEnum)
                .weather(weatherEnum)
                .speed(clamp(speed, 0, 200))
                .userMood(userMoodEnum)
                .timeOfDay(timeOfDayEnum)
                .passengerCount(clamp(passengerCount, 1, 7))
                .biometrics(biometrics)
                .location(location)
                .build();
    }

    private GpsTag parseGpsTag(String value) {
        try {
            return GpsTag.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return GpsTag.URBAN;
        }
    }

    private Weather parseWeather(String value) {
        try {
            return Weather.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return Weather.SUNNY;
        }
    }

    private UserMood parseUserMood(String value) {
        try {
            return UserMood.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return UserMood.CALM;
        }
    }

    private TimeOfDay parseTimeOfDay(String value) {
        try {
            return TimeOfDay.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return TimeOfDay.MORNING;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
