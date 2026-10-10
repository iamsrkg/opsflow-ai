-- Runbook search: one embedding per chunk, searched by cosine distance.
-- The dimension must match EmbeddingService.DIMENSIONS.
create extension if not exists vector;

alter table knowledge_chunks add column embedding vector(256);

-- HNSW: approximate nearest-neighbour search that stays fast as the number of chunks grows
create index idx_chunk_embedding on knowledge_chunks using hnsw (embedding vector_cosine_ops);
