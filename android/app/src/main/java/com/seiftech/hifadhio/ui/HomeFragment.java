package com.seiftech.hifadhio.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.seiftech.hifadhio.R;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.data.ContentItem;
import java.util.List;

public class HomeFragment extends Fragment implements ContentAdapter.OnItemActionListener {

    private ContentDb db;
    private ContentAdapter adapter;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private TextView tvTotalSaved;
    private TextView tvTotalCollections;
    private View layoutEmpty;
    private String selectedPlatform = "All";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new ContentDb(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        tvTotalSaved = view.findViewById(R.id.tv_total_saved);
        tvTotalCollections = view.findViewById(R.id.tv_total_collections);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        RecyclerView recycler = view.findViewById(R.id.recycler_home);
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_platforms);

        adapter = new ContentAdapter(requireContext(), db, this);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                refresh();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedPlatform = "All";
            } else {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    selectedPlatform = chip.getText().toString();
                }
            }
            refresh();
        });

        refresh();
    }

    public void refresh() {
        if (db == null || adapter == null) return;
        String query = etSearch != null && etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        List<ContentItem> items = db.search(query, selectedPlatform, "All");
        adapter.setItems(items);

        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }

        int totalCount = db.getTotalCount();
        List<String> collections = db.getCollections();
        if (tvTotalSaved != null) {
            tvTotalSaved.setText(String.valueOf(totalCount));
        }
        if (tvTotalCollections != null) {
            tvTotalCollections.setText(String.valueOf(Math.max(1, collections.size())));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onEdit(ContentItem item) {
        SaveLinkBottomSheet sheet = SaveLinkBottomSheet.newInstance(item);
        sheet.setOnSavedListener(savedItem -> refresh());
        sheet.show(getParentFragmentManager(), "edit_link_sheet");
    }

    @Override
    public void onDelete(ContentItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Content Item?")
                .setMessage("Are you sure you want to remove this saved item from Hifadhio?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.delete(item.getId());
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDataChanged() {
        refresh();
    }
}
