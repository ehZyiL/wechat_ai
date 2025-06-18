package xlike.top.kn_ai_chat.reply;

/**
 * 图片回复
 * @param mediaId 通过 MediaService 上传后获得的图片 media_id
 */
public record ImageReply(String mediaId) implements Reply {
}