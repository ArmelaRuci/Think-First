package com.example.thinkfirst;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thinkfirst.adapter.ChapterAdapter;
import com.example.thinkfirst.model.Task;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChapterSelectActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT = "chapter_subject";

    private String                   currentSubject;
    private SharedPreferencesManager prefsManager;
    private DatabaseHelper           dbHelper;
    private ContentLoader            contentLoader;

    private RecyclerView  rvChapters;
    private LinearLayout  llAllDoneBanner;
    private Button        btnDetective;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_select);

        currentSubject = getIntent().getStringExtra(EXTRA_SUBJECT);
        if (currentSubject == null) currentSubject = "dsa";

        prefsManager  = SharedPreferencesManager.getInstance(this);
        dbHelper      = DatabaseHelper.getInstance(this);
        contentLoader = new ContentLoader(this);

        // ── Bind views ────────────────────────────────────────────────────────
        TextView  tvSubjectName = findViewById(R.id.tv_chapter_subject_name);
        TextView  tvSubjectDesc = findViewById(R.id.tv_chapter_subject_desc);
        ImageView ivIcon        = findViewById(R.id.iv_chapter_subject_icon);
        rvChapters              = findViewById(R.id.rv_chapters);
        llAllDoneBanner         = findViewById(R.id.ll_all_done_banner);
        btnDetective            = findViewById(R.id.btn_chapter_detective);

        // ── Subject header ────────────────────────────────────────────────────
        tvSubjectName.setText(getSubjectDisplayName(currentSubject));
        tvSubjectDesc.setText(getSubjectDescription(currentSubject));

        int color = ContextCompat.getColor(this, subjectColorRes(currentSubject));
        ivIcon.setImageResource(subjectIconRes(currentSubject));
        ivIcon.setImageTintList(ColorStateList.valueOf(color));
        tvSubjectName.setTextColor(color);

        rvChapters.setLayoutManager(new LinearLayoutManager(this));

        btnDetective.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetectiveActivity.class);
            intent.putExtra(TaskActivity.EXTRA_SUBJECT, currentSubject);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildChapterList();
        updateDetectiveButton();
    }

    // ── Chapter list ──────────────────────────────────────────────────────────

    private void buildChapterList() {
        List<Task>   allTasks     = contentLoader.loadTasks(currentSubject);
        List<String> completedIds = dbHelper.getCompletedTaskIds(currentSubject);

        // Build attempt data map: taskId -> int[]{effortScore, hintTier}
        Map<String, int[]> attemptDataMap = buildAttemptMap();

        // All completed when every task ID has a completed attempt
        boolean allCompleted = !allTasks.isEmpty()
                && completedIds.size() >= allTasks.size();

        // Show/hide all-done banner
        llAllDoneBanner.setVisibility(allCompleted ? View.VISIBLE : View.GONE);

        ChapterAdapter adapter = new ChapterAdapter(
                allTasks,
                completedIds,
                attemptDataMap,
                currentSubject,
                (task, isRetake) -> launchTask(task, isRetake)
        );

        rvChapters.setAdapter(adapter);
    }

    /**
     * Queries the DB for the latest attempt per task and returns a map.
     * Uses the existing getLatestAttemptsForSubject() query.
     */
    private Map<String, int[]> buildAttemptMap() {
        Map<String, int[]> map = new HashMap<>();
        try {
            List<Pair<String, int[]>> rawList =
                    dbHelper.getLatestAttemptsForSubject(currentSubject);
            for (Pair<String, int[]> pair : rawList) {
                if (pair.first != null && pair.second != null) {
                    // pair.second = {effortScore, hintTier, timestamp_seconds}
                    map.put(pair.first, pair.second);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ChapterSelectActivity",
                    "Failed to build attempt map", e);
        }
        return map;
    }

    // ── Task launch ───────────────────────────────────────────────────────────

    private void launchTask(Task task, boolean isRetake) {
        Intent intent = new Intent(ChapterSelectActivity.this, TaskActivity.class);
        intent.putExtra(TaskActivity.EXTRA_SUBJECT,   currentSubject);
        intent.putExtra(TaskActivity.EXTRA_TASK_ID,   task.getId());
        intent.putExtra(TaskActivity.EXTRA_IS_RETAKE, isRetake);
        startActivity(intent);
    }

    // ── Detective button ──────────────────────────────────────────────────────

    private void updateDetectiveButton() {
        boolean unlocked = prefsManager.isDetectiveModeUnlocked(currentSubject);
        btnDetective.setEnabled(unlocked);
        btnDetective.setAlpha(unlocked ? 1.0f : 0.35f);
    }

    // ── Subject metadata ──────────────────────────────────────────────────────

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

    private String getSubjectDescription(String key) {
        switch (key) {
            case "dsa":  return "Arrays, linked lists, trees, sorting, searching";
            case "dbms": return "SQL, normalization, transactions, indexing";
            case "web":  return "HTML, CSS, JavaScript, HTTP, REST APIs";
            case "oop":  return "Classes, inheritance, polymorphism, design patterns";
            case "dm":   return "Classification, clustering, association rules";
            default:     return "";
        }
    }

    private int subjectIconRes(String key) {
        switch (key) {
            case "dsa":  return R.drawable.ic_subject_dsa;
            case "dbms": return R.drawable.ic_subject_dbms;
            case "web":  return R.drawable.ic_subject_web;
            case "oop":  return R.drawable.ic_subject_oop;
            case "dm":   return R.drawable.ic_subject_dm;
            default:     return R.drawable.ic_subject_dsa;
        }
    }

    private int subjectColorRes(String key) {
        switch (key) {
            case "dsa":  return R.color.colorSubjectDsa;
            case "dbms": return R.color.colorSubjectDbms;
            case "web":  return R.color.colorSubjectWeb;
            case "oop":  return R.color.colorSubjectOop;
            case "dm":   return R.color.colorSubjectDm;
            default:     return R.color.colorAccent;
        }
    }
}