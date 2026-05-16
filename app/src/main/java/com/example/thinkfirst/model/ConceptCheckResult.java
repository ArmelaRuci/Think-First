package com.example.thinkfirst.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of checking a student's answer against a task's keyword set.
 * Carries two lists: concepts the student demonstrated, and concepts
 * that were absent from their attempt.
 *
 * Passed from TaskActivity → HintActivity via Intent Extras.
 * Never affects the tank gauge or effort score.
 */
public class ConceptCheckResult {

    private final List<String> matched;   // concepts found in student answer
    private final List<String> missing;   // concepts absent from student answer

    public ConceptCheckResult(List<String> matched, List<String> missing) {
        this.matched = matched != null ? matched : new ArrayList<>();
        this.missing = missing != null ? missing : new ArrayList<>();
    }

    public List<String> getMatched() { return matched; }
    public List<String> getMissing() { return missing; }

    public boolean hasAnyMatch()   { return !matched.isEmpty(); }
    public boolean hasAnyMissing() { return !missing.isEmpty(); }

    public int matchCount()   { return matched.size(); }
    public int missingCount() { return missing.size(); }
    public int totalConcepts(){ return matched.size() + missing.size(); }

    /**
     * Coverage ratio 0.0 – 1.0.
     * Used to contextualise the hint opening line.
     */
    public float coverageRatio() {
        int total = totalConcepts();
        return total > 0 ? (float) matched.size() / total : 0f;
    }

    /**
     * Serialise to a pipe-delimited string for Intent transport.
     * Format: "matched_count|m1|m2|missing_count|s1|s2"
     */
    public String serialise() {
        StringBuilder sb = new StringBuilder();
        sb.append(matched.size());
        for (String s : matched) {
            sb.append("|").append(s.replace("|", ";;;"));
        }
        sb.append("|").append(missing.size());
        for (String s : missing) {
            sb.append("|").append(s.replace("|", ";;;"));
        }
        return sb.toString();
    }

    /**
     * Deserialise from the pipe-delimited string produced by serialise().
     * Returns an empty result if the string is null or malformed.
     */
    public static ConceptCheckResult deserialise(String raw) {
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (raw == null || raw.isEmpty()) {
            return new ConceptCheckResult(matched, missing);
        }

        try {
            String[] parts     = raw.split("\\|", -1);
            int      idx       = 0;
            int      mCount    = Integer.parseInt(parts[idx++]);
            for (int i = 0; i < mCount; i++) {
                matched.add(parts[idx++].replace(";;;", "|"));
            }
            int sCount = Integer.parseInt(parts[idx++]);
            for (int i = 0; i < sCount; i++) {
                missing.add(parts[idx++].replace(";;;", "|"));
            }
        } catch (Exception e) {
            // Malformed — return whatever was parsed so far
        }

        return new ConceptCheckResult(matched, missing);
    }
}