package com.vibe.support;

import java.util.List;
import java.util.Random;

/**
 * 叙事模板库
 * 根据环境条件选择合适的叙事文本模板
 */
public final class NarrativeTemplates {

    private static final Random RANDOM = new Random();

    private NarrativeTemplates() {
        // 工具类，禁止实例化
    }

    // ==================== 时段 + 天气组合模板 ====================

    /**
     * 深夜 + 雨天
     */
    public static final List<String> MIDNIGHT_RAIN = List.of(
        "夜深了，窗外的雨声和这首歌很配，让音乐陪你安全到家。",
        "雨夜的路上，愿这首歌温暖你的归途。",
        "深夜的雨，洗去一天的疲惫，慢慢开，安全到家。"
    );

    /**
     * 早晨 + 晴天
     */
    public static final List<String> MORNING_SUNNY = List.of(
        "阳光正好，新的一天从这首歌开始。",
        "早安，今天也是元气满满的一天！",
        "清晨的阳光和音乐，是最好的出发仪式。"
    );

    /**
     * 傍晚 + 海滨
     */
    public static final List<String> EVENING_COASTAL = List.of(
        "夕阳西下，海风轻拂，享受这片刻的宁静。",
        "海边的傍晚，让音乐和海浪一起陪伴你。"
    );

    /**
     * 夜晚 + 高速
     */
    public static final List<String> NIGHT_HIGHWAY = List.of(
        "夜色中的高速路，让音乐陪你穿越星空。",
        "远方的灯火渐近，旅途的终点就在前方。",
        "深夜的高速，愿这首歌为你驱散困意。"
    );

    /**
     * 下午 + 城市
     */
    public static final List<String> AFTERNOON_URBAN = List.of(
        "午后的城市，车水马龙中享受片刻宁静。",
        "城市的节奏，让音乐为你调节心情。"
    );

    /**
     * 黄昏 + 山区
     */
    public static final List<String> EVENING_MOUNTAIN = List.of(
        "山间的黄昏，云雾缭绕，享受这份静谧。",
        "盘山路上，让音乐与风景一起流淌。"
    );

    /**
     * 雪天
     */
    public static final List<String> SNOWY = List.of(
        "窗外飘着雪，让温暖的音乐陪伴你。",
        "银装素裹的世界，慢慢开，安全第一。"
    );

    /**
     * 雾天
     */
    public static final List<String> FOGGY = List.of(
        "雾气弥漫，请减速慢行，让音乐陪你穿越迷雾。",
        "雾中行车，安全第一，音乐相伴。"
    );

    // ==================== 情绪相关模板 ====================

    /**
     * 疲劳状态
     */
    public static final List<String> TIRED = List.of(
        "感觉有些疲惫？让舒缓的音乐帮你放松。",
        "累了就休息一下，安全比什么都重要。",
        "轻柔的旋律，陪你度过这段路程。"
    );

    /**
     * 压力状态
     */
    public static final List<String> STRESSED = List.of(
        "深呼吸，让音乐帮你释放压力。",
        "放下烦恼，享受这段属于自己的时光。"
    );

    /**
     * 开心状态
     */
    public static final List<String> HAPPY = List.of(
        "心情不错！让音乐为你的好心情加分。",
        "快乐的旅途，有音乐相伴更美好。"
    );

    // ==================== 通用模板 ====================

    public static final List<String> DEFAULT = List.of(
        "享受旅途，让音乐陪伴你。",
        "一路有歌，一路有你。",
        "音乐与风景，是旅途最好的伴侣。"
    );

    /**
     * 根据环境条件生成叙事文本
     *
     * @param timeOfDay   时段
     * @param weather     天气
     * @param gpsTag      位置标签
     * @param currentSong 当前歌曲（可选）
     * @return 叙事文本
     */
    public static String generate(String timeOfDay, String weather, String gpsTag, String currentSong) {
        String template = selectTemplate(timeOfDay, weather, gpsTag);
        if (currentSong != null && !currentSong.isBlank()) {
            template = template.replace("这首歌", "《" + currentSong + "》");
        }
        return template;
    }

    /**
     * 根据用户情绪生成叙事文本
     *
     * @param userMood    用户情绪
     * @param currentSong 当前歌曲（可选）
     * @return 叙事文本
     */
    public static String generateByMood(String userMood, String currentSong) {
        List<String> templates = switch (userMood) {
            case "tired" -> TIRED;
            case "stressed" -> STRESSED;
            case "happy", "excited" -> HAPPY;
            default -> DEFAULT;
        };
        String template = randomFrom(templates);
        if (currentSong != null && !currentSong.isBlank()) {
            template = template.replace("这首歌", "《" + currentSong + "》");
        }
        return template;
    }

    private static String selectTemplate(String timeOfDay, String weather, String gpsTag) {
        // 优先匹配特殊天气
        if ("snowy".equals(weather)) {
            return randomFrom(SNOWY);
        }
        if ("foggy".equals(weather)) {
            return randomFrom(FOGGY);
        }

        // 匹配时段 + 天气/位置组合
        if ("midnight".equals(timeOfDay) && "rainy".equals(weather)) {
            return randomFrom(MIDNIGHT_RAIN);
        }
        if ("morning".equals(timeOfDay) && "sunny".equals(weather)) {
            return randomFrom(MORNING_SUNNY);
        }
        if ("evening".equals(timeOfDay) && "coastal".equals(gpsTag)) {
            return randomFrom(EVENING_COASTAL);
        }
        if (("night".equals(timeOfDay) || "midnight".equals(timeOfDay)) && "highway".equals(gpsTag)) {
            return randomFrom(NIGHT_HIGHWAY);
        }
        if ("afternoon".equals(timeOfDay) && "urban".equals(gpsTag)) {
            return randomFrom(AFTERNOON_URBAN);
        }
        if ("evening".equals(timeOfDay) && "mountain".equals(gpsTag)) {
            return randomFrom(EVENING_MOUNTAIN);
        }

        // 默认模板
        return randomFrom(DEFAULT);
    }

    private static String randomFrom(List<String> templates) {
        return templates.get(RANDOM.nextInt(templates.size()));
    }
}
