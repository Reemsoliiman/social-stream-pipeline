package org.example;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SparkStreamingConsumer {
    private static final Logger logger = LoggerFactory.getLogger(SparkStreamingConsumer.class);
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#\\w+");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static void main(String[] args) throws InterruptedException {
        SparkConf conf = new SparkConf()
                .setAppName(Config.SPARK_APP_NAME)
                .setMaster("local[*]");
        JavaStreamingContext streamingContext = new JavaStreamingContext(conf, Durations.seconds(10));

        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Config.KAFKA_BOOTSTRAP_SERVERS);
        kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, Config.KAFKA_GROUP_ID);
        kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        Collection<String> topics = Collections.singletonList(Config.KAFKA_TOPIC);

        JavaDStream<ConsumerRecord<String, String>> stream = KafkaUtils.createDirectStream(
                streamingContext,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(topics, kafkaParams)
        );

        JavaDStream<String> tweetStream = stream.map(ConsumerRecord::value).filter(tweet -> {
            try {
                String cleanedTweet = tweet.replaceAll("[^\\x00-\\x7F]", " ");
                new JSONObject(cleanedTweet);
                return true;
            } catch (Exception e) {
                logger.warn("Skipping invalid JSON tweet: {}. Exception: {}", tweet, e.getMessage());
                return false;
            }
        });

        tweetStream.foreachRDD((rdd, time) -> {
            long count = rdd.count();
            logger.info("Processing {} tweets in batch at time {}", count, FORMATTER.format(Instant.ofEpochMilli(time.milliseconds())));
            
            rdd.foreach(tweet -> {
                try {
                    String cleanedTweet = tweet.replaceAll("[^\\x00-\\x7F]", " ");
                    JSONObject json = new JSONObject(cleanedTweet);
                    
                    String tweetText = json.getString("tweet");
                    String timestamp = json.getString("timestamp");
                    int length = json.getInt("length");
                    
                    List<String> hashtags = new ArrayList<>();
                    if (json.has("hashtags")) {
                        JSONArray hashtagArray = json.getJSONArray("hashtags");
                        for (int i = 0; i < hashtagArray.length(); i++) {
                            JSONObject hashtagObj = hashtagArray.getJSONObject(i);
                            hashtags.add(hashtagObj.getString("tag"));
                        }
                    }
                    
                    storeTweetInDatabase(tweetText, timestamp, length, hashtags);
                } catch (Exception e) {
                    logger.error("Error processing tweet: {}", e.getMessage());
                }
            });
        });

        JavaPairDStream<String, Integer> hashtagCounts = tweetStream.flatMap(tweet -> {
                    List<String> hashtags = new ArrayList<>();
                    try {
                        String cleanedTweet = tweet.replaceAll("[^\\x00-\\x7F]", " ");
                        JSONObject json = new JSONObject(cleanedTweet);
                        if (json.has("hashtags")) {
                            JSONArray hashtagArray = json.getJSONArray("hashtags");
                            for (int i = 0; i < hashtagArray.length(); i++) {
                                JSONObject hashtagObj = hashtagArray.getJSONObject(i);
                                String tag = hashtagObj.getString("tag");
                                hashtags.add("#" + tag);
                            }
                        } else {
                            String tweetText = json.getString("tweet");
                            Matcher matcher = HASHTAG_PATTERN.matcher(tweetText);
                            while (matcher.find()) {
                                hashtags.add(matcher.group());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error extracting hashtags from tweet: {}. Exception: {}", tweet, e.getMessage());
                    }
                    return hashtags.iterator();
                }).mapToPair(hashtag -> new Tuple2<>(hashtag, 1))
                .reduceByKey(Integer::sum);

        hashtagCounts.foreachRDD((rdd, time) -> {
            long count = rdd.count();
            String timestamp = FORMATTER.format(Instant.ofEpochMilli(time.milliseconds()));
            if (count > 0) {
                System.out.println("\n=== Hashtag Analysis ===");
                System.out.println("Timestamp: " + timestamp);
                System.out.println("Records: " + count);
                System.out.println("┌─────────────────┬───────┐");
                System.out.println("│ Hashtag         │ Count │");
                System.out.println("├─────────────────┼───────┤");
                rdd.collect().forEach(tuple -> {
                    System.out.printf("│ %-15s │ %5d │%n", tuple._1, tuple._2);
                    storeHashtagCount(tuple._1, tuple._2, timestamp);
                });
                System.out.println("└─────────────────┴───────┘");
            } else {
                System.out.println("\n=== No Data in Batch ===");
                System.out.println("Timestamp: " + timestamp);
            }
        });

        JavaDStream<String> recentTweets = tweetStream.filter(tweet -> {
            try {
                String cleanedTweet = tweet.replaceAll("[^\\x00-\\x7F]", " ");
                JSONObject json = new JSONObject(cleanedTweet);
                String timestamp = json.getString("timestamp");
                Instant tweetTime = Instant.parse(timestamp);
                Instant now = Instant.now();
                return tweetTime.isAfter(now.minusSeconds(3600));
            } catch (Exception e) {
                logger.warn("Error parsing timestamp in tweet: {}", tweet);
                return false;
            }
        });

        recentTweets.foreachRDD((rdd, time) -> {
            List<String> tweets = rdd.take(5);
            if (!tweets.isEmpty()) {
                System.out.println("\n=== Recent Tweets ===");
                tweets.forEach(tweet -> {
                    try {
                        String cleanedTweet = tweet.replaceAll("[^\\x00-\\x7F]", " ");
                        JSONObject json = new JSONObject(cleanedTweet);
                        System.out.println("Tweet: " + json.getString("tweet"));
                        System.out.println("Timestamp: " + json.getString("timestamp"));
                        System.out.println("----------------");
                    } catch (Exception e) {
                        logger.warn("Error displaying tweet: {}", tweet);
                    }
                });
            }
        });

        streamingContext.start();
        streamingContext.awaitTermination();
    }

    private static void storeTweetInDatabase(String tweetText, String timestamp, int length, List<String> hashtags) {
        String url = String.format("jdbc:postgresql://%s:%s/%s", 
            Config.POSTGRES_HOST, Config.POSTGRES_PORT, Config.POSTGRES_DB);
        
        String sql = "INSERT INTO tweets (tweet_text, timestamp, length, hashtags, processed) VALUES (?, ?, ?, ?, false)";
        
        try (Connection conn = DriverManager.getConnection(url, Config.POSTGRES_USER, Config.POSTGRES_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tweetText);
            pstmt.setTimestamp(2, Timestamp.from(Instant.parse(timestamp)));
            pstmt.setInt(3, length);
            pstmt.setArray(4, conn.createArrayOf("VARCHAR", hashtags.toArray()));
            pstmt.executeUpdate();
            
        } catch (Exception e) {
            logger.error("Error storing tweet in database: {}", e.getMessage());
        }
    }

    private static void storeHashtagCount(String hashtag, int count, String timestamp) {
        String url = String.format("jdbc:postgresql://%s:%s/%s", 
            Config.POSTGRES_HOST, Config.POSTGRES_PORT, Config.POSTGRES_DB);
        
        String sql = "INSERT INTO hashtag_counts (hashtag, count, timestamp) VALUES (?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(url, Config.POSTGRES_USER, Config.POSTGRES_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, hashtag);
            pstmt.setInt(2, count);
            pstmt.setTimestamp(3, Timestamp.valueOf(timestamp));
            pstmt.executeUpdate();
            
        } catch (Exception e) {
            logger.error("Error storing hashtag count: {}", e.getMessage());
        }
    }
}