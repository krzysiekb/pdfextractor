package ch.banasiak.pdfextractor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.fit.pdfdom.PDFDomTree;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PdfExtractor {

    private final String pdfPath;
    private final String outputDir;

    private PdfExtractor(String[] args) {
        this.pdfPath = args[0];
        this.outputDir = args[1];
    }

    public static void main(String[] args)
            throws IOException, ParserConfigurationException {
        PdfExtractor pdfExtractor = new PdfExtractor(args);
        pdfExtractor.run();
    }

    private void run() throws IOException, ParserConfigurationException {
        try(PDDocument pdDoc = PDDocument.load(new File(pdfPath))) {
            createOutDirs();
            extractToHtml(pdDoc);
            extractImages(pdDoc);
        }
    }

    private void createOutDirs() {
        new File(outputDir).mkdirs();
        new File(Paths.get(outputDir, "Images").toString()).mkdirs();
    }

    private void extractToHtml(PDDocument pdDoc) throws IOException, ParserConfigurationException {
        try (Writer out = new PrintWriter(
                Paths.get(outputDir, "Test.html").toString(), StandardCharsets.UTF_8)) {
            new PDFDomTree().writeText(pdDoc, out);
        }
    }

    private void extractImages(PDDocument pdDoc) {
        List<RenderedImage> images = getImages(pdDoc);
        saveImagesToFiles(images);
    }

    private List<RenderedImage> getImages(PDDocument pdDoc) {
        List<RenderedImage> images = new ArrayList<>();
        pdDoc.getPages().forEach(p -> {
            images.addAll(
                    Objects.requireNonNull(getImagesFromResources(p.getResources())));
        });
        return images;
    }

    private List<RenderedImage> getImagesFromResources(PDResources resources) {
        List<RenderedImage> images = new ArrayList<>();
        resources.getXObjectNames().forEach(xo -> {
            try {
                PDXObject xObject = resources.getXObject(xo);
                if (xObject instanceof PDFormXObject) {
                    images.addAll(
                            Objects.requireNonNull(getImagesFromResources(((PDFormXObject) xObject).getResources())));
                } else if(xObject instanceof PDImageXObject) {
                    images.add(((PDImageXObject) xObject).getImage());
                }
            } catch (IOException e) {
                throw new RuntimeException("Error getting Object on page.", e);
            }
        });
        return images;
    }

    private void saveImagesToFiles(List<RenderedImage> images) {
        for(int i = 0; i<images.size(); i++) {
            try {
                String fileName = Paths.get(
                        outputDir, "Images", String.format("test%d.png", i)).toString();
                File outputFile = new File(fileName);
                ImageIO.write(images.get(i), "png", outputFile);
            } catch (IOException e) {
                // Consumed on purpose
                System.out.println("Error writing file" + Arrays.toString(e.getStackTrace()));
            }
        }
    }
}
