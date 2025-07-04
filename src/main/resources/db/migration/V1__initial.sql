CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TYPE category AS ENUM ('POLITICS', 'ECONOMICS', 'SOCIETY', 'SPORT', 'SCIENCE_TECH');
CREATE TYPE status AS ENUM ('NEW', 'ACTIVE', 'DELETED');
CREATE TYPE source AS ENUM ('RT_RU', 'AIF_RU', 'SVPRESSA_RU');

CREATE TABLE news_articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(300) NOT NULL,
    summary TEXT NOT NULL,
    content TEXT NOT NULL,
    category category NOT NULL,
    url VARCHAR(700) NOT NULL UNIQUE,
    status status NOT NULL,
    published_at TIMESTAMP NOT NULL,
    source source NOT NULL
);

CREATE TABLE news_keywords (
    article_id UUID NOT NULL REFERENCES news_articles (id) ON DELETE CASCADE,
    keyword VARCHAR(100) NOT NULL,
    PRIMARY KEY (article_id, keyword)
);

CREATE TABLE news_media_urls (
    article_id UUID NOT NULL REFERENCES news_articles (id) ON DELETE CASCADE,
    media_url VARCHAR(700) NOT NULL,
    PRIMARY KEY (article_id, media_url)
);

CREATE INDEX idx_news_articles_category ON news_articles (category);
CREATE INDEX idx_news_articles_status ON news_articles (status);
CREATE INDEX idx_news_articles_published_at ON news_articles (published_at);
CREATE INDEX idx_news_articles_source ON news_articles (source);

CREATE INDEX idx_news_articles_published_at_id_desc ON news_articles (published_at DESC, id DESC);
CREATE INDEX idx_news_articles_category_source_published_at ON news_articles (category, source, published_at DESC);

CREATE INDEX idx_news_articles_content_russian ON news_articles
USING GIN (to_tsvector('russian', title || ' ' || content));

CREATE INDEX idx_news_keywords_keyword ON news_keywords
USING GIN (LOWER(keyword) gin_trgm_ops);
