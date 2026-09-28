package com.seiftech.hifadhio.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.seiftech.hifadhio.R;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.data.ContentItem;
import com.seiftech.hifadhio.data.PlatformDetector;
import java.util.ArrayList;
import java.util.List;

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder> {

    public interface OnItemActionListener {
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

        h.tvCollection.setText(item.getCollectionName());
        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                item.getSavedAt(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        );
        h.tvDate.setText(relativeTime);

        h.tvTitle.setText(item.getDisplayTitle());
        h.tvUrl.setText(item.getCanonicalUrl());

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

        h.btnShare.setOnClickListener(v -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, item.getUrl());
            context.startActivity(Intent.createChooser(send, "Share Link"));
        });

        h.btnMore.setOnClickListener(v -> showPopupMenu(v, item));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void showPopupMenu(View anchor, ContentItem item) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.inflate(R.menu.card_item_menu);
        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.action_open) {
                openUrl(item.getUrl());
                return true;
            } else if (id == R.id.action_share) {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, item.getUrl());
                context.startActivity(Intent.createChooser(send, "Share Link"));
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

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open link in browser", Toast.LENGTH_SHORT).show();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvPlatform, tvCollection, tvDate, tvTitle, tvUrl, tvCaption, tvNotes;
        final View layoutNotes;
        final MaterialButton btnOpen, btnShare;
        final ImageView btnFav, btnMore;

        ViewHolder(View v) {
            super(v);
            tvPlatform = v.findViewById(R.id.tv_card_platform);
            tvCollection = v.findViewById(R.id.tv_card_collection);
            tvDate = v.findViewById(R.id.tv_card_date);
            tvTitle = v.findViewById(R.id.tv_card_title);
            tvUrl = v.findViewById(R.id.tv_card_url);
            tvCaption = v.findViewById(R.id.tv_card_caption);
            tvNotes = v.findViewById(R.id.tv_card_notes);
            layoutNotes = v.findViewById(R.id.layout_card_notes);
            btnOpen = v.findViewById(R.id.btn_card_open);
            btnShare = v.findViewById(R.id.btn_card_share);
            btnFav = v.findViewById(R.id.btn_card_fav);
            btnMore = v.findViewById(R.id.btn_card_more);
        }
    }
}
