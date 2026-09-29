package com.seiftech.hifadhio.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.format.DateUtils;
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
import com.seiftech.hifadhio.data.ProcessingJob;
import com.seiftech.hifadhio.data.ProcessingJobManager;
import com.seiftech.hifadhio.data.UrlNormalizer;

public class SaveLinkBottomSheet extends BottomSheetDialogFragment {

    public interface OnSavedListener {
        void onSaved(ContentItem item);
    }

    private static final String ARG_INITIAL_URL = "arg_initial_url";
    private static final String ARG_EDIT_ITEM = "arg_edit_item";

    private String initialUrl;
    private ContentItem editItem;
    private ContentItem detectedDuplicate = null;
    private OnSavedListener onSavedListener;
    private ContentDb db;

    private View cardDuplicate;
    private TextView tvDuplicateTitle;
    private TextView tvDuplicateDetails;

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
        cardDuplicate = view.findViewById(R.id.card_duplicate_warning);
        tvDuplicateTitle = view.findViewById(R.id.tv_duplicate_title);
        tvDuplicateDetails = view.findViewById(R.id.tv_duplicate_details);

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
            checkDuplicate(extracted, etTitle, etNotes, etCollection, etTags, btnSave);
        }

        etUrl.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilUrl.setError(null);
                String current = s != null ? s.toString() : "";
                updatePlatformBadge(tvPlatform, current);
                checkDuplicate(current, etTitle, etNotes, etCollection, etTags, btnSave);
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
                if (editItem.getOriginalTitle() == null || editItem.getOriginalTitle().isEmpty()) {
                    editItem.setOriginalTitle(editItem.getTitle() != null && !editItem.getTitle().isEmpty() ? editItem.getTitle() : title);
                }
                editItem.setUrl(cleanUrl);
                editItem.setCanonicalUrl(canonical);
                editItem.setPlatform(platform);
                editItem.setTitle(title);
                editItem.setCaption(caption);
                editItem.setNotes(notes);
                editItem.setCollectionName(collection);
                editItem.setTags(tags);
                db.update(editItem);
                db.createCollection(collection);
                Toast.makeText(requireContext(), "Saved changes to Hifadhio", Toast.LENGTH_SHORT).show();
                if (onSavedListener != null) onSavedListener.onSaved(editItem);
            } else if (detectedDuplicate != null) {
                if (detectedDuplicate.getOriginalTitle() == null || detectedDuplicate.getOriginalTitle().isEmpty()) {
                    detectedDuplicate.setOriginalTitle(detectedDuplicate.getTitle());
                }
                detectedDuplicate.setUrl(cleanUrl);
                detectedDuplicate.setCanonicalUrl(canonical);
                detectedDuplicate.setPlatform(platform);
                if (!title.isEmpty()) detectedDuplicate.setTitle(title);
                if (!caption.isEmpty()) detectedDuplicate.setCaption(caption);
                if (!notes.isEmpty()) detectedDuplicate.setNotes(notes);
                if (!collection.isEmpty()) detectedDuplicate.setCollectionName(collection);
                if (!tags.isEmpty()) detectedDuplicate.setTags(tags);
                db.update(detectedDuplicate);
                db.createCollection(collection);
                Toast.makeText(requireContext(), "Updated existing item in Hifadhio", Toast.LENGTH_SHORT).show();
                if (onSavedListener != null) onSavedListener.onSaved(detectedDuplicate);
            } else {
                ContentItem item = new ContentItem();
                item.setUrl(cleanUrl);
                item.setCanonicalUrl(canonical);
                item.setPlatform(platform);
                item.setTitle(title);
                item.setOriginalTitle(title);
                item.setCaption(caption);
                item.setNotes(notes);
                item.setCollectionName(collection);
                item.setTags(tags);
                item.setStatus("SAVED");
                long insertedId = db.insert(item);
                item.setId(insertedId);
                db.createCollection(collection);
                ProcessingJobManager.getInstance(requireContext()).enqueueJob(insertedId, ProcessingJob.TYPE_METADATA_FETCH);
                Toast.makeText(requireContext(), "Link captured to Hifadhio!", Toast.LENGTH_SHORT).show();
                if (onSavedListener != null) onSavedListener.onSaved(item);
            }
            dismiss();
        });
    }

    private void checkDuplicate(String url, TextInputEditText etTitle, TextInputEditText etNotes, TextInputEditText etCollection, TextInputEditText etTags, MaterialButton btnSave) {
        if (editItem != null || cardDuplicate == null) return;
        String clean = UrlNormalizer.extractUrl(url);
        if (!UrlNormalizer.isValidUrl(clean)) {
            cardDuplicate.setVisibility(View.GONE);
            detectedDuplicate = null;
            btnSave.setText("Save to Library");
            return;
        }
        String canonical = UrlNormalizer.normalize(clean);
        ContentItem existing = db.findByCanonicalUrl(canonical);
        if (existing != null) {
            detectedDuplicate = existing;
            cardDuplicate.setVisibility(View.VISIBLE);
            CharSequence timeStr = DateUtils.getRelativeTimeSpanString(existing.getSavedAt(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            tvDuplicateDetails.setText("Already saved in '" + existing.getCollectionName() + "' (" + timeStr + "). You can update notes or collection below.");
            btnSave.setText("Update Existing Item");

            if ((etTitle.getText() == null || etTitle.getText().toString().trim().isEmpty()) && existing.getTitle() != null) {
                etTitle.setText(existing.getTitle());
            }
            if ((etNotes.getText() == null || etNotes.getText().toString().trim().isEmpty()) && existing.getNotes() != null) {
                etNotes.setText(existing.getNotes());
            }
            if ((etCollection.getText() == null || etCollection.getText().toString().equals("Inbox")) && existing.getCollectionName() != null) {
                etCollection.setText(existing.getCollectionName());
            }
            if ((etTags.getText() == null || etTags.getText().toString().trim().isEmpty()) && existing.getTags() != null) {
                etTags.setText(existing.getTags());
            }
        } else {
            detectedDuplicate = null;
            cardDuplicate.setVisibility(View.GONE);
            btnSave.setText("Save to Library");
        }
    }

    private void updatePlatformBadge(TextView tv, String url) {
        String platform = PlatformDetector.detect(url);
        tv.setText("Detected: " + platform);
        int colorRes = PlatformDetector.getPlatformColorRes(platform);
        tv.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }
}
