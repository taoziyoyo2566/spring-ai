-- 创建 Spring AI PgVectorStore 所需的 vector_store 表
-- 维度由 Flyway 占位符 vector_dimensions 决定（需与嵌入模型维度一致）
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(${vector_dimensions})
);

-- HNSW 索引用于高效相似度检索（PGvector HNSW 最多支持 2000 维）
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store USING HNSW (embedding vector_cosine_ops);
