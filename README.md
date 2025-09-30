# Social Media Pipeline

A real-time data pipeline that processes social media posts using **Apache Spark**, **Apache Kafka**, and **Java**. Tweets (or mock data) are streamed through Kafka and processed by Spark to extract **hashtag counts** and display **recent tweets**.

---

## Project Overview

* **Goal**: Stream and analyze tweets to identify trending hashtags and recent activity.
* **Stack**: Apache Kafka, Apache Spark, Java, Twitter API *(optional)*.
* **In Progress**:

    *  Docker containerization.
    *  Python integration for analytics/visualization.

---

## Project Structure

```
social-media-pipeline/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/example/
│   │   │       ├── SparkStreamingConsumer.java
│   │   │       ├── KafkaProducerUtil.java
│   │   │       ├── Config.java
│   │   │       └── TwitterStreamProducer.java
│   └── python/                 # Future Python scripts
├── docker/                    # Future Docker configs
├── pom.xml                    # Maven config
├── .gitignore
└── README.md
```

---

##  Prerequisites

| Tool              | Version / Notes                                 |
| ----------------- | ----------------------------------------------- |
| Java              | JDK 8+                                          |
| Maven             | For building the project                        |
| Apache Kafka      | Compatible with `kafka-clients` 3.6.1           |
| Apache Spark      | Version 3.5.1                                   |
| ZooKeeper         | Required for Kafka                              |
| Twitter API Token | Optional — for fetching real tweets             |
| OS                | Setup written for Windows (adapt for Linux/Mac) |

---

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/Reemsoliiman/social-media-pipeline.git
cd social-media-pipeline
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Start ZooKeeper (Windows)

```bash
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```

### 4. Start Kafka Broker

```bash
bin\windows\kafka-server-start.bat config\server.properties
```

⚠️ **Make sure the `social-media-posts` topic exists.** It's configured in `Config.java`.

### 5. Run the Kafka Producer

**Option A: Use Mock Data (default)**
No setup needed — mock tweets will be sent to Kafka.

**Option B: Use Real Twitter Data**

```bash
set TWITTER_BEARER_TOKEN="your_token"
```

Edit `USE_MOCK_DATA = false` in `TwitterStreamProducer.java`, then run:

```bash
mvn exec:java "-Dexec.mainClass=org.example.TwitterStreamProducer"
```

### 6. Run the Spark Streaming Consumer

```bash
mvn exec:java "-Dexec.mainClass=org.example.SparkStreamingConsumer" -X
```

---

## Usage

*  **Hashtag Counts**: Displays the number of times hashtags appeared per batch.
*  **Recent Tweets**: Shows up to 5 recent tweets from the last hour.
*  **Logging**: Logs invalid JSON, API rate limits, and debug info.
*  **Mock Mode**: Simulates tweets using static data for testing.

---

## Planned Improvements

*  **Dockerization**: Add Docker support for Kafka, Spark, and app containers.
*  **Python Analytics**: Integrate with `pandas`, `matplotlib`, or `Plotly`.
*  **Monitoring**: Prometheus + Grafana for pipeline metrics.
*  **Storage**: Persist tweets or hashtags in PostgreSQL or another DB.

---

##  Troubleshooting

| Issue                | Solution                                                        |
| -------------------- | --------------------------------------------------------------- |
| Kafka not connecting | Ensure ZooKeeper and Kafka are running on `localhost:9092`.     |
| Twitter API failure  | Check token or toggle back to mock mode.                        |
| Spark crashes        | Ensure Spark version is 3.5.1 and `local[*]` mode is supported. |
| Debugging            | Check SLF4J/Logback logs for detailed error output.             |

---

## Contributing

Pull requests are welcome! Feel free to open an issue for suggestions, bugs, or feature requests.
