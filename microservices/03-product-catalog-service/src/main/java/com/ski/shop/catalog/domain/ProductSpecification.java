package com.ski.shop.catalog.domain;

import java.util.Map;

/**
 * 商品仕様 Value Object (Record)
 */
public record ProductSpecification(
    Material material,
    SkiType skiType,
    DifficultyLevel difficultyLevel,
    String length,
    String width,
    String weight,
    String radius,
    Flex flex,
    Map<String, String> additionalSpecs
) {
    
    /**
     * スキーヤープロファイルとの互換性チェック
     */
    public boolean isCompatibleWith(SkierProfile profile) {
        switch (difficultyLevel) {
            case BEGINNER: return profile.level() == SkierLevel.BEGINNER || profile.level() == SkierLevel.INTERMEDIATE;
            case INTERMEDIATE: return profile.level() != SkierLevel.EXPERT;
            case ADVANCED: return profile.level() == SkierLevel.EXPERT || profile.level() == SkierLevel.ADVANCED;
            case EXPERT: return profile.level() == SkierLevel.EXPERT;
            default: return false;
        }
    }
    
    /**
     * 長さを数値として取得（例：165cm -> 165）
     */
    public Integer getLengthAsNumber() {
        if (length == null || length.isBlank()) {
            return null;
        }
        String numericPart = length.replaceAll("[^0-9]", "");
        return numericPart.isEmpty() ? null : Integer.parseInt(numericPart);
    }
    
    /**
     * 幅を数値として取得（例：75mm -> 75）
     */
    public Integer getWidthAsNumber() {
        if (width == null || width.isBlank()) {
            return null;
        }
        String numericPart = width.replaceAll("[^0-9]", "");
        return numericPart.isEmpty() ? null : Integer.parseInt(numericPart);
    }
}
