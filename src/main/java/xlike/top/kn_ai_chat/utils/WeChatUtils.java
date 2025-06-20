package xlike.top.kn_ai_chat.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;

/**
 * @author xlike
 */
public class WeChatUtils {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String sha1(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA1 计算失败", e);
        }
    }

    public static byte[] decrypt(byte[] encrypted, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "BC");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(key, 0, 16);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(encrypted);

        // 提取消息长度
        int msgLen = ByteBuffer.wrap(Arrays.copyOfRange(decrypted, 16, 20)).getInt();

        // 验证并截取有效内容（16字节随机串 + 4字节长度 + msgLen）
        int totalLen = 16 + 4 + msgLen;
        if (decrypted.length < totalLen) {
            throw new Exception("解密数据长度不足");
        }
        // 跳过随机串和长度，获取消息体
        byte[] message = Arrays.copyOfRange(decrypted, 20, 20 + msgLen);
        // 移除 PKCS#7 填充（如果存在）
        int pad = message[message.length - 1];
        if (pad > 0 && pad <= 16) {
            message = Arrays.copyOf(message, message.length - pad);
        }

        return message;
    }

    public static byte[] encrypt(byte[] msg, byte[] key, String corpId) throws Exception {
        byte[] random = new byte[16];
        ByteBuffer.wrap(random).putInt(0x12345678);
        byte[] msgLen = ByteBuffer.allocate(4).putInt(msg.length).array();
        byte[] corpIdBytes = corpId.getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[random.length + msgLen.length + msg.length + corpIdBytes.length];
        System.arraycopy(random, 0, content, 0, random.length);
        System.arraycopy(msgLen, 0, content, random.length, msgLen.length);
        System.arraycopy(msg, 0, content, random.length + msgLen.length, msg.length);
        System.arraycopy(corpIdBytes, 0, content, random.length + msgLen.length + msg.length, corpIdBytes.length);

        int pad = 16 - (content.length % 16);
        byte[] padded = new byte[content.length + pad];
        System.arraycopy(content, 0, padded, 0, content.length);
        Arrays.fill(padded, content.length, padded.length, (byte) pad);

        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "BC");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(key, 0, 16);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(padded);
    }

    public static String base64Encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] base64Decode(String data) {
        return Base64.getDecoder().decode(data);
    }

}