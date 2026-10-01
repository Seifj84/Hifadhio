package com.seiftech.hifadhio.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe provider registry managing prioritized AI enrichment engines.
 * Master Spec §1207 & ADR-008.
 */
public class AiRegistry {

    private static volatile AiRegistry sInstance;
    private final Map<String, AiProvider> providers = new ConcurrentHashMap<>();

    private AiRegistry() {
        // Register default built-in providers
        registerProvider(new LocalHeuristicAiProvider());
        registerProvider(new CloudLlmAiProvider());
    }

    public static AiRegistry getInstance() {
        if (sInstance == null) {
            synchronized (AiRegistry.class) {
                if (sInstance == null) {
                    sInstance = new AiRegistry();
                }
            }
        }
        return sInstance;
    }

    public void registerProvider(AiProvider provider) {
        if (provider != null && provider.getProviderId() != null) {
            providers.put(provider.getProviderId(), provider);
        }
    }

    public void unregisterProvider(String providerId) {
        if (providerId != null) {
            providers.remove(providerId);
        }
    }

    public AiProvider getProvider(String providerId) {
        if (providerId == null) return null;
        return providers.get(providerId);
    }

    public List<AiProvider> getAllProviders() {
        List<AiProvider> list = new ArrayList<>(providers.values());
        Collections.sort(list, (p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority()));
        return list;
    }

    public AiProvider getPrimaryProvider() {
        List<AiProvider> sorted = getAllProviders();
        for (AiProvider p : sorted) {
            if (p.isAvailable()) {
                return p;
            }
        }
        return !sorted.isEmpty() ? sorted.get(0) : null;
    }

    public void reset() {
        providers.clear();
        registerProvider(new LocalHeuristicAiProvider());
        registerProvider(new CloudLlmAiProvider());
    }
}
