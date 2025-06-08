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
                .setAppName("SocialMediaPipeline")
                .setMaster("local[*]");
        JavaStreamingContext streamingContext = new JavaStreamingContext(conf, Durations.seconds(10));

        // Use a unique consumer group ID for each run to force fresh offset
        String consumerGroupId = "spark-consumer-group-" + System.currentTimeMillis();

        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        Collection<String> topics = Collections.singletonList("social-media-posts");

        JavaDStream<ConsumerRecord<String, String>> stream = KafkaUtils.createDirectStream(
                streamingContext,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(topics, kafkaParams)
        );

        JavaDStream<String> tweetStream = stream.map(ConsumerRecord::value).filter(tweet -> {
            try {
                // Clean the string to handle special characters
                String cleanedTweet = tweet.replaceAll("[^\\x00-\\x7F]", " ");
                new JSONObject(cleanedTweet);
                return true;
            } catch (Exception e) {
                logger.warn("Skipping invalid JSON tweet: {}. Exception: {}", tweet, e.getMessage());
                return false;
            }
        });

        // Add debug log to trace number of tweets processed
        tweetStream.foreachRDD((rdd, time) -> {
            long count = rdd.count();
            logger.info("Processing {} tweets in batch at time {}", count, FORMATTER.format(Instant.ofEpochMilli(time.milliseconds())));
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
                rdd.collect().forEach(tuple -> System.out.printf("│ %-15s │ %5d │%n", tuple._1, tuple._2));
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
}