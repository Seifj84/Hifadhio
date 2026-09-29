package com.seiftech.hifadhio.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.seiftech.hifadhio.R;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.data.ContentItem;
import com.seiftech.hifadhio.data.PlatformDetector;
import com.seiftech.hifadhio.data.ProcessingEvent;
import com.seiftech.hifadhio.data.ProcessingJob;
import com.seiftech.hifadhio.data.ProcessingJobManager;
import com.seiftech.hifadhio.data.TimeUtils;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

public class ContentDetailBottomSheet extends BottomSheetDialogFragment {

    public interface OnContentActionListener {
        void onContentUpdated(ContentItem item);
        void onContentDeleted(ContentItem item);
    }

    private static final String ARG_ITEM = "arg_item";

    private ContentItem item;
    private ContentDb db;
    private OnContentActionListener listener;

    private TextView tvPlatform, tvDate, tvTitle, tvOriginalTitle, tvCaption, tvStatus, tvUrl, tvNotes;
    private TextView tvJobStatus, tvJobMessage;
    private ImageView btnFav, btnClose, btnCopyUrl;
    private Chip chipCollection;
    private ChipGroup chipGroupTags;
    private MaterialButton btnOpen, btnShare, btnEditAll, btnDelete, btnRetryJob;
    private TextView btnEditNote, btnAddTag;
    private LinearLayout layoutEventsList;

    public static ContentDetailBottomSheet newInstance(ContentItem item) {
        ContentDetailBottomSheet sheet = new ContentDetailBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ITEM, item);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnContentActionListener(OnContentActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new ContentDb(requireContext());
        if (getArguments() != null) {
            item = (ContentItem) getArguments().getSerializable(ARG_ITEM);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_content_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (item == null) {
            dismiss();
            return;
        }

        tvPlatform = view.findViewById(R.id.tv_detail_platform);
        tvDate = view.findViewById(R.id.tv_detail_date);
        btnFav = view.findViewById(R.id.btn_detail_fav);
        btnClose = view.findViewById(R.id.btn_detail_close);
        tvTitle = view.findViewById(R.id.tv_detail_title);
        tvOriginalTitle = view.findViewById(R.id.tv_detail_original_title);
        tvCaption = view.findViewById(R.id.tv_detail_caption);
        chipCollection = view.findViewById(R.id.chip_detail_collection);
        tvStatus = view.findViewById(R.id.tv_detail_status);
        tvUrl = view.findViewById(R.id.tv_detail_url);
        btnCopyUrl = view.findViewById(R.id.btn_detail_copy_url);
        tvNotes = view.findViewById(R.id.tv_detail_notes);
        btnEditNote = view.findViewById(R.id.btn_detail_edit_note);
        btnAddTag = view.findViewById(R.id.btn_detail_add_tag);
        chipGroupTags = view.findViewById(R.id.chip_group_detail_tags);
        btnOpen = view.findViewById(R.id.btn_detail_open);
        btnShare = view.findViewById(R.id.btn_detail_share);
        btnEditAll = view.findViewById(R.id.btn_detail_edit_all);
        btnDelete = view.findViewById(R.id.btn_detail_delete);
        tvJobStatus = view.findViewById(R.id.tv_detail_job_status);
        tvJobMessage = view.findViewById(R.id.tv_detail_job_message);
        btnRetryJob = view.findViewById(R.id.btn_detail_retry_job);
        layoutEventsList = view.findViewById(R.id.layout_detail_events_list);

        populateViews();

        btnClose.setOnClickListener(v -> dismiss());

        btnFav.setOnClickListener(v -> {
            item.setFavorite(!item.isFavorite());
            db.update(item);
            populateViews();
            if (listener != null) listener.onContentUpdated(item);
        });

        btnCopyUrl.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Link", item.getUrl()));
                Toast.makeText(requireContext(), "Link copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        chipCollection.setOnClickListener(v -> showMoveDialog());

        btnEditNote.setOnClickListener(v -> showEditNoteDialog());

        btnAddTag.setOnClickListener(v -> showEditTagsDialog());

        btnOpen.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Could not open link in browser", Toast.LENGTH_SHORT).show();
            }
        });

