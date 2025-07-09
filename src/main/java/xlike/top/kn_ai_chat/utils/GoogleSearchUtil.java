package xlike.top.kn_ai_chat.utils;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.customsearch.v1.CustomSearchAPI;
import com.google.api.services.customsearch.v1.model.Result;
import com.google.api.services.customsearch.v1.model.Search;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

/**
 * @author Administrator
 */
@Component
public class GoogleSearchUtil {

    private final CustomSearchAPI customSearchAPI;
    private final String cxId;
    private final String apiKey;

//    public GoogleSearchUtil(@Value("${tools.google.api-key}") String apiKey,
//                            @Value("${tools.google.cx-id}") String cxId) {
//        this.cxId = cxId;
//        this.apiKey = apiKey;
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));
//        HttpTransport transport = new NetHttpTransport.Builder().setProxy(proxy).build();
//        this.customSearchAPI = new CustomSearchAPI.Builder(transport, new GsonFactory(), null)
//                .setApplicationName("kn-ai-chat")
//                .build();
//    }

    public GoogleSearchUtil(@Value("${tools.google.api-key}") String apiKey,
                            @Value("${tools.google.cx-id}") String cxId) {
        this.cxId = cxId;
        this.apiKey = apiKey;
        // 创建默认的 HttpTransport，不使用代理
        HttpTransport transport = new NetHttpTransport();
        this.customSearchAPI = new CustomSearchAPI.Builder(transport, new GsonFactory(), null)
                .setApplicationName("kn-ai-chat")
                .build();
    }


    public List<Result> performSearch(String query) throws IOException {
        CustomSearchAPI.Cse.List request = customSearchAPI.cse().list();
        request.setQ(query);
        request.setCx(this.cxId);
        request.setKey(this.apiKey);
        Search results = request.execute();
        return results.getItems();
    }
}