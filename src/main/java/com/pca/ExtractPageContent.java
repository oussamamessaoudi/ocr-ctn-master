package com.pca;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class ExtractPageContent {

    /**
     * The original PDF that will be parsed.
     */
    public static final String PREFACE = "test.pdf";
    /**
     * The resulting text file.
     */
    public static final String RESULT = "preface.txt";

    /**
     * Parses a PDF to a plain text file.
     *
     * @param pdf the original PDF
     * @param txt the resulting text
     * @throws IOException
     */
    public void parsePdf(String pdf, String txt) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        PrintWriter out = new PrintWriter(new FileOutputStream(txt));
        TextExtractionStrategy strategy;
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
            out.println(strategy.getResultantText());
        }
        reader.close();
        out.flush();
        out.close();
    }

    /**
     * Main method.
     *
     * @param args no arguments needed
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        new ExtractPageContent().parsePdf(PREFACE, RESULT);
    }
}
