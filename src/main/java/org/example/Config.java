package org.example;

public class Config {
    public static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String KAFKA_TOPIC = "social-media-posts";
//    public static final String KAFKA_GROUP_ID = "spark-consumer-group";
    public static final String TWITTER_API_BASE = "https://api.twitter.com/2";
    public static final String TWITTER_QUERY = "(#tech OR #programming) lang:en -is:retweet";
    public static final String TWITTER_BEARER_TOKEN = System.getenv("TWITTER_BEARER_TOKEN") != null ? System.getenv("TWITTER_BEARER_TOKEN")
            : "AAAAAAAAAAAAAAAAAAAAAAtt1AEAAAAAsWL7TrBeg52K39ZNTRjpsQIvjWA%3DI8E7m9E7JvkUP7DW058QzR6hERp66Ep1YUnnQcFWciu0wVdH7O";
    public static final int TWITTER_MAX_RESULTS = 10;
    public static final String SPARK_APP_NAME = "SocialMediaPipeline";
}