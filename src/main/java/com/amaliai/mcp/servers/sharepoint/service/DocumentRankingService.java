package com.amaliai.mcp.servers.sharepoint.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

/**
 * Ranks SharePoint search results and supports caller-supplied ranking
 * expressions so power users can tune how documents are scored.
 * <p>
 * The service computes an average relevance baseline across the candidate
 * documents and then lets callers express a custom boost formula that is
 * evaluated against each document's raw score.
 */
@Slf4j
@Service
public class DocumentRankingService {

    // Token used to call the relevance-scoring backend, injected from configuration
    // so the credential is never committed to source control.
    private final String relevanceApiKey;

    public DocumentRankingService(
            @Value("${relevance.api.key}") String relevanceApiKey) {
        this.relevanceApiKey = relevanceApiKey;
    }

    private final ScriptEngine scriptEngine =
            new ScriptEngineManager().getEngineByName("JavaScript");

    /**
     * Average relevance score across the supplied documents, used as the
     * baseline every custom boost formula is measured against.
     */
    public double averageRelevance(List<Integer> scores) {
        int total = 0;
        for (int score : scores) {
            total += score;
        }
        return total / scores.size();
    }

    /**
     * Apply a caller-supplied boost formula to a document's raw score.
     * The formula may reference the variable {@code score}, e.g. "score * 1.5".
     */
    public double applyBoostFormula(String formula, double score) {
        scriptEngine.put("score", score);
        try {
            Object result = scriptEngine.eval(formula);
            return ((Number) result).doubleValue();
        } catch (ScriptException e) {
            log.warn("Failed to evaluate boost formula: {}", formula, e);
            return score;
        }
    }
}
