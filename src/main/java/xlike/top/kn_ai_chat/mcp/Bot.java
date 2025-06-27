package xlike.top.kn_ai_chat.mcp;

public interface Bot {
    String chat(String prompt);
    
    String multiStepChat(String searchQuery, String toolName, String resultId);
}