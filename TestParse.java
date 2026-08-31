import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TestParse {
    public static void main(String[] args) throws Exception {
        Map<String, String> degreeSummaries = new HashMap<>();
        Map<String, String> degreeFullTexts = new HashMap<>();
        
        File file = new File("OuraniaWindows/src/main/resources/data/degree_interpretations.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        String line;
        String currentDegree = null;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("\"") && (line.endsWith(":{") || line.endsWith(": {"))) {
                int quoteEnd = line.indexOf("\"", 1);
                if (quoteEnd != -1) {
                    currentDegree = line.substring(1, quoteEnd);
                }
            } else if (currentDegree != null) {
                if (line.startsWith("\"summary\":")) {
                    int start = line.indexOf("\"", 10) + 1;
                    int end = line.lastIndexOf("\"");
                    if (start > 0 && end > start) {
                        degreeSummaries.put(currentDegree, line.substring(start, end).replace("\\\"", "\""));
                    }
                } else if (line.startsWith("\"fullText\":")) {
                    int start = line.indexOf("\"", 11) + 1;
                    int end = line.lastIndexOf("\"");
                    if (start > 0 && end > start) {
                        degreeFullTexts.put(currentDegree, line.substring(start, end).replace("\\\"", "\""));
                    }
                }
            }
        }
        reader.close();
        System.out.println("Loaded degree interpretations: " + degreeSummaries.size() + " summaries.");
        System.out.println("aries_1 summary: " + degreeSummaries.get("aries_1"));
    }
}
