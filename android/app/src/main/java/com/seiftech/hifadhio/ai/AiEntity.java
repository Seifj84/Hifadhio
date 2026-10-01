package com.seiftech.hifadhio.ai;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.Objects;

/**
 * Domain model representing an entity mentioned in content (tools, people, orgs, products).
 * Master Spec §15.5 & §18.
 */
public class AiEntity implements Serializable {
    private String name;
    private String type;      // e.g., "tool", "person", "organization", "product", "concept"
    private String evidence;  // context or sentence where entity was mentioned

    public AiEntity() {}

    public AiEntity(String name, String type, String evidence) {
        this.name = name != null ? name.trim() : "";
        this.type = type != null ? type.trim().toLowerCase() : "concept";
        this.evidence = evidence != null ? evidence.trim() : "";
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type != null ? type : "concept";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEvidence() {
        return evidence != null ? evidence : "";
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("name", getName());
            obj.put("type", getType());
            obj.put("evidence", getEvidence());
        } catch (JSONException ignored) {}
        return obj;
    }

    public static AiEntity fromJson(JSONObject obj) {
        if (obj == null) return null;
        String name = obj.optString("name", "");
        String type = obj.optString("type", "concept");
        String evidence = obj.optString("evidence", "");
        return new AiEntity(name, type, evidence);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiEntity aiEntity = (AiEntity) o;
        return Objects.equals(getName().toLowerCase(), aiEntity.getName().toLowerCase()) &&
                Objects.equals(getType().toLowerCase(), aiEntity.getType().toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName().toLowerCase(), getType().toLowerCase());
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getName(), getType());
    }
}
