package com.example.taskmanagerapp.ui.fragment;

import android.content.Context;
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

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Task;
import com.example.taskmanagerapp.utils.ImageStorageHelper;
import com.google.android.material.appbar.MaterialToolbar;

public class TaskDetailFragment extends Fragment {

    public interface OnNavigateToFullDetailListener {
        void onNavigateToFullDetail(Task task);
    }

    private static final String ARG_TASK = "task";
    public static final String RESULT_KEY = "task_detail_result_key";
    public static final String RESULT_TASK_ID = "result_task_id";
    public static final String RESULT_MESSAGE = "result_message";
    private Task task;
    private boolean hasSentResult = false;
    private OnNavigateToFullDetailListener navigateListener;

    public static TaskDetailFragment newInstance(Task task) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK, task);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigateToFullDetailListener) {
            navigateListener = (OnNavigateToFullDetailListener) context;
        } else {
            navigateListener = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigateListener = null;
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

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {
                sendResultAndClose();
            }
        });

        View.OnClickListener openFullDetail = v -> {
            if (navigateListener != null && task != null) {
                navigateListener.onNavigateToFullDetail(task);
            }
        };
        applyOpenDetailClick(view, openFullDetail);
        toolbar.setNavigationOnClickListener(v -> sendResultAndClose());

        return view;
    }

    /** Gắn listener mở TaskDetailActivity cho toàn bộ fragment (trừ ScrollView — tránh chặn cuộn). */
    private void applyOpenDetailClick(View view, View.OnClickListener listener) {
        if (view instanceof android.widget.ScrollView) {
            view.setOnClickListener(listener);
            ViewGroup scroll = (ViewGroup) view;
            for (int i = 0; i < scroll.getChildCount(); i++) {
                applyOpenDetailClick(scroll.getChildAt(i), listener);
            }
            return;
        }
        view.setClickable(true);
        view.setOnClickListener(listener);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyOpenDetailClick(group.getChildAt(i), listener);
            }
        }
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
