package org.example;

public class Config {
    public static final String KAFKA_BOOTSTRAP_SERVERS = getEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    public static final String KAFKA_TOPIC = getEnv("KAFKA_TOPIC", "social-media-posts");
    public static final String KAFKA_GROUP_ID = "spark-consumer-group";
    
    public static final String TWITTER_API_BASE = "https://api.twitter.com/2";
    public static final String TWITTER_QUERY = getEnv("TWITTER_QUERY", "(#tech OR #programming) lang:en -is:retweet");
    public static final String TWITTER_BEARER_TOKEN = System.getenv("TWITTER_BEARER_TOKEN");
    public static final int TWITTER_MAX_RESULTS = Integer.parseInt(getEnv("TWITTER_MAX_RESULTS", "10"));
    
    public static final String POSTGRES_HOST = getEnv("POSTGRES_HOST", "localhost");
    public static final String POSTGRES_PORT = getEnv("POSTGRES_PORT", "5432");
    public static final String POSTGRES_DB = getEnv("POSTGRES_DB", "twitter_analytics");
    public static final String POSTGRES_USER = getEnv("POSTGRES_USER", "postgres");
    public static final String POSTGRES_PASSWORD = System.getenv("POSTGRES_PASSWORD");
    
    public static final String SPARK_APP_NAME = "SocialMediaPipeline";
    public static final boolean USE_MOCK_DATA = Boolean.parseBoolean(getEnv("USE_MOCK_DATA", "true"));
    
    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}