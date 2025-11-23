CREATE TABLE IF NOT EXISTS tweets (
    id SERIAL PRIMARY KEY,
    tweet_text TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    length INTEGER,
    hashtags VARCHAR(255)[],
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hashtag_counts (
    id SERIAL PRIMARY KEY,
    hashtag VARCHAR(255) NOT NULL,
    count INTEGER NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sentiment_analysis (
    id SERIAL PRIMARY KEY,
    tweet_id INTEGER REFERENCES tweets(id) ON DELETE CASCADE,
    sentiment VARCHAR(50) NOT NULL,
    confidence FLOAT,
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tweet_id)
);

CREATE TABLE IF NOT EXISTS topic_clusters (
    id SERIAL PRIMARY KEY,
    tweet_id INTEGER REFERENCES tweets(id) ON DELETE CASCADE,
    cluster_id INTEGER NOT NULL,
    cluster_label VARCHAR(255),
    clustered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tweet_id)
);

CREATE INDEX idx_tweets_timestamp ON tweets(timestamp);
CREATE INDEX idx_tweets_processed ON tweets(processed);
CREATE INDEX idx_hashtag_counts_timestamp ON hashtag_counts(timestamp);
CREATE INDEX idx_sentiment_tweet_id ON sentiment_analysis(tweet_id);
CREATE INDEX idx_clusters_tweet_id ON topic_clusters(tweet_id);