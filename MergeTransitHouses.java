import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;

public class MergeTransitHouses {
    public static void main(String[] args) throws Exception {
        String inputPath = "C:/Users/daver/Desktop/Ourania/interpretations/transit_in_houses.json";
        String mainFilePath = "C:/Users/daver/Desktop/Ourania/OuraniaWindows/src/main/resources/data/interpretations.json";

        String inContent = new String(Files.readAllBytes(Paths.get(inputPath)), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        sb.append(",\n  \"transit_house\": {\n");

        Matcher m = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{\\s*\"title\"\\s*:\\s*\"([^\"]+)\",\\s*\"summary\"\\s*:\\s*\"([^\"]+)\",\\s*\"fullText\"\\s*:\\s*\"([^\"]+)\"\\s*\\}").matcher(inContent);
        
        boolean first = true;
        while (m.find()) {
            if (!first) sb.append(",\n");
            first = false;
            String key = m.group(1);
            String title = m.group(2);
            String summary = m.group(3);
            String fullText = m.group(4);
            String html = "<b>" + title + "</b><br><i>" + summary + "</i><br><br>" + fullText;
            sb.append("    \"" + key + "\": \"" + html + "\"");
        }
        sb.append("\n  }\n}");

        String mainContent = new String(Files.readAllBytes(Paths.get(mainFilePath)), StandardCharsets.UTF_8);
        mainContent = mainContent.trim();
        if (mainContent.endsWith("}")) {
            mainContent = mainContent.substring(0, mainContent.length() - 1);
            mainContent = mainContent.trim();
            if (mainContent.endsWith("}")) {
                mainContent = mainContent.substring(0, mainContent.length() - 1);
            }
            mainContent += sb.toString();
        }

        Files.write(Paths.get(mainFilePath), mainContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("Merged transit houses.");
    }
}
