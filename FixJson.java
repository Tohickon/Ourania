import java.io.*;
import java.nio.file.*;
import java.util.regex.*;

public class FixJson {
    public static void main(String[] args) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("C:\\Users\\daver\\Desktop\\Ourania\\interpretations\\degree_interpretations.json")), "UTF-8");
        content = content.replace("```json\r\n", "").replace("```json\n", "").replace("\n```", "").replace("\r\n```", "");
        
        Pattern pattern = Pattern.compile("\"([a-z]+_[0-9]+)\":\\s*\\{\\s*\"title\":\\s*\"(.*?)\",\\s*\"summary\":\\s*\"(.*?)\",\\s*([^\\}]+)\\s*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        boolean first = true;
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String title = matcher.group(2);
            String summary = matcher.group(3);
            String fullTextRaw = matcher.group(4);
            
            summary = summary.replace("\n", " ").replace("\r", "").replace("\"", "\\\"");
            String fullText = fullTextRaw.replace("\n", " ").replace("\r", "").trim().replace("\"", "\\\"");
            
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            
            sb.append("  \"").append(key).append("\": {\n");
            sb.append("    \"title\": \"").append(title).append("\",\n");
            sb.append("    \"summary\": \"").append(summary).append("\",\n");
            sb.append("    \"fullText\": \"").append(fullText).append("\"\n");
            sb.append("  }");
        }
        sb.append("\n}\n");
        
        Files.write(Paths.get("C:\\Users\\daver\\Desktop\\Ourania\\OuraniaWindows\\src\\main\\resources\\data\\degree_interpretations.json"), sb.toString().getBytes("UTF-8"));
        System.out.println("Fixed JSON written!");
    }
}
