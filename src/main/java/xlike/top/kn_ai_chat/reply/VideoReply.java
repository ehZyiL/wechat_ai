package xlike.top.kn_ai_chat.reply;

/**
 * 视频回复
 * @param mediaId 通过 MediaService 上传后获得的视频 media_id
 */
public record VideoReply(String mediaId) implements Reply {
}