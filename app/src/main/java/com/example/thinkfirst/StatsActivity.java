package com.example.thinkfirst;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.thinkfirst.model.Subject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    private SharedPreferencesManager prefsManager;
    private DatabaseHelper           dbHelper;

    // ── Rank metadata — single source of truth ────────────────────────────────
    private static final String[] RANK_NAMES      =
            {"Rookie", "Investigator", "Analyst", "Senior Detective", "Chief Inspector"};
    private static final int[]    RANK_THRESHOLDS =
            {0, 50, 150, 300, 400};  // points needed to REACH each rank
    private static final int[]    RANK_ICONS      = {
            R.drawable.ic_rank_rookie,
            R.drawable.ic_rank_investigator,
            R.drawable.ic_rank_analyst,
            R.drawable.ic_rank_senior,
            R.drawable.ic_rank_chief
    };

    // ── Subject metadata ──────────────────────────────────────────────────────
    private static final String[] SUBJECT_KEYS   =
            {Subject.DSA, Subject.DBMS, Subject.WEB, Subject.OOP, Subject.DM};
    private static final int[]    SUBJECT_COLORS =
            {R.color.colorSubjectDsa, R.color.colorSubjectDbms,
                    R.color.colorSubjectWeb, R.color.colorSubjectOop,
                    R.color.colorSubjectDm};
    private static final String[] SUBJECT_SHORT  =
            {"DSA", "DBMS", "Web", "OOP", "Mining"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        prefsManager = SharedPreferencesManager.getInstance(this);
        dbHelper     = DatabaseHelper.getInstance(this);

        populateQuickStats();
        populateTankSection();
        populateBarChart();
        populateRankJourney();
        populateRecentCases();
    }

    // ── Quick stats ───────────────────────────────────────────────────────────

    private void populateQuickStats() {
        int    total   = dbHelper.getTotalTasksCompleted();
        int    hints   = dbHelper.getTotalHintsUsed();
        int    solo    = total - hints;
        String soloRate = total > 0
                ? Math.round((solo / (float) total) * 100) + "%" : "—";

        ((TextView) findViewById(R.id.tv_stat_tasks_count)).setText(
                String.valueOf(total));
        ((TextView) findViewById(R.id.tv_stat_hints_count)).setText(
                String.valueOf(hints));

        TextView tvSolo = findViewById(R.id.tv_stat_independence);
        tvSolo.setText(soloRate);

        int soloColor;
        if (total == 0)                  soloColor = R.color.colorTextSecondary;
        else if (solo >= total * 0.7f)   soloColor = R.color.colorTankFull;
        else if (solo >= total * 0.4f)   soloColor = R.color.colorTankMid;
        else                             soloColor = R.color.colorTankLow;
        tvSolo.setTextColor(getColor(soloColor));
    }

    // ── Tank section ──────────────────────────────────────────────────────────

    private void populateTankSection() {
        float  level   = prefsManager.getTankLevel();
        int    percent = Math.round(level * 100);

        TextView tvPercent = findViewById(R.id.tv_tank_percent);
        tvPercent.setText(percent + "%");

        String status; int colorRes;
        if      (level >= 0.8f) { status = getString(R.string.tank_label_full);     colorRes = R.color.colorTankFull; }
        else if (level >= 0.5f) { status = getString(R.string.tank_label_mid);      colorRes = R.color.colorTankMid; }
        else if (level >= 0.25f){ status = getString(R.string.tank_label_low);      colorRes = R.color.colorTankLow; }
        else                    { status = getString(R.string.tank_label_critical);  colorRes = R.color.colorTankLow; }

        tvPercent.setTextColor(getColor(colorRes));
    }

    // ── Bar chart ─────────────────────────────────────────────────────────────

    private void populateBarChart() {
        BarChartView barChart  = findViewById(R.id.bar_chart_subjects);
        TextView     tvSub     = findViewById(R.id.tv_chart_subtitle);
        int          totalDone = dbHelper.getTotalTasksCompleted();

        if (totalDone == 0) {
            barChart.setVisibility(View.GONE);
            if (tvSub != null) tvSub.setText(
                    "Complete tasks to see your performance chart.");
            return;
        }

        barChart.setVisibility(View.VISIBLE);
        List<BarChartView.BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < SUBJECT_KEYS.length; i++) {
            float  avg       = dbHelper.getAverageEffortScore(SUBJECT_KEYS[i]);
            int    completed = prefsManager.getCompletedTaskCount(SUBJECT_KEYS[i]);
            String label     = completed > 0
                    ? String.format(Locale.US, "%.1f", avg) : "—";
            entries.add(new BarChartView.BarEntry(
                    SUBJECT_SHORT[i], avg, 3f,
                    ContextCompat.getColor(this, SUBJECT_COLORS[i]), label));
        }
        barChart.setData(entries);
    }

    // ── Rank journey ──────────────────────────────────────────────────────────

    private void populateRankJourney() {
        LinearLayout llStrip = findViewById(R.id.ll_rank_strip);
        llStrip.removeAllViews();

        int    totalPoints  = prefsManager.getTotalDetectivePoints();
        String currentRank  = prefsManager.getCurrentRank();
        int    currentIndex = rankIndex(currentRank);

        float density = getResources().getDisplayMetrics().density;
        int   gapPx   = (int)(4 * density);

        for (int i = 0; i < RANK_NAMES.length; i++) {
            boolean achieved = totalPoints >= RANK_THRESHOLDS[i];
            boolean isCurrent = i == currentIndex;

            View card = buildRankCard(i, achieved, isCurrent, density);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) params.leftMargin = gapPx;
            card.setLayoutParams(params);

            llStrip.addView(card);
        }

        // Points summary below the strip
        TextView tvPoints   = findViewById(R.id.tv_detective_points);
        TextView tvNextRank = findViewById(R.id.tv_detective_next_rank);

        tvPoints.setText(totalPoints + " detective points");

        // Next rank progress message
        String nextMsg;
        if (currentIndex >= RANK_NAMES.length - 1) {
            nextMsg = "Maximum rank achieved — Chief Inspector!";
        } else {
            int nextThreshold = RANK_THRESHOLDS[currentIndex + 1];
            int needed        = nextThreshold - totalPoints;
            nextMsg = needed + " pts to " + RANK_NAMES[currentIndex + 1];
        }
        tvNextRank.setText(nextMsg);
    }

    /**
     * Builds a single rank card for the progression strip.
     *
     * States:
     * - Achieved (not current): full opacity, green checkmark overlay, subtle border
     * - Current: gold border, full opacity, rank name bold
     * - Not yet: 30% opacity, faded
     */
    private View buildRankCard(int rankIdx, boolean achieved,
                               boolean isCurrent, float density) {
        int padPx = (int)(10 * density);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(padPx, (int)(12 * density), padPx, (int)(10 * density));

        // Background and border
        if (isCurrent) {
            card.setBackgroundColor(getColor(R.color.colorSurface));
        } else if (achieved) {
            card.setBackgroundColor(getColor(R.color.colorSurface));
        } else {
            card.setBackgroundColor(getColor(R.color.colorBackground));
        }

        // Icon
        ImageView ivIcon = new ImageView(this);
        int iconSize = (int)(28 * density);
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.bottomMargin = (int)(6 * density);
        ivIcon.setLayoutParams(iconParams);
        ivIcon.setImageResource(RANK_ICONS[rankIdx]);
        ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Tint: gold for current, green for achieved, gray for locked
        int iconTint;
        if      (isCurrent) iconTint = getColor(R.color.colorDetectiveGold);
        else if (achieved)  iconTint = getColor(R.color.colorCorrect);
        else                iconTint = getColor(R.color.colorTextSecondary);
        ivIcon.setImageTintList(ColorStateList.valueOf(iconTint));

        // Rank name
        TextView tvName = new TextView(this);
        // Abbreviate long names so they fit
        tvName.setText(shortRankName(rankIdx));
        tvName.setTextSize(9f);
        tvName.setGravity(Gravity.CENTER);
        tvName.setTextColor(isCurrent
                ? getColor(R.color.colorDetectiveGold)
                : achieved
                ? getColor(R.color.colorTextPrimary)
                : getColor(R.color.colorTextSecondary));
        if (isCurrent) tvName.setTypeface(null, Typeface.BOLD);
        tvName.setMaxLines(2);

        card.addView(ivIcon);
        card.addView(tvName);

        // Achieved non-current: show small green tick below name
        if (achieved && !isCurrent) {
            TextView tvTick = new TextView(this);
            tvTick.setText("✓");
            tvTick.setTextSize(9f);
            tvTick.setGravity(Gravity.CENTER);
            tvTick.setTextColor(getColor(R.color.colorCorrect));
            LinearLayout.LayoutParams tickParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tickParams.topMargin = (int)(2 * density);
            tvTick.setLayoutParams(tickParams);
            card.addView(tvTick);
        }

        // Current rank: show "YOU" indicator
        if (isCurrent) {
            TextView tvYou = new TextView(this);
            tvYou.setText("▲ HERE");
            tvYou.setTextSize(7f);
            tvYou.setGravity(Gravity.CENTER);
            tvYou.setTextColor(getColor(R.color.colorDetectiveGold));
            tvYou.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams youParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            youParams.topMargin = (int)(3 * density);
            tvYou.setLayoutParams(youParams);
            card.addView(tvYou);
        }

        // Locked ranks: fade out
        if (!achieved) card.setAlpha(0.45f);

        return card;
    }

    private String shortRankName(int idx) {
        switch (idx) {
            case 0: return "Rookie";
            case 1: return "Investigator";
            case 2: return "Analyst";
            case 3: return "Senior\nDetective";
            case 4: return "Chief\nInspector";
            default: return RANK_NAMES[idx];
        }
    }

    private int rankIndex(String rank) {
        for (int i = 0; i < RANK_NAMES.length; i++) {
            if (RANK_NAMES[i].equals(rank)) return i;
        }
        return 0;
    }

    // ── Recent cases — limited to 3 ───────────────────────────────────────────

    private void populateRecentCases() {
        LinearLayout llCases = findViewById(R.id.ll_recent_cases);
        llCases.removeAllViews();

        // Fetch only 3
        List<DatabaseHelper.DetectiveSessionRecord> sessions =
                dbHelper.getRecentDetectiveSessions(3);

        float density = getResources().getDisplayMetrics().density;
        int   padH    = (int)(16 * density);
        int   padV    = (int)(12 * density);

        if (sessions.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText(
                    "No detective sessions yet. \nComplete 3 tasks in any subject to unlock Detective Mode.");
            tvEmpty.setTextColor(getColor(R.color.colorTextSecondary));
            tvEmpty.setTextSize(13f);
            tvEmpty.setPadding(padH, padV, padH, padV);
            tvEmpty.setLineSpacing(0f, 1.4f);
            llCases.addView(tvEmpty);
            return;
        }

        boolean first = true;
        for (DatabaseHelper.DetectiveSessionRecord session : sessions) {
            if (!first) {
                View divider = new View(this);
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(dp);
                divider.setBackgroundColor(getColor(R.color.colorBorder));
                llCases.addView(divider);
            }
            first = false;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(padH, padV, padH, padV);

            // Topic + date
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvTopic = new TextView(this);
            tvTopic.setText(session.topic);
            tvTopic.setTextColor(getColor(R.color.colorTextPrimary));
            tvTopic.setTextSize(14f);
            tvTopic.setTypeface(null, Typeface.BOLD);
            tvTopic.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvDate = new TextView(this);
            tvDate.setText(formatRelativeTime(session.timestamp));
            tvDate.setTextColor(getColor(R.color.colorTextSecondary));
            tvDate.setTextSize(12f);

            topRow.addView(tvTopic);
            topRow.addView(tvDate);

            // Score detail
            TextView tvDetail = new TextView(this);
            int pts = session.score;
            String ptsText = pts > 0 ? "+" + pts : String.valueOf(pts);
            tvDetail.setText(ptsText + " pts  ·  "
                    + session.errorsCaught + " caught  ·  "
                    + session.falseFlags  + " false flags");
            tvDetail.setTextColor(session.score > 0
                    ? getColor(R.color.colorCorrect)
                    : getColor(R.color.colorTextSecondary));
            tvDetail.setTextSize(12f);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            detailParams.topMargin = (int)(4 * density);
            tvDetail.setLayoutParams(detailParams);

            row.addView(topRow);
            row.addView(tvDetail);
            llCases.addView(row);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatRelativeTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long days = diff / (1000L * 60 * 60 * 24);
        if (days == 0)  return "Today";
        if (days == 1)  return "Yesterday";
        if (days < 7)   return days + " days ago";
        return new SimpleDateFormat("dd MMM", Locale.US).format(new Date(timestamp));
    }
}