package xlike.top.kn_ai_chat.reply;

/**
 * 定义一个通用的、密封的回复模型接口。
 * 只有在这个文件里 'permits' 关键字声明的类才能实现此接口，
 * 这使得我们可以在 WeChatService 中使用 switch 表达式进行详尽的类型匹配，
 * 保证了代码的安全性和可维护性。
 * @author xlike
 */
public sealed interface Reply permits TextReply, ImageReply, VoiceReply, VideoReply, FileReply {
}