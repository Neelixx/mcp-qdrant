package com.vpms.mcp.qdrant.chunker;

import com.vpms.mcp.qdrant.model.DocumentChunk;
import com.vpms.mcp.qdrant.proto.ChunkingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TextChunker {

    private static final Logger log = LoggerFactory.getLogger(TextChunker.class);

    private static final int DEFAULT_CHUNK_SIZE = 512;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;
    private static final String DEFAULT_SEPARATOR = "\\n\\n";

    public List<DocumentChunk> chunk(String content, Map<String, String> metadata, ChunkingConfig config) {
        int chunkSize = (config != null && config.getChunkSize() > 0) ? config.getChunkSize() : DEFAULT_CHUNK_SIZE;
        int overlap = (config != null && config.getChunkOverlap() > 0) ? config.getChunkOverlap() : DEFAULT_CHUNK_OVERLAP;
        String separator = (config != null && !config.getSeparator().isEmpty()) ? config.getSeparator() : DEFAULT_SEPARATOR;

        List<DocumentChunk> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chunks;
        }

        String[] paragraphs = content.split(separator);
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
