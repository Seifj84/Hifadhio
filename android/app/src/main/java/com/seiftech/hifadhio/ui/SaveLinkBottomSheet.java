package com.seiftech.hifadhio.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.seiftech.hifadhio.R;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.data.ContentItem;
import com.seiftech.hifadhio.data.PlatformDetector;
import com.seiftech.hifadhio.data.UrlNormalizer;

public class SaveLinkBottomSheet extends BottomSheetDialogFragment {

    public interface OnSavedListener {
        void onSaved(ContentItem item);
    }

    private static final String ARG_INITIAL_URL = "arg_initial_url";
    private static final String ARG_EDIT_ITEM = "arg_edit_item";

    private String initialUrl;
    private ContentItem editItem;
    private OnSavedListener onSavedListener;
    private ContentDb db;

    public static SaveLinkBottomSheet newInstance(String initialUrl) {
        SaveLinkBottomSheet fragment = new SaveLinkBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_URL, initialUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public static SaveLinkBottomSheet newInstance(ContentItem item) {
        SaveLinkBottomSheet fragment = new SaveLinkBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EDIT_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnSavedListener(OnSavedListener listener) {
        this.onSavedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new ContentDb(requireContext());
        if (getArguments() != null) {
            initialUrl = getArguments().getString(ARG_INITIAL_URL, "");
            editItem = (ContentItem) getArguments().getSerializable(ARG_EDIT_ITEM);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_save_link, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvHeader = view.findViewById(R.id.tv_dialog_header);
        TextInputLayout tilUrl = view.findViewById(R.id.til_url);
        TextInputEditText etUrl = view.findViewById(R.id.et_dialog_url);
        TextView tvPlatform = view.findViewById(R.id.tv_dialog_detected_platform);
        TextInputEditText etTitle = view.findViewById(R.id.et_dialog_title);
        TextInputEditText etCaption = view.findViewById(R.id.et_dialog_caption);
        TextInputEditText etNotes = view.findViewById(R.id.et_dialog_notes);
        TextInputEditText etCollection = view.findViewById(R.id.et_dialog_collection);
        TextInputEditText etTags = view.findViewById(R.id.et_dialog_tags);
        MaterialButton btnSave = view.findViewById(R.id.btn_dialog_save);

        if (editItem != null) {
            tvHeader.setText("Edit Saved Item");
            etUrl.setText(editItem.getUrl());
            etTitle.setText(editItem.getTitle());
            etCaption.setText(editItem.getCaption());
            etNotes.setText(editItem.getNotes());
            etCollection.setText(editItem.getCollectionName());
            etTags.setText(editItem.getTags());
            btnSave.setText("Save Changes");
            updatePlatformBadge(tvPlatform, editItem.getUrl());
        } else if (initialUrl != null && !initialUrl.isEmpty()) {
            String extracted = UrlNormalizer.extractUrl(initialUrl);
            etUrl.setText(extracted);
            updatePlatformBadge(tvPlatform, extracted);
        }

        etUrl.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilUrl.setError(null);
                updatePlatformBadge(tvPlatform, s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSave.setOnClickListener(v -> {
            String rawUrl = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
            String cleanUrl = UrlNormalizer.extractUrl(rawUrl);
            if (!UrlNormalizer.isValidUrl(cleanUrl)) {
                tilUrl.setError("Please enter a valid HTTP/HTTPS link");
                return;
            }

            String canonical = UrlNormalizer.normalize(cleanUrl);
            String platform = PlatformDetector.detect(cleanUrl);
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String caption = etCaption.getText() != null ? etCaption.getText().toString().trim() : "";
            String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
            String collection = etCollection.getText() != null ? etCollection.getText().toString().trim() : "Inbox";
            if (collection.isEmpty()) collection = "Inbox";
            String tags = etTags.getText() != null ? etTags.getText().toString().trim() : "";

            if (editItem != null) {
                editItem.setUrl(cleanUrl);
                editItem.setCanonicalUrl(canonical);
                editItem.setPlatform(platform);
                editItem.setTitle(title);
                editItem.setCaption(caption);
                editItem.setNotes(notes);
                editItem.setCollectionName(collection);
                editItem.setTags(tags);
                db.update(editItem);
                Toast.makeText(requireContext(), "Saved changes to Hifadhio", Toast.LENGTH_SHORT).show();
                if (onSavedListener != null) onSavedListener.onSaved(editItem);
            } else {
                ContentItem item = new ContentItem();
                item.setUrl(cleanUrl);
                item.setCanonicalUrl(canonical);
                item.setPlatform(platform);
                item.setTitle(title);
                item.setCaption(caption);
                item.setNotes(notes);
                item.setCollectionName(collection);
                item.setTags(tags);
                item.setStatus("SAVED");
                db.insert(item);
                Toast.makeText(requireContext(), "Link captured to Hifadhio!", Toast.LENGTH_SHORT).show();
                if (onSavedListener != null) onSavedListener.onSaved(item);
            }
            dismiss();
        });
    }

    private void updatePlatformBadge(TextView tv, String url) {
        String platform = PlatformDetector.detect(url);
        tv.setText("Detected: " + platform);
        int colorRes = PlatformDetector.getPlatformColorRes(platform);
        tv.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }
}
