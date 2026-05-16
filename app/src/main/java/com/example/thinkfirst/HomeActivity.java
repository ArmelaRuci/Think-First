package com.example.thinkfirst;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thinkfirst.adapter.SubjectAdapter;
import com.example.thinkfirst.model.Subject;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private SharedPreferencesManager prefsManager;
    private DatabaseHelper           dbHelper;

    private TankGaugeView tankGaugeView;
    private TextView      tvTankLabel;
    private SubjectAdapter subjectAdapter;
    private List<Subject>  subjectList;
    private LinearLayout llOnboardingContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        prefsManager = SharedPreferencesManager.getInstance(this);
        dbHelper     = DatabaseHelper.getInstance(this);
        prefsManager.updateLastOpened();

        // ── Bind views ────────────────────────────────────────────────────────
        tankGaugeView           = findViewById(R.id.tank_gauge_view);
        tvTankLabel             = findViewById(R.id.tv_tank_label);
        Button btnMyProgress    = findViewById(R.id.btn_my_progress);
        RecyclerView rvSubjects = findViewById(R.id.rv_subjects);
        llOnboardingContainer   = findViewById(R.id.ll_onboarding_container);

        // ── Subject RecyclerView ──────────────────────────────────────────────
        rvSubjects.setLayoutManager(new LinearLayoutManager(this));
        subjectList   = buildSubjectList();

        subjectAdapter = new SubjectAdapter(subjectList, subject -> {
            prefsManager.setCurrentSubject(subject.getKey());

            // ── Cold Open check ───────────────────────────────────────────────
            // Show recall prompt if:
            // 1. Student has completed at least one task in this subject
            // 2. More than 48 hours since their last attempt in this subject
            int  completed     = prefsManager.getCompletedTaskCount(subject.getKey());
            long lastAttemptTs = dbHelper.getLastAttemptTimestamp(subject.getKey());
            long elapsedMs     = System.currentTimeMillis() - lastAttemptTs;
            long fortyEightH   = 48L * 60L * 60L * 1000L;

            boolean shouldShowColdOpen =
                    completed > 0
                            && lastAttemptTs > 0
                            && elapsedMs >= fortyEightH;

            if (shouldShowColdOpen) {
                showColdOpen(subject.getKey());
            } else {
                navigateToChapterSelect(subject.getKey());
            }
        });
        rvSubjects.setAdapter(subjectAdapter);

        showOnboardingIfNeeded();

        // ── My Progress button → StatsActivity ───────────────────────────────
        btnMyProgress.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, StatsActivity.class))
        );
    }

    private void showColdOpen(String subjectKey) {
        // Pick a random completed task to recall from
        String taskId = dbHelper.getRandomCompletedTaskId(subjectKey);
        if (taskId == null) {
            // Fallback — no completed task found, go straight to chapters
            navigateToChapterSelect(subjectKey);
            return;
        }

        // Load the task question from JSON
        ContentLoader loader   = new ContentLoader(this);
        List<com.example.thinkfirst.model.Task> tasks =
                loader.loadTasks(subjectKey);

        String question = null;
        String foundId  = null;
        for (com.example.thinkfirst.model.Task t : tasks) {
            if (taskId.equals(t.getId())) {
                question = t.getQuestion();
                foundId  = t.getId();
                break;
            }
        }

        if (question == null) {
            // Task not found in JSON — skip cold open
            navigateToChapterSelect(subjectKey);
            return;
        }

        final String finalSubject = subjectKey;

        ColdOpenDialog dialog = ColdOpenDialog.newInstance(
                subjectKey, question, foundId);

        dialog.setOnCompleteListener(subject ->
                navigateToChapterSelect(finalSubject));

        dialog.show(getSupportFragmentManager(), "cold_open");
    }

    private void navigateToChapterSelect(String subjectKey) {
        Intent intent = new Intent(HomeActivity.this,
                ChapterSelectActivity.class);
        intent.putExtra(ChapterSelectActivity.EXTRA_SUBJECT, subjectKey);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tank every time the screen comes back into view
        refreshTankGauge();
        // Refresh subject completed counts
        refreshSubjectCounts();
    }

    // ── Tank gauge ────────────────────────────────────────────────────────────

    private void refreshTankGauge() {
        float level = prefsManager.getTankLevel();
        tankGaugeView.setLevel(level);

        if (dbHelper.getTotalTasksCompleted() == 0) {
            // No history yet — don't show a misleading score
            tvTankLabel.setText("Complete your first task to track independence");
            tvTankLabel.setTextColor(getColor(R.color.colorTextSecondary));
        } else {
            updateTankLabel(level);
        }
    }

    private void updateTankLabel(float level) {
        String label;
        int    colorRes;

        if (level >= 0.8f) {
            label    = getString(R.string.tank_label_full);
            colorRes = R.color.colorTankFull;
        } else if (level >= 0.5f) {
            label    = getString(R.string.tank_label_mid);
            colorRes = R.color.colorTankMid;
        } else if (level >= 0.25f) {
            label    = getString(R.string.tank_label_low);
            colorRes = R.color.colorTankLow;
        } else {
            label    = getString(R.string.tank_label_critical);
            colorRes = R.color.colorTankLow;
        }

        tvTankLabel.setText(label);
        tvTankLabel.setTextColor(getColor(colorRes));
    }

    // ── Subject list ──────────────────────────────────────────────────────────

    private List<Subject> buildSubjectList() {
        List<Subject> list = new ArrayList<>();
        list.add(new Subject(
                Subject.DSA,
                getString(R.string.subject_dsa),
                "Arrays, linked lists, trees, sorting, searching",
                R.color.colorSubjectDsa,
                R.drawable.ic_subject_dsa));
        list.add(new Subject(
                Subject.DBMS,
                getString(R.string.subject_dbms),
                "SQL, normalization, transactions, indexing",
                R.color.colorSubjectDbms,
                R.drawable.ic_subject_dbms));
        list.add(new Subject(
                Subject.WEB,
                getString(R.string.subject_web),
                "HTML, CSS, JavaScript, HTTP, REST APIs",
                R.color.colorSubjectWeb,
                R.drawable.ic_subject_web));
        list.add(new Subject(
                Subject.OOP,
                getString(R.string.subject_oop),
                "Classes, inheritance, polymorphism",
                R.color.colorSubjectOop,
                R.drawable.ic_subject_oop));
        list.add(new Subject(
                Subject.DM,
                getString(R.string.subject_dm),
                "Classification, clustering, association rules",
                R.color.colorSubjectDm,
                R.drawable.ic_subject_dm));
        for (Subject s : list) {
            s.setTasksCompleted(prefsManager.getCompletedTaskCount(s.getKey()));
        }
        return list;
    }

    private void refreshSubjectCounts() {
        if (subjectList == null || subjectAdapter == null) return;
        for (Subject s : subjectList) {
            s.setTasksCompleted(prefsManager.getCompletedTaskCount(s.getKey()));
        }
        subjectAdapter.notifyDataSetChanged();
    }


    private void showOnboardingIfNeeded() {
        if (prefsManager.isOnboardingDone()) return;

        // Inflate the card into the container
        View card = getLayoutInflater().inflate(
                R.layout.item_onboarding_card,
                llOnboardingContainer,
                false
        );

        Button btnDismiss = card.findViewById(R.id.btn_onboarding_dismiss);
        btnDismiss.setOnClickListener(v -> {
            // Animate out
            llOnboardingContainer.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(250)
                    .withEndAction(() -> {
                        llOnboardingContainer.setVisibility(View.GONE);
                        prefsManager.setOnboardingDone();
                    })
                    .start();
        });

        llOnboardingContainer.addView(card);
        llOnboardingContainer.setVisibility(View.VISIBLE);

        // Animate in
        llOnboardingContainer.setAlpha(0f);
        llOnboardingContainer.setTranslationY(16f);
        llOnboardingContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .start();
    }
}