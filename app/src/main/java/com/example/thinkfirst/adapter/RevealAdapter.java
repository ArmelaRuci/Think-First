package com.example.thinkfirst.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thinkfirst.R;
import com.example.thinkfirst.model.RevealItem;
import java.util.List;

public class RevealAdapter extends RecyclerView.Adapter<RevealAdapter.RevealViewHolder> {

    private final List<RevealItem> items;

    public RevealAdapter(List<RevealItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public RevealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reveal_card, parent, false);
        return new RevealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RevealViewHolder holder, int position) {
        RevealItem item    = items.get(position);
        Context    context = holder.itemView.getContext();

        int    bgColor;
        int    accentColor;
        String labelText;

        switch (item.getType()) {
            case "correct":
                bgColor     = context.getColor(R.color.colorHintTier3Bg);
                accentColor = context.getColor(R.color.colorCorrect);
                labelText   = "CORRECT CATCH";
                break;
            case "missed":
                bgColor     = context.getColor(R.color.colorSurface);
                accentColor = context.getColor(R.color.colorWrong);
                labelText   = "YOU MISSED THIS";
                break;
            default: // false_flag
                bgColor     = context.getColor(R.color.colorHintTier2Bg);
                accentColor = context.getColor(R.color.colorWarning);
                labelText   = "FALSE FLAG";
                break;
        }

        holder.itemView.setBackgroundColor(bgColor);
        holder.tvResultLabel.setText(labelText);
        holder.tvResultLabel.setTextColor(accentColor);

        // Points display
        int    pts     = item.getPoints();
        String ptsText = pts >= 0 ? "+" + pts : String.valueOf(pts);
        holder.tvPoints.setText(ptsText);
        holder.tvPoints.setTextColor(accentColor);

        // Main text
        holder.tvRevealText.setText(item.getDisplayText());

        // Full explanation = correction + explanation
        String fullExplanation;
        if (item.getCorrectVersion() == null || item.getCorrectVersion().isEmpty()) {
            fullExplanation = item.getExplanation();
        } else {
            fullExplanation = "Correct: " + item.getCorrectVersion()
                    + "\n\n" + item.getExplanation();
        }
        holder.tvExplanation.setText(fullExplanation);

        // Staggered fade-in animation
        holder.itemView.setAlpha(0f);
        holder.itemView.animate()
                .alpha(1f)
                .setStartDelay(position * 180L)
                .setDuration(400)
                .start();
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class RevealViewHolder extends RecyclerView.ViewHolder {
        TextView tvResultLabel;
        TextView tvPoints;
        TextView tvRevealText;
        TextView tvExplanation;

        RevealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvResultLabel = itemView.findViewById(R.id.tv_result_label);
            tvPoints      = itemView.findViewById(R.id.tv_points);
            tvRevealText  = itemView.findViewById(R.id.tv_reveal_text);
            tvExplanation = itemView.findViewById(R.id.tv_reveal_explanation);
        }
    }
}