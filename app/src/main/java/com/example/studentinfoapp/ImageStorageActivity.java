package com.example.studentinfoapp;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

/**
 * Activity demo Internal Storage cho ảnh, gồm 3 nhiệm vụ:
 *  1) Tạo Bitmap màu đỏ 500x500, lưu, nạp lại, kiểm tra file size.
 *  2) Pick ảnh từ Gallery, lưu vào Internal Storage với tên
 *     {@code task_[timestamp].jpg}, hiển thị preview, có nút DELETE.
 *  3) So sánh inSampleSize=1 vs inSampleSize=4 với 1 ảnh gốc 2000x2000:
 *     ghi nhận file size + memory usage và tính % tiết kiệm.
 */
public class ImageStorageActivity extends AppCompatActivity {

    private static final String TAG = "ImageStorageActivity";

    private static final String RED_BITMAP_FILE = "test_red_500.jpg";
    private static final String SAMPLE_BITMAP_FILE = "test_sample_2000.jpg";

    private ImageStorageHelper storage;

    // Section 1
    private ImageView ivRedBitmap;
    private TextView tvRedBitmapInfo;

    // Section 2
    private ImageView ivPicked;
    private TextView tvPickedInfo;
    private Button btnDeleteImage;
    private String currentPickedFilename = null;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;

