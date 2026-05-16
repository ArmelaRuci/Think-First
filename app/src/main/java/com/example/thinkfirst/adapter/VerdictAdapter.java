package com.example.thinkfirst.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thinkfirst.R;
import com.example.thinkfirst.model.VerdictItem;
import java.util.List;

public class VerdictAdapter extends RecyclerView.Adapter<VerdictAdapter.VerdictViewHolder> {

    private final List<VerdictItem> items;

    public VerdictAdapter(List<VerdictItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VerdictViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_verdict_card, parent, false);
        return new VerdictViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VerdictViewHolder holder, int position) {
        VerdictItem item = items.get(position);

        holder.tvSentence.setText(item.getSentenceText());

        // Clear listener before clearCheck() to avoid phantom callbacks
        holder.rgVerdict.setOnCheckedChangeListener(null);
        holder.rgVerdict.clearCheck();

        // Restore previously selected verdict if returning to this card
        switch (item.getVerdict()) {
            case "wrong_fact": holder.rgVerdict.check(R.id.rb_wrong_fact); break;
            case "misleading": holder.rgVerdict.check(R.id.rb_misleading); break;
            case "fabricated": holder.rgVerdict.check(R.id.rb_fabricated); break;
        }

        // Re-attach listener after state is restored
        holder.rgVerdict.setOnCheckedChangeListener((group, checkedId) -> {
            if      (checkedId == R.id.rb_wrong_fact) item.setVerdict("wrong_fact");
            else if (checkedId == R.id.rb_misleading) item.setVerdict("misleading");
            else if (checkedId == R.id.rb_fabricated) item.setVerdict("fabricated");
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VerdictViewHolder extends RecyclerView.ViewHolder {
        TextView   tvSentence;
        RadioGroup rgVerdict;

        VerdictViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSentence = itemView.findViewById(R.id.tv_flagged_sentence);
            rgVerdict  = itemView.findViewById(R.id.rg_verdict);
        }
    }
}