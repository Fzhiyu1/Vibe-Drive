package com.vibe.service;

import com.vibe.model.MusicRecommendation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MusicService 单元测试
 */
@SpringBootTest
class MusicServiceTest {

    @Autowired
    private MusicService musicService;

    @Nested
    @DisplayName("曲库加载测试")
    class LibraryLoadingTest {
        @Test
        void shouldLoadSongLibrary() {
            assertThat(musicService.getLibrarySize()).isGreaterThan(0);
        }

        @Test
        void shouldLoadExpectedNumberOfSongs() {
            assertThat(musicService.getLibrarySize()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("音乐推荐测试")
    class RecommendationTest {
        @Test
        void shouldRecommendMusicForCalmMood() {
            MusicRecommendation result = musicService.recommend("calm", "evening", 1, null);

            assertThat(result).isNotNull();
            assertThat(result.songs()).isNotEmpty();
            assertThat(result.mood()).isEqualTo("calm");
            assertThat(result.bpmRange().min()).isLessThanOrEqualTo(90);
        }

        @Test
        void shouldRecommendMusicForHappyMood() {
            MusicRecommendation result = musicService.recommend("happy", "morning", 1, null);

            assertThat(result).isNotNull();
            assertThat(result.songs()).isNotEmpty();
            assertThat(result.mood()).isEqualTo("happy");
            assertThat(result.bpmRange().min()).isGreaterThanOrEqualTo(100);
        }

        @Test
        void shouldRecommendMusicForExcitedMood() {
            MusicRecommendation result = musicService.recommend("excited", "noon", 1, null);

            assertThat(result).isNotNull();
            assertThat(result.songs()).isNotEmpty();
            assertThat(result.bpmRange().min()).isGreaterThanOrEqualTo(120);
        }

        @Test
        void shouldRecommendPopMusicForMultiplePassengers() {
            MusicRecommendation result = musicService.recommend("calm", "afternoon", 4, null);

            assertThat(result).isNotNull();
            assertThat(result.genre()).isEqualTo("pop");
        }

        @Test
        void shouldRespectUserGenrePreference() {
            MusicRecommendation result = musicService.recommend("calm", "evening", 1, "jazz");

            assertThat(result).isNotNull();
            assertThat(result.genre()).isEqualTo("jazz");
        }

        @Test
        void shouldReturnAtLeastOneSong() {
            MusicRecommendation result = musicService.recommend("tired", "midnight", 1, null);

            assertThat(result).isNotNull();
            assertThat(result.songs()).isNotEmpty();
        }

        @Test
        void shouldLimitSongsToFive() {
            MusicRecommendation result = musicService.recommend("calm", "evening", 1, null);

            assertThat(result.songs()).hasSizeLessThanOrEqualTo(5);
        }
    }

    @Nested
    @DisplayName("时段推荐测试")
    class TimeBasedRecommendationTest {
        @Test
        void shouldRecommendJazzForMidnight() {
            MusicRecommendation result = musicService.recommend("calm", "midnight", 1, null);

            assertThat(result).isNotNull();
            // 深夜推荐 jazz/classical/ambient
            assertThat(result.genre()).isIn("jazz", "classical", "ambient");
        }

        @Test
        void shouldRecommendPopForMorning() {
            MusicRecommendation result = musicService.recommend("happy", "morning", 1, null);

            assertThat(result).isNotNull();
            // 早晨推荐 pop/folk/rock
            assertThat(result.genre()).isIn("pop", "folk", "rock");
        }
    }
}
