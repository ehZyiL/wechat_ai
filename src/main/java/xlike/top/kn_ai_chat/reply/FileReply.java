package xlike.top.kn_ai_chat.reply;

/**
 * 文件回复
 * @param mediaId 通过 MediaService 上传后获得的文件 media_id
 */
public record FileReply(String mediaId) implements Reply {
}