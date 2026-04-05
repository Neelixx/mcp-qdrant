package com.vpms.mcp.qdrant.service;

import com.vpms.mcp.qdrant.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);

    public String summarize(String query, List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("Query: ").append(query).append("\n\n");
        context.append("Retrieved Context:\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            context.append("[").append(i + 1).append("] ")
                   .append("Source: ").append(result.getCollection())
                   .append(" (Score: ").append(String.format("%.3f", result.getScore())).append(")\n")
                   .append(result.getPayload())
                   .append("\n\n");
        }

        log.debug("Generated summary context with {} results", results.size());

        return generateSimpleSummary(context.toString(), results);
    }

    private String generateSimpleSummary(String context, List<SearchResult> results) {
        StringBuilder summary = new StringBuilder();

        summary.append("Based on the search results from ")
               .append(results.size())
               .append(" sources:\n\n");

        List<String> keyPoints = results.stream()
                .limit(3)
                .map(r -> "- " + truncate(r.getPayload(), 150))
                .collect(Collectors.toList());

        summary.append(String.join("\n", keyPoints));

        if (results.size() > 3) {
            summary.append("\n\n... and ").append(results.size() - 3).append(" more results.");
        }

        return summary.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
