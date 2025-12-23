package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户反馈类型
 */
public enum FeedbackType {
    LIKE("like"),
    DISLIKE("dislike"),
    SKIP("skip");

    private final String value;

    FeedbackType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FeedbackType fromValue(String value) {
        for (FeedbackType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown feedback type: " + value);
    }
}
