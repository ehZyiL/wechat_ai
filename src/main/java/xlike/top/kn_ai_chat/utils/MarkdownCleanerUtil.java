package xlike.top.kn_ai_chat.utils;


/**
 * @author xlike
 */
public class MarkdownCleanerUtil {

    /**
     * 清理Markdown格式文本，转换为纯文本，但保留有序列表序号
     *
     * @param markdown Markdown格式的输入文本
     * @return 清理后的纯文本
     */
    public static String cleanMarkdown(String markdown) {
        if (markdown == null) {
            return "";
        }

        String cleaned = markdown
                // 移除标题 (#, ##, ### 等)
                .replaceAll("(?m)^#+\\s+", "")
                // 移除加粗 (**text** 或 __text__)
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("__(.*?)__", "$1")
                // 移除斜体 (*text* 或 _text_)
                .replaceAll("\\*(.*?)\\*", "$1")
                .replaceAll("_(.*?)_", "$1")
                // 移除无序列表符号 (-, *, +)
                .replaceAll("(?m)^\\s*[-*+]\\s+", "")
                // 保留有序列表序号 (1., 2., 等)，不做处理
                // 移除链接 [text](url)
                .replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1")
                // 移除图片 ![alt](url)
                .replaceAll("!\\[(.*?)\\]\\(.*?\\)", "$1")
                // 移除代码块 (``` 或 ~~~)
                .replaceAll("(?m)^```[\\s\\S]*?```", "")
                .replaceAll("(?m)^~~~[\\s\\S]*?~~~", "")
                // 移除行内代码 (`code`)
                .replaceAll("`([^`]+)`", "$1")
                // 移除引用 (> text)
                .replaceAll("(?m)^>\\s+", "")
                // 移除空行和多余换行
                .replaceAll("(?m)^\\s*\\n\\s*", "\n")
                // 移除行首行尾多余空格
                .trim();

        // 进一步清理连续空行
        return cleaned.replaceAll("\\n{2,}", "\n");
    }

    public static void main(String[] args) {
        // 测试Markdown清理工具
        String markdownText = """
                    从成都春熙路到倪家桥地铁站的步行路线规划如下：
                                               
                - **总距离**：约4.77公里
                - **预计时间**：约63分钟
                
                **步行路径详情**：
                1. 沿**蜀都大道总府路**向西步行326米，然后左转。
                2. 沿**暑袜中街**向西南步行285米，直行。
                3. 沿**暑袜南街**向西南步行117米，向左前方行走。
                4. 向西南步行122米，左转。
                5. 沿**青石桥北街**向西南步行121米，直行。
                6. 沿**青石桥中街**向西南步行122米，直行。
                7. 沿**青石桥南街**向西南步行239米，直行。
                8. 沿**新开街**向南步行269米，右转。
                9. 沿**盐道街**向西步行270米，左转。
                10. 沿**人民南路二段辅路**向南步行90米，直行。
                11. 沿**人民南路二段**向南步行112米，直行。
                12. 沿**人民南路三段**向南步行114米，向左前方行走。
                13. 沿**人民南路三段辅路**向南步行286米，直行。
                14. 沿**人民南路三段**向南步行137米，直行。
                15. 沿**人民南路三段辅路**向南步行558米，直行。
                16. 沿**人民南路三段**步行444米，直行。
                17. 沿**人民南路四段**向南步行918米，右转。
                18. 沿**倪家桥路**步行11米，左转。
                19. 向南步行232米，到达目的地**倪家桥地铁站**。
                
                希望这条路线对您有帮助！如果您需要其他交通方式（如驾车或公交）的规划，请告知。
                """;

        String cleanedText = MarkdownCleanerUtil.cleanMarkdown(markdownText);
        System.out.println("清理后的文本：");
        System.out.println(cleanedText);
    }
}