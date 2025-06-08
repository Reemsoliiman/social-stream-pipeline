package org.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;

public class KafkaProducerUtil {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerUtil.class);
    private static final KafkaProducer<String, String> producer;

    static {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Config.KAFKA_BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producer = new KafkaProducer<>(props);
    }

    public static void sendTweet(String tweet) {
        ProducerRecord<String, String> record = new ProducerRecord<>(Config.KAFKA_TOPIC, tweet);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                logger.error("Error sending tweet: {}", exception.getMessage());
            } else {
                logger.info("Sent tweet to partition {}, offset {}", metadata.partition(), metadata.offset());
            }
        });
    }

    public static void close() {
        producer.close();
    }
}