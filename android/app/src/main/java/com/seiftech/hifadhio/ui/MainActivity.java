package com.seiftech.hifadhio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.seiftech.hifadhio.R;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.data.UrlNormalizer;

public class MainActivity extends AppCompatActivity {

    private ContentDb db;
    private BottomNavigationView bottomNav;
    private ExtendedFloatingActionButton fabAdd;
    private final HomeFragment homeFragment = new HomeFragment();
    private final InboxFragment inboxFragment = new InboxFragment();
    private final LibraryFragment libraryFragment = new LibraryFragment();
    private final AskFragment askFragment = new AskFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new ContentDb(this);
        com.seiftech.hifadhio.ai.AiConfig.getInstance(this);
        bottomNav = findViewById(R.id.bottom_nav);
        fabAdd = findViewById(R.id.fab_add);

        // Apply system status bar & cutout insets to AppBarLayout so toolbar never overlaps clock/notch
        AppBarLayout appBar = findViewById(R.id.app_bar);
        if (appBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(
                        WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout()
                );
                v.setPadding(0, insets.top, 0, 0);
                return windowInsets;
            });
        }

        // Dynamically adjust container and FAB margin based on measured BottomNav height + insets
        bottomNav.post(() -> {
            int navHeight = bottomNav.getHeight();
            if (navHeight > 0) {
                View container = findViewById(R.id.fragment_container);
                if (container != null && container.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
                    CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) container.getLayoutParams();
                    lp.bottomMargin = navHeight;
                    container.setLayoutParams(lp);
                }
                if (fabAdd != null && fabAdd.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
                    CoordinatorLayout.LayoutParams fabLp = (CoordinatorLayout.LayoutParams) fabAdd.getLayoutParams();
                    int extra = (int) (16 * getResources().getDisplayMetrics().density);
                    fabLp.bottomMargin = navHeight + extra;
                    fabAdd.setLayoutParams(fabLp);
                }
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, homeFragment)
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment target = homeFragment;
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                target = homeFragment;
                if (fabAdd != null) fabAdd.extend();
            } else if (id == R.id.nav_inbox) {
                target = inboxFragment;
                if (fabAdd != null) fabAdd.shrink();
            } else if (id == R.id.nav_library) {
                target = libraryFragment;
                if (fabAdd != null) fabAdd.shrink();
            } else if (id == R.id.nav_ask) {
                target = askFragment;
                if (fabAdd != null) fabAdd.shrink();
            } else if (id == R.id.nav_profile) {
                target = profileFragment;
                if (fabAdd != null) fabAdd.shrink();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, target)
                    .commit();
            return true;
        });

        fabAdd.setOnClickListener(v -> openSaveSheet(""));

        handleIncomingShareIntent(getIntent());
        updateInboxBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateInboxBadge();
    }

    public void updateInboxBadge() {
        if (bottomNav == null || db == null) return;
        int count = db.getInboxCount();
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_inbox);
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
            badge.setBackgroundColor(ContextCompat.getColor(this, R.color.mint));
            badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.primary_navy));
            badge.setVerticalOffset(4);
            badge.setHorizontalOffset(4);
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingShareIntent(intent);
    }

    private void handleIncomingShareIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("text/")) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && !sharedText.trim().isEmpty()) {
                String extracted = UrlNormalizer.extractUrl(sharedText);
                openSaveSheet(extracted);
            }
        }
    }

    private void openSaveSheet(String initialUrl) {
        SaveLinkBottomSheet sheet = SaveLinkBottomSheet.newInstance(initialUrl);
        sheet.setOnSavedListener(item -> {
            updateInboxBadge();
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof HomeFragment) {
                ((HomeFragment) current).refresh();
            } else if (current instanceof InboxFragment) {
                ((InboxFragment) current).refresh();
            } else if (current instanceof LibraryFragment) {
                ((LibraryFragment) current).refresh();
            }

            String targetCollection = item.getCollectionName() != null ? item.getCollectionName() : "Inbox";
            Snackbar snackbar = Snackbar.make(
                    findViewById(R.id.coordinator_main),
                    "Saved to " + targetCollection,
                    Snackbar.LENGTH_LONG
            );
            if ("Inbox".equalsIgnoreCase(targetCollection)) {
                snackbar.setAction("View in Inbox", v -> {
                    bottomNav.setSelectedItemId(R.id.nav_inbox);
                });
                snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.mint));
            }
            snackbar.setAnchorView(fabAdd);
            snackbar.show();
        });
        sheet.show(getSupportFragmentManager(), "save_link_sheet");
    }
}
