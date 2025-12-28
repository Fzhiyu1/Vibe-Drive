package com.vibe.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 高德地理编码工具
 * 根据地名查询坐标
 */
@Component
public class GeocodingTool {

    private static final Logger log = LoggerFactory.getLogger(GeocodingTool.class);

    @Value("${amap.key:a0d1d4b964ebf5e1aafebf3d0268222b}")
    private String amapKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool("""
        根据地名查询地理坐标。
        在生成环境时，如果用户提到了具体地点（如城市、景点、道路），
        必须先调用此工具获取准确坐标，再生成环境数据。

        示例：
        - 输入"西藏拉萨" -> 返回拉萨的坐标
        - 输入"杭州西湖" -> 返回西湖的坐标
        - 输入"北京故宫" -> 返回故宫的坐标
        """)
    public GeoResult searchLocation(
            @P("要查询的地名，如：拉萨、杭州西湖、北京故宫") String address
    ) {
        try {
            String url = String.format(
                "https://restapi.amap.com/v3/geocode/geo?key=%s&address=%s",
                amapKey, address
            );

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if ("1".equals(root.path("status").asText())
                && root.path("count").asInt() > 0) {

                JsonNode geocode = root.path("geocodes").get(0);
                String location = geocode.path("location").asText();
                String[] coords = location.split(",");

                double longitude = Double.parseDouble(coords[0]);
                double latitude = Double.parseDouble(coords[1]);
                String formattedAddress = geocode.path("formatted_address").asText();
                String city = geocode.path("city").asText();
                String province = geocode.path("province").asText();

                log.info("地理编码成功: {} -> ({}, {})", address, latitude, longitude);

                return new GeoResult(
                    true,
                    latitude,
                    longitude,
                    city.isEmpty() ? province : city,
                    formattedAddress
                );
            } else {
                log.warn("地理编码失败: {}", address);
                return new GeoResult(false, 0, 0, "", "未找到该地点");
            }
        } catch (Exception e) {
            log.error("地理编码异常: {}", e.getMessage());
            return new GeoResult(false, 0, 0, "", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 地理编码结果
     */
    public record GeoResult(
        boolean success,
        double latitude,
        double longitude,
        String cityName,
        String formattedAddress
    ) {}
}
