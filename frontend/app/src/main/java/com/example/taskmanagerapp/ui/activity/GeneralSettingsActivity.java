package com.example.taskmanagerapp.ui.activity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.taskmanagerapp.R;

public class GeneralSettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(color(R.color.colorBackground));
        root.addView(scrollPage(createContent()), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(root);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private LinearLayout createContent() {
        LinearLayout content = pageContent();
        content.addView(header());

        LinearLayout topGroup = cardContainer();
        topGroup.addView(settingsRow("Language", "English", v -> notImplemented("Language")));
        topGroup.addView(divider());
        topGroup.addView(settingsRow("Swipe Actions", null, v -> notImplemented("Swipe Actions")));
        topGroup.addView(divider());
        topGroup.addView(taskContextMenuRow());
        content.addView(topGroup);
        content.addView(space(16));

        LinearLayout detailGroup = cardContainer();
        detailGroup.addView(settingsRow("Task Detail Page", null, v -> notImplemented("Task Detail Page")));
        content.addView(detailGroup);
        content.addView(space(16));

        LinearLayout taskGroup = cardContainer();
        taskGroup.addView(settingsRow("Smart Recognition", null, v -> notImplemented("Smart Recognition")));
        taskGroup.addView(divider());
        taskGroup.addView(settingsRow("Task Quick Add", null, v -> notImplemented("Task Quick Add")));
        taskGroup.addView(divider());
        taskGroup.addView(settingsRow("Task Default", null, v -> notImplemented("Task Default")));
        taskGroup.addView(divider());
        taskGroup.addView(settingsRow("Upload/Download Attachments", null, v -> notImplemented("Attachments")));
        taskGroup.addView(divider());
        taskGroup.addView(settingsRow("Collaborate", null, v -> notImplemented("Collaborate")));
        taskGroup.addView(divider());
        taskGroup.addView(settingsRow("Manage Template", null, v -> notImplemented("Manage Template")));
        content.addView(taskGroup);
        content.addView(space(16));

        LinearLayout passcodeGroup = cardContainer();
        passcodeGroup.addView(settingsRow("Passcode & Face ID", null, v -> notImplemented("Passcode & Face ID")));
        content.addView(passcodeGroup);
        content.addView(space(24));
        return content;
    }

    private View header() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(18));

        TextView back = circularButton("<", 19, color(R.color.colorOnSurface), color(R.color.colorSurfaceVariant));
        back.setOnClickListener(v -> finish());
        row.addView(back);

        TextView title = label("General", 18, color(R.color.colorOnSurface), true);
        title.setGravity(Gravity.CENTER);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView spacer = circularButton("", 13, color(R.color.colorOnSurface), Color.TRANSPARENT);
        row.addView(spacer);
        return row;
    }

    private View taskContextMenuRow() {
        LinearLayout row = baseRow();
        row.setPadding(dp(18), dp(14), dp(14), dp(14));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        texts.addView(label("Task Context Menu", 16, color(R.color.colorOnSurface), false));
        TextView summary = label("Long press task and habit to show context menu.", 13, color(R.color.colorOnSurfaceVariant), false);
        summary.setPadding(0, dp(6), dp(12), 0);
        texts.addView(summary);

        Switch toggle = new Switch(this);
        toggle.setChecked(true);
        tintSwitch(toggle);
        toggle.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                Toast.makeText(this, isChecked ? "Context menu enabled" : "Context menu disabled", Toast.LENGTH_SHORT).show());
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        row.addView(toggle);
        return row;
    }

    private View settingsRow(String title, String value, View.OnClickListener listener) {
        LinearLayout row = baseRow();
        row.setOnClickListener(listener);

        TextView titleView = label(title, 16, color(R.color.colorOnSurface), false);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (value != null) {
            TextView valueView = label(value, 14, color(R.color.colorOnSurfaceVariant), false);
            valueView.setGravity(Gravity.END);
            valueView.setSingleLine(true);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f);
            valueParams.setMargins(dp(8), 0, 0, 0);
            row.addView(valueView, valueParams);
        }

        row.addView(arrow());
        return row;
    }

    private LinearLayout baseRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(18), dp(12), dp(14), dp(12));
        row.setMinimumHeight(dp(58));
        row.setClickable(true);
        row.setFocusable(true);
        return row;
    }

    private LinearLayout cardContainer() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_settings_card);
        card.setClipToOutline(true);
        return card;
    }

    private TextView arrow() {
        TextView arrow = label(">", 20, color(R.color.colorOnSurfaceVariant), false);
        arrow.setGravity(Gravity.CENTER);
        arrow.setPadding(dp(10), 0, 0, 0);
        return arrow;
    }

    private TextView circularButton(String text, int textSize, int textColor, int fillColor) {
        TextView button = label(text, textSize, textColor, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundedDrawable(fillColor, dp(20)));
        button.setMinWidth(dp(40));
        button.setMinHeight(dp(40));
        return button;
    }

    private TextView label(String text, int sp, int textColor, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(textColor);
        view.setIncludeFontPadding(false);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private LinearLayout pageContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(16), dp(14), dp(18));
        return content;
    }

    private ScrollView scrollPage(LinearLayout content) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setClipToPadding(false);
        scrollView.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    private View divider() {
        View divider = new View(this);
        divider.setBackgroundColor(color(R.color.colorDivider));
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        return divider;
    }

    private Space space(int dp) {
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return space;
    }

    private GradientDrawable roundedDrawable(int fillColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private void tintSwitch(Switch toggle) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        toggle.setThumbTintList(new ColorStateList(states, new int[]{
                color(R.color.colorOnPrimary),
                color(R.color.colorOnSurfaceVariant)
        }));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{
                color(R.color.colorAddAction),
                color(R.color.colorSurfaceVariant)
        }));
    }

    private void notImplemented(String feature) {
        Toast.makeText(this, feature + " is not implemented yet", Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int color(int colorRes) {
        return getColor(colorRes);
    }
}
