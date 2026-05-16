package com.example.thinkfirst;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class AIMirrorActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT     = "mirror_subject";
    public static final String EXTRA_TASK_ID     = "mirror_task_id";
    public static final String EXTRA_YOUR_ANSWER = "mirror_your_answer";
    public static final String EXTRA_AI_ANSWER   = "mirror_ai_answer";
    public static final String EXTRA_QUESTION         = "mirror_question";
    public static final String EXTRA_MISSING_CONCEPTS = "mirror_missing";
    public static final String EXTRA_MATCHED_CONCEPTS = "mirror_matched";

    private String subject;
    private String taskId;
    private String originalQuestion = "";
    private String yourAnswer       = "";

    private EditText etCovered;
    private EditText etMissed;

    private java.util.List<String> matchedConcepts = new java.util.ArrayList<>();
    private java.util.List<String> missingConcepts = new java.util.ArrayList<>();

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_mirror);

        dbHelper = DatabaseHelper.getInstance(this);

        // ── Receive data ──────────────────────────────────────────────────────
        subject         = getIntent().getStringExtra(EXTRA_SUBJECT);
        taskId          = getIntent().getStringExtra(EXTRA_TASK_ID);
        yourAnswer      = getIntent().getStringExtra(EXTRA_YOUR_ANSWER);
        String aiAnswer = getIntent().getStringExtra(EXTRA_AI_ANSWER);


        originalQuestion = getIntent().getStringExtra(EXTRA_QUESTION);
        if (originalQuestion == null) originalQuestion = "";

        java.util.ArrayList<String> matchedConcepts =
                getIntent().getStringArrayListExtra(EXTRA_MATCHED_CONCEPTS);
        java.util.ArrayList<String> missingConcepts =
                getIntent().getStringArrayListExtra(EXTRA_MISSING_CONCEPTS);
        if (matchedConcepts == null) matchedConcepts = new java.util.ArrayList<>();
        if (missingConcepts == null) missingConcepts = new java.util.ArrayList<>();

        // Store for use in buildHandoffPrompt
        this.matchedConcepts = matchedConcepts;
        this.missingConcepts = missingConcepts;


        if (subject    == null) subject    = "";
        if (taskId     == null) taskId     = "";
        if (yourAnswer == null) yourAnswer = "(No written answer)";
        if (aiAnswer   == null) aiAnswer   = "Reference answer not available.";

        // ── Bind views ────────────────────────────────────────────────────────
        TextView tvYourAnswer = findViewById(R.id.tv_mirror_your_answer);
        TextView tvAiAnswer   = findViewById(R.id.tv_mirror_ai_answer);
        etCovered             = findViewById(R.id.et_mirror_covered);
        etMissed              = findViewById(R.id.et_mirror_missed);
        Button   btnDone      = findViewById(R.id.btn_mirror_done);
        Button   btnSkip      = findViewById(R.id.btn_mirror_skip);
        Button   btnClaude    = findViewById(R.id.btn_mirror_claude);
        Button   btnChatGpt   = findViewById(R.id.btn_mirror_chatgpt);

        // ── Populate answers ──────────────────────────────────────────────────
        tvYourAnswer.setText(yourAnswer);
        startTypewriter(tvAiAnswer, aiAnswer);

        // ── Done ──────────────────────────────────────────────────────────────
        btnDone.setOnClickListener(v -> {
            String covered = etCovered.getText().toString().trim();
            String missed  = etMissed.getText().toString().trim();

            if (covered.isEmpty() && missed.isEmpty()) {
                Toast.makeText(this,
                        getString(R.string.mirror_empty_warning),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            saveAndFinish(covered, missed);
        });

        // ── Skip ──────────────────────────────────────────────────────────────
        btnSkip.setOnClickListener(v -> saveAndFinish("", ""));

        // ── AI Handoff buttons ────────────────────────────────────────────────
        // These always use the CURRENT state of the reflection fields —
        // so if the student filled them in before tapping, that context
        // is included in the prompt. If they tap before filling in, the
        // prompt still has the question and answer from before.
        btnClaude.setOnClickListener(v -> {
            String prompt = buildHandoffPrompt();
            openUrl("https://claude.ai/new?q=" + Uri.encode(prompt));
        });

        btnChatGpt.setOnClickListener(v -> {
            String prompt = buildHandoffPrompt();
            openUrl("https://chatgpt.com/?q=" + Uri.encode(prompt));
        });

        // ── Back press ────────────────────────────────────────────────────────
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        saveAndFinish(
                                etCovered.getText().toString().trim(),
                                etMissed.getText().toString().trim());
                    }
                });
    }

    // ── Build the handoff prompt ──────────────────────────────────────────────
    //
    // Richest possible context because this is built AFTER the student has:
    // 1. Submitted their own answer
    // 2. Seen the AI reference answer
    // 3. Identified what they covered that AI didn't (etCovered)
    // 4. Identified what AI covered that they missed (etMissed)
    //
    // The student arrives at Claude/ChatGPT as a thinker who has already
    // done the work — not as someone starting from scratch.

    private String buildHandoffPrompt() {
        StringBuilder sb = new StringBuilder();

        // Subject context
        sb.append("I'm studying ")
                .append(getSubjectDisplayName(subject))
                .append(".\n\n");

        // The actual question
        if (!originalQuestion.trim().isEmpty()) {
            sb.append("The question I'm working on:\n")
                    .append("\"").append(originalQuestion.trim()).append("\"")
                    .append("\n\n");
        }

        // Student's own attempt
        if (!yourAnswer.trim().isEmpty()
                && !yourAnswer.equals("(No written answer)")) {
            sb.append("My attempt:\n")
                    .append("\"").append(yourAnswer.trim()).append("\"")
                    .append("\n\n");
        }

        // Concepts still missing — from keyword checker
        if (!missingConcepts.isEmpty()) {
            sb.append("Concepts I still need help with:\n");
            for (String c : missingConcepts) {
                sb.append("- ").append(c).append("\n");
            }
            sb.append("\n");
        }

        // What the student wrote they covered that AI didn't
//        String coveredField = etCovered.getText().toString().trim();
//        if (!coveredField.isEmpty()) {
//            sb.append("Something I included that the reference answer didn't mention:\n")
//                    .append("\"").append(coveredField).append("\"")
//                    .append("\n\n");
//        }
//
//        // What the student wrote that AI covered but they missed
//        String missedField = etMissed.getText().toString().trim();
//        if (!missedField.isEmpty()) {
//            sb.append("Something the reference answer covered that I missed:\n")
//                    .append("\"").append(missedField).append("\"")
//                    .append("\n\n");
//        }

        sb.append("Please help me understand the parts I'm still missing. ")
                .append("Build on what I already understand rather than starting from scratch. ")
                .append("Use examples where it helps.");

        return sb.toString();
    }

    private String getSubjectDisplayName(String key) {
        if (key == null) return "";
        switch (key) {
            case "dsa":  return "Data Structures & Algorithms";
            case "dbms": return "Database Management Systems";
            case "web":  return "Web Programming";
            case "oop":  return "Object Oriented Programming";
            case "dm":   return "Intro to Data Mining";
            default:     return key.toUpperCase();
        }
    }

    // ── Save and navigate ─────────────────────────────────────────────────────

    private void saveAndFinish(String covered, String missed) {
        if (!subject.isEmpty() && !taskId.isEmpty()) {
            dbHelper.updateMirrorReflection(subject, taskId, covered, missed);
        }
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // ── Typewriter for AI answer ──────────────────────────────────────────────

    private void startTypewriter(TextView target, String fullText) {
        target.setText("");
        final int[] index = {0};
        final android.os.Handler handler =
                new android.os.Handler(android.os.Looper.getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < fullText.length()) {
                    target.setText(fullText.substring(0, index[0] + 1));
                    index[0]++;
                    handler.postDelayed(this, 12);
                }
            }
        };
        handler.postDelayed(runnable, 600);
    }

    // ── Open URL ──────────────────────────────────────────────────────────────

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this,
                    "No browser found. Please install a browser app.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}