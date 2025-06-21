package xlike.top.kn_ai_chat.handler;

import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.UserConfigService;

import java.util.List;
import java.util.Optional;

/**
 * @author xlike
 */
@Component
public class MenuHandler implements MessageHandler {

    private final UserConfigService userConfigService;

    public MenuHandler(UserConfigService userConfigService) {
        this.userConfigService = userConfigService;
    }


    @Override
    public boolean canHandle(String content, String externalUserId) {
        List<String> keywords = userConfigService.getKeywordsForHandler(externalUserId, this.getClass().getSimpleName());
        return keywords.stream().anyMatch(content::equalsIgnoreCase);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String menuText = """
                你好！我是你的 AI 助手，可以为你提供以下服务：
                
                --- 🎨 创意与娱乐 ---
                • 绘画: 发送 "画一张 [内容]"，为你创作图画。
                • 彩票查询: 发送 "大乐透" 或 "双色球" 查询最新开奖。
                
                --- 🧠 知识与记忆 ---
                • 知识库问答: 发送任意文件（如 .txt, .pdf, .docx），我会学习并用它来回答你的问题。
                • 文件管理:
                  - "列出文件": 查看你已上传的文件列表。
                  - "删除文件 [ID]": 删除指定的文件。
                  - "删除所有文件": 清空你的个人知识库。
                
                --- ⚙️ 系统与工具 ---
                • 语音回复: 在你的问题后加上 "语音回答"，我将用声音答复你。
                • 历史记录: 发送 "我问过的问题" 或 "清空历史对话"。
                • 统计信息: 发送 "对话统计" 或 "我的id"。
                
                直接向我提问，或使用以上指令与我互动吧！
                """;
        return Optional.of(new TextReply(menuText));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}