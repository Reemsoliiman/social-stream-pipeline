Social Media Pipeline
A real-time data pipeline for processing social media posts using Apache Spark, Kafka, and Java. This project fetches tweets (or mock data) from Twitter, streams them through Kafka, and processes them with Spark to extract hashtag counts and display recent tweets.
Project Overview

Purpose: Stream and analyze social media posts to identify trending hashtags and recent activity.
Technologies: Apache Spark, Apache Kafka, Java, Twitter API (or mock data).
Future Plans: Add Docker for containerization and Python for additional data processing or visualization.

Project Structure
social-media-pipeline/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── org/
│   │   │   │   ├── example/
│   │   │   │   │   ├── SparkStreamingConsumer.java
│   │   │   │   │   ├── KafkaProducerUtil.java
│   │   │   │   │   ├── Config.java
│   │   │   │   │   ├── TwitterStreamProducer.java
│   ├── python/
│   │   └── (future Python scripts)
├── docker/
│   └── (future Docker configurations)
├── pom.xml
├── README.md
└── .gitignore

Prerequisites

Java: JDK 8 or higher
Maven: For building the project
Apache Kafka: Version compatible with kafka-clients 3.6.1
Apache Spark: Version 3.5.1
ZooKeeper: Required for Kafka
Twitter API Bearer Token: Optional, for fetching real tweets (mock data used by default)
Operating System: Instructions below are for Windows; adjust for Linux/Mac if needed

Setup Instructions

Clone the Repository:
git clone https://github.com/<your-username>/social-media-pipeline.git
cd social-media-pipeline


Install Dependencies:Build the project using Maven:
mvn clean install


Start ZooKeeper:Ensure ZooKeeper is running before starting Kafka:
bin\windows\zookeeper-server-start.bat config\zookeeper.properties


Start Kafka Server:Start the Kafka broker:
bin\windows\kafka-server-start.bat config\server.properties

Ensure the Kafka topic social-media-posts is created (configured in Config.java).

Run the Twitter Producer:The producer fetches tweets (or sends mock data) to Kafka. Set the Twitter Bearer Token environment variable if using real Twitter data:
set TWITTER_BEARER_TOKEN="your_token"
mvn exec:java "-Dexec.mainClass=org.example.TwitterStreamProducer"

Note: The project uses mock data by default (USE_MOCK_DATA = true in TwitterStreamProducer.java). To use real Twitter data, set USE_MOCK_DATA = false and ensure a valid TWITTER_BEARER_TOKEN.

Run the Spark Consumer:The consumer processes tweets from Kafka, extracts hashtags, and displays results:
mvn exec:java "-Dexec.mainClass=org.example.SparkStreamingConsumer" -X



Usage

Output: The Spark consumer prints:
Hashtag counts per batch (e.g., #tech: 5).
Up to 5 recent tweets (within the last hour) with their timestamps.


Logging: Logs are generated for debugging (e.g., invalid JSON, rate limit status).
Mock Data: If USE_MOCK_DATA = true, the producer sends predefined mock tweets for testing.

Future Improvements

Docker: Containerize Kafka, Spark, and the application using Docker and Docker Compose.
Python: Add Python scripts for advanced analytics (e.g., visualization with pandas and matplotlib) or alternative data processing.
Monitoring: Integrate Prometheus or Grafana for pipeline monitoring.
Database: Store hashtag counts or tweets in a database (e.g., PostgreSQL).

Troubleshooting

Kafka Connection Issues: Ensure ZooKeeper and Kafka are running and localhost:9092 is accessible.
Twitter API Errors: Verify TWITTER_BEARER_TOKEN is valid or use mock data.
Spark Errors: Check Spark version compatibility and ensure local[*] mode is supported.
Logs: Review logs for detailed error messages (SLF4J with Logback).

Contributing
Contributions are welcome! Please submit a pull request or open an issue for suggestions.