        btnShare.setOnClickListener(v -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, item.getUrl());
            startActivity(Intent.createChooser(send, "Share Link"));
        });

        btnEditAll.setOnClickListener(v -> {
            SaveLinkBottomSheet sheet = SaveLinkBottomSheet.newInstance(item);
            sheet.setOnSavedListener(updated -> {
                this.item = updated;
                populateViews();
                if (listener != null) listener.onContentUpdated(updated);
            });
            sheet.show(getParentFragmentManager(), "edit_link_from_detail");
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Content Item?")
                    .setMessage("Are you sure you want to remove this saved item from Hifadhio?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.delete(item.getId());
                        Toast.makeText(requireContext(), "Item removed", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onContentDeleted(item);
                        dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void populateViews() {
        tvPlatform.setText(item.getPlatform());
        int colorRes = PlatformDetector.getPlatformColorRes(item.getPlatform());
        tvPlatform.setTextColor(ContextCompat.getColor(requireContext(), colorRes));

        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                item.getSavedAt(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        );
        tvDate.setText("Saved " + relativeTime);

        btnFav.setImageResource(item.isFavorite() ? R.drawable.ic_star : R.drawable.ic_star_outline);

        tvTitle.setText(item.getDisplayTitle());

        if (item.getOriginalTitle() != null && !item.getOriginalTitle().trim().isEmpty()
                && !item.getOriginalTitle().trim().equals(item.getTitle().trim())) {
            tvOriginalTitle.setVisibility(View.VISIBLE);
            tvOriginalTitle.setText("Original: " + item.getOriginalTitle().trim());
        } else {
            tvOriginalTitle.setVisibility(View.GONE);
        }

        if (item.getCaption() != null && !item.getCaption().trim().isEmpty()) {
            tvCaption.setVisibility(View.VISIBLE);
            tvCaption.setText(item.getCaption().trim());
        } else {
            tvCaption.setVisibility(View.GONE);
        }

        chipCollection.setText(item.getCollectionName());
        tvStatus.setText(item.getStatus() != null ? item.getStatus() : "SAVED");
        tvUrl.setText(item.getCanonicalUrl());

        if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
            tvNotes.setText(item.getNotes().trim());
            tvNotes.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        } else {
            tvNotes.setText("No personal notes added yet. Tap 'Edit Note' to add context.");
            tvNotes.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }

        populateTags();
        populateProcessingTimeline();
    }

    private void populateProcessingTimeline() {
        if (tvJobStatus == null || tvJobMessage == null || layoutEventsList == null) return;
        ProcessingJob job = db.getLatestJobForContentItem(item.getId());
        if (job == null) {
            tvJobStatus.setText("READY");
            tvJobStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint));
            tvJobMessage.setText("Saved and ready");
            if (btnRetryJob != null) btnRetryJob.setVisibility(View.GONE);
        } else {
            String state = job.getState();
            tvJobStatus.setText(state);
            if (ProcessingJob.STATE_COMPLETED.equalsIgnoreCase(state)) {
                tvJobStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint));
                tvJobMessage.setText(job.getStageMessage() != null && !job.getStageMessage().isEmpty() ? job.getStageMessage() : "Completed successfully");
                if (btnRetryJob != null) btnRetryJob.setVisibility(View.GONE);
            } else if (ProcessingJob.STATE_FAILED.equalsIgnoreCase(state)) {
                tvJobStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
                tvJobMessage.setText(job.getSanitizedErrorMessage() != null && !job.getSanitizedErrorMessage().isEmpty() ? job.getSanitizedErrorMessage() : "Processing failed");
                if (btnRetryJob != null) {
                    btnRetryJob.setVisibility(View.VISIBLE);
                    btnRetryJob.setOnClickListener(v -> {
                        ProcessingJobManager.getInstance(requireContext()).retryJob(job.getId());
                        Toast.makeText(requireContext(), "Processing retry scheduled", Toast.LENGTH_SHORT).show();
                        populateViews();
                        if (listener != null) listener.onContentUpdated(item);
                    });
                }
            } else {
                tvJobStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue));
                tvJobMessage.setText(job.getStageMessage() != null && !job.getStageMessage().isEmpty() ? job.getStageMessage() : "Processing in progress...");
                if (btnRetryJob != null) btnRetryJob.setVisibility(View.GONE);
            }
        }

        layoutEventsList.removeAllViews();
        List<ProcessingEvent> events = db.getEventsForContentItem(item.getId());
        if (events.isEmpty()) {
            TextView emptyTv = new TextView(requireContext());
            emptyTv.setText("No audit events recorded.");
            emptyTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            emptyTv.setTextSize(12f);
            layoutEventsList.addView(emptyTv);
        } else {
            for (ProcessingEvent ev : events) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);

                TextView tvTime = new TextView(requireContext());
                tvTime.setText(TimeUtils.formatRelativeTime(ev.getCreatedAt()) + " • ");
                tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                tvTime.setTextSize(11f);
                row.addView(tvTime);

                TextView tvMsg = new TextView(requireContext());
                tvMsg.setText(ev.getMessage());
                tvMsg.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                tvMsg.setTextSize(11f);
                row.addView(tvMsg);

                layoutEventsList.addView(row);
            }
        }
    }

    private void populateTags() {
        chipGroupTags.removeAllViews();
        String raw = item.getTags();
        if (raw != null && !raw.trim().isEmpty()) {
            String[] split = raw.split("[,\\s]+");
            for (String tag : split) {
                String clean = tag.trim();
                if (!clean.isEmpty()) {
                    if (!clean.startsWith("#")) clean = "#" + clean;
                    Chip c = new Chip(requireContext());
                    c.setText(clean);
                    c.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_navy));
                    c.setChipBackgroundColorResource(R.color.mint_soft);
                    chipGroupTags.addView(c);
                }
            }
        }
    }

    private void showEditNoteDialog() {
        final EditText input = new EditText(requireContext());
        input.setText(item.getNotes());
        input.setHint("Why did you save this link?");
        input.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        input.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Note")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String notes = input.getText() != null ? input.getText().toString().trim() : "";
                    item.setNotes(notes);
                    db.update(item);
                    populateViews();
                    Toast.makeText(requireContext(), "Note updated", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onContentUpdated(item);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditTagsDialog() {
        final EditText input = new EditText(requireContext());
        input.setText(item.getTags());
        input.setHint("e.g. #ai #design #reading (separated by space or comma)");
        input.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        input.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Tags")
                .setMessage("Enter tags for easy search and filtering:")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String tags = input.getText() != null ? input.getText().toString().trim() : "";
                    item.setTags(tags);
                    db.update(item);
                    populateViews();
                    Toast.makeText(requireContext(), "Tags updated", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onContentUpdated(item);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMoveDialog() {
        List<String> rawCollections = db.getCollections();
        List<String> options = new ArrayList<>(rawCollections);
        options.add("+ New Collection...");

        int currentSelection = rawCollections.indexOf(item.getCollectionName());
        if (currentSelection < 0) currentSelection = 0;

        new AlertDialog.Builder(requireContext())
                .setTitle("Move to Collection")
                .setSingleChoiceItems(options.toArray(new String[0]), currentSelection, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == options.size() - 1) {
                        showCreateCollectionDialog();
                    } else {
                        String target = options.get(which);
                        item.setCollectionName(target);
                        db.update(item);
                        populateViews();
                        Toast.makeText(requireContext(), "Moved to " + target, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onContentUpdated(item);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreateCollectionDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("e.g. Design Systems");
        input.setSingleLine(true);
        input.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        input.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("New Collection")
                .setMessage("Enter collection name:")
                .setView(container)
                .setPositiveButton("Create & Move", (dialog, which) -> {
                    String name = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!name.isEmpty()) {
                        db.createCollection(name);
                        item.setCollectionName(name);
                        db.update(item);
                        populateViews();
                        Toast.makeText(requireContext(), "Moved to " + name, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onContentUpdated(item);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
