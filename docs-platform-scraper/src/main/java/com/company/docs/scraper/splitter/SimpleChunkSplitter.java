package com.company.docs.scraper.splitter;

import com.company.docs.common.model.bo.ChunkBO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 简化分片器。
 * <p>
 * 按段落聚合，控制单片长度，保证检索粒度与可读性平衡。
 * </p>
 */
@Component
public class SimpleChunkSplitter {

    private static final int MAX_CHUNK_SIZE = 1000;

    /**
     * 将文本切分为多个分片。
     */
    public List<ChunkBO> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.replace("\r", "").trim();
        List<String> paragraphs = Arrays.stream(normalized.split("\\n\\s*\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        List<ChunkBO> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        int index = 0;
        for (String paragraph : paragraphs.isEmpty() ? List.of(normalized) : paragraphs) {
            if (buffer.length() > 0 && buffer.length() + paragraph.length() + 1 > MAX_CHUNK_SIZE) {
                chunks.add(buildChunk(buffer.toString(), index++));
                buffer.setLength(0);
            }
            if (buffer.length() > 0) {
                buffer.append('\n');
            }
            buffer.append(paragraph);
        }
        if (buffer.length() > 0) {
            chunks.add(buildChunk(buffer.toString(), index));
        }
        return chunks;
    }

    private ChunkBO buildChunk(String content, int index) {
        ChunkBO chunk = new ChunkBO();
        chunk.setContent(content);
        chunk.setLevel(1);
        chunk.setPath(List.of("root", String.valueOf(index)));
        chunk.setTypes(List.of("text"));
        return chunk;
    }
}
