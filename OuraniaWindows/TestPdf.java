import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import javax.swing.JEditorPane;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.io.FileOutputStream;

public class TestPdf {
    public static void main(String[] args) throws Exception {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setText("<html><body><h1>Hello World</h1><p>This is a test of OpenPDF.</p></body></html>");
        pane.setSize(500, 800);

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("test.pdf"));
        document.open();

        Printable printable = pane.getPrintable(null, null);
        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        paper.setSize(595.0, 842.0); // A4
        paper.setImageableArea(36.0, 36.0, 523.0, 770.0);
        pf.setPaper(paper);

        PdfContentByte cb = writer.getDirectContent();
        Graphics2D g2 = cb.createGraphics(595.0f, 842.0f);
        
        int res = printable.print(g2, pf, 0);
        System.out.println("Printable result: " + res);
        
        g2.dispose();
        document.close();
        System.out.println("Done.");
    }
}
