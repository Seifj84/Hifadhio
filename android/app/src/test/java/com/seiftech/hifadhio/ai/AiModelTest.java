package com.seiftech.hifadhio.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test suite for Phase 10 AI Enrichment domain models and strict schema serialization.
 * Master Spec §15.5, §18, & §1208.
 */
public class AiModelTest {

    @Test
    public void testAiEntityModel() throws Exception {
        AiEntity entity = new AiEntity("Docker", "tool", "deploy application using docker container");
        assertEquals("Docker", entity.getName());
        assertEquals("tool", entity.getType());
        assertEquals("deploy application using docker container", entity.getEvidence());

        JSONObject json = entity.toJson();
        assertEquals("Docker", json.getString("name"));
        assertEquals("tool", json.getString("type"));
        assertEquals("deploy application using docker container", json.getString("evidence"));

        AiEntity parsed = AiEntity.fromJson(json);
        assertNotNull(parsed);
        assertEquals(entity, parsed);
        assertEquals("Docker (tool)", entity.toString());
    }

    @Test
    public void testAiEnrichmentModelSerialization() throws Exception {
        AiEnrichment e = new AiEnrichment(42L, "Brief summary.", "Detailed summary of architecture.");
        e.setKeyPoints(Arrays.asList("Modular design", "Offline-first"));
        e.setTopics(Arrays.asList("Architecture", "Android"));
        e.setSuggestedTags(Arrays.asList("android", "architecture"));
        e.setEntities(Arrays.asList(
                new AiEntity("GitHub", "tool", "hosted on github"),
                new AiEntity("OpenAI", "organization", "api provided by openai")
        ));
        e.setActionItems(Arrays.asList("Review clean architecture principles"));
        e.setSuggestedCollection("Dev");
        e.setPromptVersion("v1.0.0");
        e.setProviderId("local_nlp");
        e.setModel("heuristic-v1");
        e.setCostUsd(0.0);

        String schemaJson = e.toStrictSchemaJson();
        assertNotNull(schemaJson);
        JSONObject obj = new JSONObject(schemaJson);

        assertTrue(PromptManager.validateSchema(obj));
        assertEquals("Brief summary.", obj.getString("summary_short"));
        assertEquals("Detailed summary of architecture.", obj.getString("summary_detailed"));
        assertEquals(2, obj.getJSONArray("key_points").length());
        assertEquals(2, obj.getJSONArray("topics").length());
        assertEquals(2, obj.getJSONArray("suggested_tags").length());
        assertEquals(2, obj.getJSONArray("entities").length());
        assertEquals("Dev", obj.getString("suggested_collection"));
        assertEquals("v1.0.0", obj.getString("prompt_version"));

        AiEnrichment parsed = AiEnrichment.fromStrictSchemaJson(42L, obj);
        assertNotNull(parsed);
        assertEquals(42L, parsed.getContentItemId());
        assertEquals("Brief summary.", parsed.getSummaryShort());
        assertEquals("Detailed summary of architecture.", parsed.getSummaryDetailed());
        assertEquals(2, parsed.getKeyPoints().size());
        assertEquals(2, parsed.getEntities().size());
        assertEquals("Dev", parsed.getSuggestedCollection());
        assertEquals("v1.0.0", parsed.getPromptVersion());
    }

    @Test
    public void testStrictSchemaValidationRejectsInvalid() throws Exception {
        // Missing summary_short
        JSONObject invalid1 = new JSONObject();
        invalid1.put("summary_detailed", "Detailed...");
        invalid1.put("key_points", new JSONArray().put("Point 1"));
        invalid1.put("topics", new JSONArray().put("Topic 1"));
        invalid1.put("suggested_tags", new JSONArray().put("tag1"));
        assertFalse(PromptManager.validateSchema(invalid1));

        // Missing key_points
        JSONObject invalid2 = new JSONObject();
        invalid2.put("summary_short", "Short...");
        invalid2.put("summary_detailed", "Detailed...");
        invalid2.put("topics", new JSONArray().put("Topic 1"));
        invalid2.put("suggested_tags", new JSONArray().put("tag1"));
        assertFalse(PromptManager.validateSchema(invalid2));

        // Empty summaries
        JSONObject invalid3 = new JSONObject();
        invalid3.put("summary_short", "   ");
        invalid3.put("summary_detailed", "Detailed...");
        invalid3.put("key_points", new JSONArray().put("Point 1"));
        invalid3.put("topics", new JSONArray().put("Topic 1"));
        invalid3.put("suggested_tags", new JSONArray().put("tag1"));
        assertFalse(PromptManager.validateSchema(invalid3));

        // Malformed entity (missing name)
        JSONObject invalidEntity = new JSONObject();
        invalidEntity.put("summary_short", "Short...");
        invalidEntity.put("summary_detailed", "Detailed...");
        invalidEntity.put("key_points", new JSONArray().put("Point 1"));
        invalidEntity.put("topics", new JSONArray().put("Topic 1"));
        invalidEntity.put("suggested_tags", new JSONArray().put("tag1"));
        JSONArray ent = new JSONArray();
        JSONObject badObj = new JSONObject();
        badObj.put("type", "tool");
        ent.put(badObj);
        invalidEntity.put("entities", ent);
        assertFalse(PromptManager.validateSchema(invalidEntity));
    }

    @Test
    public void testStringListJsonConversions() {
        List<String> original = Arrays.asList("Alpha", "Beta", "Gamma");
        String json = AiEnrichment.stringListToJson(original);
        List<String> parsed = AiEnrichment.jsonToStringList(json);
        assertEquals(3, parsed.size());
        assertEquals("Alpha", parsed.get(0));
        assertEquals("Beta", parsed.get(1));
        assertEquals("Gamma", parsed.get(2));

        // Empty handling
        assertEquals("[]", AiEnrichment.stringListToJson(null));
        assertTrue(AiEnrichment.jsonToStringList(null).isEmpty());
        assertTrue(AiEnrichment.jsonToStringList("invalid json").isEmpty());
    }

    @Test
    public void testAiOptionsDefaultsAndReprocess() {
        AiOptions def = AiOptions.defaults();
        assertEquals("v1.0.0", def.getPromptVersion());
        assertFalse(def.isForceReprocess());
        assertTrue(def.isIncludeEntities());
        assertTrue(def.isIncludeActionItems());

        AiOptions reproc = AiOptions.reprocess();
        assertTrue(reproc.isForceReprocess());
        assertEquals("v1.0.0", reproc.getPromptVersion());
    }

    @Test
    public void testAiResult() {
        AiEnrichment e = new AiEnrichment(10L, "Short", "Detailed");
        AiResult success = AiResult.success(e, 150L, 0.001);
        assertTrue(success.isSuccess());
        assertEquals(e, success.getEnrichment());
        assertEquals(150L, success.getLatencyMs());
        assertEquals(0.001, success.getCostUsd(), 0.0001);

        AiResult failure = AiResult.failure(AiResult.ERROR_AI_ENRICHMENT_FAILED, "Timeout occurred");
        assertFalse(failure.isSuccess());
        assertNull(failure.getEnrichment());
        assertEquals(AiResult.ERROR_AI_ENRICHMENT_FAILED, failure.getErrorCode());
        assertEquals("Timeout occurred", failure.getErrorMessage());
    }
}
