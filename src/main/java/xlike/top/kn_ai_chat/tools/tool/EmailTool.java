package xlike.top.kn_ai_chat.tools.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * @author Administrator
 */
@Component
@Slf4j
public class EmailTool {

    private final String smtpHost;
    private final String smtpPort;
    private final String username;
    private final String password;

    // 读取邮件用
    private final String imapHost;
    private final String imapPort;

    private final Session smtpSession;

    public EmailTool(
            @Value("${tools.email.host}") String smtpHost,
            @Value("${tools.email.port}") String smtpPort,
            @Value("${tools.email.username}") String username,
            @Value("${tools.email.password}") String password,
            @Value("${tools.email.imap.host}") String imapHost,
            @Value("${tools.email.imap.port}") String imapPort
    ) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
        this.imapHost = imapHost;
        this.imapPort = imapPort;

        // SMTP用于发信
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", this.smtpHost);
        props.put("mail.smtp.port", this.smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.debug", "true");
        this.smtpSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EmailTool.this.username, EmailTool.this.password);
            }
        });
    }

    // =================== 发送邮件相关方法 ==========================
    @Tool(name = "send_simple_text_email", value = "发送一封简单的纯文本邮件给单个收件人。")
    public String send(
            @P("收件人的电子邮件地址") String to,
            @P("邮件的主题") String subject,
            @P("邮件的正文内容（纯文本，里面不能包含markdown的格式语法）") String body) {
        return executeSend(List.of(to), null, subject, body, false);
    }

    @Tool(name = "send_text_email_to_multiple_recipients", value = "发送一封纯文本邮件给一个或多个收件人列表。")
    public String send(
            @P("收件人的电子邮件地址列表") List<String> to,
            @P("邮件的主题") String subject,
            @P("邮件的正文内容（纯文本，里面不能包含markdown的格式语法）") String body) {
        return executeSend(to, null, subject, body, false);
    }

    @Tool(name = "send_text_email_with_cc", value = "发送一封纯文本邮件，可以包含主要收件人和抄送人（CC）。")
    public String send(
            @P("主要收件人的电子邮件地址列表") List<String> to,
            @P("需要抄送的收件人电子邮件地址列表") List<String> cc,
            @P("邮件的主题") String subject,
            @P("邮件的正文内容（纯文本，里面不能包含markdown的格式语法）") String body) {
        return executeSend(to, cc, subject, body, false);
    }

    @Tool(name = "send_html_email", value = "发送一封格式丰富的HTML邮件给单个收件人。")
    public String send(
            @P("收件人的电子邮件地址") String to,
            @P("邮件的主题") String subject,
            @P("邮件的HTML内容。必须是合法的HTML代码。") String htmlContent,
            @P("这个值必须为 true, 用来确认发送的是HTML邮件") boolean isHtml) {
        if (!isHtml) {
            return "如果要发送HTML邮件，isHtml参数必须为true。如需发送纯文本，请使用另一个工具。";
        }
        return executeSend(List.of(to), null, subject, htmlContent, true);
    }

    private String executeSend(List<String> to, List<String> cc, String subject, String content, boolean isHtml) {
        try {
            MimeMessage message = new MimeMessage(this.smtpSession);
            message.setFrom(new InternetAddress(this.username));

            for (String recipient : to) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            if (cc != null && !cc.isEmpty()) {
                for (String recipient : cc) {
                    message.addRecipient(Message.RecipientType.CC, new InternetAddress(recipient));
                }
            }
            message.setSubject(subject, "UTF-8");
            if (isHtml) {
                message.setContent(content, "text/html; charset=utf-8");
            } else {
                message.setText(content);
            }
            Transport.send(message);

            String response = "邮件已成功发送给 " + String.join(", ", to);
            if (cc != null && !cc.isEmpty()) {
                response += " 并抄送给 " + String.join(", ", cc);
            }
            return response;

        } catch (MessagingException e) {
            return "邮件发送失败: " + e.getMessage();
        }
    }


    // 共用：获取IMAP连接
    private Store connectImapStore() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", imapHost);
        props.put("mail.imap.port", imapPort);
        props.put("mail.imap.ssl.enable", "true");
        Session session = Session.getInstance(props);
        Store store = session.getStore("imap");
        store.connect(imapHost, Integer.parseInt(imapPort), username, password);
        return store;
    }

    /**
     * 读取最近10封邮件（默认INBOX）
     */
    @Tool(name = "read_recent_emails", value = "读取邮箱中最近10封邮件，返回主题、发件人和时间。")
    public String readRecentEmails() {
        return readEmails(10);
    }

    /**
     * 读取指定数量的最新邮件
     */
    @Tool(name = "read_emails", value = "读取邮箱中最新的若干封邮件，参数为数量。")
    public String readEmails(@P("要读取的邮件数量") int count) {
        try (Store store = connectImapStore()) {
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            int total = inbox.getMessageCount();
            if (total == 0) {
                return "邮箱中没有邮件。";
            }
            int start = Math.max(1, total - count + 1);
            Message[] messages = inbox.getMessages(start, total);
            List<String> results = new ArrayList<>();
            for (int i = messages.length - 1; i >= 0; i--) {
                Message msg = messages[i];
                String subject = msg.getSubject();
                Address from = (msg.getFrom() != null && msg.getFrom().length > 0) ? msg.getFrom()[0] : null;
                Date sent = msg.getSentDate();

                String content = getTextFromMessage(msg);
                // 截取正文内容长度，避免太长，可根据需求调整
                if (content.length() > 500) {
                    content = content.substring(0, 500) + "...";
                }
                results.add(String.format("主题: %s\n发件人: %s\n时间: %s\n正文:\n%s\n----------------------",
                        subject,
                        from,
                        sent,
                        content));
            }
            return String.join("\n", results);

        } catch (Exception e) {
            return "读取邮件失败: " + e.getMessage();
        }
    }

    /**
     * 读取发件人为指定email的最近count封邮件，返回详细信息（主题/发件人/时间/正文）
     */
    @Tool(name = "read_emails_from_sender", value = "读取指定发件人的最新若干封邮件。")
    public String readEmailsFromSender(
            @P("发件人邮箱地址") String senderEmail,
            @P("要读取的邮件数量") int count) {
        try (Store store = connectImapStore()) {
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            List<String> results = new ArrayList<>();
            // 从后往前遍历邮件，最近的邮件在后面，直到满足数量
            for (int i = messages.length - 1; i >= 0 && results.size() < count; i--) {
                Message msg = messages[i];
                Address[] fromArr = msg.getFrom();
                if (fromArr != null && fromArr.length > 0) {
                    String fromStr = fromArr[0].toString();
                    if (fromStr.contains(senderEmail)) {
                        String subject = msg.getSubject();
                        Date sent = msg.getSentDate();

                        String content;
                        try {
                            content = getTextFromMessage(msg);
                            // 文本过长时截断，避免返回信息过大
                            if (content.length() > 500) {
                                content = content.substring(0, 500) + "...";
                            }
                        } catch (Exception e) {
                            content = "[无法读取邮件正文: " + e.getMessage() + "]";
                        }

                        String item = String.format(
                                "主题: %s\n发件人: %s\n时间: %s\n正文:\n%s\n----------------------",
                                subject, fromStr, sent, content);
                        results.add(item);
                    }
                }
            }
            if (results.isEmpty()) {
                return "没有找到来自该发件人的邮件。";
            }
            return String.join("\n", results);
        } catch (Exception e) {
            log.error("读取发件人邮件失败", e);
            return "读取发件人邮件失败: " + e.getMessage();
        }
    }



    public String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("text/html")) {
            // 如果是HTML，可以选择直接返回HTML或者用Jsoup等库清理HTML标签
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            return getTextFromMultipart(multipart);
        }
        return "";
    }

    private String getTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("text/html")) {
                // 如果需要只保留纯文本，可以用Jsoup库清理HTML标签，否则直接返回HTML内容
                result.append(bodyPart.getContent().toString());
            } else if (bodyPart.getContent() instanceof Multipart) {
                result.append(getTextFromMultipart((Multipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }
}