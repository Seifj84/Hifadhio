package com.seiftech.hifadhio.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.seiftech.hifadhio.R;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.data.ContentItem;
import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment implements ContentAdapter.OnItemActionListener {

    private enum FilterMode {
        ALL,
        FAVORITES,
        COLLECTION,
        TAG
    }

    private ContentDb db;
    private ContentAdapter adapter;
    private ChipGroup chipGroupCollections;
    private ChipGroup chipGroupTags;
    private LinearLayout layoutTagsSection;
    private TextView tvFilterTitle, btnManageCollection;
    private View layoutEmpty;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle, tvEmptySubtitle;

    private FilterMode currentMode = FilterMode.ALL;
    private String selectedCollection = "All";
    private String selectedTag = "";

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
        chipGroupTags = view.findViewById(R.id.chip_group_tags);
        layoutTagsSection = view.findViewById(R.id.layout_tags_section);
        tvFilterTitle = view.findViewById(R.id.tv_library_filter_title);
        btnManageCollection = view.findViewById(R.id.btn_manage_collection);
        layoutEmpty = view.findViewById(R.id.layout_library_empty);
        ivEmptyIcon = view.findViewById(R.id.iv_library_empty_icon);
        tvEmptyTitle = view.findViewById(R.id.tv_library_empty_title);
        tvEmptySubtitle = view.findViewById(R.id.tv_library_empty_subtitle);

        MaterialButton btnNewCollection = view.findViewById(R.id.btn_new_collection);
        btnNewCollection.setOnClickListener(v -> showCreateCollectionDialog());

        btnManageCollection.setOnClickListener(v -> {
            if (currentMode == FilterMode.COLLECTION && !"All".equalsIgnoreCase(selectedCollection) && !"Inbox".equalsIgnoreCase(selectedCollection)) {
                showCollectionOptions(selectedCollection, v);
            }
        });

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

        int totalCount = db.getTotalCount();
        Chip allChip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter);
        allChip.setText("All (" + totalCount + ")");
        allChip.setCheckable(true);
        allChip.setChecked(currentMode == FilterMode.ALL);
        allChip.setOnClickListener(v -> {
            currentMode = FilterMode.ALL;
            selectedCollection = "All";
            selectedTag = "";
            refresh();
            setupChips();
        });
        chipGroupCollections.addView(allChip);

        int favCount = db.getFavoritesCount();
        Chip favChip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter);
        favChip.setText("★ Favorites (" + favCount + ")");
        favChip.setCheckable(true);
        favChip.setChecked(currentMode == FilterMode.FAVORITES);
        favChip.setOnClickListener(v -> {
            currentMode = FilterMode.FAVORITES;
            selectedCollection = "";
            selectedTag = "";
            refresh();
            setupChips();
        });
        chipGroupCollections.addView(favChip);

        List<ContentDb.CollectionStat> collections = db.getCollectionsWithCounts();
        for (ContentDb.CollectionStat stat : collections) {
            final String cName = stat.getName();
            Chip chip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter);
            chip.setText(cName + " (" + stat.getItemCount() + ")");
            chip.setCheckable(true);
            boolean isSelected = currentMode == FilterMode.COLLECTION && cName.equals(selectedCollection);
            chip.setChecked(isSelected);

            chip.setOnClickListener(v -> {
                currentMode = FilterMode.COLLECTION;
                selectedCollection = cName;
                selectedTag = "";
                refresh();
                setupChips();
            });

            if (!cName.equalsIgnoreCase("Inbox")) {
                chip.setOnLongClickListener(v -> {
                    showCollectionOptions(cName, v);
                    return true;
                });
            }
            chipGroupCollections.addView(chip);
        }

        setupTagsChips();
    }

    private void setupTagsChips() {
        if (chipGroupTags == null || layoutTagsSection == null || db == null) return;
        chipGroupTags.removeAllViews();
        List<String> tags = db.getAllTags();
        if (tags.isEmpty()) {
            layoutTagsSection.setVisibility(View.GONE);
            return;
        }

        layoutTagsSection.setVisibility(View.VISIBLE);
        for (String tag : tags) {
            Chip chip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter);
            chip.setText(tag);
            chip.setCheckable(true);
            boolean isChecked = currentMode == FilterMode.TAG && tag.equalsIgnoreCase(selectedTag);
            chip.setChecked(isChecked);
            chip.setOnClickListener(v -> {
                if (isChecked) {
                    currentMode = FilterMode.ALL;
                    selectedTag = "";
                } else {
                    currentMode = FilterMode.TAG;
                    selectedTag = tag;
                }
                refresh();
                setupChips();
            });
            chipGroupTags.addView(chip);
        }
    }

    public void refresh() {
        if (db == null || adapter == null) return;
        List<ContentItem> items;
        if (currentMode == FilterMode.FAVORITES) {
            items = db.getFavorites();
            tvFilterTitle.setText("★ Favorites • " + items.size() + " items");
            btnManageCollection.setVisibility(View.GONE);
            updateEmptyState(R.drawable.ic_star_outline, "No Starred Items", "Tap the star icon on any card to add items to your favorites.");
        } else if (currentMode == FilterMode.TAG) {
            items = db.getByTag(selectedTag);
            tvFilterTitle.setText("Tag " + selectedTag + " • " + items.size() + " items");
            btnManageCollection.setVisibility(View.GONE);
            updateEmptyState(R.drawable.ic_link, "No Items with " + selectedTag, "Items tagged with " + selectedTag + " will appear here.");
        } else if (currentMode == FilterMode.COLLECTION) {
            items = db.search("", "All", selectedCollection);
            tvFilterTitle.setText("Collection: " + selectedCollection + " • " + items.size() + " items");
            btnManageCollection.setVisibility("Inbox".equalsIgnoreCase(selectedCollection) ? View.GONE : View.VISIBLE);
            updateEmptyState(R.drawable.ic_library, "Empty Collection", "No links in '" + selectedCollection + "' yet. Move links here to organize.");
        } else {
            items = db.getAll();
            tvFilterTitle.setText("All Items • " + items.size() + " items");
            btnManageCollection.setVisibility(View.GONE);
            updateEmptyState(R.drawable.ic_library, "No Saved Items", "Save links from your favorite apps to build your knowledge base.");
        }

        adapter.setItems(items);
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateEmptyState(int iconRes, String title, String subtitle) {
        if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(iconRes);
        if (tvEmptyTitle != null) tvEmptyTitle.setText(title);
        if (tvEmptySubtitle != null) tvEmptySubtitle.setText(subtitle);
    }

    private void showCreateCollectionDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("e.g. Design Systems, Recipes");
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
                .setTitle("Create New Collection")
                .setMessage("Group saved items by topic or project:")
                .setView(container)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!name.isEmpty()) {
                        boolean created = db.createCollection(name);
                        if (created) {
                            Toast.makeText(requireContext(), "Created collection '" + name + "'", Toast.LENGTH_SHORT).show();
                            currentMode = FilterMode.COLLECTION;
                            selectedCollection = name;
                            setupChips();
                            refresh();
                        } else {
                            Toast.makeText(requireContext(), "Collection already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCollectionOptions(String collectionName, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "Rename Collection");
        popup.getMenu().add(0, 2, 1, "Delete Collection");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showRenameCollectionDialog(collectionName);
                return true;
            } else if (item.getItemId() == 2) {
                showDeleteCollectionDialog(collectionName);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showRenameCollectionDialog(String oldName) {
        final EditText input = new EditText(requireContext());
        input.setText(oldName);
        input.setSelection(oldName.length());
        input.setSingleLine(true);
        input.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

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
                .setTitle("Rename Collection")
                .setView(container)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!newName.isEmpty() && !newName.equalsIgnoreCase(oldName)) {
                        boolean ok = db.renameCollection(oldName, newName);
                        if (ok) {
                            Toast.makeText(requireContext(), "Renamed to '" + newName + "'", Toast.LENGTH_SHORT).show();
                            if (selectedCollection.equals(oldName)) {
                                selectedCollection = newName;
                            }
                            setupChips();
                            refresh();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteCollectionDialog(String collectionName) {
        int count = db.getCountByCollection(collectionName);
        String msg = count > 0
                ? "Collection '" + collectionName + "' contains " + count + " items. Do you want to move them back to your Inbox or delete them completely?"
                : "Are you sure you want to delete collection '" + collectionName + "'?";

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Delete Collection?")
                .setMessage(msg);

        if (count > 0) {
            builder.setPositiveButton("Move to Inbox", (dialog, which) -> {
                db.deleteCollection(collectionName, true);
                Toast.makeText(requireContext(), "Collection deleted; items moved to Inbox", Toast.LENGTH_SHORT).show();
                currentMode = FilterMode.ALL;
                selectedCollection = "All";
                setupChips();
                refresh();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateInboxBadge();
                }
            });
            builder.setNeutralButton("Delete Items Too", (dialog, which) -> {
                db.deleteCollection(collectionName, false);
                Toast.makeText(requireContext(), "Collection and items deleted", Toast.LENGTH_SHORT).show();
                currentMode = FilterMode.ALL;
                selectedCollection = "All";
                setupChips();
                refresh();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateInboxBadge();
                }
            });
        } else {
            builder.setPositiveButton("Delete", (dialog, which) -> {
                db.deleteCollection(collectionName, true);
                Toast.makeText(requireContext(), "Collection deleted", Toast.LENGTH_SHORT).show();
                currentMode = FilterMode.ALL;
                selectedCollection = "All";
                setupChips();
                refresh();
            });
        }

        builder.setNegativeButton("Cancel", null).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupChips();
        refresh();
    }

    @Override
    public void onViewDetail(ContentItem item) {
        ContentDetailBottomSheet sheet = ContentDetailBottomSheet.newInstance(item);
        sheet.setOnContentActionListener(new ContentDetailBottomSheet.OnContentActionListener() {
            @Override
            public void onContentUpdated(ContentItem item) {
                setupChips();
                refresh();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateInboxBadge();
                }
            }

            @Override
            public void onContentDeleted(ContentItem item) {
                setupChips();
                refresh();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateInboxBadge();
                }
            }
        });
        sheet.show(getParentFragmentManager(), "detail_sheet_library");
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
