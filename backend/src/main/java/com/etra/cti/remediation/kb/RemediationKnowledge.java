package com.etra.cti.remediation.kb;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "remediation_knowledge")
public class RemediationKnowledge extends BaseEntity {

    @Column(name = "match_keyword", nullable = false, unique = true, length = 255)
    private String matchKeyword;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Column(length = 20)
    private String cwe;

    @Column(length = 80)
    private String owasp;

    @Column(nullable = false, length = 20)
    private String priority = "MEDIUM";

    public String getMatchKeyword() { return matchKeyword; }
    public void setMatchKeyword(String matchKeyword) { this.matchKeyword = matchKeyword; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getCwe() { return cwe; }
    public void setCwe(String cwe) { this.cwe = cwe; }
    public String getOwasp() { return owasp; }
    public void setOwasp(String owasp) { this.owasp = owasp; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
