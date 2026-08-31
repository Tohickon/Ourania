import java.awt.Font;
import java.awt.GraphicsEnvironment;

public class FontCheck {
    public static void main(String[] args) {
        String[] chars = {"\u2BF0", "\u26A2", "\u2695", "\u2BF1", "\u2BF2"};
        String[] names = {"Eris", "Eros", "Hygiea", "Nessus", "Pholus"};
        
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = ge.getAllFonts();
        
        for (int i = 0; i < chars.length; i++) {
            System.out.println("Checking " + names[i] + " (" + chars[i] + "):");
            boolean found = false;
            for (Font font : fonts) {
                if (font.canDisplay(chars[i].codePointAt(0))) {
                    System.out.println("  - " + font.getName());
                    found = true;
                }
            }
            if (!found) {
                System.out.println("  No fonts found.");
            }
        }
    }
}
