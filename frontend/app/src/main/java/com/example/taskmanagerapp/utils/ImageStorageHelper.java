package com.example.taskmanagerapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper cho lưu trữ ảnh.
 * - Thư mục mặc định: {@code context.getFilesDir()/images/}.
 * - Cung cấp chức năng save / load / load with inSampleSize / delete / get size.
 */
public class ImageStorageHelper {

    private static final String TAG = "ImageStorageHelper";
    private static final String IMAGE_DIR = "images";
    private static final int DEFAULT_QUALITY = 90;

    private final Context context;

    public ImageStorageHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Thư mục lưu ảnh trong Internal Storage. Tự tạo nếu chưa có. */
    public File getImageDir() {
        File dir = new File(context.getFilesDir(), IMAGE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Không tạo được thư mục " + dir.getAbsolutePath());
        }
        return dir;
    }

    public File getFile(String filename) {
        return new File(getImageDir(), filename);
    }

    /**
     * Lưu Bitmap dưới dạng JPEG vào Internal Storage.
     *
     * @return File đã được ghi, hoặc {@code null} nếu lỗi.
     */
    public File saveBitmap(Bitmap bitmap, String filename) {
        return saveBitmap(bitmap, filename, Bitmap.CompressFormat.JPEG, DEFAULT_QUALITY);
    }

    public File saveBitmap(Bitmap bitmap, String filename,
                           Bitmap.CompressFormat format, int quality) {
        if (bitmap == null || filename == null) return null;
        File file = getFile(filename);
        try (OutputStream os = new FileOutputStream(file)) {
            bitmap.compress(format, quality, os);
            os.flush();
            Log.d(TAG, "saveBitmap -> " + file.getAbsolutePath()
                    + " (" + file.length() + " bytes)");
            return file;
        } catch (IOException e) {
            Log.e(TAG, "saveBitmap lỗi: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Lưu 1 InputStream (ví dụ từ Uri từ Gallery) vào Internal Storage như là.
     * Sử dụng cho Task 2 để giữ nguyên chất lượng ảnh gốc.
     */
    public File saveStream(InputStream input, String filename) {
        if (input == null || filename == null) return null;
        File file = getFile(filename);
        try (OutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[8 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
            Log.d(TAG, "saveStream -> " + file.getAbsolutePath()
                    + " (" + file.length() + " bytes)");
            return file;
        } catch (IOException e) {
            Log.e(TAG, "saveStream lỗi: " + e.getMessage(), e);
            return null;
        }
    }

    /** Sao chép nội dung từ 1 Uri (content://) vào Internal Storage. */
    public File saveFromUri(Uri uri, String filename) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            return saveStream(is, filename);
        } catch (IOException e) {
            Log.e(TAG, "saveFromUri lỗi: " + e.getMessage(), e);
            return null;
        }
    }

    /** Nạp Bitmap từ Internal Storage. */
    public Bitmap loadBitmap(String filename) {
        return loadBitmap(filename, 1);
    }

    /**
     * Nạp Bitmap với {@code inSampleSize}. Giá trị 1 = gốc,
     * 2 = giảm mỗi kích thước bằng 1/2 (1/4 diện tích), 4 = 1/4 kích thước (1/16 diện tích)...
     */
    public Bitmap loadBitmap(String filename, int inSampleSize) {
        File file = getFile(filename);
        if (!file.exists()) {
                            Log.w(TAG, "loadBitmap: file không tồn tại " + file.getAbsolutePath());
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, inSampleSize);
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap != null) {
            Log.d(TAG, "loadBitmap " + filename + " sample=" + inSampleSize
                    + " -> " + bitmap.getWidth() + "x" + bitmap.getHeight()
                    + " (" + bitmap.getAllocationByteCount() + " bytes ram)");
        }
        return bitmap;
    }

    /**
     * Nạp Bitmap để phù hợp với kích thước view mục tiêu, tự động tính toán inSampleSize
     * để tiết kiệm RAM. Hữu ích cho hiển thị thumbnail trong RecyclerView.
     */
    public Bitmap loadBitmapForView(String filename, int targetWidth, int targetHeight) {
        File file = getFile(filename);
        if (!file.exists()) return null;

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);

        int sample = calculateInSampleSize(bounds, targetWidth, targetHeight);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap != null) {
            Log.d(TAG, "loadBitmapForView " + filename
                    + " -> sample=" + sample
                    + " size=" + bitmap.getWidth() + "x" + bitmap.getHeight()
                    + " ram=" + bitmap.getAllocationByteCount() + "B");
        }
        return bitmap;
    }

    /** Tính toán inSampleSize theo luật power of 2. */
    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (reqWidth <= 0 || reqHeight <= 0) return inSampleSize;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /** Xóa file ảnh, trả về true nếu thành công. */
    public boolean deleteImage(String filename) {
        File file = getFile(filename);
        if (!file.exists()) return false;
        boolean ok = file.delete();
        Log.d(TAG, "deleteImage " + filename + " -> " + ok);
        return ok;
    }

    /** Kích thước file (bytes), 0 nếu file không tồn tại. */
    public long getFileSize(String filename) {
        File file = getFile(filename);
        return file.exists() ? file.length() : 0L;
    }
}
