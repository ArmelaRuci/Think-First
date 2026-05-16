package com.example.thinkfirst.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thinkfirst.R;
import com.example.thinkfirst.model.Task;
import java.util.List;
import java.util.Map;

public class ChapterAdapter
        extends RecyclerView.Adapter<ChapterAdapter.ChapterVH> {

    public interface OnChapterClickListener {
        void onChapterClick(Task task, boolean isRetake);
    }

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<Task>             tasks;
    private final List<String>           completedIds;
    private final Map<String, int[]>     attemptDataMap; // taskId -> {effortScore, hintTier, ts}
    private final String                 subjectKey;
    private final OnChapterClickListener listener;

    public ChapterAdapter(List<Task> tasks,
                          List<String> completedIds,
                          Map<String, int[]> attemptDataMap,
                          String subjectKey,
                          OnChapterClickListener listener) {
        this.tasks          = tasks;
        this.completedIds   = completedIds;
        this.attemptDataMap = attemptDataMap;
        this.subjectKey     = subjectKey;
        this.listener       = listener;
    }

    @NonNull
    @Override
    public ChapterVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter_card, parent, false);
        return new ChapterVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterVH holder, int position) {
        Task    task      = tasks.get(position);
        boolean completed = completedIds.contains(task.getId());
        int[]   attempt   = attemptDataMap.get(task.getId()); // may be null

        int accentColor = ContextCompat.getColor(
                holder.itemView.getContext(), subjectAccentColorRes(subjectKey));

        // ── Chapter number ────────────────────────────────────────────────────
        holder.tvNumber.setText(String.valueOf(position + 1));

        // ── Chapter name ──────────────────────────────────────────────────────
        String chapter = task.getChapter();
        if (chapter == null || chapter.isEmpty()) {
            chapter = "Chapter " + (position + 1);
        }
        holder.tvName.setText(chapter);

        // ── Difficulty label ──────────────────────────────────────────────────
        holder.tvDifficulty.setText(
                task.getDifficulty() >= 2 ? "Intermediate" : "Beginner");

        // ── Accent bar ────────────────────────────────────────────────────────
        holder.viewAccent.setBackgroundColor(accentColor);

        // ── State-based rendering ─────────────────────────────────────────────
        if (completed && attempt != null) {
            renderCompleted(holder, task, attempt);
        } else if (completed) {
            renderCompletedNoData(holder, task);
        } else {
            renderAvailable(holder, task, accentColor);
        }

        // ── Staggered entry animation ─────────────────────────────────────────
        holder.itemView.animate().cancel();
        holder.itemView.setTranslationX(24f);
        holder.itemView.setAlpha(0f);
        holder.itemView.animate()
                .alpha(completed ? 0.80f : 1.0f)
                .translationX(0f)
                .setStartDelay(position * 55L)
                .setDuration(260)
                .start();
    }

    // ── Completed chapter with attempt data ───────────────────────────────────

    private void renderCompleted(ChapterVH holder, Task task, int[] attempt) {
        int effortScore = attempt[0];
        int hintTier    = attempt[1];

        // ── Status: green checkmark ───────────────────────────────────────────
        holder.tvStatus.setText("✓");
        holder.tvStatus.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.colorCorrect));

        // ── Chapter number: green tint ────────────────────────────────────────
        holder.tvNumber.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.colorCorrect));

        // ── Show completion row + divider ─────────────────────────────────────
        holder.llCompletionRow.setVisibility(View.VISIBLE);
        holder.vDivider.setVisibility(View.VISIBLE);

        // ── Effort dots ───────────────────────────────────────────────────────
        setDotColors(holder, effortScore);

        // ── Badge: INDEPENDENT or HINT TIER X ────────────────────────────────
        String badgeText;
        int    badgeColorRes;
        if (hintTier == 0) {
            badgeText    = "INDEPENDENT";
            badgeColorRes = R.color.colorCorrect;
        } else {
            badgeText    = "HINT TIER " + hintTier;
            badgeColorRes = effortScore >= 2 ? R.color.colorTankMid : R.color.colorTankLow;
        }
        holder.tvCompletionBadge.setText(badgeText);
        holder.tvCompletionBadge.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), badgeColorRes));

        // ── Top row not directly tappable — retake is via button ──────────────
        holder.llTopRow.setOnClickListener(null);
        holder.llTopRow.setClickable(false);

        // ── Retake button ──────────────────────────────────────────────────────
        holder.btnRetake.setOnClickListener(v -> listener.onChapterClick(task, true));
    }

    // ── Completed but no DB attempt record ────────────────────────────────────

    private void renderCompletedNoData(ChapterVH holder, Task task) {
        holder.tvStatus.setText("✓");
        holder.tvStatus.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.colorCorrect));
        holder.tvNumber.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.colorCorrect));

        holder.llCompletionRow.setVisibility(View.VISIBLE);
        holder.vDivider.setVisibility(View.VISIBLE);

        // No attempt data — show all dots as empty, badge as "COMPLETED"
        setDotColors(holder, 0);
        holder.tvCompletionBadge.setText("COMPLETED");
        holder.tvCompletionBadge.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.colorCorrect));

        holder.llTopRow.setOnClickListener(null);
        holder.llTopRow.setClickable(false);
        holder.btnRetake.setOnClickListener(v -> listener.onChapterClick(task, true));
    }

    // ── Available chapter (not yet completed) ─────────────────────────────────

    private void renderAvailable(ChapterVH holder, Task task, int accentColor) {
        holder.tvStatus.setText("→");
        holder.tvStatus.setTextColor(accentColor);
        holder.tvNumber.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.colorTextSecondary));

        holder.llCompletionRow.setVisibility(View.GONE);
        holder.vDivider.setVisibility(View.GONE);

        holder.llTopRow.setClickable(true);
        holder.llTopRow.setOnClickListener(v -> listener.onChapterClick(task, false));
    }

    // ── Effort dot color helper ───────────────────────────────────────────────

    /**
     * Colours the three effort dots based on effortScore (0–3).
     * Filled dots use a colour scaled to the score; empty dots are muted.
     */
    private void setDotColors(ChapterVH holder, int effortScore) {
        android.content.Context ctx = holder.itemView.getContext();

        int filledColor;
        if      (effortScore >= 3) filledColor = ContextCompat.getColor(ctx, R.color.colorCorrect);
        else if (effortScore == 2) filledColor = ContextCompat.getColor(ctx, R.color.colorTankMid);
        else                       filledColor = ContextCompat.getColor(ctx, R.color.colorTankLow);

        int emptyColor = ContextCompat.getColor(ctx, R.color.colorBorder);

        View[] dots = {holder.dot1, holder.dot2, holder.dot3};
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundTintList(
                    ColorStateList.valueOf(i < effortScore ? filledColor : emptyColor));
        }
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    private int subjectAccentColorRes(String key) {
        switch (key) {
            case "dsa":  return R.color.colorSubjectDsa;
            case "dbms": return R.color.colorSubjectDbms;
            case "web":  return R.color.colorSubjectWeb;
            case "oop":  return R.color.colorSubjectOop;
            case "dm":   return R.color.colorSubjectDm;
            default:     return R.color.colorAccent;
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ChapterVH extends RecyclerView.ViewHolder {
        View         viewAccent;
        TextView     tvNumber;
        TextView     tvName;
        TextView     tvDifficulty;
        TextView     tvStatus;
        LinearLayout llTopRow;
        View         vDivider;
        LinearLayout llCompletionRow;
        View         dot1, dot2, dot3;
        TextView     tvCompletionBadge;
        TextView     btnRetake;

        ChapterVH(@NonNull View itemView) {
            super(itemView);
            viewAccent        = itemView.findViewById(R.id.view_chapter_accent);
            tvNumber          = itemView.findViewById(R.id.tv_chapter_number);
            tvName            = itemView.findViewById(R.id.tv_chapter_name);
            tvDifficulty      = itemView.findViewById(R.id.tv_chapter_difficulty);
            tvStatus          = itemView.findViewById(R.id.tv_chapter_status);
            llTopRow          = itemView.findViewById(R.id.ll_top_row);
            vDivider          = itemView.findViewById(R.id.v_completion_divider);
            llCompletionRow   = itemView.findViewById(R.id.ll_completion_row);
            dot1              = itemView.findViewById(R.id.dot_effort_1);
            dot2              = itemView.findViewById(R.id.dot_effort_2);
            dot3              = itemView.findViewById(R.id.dot_effort_3);
            tvCompletionBadge = itemView.findViewById(R.id.tv_completion_badge);
            btnRetake         = itemView.findViewById(R.id.btn_retake);
        }
    }
}