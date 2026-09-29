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
import com.seiftech.hifadhio.R;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.data.ContentItem;
import java.util.List;

public class InboxFragment extends Fragment implements ContentAdapter.OnItemActionListener {

    private ContentDb db;
    private ContentAdapter adapter;
    private View layoutEmpty;
    private android.widget.TextView tvInboxCountBadge;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new ContentDb(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inbox, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        layoutEmpty = view.findViewById(R.id.layout_inbox_empty);
        tvInboxCountBadge = view.findViewById(R.id.tv_inbox_count_badge);
        RecyclerView recycler = view.findViewById(R.id.recycler_inbox);

        adapter = new ContentAdapter(requireContext(), db, this);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        refresh();
    }

    public void refresh() {
        if (db == null || adapter == null) return;
        List<ContentItem> items = db.getInboxItems();
        adapter.setItems(items);
        int count = items.size();
        if (tvInboxCountBadge != null) {
            if (count > 0) {
                tvInboxCountBadge.setVisibility(View.VISIBLE);
                tvInboxCountBadge.setText(count == 1 ? "1 item to triage" : count + " items to triage");
            } else {
                tvInboxCountBadge.setVisibility(View.GONE);
            }
        }
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateInboxBadge();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onViewDetail(ContentItem item) {
        ContentDetailBottomSheet sheet = ContentDetailBottomSheet.newInstance(item);
        sheet.setOnContentActionListener(new ContentDetailBottomSheet.OnContentActionListener() {
            @Override
            public void onContentUpdated(ContentItem item) {
                refresh();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateInboxBadge();
                }
            }

            @Override
            public void onContentDeleted(ContentItem item) {
                refresh();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateInboxBadge();
                }
            }
        });
        sheet.show(getParentFragmentManager(), "detail_sheet_inbox");
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
                .setMessage("Remove this item from your Inbox?")
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
