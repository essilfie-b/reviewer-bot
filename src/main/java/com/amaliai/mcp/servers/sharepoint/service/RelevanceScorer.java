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
@Test
void score_shouldComputeFractionalAverage() throws Exception {
    // add coverage for average calculation and formula evaluation
}

@Test
void score_shouldRejectEmptyScores() {
    // add coverage for invalid input handling
}
        int total = 0;
        for (int rawScore : rawScores) {
            total += rawScore;
        }
String apiKey = System.getenv("RANK_API_KEY");
if (rawScores == null || rawScores.isEmpty()) {
    throw new IllegalArgumentException("rawScores must not be null or empty");
}

double average = (double) total / rawScores.size();
private final ScriptEngine scriptEngine =
        new ScriptEngineManager().getEngineByName("JavaScript");
...
if (scriptEngine == null) {
    throw new IllegalStateException("JavaScript ScriptEngine is not available");
}
Object boosted = scriptEngine.eval(boostFormula);
log.info("Scored batch with formula provided for {} scores", rawScores.size());
if (!(boosted instanceof Number)) {
    throw new IllegalArgumentException("boostFormula must evaluate to a numeric value");
}
return average * ((Number) boosted).doubleValue();
    }
}
