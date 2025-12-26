package com.vibe.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 音乐变异服务
 * 生成随机种子，打破 AI 的固定选歌模式
 */
@Service
public class MusicVariationService {

    private final Random random = new Random();

    // 曲风库
    private static final List<String> GENRES = List.of(
        "流行", "摇滚", "民谣", "爵士", "古典", "电子",
        "R&B", "嘻哈", "蓝调", "乡村", "放克",
        "新世纪", "氛围", "后摇", "独立"
    );

    // 情绪/类型库
    private static final List<String> MOODS = List.of(
        "抒情", "欢快", "舒缓", "动感", "治愈", "伤感",
        "浪漫", "励志", "怀旧", "清新", "梦幻",
        "温暖", "孤独", "自由", "宁静"
    );

    // 语言库
    private static final List<String> LANGUAGES = List.of(
        "中文", "英文", "日语", "韩语", "粤语", "纯音乐"
    );

    /**
     * 生成随机种子列表
     */
    public List<String> generateSeeds(int count) {
        List<String> seeds = new ArrayList<>();

        List<String> shuffledGenres = new ArrayList<>(GENRES);
        List<String> shuffledMoods = new ArrayList<>(MOODS);
        List<String> shuffledLanguages = new ArrayList<>(LANGUAGES);

        Collections.shuffle(shuffledGenres, random);
        Collections.shuffle(shuffledMoods, random);
        Collections.shuffle(shuffledLanguages, random);

        for (int i = 0; i < count; i++) {
            String seed = buildSeed(
                shuffledGenres.get(i % shuffledGenres.size()),
                shuffledMoods.get(i % shuffledMoods.size()),
                shuffledLanguages.get(i % shuffledLanguages.size())
            );
            seeds.add(seed);
        }

        return seeds;
    }

    /**
     * 构建单个种子
     */
    private String buildSeed(String genre, String mood, String language) {
        int pattern = random.nextInt(4);
        return switch (pattern) {
            case 0 -> genre + mood;
            case 1 -> language + genre;
            case 2 -> language + mood;
            case 3 -> mood + genre;
            default -> genre + mood;
        };
    }
}
