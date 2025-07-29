package com.ski.shop.catalog.domain;

/**
 * スキータイプ列挙型
 */
public enum SkiType {
    // 従来のスキータイプ
    ALL_MOUNTAIN("オールマウンテン"),
    CARVING("カービング"),
    FREESTYLE("フリースタイル"),
    FREERIDE("フリーライド"),
    MAINTENANCE("メンテナンス"),
    MOGUL("モーグル"),
    POWDER("パウダー"),
    PROTECTION("プロテクション"),
    RACING("レーシング"),
    TOURING("ツーリング"),
    
    // 設備タイプ（Equipment Type用）
    SKI_BOARD("スキー板"),
    BINDING("ビンディング"),
    POLE("ストック"),
    BOOT("ブーツ"),
    HELMET("ヘルメット"),
    PROTECTOR("プロテクター"),
    WEAR("ウェア"),
    GOGGLE("ゴーグル"),
    GLOVE("グローブ"),
    BAG("バッグ"),
    WAX("ワックス"),
    TUNING("チューンナップ");

    private final String displayName;

    SkiType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
