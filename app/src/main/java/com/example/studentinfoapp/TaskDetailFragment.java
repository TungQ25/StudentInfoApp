package com.example.studentinfoapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

public class TaskDetailFragment extends Fragment {

    private static final String ARG_TASK = "task";
    public static final String RESULT_KEY = "task_detail_result_key";
    public static final String RESULT_TASK_ID = "result_task_id";
    public static final String RESULT_MESSAGE = "result_message";
    private Task task;
    private boolean hasSentResult = false;

    public static TaskDetailFragment newInstance(Task task) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK, task);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            task = (Task) getArguments().getSerializable(ARG_TASK);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_detail, container, false);

        MaterialToolbar toolbar = view.findViewById(R.id.detailToolbar);
        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvDescription = view.findViewById(R.id.tvDetailDescription);
        TextView tvCategory = view.findViewById(R.id.tvDetailCategory);
        TextView tvDeadline = view.findViewById(R.id.tvDetailDeadline);
        TextView tvPriority = view.findViewById(R.id.tvDetailPriority);
        TextView tvStatus = view.findViewById(R.id.tvDetailStatus);
        ImageView ivAttachment = view.findViewById(R.id.ivDetailAttachment);

        if (task != null) {
            tvTitle.setText(task.getTitle());
            
            // Kiểm tra description, nếu trống thì ẩn TextView đi
            if (TextUtils.isEmpty(task.getDescription())) {
                tvDescription.setVisibility(View.GONE);
            } else {
                tvDescription.setVisibility(View.VISIBLE);
                tvDescription.setText(task.getDescription());
            }

            tvCategory.setText(task.getCategory());
            tvDeadline.setText(task.getDeadline());
            tvPriority.setText(task.getPriority());
            tvStatus.setText(task.isCompleted() ? "Completed" : "Pending");

            String img = task.getImagePath();
            if (img != null && !img.isEmpty()) {
                ImageStorageHelper storage = new ImageStorageHelper(requireContext());
                Bitmap bm = storage.loadBitmapForView(img, 1024, 1024);
                if (bm != null) {
                    ivAttachment.setImageBitmap(bm);
                    ivAttachment.setVisibility(View.VISIBLE);
                }
            }
        }

        toolbar.setNavigationOnClickListener(v -> sendResultAndClose());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {
                sendResultAndClose();
            }
        });

        return view;
    }

    private void sendResultAndClose() {
        if (!hasSentResult) {
            Bundle result = new Bundle();
            if (task != null) {
                result.putString(RESULT_TASK_ID, task.getId());
                result.putString(RESULT_MESSAGE, "Đã xem: " + task.getTitle());
            } else {
                result.putString(RESULT_MESSAGE, "Đóng màn hình chi tiết");
            }
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            hasSentResult = true;
        }
        getParentFragmentManager().popBackStack();
    }
}
