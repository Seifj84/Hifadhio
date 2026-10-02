package com.seiftech.hifadhio.ai;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test suite for OpenRouter free cloud AI provider.
 */
public class OpenRouterAiProviderTest {

    @Test
    public void testProviderProperties() {
        OpenRouterAiProvider provider = new OpenRouterAiProvider();
        assertEquals("openrouter", provider.getProviderId());
        assertEquals("OpenRouter AI (Free)", provider.getDisplayName());
        assertEquals(120, provider.getPriority());
        assertFalse(provider.isOffline());
        assertEquals(0.0, provider.estimateCost(5000), 0.0001);
    }

    @Test
    public void testAvailabilityWithDefaultKey() {
        OpenRouterAiProvider provider = new OpenRouterAiProvider();
        assertTrue(provider.isAvailable());
    }

    @Test
    public void testEmptyContextValidation() {
        OpenRouterAiProvider provider = new OpenRouterAiProvider();
        AiResult emptyResult = provider.enrich(1L, "", AiOptions.defaults());
        assertFalse(emptyResult.isSuccess());
        assertEquals(AiResult.ERROR_EMPTY_INPUT, emptyResult.getErrorCode());

        AiResult whitespaceResult = provider.enrich(1L, "   \n\t  ", AiOptions.defaults());
        assertFalse(whitespaceResult.isSuccess());
        assertEquals(AiResult.ERROR_EMPTY_INPUT, whitespaceResult.getErrorCode());
    }

    @Test
    public void testDefaultConfigValues() {
        assertNotNull(AiConfig.DEFAULT_OPENROUTER_KEY);
        assertTrue(AiConfig.DEFAULT_OPENROUTER_KEY.startsWith("sk-or-"));
        assertEquals(73, AiConfig.DEFAULT_OPENROUTER_KEY.length());
        assertEquals("qwen/qwen3.8-27b:free", AiConfig.DEFAULT_FREE_MODEL);
        assertEquals("apodex/apodex-1.1-mini:free", AiConfig.FALLBACK_FREE_MODEL);
    }
}
