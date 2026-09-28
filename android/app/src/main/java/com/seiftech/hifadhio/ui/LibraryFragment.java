package com.seiftech.hifadhio.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class LibraryFragment extends Fragment implements ContentAdapter.OnItemActionListener {

    private ContentDb db;
    private ContentAdapter adapter;
    private ChipGroup chipGroupCollections;
    private String selectedCollection = "All";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new ContentDb(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chipGroupCollections = view.findViewById(R.id.chip_group_collections);
        RecyclerView recycler = view.findViewById(R.id.recycler_library);

        adapter = new ContentAdapter(requireContext(), db, this);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        setupChips();
        refresh();
    }

    private void setupChips() {
        if (chipGroupCollections == null || db == null) return;
        chipGroupCollections.removeAllViews();

        Chip allChip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter);
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setChecked("All".equals(selectedCollection));
        allChip.setOnClickListener(v -> {
            selectedCollection = "All";
            refresh();
        });
        chipGroupCollections.addView(allChip);

        List<String> collections = db.getCollections();
        for (String c : collections) {
            Chip chip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter);
            int count = db.getCountByCollection(c);
            chip.setText(c + " (" + count + ")");
            chip.setCheckable(true);
            chip.setChecked(c.equals(selectedCollection));
            chip.setOnClickListener(v -> {
                selectedCollection = c;
                refresh();
            });
            chipGroupCollections.addView(chip);
        }
    }

    public void refresh() {
        if (db == null || adapter == null) return;
        List<ContentItem> items = db.search("", "All", selectedCollection);
        adapter.setItems(items);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupChips();
        refresh();
    }

    @Override
    public void onEdit(ContentItem item) {
        SaveLinkBottomSheet sheet = SaveLinkBottomSheet.newInstance(item);
        sheet.setOnSavedListener(savedItem -> {
            setupChips();
            refresh();
        });
        sheet.show(getParentFragmentManager(), "edit_link_sheet");
    }

    @Override
    public void onDelete(ContentItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Content Item?")
                .setMessage("Remove this item from the collection?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.delete(item.getId());
                    setupChips();
                    refresh();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).updateInboxBadge();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDataChanged() {
        setupChips();
        refresh();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateInboxBadge();
        }
    }
}
