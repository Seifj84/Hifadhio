package com.seiftech.hifadhio.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.seiftech.hifadhio.R;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.data.ContentItem;
import com.seiftech.hifadhio.data.PlatformDetector;
import com.seiftech.hifadhio.data.TimeUtils;
import java.util.ArrayList;
import java.util.List;

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onViewDetail(ContentItem item);
        void onEdit(ContentItem item);
        void onDelete(ContentItem item);
        void onDataChanged();
    }

    private final Context context;
    private final ContentDb db;
    private final OnItemActionListener listener;
    private List<ContentItem> items = new ArrayList<>();

    public ContentAdapter(Context context, ContentDb db, OnItemActionListener listener) {
        this.context = context;
        this.db = db;
        this.listener = listener;
    }

    public void setItems(List<ContentItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_content_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ContentItem item = items.get(position);

        h.tvPlatform.setText(item.getPlatform());
        int colorRes = PlatformDetector.getPlatformColorRes(item.getPlatform());
        int platformColor = ContextCompat.getColor(context, colorRes);
        h.tvPlatform.setTextColor(platformColor);

        h.tvCollection.setText(item.getCollectionName() + " ▾");
        h.tvCollection.setOnClickListener(v -> showMoveDialog(item));

        h.tvDate.setText(TimeUtils.formatRelativeTime(item.getSavedAt()));

        if (h.tvStatus != null) {
            String status = item.getStatus();
            if ("PROCESSING".equalsIgnoreCase(status) || "QUEUED".equalsIgnoreCase(status)) {
                h.tvStatus.setVisibility(View.VISIBLE);
                h.tvStatus.setText("Processing");
                h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.blue));
            } else if ("RETRYING".equalsIgnoreCase(status)) {
                h.tvStatus.setVisibility(View.VISIBLE);
                h.tvStatus.setText("Retrying");
                h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.amber));
            } else if ("FAILED".equalsIgnoreCase(status)) {
                h.tvStatus.setVisibility(View.VISIBLE);
                h.tvStatus.setText("Could not analyze");
                h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            } else {
                h.tvStatus.setVisibility(View.GONE);
            }
        }

        h.tvTitle.setText(item.getDisplayTitle());

        String domainPreview = item.getDomainPreview();
        h.tvUrl.setText(domainPreview != null && !domainPreview.isEmpty() ? domainPreview : item.getCanonicalUrl());

        if (item.getCaption() != null && !item.getCaption().trim().isEmpty()) {
            h.tvCaption.setVisibility(View.VISIBLE);
            h.tvCaption.setText(item.getCaption().trim());
        } else {
            h.tvCaption.setVisibility(View.GONE);
        }

        if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
            h.layoutNotes.setVisibility(View.VISIBLE);
            h.tvNotes.setText("Why saved: " + item.getNotes().trim());
        } else {
            h.layoutNotes.setVisibility(View.GONE);
        }

        h.btnFav.setImageResource(item.isFavorite() ? R.drawable.ic_star : R.drawable.ic_star_outline);
        h.btnFav.setOnClickListener(v -> {
            item.setFavorite(!item.isFavorite());
            db.update(item);
            notifyItemChanged(position);
        });

        h.btnOpen.setOnClickListener(v -> openUrl(item.getUrl()));

        if (h.btnOrganize != null) {
            h.btnOrganize.setOnClickListener(v -> showMoveDialog(item));
        }

        h.btnMore.setOnClickListener(v -> showPopupMenu(v, item));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetail(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void showPopupMenu(View anchor, ContentItem item) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(context, R.style.ThemeOverlay_Hifadhio_Popup);
        PopupMenu popup = new PopupMenu(wrapper, anchor, Gravity.END);
        popup.inflate(R.menu.card_item_menu);

        MenuItem deleteItem = popup.getMenu().findItem(R.id.action_delete);
        if (deleteItem != null) {
            SpannableString span = new SpannableString(deleteItem.getTitle());
            int dangerColor = ContextCompat.getColor(context, R.color.danger);
            span.setSpan(new ForegroundColorSpan(dangerColor), 0, span.length(), 0);
            deleteItem.setTitle(span);
        }

        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.action_view_detail) {
                if (listener != null) listener.onViewDetail(item);
                return true;
            } else if (id == R.id.action_share) {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, item.getUrl());
                context.startActivity(Intent.createChooser(send, "Share Link"));
                return true;
            } else if (id == R.id.action_move) {
                showMoveDialog(item);
                return true;
            } else if (id == R.id.action_edit) {
                if (listener != null) listener.onEdit(item);
                return true;
            } else if (id == R.id.action_delete) {
                if (listener != null) listener.onDelete(item);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showMoveDialog(ContentItem item) {
        List<String> rawCollections = db.getCollections();
        List<String> collections = new ArrayList<>();
        String[] presets = new String[]{"Inbox", "Articles", "Research", "Inspiration", "Work", "Personal"};
        for (String p : presets) {
            if (!collections.contains(p)) collections.add(p);
        }
        for (String c : rawCollections) {
            if (c != null && !c.trim().isEmpty() && !collections.contains(c.trim())) {
                collections.add(c.trim());
            }
        }

        List<String> options = new ArrayList<>(collections);
        options.add("+ New Collection...");

        int currentSelection = collections.indexOf(item.getCollectionName());
        if (currentSelection < 0) currentSelection = 0;

        new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Hifadhio_Dialog)
                .setTitle("Move to Collection")
                .setSingleChoiceItems(options.toArray(new String[0]), currentSelection, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == options.size() - 1) {
                        showCreateCollectionDialog(item);
                    } else {
                        String target = options.get(which);
                        db.moveToCollection(item.getId(), target);
                        Toast.makeText(context, "Moved to " + target, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onDataChanged();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreateCollectionDialog(ContentItem item) {
        final android.widget.EditText input = new android.widget.EditText(context);
        input.setHint("e.g. Design Systems");
        input.setSingleLine(true);
        input.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        input.setHintTextColor(ContextCompat.getColor(context, R.color.text_secondary));

        android.widget.FrameLayout container = new android.widget.FrameLayout(context);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (20 * context.getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Hifadhio_Dialog)
                .setTitle("New Collection")
                .setMessage("Enter collection name for this item:")
                .setView(container)
                .setPositiveButton("Move", (dialog, which) -> {
                    String name = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!name.isEmpty()) {
                        db.moveToCollection(item.getId(), name);
                        Toast.makeText(context, "Moved to " + name, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onDataChanged();
                    } else {
                        Toast.makeText(context, "Collection name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open link in browser", Toast.LENGTH_SHORT).show();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvPlatform, tvCollection, tvStatus, tvDate, tvTitle, tvUrl, tvCaption, tvNotes;
        final View layoutNotes;
        final MaterialButton btnOpen, btnOrganize;
        final ImageView btnFav, btnMore;

        ViewHolder(View v) {
            super(v);
            tvPlatform = v.findViewById(R.id.tv_card_platform);
            tvCollection = v.findViewById(R.id.tv_card_collection);
            tvStatus = v.findViewById(R.id.tv_card_status);
            tvDate = v.findViewById(R.id.tv_card_date);
            tvTitle = v.findViewById(R.id.tv_card_title);
            tvUrl = v.findViewById(R.id.tv_card_url);
            tvCaption = v.findViewById(R.id.tv_card_caption);
            tvNotes = v.findViewById(R.id.tv_card_notes);
            layoutNotes = v.findViewById(R.id.layout_card_notes);
            btnOpen = v.findViewById(R.id.btn_card_open);
            btnOrganize = v.findViewById(R.id.btn_card_organize);
            btnFav = v.findViewById(R.id.btn_card_fav);
            btnMore = v.findViewById(R.id.btn_card_more);
        }
    }
}
