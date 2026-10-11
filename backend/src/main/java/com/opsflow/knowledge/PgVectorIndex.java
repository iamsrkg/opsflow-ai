package com.opsflow.knowledge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Used with PostgreSQL. The embedding lives in a pgvector column and the database does the
 * nearest-neighbour search through an HNSW index. "<=>" is pgvector's cosine distance, so
 * similarity is 1 minus it.
 *
 * A pgvector column has a fixed number of dimensions, so each kind of embedding has its own
 * column: "embedding" (256, feature hashing, V2 migration) and "embedding_model" (384, an
 * embedding model, V3 migration). Vectors from different embedders are never compared.
 */
@Component
@ConditionalOnProperty(name = "opsflow.vector.store", havingValue = "pgvector")
public class PgVectorIndex implements VectorIndex {

    static final int MODEL_COLUMN_DIMENSIONS = 384;

    private final JdbcTemplate jdbc;
    private final String embedderName;
    private final boolean fromModel;
    private final String column;

    public PgVectorIndex(JdbcTemplate jdbc, TextEmbedder embedder) {
        this.jdbc = jdbc;
        this.embedderName = embedder.name();
        this.fromModel = !(embedder instanceof EmbeddingService);
        this.column = fromModel ? "embedding_model" : "embedding";
        if (fromModel && embedder.dimensions() != MODEL_COLUMN_DIMENSIONS) {
            throw new IllegalStateException("The embedding_model column holds " + MODEL_COLUMN_DIMENSIONS
                    + " dimensions but " + embedderName + " produces " + embedder.dimensions()
                    + ". Use a " + MODEL_COLUMN_DIMENSIONS + "-dimension model, or add a migration for a new column.");
        }
    }

    @Override
    public void store(long chunkId, float[] embedding) {
        if (fromModel) {
            jdbc.update("update knowledge_chunks set embedding_model = ?::vector, embedding_model_name = ? where id = ?",
                    Vectors.toText(embedding), embedderName, chunkId);
        } else {
            jdbc.update("update knowledge_chunks set embedding = ?::vector where id = ?", Vectors.toText(embedding), chunkId);
        }
    }

    @Override
    public List<Hit> nearest(float[] query, int limit) {
        String vector = Vectors.toText(query);
        // the column name is one of two constants chosen above, never user input
        String sql = "select id, 1 - (" + column + " <=> ?::vector) as score from knowledge_chunks where "
                + (fromModel ? "embedding_model is not null and embedding_model_name = ?" : "embedding is not null")
                + " order by " + column + " <=> ?::vector limit ?";
        return fromModel
                ? jdbc.query(sql, (row, n) -> new Hit(row.getLong("id"), row.getDouble("score")), vector, embedderName, vector, limit)
                : jdbc.query(sql, (row, n) -> new Hit(row.getLong("id"), row.getDouble("score")), vector, vector, limit);
    }

    @Override
    public List<Long> chunksWithoutEmbedding() {
        if (fromModel) {
            // also chunks embedded by a model that is no longer the configured one
            return jdbc.queryForList(
                    "select id from knowledge_chunks where embedding_model is null or embedding_model_name is distinct from ?",
                    Long.class, embedderName);
        }
        return jdbc.queryForList("select id from knowledge_chunks where embedding is null", Long.class);
    }
}
