package com.seiftech.hifadhio.ai;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing AI enrichment data for a content item.
 * Master Spec §15.5, §18, §1203–§1221.
 * Strict separation: Generated outputs must identify themselves as generated
 * and must not overwrite source text (§790).
 */
public class AiEnrichment implements Serializable {

    private long id;
    private long contentItemId;
    private String summaryShort = "";
    private String summaryDetailed = "";
    private List<String> keyPoints = new ArrayList<>();
    private List<String> topics = new ArrayList<>();
    private List<String> suggestedTags = new ArrayList<>();
    private List<AiEntity> entities = new ArrayList<>();
    private List<String> actionItems = new ArrayList<>();
    private String suggestedCollection = "";
    private String providerId = "";
    private String model = "";
    private String promptVersion = "v1.0.0";
    private double costUsd = 0.0;
    private String rawJson = "{}";
    private long createdAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    public AiEnrichment() {}

    public AiEnrichment(long contentItemId, String summaryShort, String summaryDetailed) {
        this.contentItemId = contentItemId;
        this.summaryShort = summaryShort != null ? summaryShort.trim() : "";
        this.summaryDetailed = summaryDetailed != null ? summaryDetailed.trim() : "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getContentItemId() { return contentItemId; }
    public void setContentItemId(long contentItemId) { this.contentItemId = contentItemId; }

    public String getSummaryShort() { return summaryShort != null ? summaryShort : ""; }
    public void setSummaryShort(String summaryShort) { this.summaryShort = summaryShort; }

    public String getSummaryDetailed() { return summaryDetailed != null ? summaryDetailed : ""; }
    public void setSummaryDetailed(String summaryDetailed) { this.summaryDetailed = summaryDetailed; }

    public List<String> getKeyPoints() { return keyPoints != null ? keyPoints : new ArrayList<>(); }
    public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints != null ? keyPoints : new ArrayList<>(); }

    public List<String> getTopics() { return topics != null ? topics : new ArrayList<>(); }
    public void setTopics(List<String> topics) { this.topics = topics != null ? topics : new ArrayList<>(); }

    public List<String> getSuggestedTags() { return suggestedTags != null ? suggestedTags : new ArrayList<>(); }
    public void setSuggestedTags(List<String> suggestedTags) { this.suggestedTags = suggestedTags != null ? suggestedTags : new ArrayList<>(); }

    public List<AiEntity> getEntities() { return entities != null ? entities : new ArrayList<>(); }
    public void setEntities(List<AiEntity> entities) { this.entities = entities != null ? entities : new ArrayList<>(); }

    public List<String> getActionItems() { return actionItems != null ? actionItems : new ArrayList<>(); }
    public void setActionItems(List<String> actionItems) { this.actionItems = actionItems != null ? actionItems : new ArrayList<>(); }

    public String getSuggestedCollection() { return suggestedCollection != null ? suggestedCollection : ""; }
    public void setSuggestedCollection(String suggestedCollection) { this.suggestedCollection = suggestedCollection; }

    public String getProviderId() { return providerId != null ? providerId : ""; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getModel() { return model != null ? model : ""; }
    public void setModel(String model) { this.model = model; }

    public String getPromptVersion() { return promptVersion != null ? promptVersion : "v1.0.0"; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }

    public double getCostUsd() { return costUsd; }
    public void setCostUsd(double costUsd) { this.costUsd = costUsd; }

    public String getRawJson() { return rawJson != null ? rawJson : "{}"; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Converts to strict schema JSON string per Master Spec §18.
     */
    public String toStrictSchemaJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("summary_short", getSummaryShort());
            obj.put("summary_detailed", getSummaryDetailed());

            JSONArray kp = new JSONArray();
            for (String s : getKeyPoints()) kp.put(s);
            obj.put("key_points", kp);

            JSONArray tp = new JSONArray();
            for (String s : getTopics()) tp.put(s);
            obj.put("topics", tp);

            JSONArray st = new JSONArray();
            for (String s : getSuggestedTags()) st.put(s);
            obj.put("suggested_tags", st);

            JSONArray ent = new JSONArray();
            for (AiEntity e : getEntities()) ent.put(e.toJson());
            obj.put("entities", ent);

            JSONArray ai = new JSONArray();
            for (String s : getActionItems()) ai.put(s);
            obj.put("action_items", ai);

            obj.put("suggested_collection", getSuggestedCollection());
            obj.put("prompt_version", getPromptVersion());
            obj.put("provider_id", getProviderId());
            obj.put("model", getModel());
        } catch (JSONException ignored) {}
        return obj.toString();
    }

