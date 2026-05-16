package com.example.thinkfirst;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.thinkfirst.model.ConceptCheckResult;
import com.example.thinkfirst.model.Task;
import java.util.ArrayList;
import java.util.List;

public class TaskActivity extends AppCompatActivity {

    // ── Intent keys ───────────────────────────────────────────────────────────
    public static final String EXTRA_SUBJECT        = "extra_subject";
    public static final String EXTRA_TASK_ID        = "extra_task_id";
    public static final String EXTRA_EFFORT_SCORE   = "extra_effort_score";
    public static final String EXTRA_ANSWER_TEXT    = "extra_answer_text";
    public static final String EXTRA_HINT_TIER_USED = "extra_hint_tier_used";
    public static final String EXTRA_TASK_QUESTION  = "extra_task_question";
    public static final String EXTRA_HINT_1         = "extra_hint_1";
    public static final String EXTRA_HINT_2         = "extra_hint_2";
    public static final String EXTRA_HINT_3         = "extra_hint_3";
    public static final String EXTRA_IS_RETAKE      = "extra_is_retake";
    public static final String RESULT_HINT_TIER     = "result_hint_tier";
    public static final String RESULT_REFLECTION    = "result_reflection";

    private static final int    REQUEST_AUDIO = 101;
    private static final String TAG           = "TaskActivity";

    // ── Effort thresholds ─────────────────────────────────────────────────────
    private static final int CHARS_TIER1 = 10;
    private static final int CHARS_TIER2 = 30;
    private static final int CHARS_TIER3 = 150;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView     tvSubjectLabel;
    private TextView     tvTaskQuestion;
    private EditText     etAnswer;
    private TextView     tvCharCount;
    private ImageButton  btnMic;
    private TextView     tvRecordingStatus;
    private TextView     tvTranscriptionPreview;
    private WaveformView waveformView;
    private ProgressBar  pbEffort;
    private TextView     tvEffortHintLabel;
    private Button       btnGetHint;
    private Button       btnSubmit;
    private Button       btnDetectiveMode;

    // ── State ─────────────────────────────────────────────────────────────────
    private String  currentSubject;
    private Task    currentTask;
    private int     effortScore         = 0;
    private int     hintTierUsed        = 0;
    private int     hintTierAlreadyUsed = 0;
    private String  reflectionAnswer    = "";
    private boolean isRetake            = false;
    private boolean isListening         = false;

    // ── Speech ────────────────────────────────────────────────────────────────
    private SpeechRecognizer speechRecognizer;

    // ── Hint launcher ─────────────────────────────────────────────────────────
    private ActivityResultLauncher<Intent> hintLauncher;

    // ── Managers ──────────────────────────────────────────────────────────────
    private SharedPreferencesManager prefsManager;
    private DatabaseHelper           dbHelper;
    private ContentLoader            contentLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        prefsManager  = SharedPreferencesManager.getInstance(this);
        dbHelper      = DatabaseHelper.getInstance(this);
        contentLoader = new ContentLoader(this);

        currentSubject = getIntent().getStringExtra(EXTRA_SUBJECT);
        if (currentSubject == null) currentSubject = "dsa";
        String taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        isRetake      = getIntent().getBooleanExtra(EXTRA_IS_RETAKE, false);

        // ── Bind ALL views — must match layout IDs exactly ────────────────────
        tvSubjectLabel         = findViewById(R.id.tv_subject_label);
        tvTaskQuestion         = findViewById(R.id.tv_task_question);
        etAnswer               = findViewById(R.id.et_answer);
        tvCharCount            = findViewById(R.id.tv_char_count);
        btnMic                 = findViewById(R.id.btn_mic);
        tvRecordingStatus      = findViewById(R.id.tv_recording_status);
        tvTranscriptionPreview = findViewById(R.id.tv_transcription_preview);
        waveformView           = findViewById(R.id.waveform_view);
        pbEffort               = findViewById(R.id.pb_effort);
        tvEffortHintLabel      = findViewById(R.id.tv_effort_hint_label);
        btnGetHint             = findViewById(R.id.btn_get_hint);
        btnSubmit              = findViewById(R.id.btn_submit);
        btnDetectiveMode       = findViewById(R.id.btn_detective_mode);

