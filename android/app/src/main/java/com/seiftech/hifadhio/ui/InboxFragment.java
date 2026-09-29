package com.seiftech.hifadhio.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.seiftech.hifadhio.R;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.data.ContentItem;
import java.util.List;

public class InboxFragment extends Fragment implements ContentAdapter.OnItemActionListener {

    private ContentDb db;
    private ContentAdapter adapter;
    private View layoutTriageHeader;
    private View layoutEmpty;
    private RecyclerView recycler;
    private TextView tvInboxCountBadge;

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
        layoutTriageHeader = view.findViewById(R.id.layout_inbox_triage_header);
        layoutEmpty = view.findViewById(R.id.layout_inbox_empty);
        tvInboxCountBadge = view.findViewById(R.id.tv_inbox_count_badge);
        recycler = view.findViewById(R.id.recycler_inbox);

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

        if (count > 0) {
            if (layoutTriageHeader != null) layoutTriageHeader.setVisibility(View.VISIBLE);
            if (tvInboxCountBadge != null) {
                tvInboxCountBadge.setText(count == 1 ? "1 item to triage" : count + " items to triage");
            }
            if (recycler != null) recycler.setVisibility(View.VISIBLE);
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        } else {
            if (layoutTriageHeader != null) layoutTriageHeader.setVisibility(View.GONE);
            if (recycler != null) recycler.setVisibility(View.GONE);
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
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
            }

            @Override
            public void onContentDeleted(ContentItem item) {
                refresh();
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
        db.delete(item.getId());
        refresh();

        View rootView = getView();
        if (rootView != null) {
            Snackbar snackbar = Snackbar.make(rootView, "Item deleted", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", v -> {
                db.insert(item);
                refresh();
            });
            snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.mint));
            View fab = getActivity() != null ? getActivity().findViewById(R.id.fab_add) : null;
            if (fab != null) {
                snackbar.setAnchorView(fab);
            }
            snackbar.show();
        }
    }

    @Override
    public void onDataChanged() {
        refresh();
    }
}
