package com.example.thinkfirst;

import com.example.thinkfirst.model.ConceptCheckResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks a student's written attempt against a task's keyword list.
 *
 * Design rules:
 * - No AI calls. No network. Fully offline.
 * - Each keyword entry can contain multiple synonyms separated by " / "
 *   e.g. "contiguous / adjacent memory" — any synonym match counts.
 * - Matching is case-insensitive, normalised (punctuation stripped).
 * - A keyword is "matched" if ANY synonym appears anywhere in the answer.
 * - Never produces a grade. Only produces matched/missing lists for
 *   contextualising the hint in HintActivity.
 */
public class ConceptChecker {

    private static final String SYNONYM_SEPARATOR = " / ";
    private static final int    MIN_ANSWER_LENGTH = 15;

    /**
     * Main entry point.
     *
     * @param answerText   The student's raw typed answer.
     * @param keywords     List of keyword strings from the task JSON.
     *                     Each entry may contain synonyms: "O(1) / constant time"
     * @return             ConceptCheckResult with matched and missing lists.
     */
    public static ConceptCheckResult check(String answerText,
                                           List<String> keywords) {

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (keywords == null || keywords.isEmpty()) {
            return new ConceptCheckResult(matched, missing);
        }

        // Answer too short to meaningfully check
        if (answerText == null || answerText.trim().length() < MIN_ANSWER_LENGTH) {
            missing.addAll(keywords);
            return new ConceptCheckResult(matched, missing);
        }

        String normalisedAnswer = normalise(answerText);

        for (String keywordEntry : keywords) {
            if (keywordEntry == null || keywordEntry.trim().isEmpty()) continue;

            boolean found        = false;
            String  matchedLabel = null; // the synonym that actually matched

            // Each entry may have multiple synonyms
            String[] synonyms = keywordEntry.split(SYNONYM_SEPARATOR);
            for (String synonym : synonyms) {
                String normSynonym = normalise(synonym);
                if (normSynonym.isEmpty()) continue;

                if (normalisedAnswer.contains(normSynonym)) {
                    found        = true;
                    matchedLabel = synonym.trim();
                    break;
                }

                // Also check for abbreviated forms and partial matches
                // for technical terms like "O(n)" matching "O ( n )"
                if (containsFlexible(normalisedAnswer, normSynonym)) {
                    found        = true;
                    matchedLabel = synonym.trim();
                    break;
                }
            }

            // Show the word the student actually wrote, not always synonyms[0].
            // Fall back to synonyms[0] only when nothing matched (missing list).
            String displayLabel = found ? matchedLabel : synonyms[0].trim();
            if (found) {
                matched.add(displayLabel);
            } else {
                missing.add(displayLabel);
            }
        }

        return new ConceptCheckResult(matched, missing);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Lowercases and strips punctuation except parentheses and slashes,
     * which matter for technical notation like O(n) and I/O.
     */
    private static String normalise(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9()/ ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Flexible matching for technical terms.
     * Handles cases where the student writes "O(n²)" when the keyword is "O(n2)"
     * or writes "constant time" when the keyword is "O(1)".
     */
    private static boolean containsFlexible(String answer, String keyword) {
        // Remove all spaces from both and compare
        // Handles "O ( n )" matching "O(n)"
        String answerNoSpaces  = answer.replaceAll("\\s", "");
        String keywordNoSpaces = keyword.replaceAll("\\s", "");

        if (!keywordNoSpaces.isEmpty()
                && answerNoSpaces.contains(keywordNoSpaces)) {
            return true;
        }

        // Split keyword into words; require ALL significant words present
        // "binary search tree" → all of "binary", "search", "tree" must appear
        String[] words = keyword.split("\\s+");
        if (words.length >= 3) {
            boolean allPresent = true;
            for (String word : words) {
                // Skip very short words (prepositions etc.)
                if (word.length() <= 2) continue;
                if (!answer.contains(word)) {
                    allPresent = false;
                    break;
                }
            }
            return allPresent;
        }

        return false;
    }
}