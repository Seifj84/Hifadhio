package com.seiftech.hifadhio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.seiftech.hifadhio.R;
import com.seiftech.hifadhio.ai.AiConfig;
import com.seiftech.hifadhio.ai.AiOptions;
import com.seiftech.hifadhio.ai.AiProvider;
import com.seiftech.hifadhio.ai.AiRegistry;
import com.seiftech.hifadhio.ai.AiResult;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.media.MediaCleanupWorker;
import com.seiftech.hifadhio.media.MediaStorageManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private ContentDb db;
    private TextView tvStats;
    private TextView tvAiStatus;
    private TextView tvAiDetails;
    private MaterialButton btnTestAi;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new ContentDb(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvStats = view.findViewById(R.id.tv_profile_stats);
        tvAiStatus = view.findViewById(R.id.tv_ai_provider_status);
        tvAiDetails = view.findViewById(R.id.tv_ai_provider_details);
        btnTestAi = view.findViewById(R.id.btn_test_ai_connection);
        MaterialButton btnExport = view.findViewById(R.id.btn_export_json);
        MaterialButton btnCleanCache = view.findViewById(R.id.btn_clean_cache);

        if (btnExport != null) {
            btnExport.setOnClickListener(v -> {
                String json = db.exportToJson();
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("application/json");
                send.putExtra(Intent.EXTRA_SUBJECT, "Hifadhio Library Export");
                send.putExtra(Intent.EXTRA_TEXT, json);
                startActivity(Intent.createChooser(send, "Export Hifadhio Library"));
            });
        }

        if (btnCleanCache != null) {
            btnCleanCache.setOnClickListener(v -> {
                MediaStorageManager sm = MediaStorageManager.getInstance(requireContext());
                MediaCleanupWorker worker = new MediaCleanupWorker(requireContext(), sm, db);
                MediaCleanupWorker.CleanupReport report = worker.runCleanupSweep();
                String freedStr = Formatter.formatFileSize(requireContext(), report.bytesFreed);
                Toast.makeText(requireContext(), "Cleaned cache: " + freedStr + " freed", Toast.LENGTH_SHORT).show();
                updateStats();
            });
        }

        if (btnTestAi != null) {
            btnTestAi.setOnClickListener(v -> runAiConnectionTest());
        }

        updateStats();
        updateAiStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStats();
        updateAiStatus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void updateStats() {
        if (db == null || tvStats == null || getContext() == null) return;
        int count = db.getTotalCount();
        int collectionsCount = db.getCollections().size();
        long mediaBytes = MediaStorageManager.getInstance(requireContext()).getTotalMediaStorageBytes();
        String mediaStr = Formatter.formatFileSize(requireContext(), mediaBytes);
        tvStats.setText(count + " saved items • " + collectionsCount + " collections • " + mediaStr + " media cache");
    }

    private void updateAiStatus() {
        if (tvAiStatus == null || tvAiDetails == null || getContext() == null) return;

        AiProvider primary = AiRegistry.getInstance().getPrimaryProvider();
        AiConfig config = AiConfig.getInstance(requireContext());

        if (primary != null && primary.isAvailable() && !primary.isOffline()) {
            tvAiStatus.setText("Primary: " + primary.getDisplayName() + " • Active");
            tvAiDetails.setText("Model: " + config.getActiveModel() + " (Free Tier)\n"
                    + "Fallback: " + AiConfig.FALLBACK_FREE_MODEL + "\n"
                    + "Offline-first local heuristic fallback enabled.");
        } else {
            tvAiStatus.setText("Primary: Local Heuristic AI • Offline Mode");
            tvAiDetails.setText("Rule-based local enrichment active.\nOffline-first operation enabled.");
        }
    }

    private void runAiConnectionTest() {
        if (btnTestAi == null || getContext() == null) return;

        btnTestAi.setEnabled(false);
        btnTestAi.setText("Testing AI Connection...");

        executor.execute(() -> {
            String testContext = "Title: Quick Test Reel\n"
                    + "Description: Testing AI multimodal intelligence connection for Hifadhio library.\n"
                    + "Tags: #productivity #hifadhio #test";

            AiProvider provider = AiRegistry.getInstance().getPrimaryProvider();
            AiResult result = null;
            if (provider != null) {
                result = provider.enrich(0L, testContext, AiOptions.defaults());
            }

            final AiResult finalResult = result;
            final AiProvider finalProvider = provider;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    btnTestAi.setEnabled(true);
                    btnTestAi.setText("Test AI Connection");

                    if (finalResult != null && finalResult.isSuccess()) {
                        String model = finalResult.getEnrichment() != null && finalResult.getEnrichment().getModel() != null
                                ? finalResult.getEnrichment().getModel()
                                : (finalProvider != null ? finalProvider.getDisplayName() : "AI");
                        Toast.makeText(requireContext(),
                                "AI Connection Successful!\n" + model + " responded in " + finalResult.getLatencyMs() + "ms",
                                Toast.LENGTH_LONG).show();
                    } else {
                        String errMsg = finalResult != null ? finalResult.getErrorMessage() : "Provider unavailable";
                        Toast.makeText(requireContext(),
                                "AI Test: " + errMsg + "\nFalling back to local heuristic provider.",
                                Toast.LENGTH_LONG).show();
                    }
                    updateAiStatus();
                });
            }
        });
    }
}
