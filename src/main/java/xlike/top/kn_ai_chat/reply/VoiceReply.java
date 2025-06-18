package xlike.top.kn_ai_chat.reply;

/**
 * 语音回复
 * @param mediaId 通过 MediaService 上传后获得的语音 media_id
 */
public record VoiceReply(String mediaId) implements Reply {
}