    /**
     * Populates fields from a validated strict schema JSON object per Master Spec §18.
     */
    public static AiEnrichment fromStrictSchemaJson(long contentItemId, JSONObject obj) {
        if (obj == null) return null;
        AiEnrichment e = new AiEnrichment();
        e.setContentItemId(contentItemId);
        e.setSummaryShort(obj.optString("summary_short", ""));
        e.setSummaryDetailed(obj.optString("summary_detailed", ""));

        JSONArray kp = obj.optJSONArray("key_points");
        if (kp != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < kp.length(); i++) {
                String val = kp.optString(i, "").trim();
                if (!val.isEmpty()) list.add(val);
            }
            e.setKeyPoints(list);
        }

        JSONArray tp = obj.optJSONArray("topics");
        if (tp != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < tp.length(); i++) {
                String val = tp.optString(i, "").trim();
                if (!val.isEmpty()) list.add(val);
            }
            e.setTopics(list);
        }

        JSONArray st = obj.optJSONArray("suggested_tags");
        if (st != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < st.length(); i++) {
                String val = st.optString(i, "").trim();
                if (!val.isEmpty()) {
                    if (val.startsWith("#")) val = val.substring(1).trim();
                    list.add(val);
                }
            }
            e.setSuggestedTags(list);
        }

        JSONArray ent = obj.optJSONArray("entities");
        if (ent != null) {
            List<AiEntity> list = new ArrayList<>();
            for (int i = 0; i < ent.length(); i++) {
                JSONObject o = ent.optJSONObject(i);
                if (o != null) {
                    AiEntity entity = AiEntity.fromJson(o);
                    if (entity != null && !entity.getName().isEmpty()) {
                        list.add(entity);
                    }
                }
            }
            e.setEntities(list);
        }

        JSONArray ai = obj.optJSONArray("action_items");
        if (ai != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < ai.length(); i++) {
                String val = ai.optString(i, "").trim();
                if (!val.isEmpty()) list.add(val);
            }
            e.setActionItems(list);
        }

        e.setSuggestedCollection(obj.optString("suggested_collection", ""));
        e.setPromptVersion(obj.optString("prompt_version", "v1.0.0"));
        e.setProviderId(obj.optString("provider_id", ""));
        e.setModel(obj.optString("model", ""));
        e.setRawJson(obj.toString());
        return e;
    }

    public static String stringListToJson(List<String> list) {
        if (list == null) return "[]";
        JSONArray arr = new JSONArray();
        for (String s : list) {
            if (s != null && !s.trim().isEmpty()) arr.put(s.trim());
        }
        return arr.toString();
    }

    public static List<String> jsonToStringList(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                String s = arr.optString(i, "").trim();
                if (!s.isEmpty()) list.add(s);
            }
        } catch (JSONException ignored) {}
        return list;
    }

    public static String entitiesToJson(List<AiEntity> list) {
        if (list == null) return "[]";
        JSONArray arr = new JSONArray();
        for (AiEntity e : list) {
            if (e != null) arr.put(e.toJson());
        }
        return arr.toString();
    }

    public static List<AiEntity> jsonToEntities(String json) {
        List<AiEntity> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    AiEntity e = AiEntity.fromJson(o);
                    if (e != null) list.add(e);
                }
            }
        } catch (JSONException ignored) {}
        return list;
    }
}
