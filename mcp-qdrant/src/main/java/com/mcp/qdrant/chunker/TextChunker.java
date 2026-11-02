package com.mcp.qdrant.chunker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mcp.qdrant.model.DocumentChunk;
import com.mcp.qdrant.proto.ChunkingConfig;

@Component
public class TextChunker {

    private static final Logger log = LoggerFactory.getLogger(TextChunker.class);

    private static final int DEFAULT_CHUNK_SIZE = 512;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;
    private static final String DEFAULT_SEPARATOR = "\\n\\n";

    public List<DocumentChunk> chunk(String content, Map<String, String> metadata, ChunkingConfig config) {
        int chunkSize = (config != null && config.getChunkSize() > 0) ? config.getChunkSize() : DEFAULT_CHUNK_SIZE;
        int overlap = (config != null && config.getChunkOverlap() > 0) ? config.getChunkOverlap() : DEFAULT_CHUNK_OVERLAP;
        String separator = (config != null && config.getSeparator() != null && !config.getSeparator().isEmpty()) 
    ? config.getSeparator() 
    : DEFAULT_SEPARATOR;
        
        // Ensure overlap is less than chunk size to prevent infinite loops
        overlap = Math.min(overlap, chunkSize / 2);
        
        List<DocumentChunk> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chunks;
        }

        List<String> paragraphs = splitIntoFitParagraphs(content, chunkSize, separator);
        StringBuilder currentChunk = new StringBuilder();
        int sequenceNumber = 0;

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(createChunk(currentChunk.toString(), metadata, sequenceNumber++));

                int overlapStart = Math.max(0, currentChunk.length() - overlap);
                currentChunk = new StringBuilder(currentChunk.substring(overlapStart));
            }

            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(paragraph);

            if (currentChunk.length() >= chunkSize) {
                chunks.add(createChunk(currentChunk.toString(), metadata, sequenceNumber++));

                int overlapStart = Math.max(0, currentChunk.length() - overlap);
                currentChunk = new StringBuilder(currentChunk.substring(overlapStart));
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString(), metadata, sequenceNumber++));
        }

        log.info("Chunked document into {} chunks", chunks.size());
        return chunks;
    }

    // Split the content into paragraphs, hard-splitting any paragraph that exceeds
    // chunkSize so no oversized chunk (e.g. a whole markdown table or code block)
    // reaches the embedding service and overflows the model context.
    private List<String> splitIntoFitParagraphs(String content, int chunkSize, String separator) {
        List<String> result = new ArrayList<>();
        for (String paragraph : content.split(separator)) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }
            if (paragraph.length() <= chunkSize) {
                result.add(paragraph);
            } else {
                result.addAll(splitOversizedParagraph(paragraph, chunkSize));
            }
        }
        return result;
    }

    // Greedily re-assemble lines of an oversized paragraph into pieces of at most
    // chunkSize characters; single lines longer than chunkSize are cut at character
    // boundaries as a last resort.
    private List<String> splitOversizedParagraph(String paragraph, int chunkSize) {
        List<String> pieces = new ArrayList<>();
        StringBuilder piece = new StringBuilder();
        for (String line : paragraph.split("\n")) {
            while (line.length() > chunkSize) {
                if (piece.length() > 0) {
                    pieces.add(piece.toString());
                    piece = new StringBuilder();
                }
                pieces.add(line.substring(0, chunkSize));
                line = line.substring(chunkSize);
            }
            if (piece.length() > 0 && piece.length() + 1 + line.length() > chunkSize) {
                pieces.add(piece.toString());
                piece = new StringBuilder();
            }
            if (piece.length() > 0) {
                piece.append("\n");
            }
            piece.append(line);
        }
        if (piece.length() > 0) {
            pieces.add(piece.toString());
        }
        return pieces;
    }

    private DocumentChunk createChunk(String content, Map<String, String> metadata, int sequenceNumber) {
        String chunkId = UUID.randomUUID().toString();

        Map<String, String> chunkMetadata = new HashMap<>();
        if (metadata != null) {
            chunkMetadata.putAll(metadata);
        }
        chunkMetadata.put("sequence", String.valueOf(sequenceNumber));

        return DocumentChunk.builder()
                .chunkId(chunkId)
                .content(content.trim())
                .metadata(chunkMetadata)
                .sequenceNumber(sequenceNumber)
                .build();
    }
}
