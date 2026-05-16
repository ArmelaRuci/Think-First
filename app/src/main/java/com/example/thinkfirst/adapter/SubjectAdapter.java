package com.example.thinkfirst.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thinkfirst.R;
import com.example.thinkfirst.model.Subject;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {

    public interface OnSubjectClickListener {
        void onSubjectClick(Subject subject);
    }

    private final List<Subject>          subjects;
    private final OnSubjectClickListener listener;

    public SubjectAdapter(List<Subject> subjects, OnSubjectClickListener listener) {
        this.subjects = subjects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject_card, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        Subject subject = subjects.get(position);
        int     accentColor = ContextCompat.getColor(
                holder.itemView.getContext(), subject.getColorResId());

        // ── Text fields ───────────────────────────────────────────────────────
        holder.tvName.setText(subject.getDisplayName());
        holder.tvDescription.setText(subject.getDescription());

        int completed = subject.getTasksCompleted();
        holder.tvBadge.setText(completed == 0 ? "start" : completed + " / 5");
        holder.tvBadge.setTextColor(completed == 0
                ? ContextCompat.getColor(holder.itemView.getContext(),
                R.color.colorTextSecondary)
                : accentColor);

        // ── Accent bar color ──────────────────────────────────────────────────
        holder.viewAccentBar.setBackgroundColor(accentColor);

        // ── Subject icon — tinted to subject accent color ─────────────────────
        holder.ivIcon.setImageResource(subject.getIconResId());
        holder.ivIcon.setImageTintList(ColorStateList.valueOf(accentColor));

        // ── Staggered slide-up entry animation ────────────────────────────────
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(24f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(position * 70L)
                .setDuration(320)
                .start();

        holder.itemView.setOnClickListener(v -> listener.onSubjectClick(subject));
    }

    @Override
    public int getItemCount() { return subjects.size(); }

    static class SubjectViewHolder extends RecyclerView.ViewHolder {
        TextView  tvName;
        TextView  tvDescription;
        TextView  tvBadge;
        View      viewAccentBar;
        ImageView ivIcon;

        SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName        = itemView.findViewById(R.id.tv_subject_name);
            tvDescription = itemView.findViewById(R.id.tv_subject_description);
            tvBadge       = itemView.findViewById(R.id.tv_progress_badge);
            viewAccentBar = itemView.findViewById(R.id.view_accent_bar);
            ivIcon        = itemView.findViewById(R.id.iv_subject_icon);
        }
    }
}