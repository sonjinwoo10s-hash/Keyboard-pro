package com.keyboardpro.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.BuildConfig;
import com.example.R;
import com.keyboardpro.data.KeyboardPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Screen 3: Requests & Suggestions (الطلبات والاقتراحات).
 * Features 2 sub-tabs (طلب جديد / طلباتي), 4 request type buttons, 2000-char counter,
 * zero-permission Android Photo Picker with auto-compression to <= 2MB,
 * expandable Device Info preview, privacy notice, and persistent My Requests list.
 */
public class RequestsFragment extends Fragment {

    private static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024; // 2MB

    private KeyboardPrefs prefs;
    private String selectedType = "طلب ميزة";
    private String attachedImagePath = "";

    private TextView tabNew, tabMine;
    private View formContainer;
    private LinearLayout mineContainer;
    private TextView btnFeature, btnBug, btnSuggestion, btnInstant;
    private EditText etName, etTitle, etDesc, etContact;
    private TextView tvCounter, tvDeviceBody, tvDeviceArrow, tvAttachmentInfo;
    private View attachmentRow;
    private ImageView ivAttachment;

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    compressAndAttachImage(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new KeyboardPrefs(requireContext());

        tabNew = view.findViewById(R.id.req_tab_new);
        tabMine = view.findViewById(R.id.req_tab_mine);
        formContainer = view.findViewById(R.id.req_form_container);
        mineContainer = view.findViewById(R.id.req_mine_container);

        btnFeature = view.findViewById(R.id.btn_type_feature);
        btnBug = view.findViewById(R.id.btn_type_bug);
        btnSuggestion = view.findViewById(R.id.btn_type_suggestion);
        btnInstant = view.findViewById(R.id.btn_type_instant);

        etName = view.findViewById(R.id.et_req_name);
        etTitle = view.findViewById(R.id.et_req_title);
        etDesc = view.findViewById(R.id.et_req_desc);
        etContact = view.findViewById(R.id.et_req_contact);
        tvCounter = view.findViewById(R.id.tv_desc_counter);
        tvDeviceBody = view.findViewById(R.id.tv_device_info_body);
        tvDeviceArrow = view.findViewById(R.id.tv_device_info_arrow);
        attachmentRow = view.findViewById(R.id.attachment_preview_row);
        ivAttachment = view.findViewById(R.id.iv_attachment_preview);
        tvAttachmentInfo = view.findViewById(R.id.tv_attachment_info);

        tabNew.setOnClickListener(v -> showSubTab(true));
        tabMine.setOnClickListener(v -> showSubTab(false));

        btnFeature.setOnClickListener(v -> selectType("طلب ميزة", btnFeature));
        btnBug.setOnClickListener(v -> selectType("بلاغ مشكلة", btnBug));
        btnSuggestion.setOnClickListener(v -> selectType("اقتراح", btnSuggestion));
        btnInstant.setOnClickListener(v -> selectType("ميزة فورية", btnInstant));

        etDesc.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s == null ? 0 : s.length();
                tvCounter.setText(len + " / 2000");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        view.findViewById(R.id.btn_pick_attachment).setOnClickListener(v ->
                photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        view.findViewById(R.id.btn_remove_attachment).setOnClickListener(v -> {
            attachedImagePath = "";
            attachmentRow.setVisibility(View.GONE);
        });

        tvDeviceBody.setText(buildDeviceInfoString());
        view.findViewById(R.id.btn_toggle_device_info).setOnClickListener(v -> {
            boolean show = tvDeviceBody.getVisibility() != View.VISIBLE;
            tvDeviceBody.setVisibility(show ? View.VISIBLE : View.GONE);
            tvDeviceArrow.setText(show ? "إخفاء ▲" : "عرض ▼");
        });

        view.findViewById(R.id.btn_submit_request).setOnClickListener(v -> submitRequest());

        showSubTab(true);
    }

    private void showSubTab(boolean isNew) {
        tabNew.setBackgroundResource(isNew ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        tabNew.setTextColor(ContextCompat.getColor(requireContext(),
                isNew ? R.color.kp_primary : R.color.kp_text_secondary));

        tabMine.setBackgroundResource(!isNew ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        tabMine.setTextColor(ContextCompat.getColor(requireContext(),
                !isNew ? R.color.kp_primary : R.color.kp_text_secondary));

        formContainer.setVisibility(isNew ? View.VISIBLE : View.GONE);
        mineContainer.setVisibility(isNew ? View.GONE : View.VISIBLE);
        if (!isNew) {
            renderMyRequests();
        }
    }

    private void selectType(String type, TextView selectedView) {
        selectedType = type;
        TextView[] all = {btnFeature, btnBug, btnSuggestion, btnInstant};
        for (TextView tv : all) {
            boolean active = tv == selectedView;
            tv.setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            tv.setTextColor(ContextCompat.getColor(requireContext(),
                    active ? R.color.kp_primary : R.color.kp_text_secondary));
        }
    }

    private void compressAndAttachImage(Uri uri) {
        try {
            Bitmap scaled = com.keyboardpro.data.BitmapUtils.decodeSampledFromUri(
                    requireContext(), uri, 1024, 1024);
            if (scaled == null) return;

            // Compress iteratively to stay <= 2MB
            int quality = 88;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            while (baos.size() > MAX_IMAGE_BYTES && quality > 25) {
                baos.reset();
                quality -= 12;
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            }

            File dir = new File(requireContext().getFilesDir(), "req_attachments");
            if (!dir.exists()) dir.mkdirs();
            File outFile = new File(dir, "att_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(baos.toByteArray());
            }

            attachedImagePath = outFile.getAbsolutePath();
            int thumbPx = dp(64);
            Bitmap thumb = com.keyboardpro.data.BitmapUtils.decodeSampledFromFile(attachedImagePath, thumbPx, thumbPx);
            ivAttachment.setImageBitmap(thumb != null ? thumb : scaled);
            if (thumb != null && scaled != thumb) scaled.recycle();
            int kb = Math.max(1, baos.size() / 1024);
            tvAttachmentInfo.setText("تم إرفاق الصورة وضغطها تلقائياً (" + kb + " KB)");
            attachmentRow.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "تعذر معالجة الصورة المختارة", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildDeviceInfoString() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return "App: Keyboard Pro v" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n"
                + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                + "Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n"
                + "Screen: " + dm.widthPixels + "x" + dm.heightPixels + " (" + dm.densityDpi + " dpi)\n"
                + "Active Theme: " + prefs.getActiveThemeId();
    }

    private void submitRequest() {
        String name = etName.getText().toString().trim();
        String title = etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String contact = etContact.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("الرجاء إدخال الاسم");
            return;
        }
        if (title.isEmpty()) {
            etTitle.setError("الرجاء إدخال عنوان مختصر");
            return;
        }
        if (desc.isEmpty()) {
            etDesc.setError("الرجاء كتابة تفاصيل الطلب");
            return;
        }

        try {
            JSONArray arr = new JSONArray(prefs.getUserRequestsJson());
            JSONObject obj = new JSONObject();
            obj.put("id", "KP-" + (1000 + arr.length() + 1));
            obj.put("type", selectedType);
            obj.put("name", name);
            obj.put("title", title);
            obj.put("desc", desc);
            obj.put("contact", contact);
            obj.put("attachment", attachedImagePath);
            obj.put("device", buildDeviceInfoString());
            obj.put("date", new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date()));
            obj.put("status", "قيد المراجعة");

            // Prepend newest first
            JSONArray updated = new JSONArray();
            updated.put(obj);
            for (int i = 0; i < arr.length(); i++) {
                updated.put(arr.get(i));
            }
            prefs.setUserRequestsJson(updated.toString());

            etTitle.setText("");
            etDesc.setText("");
            attachedImagePath = "";
            attachmentRow.setVisibility(View.GONE);

            Toast.makeText(requireContext(), "تم إرسال طلبك بنجاح وحفظه في «طلباتي» ✅", Toast.LENGTH_LONG).show();
            showSubTab(false);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "حدث خطأ أثناء حفظ الطلب", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderMyRequests() {
        mineContainer.removeAllViews();
        try {
            JSONArray arr = new JSONArray(prefs.getUserRequestsJson());
            if (arr.length() == 0) {
                TextView empty = new TextView(requireContext());
                empty.setText(R.string.req_empty_mine);
                empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.kp_text_secondary));
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(dp(16), dp(48), dp(16), dp(32));
                mineContainer.addView(empty);
                return;
            }

            int textPrimary = ContextCompat.getColor(requireContext(), R.color.kp_text_primary);
            int textSecondary = ContextCompat.getColor(requireContext(), R.color.kp_text_secondary);
            int accent = ContextCompat.getColor(requireContext(), R.color.kp_primary);
            int success = ContextCompat.getColor(requireContext(), R.color.kp_success);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                LinearLayout card = new LinearLayout(requireContext());
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundResource(R.drawable.bg_card);
                card.setPadding(dp(14), dp(12), dp(14), dp(12));
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                clp.bottomMargin = dp(10);
                card.setLayoutParams(clp);

                LinearLayout top = new LinearLayout(requireContext());
                top.setOrientation(LinearLayout.HORIZONTAL);
                top.setGravity(Gravity.CENTER_VERTICAL);

                TextView badge = new TextView(requireContext());
                badge.setText(o.optString("type") + " • " + o.optString("id"));
                badge.setTextColor(accent);
                badge.setTextSize(11);
                badge.setBackgroundResource(R.drawable.bg_chip_selected);
                badge.setPadding(dp(8), dp(3), dp(8), dp(3));
                top.addView(badge);

                View spacer = new View(requireContext());
                top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

                TextView status = new TextView(requireContext());
                status.setText(o.optString("status", "قيد المراجعة"));
                status.setTextColor(success);
                status.setTextSize(11);
                top.addView(status);
                card.addView(top);

                TextView title = new TextView(requireContext());
                title.setText(o.optString("title"));
                title.setTextColor(textPrimary);
                title.setTextSize(15);
                title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
                title.setPadding(0, dp(8), 0, dp(4));
                card.addView(title);

                TextView desc = new TextView(requireContext());
                desc.setText(o.optString("desc"));
                desc.setTextColor(textSecondary);
                desc.setTextSize(12.5f);
                card.addView(desc);

                TextView meta = new TextView(requireContext());
                meta.setText("بواسطة: " + o.optString("name") + "  •  " + o.optString("date"));
                meta.setTextColor(ContextCompat.getColor(requireContext(), R.color.kp_text_muted));
                meta.setTextSize(11);
                meta.setPadding(0, dp(8), 0, 0);
                card.addView(meta);

                mineContainer.addView(card);
            }
        } catch (Exception ignored) {
        }
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }
}
