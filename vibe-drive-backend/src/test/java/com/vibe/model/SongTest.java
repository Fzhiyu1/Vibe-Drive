package com.vibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Song 模型测试
 */
@SpringBootTest
class SongTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("JSON 序列化测试")
    class JsonSerializationTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            Song song = new Song("1", "Test Song", "Test Artist", "Test Album", 180, 120, "pop", List.of("happy"), null);
            String json = objectMapper.writeValueAsString(song);

            assertThat(json).contains("\"id\":\"1\"");
            assertThat(json).contains("\"title\":\"Test Song\"");
            assertThat(json).contains("\"artist\":\"Test Artist\"");
            assertThat(json).contains("\"duration\":180");
            assertThat(json).contains("\"bpm\":120");
            assertThat(json).contains("\"mood\":[\"happy\"]");
        }

        @Test
        void shouldDeserializeFromJson() throws Exception {
            String json = """
                {
                    "id": "1",
                    "title": "Test Song",
                    "artist": "Test Artist",
                    "album": "Test Album",
                    "duration": 180,
                    "bpm": 120,
                    "genre": "pop",
                    "mood": ["happy", "excited"]
                }
                """;
            Song song = objectMapper.readValue(json, Song.class);

            assertThat(song.id()).isEqualTo("1");
            assertThat(song.title()).isEqualTo("Test Song");
            assertThat(song.artist()).isEqualTo("Test Artist");
            assertThat(song.duration()).isEqualTo(180);
            assertThat(song.mood()).containsExactly("happy", "excited");
        }
    }

    @Nested
    @DisplayName("紧凑构造器校验测试")
    class ValidationTest {
        @Test
        void shouldRejectEmptyId() {
            assertThatThrownBy(() -> new Song("", "Title", "Artist", null, 180, 120, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Song id cannot be empty");
        }

        @Test
        void shouldRejectNullId() {
            assertThatThrownBy(() -> new Song(null, "Title", "Artist", null, 180, 120, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Song id cannot be empty");
        }

        @Test
        void shouldRejectEmptyTitle() {
            assertThatThrownBy(() -> new Song("1", "", "Artist", null, 180, 120, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Song title cannot be empty");
        }

        @Test
        void shouldRejectEmptyArtist() {
            assertThatThrownBy(() -> new Song("1", "Title", "", null, 180, 120, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Artist cannot be empty");
        }

        @Test
        void shouldRejectNegativeDuration() {
            assertThatThrownBy(() -> new Song("1", "Title", "Artist", null, -1, 120, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration must be non-negative");
        }

        @Test
        void shouldRejectNegativeBpm() {
            assertThatThrownBy(() -> new Song("1", "Title", "Artist", null, 180, -1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BPM must be non-negative");
        }
    }

    @Nested
    @DisplayName("业务方法测试")
    class BusinessMethodTest {
        @Test
        void shouldFormatDuration() {
            Song song = new Song("1", "Title", "Artist", null, 185, 120, null, null, null);
            assertThat(song.getFormattedDuration()).isEqualTo("3:05");
        }

        @Test
        void shouldIdentifySlowTempo() {
            Song slow = new Song("1", "Title", "Artist", null, 180, 70, null, null, null);
            Song fast = new Song("2", "Title", "Artist", null, 180, 130, null, null, null);

            assertThat(slow.isSlowTempo()).isTrue();
            assertThat(fast.isSlowTempo()).isFalse();
        }

        @Test
        void shouldIdentifyFastTempo() {
            Song slow = new Song("1", "Title", "Artist", null, 180, 70, null, null, null);
            Song fast = new Song("2", "Title", "Artist", null, 180, 130, null, null, null);

            assertThat(slow.isFastTempo()).isFalse();
            assertThat(fast.isFastTempo()).isTrue();
        }

        @Test
        void shouldMatchMood() {
            Song song = new Song("1", "Title", "Artist", null, 180, 120, "pop", List.of("happy", "excited"), null);

            assertThat(song.matchesMood("happy")).isTrue();
            assertThat(song.matchesMood("EXCITED")).isTrue();  // case insensitive
            assertThat(song.matchesMood("calm")).isFalse();
        }

        @Test
        void shouldMatchAnyMoodWhenNoMoodTags() {
            Song song = new Song("1", "Title", "Artist", null, 180, 120, "pop", null, null);

            assertThat(song.matchesMood("happy")).isTrue();
            assertThat(song.matchesMood("calm")).isTrue();
        }
    }
}
