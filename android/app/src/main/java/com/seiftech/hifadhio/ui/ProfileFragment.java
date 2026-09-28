package com.seiftech.hifadhio.ui;

import android.content.Intent;
import android.os.Bundle;
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
import com.seiftech.hifadhio.data.ContentDb;

public class ProfileFragment extends Fragment {

    private ContentDb db;
    private TextView tvStats;

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
        MaterialButton btnExport = view.findViewById(R.id.btn_export_json);

        btnExport.setOnClickListener(v -> {
            String json = db.exportToJson();
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/json");
            send.putExtra(Intent.EXTRA_SUBJECT, "Hifadhio Library Export");
            send.putExtra(Intent.EXTRA_TEXT, json);
            startActivity(Intent.createChooser(send, "Export Hifadhio Library"));
        });

        updateStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStats();
    }

    private void updateStats() {
        if (db == null || tvStats == null) return;
        int count = db.getTotalCount();
        int collectionsCount = db.getCollections().size();
        tvStats.setText(count + " saved items across " + collectionsCount + " collections • Local SQLite Active");
    }
}
