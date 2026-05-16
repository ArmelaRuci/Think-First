package com.example.thinkfirst;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.example.thinkfirst.model.ConceptCheckResult;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.List;

public class HintActivity extends AppCompatActivity {

    private static final int TYPEWRITER_DELAY_MS = 18;

    private TextView     tvHintContent;
    private LinearLayout llConceptCoverage;
    private TextView     tvConceptOpening;
    private LinearLayout llMatchedConcepts;
    private LinearLayout llMissingConcepts;
    private ChipGroup    chipGroupMatched;
    private ChipGroup    chipGroupMissing;

    private int    hintTierUsed;
    private String reflectionAnswer = "";

    private Handler  typewriterHandler;
    private Runnable typewriterRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hint);

        // ── Receive data ──────────────────────────────────────────────────────
        String answerText = getIntent().getStringExtra(TaskActivity.EXTRA_ANSWER_TEXT);
        hintTierUsed      = getIntent().getIntExtra(TaskActivity.EXTRA_HINT_TIER_USED, 1);
        String hint1      = getIntent().getStringExtra(TaskActivity.EXTRA_HINT_1);
        String hint2      = getIntent().getStringExtra(TaskActivity.EXTRA_HINT_2);
        String hint3      = getIntent().getStringExtra(TaskActivity.EXTRA_HINT_3);
        String conceptRaw = getIntent().getStringExtra("extra_concept_check");

        if (answerText == null) answerText = "";
        if (hint1      == null) hint1      = "Think about the fundamentals of this concept.";
        if (hint2      == null) hint2      = "Break the problem into smaller parts.";
        if (hint3      == null) hint3      = "Review the core definition and think about examples.";

        ConceptCheckResult conceptResult =
                ConceptCheckResult.deserialise(conceptRaw);

        // ── Bind views ────────────────────────────────────────────────────────
        TextView   tvTitle      = findViewById(R.id.tv_hint_title);
        TextView   tvAttempt    = findViewById(R.id.tv_your_attempt_content);
        tvHintContent           = findViewById(R.id.tv_hint_content);
        RadioGroup rgReflection = findViewById(R.id.rg_reflection);
        Button     btnBack      = findViewById(R.id.btn_back_to_task);

        llConceptCoverage = findViewById(R.id.ll_concept_coverage);
        tvConceptOpening  = findViewById(R.id.tv_concept_opening);
        llMatchedConcepts = findViewById(R.id.ll_matched_concepts);
        llMissingConcepts = findViewById(R.id.ll_missing_concepts);
        chipGroupMatched  = findViewById(R.id.chip_group_matched);
        chipGroupMissing  = findViewById(R.id.chip_group_missing);

        typewriterHandler = new Handler(Looper.getMainLooper());

        // ── Your attempt ──────────────────────────────────────────────────────
        tvAttempt.setText(answerText.isEmpty()
                ? "(No written attempt)"
                : answerText);

        // ── Concept coverage ──────────────────────────────────────────────────
        boolean hasContent = answerText.length() >= 15;
        if (hasContent && conceptResult.totalConcepts() > 0) {
            llConceptCoverage.setVisibility(View.VISIBLE);
            tvConceptOpening.setText(buildOpeningLine(conceptResult));

            if (conceptResult.hasAnyMatch()) {
                llMatchedConcepts.setVisibility(View.VISIBLE);
                for (String c : conceptResult.getMatched()) {
                    chipGroupMatched.addView(buildChip(c, true));
                }
            }
            if (conceptResult.hasAnyMissing()) {
                llMissingConcepts.setVisibility(View.VISIBLE);
                for (String c : conceptResult.getMissing()) {
                    chipGroupMissing.addView(buildChip(c, false));
                }
            }
        }

        // ── Hint tier ─────────────────────────────────────────────────────────
        String hintText;
        int    hintBgColor;

        switch (hintTierUsed) {
            case 3:
                hintText    = hint3;
                hintBgColor = getColor(R.color.colorHintTier3Bg);
                tvTitle.setText("Full Guidance");
                break;
            case 2:
                hintText    = hint2;
                hintBgColor = getColor(R.color.colorHintTier2Bg);
                tvTitle.setText("Partial Hint");
                break;
            default:
                hintText    = hint1;
                hintBgColor = getColor(R.color.colorHintTier1Bg);
                tvTitle.setText("Nudge");
                break;
        }

        tvHintContent.setBackgroundColor(hintBgColor);
        tvHintContent.setPadding(40, 40, 40, 40);
        startTypewriterAnimation(hintText);

        // ── Reflection ────────────────────────────────────────────────────────
        rgReflection.setOnCheckedChangeListener((group, checkedId) -> {
            if      (checkedId == R.id.rb_yes)      reflectionAnswer = "yes";
            else if (checkedId == R.id.rb_probably) reflectionAnswer = "probably";
            else if (checkedId == R.id.rb_no)       reflectionAnswer = "no";
        });

        btnBack.setOnClickListener(v -> attemptReturn());

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() { attemptReturn(); }
                });
    }

    // ── Concept opening line ──────────────────────────────────────────────────

    private String buildOpeningLine(ConceptCheckResult result) {
        float coverage = result.coverageRatio();
        int   matched  = result.matchCount();
        int   total    = result.totalConcepts();

        if (matched == 0)
            return "Your attempt doesn't cover the key ideas yet — here's a pointer:";
        if (coverage == 1.0f)
            return "You've covered all the key concepts — here's the full picture to confirm:";
        if (coverage >= 0.5f)
            return "Good thinking — you've covered " + matched + " of " + total
                    + " key concepts. \nThe hint addresses what's still missing:";
        return "You're on the right track with " + matched
                + " concept" + (matched > 1 ? "s" : "")
                + " — but there's more to consider:";
    }

    // ── Chip builder ──────────────────────────────────────────────────────────

    private Chip buildChip(String label, boolean isMatched) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setClickable(false);
        chip.setCheckable(false);
        chip.setTextSize(11f);
        if (isMatched) {
            chip.setChipBackgroundColorResource(R.color.colorHintTier3Bg);
            chip.setTextColor(getColor(R.color.colorCorrect));
            chip.setChipStrokeColorResource(R.color.colorCorrect);
            chip.setChipStrokeWidth(1f);
        } else {
            chip.setChipBackgroundColorResource(R.color.colorHintTier2Bg);
            chip.setTextColor(getColor(R.color.colorWarning));
            chip.setChipStrokeColorResource(R.color.colorWarning);
            chip.setChipStrokeWidth(1f);
        }
        return chip;
    }

    // ── Typewriter ────────────────────────────────────────────────────────────

    private void startTypewriterAnimation(String fullText) {
        tvHintContent.setText("");
        final int[] idx = {0};
        typewriterRunnable = new Runnable() {
            @Override
            public void run() {
                if (idx[0] < fullText.length()) {
                    tvHintContent.setText(fullText.substring(0, idx[0] + 1));
                    idx[0]++;
                    typewriterHandler.postDelayed(this, TYPEWRITER_DELAY_MS);
                }
            }
        };
        typewriterHandler.postDelayed(typewriterRunnable, 300);
    }

    // ── Return ────────────────────────────────────────────────────────────────

    private void attemptReturn() {
        if (reflectionAnswer.isEmpty()) {
            Toast.makeText(this,
                    "Please answer the reflection question before going back.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra(TaskActivity.RESULT_HINT_TIER,  hintTierUsed);
        result.putExtra(TaskActivity.RESULT_REFLECTION, reflectionAnswer);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (typewriterHandler != null && typewriterRunnable != null) {
            typewriterHandler.removeCallbacks(typewriterRunnable);
        }
    }
}