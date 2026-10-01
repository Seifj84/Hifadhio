package com.seiftech.hifadhio.ocr;

import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class OcrProviderTest {

    private OcrRegistry registry;

    @Before
    public void setUp() {
        registry = OcrRegistry.getInstance();
        registry.resetForTesting();
    }

    @Test
    public void testRegistryPriorityResolution() {
        List<OcrProvider> all = registry.getAllProviders();
        assertTrue(all.size() >= 2);

        // Highest priority should be on-device (Priority 100)
        assertEquals("on_device_ocr", all.get(0).getProviderId());
        assertEquals(100, all.get(0).getPriority());

        // Second should be cloud vision (Priority 50)
        assertEquals("cloud_vision", all.get(1).getProviderId());
        assertEquals(50, all.get(1).getPriority());

        // Best provider for default options
        OcrProvider best = registry.getBestProvider(OcrOptions.createDefault());
        assertNotNull(best);
        assertEquals("on_device_ocr", best.getProviderId());
    }

    @Test
    public void testOnDeviceOcrProviderExtraction() {
        OnDeviceOcrProvider provider = new OnDeviceOcrProvider();
        assertTrue(provider.isAvailable());
        assertTrue(provider.isOffline());
        assertEquals(0.0, provider.estimateCost(10), 0.0001);

        List<OcrFrame> frames = new ArrayList<>();
        frames.add(new OcrFrame(0, 0L, "First Slide Title"));
        frames.add(new OcrFrame(1, 5000L, "Second Slide Bullet Point"));

        OcrResult result = provider.processFrames(101L, frames, OcrOptions.createDefault());
        assertNotNull(result);
        assertTrue(result.isSuccess());

        OcrRecord record = result.getRecord();
        assertNotNull(record);
        assertEquals(101L, record.getContentItemId());
        assertEquals("on_device_ocr", record.getProviderId());
        assertEquals(0.0, record.getCostUsd(), 0.0001);
        assertTrue(record.getFullText().contains("First Slide Title"));
        assertTrue(record.getFullText().contains("Second Slide Bullet Point"));
    }

    @Test
    public void testCloudVisionCostEstimationAndOfflineEnforcement() {
        CloudVisionOcrProvider cloud = new CloudVisionOcrProvider();
        assertFalse(cloud.isOffline());

        // 10 frames * $0.0015 = $0.015
        assertEquals(0.015, cloud.estimateCost(10), 0.0001);

        List<OcrFrame> frames = new ArrayList<>();
        frames.add(new OcrFrame(0, 0L, "Infographic Headline"));

        // When offline-only is requested, cloud vision must fail with PROVIDER_UNAVAILABLE
        OcrOptions offlineOptions = OcrOptions.createDefault();
        offlineOptions.setOfflineOnly(true);

        OcrResult failResult = cloud.processFrames(102L, frames, offlineOptions);
        assertNotNull(failResult);
        assertFalse(failResult.isSuccess());
        assertEquals(OcrResult.ERROR_PROVIDER_UNAVAILABLE, failResult.getErrorCode());

        // When offline-only is false, cloud vision succeeds
        OcrOptions onlineOptions = OcrOptions.createDefault();
        onlineOptions.setOfflineOnly(false);

        OcrResult passResult = cloud.processFrames(102L, frames, onlineOptions);
        assertNotNull(passResult);
        assertTrue(passResult.isSuccess());
        assertEquals(0.0015, passResult.getRecord().getCostUsd(), 0.0001);
    }

    @Test
    public void testEmptyFramesHandling() {
        OnDeviceOcrProvider provider = new OnDeviceOcrProvider();
        OcrResult emptyResult = provider.processFrames(103L, new ArrayList<OcrFrame>(), OcrOptions.createDefault());
        assertNotNull(emptyResult);
        assertFalse(emptyResult.isSuccess());
        assertEquals(OcrResult.ERROR_NO_FRAMES_EXTRACTED, emptyResult.getErrorCode());
    }
}
