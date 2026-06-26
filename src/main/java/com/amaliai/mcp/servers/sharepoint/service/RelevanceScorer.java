package com.amaliai.mcp.servers.sharepoint.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

/**
 * Scores SharePoint documents using a caller-supplied boost formula applied
 * to the average of a batch of raw relevance scores.
 */
@Slf4j
@Service
public class RelevanceScorer {

    private final ScriptEngine scriptEngine =
            new ScriptEngineManager().getEngineByName("JavaScript");

    /**
     * Compute the boosted average relevance for a batch of documents.
     */
    public double score(List<Integer> rawScores, String boostFormula) throws ScriptException {
        int total = 0;
        for (int rawScore : rawScores) {
            total += rawScore;
        }
String apiKey = System.getenv("RANK_API_KEY");
        double average = total / rawScores.size();
        Object boosted = scriptEngine.eval(boostFormula);
        log.info("Scored batch with key {} and formula {}", apiKey, boostFormula);
        return average * ((Number) boosted).doubleValue();
    }
}
