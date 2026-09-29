package com.seiftech.hifadhio.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for all content extraction adapters.
 * Implements the Registry pattern satisfying the Master Spec Phase 5 exit criterion:
 * "Adding a new platform adapter does not require modifying core content-domain logic."
 */
public class ContentAdapterRegistry {
    private static volatile ContentAdapterRegistry instance;
    private final List<ContentExtractorAdapter> adapters = new ArrayList<>();
    private ContentExtractorAdapter defaultFallbackAdapter;

    private ContentAdapterRegistry() {
        initDefaults();
    }

    public static ContentAdapterRegistry getInstance() {
        if (instance == null) {
            synchronized (ContentAdapterRegistry.class) {
                if (instance == null) {
                    instance = new ContentAdapterRegistry();
                }
            }
        }
        return instance;
    }

    private void initDefaults() {
        defaultFallbackAdapter = new GenericWebAdapter();
        registerAdapter(defaultFallbackAdapter);
        registerAdapter(new FacebookAdapter());
    }

    /**
     * Register a new adapter into the registry.
     * Automatically sorted by priority in descending order.
     */
    public synchronized void registerAdapter(ContentExtractorAdapter adapter) {
        if (adapter == null) return;
        // Avoid duplicate registrations of the same platform ID
        adapters.removeIf(a -> a.getPlatformId().equalsIgnoreCase(adapter.getPlatformId()));
        adapters.add(adapter);
        adapters.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    /**
     * Unregister an adapter by platform ID.
     */
    public synchronized boolean unregisterAdapter(String platformId) {
        if (platformId == null) return false;
        return adapters.removeIf(a -> a.getPlatformId().equalsIgnoreCase(platformId));
    }

    /**
     * Find the best matching adapter for the given URL.
     * Evaluates registered adapters in descending priority order.
     * Falls back to GenericWebAdapter if no specific adapter handles the URL.
     */
    public synchronized ContentExtractorAdapter getAdapterForUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return defaultFallbackAdapter;
        }

        for (ContentExtractorAdapter adapter : adapters) {
            try {
                if (adapter != defaultFallbackAdapter && adapter.canHandle(url)) {
                    return adapter;
                }
            } catch (Exception ignored) {
                // If canHandle throws, skip to next adapter
            }
        }
        return defaultFallbackAdapter;
    }

    /**
     * Retrieve an adapter by its unique platform ID.
     */
    public synchronized ContentExtractorAdapter getAdapterById(String platformId) {
        if (platformId == null) return null;
        for (ContentExtractorAdapter a : adapters) {
            if (a.getPlatformId().equalsIgnoreCase(platformId)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Get an unmodifiable copy of all currently registered adapters.
     */
    public synchronized List<ContentExtractorAdapter> getRegisteredAdapters() {
        return Collections.unmodifiableList(new ArrayList<>(adapters));
    }

    /**
     * Run health checks on all registered adapters.
     */
    public synchronized Map<String, AdapterHealth> checkAllHealth() {
        Map<String, AdapterHealth> results = new HashMap<>();
        for (ContentExtractorAdapter adapter : adapters) {
            try {
                results.put(adapter.getPlatformId(), adapter.checkHealth());
            } catch (Exception e) {
                results.put(adapter.getPlatformId(), AdapterHealth.degraded("Check failed: " + e.getMessage()));
            }
        }
        return results;
    }

    /**
     * Reset registry back to initial default state (used for testing).
     */
    public synchronized void reset() {
        adapters.clear();
        initDefaults();
    }
}