        // Verify critical views found — if any are null the layout is wrong
        if (pbEffort == null || etAnswer == null || btnGetHint == null) {
            Log.e(TAG, "CRITICAL: one or more views not found in layout. "
                    + "Ensure activity_task.xml is the latest version.");
            Toast.makeText(this, "Layout error — please rebuild.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ── Set initial progress bar state ────────────────────────────────────
        pbEffort.setMax(3);
        pbEffort.setProgress(0);

        // ── Wire everything ───────────────────────────────────────────────────
        registerHintLauncher();

        if (taskId != null && !taskId.isEmpty()) {
            loadSpecificTask(taskId);
        } else {
            loadNextTask();
        }

        setupTextWatcher();
        setupMicButton();
        setupHintButton();
        setupSubmitButton();
        setupDetectiveButton();

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        stopListening();
                        finish();
                    }
                });
    }

    // ── Hint launcher ─────────────────────────────────────────────────────────

    private void registerHintLauncher() {
        hintLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null) {
                        int tier = result.getData().getIntExtra(RESULT_HINT_TIER, 0);
                        reflectionAnswer = result.getData()
                                .getStringExtra(RESULT_REFLECTION);
                        if (reflectionAnswer == null) reflectionAnswer = "";

                        if (tier > hintTierAlreadyUsed) {
                            float delta = 0f;
                            for (int t = hintTierAlreadyUsed + 1; t <= tier; t++) {
                                if      (t == 1) delta += SharedPreferencesManager.TANK_DELTA_HINT_TIER1;
                                else if (t == 2) delta += SharedPreferencesManager.TANK_DELTA_HINT_TIER2;
                                else if (t == 3) delta += SharedPreferencesManager.TANK_DELTA_HINT_TIER3;
                            }
                            prefsManager.adjustTankLevel(delta);
                            hintTierAlreadyUsed = tier;
                            hintTierUsed        = tier;
                        }
                        updateHintButton();
                    }
                });
    }

    // ── Task loading ──────────────────────────────────────────────────────────

    private void loadNextTask() {
        List<Task>   tasks        = contentLoader.loadTasks(currentSubject);
        List<String> completedIds = dbHelper.getCompletedTaskIds(currentSubject);
        currentTask = null;
        for (Task t : tasks) {
            if (!completedIds.contains(t.getId())) { currentTask = t; break; }
        }
        if (currentTask == null) { finish(); return; }
        displayTask();
    }

    private void loadSpecificTask(String taskId) {
        List<Task> all = contentLoader.loadTasks(currentSubject);
        Log.d(TAG, "loadSpecificTask: " + taskId + " in " + all.size() + " tasks");

        currentTask = null;
        for (Task t : all) { if (taskId.equals(t.getId())) { currentTask = t; break; } }

        if (currentTask == null && !all.isEmpty()) {
            try {
                int pos = Integer.parseInt(taskId.replaceAll("[^0-9]", "")) - 1;
                if (pos >= 0 && pos < all.size()) currentTask = all.get(pos);
            } catch (Exception e) { Log.e(TAG, "positional fallback failed", e); }
        }
        if (currentTask == null && !all.isEmpty()) currentTask = all.get(0);
        if (currentTask == null) {
            Toast.makeText(this, "Could not load task.", Toast.LENGTH_SHORT).show();
            finish(); return;
        }
        displayTask();
    }

    private void displayTask() {
        String label = getSubjectDisplayName(currentSubject);
        if (isRetake) label += "  ·  Retake";
        tvSubjectLabel.setText(label);
        tvTaskQuestion.setText(currentTask.getQuestion());
        if (prefsManager.isDetectiveModeUnlocked(currentSubject)) unlockDetectiveMode();
    }

    // ── TextWatcher — sole source of effort score ─────────────────────────────

    private void setupTextWatcher() {
        etAnswer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){}
            @Override
            public void afterTextChanged(Editable s) {
                int chars = s.toString().trim().length();
                tvCharCount.setText(chars + " characters");
                recalcEffort(chars);
            }
        });
    }

    // ── Effort scoring ────────────────────────────────────────────────────────

    private void recalcEffort(int chars) {
        if      (chars >= CHARS_TIER3) effortScore = 3;
        else if (chars >= CHARS_TIER2) effortScore = 2;
        else if (chars >= CHARS_TIER1) effortScore = 1;
        else                           effortScore = 0;

        pbEffort.setProgress(effortScore);

        int color;
        switch (effortScore) {
            case 3:  color = getColor(R.color.colorCorrect); break;
            case 2:  color = getColor(R.color.colorWarning); break;
            case 1:  color = getColor(R.color.colorWrong);   break;
            default: color = getColor(R.color.colorBorder);  break;
        }
        pbEffort.setProgressTintList(
                android.content.res.ColorStateList.valueOf(color));

        updateHintButton();
    }

    // ── Hint button label ─────────────────────────────────────────────────────

    private void updateHintButton() {
        if (effortScore == 0) {
            btnGetHint.setText(getString(R.string.task_hint_try_more));
            btnGetHint.setAlpha(0.5f);
            tvEffortHintLabel.setText("Write or speak your attempt to unlock hints");
            tvEffortHintLabel.setTextColor(getColor(R.color.colorTextSecondary));
            return;
        }
        if (hintTierAlreadyUsed >= 3) {
            btnGetHint.setText("All hints used");
            btnGetHint.setAlpha(0.45f);
            tvEffortHintLabel.setText("You have used all available hints");
            tvEffortHintLabel.setTextColor(getColor(R.color.colorTextSecondary));
            return;
        }
        if (hintTierAlreadyUsed >= effortScore) {
            btnGetHint.setText("Write more to unlock next hint");
            btnGetHint.setAlpha(0.45f);
            tvEffortHintLabel.setText("Keep writing to unlock tier " + (hintTierAlreadyUsed + 1));
            tvEffortHintLabel.setTextColor(getColor(R.color.colorTextSecondary));
            return;
        }
        int next = Math.min(Math.max(effortScore, hintTierAlreadyUsed + 1), 3);
        btnGetHint.setAlpha(1.0f);
        switch (next) {
            case 1:
                btnGetHint.setText(getString(R.string.task_hint_get_nudge));
                tvEffortHintLabel.setText("Tier 1 - gentle nudge available");
                tvEffortHintLabel.setTextColor(getColor(R.color.colorTextSecondary));
                break;
            case 2:
                btnGetHint.setText("Get a partial hint");
                tvEffortHintLabel.setText("Tier 2 - partial hint available");
                tvEffortHintLabel.setTextColor(getColor(R.color.colorTankMid));
                break;
            case 3:
                btnGetHint.setText(getString(R.string.task_hint_get_full));
                tvEffortHintLabel.setText("Tier 3 - full guidance available");
                tvEffortHintLabel.setTextColor(getColor(R.color.colorTankFull));
                break;
        }
    }

    // ── Mic / SpeechRecognizer ────────────────────────────────────────────────

    private void setupMicButton() {
        // Check availability upfront
        boolean available = SpeechRecognizer.isRecognitionAvailable(this);
        if (!available) {
            btnMic.setAlpha(0.35f);
            tvRecordingStatus.setText("Speech recognition not available");
            btnMic.setOnClickListener(v ->
                    Toast.makeText(this,
                            "Speech recognition requires Google app or Play Services.",
                            Toast.LENGTH_LONG).show());
            return;
        }

        btnMic.setOnClickListener(v -> {
            if (isListening) {
                stopListening();
            } else {
                if (checkAudioPermission()) startListening();
            }
        });
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) return true;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int code,
                                           String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQUEST_AUDIO && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            Toast.makeText(this,
                    getString(R.string.permission_audio_required),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void startListening() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                runOnUiThread(() -> {
                    isListening = true;
                    tvRecordingStatus.setText("Listening... speak now");
                    tvRecordingStatus.setTextColor(getColor(R.color.colorCorrect));
                    btnMic.setImageResource(android.R.drawable.ic_media_pause);
                    if (waveformView != null) {
                        waveformView.setVisibility(View.VISIBLE);
                        waveformView.startWave();
                    }
                    if (tvTranscriptionPreview != null) {
                        tvTranscriptionPreview.setText("");
                        tvTranscriptionPreview.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                runOnUiThread(() ->
                        tvRecordingStatus.setText("Processing..."));
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                runOnUiThread(() -> {
                    List<String> partial = partialResults.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);
                    if (partial != null && !partial.isEmpty()
                            && !partial.get(0).isEmpty()) {
                        if (tvTranscriptionPreview != null) {
                            tvTranscriptionPreview.setVisibility(View.VISIBLE);
                            tvTranscriptionPreview.setText(partial.get(0) + "...");
                        }
                    }
                });
            }

            @Override
            public void onResults(Bundle results) {
                runOnUiThread(() -> {
                    List<String> matches = results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);

                    if (matches != null && !matches.isEmpty()) {
                        String transcribed = matches.get(0).trim();
                        if (!transcribed.isEmpty()) {
                            appendText(transcribed);
                        }
                    }
                    resetListeningUI();
                });
            }

            @Override
            public void onError(int error) {
                runOnUiThread(() -> {
                    Log.w(TAG, "SpeechRecognizer error: " + error);
                    resetListeningUI();
                    // 6=timeout, 7=no match — non-fatal, stay silent
                    // Show toast for all other errors so issues are visible
                    if (error != SpeechRecognizer.ERROR_NO_MATCH
                            && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Toast.makeText(TaskActivity.this,
                                "Speech error " + error + " — please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        // Minimal intent — no non-standard extras that cause silent failures
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        speechRecognizer.startListening(intent);
    }

    private void stopListening() {
        // Guard: only call stopListening if recognizer is actually active
        // Calling it when idle causes ERROR_CLIENT (error 5)
        if (isListening && speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception e) {
                Log.w(TAG, "stopListening exception: " + e.getMessage());
            }
        }
        resetListeningUI();
    }

    private void resetListeningUI() {
        isListening = false;
        if (waveformView != null) {
            waveformView.stopWave();
            waveformView.setVisibility(View.INVISIBLE);
        }
        if (tvTranscriptionPreview != null) {
            tvTranscriptionPreview.setVisibility(View.GONE);
            tvTranscriptionPreview.setText("");
        }
        tvRecordingStatus.setText(getString(R.string.task_record_start));
        tvRecordingStatus.setTextColor(getColor(R.color.colorTextSecondary));
        btnMic.setImageResource(android.R.drawable.ic_btn_speak_now);
    }

    /**
     * Appends transcribed text to the EditText with a space separator.
     * TextWatcher fires automatically, updating effort score and hint label.
     */
    private void appendText(String transcribed) {
        if (transcribed == null || transcribed.trim().isEmpty()) return;

        String trimmed = transcribed.trim();

        // append() inserts at the current cursor position and always renders —
        // more reliable than setText() when the IME is active or view has focus
        if (etAnswer.getText().toString().trim().isEmpty()) {
            // Field is empty — set directly so there's no leading space
            etAnswer.setText(trimmed);
            etAnswer.setSelection(etAnswer.getText().length());
        } else {
            // Field has content — append with a space separator
            etAnswer.append(" ");
            etAnswer.append(trimmed);
        }

        // Log so you can verify in Logcat that onResults fired and text was set
        Log.d(TAG, "Transcribed and appended: [" + trimmed + "]");
        Log.d(TAG, "EditText now contains: [" + etAnswer.getText().toString() + "]");
    }

    // ── Hint button ───────────────────────────────────────────────────────────

    private void setupHintButton() {
        btnGetHint.setOnClickListener(v -> {
            if (currentTask == null) return;
            if (effortScore == 0) {
                Toast.makeText(this,
                        "Write or speak your attempt first.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (hintTierAlreadyUsed >= 3) {
                Toast.makeText(this,
                        "All hints used for this task.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (hintTierAlreadyUsed >= effortScore) {
                Toast.makeText(this,
                        "Write more to unlock the next hint tier.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (isListening) stopListening();

            int tier = Math.min(Math.max(effortScore, hintTierAlreadyUsed + 1), 3);
            String answer = etAnswer.getText().toString().trim();
            ConceptCheckResult check =
                    ConceptChecker.check(answer, currentTask.getKeywords());

            Intent intent = new Intent(this, HintActivity.class);
            intent.putExtra(EXTRA_SUBJECT,         currentSubject);
            intent.putExtra(EXTRA_TASK_ID,         currentTask.getId());
            intent.putExtra(EXTRA_EFFORT_SCORE,    effortScore);
            intent.putExtra(EXTRA_HINT_TIER_USED,  tier);
            intent.putExtra(EXTRA_ANSWER_TEXT,     answer);
            intent.putExtra(EXTRA_TASK_QUESTION,   currentTask.getQuestion());
            intent.putExtra(EXTRA_HINT_1,          currentTask.getHintTier1());
            intent.putExtra(EXTRA_HINT_2,          currentTask.getHintTier2());
            intent.putExtra(EXTRA_HINT_3,          currentTask.getHintTier3());
            intent.putExtra("extra_concept_check", check.serialise());
            hintLauncher.launch(intent);
        });
    }

    // ── Submit button ─────────────────────────────────────────────────────────

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            if (isListening) stopListening();

            String answerText = etAnswer.getText().toString().trim();

            if (answerText.isEmpty()) {
                Toast.makeText(this,
                        "Please write or speak your attempt before submitting.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentTask == null) return;

            // Save to SQLite
            dbHelper.insertTaskAttempt(
                    currentSubject,
                    currentTask.getId(),
                    effortScore,
                    hintTierUsed,
                    answerText,
                    "",
                    reflectionAnswer);

            if (!isRetake) {
                prefsManager.incrementCompletedTaskCount(currentSubject);
                if (hintTierUsed == 0)
                    prefsManager.adjustTankLevel(
                            SharedPreferencesManager.TANK_DELTA_INDEPENDENT);
                if (prefsManager.isDetectiveModeUnlocked(currentSubject))
                    unlockDetectiveMode();
            } else {
                prefsManager.adjustTankLevel(0.04f);
            }

            btnSubmit.setEnabled(false);

            float tankChange = isRetake ? 0.04f
                    : hintTierUsed == 0
                    ? SharedPreferencesManager.TANK_DELTA_INDEPENDENT
                    : 0f;

            // Key concepts for reveal sheet
            List<String> keyConcepts = currentTask.getKeyConcepts();
            if (keyConcepts == null || keyConcepts.isEmpty()) {
                keyConcepts = new ArrayList<>();
                keyConcepts.add("Review the core definition of this concept");
                keyConcepts.add("Consider real-world applications");
            }

            // ── Declare finals BEFORE any lambda that captures them ───────────
            final String finalAnswerText = answerText;
            final String finalSubject    = currentSubject;
            final String finalTaskId     = currentTask.getId();
            final String aiAnswer        = currentTask.getHintTier3() != null
                    ? currentTask.getHintTier3()
                    : "Reference not available.";

            // ── Concept check — uses finals which are now in scope ────────────
            ConceptCheckResult conceptResult =
                    ConceptChecker.check(finalAnswerText, currentTask.getKeywords());
            List<String> missingConcepts = conceptResult.getMissing();

            // ── Build reveal sheet ────────────────────────────────────────────
            AnswerRevealSheet sheet = AnswerRevealSheet.newInstance(
                    currentTask.getQuestion(),
                    keyConcepts,
                    hintTierUsed,
                    tankChange,
                    finalTaskId,
                    finalAnswerText,
                    finalSubject,
                    missingConcepts);

            sheet.setOnDismissListener(() -> finish());

            sheet.setOnCompareWithAiListener(() -> {
                // Run concept check so mirror has matched/missing context
                ConceptCheckResult mirrorCheck =
                        ConceptChecker.check(finalAnswerText,
                                currentTask.getKeywords());

                Intent mirrorIntent = new Intent(
                        TaskActivity.this, AIMirrorActivity.class);
                mirrorIntent.putExtra(
                        AIMirrorActivity.EXTRA_SUBJECT,     finalSubject);
                mirrorIntent.putExtra(
                        AIMirrorActivity.EXTRA_TASK_ID,     finalTaskId);
                mirrorIntent.putExtra(
                        AIMirrorActivity.EXTRA_YOUR_ANSWER, finalAnswerText);
                mirrorIntent.putExtra(
                        AIMirrorActivity.EXTRA_AI_ANSWER,   aiAnswer);
                mirrorIntent.putExtra(
                        AIMirrorActivity.EXTRA_QUESTION,
                        currentTask.getQuestion());
                mirrorIntent.putStringArrayListExtra(
                        AIMirrorActivity.EXTRA_MATCHED_CONCEPTS,
                        new java.util.ArrayList<>(mirrorCheck.getMatched()));
                mirrorIntent.putStringArrayListExtra(
                        AIMirrorActivity.EXTRA_MISSING_CONCEPTS,
                        new java.util.ArrayList<>(mirrorCheck.getMissing()));
                startActivity(mirrorIntent);
            });

            sheet.show(getSupportFragmentManager(), "reveal");
        });
    }

    // ── Detective button ──────────────────────────────────────────────────────

    private void setupDetectiveButton() {
        btnDetectiveMode.setOnClickListener(v -> {
            if (isListening) stopListening();
            Intent intent = new Intent(this, DetectiveActivity.class);
            intent.putExtra(EXTRA_SUBJECT, currentSubject);
            startActivity(intent);
        });
    }

    private void unlockDetectiveMode() {
        btnDetectiveMode.setEnabled(true);
        btnDetectiveMode.setAlpha(1.0f);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getSubjectDisplayName(String key) {
        switch (key) {
            case "dsa":  return getString(R.string.subject_dsa);
            case "dbms": return getString(R.string.subject_dbms);
            case "web":  return getString(R.string.subject_web);
            case "oop":  return getString(R.string.subject_oop);
            case "dm":   return getString(R.string.subject_dm);
            default:     return key.toUpperCase();
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onPause() {
        super.onPause();
        if (isListening) stopListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}