package com.example.thinkfirst;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.thinkfirst.adapter.RevealAdapter;
import com.example.thinkfirst.model.DetectiveCase;
import com.example.thinkfirst.model.DetectiveError;
import com.example.thinkfirst.model.RevealItem;
import com.example.thinkfirst.model.VerdictItem;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DetectiveActivity extends AppCompatActivity {

    // ── Phase containers ──────────────────────────────────────────────────────
    private View cl_phase1;
    private View cl_phase2;
    private View cl_phase3;

    // ── Phase 1 views ─────────────────────────────────────────────────────────
    private LinearLayout llSentences;
    private TextView     tvFlagCount;
    private Button       btnSubmitFindings;
    private TextView     tvErrorCountHint;

    // ── Phase 2 views ─────────────────────────────────────────────────────────
    private LinearLayout llVerdictCards;
    private Button       btnSubmitFinal;

    // ── Phase 3 views — looked up from container, not member vars ─────────────
    // (avoids stale binding bug)

    // ── Header views ──────────────────────────────────────────────────────────
    private TextView tvRankBadge;
    private TextView tvTopicLabel;
    private TextView tvModeLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private String            currentSubject;
    private DetectiveCase     currentCase;
    private final List<String>      sentences        = new ArrayList<>();
    private final List<String>      flaggedSentences = new ArrayList<>();
    private final List<VerdictItem> verdictItems     = new ArrayList<>();
    private final List<View>        sentenceViews    = new ArrayList<>();

    // ── Managers ──────────────────────────────────────────────────────────────
    private SharedPreferencesManager prefsManager;
    private DatabaseHelper           dbHelper;
    private ContentLoader            contentLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detective);

        prefsManager  = SharedPreferencesManager.getInstance(this);
        dbHelper      = DatabaseHelper.getInstance(this);
        contentLoader = new ContentLoader(this);

        currentSubject = getIntent().getStringExtra(TaskActivity.EXTRA_SUBJECT);
        if (currentSubject == null) currentSubject = "dsa";

        // ── Bind views ────────────────────────────────────────────────────────
        tvRankBadge       = findViewById(R.id.tv_rank_badge);
        tvTopicLabel      = findViewById(R.id.tv_topic_label);
        tvModeLabel       = findViewById(R.id.tv_mode_label);

        cl_phase1         = findViewById(R.id.cl_phase1);
        cl_phase2         = findViewById(R.id.cl_phase2);
        cl_phase3         = findViewById(R.id.cl_phase3);

        llSentences       = findViewById(R.id.ll_sentences);
        tvFlagCount       = findViewById(R.id.tv_flag_count);
        btnSubmitFindings = findViewById(R.id.btn_submit_findings);
        tvErrorCountHint  = findViewById(R.id.tv_error_count_hint);

        llVerdictCards    = findViewById(R.id.ll_verdict_cards);
        btnSubmitFinal    = findViewById(R.id.btn_submit_final);

        // ── Header ────────────────────────────────────────────────────────────
        String rank = prefsManager.getCurrentRank();
        tvRankBadge.setText(rank);
        tvRankBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                rankIconRes(rank), 0, 0, 0);
        tvRankBadge.setCompoundDrawablePadding(6);

        tvModeLabel.setText("Hunt Mode  ·  Find the planted errors");

        // ── Load case ─────────────────────────────────────────────────────────
        loadDetectiveCase();

        // ── Button wiring ─────────────────────────────────────────────────────
        btnSubmitFindings.setOnClickListener(v -> transitionToPhase2());
        btnSubmitFinal.setOnClickListener(v    -> transitionToPhase3());

        // ── Back handling ─────────────────────────────────────────────────────
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (cl_phase2.getVisibility() == View.VISIBLE) {
                            cl_phase2.setVisibility(View.GONE);
                            cl_phase1.setVisibility(View.VISIBLE);
                            tvModeLabel.setText("Hunt Mode  ·  Find the planted errors");
                        } else {
                            finish();
                        }
                    }
                });
    }

    // ── Case loading ──────────────────────────────────────────────────────────

    private void loadDetectiveCase() {
        List<DetectiveCase> cases = contentLoader.loadDetectiveCases(currentSubject);

        if (cases.isEmpty()) {
            tvTopicLabel.setText("No cases available for this subject.");
            btnSubmitFindings.setEnabled(false);
            tvErrorCountHint.setText("—");
            return;
        }

        int caseIndex = prefsManager.getDetectiveCaseIndex(currentSubject) % cases.size();
        currentCase   = cases.get(caseIndex);

        tvTopicLabel.setText("Case: " + currentCase.getTopic());

        // Show error count upfront
        int errorCount = currentCase.getErrors() != null
                ? currentCase.getErrors().size() : 0;
        tvErrorCountHint.setText(errorCount + (errorCount == 1 ? " error" : " errors"));

        buildSentenceCards(currentCase.getAiResponse());
    }

    // ── Phase 1: Sentence cards ───────────────────────────────────────────────

    private void buildSentenceCards(String aiResponse) {
        llSentences.removeAllViews();
        sentences.clear();
        sentenceViews.clear();
        flaggedSentences.clear();

        // Split on ". " but not on common abbreviations
        String[] parts = aiResponse.split("(?<=\\.) ");
        for (String part : parts) {
            String s = part.trim();
            if (!s.isEmpty()) sentences.add(s);
        }

        float density = getResources().getDisplayMetrics().density;
        int   padPx   = (int)(12 * density);
        int   marginB = (int)(10 * density);

        for (String sentence : sentences) {
            TextView tv = new TextView(this);
            tv.setText(sentence);
            tv.setTextColor(getColor(R.color.colorTextPrimary));
            tv.setTextSize(14f);
            tv.setLineSpacing(0f, 1.5f);
            tv.setPadding(padPx, padPx, padPx, padPx);
            tv.setBackground(ContextCompat.getDrawable(
                    this, R.drawable.bg_sentence_normal));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = marginB;
            tv.setLayoutParams(params);

            tv.setOnClickListener(v     -> toggleHighlight(tv, sentence));
            tv.setOnLongClickListener(v -> { toggleHighlight(tv, sentence); return true; });

            llSentences.addView(tv);
            sentenceViews.add(tv);
        }
    }

    private void toggleHighlight(TextView tv, String sentence) {
        if (flaggedSentences.contains(sentence)) {
            flaggedSentences.remove(sentence);
            tv.setBackground(ContextCompat.getDrawable(
                    this, R.drawable.bg_sentence_normal));
            tv.setTextColor(getColor(R.color.colorTextPrimary));
        } else {
            flaggedSentences.add(sentence);
            tv.setBackground(ContextCompat.getDrawable(
                    this, R.drawable.bg_sentence_highlighted));
            tv.setTextColor(getColor(R.color.colorWarning));
        }
        updateFlagCounter();
    }

    private void updateFlagCounter() {
        int n = flaggedSentences.size();
        tvFlagCount.setText(n + (n == 1 ? " sentence flagged" : " sentences flagged"));
        btnSubmitFindings.setEnabled(n > 0);
        btnSubmitFindings.setAlpha(n > 0 ? 1.0f : 0.4f);
    }

    // ── Phase 1 → 2: Jury Mode ────────────────────────────────────────────────

    private void transitionToPhase2() {
        if (flaggedSentences.isEmpty()) {
            Toast.makeText(this,
                    "Tap at least one sentence to flag it first.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        verdictItems.clear();
        llVerdictCards.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int   marginB = (int)(12 * density);

        for (String sentence : flaggedSentences) {
            VerdictItem item = new VerdictItem(sentence);
            verdictItems.add(item);

            View card = getLayoutInflater().inflate(
                    R.layout.item_verdict_card, llVerdictCards, false);

            TextView   tvSentence = card.findViewById(R.id.tv_flagged_sentence);
            android.widget.RadioGroup rgVerdict =
                    card.findViewById(R.id.rg_verdict);

            tvSentence.setText(sentence);

            rgVerdict.setOnCheckedChangeListener((group, checkedId) -> {
                if      (checkedId == R.id.rb_wrong_fact) item.setVerdict("wrong_fact");
                else if (checkedId == R.id.rb_misleading) item.setVerdict("misleading");
                else if (checkedId == R.id.rb_fabricated) item.setVerdict("fabricated");
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = marginB;
            card.setLayoutParams(params);

            llVerdictCards.addView(card);
        }

        cl_phase1.setVisibility(View.GONE);
        cl_phase2.setVisibility(View.VISIBLE);

        // Update mode label
        tvModeLabel.setText("Jury Mode  ·  Deliver your verdict");
    }

    // ── Phase 2 → 3: Verdict ─────────────────────────────────────────────────

    private void transitionToPhase3() {
        for (VerdictItem item : verdictItems) {
            if (!item.isVerdictSelected()) {
                Toast.makeText(this,
                        "Please select a verdict for every flagged sentence.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        List<RevealItem> revealItems = calculateScore();

        int correctCount   = 0;
        int falseFlagCount = 0;
        int totalScore     = 0;

        for (RevealItem ri : revealItems) {
            totalScore += ri.getPoints();
            if ("correct".equals(ri.getType()))    correctCount++;
            if ("false_flag".equals(ri.getType())) falseFlagCount++;
        }

        int displayScore = Math.max(0, totalScore);
        int totalErrors  = currentCase != null && currentCase.getErrors() != null
                ? currentCase.getErrors().size() : 0;

        // ── Persist ───────────────────────────────────────────────────────────
        prefsManager.addDetectivePoints(displayScore);
        prefsManager.advanceDetectiveCaseIndex(
                currentSubject,
                contentLoader.loadDetectiveCases(currentSubject).size());

        String highlightsJson = new Gson().toJson(flaggedSentences);
        dbHelper.insertDetectiveSession(
                currentSubject,
                currentCase != null ? currentCase.getTopic() : "",
                displayScore,
                correctCount,
                falseFlagCount,
                prefsManager.getCurrentRank(),
                highlightsJson);

        // ── Switch to Phase 3 FIRST ───────────────────────────────────────────
        cl_phase2.setVisibility(View.GONE);
        cl_phase3.setVisibility(View.VISIBLE);

        tvModeLabel.setText("Verdict  ·  Case closed");

        // ── Update header rank badge ───────────────────────────────────────────
        String updatedRank = prefsManager.getCurrentRank();
        tvRankBadge.setText(updatedRank);
        tvRankBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                rankIconRes(updatedRank), 0, 0, 0);
        tvRankBadge.setCompoundDrawablePadding(6);

        // ── Populate Phase 3 via direct container lookup ──────────────────────
        // Using cl_phase3.findViewById to guarantee we hit the right views
        // inside the NestedScrollView, never a stale Activity-level binding.
        TextView   tvScore   = cl_phase3.findViewById(R.id.tv_final_score);
        TextView   tvSummary = cl_phase3.findViewById(R.id.tv_score_summary);
        LinearLayout llCards = cl_phase3.findViewById(R.id.ll_reveal_cards);
        Button     btnShare  = cl_phase3.findViewById(R.id.btn_share_result);

        if (tvScore != null) {
            tvScore.setText("Score: " + displayScore);
        }

        if (tvSummary != null) {
            String summary = correctCount + "/" + totalErrors + " errors caught";
            if (falseFlagCount > 0) {
                summary += "   ·   " + falseFlagCount + " false flag(s)";
            }
            tvSummary.setText(summary);
        }

        // ── Build reveal cards into LinearLayout ──────────────────────────────
        // LinearLayout inside NestedScrollView = guaranteed full scroll access.
        // Never truncated, never clipped by a pinned button.
        if (llCards != null) {
            llCards.removeAllViews();
            for (int i = 0; i < revealItems.size(); i++) {
                View card = buildRevealCard(revealItems.get(i), i);
                llCards.addView(card);
            }
        }

        if (btnShare != null) {
            final int finalScore = displayScore;
            btnShare.setOnClickListener(v -> shareResult(finalScore));
        }
    }

    // ── Build a single reveal card programmatically ───────────────────────────

    private View buildRevealCard(RevealItem item, int position) {
        View card = getLayoutInflater().inflate(
                R.layout.item_reveal_card, null, false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = (int)(10 * getResources().getDisplayMetrics().density);
        card.setLayoutParams(params);

        TextView tvResultLabel = card.findViewById(R.id.tv_result_label);
        TextView tvPoints      = card.findViewById(R.id.tv_points);
        TextView tvRevealText  = card.findViewById(R.id.tv_reveal_text);
        TextView tvExplanation = card.findViewById(R.id.tv_reveal_explanation);

        int    bgColor;
        int    accentColor;
        String labelText;

        switch (item.getType()) {
            case "correct":
                // Dark green background, bright green labels
                bgColor     = getColor(R.color.colorHintTier3Bg);
                accentColor = getColor(R.color.colorCorrect);
                labelText   = "CORRECT CATCH";
                break;

            case "missed":
                // Deep red background — subdued but unmistakably wrong
                // Text uses a lighter red so it contrasts against the dark bg
                bgColor     = getColor(R.color.colorMissedBg);
                accentColor = 0xFFFF6B6B;   // warm light red — readable on dark
                labelText   = "YOU MISSED THIS";
                break;

            default: // false_flag
                // Dark amber background, amber labels
                bgColor     = getColor(R.color.colorHintTier2Bg);
                accentColor = getColor(R.color.colorWarning);
                labelText   = "FALSE FLAG";
                break;
        }

        card.setBackgroundColor(bgColor);

        if (tvResultLabel != null) {
            tvResultLabel.setText(labelText);
            tvResultLabel.setTextColor(accentColor);
        }
        if (tvPoints != null) {
            int pts = item.getPoints();
            tvPoints.setText(pts >= 0 ? "+" + pts : String.valueOf(pts));
            tvPoints.setTextColor(accentColor);
        }
        if (tvRevealText != null) {
            // Missed cards: make the main text the same warm red so it reads
            // as erroneous rather than neutral
            tvRevealText.setText(item.getDisplayText());
            tvRevealText.setTextColor(
                    "missed".equals(item.getType())
                            ? accentColor
                            : getColor(R.color.colorTextPrimary));
        }
        if (tvExplanation != null) {
            String fullExplanation;
            if (item.getCorrectVersion() == null
                    || item.getCorrectVersion().isEmpty()) {
                fullExplanation = item.getExplanation();
            } else {
                fullExplanation = "Correct: " + item.getCorrectVersion()
                        + "\n\n" + item.getExplanation();
            }
            tvExplanation.setText(fullExplanation);
            // Explanation stays readable secondary color on all card types
            tvExplanation.setTextColor(getColor(R.color.colorTextSecondary));
        }

        // Staggered fade-in
        card.setAlpha(0f);
        card.animate()
                .alpha(1f)
                .setStartDelay(position * 150L)
                .setDuration(380)
                .start();

        return card;
    }

    // ── Scoring engine ────────────────────────────────────────────────────────

    private List<RevealItem> calculateScore() {
        List<RevealItem>     reveal  = new ArrayList<>();
        List<DetectiveError> errors  = currentCase.getErrors();
        Set<String>          caught  = new HashSet<>();

        // Check each flagged sentence against planted errors
        for (VerdictItem verdict : verdictItems) {
            String  flaggedLower = verdict.getSentenceText().toLowerCase();
            boolean matchFound   = false;

            for (DetectiveError error : errors) {
                String errorLower = error.getErrorText().toLowerCase();

                if (flaggedLower.contains(errorLower)
                        || errorLower.contains(flaggedLower)
                        || containsSignificantOverlap(flaggedLower, errorLower)) {
                    int points = pointsForDifficulty(error.getDifficulty());
                    caught.add(error.getErrorText());
                    reveal.add(new RevealItem(
                            "correct",
                            verdict.getSentenceText(),
                            error.getCorrect(),
                            error.getExplanation(),
                            points));
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
                reveal.add(new RevealItem(
                        "false_flag",
                        verdict.getSentenceText(),
                        "",
                        "This statement was actually correct.",
                        -10));
            }
        }

        // Add cards for errors the student missed entirely
        for (DetectiveError error : errors) {
            if (!caught.contains(error.getErrorText())) {
                reveal.add(new RevealItem(
                        "missed",
                        "Missed: \"" + error.getErrorText() + "\"",
                        error.getCorrect(),
                        error.getExplanation(),
                        -5));
            }
        }

        return reveal;
    }

    /**
     * Checks whether two strings share significant word overlap.
     * Handles cases where the student flags a longer sentence
     * containing the error phrase.
     */
    private boolean containsSignificantOverlap(String flagged, String error) {
        String[] errorWords = error.split("\\s+");
        if (errorWords.length < 3) return false;

        int matchCount = 0;
        for (String word : errorWords) {
            if (word.length() > 3 && flagged.contains(word)) {
                matchCount++;
            }
        }
        // 70% of significant words must match
        return matchCount >= Math.ceil(errorWords.length * 0.7);
    }

    private int pointsForDifficulty(String difficulty) {
        if (difficulty == null) return 10;
        switch (difficulty.toLowerCase()) {
            case "hard":   return 30;
            case "medium": return 20;
            default:       return 10;
        }
    }

    // ── Share ─────────────────────────────────────────────────────────────────

    private void shareResult(int score) {
        String rank = prefsManager.getCurrentRank();
        String text = "I scored " + score
                + " pts on ThinkFirst Detective Mode! Rank: " + rank
                + " #ThinkFirst #AILiteracy";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Share your result"));
    }

    // ── Rank icon helper ──────────────────────────────────────────────────────

    private static int rankIconRes(String rank) {
        switch (rank) {
            case "Investigator":     return R.drawable.ic_rank_investigator;
            case "Analyst":          return R.drawable.ic_rank_analyst;
            case "Senior Detective": return R.drawable.ic_rank_senior;
            case "Chief Inspector":  return R.drawable.ic_rank_chief;
            default:                 return R.drawable.ic_rank_rookie;
        }
    }
}