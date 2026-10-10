package com.opsflow.knowledge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Used with PostgreSQL. The embedding lives in a pgvector column and the database does the
 * nearest-neighbour search through the HNSW index created in the V2 migration.
 * "<=>" is pgvector's cosine distance, so similarity is 1 minus it.
 */
@Component
@ConditionalOnProperty(name = "opsflow.vector.store", havingValue = "pgvector")
public class PgVectorIndex implements VectorIndex {

    private final JdbcTemplate jdbc;

    public PgVectorIndex(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void store(long chunkId, float[] embedding) {
        jdbc.update("update knowledge_chunks set embedding = ?::vector where id = ?", Vectors.toText(embedding), chunkId);
    }

    @Override
    public List<Hit> nearest(float[] query, int limit) {
        String vector = Vectors.toText(query);
        return jdbc.query("""
                        select id, 1 - (embedding <=> ?::vector) as score
                        from knowledge_chunks
                        where embedding is not null
                        order by embedding <=> ?::vector
                        limit ?
                        """,
                (row, n) -> new Hit(row.getLong("id"), row.getDouble("score")),
                vector, vector, limit);
    }

    @Override
    public List<Long> chunksWithoutEmbedding() {
        return jdbc.queryForList("select id from knowledge_chunks where embedding is null", Long.class);
    }
}
