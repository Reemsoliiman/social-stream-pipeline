import os
import sys
import time
import logging
from datetime import datetime

sys.path.append(os.path.dirname(__file__))

from data.database_manager import DatabaseManager
from analytics.sentiment_analyzer import SentimentAnalyzer
from analytics.topic_clustering import TopicClusterer
from analytics.feature_engineering import FeatureEngineer

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class AnalyticsProcessor:
    def __init__(self):
        self.db = DatabaseManager(
            host=os.getenv('POSTGRES_HOST', 'localhost'),
            port=os.getenv('POSTGRES_PORT', '5432'),
            database=os.getenv('POSTGRES_DB', 'twitter_analytics'),
            user=os.getenv('POSTGRES_USER', 'postgres'),
            password=os.getenv('POSTGRES_PASSWORD', 'postgres')
        )
        self.sentiment_analyzer = SentimentAnalyzer()
        self.topic_clusterer = TopicClusterer(n_clusters=5)
        self.feature_engineer = FeatureEngineer()
    
    def process_batch(self, batch_size=50):
        """
        Processes a batch of unprocessed tweets
        """
        logger.info("Fetching unprocessed tweets...")
        tweets_df = self.db.fetch_unprocessed_tweets(limit=batch_size)
        
        if tweets_df.empty:
            logger.info("No unprocessed tweets found")
            return 0
        
        logger.info(f"Processing {len(tweets_df)} tweets")
        
        logger.info("Performing sentiment analysis...")
        for idx, row in tweets_df.iterrows():
            sentiment, confidence = self.sentiment_analyzer.analyze_sentiment(row['tweet_text'])
            self.db.store_sentiment_results(row['id'], sentiment, confidence)
        
        logger.info("Extracting features...")
        features_df = self.feature_engineer.extract_features(tweets_df)
        
        if len(tweets_df) >= 5:
            logger.info("Performing topic clustering...")
            clusters, coords = self.topic_clusterer.fit_predict(tweets_df['tweet_text'].tolist())
            keywords = self.topic_clusterer.get_cluster_keywords()
            
            for idx, row in tweets_df.iterrows():
                cluster_id = int(clusters[idx])
                cluster_label = ', '.join(keywords.get(cluster_id, ['Unknown']))
                self.db.store_topic_cluster(row['id'], cluster_id, cluster_label)
        else:
            logger.info("Not enough tweets for clustering (minimum 5 required)")
        
        tweet_ids = tweets_df['id'].tolist()
        self.db.mark_tweets_processed(tweet_ids)
        
        logger.info(f"Successfully processed {len(tweets_df)} tweets")
        return len(tweets_df)
    
    def run_continuous(self, interval=15):
        """
        Continuously processes tweets at specified interval
        """
        logger.info(f"Starting continuous processing with {interval}s interval")
        
        while True:
            try:
                processed = self.process_batch()
                logger.info(f"Batch complete. Waiting {interval} seconds...")
                time.sleep(interval)
            except KeyboardInterrupt:
                logger.info("Shutting down processor...")
                break
            except Exception as e:
                logger.error(f"Error in processing loop: {e}")
                time.sleep(interval)

if __name__ == "__main__":
    processor = AnalyticsProcessor()
    processor.run_continuous(interval=15)