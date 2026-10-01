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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.LinearLayout;
import com.seiftech.hifadhio.media.MediaArtifact;
import com.seiftech.hifadhio.media.MediaStorageManager;
import com.seiftech.hifadhio.transcription.AudioExtractor;
import com.seiftech.hifadhio.transcription.Transcript;
import com.seiftech.hifadhio.transcription.TranscriptSegment;
import com.seiftech.hifadhio.ocr.FrameExtractor;
import com.seiftech.hifadhio.ocr.OcrFrame;
import com.seiftech.hifadhio.ocr.OcrRecord;
import com.seiftech.hifadhio.ai.AiEnrichment;
import com.seiftech.hifadhio.ai.AiEntity;
import java.io.File;
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
    private TextView btnToggleTechDetails, tvTechWorkerInfo, tvTechErrorInfo, tvTechStorageInfo;
    private View cardThumbnail;
    private ImageView ivThumbnail;
    private ImageView btnFav, btnClose, btnCopyUrl;
    private Chip chipCollection;
    private ChipGroup chipGroupTags;
    private MaterialButton btnOpen, btnShare, btnEditAll, btnDelete, btnRetryJob;
    private TextView btnEditNote, btnAddTag;
    private LinearLayout layoutEventsList, layoutTechDetails, layoutUserSteps;
    private boolean isTechDetailsExpanded = false;

    // Phase 08: Transcript Views
    private LinearLayout layoutTranscript, layoutTranscriptSegments;
    private TextView tvTranscriptProvider, tvTranscriptDuration, tvTranscriptText;
    private EditText etTranscriptSearch;
    private ImageButton btnCopyTranscript;
    private MaterialButton btnTranscribeAction;

    // Phase 09: OCR Visual Text Views
    private LinearLayout layoutOcr, layoutOcrFrames;
    private TextView tvOcrProvider, tvOcrFrames, tvOcrText;
    private EditText etOcrSearch;
    private ImageButton btnCopyOcr;
    private MaterialButton btnOcrAction;

    // Phase 10: AI Enrichment Views
    private LinearLayout layoutAiEnrichment, layoutAiKeyPoints, layoutAiEntities, layoutAiSuggestedTags, layoutAiSuggestedCollection;
    private TextView tvAiBadgeGenerated, tvAiBadgeVersion, tvAiSummaryShort, tvAiSummaryDetailed;
    private TextView tvAiKeyPointsHeader, tvAiEntitiesHeader, tvAiSuggestedTagsHeader, tvAiSuggestedCollection;
    private ImageButton btnCopyAiSummary;
    private MaterialButton btnAiAcceptCollection, btnAiReprocess;

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
        layoutTechDetails = view.findViewById(R.id.layout_technical_details);
        layoutUserSteps = view.findViewById(R.id.layout_detail_user_steps);
        btnToggleTechDetails = view.findViewById(R.id.btn_toggle_tech_details);
        tvTechWorkerInfo = view.findViewById(R.id.tv_tech_worker_info);
        tvTechErrorInfo = view.findViewById(R.id.tv_tech_error_info);
        tvTechStorageInfo = view.findViewById(R.id.tv_tech_storage_info);
        cardThumbnail = view.findViewById(R.id.card_detail_thumbnail);
        ivThumbnail = view.findViewById(R.id.iv_detail_thumbnail);

        // Phase 08 Transcript Views
        layoutTranscript = view.findViewById(R.id.layout_detail_transcript);
        layoutTranscriptSegments = view.findViewById(R.id.layout_transcript_segments);
        tvTranscriptProvider = view.findViewById(R.id.tv_transcript_badge_provider);
        tvTranscriptDuration = view.findViewById(R.id.tv_transcript_badge_duration);
        tvTranscriptText = view.findViewById(R.id.tv_detail_transcript_text);
        etTranscriptSearch = view.findViewById(R.id.et_transcript_search);
        btnCopyTranscript = view.findViewById(R.id.btn_copy_transcript);
        btnTranscribeAction = view.findViewById(R.id.btn_transcribe_action);

        if (btnCopyTranscript != null) {
            btnCopyTranscript.setOnClickListener(v -> {
                Transcript t = db.getTranscriptForItem(item.getId());
                if (t != null && t.getFullText() != null && !t.getFullText().isEmpty()) {
                    ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("Transcript", t.getFullText()));
                        Toast.makeText(requireContext(), "Transcript copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnTranscribeAction != null) {
            btnTranscribeAction.setOnClickListener(v -> {
                ProcessingJobManager.getInstance(requireContext()).enqueueTranscription(item.getId());
                Toast.makeText(requireContext(), "Transcription job scheduled", Toast.LENGTH_SHORT).show();
                populateViews();
                if (listener != null) listener.onContentUpdated(item);
            });
        }

        if (etTranscriptSearch != null) {
            etTranscriptSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterTranscript(s != null ? s.toString() : "");
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Phase 09 OCR Views
        layoutOcr = view.findViewById(R.id.layout_detail_ocr);
        layoutOcrFrames = view.findViewById(R.id.layout_ocr_frames);
        tvOcrProvider = view.findViewById(R.id.tv_ocr_badge_provider);
        tvOcrFrames = view.findViewById(R.id.tv_ocr_badge_frames);
        tvOcrText = view.findViewById(R.id.tv_detail_ocr_text);
        etOcrSearch = view.findViewById(R.id.et_ocr_search);
        btnCopyOcr = view.findViewById(R.id.btn_copy_ocr);
        btnOcrAction = view.findViewById(R.id.btn_ocr_action);

        if (btnCopyOcr != null) {
            btnCopyOcr.setOnClickListener(v -> {
                OcrRecord r = db.getOcrRecordForItem(item.getId());
                if (r != null && r.getFullText() != null && !r.getFullText().isEmpty()) {
                    ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("Visual Text", r.getFullText()));
                        Toast.makeText(requireContext(), "Visual text copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnOcrAction != null) {
            btnOcrAction.setOnClickListener(v -> {
                ProcessingJobManager.getInstance(requireContext()).enqueueOcr(item.getId());
                Toast.makeText(requireContext(), "OCR visual text job scheduled", Toast.LENGTH_SHORT).show();
                populateViews();
                if (listener != null) listener.onContentUpdated(item);
            });
        }

        if (etOcrSearch != null) {
            etOcrSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterOcr(s != null ? s.toString() : "");
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Phase 10 AI Enrichment Views
        layoutAiEnrichment = view.findViewById(R.id.layout_detail_ai_enrichment);
        layoutAiKeyPoints = view.findViewById(R.id.layout_ai_key_points);
        layoutAiEntities = view.findViewById(R.id.layout_ai_entities);
        layoutAiSuggestedTags = view.findViewById(R.id.layout_ai_suggested_tags);
        layoutAiSuggestedCollection = view.findViewById(R.id.layout_ai_suggested_collection);
        tvAiBadgeGenerated = view.findViewById(R.id.tv_ai_badge_generated);
        tvAiBadgeVersion = view.findViewById(R.id.tv_ai_badge_version);
        tvAiSummaryShort = view.findViewById(R.id.tv_ai_summary_short);
        tvAiSummaryDetailed = view.findViewById(R.id.tv_ai_summary_detailed);
        tvAiKeyPointsHeader = view.findViewById(R.id.tv_ai_key_points_header);
        tvAiEntitiesHeader = view.findViewById(R.id.tv_ai_entities_header);
        tvAiSuggestedTagsHeader = view.findViewById(R.id.tv_ai_suggested_tags_header);
        tvAiSuggestedCollection = view.findViewById(R.id.tv_ai_suggested_collection);
        btnCopyAiSummary = view.findViewById(R.id.btn_copy_ai_summary);
        btnAiAcceptCollection = view.findViewById(R.id.btn_ai_accept_collection);
        btnAiReprocess = view.findViewById(R.id.btn_ai_reprocess);

        if (btnCopyAiSummary != null) {
            btnCopyAiSummary.setOnClickListener(v -> {
                AiEnrichment enrichment = db.getAiEnrichmentForItem(item.getId());
                if (enrichment != null) {
                    String fullSummary = enrichment.getSummaryShort() + "\n\n" + enrichment.getSummaryDetailed();
                    ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("AI Summary", fullSummary.trim()));
                        Toast.makeText(requireContext(), "AI summary copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnAiReprocess != null) {
            btnAiReprocess.setOnClickListener(v -> {
                ProcessingJobManager.getInstance(requireContext()).enqueueAiEnrichment(item.getId(), true);
                Toast.makeText(requireContext(), "Reprocessing AI enrichment with latest prompt...", Toast.LENGTH_SHORT).show();
                populateViews();
                if (listener != null) listener.onContentUpdated(item);
            });
        }

        if (btnToggleTechDetails != null && layoutTechDetails != null) {
            btnToggleTechDetails.setOnClickListener(v -> {
                isTechDetailsExpanded = !isTechDetailsExpanded;
                layoutTechDetails.setVisibility(isTechDetailsExpanded ? View.VISIBLE : View.GONE);
                btnToggleTechDetails.setText(isTechDetailsExpanded ? "▾ Hide Developer Diagnostics" : "▸ Developer Diagnostics");
            });
        }

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
            androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Hifadhio_Dialog)
                    .setTitle("Delete Content Item?")
                    .setMessage("Are you sure you want to remove this saved item from Hifadhio? This cannot be undone.")
                    .setPositiveButton("Delete", (d, which) -> {
                        db.delete(item.getId());
                        Toast.makeText(requireContext(), "Item removed", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onContentDeleted(item);
                        dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.setOnShowListener(d -> {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            });
            dialog.show();
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

        // Thumbnail preview loading from local object storage (Phase 07)
        if (cardThumbnail != null && ivThumbnail != null) {
            File thumbFile = MediaStorageManager.getInstance(requireContext()).getThumbnailFile(item.getId(), db);
            if (thumbFile != null && thumbFile.exists() && thumbFile.length() > 0) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(thumbFile.getAbsolutePath());
                    if (bmp != null) {
                        ivThumbnail.setImageBitmap(bmp);
                        cardThumbnail.setVisibility(View.VISIBLE);
                    } else {
                        cardThumbnail.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    cardThumbnail.setVisibility(View.GONE);
                }
            } else {
                cardThumbnail.setVisibility(View.GONE);
            }
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
        populateTranscript();
        populateOcr();
        populateAiEnrichment();
    }

    private void populateProcessingTimeline() {
        if (tvJobStatus == null || tvJobMessage == null) return;
        ProcessingJob job = db.getLatestJobForContentItem(item.getId());

        if (job == null) {
            tvJobStatus.setText("Ready");
            tvJobStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint));
            tvJobMessage.setText("Page details and summary ready");
            if (btnRetryJob != null) btnRetryJob.setVisibility(View.GONE);
        } else {
            String state = job.getState();
            if (ProcessingJob.STATE_COMPLETED.equalsIgnoreCase(state)) {
                tvJobStatus.setText("Ready");
                tvJobStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint));
                tvJobMessage.setText("Page details extracted and ready");
                if (btnRetryJob != null) btnRetryJob.setVisibility(View.GONE);
            } else if (ProcessingJob.STATE_FAILED.equalsIgnoreCase(state)) {
                tvJobStatus.setText("Could not analyze this link");
                tvJobStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                tvJobMessage.setText("We couldn't extract details from this link, but your link is saved safely and can be opened in your browser.");
                if (btnRetryJob != null) {
                    btnRetryJob.setVisibility(View.VISIBLE);
                    btnRetryJob.setText("Tap to retry");
                    btnRetryJob.setOnClickListener(v -> {
                        ProcessingJobManager.getInstance(requireContext()).retryJob(job.getId());
                        Toast.makeText(requireContext(), "Processing retry scheduled", Toast.LENGTH_SHORT).show();
                        populateViews();
                        if (listener != null) listener.onContentUpdated(item);
                    });
                }
            } else if (ProcessingJob.STATE_RETRYING.equalsIgnoreCase(state)) {
                tvJobStatus.setText("Retrying automatically");
                tvJobStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.amber));
                tvJobMessage.setText("Temporary connection issue. Retrying automatically in a few seconds...");
                if (btnRetryJob != null) {
                    btnRetryJob.setVisibility(View.VISIBLE);
                    btnRetryJob.setText("Tap to retry now");
                    btnRetryJob.setOnClickListener(v -> {
                        ProcessingJobManager.getInstance(requireContext()).retryJob(job.getId());
                        Toast.makeText(requireContext(), "Processing retry scheduled", Toast.LENGTH_SHORT).show();
                        populateViews();
                        if (listener != null) listener.onContentUpdated(item);
                    });
                }
            } else if (ProcessingJob.STATE_QUEUED.equalsIgnoreCase(state)) {
                tvJobStatus.setText("Preparing link");
                tvJobStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue));
                tvJobMessage.setText("Scheduled for analysis...");
                if (btnRetryJob != null) btnRetryJob.setVisibility(View.GONE);
            } else { // CLAIMED or RUNNING
                int progress = job.getProgressPercent();
                if (progress < 30) {
                    tvJobStatus.setText("Preparing link");
                    tvJobMessage.setText("Checking page format...");
                } else if (progress < 70) {
                    tvJobStatus.setText("Reading page details");
                    tvJobMessage.setText("Fetching page content...");
                } else {
                    tvJobStatus.setText("Extracting details");
                    tvJobMessage.setText("Saving page details & summary...");
                }
                tvJobStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue));
                if (btnRetryJob != null) btnRetryJob.setVisibility(View.GONE);
            }
        }

        // Render simple user progress steps
        if (layoutUserSteps != null) {
            layoutUserSteps.removeAllViews();
            addUserStepRow(layoutUserSteps, "✓ Link saved securely", R.color.mint);

            if (job == null || ProcessingJob.STATE_COMPLETED.equalsIgnoreCase(job.getState())) {
                addUserStepRow(layoutUserSteps, "✓ Page details extracted", R.color.mint);
                addUserStepRow(layoutUserSteps, "✓ Ready in Library", R.color.mint);
            } else if (ProcessingJob.STATE_FAILED.equalsIgnoreCase(job.getState())) {
                addUserStepRow(layoutUserSteps, "○ Could not analyze this link — original link preserved", R.color.text_secondary);
                addUserStepRow(layoutUserSteps, "✓ Ready to open in browser", R.color.mint);
            } else if (ProcessingJob.STATE_RETRYING.equalsIgnoreCase(job.getState())) {
                addUserStepRow(layoutUserSteps, "⟳ Retrying automatically...", R.color.amber);
            } else {
                addUserStepRow(layoutUserSteps, "● Reading and extracting page details...", R.color.blue);
            }
        }

        // Render technical developer diagnostics
        if (tvTechWorkerInfo != null) {
            if (job != null) {
                tvTechWorkerInfo.setText("Job ID: #" + job.getId() + " • Worker: " + (job.getWorkerId() != null ? job.getWorkerId() : "none") + " • Attempt: " + job.getAttemptCount() + "/" + job.getMaxAttempts());
            } else {
                tvTechWorkerInfo.setText("Job ID: None • State: Initialized");
            }
        }
        if (tvTechErrorInfo != null) {
            if (job != null && job.getSanitizedErrorMessage() != null && !job.getSanitizedErrorMessage().isEmpty()) {
                tvTechErrorInfo.setVisibility(View.VISIBLE);
                tvTechErrorInfo.setText("Diagnostic: " + job.getSanitizedErrorMessage());
            } else {
                tvTechErrorInfo.setVisibility(View.GONE);
            }
        }

        if (tvTechStorageInfo != null) {
            MediaArtifact thumbArt = db.getArtifact(item.getId(), MediaArtifact.TYPE_THUMBNAIL);
            if (thumbArt != null && thumbArt.getStoragePath() != null) {
                tvTechStorageInfo.setVisibility(View.VISIBLE);
                String shaShort = thumbArt.getSha256() != null && thumbArt.getSha256().length() >= 10
                        ? thumbArt.getSha256().substring(0, 10) : "none";
                tvTechStorageInfo.setText("Media Object: " + thumbArt.getStoragePath()
                        + "\nSize: " + thumbArt.getFileSizeBytes() + " bytes • SHA256: " + shaShort
                        + " • Policy: " + thumbArt.getRetentionPolicy());
            } else {
                tvTechStorageInfo.setVisibility(View.GONE);
            }
        }

        if (layoutEventsList != null) {
            layoutEventsList.removeAllViews();
            List<ProcessingEvent> events = db.getEventsForContentItem(item.getId());
            if (events.isEmpty()) {
                TextView emptyTv = new TextView(requireContext());
                emptyTv.setText("No audit events recorded.");
                emptyTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                emptyTv.setTextSize(11f);
                layoutEventsList.addView(emptyTv);
            } else {
                for (ProcessingEvent ev : events) {
                    LinearLayout row = new LinearLayout(requireContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, 3, 0, 3);

                    TextView tvTime = new TextView(requireContext());
                    tvTime.setText(TimeUtils.formatRelativeTime(ev.getCreatedAt()) + " • ");
                    tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                    tvTime.setTextSize(10f);
                    row.addView(tvTime);

                    TextView tvMsg = new TextView(requireContext());
                    tvMsg.setText(ev.getMessage());
                    tvMsg.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                    tvMsg.setTextSize(10f);
                    row.addView(tvMsg);

                    layoutEventsList.addView(row);
                }
            }
        }
    }

    private void addUserStepRow(LinearLayout container, String text, int colorRes) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        tv.setTextSize(12f);
        tv.setPadding(0, 3, 0, 3);
        container.addView(tv);
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

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Hifadhio_Dialog)
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

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Hifadhio_Dialog)
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

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Hifadhio_Dialog)
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

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Hifadhio_Dialog)
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

    private void populateTranscript() {
        if (layoutTranscript == null) return;

        Transcript transcript = db.getTranscriptForItem(item.getId());
        AudioExtractor audioExtractor = new AudioExtractor(requireContext(), MediaStorageManager.getInstance(requireContext()));
        boolean isEligible = audioExtractor.isEligibleForTranscription(item);

        if (transcript != null) {
            layoutTranscript.setVisibility(View.VISIBLE);
            if (tvTranscriptProvider != null) {
                tvTranscriptProvider.setText(transcript.getProviderId());
            }
            if (tvTranscriptDuration != null) {
                tvTranscriptDuration.setText(transcript.getFormattedDuration());
                tvTranscriptDuration.setVisibility(transcript.getDurationMs() > 0 ? View.VISIBLE : View.GONE);
            }
            if (tvTranscriptText != null) {
                tvTranscriptText.setText(transcript.getFullText());
            }
            if (btnCopyTranscript != null) {
                btnCopyTranscript.setVisibility(View.VISIBLE);
            }
            if (btnTranscribeAction != null) {
                btnTranscribeAction.setVisibility(View.GONE);
            }
            renderTranscriptSegments(transcript.getSegments());
        } else if (isEligible) {
            layoutTranscript.setVisibility(View.VISIBLE);
            if (tvTranscriptProvider != null) {
                tvTranscriptProvider.setText("Eligible");
            }
            if (tvTranscriptDuration != null) {
                tvTranscriptDuration.setVisibility(View.GONE);
            }
            if (tvTranscriptText != null) {
                tvTranscriptText.setText("Speech transcript has not been extracted yet.");
            }
            if (btnCopyTranscript != null) {
                btnCopyTranscript.setVisibility(View.GONE);
            }
            if (btnTranscribeAction != null) {
                btnTranscribeAction.setVisibility(View.VISIBLE);
                btnTranscribeAction.setText("Transcribe Speech");
            }
            if (layoutTranscriptSegments != null) {
                layoutTranscriptSegments.removeAllViews();
            }
        } else {
            layoutTranscript.setVisibility(View.GONE);
        }
    }

    private void renderTranscriptSegments(List<TranscriptSegment> segments) {
        if (layoutTranscriptSegments == null) return;
        layoutTranscriptSegments.removeAllViews();
        if (segments == null || segments.isEmpty()) return;

        for (TranscriptSegment seg : segments) {
            if (!seg.hasTimestamps()) continue;
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 4, 0, 4);

            TextView tvTime = new TextView(requireContext());
            tvTime.setText("[" + seg.getFormattedTimestamp() + "] ");
            tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint));
            tvTime.setTextSize(11);
            tvTime.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvSegText = new TextView(requireContext());
            tvSegText.setText(seg.getText());
            tvSegText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvSegText.setTextSize(12);

            row.addView(tvTime);
            row.addView(tvSegText);
            layoutTranscriptSegments.addView(row);
        }
    }

    private void filterTranscript(String query) {
        if (layoutTranscript == null) return;
        Transcript transcript = db.getTranscriptForItem(item.getId());
        if (transcript == null) return;

        if (query == null || query.trim().isEmpty()) {
            if (tvTranscriptText != null) {
                tvTranscriptText.setText(transcript.getFullText());
                tvTranscriptText.setVisibility(View.VISIBLE);
            }
            renderTranscriptSegments(transcript.getSegments());
            return;
        }

        String q = query.trim().toLowerCase();
        if (transcript.hasTimestamps()) {
            List<TranscriptSegment> filtered = transcript.searchSegments(q);
            renderTranscriptSegments(filtered);
            if (tvTranscriptText != null) {
                tvTranscriptText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                if (filtered.isEmpty()) {
                    tvTranscriptText.setText("No segments matching \"" + query + "\"");
                }
            }
        } else {
            if (tvTranscriptText != null) {
                tvTranscriptText.setVisibility(View.VISIBLE);
                if (transcript.getFullText().toLowerCase().contains(q)) {
                    tvTranscriptText.setText(transcript.getFullText());
                } else {
                    tvTranscriptText.setText("No matching speech text for \"" + query + "\"");
                }
            }
        }
    }

    private void populateOcr() {
        if (layoutOcr == null) return;
        OcrRecord record = db.getOcrRecordForItem(item.getId());

        if (record != null && record.getFullText() != null && !record.getFullText().isEmpty()) {
            layoutOcr.setVisibility(View.VISIBLE);
            if (tvOcrProvider != null) {
                tvOcrProvider.setText(record.getProviderId());
                tvOcrProvider.setVisibility(View.VISIBLE);
            }
            if (tvOcrFrames != null) {
                tvOcrFrames.setText(record.getFramesCount() + " frames");
                tvOcrFrames.setVisibility(View.VISIBLE);
            }
            if (tvOcrText != null) {
                tvOcrText.setText(record.getFullText());
                tvOcrText.setVisibility(View.VISIBLE);
            }
            if (btnCopyOcr != null) {
                btnCopyOcr.setVisibility(View.VISIBLE);
            }
            if (btnOcrAction != null) {
                btnOcrAction.setVisibility(View.GONE);
            }
            renderOcrFrames(record.getFrames());
        } else if (FrameExtractor.isEligibleForOcr(item)) {
            layoutOcr.setVisibility(View.VISIBLE);
            if (tvOcrProvider != null) {
                tvOcrProvider.setText("Eligible");
                tvOcrProvider.setVisibility(View.VISIBLE);
            }
            if (tvOcrFrames != null) {
                tvOcrFrames.setVisibility(View.GONE);
            }
            if (tvOcrText != null) {
                tvOcrText.setText("Visual text has not been extracted yet.");
            }
            if (btnCopyOcr != null) {
                btnCopyOcr.setVisibility(View.GONE);
            }
            if (btnOcrAction != null) {
                btnOcrAction.setVisibility(View.VISIBLE);
                btnOcrAction.setText("Extract Visual Text (OCR)");
            }
            if (layoutOcrFrames != null) {
                layoutOcrFrames.removeAllViews();
            }
        } else {
            layoutOcr.setVisibility(View.GONE);
        }
    }

    private void renderOcrFrames(List<OcrFrame> frames) {
        if (layoutOcrFrames == null) return;
        layoutOcrFrames.removeAllViews();
        if (frames == null || frames.isEmpty()) return;

        for (OcrFrame frame : frames) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 4, 0, 4);

            TextView tvHeader = new TextView(requireContext());
            if (frame.hasTimestamp()) {
                tvHeader.setText("[" + frame.getFormattedTimestamp() + "] ");
            } else {
                tvHeader.setText("[Frame " + (frame.getFrameIndex() + 1) + "] ");
            }
            tvHeader.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint));
            tvHeader.setTextSize(11);
            tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvFrameText = new TextView(requireContext());
            tvFrameText.setText(frame.getText());
            tvFrameText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvFrameText.setTextSize(12);

            row.addView(tvHeader);
            row.addView(tvFrameText);
            layoutOcrFrames.addView(row);
        }
    }

    private void filterOcr(String query) {
        if (layoutOcr == null) return;
        OcrRecord record = db.getOcrRecordForItem(item.getId());
        if (record == null) return;

        if (query == null || query.trim().isEmpty()) {
            if (tvOcrText != null) {
                tvOcrText.setText(record.getFullText());
                tvOcrText.setVisibility(View.VISIBLE);
            }
            renderOcrFrames(record.getFrames());
            return;
        }

        String q = query.trim().toLowerCase();
        List<OcrFrame> filtered = record.searchFrames(q);
        renderOcrFrames(filtered);
        if (tvOcrText != null) {
            tvOcrText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            if (filtered.isEmpty()) {
                tvOcrText.setText("No visual text matching \"" + query + "\"");
            }
        }
    }

    private void populateAiEnrichment() {
        if (layoutAiEnrichment == null) return;
        AiEnrichment enrichment = db.getAiEnrichmentForItem(item.getId());

        if (enrichment != null && (!enrichment.getSummaryShort().isEmpty() || !enrichment.getSummaryDetailed().isEmpty())) {
            layoutAiEnrichment.setVisibility(View.VISIBLE);

            if (tvAiBadgeGenerated != null) {
                tvAiBadgeGenerated.setText("AI GENERATED");
                tvAiBadgeGenerated.setVisibility(View.VISIBLE);
            }
            if (tvAiBadgeVersion != null) {
                tvAiBadgeVersion.setText(enrichment.getPromptVersion() + " (" + enrichment.getProviderId() + ")");
                tvAiBadgeVersion.setVisibility(View.VISIBLE);
            }
            if (tvAiSummaryShort != null) {
                tvAiSummaryShort.setText(enrichment.getSummaryShort());
                tvAiSummaryShort.setVisibility(View.VISIBLE);
            }
            if (tvAiSummaryDetailed != null) {
                tvAiSummaryDetailed.setText(enrichment.getSummaryDetailed());
                tvAiSummaryDetailed.setVisibility(View.VISIBLE);
            }
            if (btnCopyAiSummary != null) {
                btnCopyAiSummary.setVisibility(View.VISIBLE);
            }
            if (btnAiReprocess != null) {
                btnAiReprocess.setText("Reprocess with latest prompt");
            }

            renderAiKeyPoints(enrichment.getKeyPoints());
            renderAiEntities(enrichment.getEntities());
            renderAiSuggestedTags(enrichment.getSuggestedTags());
            renderAiSuggestedCollection(enrichment.getSuggestedCollection());
        } else {
            layoutAiEnrichment.setVisibility(View.VISIBLE);

            if (tvAiBadgeGenerated != null) {
                tvAiBadgeGenerated.setText("READY FOR ENRICHMENT");
                tvAiBadgeGenerated.setVisibility(View.VISIBLE);
            }
            if (tvAiBadgeVersion != null) {
                tvAiBadgeVersion.setVisibility(View.GONE);
            }
            if (tvAiSummaryShort != null) {
                tvAiSummaryShort.setText("AI enrichment not generated yet.");
                tvAiSummaryShort.setVisibility(View.VISIBLE);
            }
            if (tvAiSummaryDetailed != null) {
                tvAiSummaryDetailed.setVisibility(View.GONE);
            }
            if (tvAiKeyPointsHeader != null) tvAiKeyPointsHeader.setVisibility(View.GONE);
            if (layoutAiKeyPoints != null) layoutAiKeyPoints.removeAllViews();
            if (tvAiEntitiesHeader != null) tvAiEntitiesHeader.setVisibility(View.GONE);
            if (layoutAiEntities != null) layoutAiEntities.removeAllViews();
            if (tvAiSuggestedTagsHeader != null) tvAiSuggestedTagsHeader.setVisibility(View.GONE);
            if (layoutAiSuggestedTags != null) layoutAiSuggestedTags.removeAllViews();
            if (layoutAiSuggestedCollection != null) layoutAiSuggestedCollection.setVisibility(View.GONE);
            if (btnCopyAiSummary != null) btnCopyAiSummary.setVisibility(View.GONE);
            if (btnAiReprocess != null) {
                btnAiReprocess.setText("Generate AI Enrichment");
            }
        }
    }

    private void renderAiKeyPoints(List<String> keyPoints) {
        if (layoutAiKeyPoints == null) return;
        layoutAiKeyPoints.removeAllViews();
        if (keyPoints == null || keyPoints.isEmpty()) {
            if (tvAiKeyPointsHeader != null) tvAiKeyPointsHeader.setVisibility(View.GONE);
            return;
        }
        if (tvAiKeyPointsHeader != null) tvAiKeyPointsHeader.setVisibility(View.VISIBLE);

        for (String kp : keyPoints) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 3, 0, 3);

            TextView tvBullet = new TextView(requireContext());
            tvBullet.setText("• ");
            tvBullet.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint));
            tvBullet.setTextSize(13);
            tvBullet.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvText = new TextView(requireContext());
            tvText.setText(kp);
            tvText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvText.setTextSize(12);

            row.addView(tvBullet);
            row.addView(tvText);
            layoutAiKeyPoints.addView(row);
        }
    }

    private void renderAiEntities(List<AiEntity> entities) {
        if (layoutAiEntities == null) return;
        layoutAiEntities.removeAllViews();
        if (entities == null || entities.isEmpty()) {
            if (tvAiEntitiesHeader != null) tvAiEntitiesHeader.setVisibility(View.GONE);
            return;
        }
        if (tvAiEntitiesHeader != null) tvAiEntitiesHeader.setVisibility(View.VISIBLE);

        for (AiEntity entity : entities) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 3, 0, 3);

            TextView tvBadge = new TextView(requireContext());
            tvBadge.setText("[" + entity.getType().toUpperCase(java.util.Locale.US) + "] ");
            tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint));
            tvBadge.setTextSize(11);
            tvBadge.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvName = new TextView(requireContext());
            tvName.setText(entity.getName());
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvName.setTextSize(12);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);

            row.addView(tvBadge);
            row.addView(tvName);

            if (entity.getEvidence() != null && !entity.getEvidence().isEmpty()) {
                TextView tvEvidence = new TextView(requireContext());
                tvEvidence.setText(" — " + entity.getEvidence());
                tvEvidence.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                tvEvidence.setTextSize(11);
                row.addView(tvEvidence);
            }

            layoutAiEntities.addView(row);
        }
    }

    private void renderAiSuggestedTags(List<String> suggestedTags) {
        if (layoutAiSuggestedTags == null) return;
        layoutAiSuggestedTags.removeAllViews();
        if (suggestedTags == null || suggestedTags.isEmpty()) {
            if (tvAiSuggestedTagsHeader != null) tvAiSuggestedTagsHeader.setVisibility(View.GONE);
            return;
        }
        if (tvAiSuggestedTagsHeader != null) tvAiSuggestedTagsHeader.setVisibility(View.VISIBLE);

        for (String tag : suggestedTags) {
            final String cleanTag = tag.startsWith("#") ? tag.substring(1).trim() : tag.trim();
            if (cleanTag.isEmpty()) continue;

            com.google.android.material.button.MaterialButton chip = new com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle);
            chip.setText("#" + cleanTag);
            chip.setTextSize(11);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint));
            chip.setPadding(12, 4, 12, 4);

            chip.setOnClickListener(v -> {
                String existing = item.getTags() != null ? item.getTags() : "";
                if (!existing.toLowerCase(java.util.Locale.US).contains(cleanTag.toLowerCase(java.util.Locale.US))) {
                    String updatedTags = existing.trim().isEmpty() ? cleanTag : existing.trim() + "," + cleanTag;
                    item.setTags(updatedTags);
                    db.update(item);
                    populateTags();
                    Toast.makeText(requireContext(), "Added #" + cleanTag, Toast.LENGTH_SHORT).show();
                    chip.setEnabled(false);
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                    if (listener != null) listener.onContentUpdated(item);
                } else {
                    Toast.makeText(requireContext(), "#" + cleanTag + " is already in item tags", Toast.LENGTH_SHORT).show();
                }
            });

            layoutAiSuggestedTags.addView(chip);
        }
    }

    private void renderAiSuggestedCollection(String suggestedCollection) {
        if (layoutAiSuggestedCollection == null) return;
        if (suggestedCollection == null || suggestedCollection.trim().isEmpty() || suggestedCollection.equalsIgnoreCase(item.getCollectionName())) {
            layoutAiSuggestedCollection.setVisibility(View.GONE);
            return;
        }

        layoutAiSuggestedCollection.setVisibility(View.VISIBLE);
        if (tvAiSuggestedCollection != null) {
            tvAiSuggestedCollection.setText("Suggested Collection: " + suggestedCollection.trim());
        }
        if (btnAiAcceptCollection != null) {
            btnAiAcceptCollection.setText("Move to " + suggestedCollection.trim());
            btnAiAcceptCollection.setOnClickListener(v -> {
                item.setCollectionName(suggestedCollection.trim());
                db.update(item);
                if (chipCollection != null) chipCollection.setText(item.getCollectionName());
                Toast.makeText(requireContext(), "Moved to " + suggestedCollection.trim(), Toast.LENGTH_SHORT).show();
                layoutAiSuggestedCollection.setVisibility(View.GONE);
                if (listener != null) listener.onContentUpdated(item);
            });
        }
    }
}
