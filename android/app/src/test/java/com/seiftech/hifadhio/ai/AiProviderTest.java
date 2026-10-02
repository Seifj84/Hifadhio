package com.seiftech.hifadhio.ai;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test suite for Phase 10 AI Provider Abstraction and Registry.
 * Master Spec §15.5, §18, ADR-008, & §1207.
 */
public class AiProviderTest {

    private AiRegistry registry;

    @Before
    public void setUp() {
        registry = AiRegistry.getInstance();
        registry.reset();
    }

    @Test
    public void testRegistryPrioritySorting() {
        List<AiProvider> all = registry.getAllProviders();
        assertTrue(all.size() >= 3);

        // Highest priority should be OpenRouterAiProvider (120)
        assertEquals(OpenRouterAiProvider.PROVIDER_ID, all.get(0).getProviderId());
        assertEquals(120, all.get(0).getPriority());

        // Secondary priority should be LocalHeuristicAiProvider (100)
        assertEquals(LocalHeuristicAiProvider.PROVIDER_ID, all.get(1).getProviderId());
        assertEquals(100, all.get(1).getPriority());

        // Tertiary priority should be CloudLlmAiProvider (50)
        assertEquals(CloudLlmAiProvider.PROVIDER_ID, all.get(2).getProviderId());
        assertEquals(50, all.get(2).getPriority());

        // Primary provider should be OpenRouter when configured and available
        AiProvider primary = registry.getPrimaryProvider();
        assertNotNull(primary);
        assertEquals(OpenRouterAiProvider.PROVIDER_ID, primary.getProviderId());

        // When OpenRouter is removed or disabled, fallback primary is LocalHeuristicAiProvider
        registry.unregisterProvider(OpenRouterAiProvider.PROVIDER_ID);
        AiProvider fallbackPrimary = registry.getPrimaryProvider();
        assertNotNull(fallbackPrimary);
        assertEquals(LocalHeuristicAiProvider.PROVIDER_ID, fallbackPrimary.getProviderId());
    }

    @Test
    public void testLocalHeuristicProviderProperties() {
        LocalHeuristicAiProvider provider = new LocalHeuristicAiProvider();
        assertEquals("local_nlp", provider.getProviderId());
        assertEquals("Local On-Device NLP", provider.getDisplayName());
        assertEquals(100, provider.getPriority());
        assertTrue(provider.isAvailable());
        assertTrue(provider.isOffline());
        assertEquals(0.0, provider.estimateCost(5000), 0.0001);
    }

    @Test
    public void testLocalHeuristicEnrichmentSuccess() {
        LocalHeuristicAiProvider provider = new LocalHeuristicAiProvider();

        String context = "TITLE: Modern Android Architecture with Kotlin and Docker\n"
                + "PLATFORM: YouTube\n"
                + "DESCRIPTION: Learn how to build clean architecture apps using Kotlin, SQLite, and deploy with Docker containers on GitHub.\n"
                + "SPOKEN AUDIO TRANSCRIPT:\n"
                + "In this video we explore Clean Architecture. Ensure you separate domain logic from adapters. Always test your providers.\n";

        AiResult result = provider.enrich(101L, context, AiOptions.defaults());
        assertTrue(result.isSuccess());
        assertNotNull(result.getEnrichment());

        AiEnrichment e = result.getEnrichment();
        assertEquals(101L, e.getContentItemId());
        assertEquals("local_nlp", e.getProviderId());
        assertEquals("v1.0.0", e.getPromptVersion());
        assertEquals(0.0, e.getCostUsd(), 0.0001);

        assertFalse(e.getSummaryShort().isEmpty());
        assertFalse(e.getSummaryDetailed().isEmpty());
        assertFalse(e.getKeyPoints().isEmpty());
        assertFalse(e.getTopics().isEmpty());
        assertFalse(e.getSuggestedTags().isEmpty());

        // Check entity recognition
        boolean foundDocker = false;
        boolean foundGithub = false;
        for (AiEntity entity : e.getEntities()) {
            if ("Docker".equalsIgnoreCase(entity.getName())) foundDocker = true;
            if ("Github".equalsIgnoreCase(entity.getName())) foundGithub = true;
        }
        assertTrue("Should detect Docker entity", foundDocker);
        assertTrue("Should detect GitHub entity", foundGithub);

        // Check collection suggestion
        assertEquals("Dev", e.getSuggestedCollection());
    }

    @Test
    public void testLocalHeuristicDesignCollectionRouting() {
        LocalHeuristicAiProvider provider = new LocalHeuristicAiProvider();

        String context = "TITLE: Figma UI Design System Tutorial\n"
                + "DESCRIPTION: Designing beautiful mobile interfaces and color tokens in Figma.\n";

        AiResult result = provider.enrich(102L, context, AiOptions.defaults());
        assertTrue(result.isSuccess());
        AiEnrichment e = result.getEnrichment();
        assertEquals("Design", e.getSuggestedCollection());

        boolean foundFigma = false;
        for (AiEntity ent : e.getEntities()) {
            if ("Figma".equalsIgnoreCase(ent.getName())) foundFigma = true;
        }
        assertTrue("Should detect Figma tool entity", foundFigma);
    }

    @Test
    public void testLocalHeuristicEmptyContextFailsGracefully() {
        LocalHeuristicAiProvider provider = new LocalHeuristicAiProvider();
        AiResult result = provider.enrich(103L, "   ", AiOptions.defaults());
        assertFalse(result.isSuccess());
        assertEquals(AiResult.ERROR_EMPTY_INPUT, result.getErrorCode());
    }

    @Test
    public void testCloudLlmProviderCostAndOfflineBehavior() {
        CloudLlmAiProvider provider = new CloudLlmAiProvider();
        assertEquals("cloud_llm", provider.getProviderId());
        assertEquals(50, provider.getPriority());
        assertFalse(provider.isOffline());

        // Cost estimation: $0.0005 per 1,000 chars
        double cost10k = provider.estimateCost(10000);
        assertEquals(0.005, cost10k, 0.0001);

        // When unconfigured, should report unavailable
        assertFalse(provider.isAvailable());
        AiResult res = provider.enrich(104L, "Some text to enrich", AiOptions.defaults());
        assertFalse(res.isSuccess());
        assertEquals(AiResult.ERROR_PROVIDER_UNAVAILABLE, res.getErrorCode());
    }
}
