package com.byteforce.web.dto;

import com.byteforce.domain.Concept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * View presentation model for ByteForce placement concepts.
 * Structures raw concept text into distinct visual learning sections:
 * One-line Summary, Why This Matters, The Idea, Visual Sequence/Diagram,
 * Real Example, and Code Example with language detection.
 */
public class ConceptPresentation {

    private final String summary;
    private final String whyThisMatters;
    private final String theIdea;
    private final String howItWorks;
    private final String realExample;
    private final String codeExample;
    private final String codeLanguage;
    private final List<String> visualSteps;

    public ConceptPresentation(String summary,
                               String whyThisMatters,
                               String theIdea,
                               String howItWorks,
                               String realExample,
                               String codeExample,
                               String codeLanguage,
                               List<String> visualSteps) {
        this.summary = summary != null ? summary.trim() : "";
        this.whyThisMatters = whyThisMatters != null && !whyThisMatters.isBlank() ? whyThisMatters.trim() : null;
        this.theIdea = theIdea != null && !theIdea.isBlank() ? theIdea.trim() : null;
        this.howItWorks = howItWorks != null && !howItWorks.isBlank() ? howItWorks.trim() : null;
        this.realExample = realExample != null && !realExample.isBlank() ? realExample.trim() : null;
        this.codeExample = codeExample != null && !codeExample.isBlank() ? codeExample.trim() : null;
        this.codeLanguage = codeLanguage != null ? codeLanguage : "text";
        this.visualSteps = visualSteps != null ? Collections.unmodifiableList(new ArrayList<>(visualSteps)) : List.of();
    }

    /**
     * Parses a {@link Concept} into a structured {@link ConceptPresentation}.
     */
    public static ConceptPresentation from(Concept concept) {
        if (concept == null) {
            return new ConceptPresentation("", null, null, null, null, null, "text", List.of());
        }

        String rawExplanation = concept.getShortExplanation() != null ? concept.getShortExplanation() : "";
        String summary = rawExplanation;
        String whyThisMatters = null;
        String theIdea = null;
        String howItWorks = null;
        String realExample = null;

        // Parse section headers if present (e.g. ### Why This Matters)
        if (rawExplanation.contains("###")) {
            String[] parts = rawExplanation.split("(?=###\\s+)");
            if (parts.length > 0) {
                // First part before any header is the one-line summary
                summary = parts[0].replace("###", "").trim();

                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.startsWith("### Why This Matters") || trimmed.startsWith("### Why this Matters")) {
                        whyThisMatters = extractSectionBody(trimmed);
                    } else if (trimmed.startsWith("### The Idea")) {
                        theIdea = extractSectionBody(trimmed);
                    } else if (trimmed.startsWith("### How It Works")) {
                        howItWorks = extractSectionBody(trimmed);
                    } else if (trimmed.startsWith("### Real Example")) {
                        realExample = extractSectionBody(trimmed);
                    }
                }
            }
        }

        String exampleRaw = concept.getExample() != null ? concept.getExample().trim() : "";
        String codeExample = exampleRaw;
        String codeLanguage = detectLanguage(exampleRaw);
        List<String> visualSteps = extractVisualSteps(howItWorks != null ? howItWorks : exampleRaw);

        return new ConceptPresentation(
                summary,
                whyThisMatters,
                theIdea,
                howItWorks,
                realExample,
                codeExample,
                codeLanguage,
                visualSteps
        );
    }

    private static String extractSectionBody(String sectionText) {
        int firstNewline = sectionText.indexOf('\n');
        if (firstNewline != -1 && firstNewline < sectionText.length() - 1) {
            return sectionText.substring(firstNewline + 1).trim();
        }
        return "";
    }

    private static String detectLanguage(String code) {
        if (code == null || code.isBlank()) {
            return "text";
        }
        String upper = code.toUpperCase();
        if (upper.contains("SELECT ") || upper.contains("FROM ") || upper.contains("JOIN ") || upper.contains("GROUP BY") || upper.startsWith("--")) {
            return "sql";
        }
        if (code.contains("class ") || code.contains("public ") || code.contains("void ") || code.contains("int ") || code.contains("System.out") || code.contains("List<") || code.contains("Map<")) {
            return "java";
        }
        if (code.contains("{") && code.contains("}") && (code.contains("\":") || code.contains("\" :"))) {
            return "json";
        }
        return "java";
    }

    private static List<String> extractVisualSteps(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        // Check for sequence patterns like [NEW] -> [READY] -> [RUNNING] -> [TERMINATED]
        Pattern stepPattern = Pattern.compile("\\[([^\\]]+)\\]\\s*(?:->|-->|=>|─+►|──►)\\s*");
        Matcher matcher = stepPattern.matcher(text);
        List<String> steps = new ArrayList<>();
        int lastEnd = 0;
        while (matcher.find()) {
            steps.add(matcher.group(1).trim());
            lastEnd = matcher.end();
        }
        // Capture final node
        if (!steps.isEmpty()) {
            Pattern lastNodePattern = Pattern.compile("^(?:->|-->|=>|─+►|──►)?\\s*\\[([^\\]]+)\\]");
            Matcher lastMatcher = lastNodePattern.matcher(text.substring(lastEnd));
            if (lastMatcher.find()) {
                steps.add(lastMatcher.group(1).trim());
            }
        }
        return steps;
    }

    public String getSummary() {
        return summary;
    }

    public String getWhyThisMatters() {
        return whyThisMatters;
    }

    public String getTheIdea() {
        return theIdea;
    }

    public String getHowItWorks() {
        return howItWorks;
    }

    public String getRealExample() {
        return realExample;
    }

    public String getCodeExample() {
        return codeExample;
    }

    public String getCodeLanguage() {
        return codeLanguage;
    }

    public List<String> getVisualSteps() {
        return visualSteps;
    }

    public boolean hasVisualSteps() {
        return visualSteps != null && visualSteps.size() >= 2;
    }

    public boolean hasCodeExample() {
        return codeExample != null && !codeExample.isBlank();
    }
}