    // Section 3
    private ImageView ivSample1;
    private ImageView ivSample4;
    private TextView tvSampleSizeResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_storage);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        storage = new ImageStorageHelper(this);

        bindSection1();
        bindSection2();
        bindSection3();
    }

    // =========================================================================
    // TASK 1: Red bitmap 500x500
    // =========================================================================
    private void bindSection1() {
        Button btn = findViewById(R.id.btnRunRedBitmapTest);
        ivRedBitmap = findViewById(R.id.ivRedBitmap);
        tvRedBitmapInfo = findViewById(R.id.tvRedBitmapInfo);

        btn.setOnClickListener(v -> runRedBitmapTest());
    }

    private void runRedBitmapTest() {
        Bitmap red = ImageStorageHelper.createRedBitmap(500, 500);
        long ramSrc = red.getAllocationByteCount();

        java.io.File saved = storage.saveBitmap(red, RED_BITMAP_FILE);
        if (saved == null) {
            toast("Saved failed");
            return;
        }
        long fileSize = storage.getFileSize(RED_BITMAP_FILE);

        Bitmap loaded = storage.loadBitmap(RED_BITMAP_FILE);
        if (loaded == null) {
            toast("Load failed");
            return;
        }
        long ramLoaded = loaded.getAllocationByteCount();

        ivRedBitmap.setImageBitmap(loaded);

        String info = String.format(Locale.US,
                "Path : %s\n" +
                        "Size : %d x %d\n" +
                        "File : %s\n" +
                        "RAM (src) : %s\n" +
                        "RAM (loaded) : %s",
                saved.getAbsolutePath(),
                loaded.getWidth(), loaded.getHeight(),
                ImageStorageHelper.formatSize(fileSize),
                ImageStorageHelper.formatSize(ramSrc),
                ImageStorageHelper.formatSize(ramLoaded));
        tvRedBitmapInfo.setText(info);
        Log.d(TAG, "Red bitmap test:\n" + info);
    }

    // =========================================================================
    // TASK 2: Pick from Gallery -> save -> preview -> delete
    // =========================================================================
    private void bindSection2() {
        Button btnPick = findViewById(R.id.btnPickImage);
        btnDeleteImage = findViewById(R.id.btnDeleteImage);
        ivPicked = findViewById(R.id.ivPicked);
        tvPickedInfo = findViewById(R.id.tvPickedInfo);

        // Photo Picker (m\u1ed9t API m\u1edbi, kh\u00f4ng c\u1ea7n READ_EXTERNAL_STORAGE permission)
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                this::onPickedImage);

        btnPick.setOnClickListener(v -> pickImageLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        btnDeleteImage.setOnClickListener(v -> deleteCurrentImage());
    }

    private void onPickedImage(Uri uri) {
        if (uri == null) {
            Log.d(TAG, "User huy chon anh");
            return;
        }
        String filename = "task_" + System.currentTimeMillis() + ".jpg";
        java.io.File saved = storage.saveFromUri(uri, filename);
        if (saved == null) {
            toast("Save failed");
            return;
        }

        currentPickedFilename = filename;
        Bitmap loaded = storage.loadBitmap(filename);
        if (loaded != null) {
            ivPicked.setImageBitmap(loaded);
        }

        long fileSize = storage.getFileSize(filename);
        String info = String.format(Locale.US,
                "Filename : %s\nPath : %s\nFile size : %s%s",
                filename,
                saved.getAbsolutePath(),
                ImageStorageHelper.formatSize(fileSize),
                loaded != null
                        ? "\nDimensions : " + loaded.getWidth() + " x " + loaded.getHeight()
                        + "\nRAM : " + ImageStorageHelper.formatSize(loaded.getAllocationByteCount())
                        : "");
        tvPickedInfo.setText(info);
        btnDeleteImage.setEnabled(true);
        Log.d(TAG, "Pick image:\n" + info);
    }

    private void deleteCurrentImage() {
        if (currentPickedFilename == null) return;
        boolean ok = storage.deleteImage(currentPickedFilename);
        if (ok) {
            ivPicked.setImageDrawable(null);
            tvPickedInfo.setText(getString(R.string.img_deleted_fmt, currentPickedFilename));
            currentPickedFilename = null;
            btnDeleteImage.setEnabled(false);
            toast("Deleted");
        } else {
            toast("Delete failed");
        }
    }

    // =========================================================================
    // TASK 3: inSampleSize = 1 vs 4
    // =========================================================================
    private void bindSection3() {
        Button btn = findViewById(R.id.btnRunSampleSizeTest);
        ivSample1 = findViewById(R.id.ivSample1);
        ivSample4 = findViewById(R.id.ivSample4);
        tvSampleSizeResult = findViewById(R.id.tvSampleSizeResult);

        btn.setOnClickListener(v -> runSampleSizeTest());
    }

    private void runSampleSizeTest() {
        // 1) Sinh ảnh gốc 2000x2000 (gradient + checker để có detail thật)
        Bitmap original = ImageStorageHelper.createSampleBitmap(2000, 2000);
        long ramOriginal = original.getAllocationByteCount();

        // 2) Lưu xuống Internal Storage 1 lần duy nhất (file giống nhau với cả 2 case)
        java.io.File saved = storage.saveBitmap(original, SAMPLE_BITMAP_FILE);
        if (saved == null) {
            toast("Save failed");
            return;
        }
        long fileSize = storage.getFileSize(SAMPLE_BITMAP_FILE);
        original.recycle();

        // 3) Nạp lại với inSampleSize = 1 và = 4
        Bitmap b1 = storage.loadBitmap(SAMPLE_BITMAP_FILE, 1);
        Bitmap b4 = storage.loadBitmap(SAMPLE_BITMAP_FILE, 4);
        if (b1 == null || b4 == null) {
            toast("Load failed");
            return;
        }
        long ram1 = b1.getAllocationByteCount();
        long ram4 = b4.getAllocationByteCount();

        ivSample1.setImageBitmap(b1);
        ivSample4.setImageBitmap(b4);

        double saved4 = ram1 == 0 ? 0 : (1.0 - (double) ram4 / ram1) * 100.0;

        String result = String.format(Locale.US,
                "File on disk : %s  (cùng một file)\n" +
                        "RAM (goc 2000x2000) : %s\n" +
                        "\n" +
                        "inSampleSize = 1\n" +
                        "  Dimensions : %d x %d\n" +
                        "  RAM        : %s\n" +
                        "\n" +
                        "inSampleSize = 4\n" +
                        "  Dimensions : %d x %d\n" +
                        "  RAM        : %s\n" +
                        "\n" +
                        "=> Tiết kiệm RAM : %.2f %%\n" +
                        "(file trên disk không đổi vi inSampleSize chỉ ảnh hưởng khi DECODE)",
                ImageStorageHelper.formatSize(fileSize),
                ImageStorageHelper.formatSize(ramOriginal),
                b1.getWidth(), b1.getHeight(), ImageStorageHelper.formatSize(ram1),
                b4.getWidth(), b4.getHeight(), ImageStorageHelper.formatSize(ram4),
                saved4);

        tvSampleSizeResult.setText(result);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
