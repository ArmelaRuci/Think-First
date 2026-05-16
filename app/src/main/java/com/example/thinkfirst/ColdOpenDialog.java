package com.example.thinkfirst;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ColdOpenDialog extends DialogFragment {

    private static final String ARG_SUBJECT  = "co_subject";
    private static final String ARG_QUESTION = "co_question";
    private static final String ARG_TASK_ID  = "co_task_id";

    /** Called when the student taps Continue or Skip — proceed to ChapterSelect */
    public interface OnColdOpenCompleteListener {
        void onComplete(String subject);
    }

    private OnColdOpenCompleteListener listener;

    public static ColdOpenDialog newInstance(String subject,
                                             String question,
                                             String taskId) {
        ColdOpenDialog d = new ColdOpenDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SUBJECT,  subject);
        args.putString(ARG_QUESTION, question);
        args.putString(ARG_TASK_ID,  taskId);
        d.setArguments(args);
        return d;
    }

    public void setOnCompleteListener(OnColdOpenCompleteListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_cold_open, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String subject  = getArguments().getString(ARG_SUBJECT,  "");
        String question = getArguments().getString(ARG_QUESTION, "");
        String taskId   = getArguments().getString(ARG_TASK_ID,  "");

        TextView tvQuestion = view.findViewById(R.id.tv_cold_open_question);
        EditText etAnswer   = view.findViewById(R.id.et_cold_open_answer);
        Button   btnCont    = view.findViewById(R.id.btn_cold_open_continue);
        Button   btnSkip    = view.findViewById(R.id.btn_cold_open_skip);

        tvQuestion.setText(question);

        // Cannot dismiss by tapping outside — student must make a choice
        setCancelable(false);

        btnCont.setOnClickListener(v -> {
            String answer = etAnswer.getText().toString().trim();

            // Save recall attempt to DB — even empty answers are saved
            // The act of trying to retrieve matters more than the answer
            if (!taskId.isEmpty()) {
                DatabaseHelper.getInstance(requireContext())
                        .saveRecallAttempt(subject, taskId, answer);
            }

            dismiss();
            if (listener != null) listener.onComplete(subject);
        });

        btnSkip.setOnClickListener(v -> {
            // Skip saves nothing — no penalty, no judgment
            dismiss();
            if (listener != null) listener.onComplete(subject);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make dialog fill most of the screen width
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9f);
            dialog.getWindow().setLayout(width,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent);
        }
    }
}