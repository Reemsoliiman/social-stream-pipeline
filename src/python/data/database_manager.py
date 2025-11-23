import psycopg2
from psycopg2.extras import RealDictCursor
import pandas as pd
from datetime import datetime, timedelta
import logging

logger = logging.getLogger(__name__)

class DatabaseManager:
    def __init__(self, host, port, database, user, password):
        self.conn_params = {
            'host': host,
            'port': port,
            'database': database,
            'user': user,
            'password': password
        }
    
    def get_connection(self):
        return psycopg2.connect(**self.conn_params)
    
    def fetch_unprocessed_tweets(self, limit=100):
        query = """
            SELECT id, tweet_text, timestamp, length, hashtags
            FROM tweets
            WHERE processed = false
            ORDER BY timestamp DESC
            LIMIT %s
        """
        try:
            with self.get_connection() as conn:
                df = pd.read_sql_query(query, conn, params=(hours,)) # type: ignore
            return df
        except Exception as e:
            logger.error(f"Error fetching hashtag trends: {e}")
            return pd.DataFrame()
    
    def get_sentiment_distribution(self, hours=24):
        query = """
            SELECT sa.sentiment, COUNT(*) as count
            FROM sentiment_analysis sa
            JOIN tweets t ON sa.tweet_id = t.id
            WHERE sa.analyzed_at > NOW() - INTERVAL '%s hours'
            GROUP BY sa.sentiment
        """
        try:
            with self.get_connection() as conn:
                df = pd.read_sql_query(query, conn, params=(hours,))
            return df
        except Exception as e:
            logger.error(f"Error fetching sentiment distribution: {e}")
            return pd.DataFrame()