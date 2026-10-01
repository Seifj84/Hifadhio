package com.seiftech.hifadhio.ai;

import java.io.Serializable;

/**
 * Execution options for AI enrichment requests.
 * Master Spec §18 & §1209.
 */
public class AiOptions implements Serializable {

    public static final String DEFAULT_PROMPT_VERSION = "v1.0.0";

    private String promptVersion = DEFAULT_PROMPT_VERSION;
    private boolean forceReprocess = false;
    private float temperature = 0.2f;
    private int maxTokens = 1024;
    private boolean includeEntities = true;
    private boolean includeActionItems = true;
    private String preferredModel = "";

    public AiOptions() {}

    public static AiOptions defaults() {
        return new AiOptions();
    }

    public static AiOptions reprocess() {
        AiOptions opt = new AiOptions();
        opt.setForceReprocess(true);
        return opt;
    }

    public String getPromptVersion() {
        return promptVersion != null ? promptVersion : DEFAULT_PROMPT_VERSION;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public boolean isForceReprocess() {
        return forceReprocess;
    }

    public void setForceReprocess(boolean forceReprocess) {
        this.forceReprocess = forceReprocess;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isIncludeEntities() {
        return includeEntities;
    }

    public void setIncludeEntities(boolean includeEntities) {
        this.includeEntities = includeEntities;
    }

    public boolean isIncludeActionItems() {
        return includeActionItems;
    }

    public void setIncludeActionItems(boolean includeActionItems) {
        this.includeActionItems = includeActionItems;
    }

    public String getPreferredModel() {
        return preferredModel != null ? preferredModel : "";
    }

    public void setPreferredModel(String preferredModel) {
        this.preferredModel = preferredModel;
    }
}
