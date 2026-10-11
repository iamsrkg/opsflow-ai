-- Embeddings from a language model (EMBEDDING_PROVIDER=model), next to the hashed ones from V2.
-- A pgvector column has a fixed dimension, so the two kinds cannot share a column.
-- 384 dimensions: all-MiniLM-L6-v2 and other small sentence-embedding models.
alter table knowledge_chunks add column embedding_model vector(384);

-- which model made the vector: when the configured model changes, the chunk is embedded again
alter table knowledge_chunks add column embedding_model_name varchar(100);

create index idx_chunk_embedding_model on knowledge_chunks using hnsw (embedding_model vector_cosine_ops);
