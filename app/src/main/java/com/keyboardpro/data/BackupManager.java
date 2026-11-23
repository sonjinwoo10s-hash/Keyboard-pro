package com.keyboardpro.data;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Handles local JSON backup/restore via Storage Access Framework (SAF) and
 * cloud backup/restore via Google Drive REST v3 API.
 */
public class BackupManager {

    public interface SyncCallback {
        void onComplete(boolean success, String message);
    }

    private static final String DRIVE_BACKUP_NAME = "keyboard_pro_backup.json";
    private final Context context;
    private final KeyboardPrefs prefs;
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = new KeyboardPrefs(context);
    }

    public boolean exportLocalBackup(Uri destinationUri) {
        try (OutputStream os = context.getContentResolver().openOutputStream(destinationUri)) {
            if (os == null) return false;
            String json = prefs.exportToJson().toString(2);
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean importLocalBackup(Uri sourceUri) {
        try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
            if (is == null) return false;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return prefs.importFromJson(sb.toString());
        } catch (Exception e) {
            return false;
        }
    }

    public void uploadToGoogleDrive(String oauthToken, SyncCallback callback) {
        if (oauthToken == null || oauthToken.trim().isEmpty()) {
            mainHandler.post(() -> callback.onComplete(false,
                    "الرجاء إدخال رمز وصول Google Drive OAuth الصالح للمزامنة السحابية."));
            return;
        }
        executor.execute(() -> {
            try {
                String jsonData = prefs.exportToJson().toString(2);
                String boundary = "keyboardpro_boundary_2026";
                String meta = "{\"name\":\"" + DRIVE_BACKUP_NAME + "\",\"mimeType\":\"application/json\"}";
                String multipartBody = "--" + boundary + "\r\n"
                        + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                        + meta + "\r\n"
                        + "--" + boundary + "\r\n"
                        + "Content-Type: application/json\r\n\r\n"
                        + jsonData + "\r\n"
                        + "--" + boundary + "--";

                Request request = new Request.Builder()
                        .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                        .addHeader("Authorization", "Bearer " + oauthToken.trim())
                        .post(RequestBody.create(
                                multipartBody.getBytes(StandardCharsets.UTF_8),
                                MediaType.parse("multipart/related; boundary=" + boundary)))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        prefs.setLastCloudSyncTime(System.currentTimeMillis());
                        mainHandler.post(() -> callback.onComplete(true,
                                "تم رفع النسخة الاحتياطية إلى Google Drive بنجاح."));
                    } else {
                        int code = response.code();
                        mainHandler.post(() -> callback.onComplete(false,
                                "تعذر الرفع إلى Google Drive (رمز الاستجابة " + code + "). تحقق من صلاحية الرمز."));
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onComplete(false,
                        "خطأ في الاتصال بـ Google Drive: " + e.getMessage()));
            }
        });
    }

    public void restoreFromGoogleDrive(String oauthToken, SyncCallback callback) {
        if (oauthToken == null || oauthToken.trim().isEmpty()) {
            mainHandler.post(() -> callback.onComplete(false,
                    "الرجاء إدخال رمز وصول Google Drive OAuth لاستعادة النسخة."));
            return;
        }
        executor.execute(() -> {
            try {
                Request listReq = new Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files?q=name%3D%27" + DRIVE_BACKUP_NAME + "%27%20and%20trashed%3Dfalse&orderBy=modifiedTime%20desc&pageSize=1")
                        .addHeader("Authorization", "Bearer " + oauthToken.trim())
                        .get()
                        .build();

                String fileId = null;
                try (Response listResp = client.newCall(listReq).execute()) {
                    if (!listResp.isSuccessful() || listResp.body() == null) {
                        int code = listResp.code();
                        mainHandler.post(() -> callback.onComplete(false,
                                "تعذر البحث في Google Drive (رمز " + code + ")."));
                        return;
                    }
                    JSONObject obj = new JSONObject(listResp.body().string());
                    JSONArray files = obj.optJSONArray("files");
                    if (files != null && files.length() > 0) {
                        fileId = files.getJSONObject(0).optString("id");
                    }
                }

                if (fileId == null || fileId.isEmpty()) {
                    mainHandler.post(() -> callback.onComplete(false,
                            "لم يتم العثور على نسخة احتياطية سابقة في Google Drive."));
                    return;
                }

                Request dlReq = new Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media")
                        .addHeader("Authorization", "Bearer " + oauthToken.trim())
                        .get()
                        .build();

                try (Response dlResp = client.newCall(dlReq).execute()) {
                    if (dlResp.isSuccessful() && dlResp.body() != null) {
                        boolean ok = prefs.importFromJson(dlResp.body().string());
                        if (ok) {
                            prefs.setLastCloudSyncTime(System.currentTimeMillis());
                        }
                        mainHandler.post(() -> callback.onComplete(ok,
                                ok ? "تمت استعادة الإعدادات والتخطيطات من Google Drive بنجاح."
                                        : "ملف النسخة الاحتياطية غير صالح."));
                    } else {
                        mainHandler.post(() -> callback.onComplete(false,
                                "فشل تنزيل النسخة من Google Drive."));
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onComplete(false,
                        "خطأ أثناء الاستعادة: " + e.getMessage()));
            }
        });
    }
}
