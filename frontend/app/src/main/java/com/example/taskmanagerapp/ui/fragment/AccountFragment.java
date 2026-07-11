package com.example.taskmanagerapp.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.utils.PreferenceHelper;

public class AccountFragment extends Fragment {
    private static final long ENTER_DURATION_MS = 280L;
    private static final long EXIT_DURATION_MS = 220L;
    private static final long SCRIM_DURATION_MS = 180L;

    private View rootView;
    private View scrimView;
    private View panelView;
    private ScrollView scrollView;
    private PreferenceHelper preferenceHelper;
    private float dragStartY;
    private boolean draggingDown;
    private boolean dismissing;
    private int dismissDistance;
    private int touchSlop;
    private View.OnTouchListener dragTouchListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        scrimView = view.findViewById(R.id.accountScrim);
        scrollView = view.findViewById(R.id.accountScroll);
        panelView = scrollView;
        preferenceHelper = new PreferenceHelper(requireContext());
        dismissDistance = dp(96);
        touchSlop = ViewConfiguration.get(requireContext()).getScaledTouchSlop();

        prepareEntranceAnimation();
        bindProfile();
        setupActions(view);
        setupDragToDismiss();
        setupBackHandler();
        rootView.post(this::playEntranceAnimation);
    }

    private void bindProfile() {
        TextView avatar = rootView.findViewById(R.id.tvAccountAvatar);
        TextView name = rootView.findViewById(R.id.tvAccountName);
        TextView email = rootView.findViewById(R.id.tvAccountEmail);

        avatar.setText(displayInitials());
        name.setText(displayName());
        email.setText(displayEmail());
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btnAccountBack).setOnClickListener(v -> dismissWithSlideDown());
        setComingSoon(R.id.avatarRow, "Avatar");
        setComingSoon(R.id.nicknameRow, "Nickname");
        setComingSoon(R.id.emailRow, "Email");
        setComingSoon(R.id.passkeyRow, "Passkey");
        setComingSoon(R.id.twoStepRow, "2-Step Verification");
        setComingSoon(R.id.deviceRow, "Device Management");
        setComingSoon(R.id.btnDeleteAccount, "Delete Account");
    }

    private void setupBackHandler() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                dismissWithSlideDown();
            }
        });
    }

    private void prepareEntranceAnimation() {
        if (scrimView != null) {
            scrimView.setAlpha(0f);
        }
        if (panelView != null) {
            panelView.setTranslationY(getResources().getDisplayMetrics().heightPixels);
        }
    }

    private void playEntranceAnimation() {
        if (rootView == null || panelView == null) {
            return;
        }

        panelView.setTranslationY(rootView.getHeight() + dp(32));
        panelView.animate()
                .translationY(0f)
                .setDuration(ENTER_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (scrimView != null) {
            scrimView.animate()
                    .alpha(1f)
                    .setDuration(SCRIM_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void setComingSoon(int viewId, String feature) {
        View view = rootView.findViewById(viewId);
        view.setOnClickListener(v -> Toast.makeText(requireContext(), feature + " is not implemented yet", Toast.LENGTH_SHORT).show());
    }

    private void setupDragToDismiss() {
        dragTouchListener = (v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dragStartY = event.getRawY();
                    draggingDown = false;
                    panelView.animate().cancel();
                    if (scrimView != null) {
                        scrimView.animate().cancel();
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    return handleDragMove(event);
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return finishDrag();
                default:
                    return false;
            }
        };
        attachDragListener(rootView);
    }

    private void attachDragListener(View view) {
        view.setOnTouchListener(dragTouchListener);
        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            attachDragListener(group.getChildAt(i));
        }
    }

    private boolean handleDragMove(MotionEvent event) {
        float deltaY = event.getRawY() - dragStartY;
        if (deltaY <= touchSlop) {
            return draggingDown;
        }
        if (scrollView.getScrollY() > 0 && !draggingDown) {
            return false;
        }

        draggingDown = true;
        float offset = Math.min(deltaY * 0.58f, dp(220));
        panelView.setTranslationY(offset);
        return true;
    }

    private boolean finishDrag() {
        if (!draggingDown) {
            return false;
        }

        draggingDown = false;
        if (panelView.getTranslationY() >= dismissDistance) {
            dismissWithSlideDown();
        } else {
            panelView.animate()
                    .translationY(0f)
                    .setDuration(160L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        return true;
    }

    private void dismissWithSlideDown() {
        if (dismissing) {
            return;
        }
        dismissing = true;
        if (panelView == null) {
            dismissSelf();
            return;
        }

        panelView.animate().cancel();
        if (scrimView != null) {
            scrimView.animate().cancel();
        }

        float target = rootView == null ? dp(720) : rootView.getHeight() + dp(32);
        if (scrimView != null) {
            scrimView.animate()
                    .alpha(0f)
                    .setDuration(SCRIM_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        panelView.animate()
                .translationY(target)
                .setDuration(EXIT_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(this::dismissSelf)
                .start();
    }

    private void dismissSelf() {
        getParentFragmentManager().popBackStack();
    }

    private String displayName() {
        String username = preferenceHelper.getAuthUsername();
        return username == null || username.trim().isEmpty() ? "User" : username;
    }

    private String displayEmail() {
        String email = preferenceHelper.getAuthEmail();
        return email == null || email.trim().isEmpty() ? "No email" : email;
    }

    private String displayInitials() {
        String name = displayName().trim();
        if (name.isEmpty()) {
            return "U";
        }
        String[] parts = name.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
        scrimView = null;
        panelView = null;
        scrollView = null;
    }
}
