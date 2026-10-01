package com.seiftech.hifadhio.ai;

import com.seiftech.hifadhio.data.ContentItem;
import com.seiftech.hifadhio.ocr.OcrRecord;
import com.seiftech.hifadhio.transcription.Transcript;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test suite for Phase 10 PromptManager (Prompt Versioning, Context Assembly, Schema Validation).
 * Master Spec §15.5, §18, & §1208–§1209.
 */
public class PromptManagerTest {

    @Test
    public void testAssembleContextMultiSource() {
        ContentItem item = new ContentItem();
        item.setTitle("Kotlin Multiplatform in Action");
        item.setPlatform("YouTube");
        item.setCaption("Sharing business logic across Android and iOS.");
        item.setNotes("Review repository pattern implementation.");
        item.setUrl("https://youtube.com/watch?v=kmp123");

        Transcript transcript = new Transcript();
        transcript.setFullText("Today we look at Kotlin Multiplatform and how SQLDelight handles local caching.");

        OcrRecord ocr = new OcrRecord();
        ocr.setFullText("SLIDE 1: KMP ARCHITECTURE\nSLIDE 2: SHARED STORAGE");

        String context = PromptManager.assembleContext(item, transcript, ocr);
        assertNotNull(context);
        assertTrue(context.contains("TITLE: Kotlin Multiplatform in Action"));
        assertTrue(context.contains("PLATFORM: YouTube"));
        assertTrue(context.contains("DESCRIPTION/CAPTION: Sharing business logic across Android and iOS."));
        assertTrue(context.contains("USER NOTES: Review repository pattern implementation."));
        assertTrue(context.contains("SPOKEN AUDIO TRANSCRIPT:"));
        assertTrue(context.contains("SQLDelight handles local caching"));
        assertTrue(context.contains("ON-SCREEN VISUAL OCR TEXT:"));
        assertTrue(context.contains("SLIDE 1: KMP ARCHITECTURE"));
    }

    @Test
    public void testAssembleContextNullHandling() {
        String context = PromptManager.assembleContext(null, null, null);
        assertNotNull(context);
        assertTrue(context.isEmpty());

        ContentItem item = new ContentItem();
        item.setTitle("Solo Title");
        String itemOnly = PromptManager.assembleContext(item, null, null);
        assertTrue(itemOnly.contains("TITLE: Solo Title"));
        assertFalse(itemOnly.contains("SPOKEN AUDIO TRANSCRIPT:"));
        assertFalse(itemOnly.contains("ON-SCREEN VISUAL OCR TEXT:"));
    }

    @Test
    public void testBuildPromptContainsVersioningAndSchema() {
        AiOptions options = new AiOptions();
        options.setPromptVersion("v1.0.0");

        String prompt = PromptManager.buildPrompt("Sample content text here", options);
        assertNotNull(prompt);
        assertTrue(prompt.contains("Prompt v1.0.0"));
        assertTrue(prompt.contains("\"summary_short\""));
        assertTrue(prompt.contains("\"summary_detailed\""));
        assertTrue(prompt.contains("\"key_points\""));
        assertTrue(prompt.contains("\"suggested_collection\""));
        assertTrue(prompt.contains("Sample content text here"));
    }

    @Test
    public void testValidateSchemaCompleteObject() throws Exception {
        JSONObject json = new JSONObject();
        json.put("summary_short", "Short overview.");
        json.put("summary_detailed", "Longer detailed analysis of the saved item.");

        JSONArray kp = new JSONArray();
        kp.put("Point 1");
        kp.put("Point 2");
        json.put("key_points", kp);

        JSONArray tp = new JSONArray();
        tp.put("Topic A");
        json.put("topics", tp);

        JSONArray st = new JSONArray();
        st.put("tag1");
        json.put("suggested_tags", st);

        JSONArray ent = new JSONArray();
        JSONObject e1 = new JSONObject();
        e1.put("name", "Docker");
        e1.put("type", "tool");
        e1.put("evidence", "uses docker");
        ent.put(e1);
        json.put("entities", ent);

        assertTrue(PromptManager.validateSchema(json));
    }
}
