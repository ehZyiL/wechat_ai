package xlike.top.kn_ai_chat.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * XML解析工具类
 * @author Administrator
 */
public class XmlParseUtil {

    /**
     * 将XML字符串转换为Map
     *
     * @param xmlString XML字符串
     * @return 转换后的Map
     * @throws Exception 解析异常
     */
    public static Map<String, String> xmlToMap(String xmlString) throws Exception {
        Map<String, String> data = new HashMap<>();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        // 禁用外部实体，防止XXE攻击
        documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        documentBuilderFactory.setExpandEntityReferences(false);

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        try (InputStream stream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))) {
            Document doc = documentBuilder.parse(stream);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getDocumentElement().getChildNodes();
            for (int idx = 0; idx < nodeList.getLength(); ++idx) {
                Node node = nodeList.item(idx);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    data.put(element.getNodeName(), element.getTextContent());
                }
            }
        }
        return data;
    }

    /**
     * 从加密的XML中提取Encrypt字段内容
     * @param encryptedXml 加密的XML原文
     * @return Encrypt字段的值
     * @throws Exception 解析异常
     */
    public static String extractEncryptPart(String encryptedXml) throws Exception {
        Map<String, String> map = xmlToMap(encryptedXml);
        return map.get("Encrypt");
    }
}