package com.keyboardpro.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.InputStream;

/**
 * Memory-safe Bitmap downsampling helper using BitmapFactory.Options.inSampleSize
 * to match exact target view dimensions and prevent OOM/RAM spikes.
 */
public final class BitmapUtils {

    private BitmapUtils() {
    }

    public static Bitmap decodeSampledFromUri(Context context, Uri uri, int reqWidth, int reqHeight) {
        if (uri == null) return null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream boundsStream = context.getContentResolver().openInputStream(uri)) {
                if (boundsStream == null) return null;
                BitmapFactory.decodeStream(boundsStream, null, opts);
            }

            opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
            opts.inJustDecodeBounds = false;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;

            try (InputStream decodeStream = context.getContentResolver().openInputStream(uri)) {
                if (decodeStream == null) return null;
                return BitmapFactory.decodeStream(decodeStream, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap decodeSampledFromFile(String filePath, int reqWidth, int reqHeight) {
        if (filePath == null || filePath.isEmpty()) return null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, opts);

            opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
            opts.inJustDecodeBounds = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(filePath, opts);
        } catch (Exception e) {
            return null;
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }
}
