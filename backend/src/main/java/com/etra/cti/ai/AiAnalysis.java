package com.etra.cti.ai;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "ai_analyses")
public class AiAnalysis extends BaseEntity {

    @Column(name = "analysis_type", nullable = false, length = 40)
    private String analysisType;

    @Column(name = "resource_type", nullable = false, length = 40)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 80)
    private String resourceId;

    @Column(name = "prompt_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String promptHash;

    @Column(name = "result_text", nullable = false, columnDefinition = "TEXT")
    private String resultText;

    @Column(length = 80)
    private String model;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getPromptHash() { return promptHash; }
    public void setPromptHash(String promptHash) { this.promptHash = promptHash; }

    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
}
