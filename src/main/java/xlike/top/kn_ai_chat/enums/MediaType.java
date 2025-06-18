package xlike.top.kn_ai_chat.enums;

import lombok.Getter;

/**
 * 媒体文件类型枚举
 * <p>
 * 用于规范微信客服接口中 'type' 参数的取值。 
 * 使用枚举代替字符串，可以增强代码的类型安全性和可维护性。
 * @author xlike
 */
@Getter
public enum MediaType {
    /**
     * 图片类型
     */
    IMAGE("image"),

    /**
     * 语音类型
     */
    VOICE("voice"),

    /**
     * 视频类型
     */
    VIDEO("video"),

    /**
     * 普通文件类型
     */
    FILE("file");

    private final String typeName;

    MediaType(String typeName) {
        this.typeName = typeName;
    }
}