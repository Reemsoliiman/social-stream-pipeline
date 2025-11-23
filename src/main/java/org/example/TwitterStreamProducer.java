package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class TwitterStreamProducer {
    private static final Logger logger = LoggerFactory.getLogger(TwitterStreamProducer.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static int requestCount = 0;
    private static long lastResetTimestamp = 0;
    private static final String RATE_LIMIT_FILE = "rate_limit.properties";

    public static void main(String[] args) {
        if (Config.TWITTER_BEARER_TOKEN == null && !Config.USE_MOCK_DATA) {
            logger.error("TWITTER_BEARER_TOKEN environment variable is not set");
            System.exit(1);
        }

        logger.info("Starting producer in {} mode", Config.USE_MOCK_DATA ? "MOCK" : "LIVE");
        loadRateLimitState();

        while (true) {
            try {
                if (Config.USE_MOCK_DATA) {
                    sendMockTweets();
                } else {
                    fetchAndSendTweets();
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.error("Polling interrupted: {}", e.getMessage());
                KafkaProducerUtil.close();
                saveRateLimitState();
                break;
            } catch (Exception e) {
                logger.error("Error during polling: {}", e.getMessage());
                saveRateLimitState();
            }
        }
    }

    private static void loadRateLimitState() {
        try (FileInputStream fis = new FileInputStream(RATE_LIMIT_FILE)) {
            Properties props = new Properties();
            props.load(fis);
            requestCount = Integer.parseInt(props.getProperty("requestCount", "0"));
            lastResetTimestamp = Long.parseLong(props.getProperty("lastResetTimestamp", "0"));
            logger.info("Loaded rate limit state: requestCount={}, lastResetTimestamp={}", requestCount, lastResetTimestamp);
        } catch (Exception e) {
            logger.warn("Could not load rate limit state: {}. Starting fresh.", e.getMessage());
        }
    }

    private static void saveRateLimitState() {
        try (FileOutputStream fos = new FileOutputStream(RATE_LIMIT_FILE)) {
            Properties props = new Properties();
            props.setProperty("requestCount", String.valueOf(requestCount));
            props.setProperty("lastResetTimestamp", String.valueOf(lastResetTimestamp));
            props.store(fos, "Rate Limit State");
            logger.info("Saved rate limit state: requestCount={}, lastResetTimestamp={}", requestCount, lastResetTimestamp);
        } catch (Exception e) {
            logger.error("Could not save rate limit state: {}", e.getMessage());
        }
    }

    private static void sendMockTweets() throws Exception {
        logger.info("Generating mock tweets...");
        String[] mockTweets = {
                "This is a mock tweet about #Tech and #AI!",
                "Another mock tweet for testing #Streaming #Data",
                "Mock tweet 3 with #Kafka and #Spark",
                "Mock tweet 4 discussing #SocialMedia trends",
                "Final mock tweet with #spark #Testing"
        };

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String timestamp = sdf.format(new Date());

        for (String tweetText : mockTweets) {
            ObjectNode enrichedTweet = mapper.createObjectNode();
            enrichedTweet.put("tweet", tweetText);
            enrichedTweet.put("timestamp", timestamp);
            enrichedTweet.put("length", tweetText.length());

            ArrayNode hashtagsArray = mapper.createArrayNode();
            String[] words = tweetText.split(" ");
            for (String word : words) {
                if (word.startsWith("#")) {
                    ObjectNode hashtag = mapper.createObjectNode();
                    hashtag.put("start", tweetText.indexOf(word));
                    hashtag.put("end", tweetText.indexOf(word) + word.length());
                    hashtag.put("tag", word.substring(1));
                    hashtagsArray.add(hashtag);
                }
            }
            enrichedTweet.set("hashtags", hashtagsArray);

            String enrichedTweetJson = mapper.writeValueAsString(enrichedTweet);
            logger.debug("Mock Tweet: {}", enrichedTweetJson);
            KafkaProducerUtil.sendTweet(enrichedTweetJson);
        }
        logger.info("Sent 5 mock tweets to Kafka");
    }

    private static void fetchAndSendTweets() throws Exception {
        long currentTime = System.currentTimeMillis() / 1000;

        if (lastResetTimestamp > 0 && currentTime >= lastResetTimestamp) {
            logger.info("Rate limit window reset. Clearing request count.");
            requestCount = 0;
        }

        if (requestCount >= 10 && currentTime < lastResetTimestamp) {
            long waitSeconds = lastResetTimestamp - currentTime + 1;
            logger.info("Approaching rate limit ({} requests made). Waiting {} seconds until reset.", requestCount, waitSeconds);
            Thread.sleep(waitSeconds * 1000);
            requestCount = 0;
            return;
        }

        requestCount++;
        String encodedQuery = URLEncoder.encode(Config.TWITTER_QUERY, StandardCharsets.UTF_8);
        String url = String.format("%s/tweets/search/recent?query=%s&max_results=%d&tweet.fields=created_at,entities",
                Config.TWITTER_API_BASE, encodedQuery, Config.TWITTER_MAX_RESULTS);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + Config.TWITTER_BEARER_TOKEN)
                .header("Accept", "application/json")
                .GET()
                .build();

        logger.info("Sending request #{} to Twitter API", requestCount);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logger.debug("Response headers: {}", response.headers().map());
        logger.debug("Response body: {}", response.body());
        String remaining = response.headers().firstValue("x-rate-limit-remaining").orElse("N/A");
        String resetTime = response.headers().firstValue("x-rate-limit-reset").orElse("N/A");
        logger.info("Rate limit remaining: {}", remaining);
        logger.info("Rate limit reset time: {}", resetTime);

        try {
            int remainingRequests = Integer.parseInt(remaining);
            if (remainingRequests < 5) {
                logger.warn("Low rate limit remaining: {} requests left. Reset at timestamp: {}", remainingRequests, resetTime);
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse x-rate-limit-remaining: {}", remaining);
        }

        try {
            lastResetTimestamp = Long.parseLong(resetTime);
            if (lastResetTimestamp < currentTime) {
                logger.warn("Reset time {} is in the past. Using current time + 900 seconds.", resetTime);
                lastResetTimestamp = currentTime + 900;
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse x-rate-limit-reset: {}. Using current time + 900 seconds", resetTime);
            lastResetTimestamp = currentTime + 900;
        }

        if (response.statusCode() == 429) {
            logger.warn("Rate limit exceeded. Request #{}", requestCount);
            long waitSeconds = Math.max(900, lastResetTimestamp - currentTime + 1);
            logger.info("Waiting {} seconds until rate limit reset", waitSeconds);
            Thread.sleep(waitSeconds * 1000);
            requestCount = 0;
            saveRateLimitState();
            return;
        }

        if (response.statusCode() != 200) {
            logger.error("Twitter API error: HTTP {} - {}", response.statusCode(), response.body());
            return;
        }

        JsonNode responseJson = mapper.readTree(response.body());
        if (!responseJson.has("data")) {
            logger.info("No tweets found for query: {}", Config.TWITTER_QUERY);
            return;
        }

        int tweetCount = responseJson.get("data").size();
        logger.info("Found {} tweets in request #{}", tweetCount, requestCount);
        for (JsonNode tweet : responseJson.get("data")) {
            String tweetText = tweet.get("text").asText();
            String timestamp = tweet.has("created_at") ? tweet.get("created_at").asText()
                    : new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());

            ObjectNode enrichedTweet = mapper.createObjectNode();
            enrichedTweet.put("tweet", tweetText);
            enrichedTweet.put("timestamp", timestamp);
            enrichedTweet.put("length", tweetText.length());
            enrichedTweet.set("hashtags",
                    tweet.has("entities") && tweet.get("entities").has("hashtags")
                            ? tweet.get("entities").get("hashtags")
                            : mapper.createArrayNode());

            String enrichedTweetJson = mapper.writeValueAsString(enrichedTweet);
            logger.debug("Enriched Tweet: {}", enrichedTweetJson);
            KafkaProducerUtil.sendTweet(enrichedTweetJson);
        }
        saveRateLimitState();
    }
}