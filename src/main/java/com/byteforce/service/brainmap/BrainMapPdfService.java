package com.byteforce.service.brainmap;

import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.brainmap.BrainMapEdge;
import com.byteforce.domain.brainmap.BrainMapGraph;
import com.byteforce.domain.brainmap.BrainMapNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Service for generating clean, vector-rendered Brain Map PDF documents.
 */
public class BrainMapPdfService {

    private static final Logger log = LoggerFactory.getLogger(BrainMapPdfService.class);

    public byte[] generateBrainMapPdf(BrainMapGraph graph) {
        Objects.requireNonNull(graph, "graph must not be null");

        // Use Landscape A4: 842 points wide by 595 points tall
        float pageWidth = PDRectangle.A4.getHeight();
        float pageHeight = PDRectangle.A4.getWidth();
        PDRectangle landscape = new PDRectangle(pageWidth, pageHeight);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(landscape);
            doc.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // 1. Header background bar (ByteForce Dark Navy)
                cs.setNonStrokingColor(15 / 255f, 23 / 255f, 42 / 255f); // #0f172a
                cs.addRect(0, pageHeight - 65, pageWidth, 65);
                cs.fill();

                // 2. Header Title: "BYTEFORCE BRAIN MAP"
                cs.setNonStrokingColor(1.0f, 1.0f, 1.0f);
                cs.beginText();
                cs.setFont(fontBold, 16);
                cs.newLineAtOffset(36, pageHeight - 38);
                cs.showText("BYTEFORCE  |  BRAIN MAP");
                cs.endText();

                // 3. Date & Document Info on top right
                String dateStr = "Generated " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
                cs.setNonStrokingColor(148 / 255f, 163 / 255f, 184 / 255f); // #94a3b8
                cs.beginText();
                cs.setFont(fontRegular, 9);
                cs.newLineAtOffset(pageWidth - 170, pageHeight - 38);
                cs.showText(dateStr);
                cs.endText();

                // 4. Concept Title & Context Banner
                cs.setNonStrokingColor(248 / 255f, 250 / 255f, 252 / 255f); // #f8fafc
                cs.addRect(0, pageHeight - 110, pageWidth, 45);
                cs.fill();

                // Subtle border under banner
                cs.setStrokingColor(226 / 255f, 232 / 255f, 240 / 255f); // #e2e8f0
                cs.setLineWidth(1.0f);
                cs.moveTo(0, pageHeight - 110);
                cs.lineTo(pageWidth, pageHeight - 110);
                cs.stroke();

                // Breadcrumb
                String subjectName = graph.getSubject() != null ? graph.getSubject().getName() : "Curriculum";
                String topicName = graph.getTopic() != null ? graph.getTopic().getName() : (graph.getCenterConcept().getTopicName() != null ? graph.getCenterConcept().getTopicName() : "");
                String breadcrumb = (subjectName + " > " + topicName).toUpperCase();

                cs.setNonStrokingColor(100 / 255f, 116 / 255f, 139 / 255f); // #64748b
                cs.beginText();
                cs.setFont(fontBold, 8);
                cs.newLineAtOffset(36, pageHeight - 85);
                cs.showText(sanitizeText(breadcrumb));
                cs.endText();

                // Concept title
                cs.setNonStrokingColor(15 / 255f, 23 / 255f, 42 / 255f); // #0f172a
                cs.beginText();
                cs.setFont(fontBold, 14);
                cs.newLineAtOffset(36, pageHeight - 102);
                cs.showText(sanitizeText(graph.getCenterConcept().getTitle()));
                cs.endText();

                // Scale factor from graph viewBox (e.g. 960x600) to available PDF canvas area
                // Canvas area: x in [36, pageWidth - 36], y in [70, pageHeight - 120]
                float canvasX = 36f;
                float canvasY = 70f;
                float canvasW = pageWidth - 72f;
                float canvasH = pageHeight - 190f;

                float minX = (float) graph.getMinX();
                float minY = (float) graph.getMinY();

                float graphW = (float) graph.getViewBoxWidth();
                float graphH = (float) graph.getViewBoxHeight();
                float scale = Math.min(canvasW / graphW, canvasH / graphH);

                float offsetX = canvasX + ((canvasW - (graphW * scale)) / 2.0f);
                float offsetY = canvasY + ((canvasH - (graphH * scale)) / 2.0f);

                // 5. Draw Edges
                for (BrainMapEdge edge : graph.getEdges()) {
                    float x1 = offsetX + (float) ((edge.getX1() - minX) * scale);
                    float y1 = offsetY + (float) ((graphH - (edge.getY1() - minY)) * scale);
                    float x2 = offsetX + (float) ((edge.getX2() - minX) * scale);
                    float y2 = offsetY + (float) ((graphH - (edge.getY2() - minY)) * scale);

                    setEdgeStrokeColor(cs, edge.getType());
                    cs.setLineWidth(edge.getType() == ConceptRelationshipType.PREREQUISITE ? 1.8f : 1.2f);
                    cs.moveTo(x1, y1);
                    cs.lineTo(x2, y2);
                    cs.stroke();

                    // Edge label
                    float midX = (x1 + x2) / 2f;
                    float midY = (y1 + y2) / 2f;
                    cs.setNonStrokingColor(100 / 255f, 116 / 255f, 139 / 255f);
                    cs.beginText();
                    cs.setFont(fontOblique, 7);
                    cs.newLineAtOffset(midX - 15, midY + 3);
                    cs.showText(sanitizeText(edge.getType().getDisplayName()));
                    cs.endText();
                }

                // 6. Draw Nodes
                for (BrainMapNode node : graph.getNodes()) {
                    float nw = (float) (node.getWidth() * scale);
                    float nh = (float) (node.getHeight() * scale);
                    float nx = offsetX + (float) ((node.getX() - minX) * scale);
                    // In PDFBox (0,0) is bottom-left, so invert Y coordinate
                    float ny = offsetY + (float) ((graphH - ((node.getY() - minY) + node.getHeight())) * scale);

                    // Box Fill
                    if (node.isCenter()) {
                        cs.setNonStrokingColor(238 / 255f, 242 / 255f, 255 / 255f); // Indigo-50
                        cs.setStrokingColor(79 / 255f, 70 / 255f, 229 / 255f);      // Indigo-600
                        cs.setLineWidth(2.0f);
                    } else if ("PREREQUISITE".equalsIgnoreCase(node.getRelationshipRole())) {
                        cs.setNonStrokingColor(241 / 255f, 245 / 255f, 249 / 255f); // Slate-100
                        cs.setStrokingColor(71 / 255f, 85 / 255f, 105 / 255f);       // Slate-600
                        cs.setLineWidth(1.2f);
                    } else if ("COMMONLY_CONFUSED".equalsIgnoreCase(node.getRelationshipRole())) {
                        cs.setNonStrokingColor(254 / 255f, 243 / 255f, 199 / 255f); // Amber-100
                        cs.setStrokingColor(217 / 255f, 119 / 255f, 6 / 255f);       // Amber-600
                        cs.setLineWidth(1.2f);
                    } else {
                        cs.setNonStrokingColor(255 / 255f, 255 / 255f, 255 / 255f); // White
                        cs.setStrokingColor(203 / 255f, 213 / 255f, 225 / 255f);    // Slate-300
                        cs.setLineWidth(1.0f);
                    }

                    cs.addRect(nx, ny, nw, nh);
                    cs.fillAndStroke();

                    // Role Badge Text
                    String roleTag = node.isCenter() ? "FOCAL CONCEPT" : node.getRelationshipRole().replace('_', ' ');
                    cs.setNonStrokingColor(100 / 255f, 116 / 255f, 139 / 255f);
                    cs.beginText();
                    cs.setFont(fontBold, 6);
                    cs.newLineAtOffset(nx + 8, ny + nh - 12);
                    cs.showText(sanitizeText(roleTag));
                    cs.endText();

                    // Node Title
                    cs.setNonStrokingColor(15 / 255f, 23 / 255f, 42 / 255f);
                    cs.beginText();
                    cs.setFont(fontBold, node.isCenter() ? 10 : 8.5f);
                    cs.newLineAtOffset(nx + 8, ny + (nh / 2.0f) - 3);
                    String displayTitle = truncate(node.getTitle(), 24);
                    cs.showText(sanitizeText(displayTitle));
                    cs.endText();
                }

                // 7. Footer: Legend
                cs.setNonStrokingColor(248 / 255f, 250 / 255f, 252 / 255f);
                cs.addRect(0, 0, pageWidth, 45);
                cs.fill();

                cs.setStrokingColor(226 / 255f, 232 / 255f, 240 / 255f);
                cs.setLineWidth(1.0f);
                cs.moveTo(0, 45);
                cs.lineTo(pageWidth, 45);
                cs.stroke();

                cs.setNonStrokingColor(71 / 255f, 85 / 255f, 105 / 255f);
                cs.beginText();
                cs.setFont(fontBold, 8);
                cs.newLineAtOffset(36, 20);
                cs.showText("RELATIONSHIPS:");
                cs.endText();

                drawLegendItem(cs, fontRegular, 115, 20, "Prerequisite", 79 / 255f, 70 / 255f, 229 / 255f);
                drawLegendItem(cs, fontRegular, 200, 20, "Parent Concept", 59 / 255f, 130 / 255f, 246 / 255f);
                drawLegendItem(cs, fontRegular, 300, 20, "Child Sub-concept", 14 / 255f, 165 / 255f, 233 / 255f);
                drawLegendItem(cs, fontRegular, 415, 20, "Common Confusion", 217 / 255f, 119 / 255f, 6 / 255f);
                drawLegendItem(cs, fontRegular, 535, 20, "Related Concept", 100 / 255f, 116 / 255f, 139 / 255f);

                cs.setNonStrokingColor(148 / 255f, 163 / 255f, 184 / 255f);
                cs.beginText();
                cs.setFont(fontRegular, 8);
                cs.newLineAtOffset(pageWidth - 165, 20);
                cs.showText("byteforce.internal/brain-maps");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            log.info("Generated Brain Map PDF for concept ID {} ({} bytes)", graph.getCenterConcept().getId(), baos.size());
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate Brain Map PDF for concept ID {}", graph.getCenterConcept().getId(), e);
            throw new RuntimeException("Failed to generate Brain Map PDF", e);
        }
    }

    private void drawLegendItem(PDPageContentStream cs, PDType1Font font, float x, float y, String label, float r, float g, float b) throws IOException {
        cs.setStrokingColor(r, g, b);
        cs.setLineWidth(2.0f);
        cs.moveTo(x, y + 2);
        cs.lineTo(x + 12, y + 2);
        cs.stroke();

        cs.setNonStrokingColor(51 / 255f, 65 / 255f, 85 / 255f);
        cs.beginText();
        cs.setFont(font, 8);
        cs.newLineAtOffset(x + 16, y);
        cs.showText(label);
        cs.endText();
    }

    private void setEdgeStrokeColor(PDPageContentStream cs, ConceptRelationshipType type) throws IOException {
        switch (type) {
            case PREREQUISITE -> cs.setStrokingColor(79 / 255f, 70 / 255f, 229 / 255f);
            case PARENT -> cs.setStrokingColor(59 / 255f, 130 / 255f, 246 / 255f);
            case CHILD -> cs.setStrokingColor(14 / 255f, 165 / 255f, 233 / 255f);
            case COMMONLY_CONFUSED -> cs.setStrokingColor(217 / 255f, 119 / 255f, 6 / 255f);
            case RELATED -> cs.setStrokingColor(148 / 255f, 163 / 255f, 184 / 255f);
        }
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        // Replace non-ASCII or unsupported characters
        return text.replaceAll("[^\\x20-\\x7E]", " ").trim();
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 1) + "…";
    }
}
