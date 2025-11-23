# Social Media Analytics Pipeline

A production-ready real-time data pipeline that ingests social media posts, processes them through Apache Kafka and Spark Streaming, performs ML-based sentiment analysis and topic clustering, and visualizes insights through an interactive dashboard.

---

## Architecture

```
Twitter API → Kafka Producer → Kafka Broker → Spark Consumer → PostgreSQL
                                                     ↓
                                              Python Analytics
                                           (Sentiment + Clustering)
                                                     ↓
                                            Streamlit Dashboard
```

**Data Flow:**
1. Producer fetches tweets from Twitter API or generates mock data
2. Kafka streams tweets in real-time
3. Spark Streaming processes tweets in 10-second batches
4. Data stored in PostgreSQL with extracted features
5. Python analytics processes sentiment and topic clusters
6. Interactive dashboard displays real-time insights

---

## Features

### Data Engineering
- Real-time streaming with Apache Kafka
- Distributed processing with Apache Spark
- Persistent storage with PostgreSQL
- Configurable environment variables
- Rate limit handling for Twitter API

### Machine Learning
- Sentiment analysis using TextBlob NLP
- Topic clustering with K-Means algorithm
- Feature engineering (20+ features extracted)
- Batch processing pipeline

### Visualization
- Interactive Streamlit dashboard
- Real-time sentiment distribution
- Topic cluster visualization
- Hashtag trend analysis
- Tweet analytics and statistics

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Streaming | Apache Kafka 2.8.2 |
| Processing | Apache Spark 3.2.4 |
| Storage | PostgreSQL 15 |
| ML/Analytics | scikit-learn, TextBlob, NLTK |
| Visualization | Streamlit, Plotly |
| Language | Java 11, Python 3.10 |
| Containerization | Docker, Docker Compose |

---

## Project Structure

```
social-media-pipeline/
├── src/
│   ├── main/java/org/example/
│   │   ├── Config.java                      # Configuration management
│   │   ├── TwitterStreamProducer.java       # Kafka producer
│   │   ├── SparkStreamingConsumer.java      # Spark consumer
│   │   └── KafkaProducerUtil.java           # Kafka utilities
│   └── python/
│       ├── analytics_processor.py           # Main ML processor
│       ├── analytics/
│       │   ├── sentiment_analyzer.py        # Sentiment analysis
│       │   ├── topic_clustering.py          # Topic clustering
│       │   └── feature_engineering.py       # Feature extraction
│       ├── data/
│       │   └── database_manager.py          # Database operations
│       ├── dashboard/
│       │   └── app.py                       # Streamlit dashboard
│       └── config/
│           └── database.py                  # DB configuration
├── docker/
│   ├── docker-compose.yml                   # Multi-container setup
│   ├── Dockerfile.python                    # Python service
│   └── init-db.sql                          # Database schema
└── pom.xml                                  # Maven dependencies
```

---

## Quick Start

### Prerequisites

- Java 11+
- Python 3.10+
- Maven 3.6+
- Docker & Docker Compose
- Twitter API Bearer Token (optional for real data)

### 1. Clone Repository

```bash
git clone https://github.com/Reemsoliiman/social-media-pipeline.git
cd social-media-pipeline
```

### 2. Environment Setup

Create `.env` file:

```bash
# Twitter API (optional)
TWITTER_BEARER_TOKEN=your_token_here
USE_MOCK_DATA=true

# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=twitter_analytics
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC=social-media-posts
```

### 3. Start Infrastructure with Docker

```bash
cd docker
docker-compose up -d
```

This starts:
- ZooKeeper (port 2181)
- Kafka (port 9092)
- PostgreSQL (port 5432)
- Streamlit Dashboard (port 8501)

### 4. Build Java Application

```bash
mvn clean install
```

### 5. Install Python Dependencies

```bash
cd src/python
pip install -r requirements.txt
python -m nltk.downloader punkt stopwords
```

### 6. Run Pipeline Components

**Terminal 1 - Kafka Producer:**
```bash
mvn exec:java -Dexec.mainClass="org.example.TwitterStreamProducer"
```

**Terminal 2 - Spark Consumer:**
```bash
mvn exec:java -Dexec.mainClass="org.example.SparkStreamingConsumer"
```

**Terminal 3 - Analytics Processor:**
```bash
python src/python/analytics_processor.py
```

**Terminal 4 - Dashboard (if not using Docker):**
```bash
streamlit run src/python/dashboard/app.py
```

