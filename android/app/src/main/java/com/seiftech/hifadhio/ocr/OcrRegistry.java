package com.seiftech.hifadhio.ocr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry managing OCR visual text extraction providers.
 * Selects highest-priority available provider for each execution request.
 */
public class OcrRegistry {
    private static volatile OcrRegistry instance;
    private final List<OcrProvider> providers = new CopyOnWriteArrayList<>();

    private OcrRegistry() {
        // Register default providers in priority order
        registerProvider(new OnDeviceOcrProvider());
        registerProvider(new CloudVisionOcrProvider());
    }

    public static OcrRegistry getInstance() {
        if (instance == null) {
            synchronized (OcrRegistry.class) {
                if (instance == null) {
                    instance = new OcrRegistry();
                }
            }
        }
        return instance;
    }

    public void registerProvider(OcrProvider provider) {
        if (provider == null) return;
        // Avoid duplicate provider IDs
        for (OcrProvider existing : providers) {
            if (existing.getProviderId().equalsIgnoreCase(provider.getProviderId())) {
                providers.remove(existing);
                break;
            }
        }
        providers.add(provider);
        sortProviders();
    }

    private void sortProviders() {
        List<OcrProvider> list = new ArrayList<>(providers);
        Collections.sort(list, new Comparator<OcrProvider>() {
            @Override
            public int compare(OcrProvider a, OcrProvider b) {
                return Integer.compare(b.getPriority(), a.getPriority()); // Descending
            }
        });
        providers.clear();
        providers.addAll(list);
    }

    public OcrProvider getBestProvider(OcrOptions options) {
        for (OcrProvider p : providers) {
            if (options != null && options.isOfflineOnly() && !p.isOffline()) {
                continue;
            }
            if (p.isAvailable()) {
                return p;
            }
        }
        return null;
    }

    public OcrProvider getProvider(String providerId) {
        if (providerId == null) return null;
        for (OcrProvider p : providers) {
            if (p.getProviderId().equalsIgnoreCase(providerId.trim())) {
                return p;
            }
        }
        return null;
    }

    public List<OcrProvider> getAllProviders() {
        return Collections.unmodifiableList(new ArrayList<>(providers));
    }

    public void resetForTesting() {
        providers.clear();
        registerProvider(new OnDeviceOcrProvider());
        registerProvider(new CloudVisionOcrProvider());
    }
}
