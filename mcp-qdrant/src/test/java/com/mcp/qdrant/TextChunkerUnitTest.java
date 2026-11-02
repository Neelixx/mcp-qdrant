package com.mcp.qdrant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.mcp.qdrant.chunker.TextChunker;
import com.mcp.qdrant.model.DocumentChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Regression tests for TextChunker: a single oversized paragraph (markdown table,
 * fenced code block) must be hard-split so no chunk exceeds the configured bound,
 * and no content may be lost.
 */
public class TextChunkerUnitTest {

    private static final int DEFAULT_CHUNK_SIZE = 512;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    @Test
    void testOversizedParagraphIsHardSplit() {
        // One paragraph (no blank lines) far beyond chunkSize, like a markdown table.
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            lines.add("| " + i + " | `SomeTaskClass` | `some-input.ext` -> `some-output.ext` | explanation of the row " + i + " |");
        }
        String content = String.join("\n", lines);
        assertTrue(content.length() > 4 * DEFAULT_CHUNK_SIZE);

        List<DocumentChunk> chunks = new TextChunker().chunk(content, null, null);
        assertTrue(chunks.size() > 1);

        // Packer bound: overlap tail + separator + one fitting paragraph.
        int maxAllowed = DEFAULT_CHUNK_SIZE + DEFAULT_CHUNK_OVERLAP + 1;
        for (DocumentChunk chunk : chunks) {
            assertTrue(chunk.getContent().length() <= maxAllowed);
        }

        // No line may be lost: every input line must appear in some chunk.
        for (String line : lines) {
            boolean found = chunks.stream().anyMatch(chunk -> chunk.getContent().contains(line));
            assertTrue(found, "line missing from chunks: " + line);
        }
    }

    @Test
    void testNormalParagraphsStillChunk() {
        String content = "First paragraph text.\n\nSecond paragraph text.\n\nThird paragraph text.";
        List<DocumentChunk> chunks = new TextChunker().chunk(content, null, null);
        assertEquals(1, chunks.size());
        assertEquals("First paragraph text. Second paragraph text. Third paragraph text.",
                chunks.get(0).getContent());
    }
}