### 7. Access Dashboard

Open browser: `http://localhost:8501`

---

## Using Real Twitter Data

1. Get API credentials from [developer.twitter.com](https://developer.twitter.com)
2. Set environment variable:
   ```bash
   export TWITTER_BEARER_TOKEN="your_token"
   export USE_MOCK_DATA=false
   ```
3. Restart the producer

**Note:** Twitter API Basic tier allows 10 tweets per 15 minutes

---

## Database Schema

### tweets
- `id` (PK)
- `tweet_text` TEXT
- `timestamp` TIMESTAMP
- `length` INTEGER
- `hashtags` VARCHAR[]
- `processed` BOOLEAN

### sentiment_analysis
- `id` (PK)
- `tweet_id` (FK)
- `sentiment` VARCHAR (positive/neutral/negative)
- `confidence` FLOAT
- `analyzed_at` TIMESTAMP

### topic_clusters
- `id` (PK)
- `tweet_id` (FK)
- `cluster_id` INTEGER
- `cluster_label` VARCHAR
- `clustered_at` TIMESTAMP

### hashtag_counts
- `id` (PK)
- `hashtag` VARCHAR
- `count` INTEGER
- `timestamp` TIMESTAMP

---

## ML Pipeline Details

### Sentiment Analysis
- Algorithm: TextBlob Polarity Analysis
- Output: positive/neutral/negative + confidence score
- Processing: Real-time per tweet

### Topic Clustering
- Algorithm: K-Means (k=5) with TF-IDF vectorization
- Features: Unigrams and bigrams
- Dimensionality Reduction: PCA for visualization
- Batch: Minimum 5 tweets required

### Feature Engineering
Extracted features include:
- Text metrics: length, word count, avg word length
- Content: hashtag count, URL count, mention count, emoji count
- Syntax: question marks, exclamation points, all caps detection
- Temporal: hour of day, day of week, weekend flag
- Ratios: uppercase ratio, digit ratio

---

## Dashboard Features

### 1. Sentiment Analysis Tab
- Pie chart of sentiment distribution
- Bar chart of sentiment counts
- Real-time sentiment metrics

### 2. Topic Clusters Tab
- 2D scatter plot of tweet clusters
- Top keywords per cluster
- Interactive hover information

### 3. Hashtag Trends Tab
- Top 15 trending hashtags
- Historical hashtag counts
- Searchable hashtag table

### 4. Tweet Analytics Tab
- Tweet length distribution
- Hashtags per tweet histogram
- Feature statistics summary

---

## Configuration

All configuration is managed through environment variables or `Config.java`:

| Variable | Default | Description |
|----------|---------|-------------|
| USE_MOCK_DATA | true | Use mock data instead of Twitter API |
| TWITTER_BEARER_TOKEN | - | Twitter API authentication token |
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092 | Kafka broker address |
| KAFKA_TOPIC | social-media-posts | Kafka topic name |
| POSTGRES_HOST | localhost | PostgreSQL host |
| POSTGRES_PORT | 5432 | PostgreSQL port |
| POSTGRES_DB | twitter_analytics | Database name |

---

## Performance Metrics

- Streaming Throughput: ~100 tweets/second
- Batch Processing: 10-second windows
- ML Processing: ~50 tweets per batch
- Dashboard Refresh: 10 seconds
- Storage: Optimized with indexes

---

## Troubleshooting

### Kafka Connection Issues
```bash
# Check if Kafka is running
docker ps | grep kafka

# View Kafka logs
docker logs kafka
```

### Database Connection Errors
```bash
# Test PostgreSQL connection
psql -h localhost -U postgres -d twitter_analytics

# Check if tables exist
\dt
```

### Dashboard Not Loading
```bash
# Check Python dependencies
pip list | grep streamlit

# Run with verbose logging
streamlit run src/python/dashboard/app.py --logger.level=debug
```

---

## Future Enhancements

- Deep learning sentiment models (BERT, RoBERTa)
- Real-time anomaly detection
- User influence scoring
- Geographic analysis with map visualizations
- A/B testing framework for ML models
- API endpoint for model serving
- Automated model retraining pipeline

---

## Development

### Running Tests
```bash
mvn test                    # Java tests
pytest src/python/tests/    # Python tests
```

### Code Quality
```bash
mvn checkstyle:check        # Java linting
black src/python/           # Python formatting
flake8 src/python/          # Python linting
```