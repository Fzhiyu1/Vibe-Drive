package com.vibe.model;

import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Min;

/**
 * Token 使用统计
 * 用于成本监控和分析
 */
@Description("Token使用统计，用于成本监控和分析")
public record TokenUsageInfo(
    @Min(value = 0, message = "Input token count must be non-negative")
    @Description("输入Token数量")
    Integer inputTokenCount,

    @Min(value = 0, message = "Output token count must be non-negative")
    @Description("输出Token数量")
    Integer outputTokenCount,

    @Min(value = 0, message = "Total token count must be non-negative")
    @Description("总Token数量")
    Integer totalTokenCount
) {
    /**
     * GPT-4o 定价（美元/百万Token）
     */
    private static final double GPT4O_INPUT_PRICE_PER_MILLION = 2.50;
    private static final double GPT4O_OUTPUT_PRICE_PER_MILLION = 10.00;

    /**
     * 从 LangChain4j TokenUsage 转换
     */
    public static TokenUsageInfo from(TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return null;
        }
        return new TokenUsageInfo(
            tokenUsage.inputTokenCount(),
            tokenUsage.outputTokenCount(),
            tokenUsage.totalTokenCount()
        );
    }

    /**
     * 创建空的 TokenUsageInfo
     */
    public static TokenUsageInfo empty() {
        return new TokenUsageInfo(0, 0, 0);
    }

    /**
     * 估算成本（基于 GPT-4o 定价）
     * @return 估算成本（美元）
     */
    public double estimateCost() {
        if (inputTokenCount == null || outputTokenCount == null) {
            return 0.0;
        }
        double inputCost = (inputTokenCount / 1_000_000.0) * GPT4O_INPUT_PRICE_PER_MILLION;
        double outputCost = (outputTokenCount / 1_000_000.0) * GPT4O_OUTPUT_PRICE_PER_MILLION;
        return inputCost + outputCost;
    }

    /**
     * 估算成本（格式化字符串）
     */
    public String estimateCostFormatted() {
        return String.format("$%.6f", estimateCost());
    }

    /**
     * 合并两个 TokenUsageInfo
     */
    public TokenUsageInfo merge(TokenUsageInfo other) {
        if (other == null) {
            return this;
        }
        return new TokenUsageInfo(
            (inputTokenCount != null ? inputTokenCount : 0) + (other.inputTokenCount != null ? other.inputTokenCount : 0),
            (outputTokenCount != null ? outputTokenCount : 0) + (other.outputTokenCount != null ? other.outputTokenCount : 0),
            (totalTokenCount != null ? totalTokenCount : 0) + (other.totalTokenCount != null ? other.totalTokenCount : 0)
        );
    }
}
