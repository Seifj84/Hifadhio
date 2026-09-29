package com.seiftech.hifadhio.transcription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for transcription providers.
 * Adheres strictly to Master Spec ADR-007 (Provider Abstraction).
 */
public class TranscriptionRegistry {
    private static volatile TranscriptionRegistry instance;

    private final Map<String, TranscriptionProvider> providers = new ConcurrentHashMap<>();

    private TranscriptionRegistry() {
        register(new SubtitlesExtractorProvider());
        register(new OfflineSpeechProvider());
        register(new CloudWhisperProvider());
    }

    public static TranscriptionRegistry getInstance() {
        if (instance == null) {
            synchronized (TranscriptionRegistry.class) {
                if (instance == null) {
                    instance = new TranscriptionRegistry();
                }
            }
        }
        return instance;
    }

    public void register(TranscriptionProvider provider) {
        if (provider != null) {
            providers.put(provider.getProviderId(), provider);
        }
    }

    public TranscriptionProvider getProvider(String providerId) {
        if (providerId == null) return null;
        return providers.get(providerId);
    }

    public List<TranscriptionProvider> getAllProviders() {
        List<TranscriptionProvider> list = new ArrayList<>(providers.values());
        Collections.sort(list, new Comparator<TranscriptionProvider>() {
            @Override
            public int compare(TranscriptionProvider o1, TranscriptionProvider o2) {
                return Integer.compare(o2.getPriority(), o1.getPriority());
            }
        });
        return list;
    }

    /**
     * Resolves best available provider based on offline requirements and provider priority.
     */
    public TranscriptionProvider getBestProvider(boolean offlineOnly) {
        for (TranscriptionProvider p : getAllProviders()) {
            if (offlineOnly && !p.isOffline()) {
                continue;
            }
            if (p.isAvailable()) {
                return p;
            }
        }
        return null;
    }

    public double estimateCost(String providerId, long audioDurationMs) {
        TranscriptionProvider p = getProvider(providerId);
        if (p != null) {
            return p.estimateCost(audioDurationMs);
        }
        return 0.0;
    }
}
