package com.example.thinkfirst;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.List;

public class AnswerRevealSheet extends BottomSheetDialogFragment {

    // ── Argument keys ─────────────────────────────────────────────────────────
    private static final String ARG_QUESTION     = "arg_question";
    private static final String ARG_KEY_CONCEPTS = "arg_key_concepts";
    private static final String ARG_HINT_USED    = "arg_hint_tier_used";
    private static final String ARG_TANK_CHANGE  = "arg_tank_change";
    private static final String ARG_TASK_ID      = "arg_task_id";
    private static final String ARG_YOUR_ANSWER  = "arg_your_answer";
    private static final String ARG_SUBJECT      = "arg_subject";
    private static final String ARG_MISSING_KEYS = "arg_missing_keys";

    // ── Dismissal intent ──────────────────────────────────────────────────────
    private enum DismissReason { NORMAL, MIRROR }
    private DismissReason pendingReason = DismissReason.NORMAL;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    public interface OnDismissListener       { void onRevealDismissed(); }
    public interface OnCompareWithAiListener { void onCompareWithAi(); }

    private OnDismissListener       dismissListener;
    private OnCompareWithAiListener compareListener;

    public void setOnDismissListener(OnDismissListener l)            { dismissListener = l; }
    public void setOnCompareWithAiListener(OnCompareWithAiListener l){ compareListener  = l; }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static AnswerRevealSheet newInstance(
            String question,
            List<String> keyConcepts,
            int hintTierUsed,
            float tankChange,
            String taskId,
            String yourAnswer,
            String subject,
            List<String> missingConcepts) {

        AnswerRevealSheet sheet = new AnswerRevealSheet();
        Bundle args = new Bundle();
        args.putString(ARG_QUESTION,     question);
        args.putStringArrayList(ARG_KEY_CONCEPTS,
                keyConcepts != null ? new ArrayList<>(keyConcepts) : new ArrayList<>());
        args.putInt(ARG_HINT_USED,       hintTierUsed);
        args.putFloat(ARG_TANK_CHANGE,   tankChange);
        args.putString(ARG_TASK_ID,      taskId     != null ? taskId     : "");
        args.putString(ARG_YOUR_ANSWER,  yourAnswer != null ? yourAnswer : "");
        args.putString(ARG_SUBJECT,      subject    != null ? subject    : "");
        args.putStringArrayList(ARG_MISSING_KEYS,
                missingConcepts != null
                        ? new ArrayList<>(missingConcepts)
                        : new ArrayList<>());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_answer_reveal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Expand fully
        BottomSheetDialog dialog = (BottomSheetDialog) requireDialog();
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        // ── Receive all args ──────────────────────────────────────────────────
        String            question   = getArguments().getString(ARG_QUESTION, "");
        ArrayList<String> concepts   = getArguments().getStringArrayList(ARG_KEY_CONCEPTS);
        int               tierUsed   = getArguments().getInt(ARG_HINT_USED, 0);
        float             tankChange = getArguments().getFloat(ARG_TANK_CHANGE, 0f);
        String            yourAnswer = getArguments().getString(ARG_YOUR_ANSWER, "");

        // ── Bind views ────────────────────────────────────────────────────────
        TextView     tvQuestion   = view.findViewById(R.id.tv_reveal_question);
        LinearLayout llConcepts   = view.findViewById(R.id.ll_key_concepts);
        TextView     tvHintLabel  = view.findViewById(R.id.tv_reveal_hint_label);
        TextView     tvTankChange = view.findViewById(R.id.tv_reveal_tank_change);
        Button       btnClose     = view.findViewById(R.id.btn_reveal_close);
        Button       btnCompareAi = view.findViewById(R.id.btn_compare_ai);

        tvQuestion.setText(question);

        // ── Bullet concepts ───────────────────────────────────────────────────
        llConcepts.removeAllViews();
        if (concepts != null && !concepts.isEmpty()) {
            float density = requireContext().getResources()
                    .getDisplayMetrics().density;
            for (String concept : concepts) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rp.bottomMargin = (int)(6 * density);
                row.setLayoutParams(rp);

                TextView tvDot = new TextView(requireContext());
                tvDot.setText("•");
                tvDot.setTextColor(requireContext().getColor(R.color.colorAccent));
                tvDot.setTextSize(14f);
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                dp.setMarginEnd((int)(8 * density));
                tvDot.setLayoutParams(dp);

                TextView tvText = new TextView(requireContext());
                tvText.setText(concept);
                tvText.setTextColor(requireContext().getColor(R.color.colorTextPrimary));
                tvText.setTextSize(13f);
                tvText.setLineSpacing(0f, 1.35f);
                tvText.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                row.addView(tvDot);
                row.addView(tvText);
                llConcepts.addView(row);
            }
        }

        // ── Contextual label ──────────────────────────────────────────────────
        String contextMsg;
        int    contextColor;
        switch (tierUsed) {
            case 0:
                contextMsg   = "Submitted independently — great work.";
                contextColor = R.color.colorCorrect;
                break;
            case 1:
                contextMsg   = "Used a nudge. Compare your answer above.";
                contextColor = R.color.colorTankFull;
                break;
            case 2:
                contextMsg   = "Used a partial hint. See what you missed.";
                contextColor = R.color.colorTankMid;
                break;
            default:
                contextMsg   = "Used full guidance. Study these concepts.";
                contextColor = R.color.colorTankLow;
                break;
        }
        tvHintLabel.setText(contextMsg);
        tvHintLabel.setTextColor(requireContext().getColor(contextColor));

        // ── Tank change ───────────────────────────────────────────────────────
        String tankText;
        int    tankColor;
        if (tankChange > 0) {
            tankText  = String.format("Tank +%.0f%% — independent submission", tankChange * 100);
            tankColor = R.color.colorCorrect;
        } else if (tankChange < 0) {
            tankText  = String.format("Tank %.0f%% — hint cost applied earlier", tankChange * 100);
            tankColor = R.color.colorTankLow;
        } else {
            tankText  = "Tank unchanged — hint cost already applied";
            tankColor = R.color.colorTextSecondary;
        }
        tvTankChange.setText(tankText);
        tvTankChange.setTextColor(requireContext().getColor(tankColor));

        // ── Got it ────────────────────────────────────────────────────────────
        btnClose.setOnClickListener(v -> {
            pendingReason = DismissReason.NORMAL;
            dismiss();
        });

        // ── Compare with AI — independent only ───────────────────────────────
        if (tierUsed == 0) {
            btnCompareAi.setVisibility(View.VISIBLE);
            btnCompareAi.setOnClickListener(v -> {
                pendingReason = DismissReason.MIRROR;
                dismiss();
                if (compareListener != null) compareListener.onCompareWithAi();
            });
        } else {
            btnCompareAi.setVisibility(View.GONE);
        }

        // ── Back press = Got it ───────────────────────────────────────────────
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        pendingReason = DismissReason.NORMAL;
                        dismiss();
                    }
                });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (pendingReason == DismissReason.NORMAL && dismissListener != null) {
            dismissListener.onRevealDismissed();
        }
    }
}