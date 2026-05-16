package com.example.thinkfirst.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thinkfirst.R;
import com.example.thinkfirst.model.TaskAttemptRecord;
import java.util.List;

public class CompletedTaskAdapter
        extends RecyclerView.Adapter<CompletedTaskAdapter.CompletedTaskVH> {

    public interface OnRetakeClickListener {
        void onRetakeClick(TaskAttemptRecord record);
    }

    private final List<TaskAttemptRecord> records;
    private final OnRetakeClickListener   listener;
    private final int                     subjectColorRes;

    public CompletedTaskAdapter(List<TaskAttemptRecord> records,
                                OnRetakeClickListener listener,
                                int subjectColorRes) {
        this.records         = records;
        this.listener        = listener;
        this.subjectColorRes = subjectColorRes;
    }

    @NonNull
    @Override
    public CompletedTaskVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_completed_task_card, parent, false);
        return new CompletedTaskVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompletedTaskVH holder, int position) {
        TaskAttemptRecord record = records.get(position);

        // ── Chapter name (primary display label) ──────────────────────────────
        String chapterName = record.getChapter();
        holder.tvChapterName.setText(
                chapterName != null && !chapterName.isEmpty()
                        ? chapterName
                        : record.getQuestion());

        // ── Accent bar color ──────────────────────────────────────────────────
        int accentColor = ContextCompat.getColor(
                holder.itemView.getContext(), subjectColorRes);
        holder.viewAccent.setBackgroundColor(accentColor);

        // ── Arrow indicator — tinted to accent color ──────────────────────────
        holder.tvArrow.setTextColor(accentColor);

        // ── Retake symbol — tinted to accent color ────────────────────────────
        holder.tvRetake.setTextColor(accentColor);
        holder.tvRetake.setOnClickListener(v -> listener.onRetakeClick(record));

        // ── Effort dots ───────────────────────────────────────────────────────
        int effortScore = record.getEffortScore();
        View[] dots     = {holder.dot1, holder.dot2, holder.dot3};
        int filledColor = effortDotColor(effortScore, holder.itemView.getContext());
        int emptyColor  = ContextCompat.getColor(
                holder.itemView.getContext(), R.color.colorBorder);

        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundTintList(ColorStateList.valueOf(
                    i < effortScore ? filledColor : emptyColor));
        }

        // ── Independent / hint badge ──────────────────────────────────────────
        if (record.wasIndependent()) {
            holder.tvHintBadge.setText("INDEPENDENT");
            holder.tvHintBadge.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.colorCorrect));
        } else {
            holder.tvHintBadge.setText("HINT TIER " + record.getHintTierUsed());
            holder.tvHintBadge.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(),
                    effortScore >= 2 ? R.color.colorTankMid : R.color.colorTankLow));
        }

        // ── Staggered entry animation ─────────────────────────────────────────
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationX(20f);
        holder.itemView.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(position * 60L)
                .setDuration(260)
                .start();
    }

    @Override
    public int getItemCount() { return records.size(); }

    private int effortDotColor(int score, android.content.Context ctx) {
        if (score >= 3) return ContextCompat.getColor(ctx, R.color.colorCorrect);
        if (score == 2) return ContextCompat.getColor(ctx, R.color.colorTankMid);
        return ContextCompat.getColor(ctx, R.color.colorTankLow);
    }

    static class CompletedTaskVH extends RecyclerView.ViewHolder {
        View     viewAccent;
        TextView tvChapterName;
        View     dot1, dot2, dot3;
        TextView tvHintBadge;
        TextView tvArrow;
        TextView tvRetake;

        CompletedTaskVH(@NonNull View itemView) {
            super(itemView);
            viewAccent   = itemView.findViewById(R.id.view_card_accent);
            tvChapterName = itemView.findViewById(R.id.tv_task_question_preview);
            dot1         = itemView.findViewById(R.id.dot_effort_1);
            dot2         = itemView.findViewById(R.id.dot_effort_2);
            dot3         = itemView.findViewById(R.id.dot_effort_3);
            tvHintBadge  = itemView.findViewById(R.id.tv_hint_badge);
            tvArrow      = itemView.findViewById(R.id.tv_arrow_indicator);
            tvRetake     = itemView.findViewById(R.id.btn_retake);
        }
    }
}