package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 分析动作枚举
 * 表示 Vibe Agent 分析环境后的决策结果
 */
public enum AnalyzeAction {
    APPLY("APPLY", "应用"),
    NO_ACTION("NO_ACTION", "不操作");

    private final String value;
    private final String displayName;

    AnalyzeAction(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static AnalyzeAction fromValue(String value) {
        for (AnalyzeAction action : values()) {
            if (action.value.equalsIgnoreCase(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown AnalyzeAction: " + value);
    }

    /**
     * 判断是否需要更新 UI
     */
    public boolean requiresUiUpdate() {
        return this == APPLY;
    }
}
