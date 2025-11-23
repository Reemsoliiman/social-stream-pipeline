import pandas as pd
import numpy as np
from datetime import datetime
import re
import logging

logger = logging.getLogger(__name__)

class FeatureEngineer:
    def __init__(self):
        self.url_pattern = re.compile(r'http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\\(\\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+')
        self.mention_pattern = re.compile(r'@\w+')
        self.emoji_pattern = re.compile("["
            u"\U0001F600-\U0001F64F"
            u"\U0001F300-\U0001F5FF"
            u"\U0001F680-\U0001F6FF"
            u"\U0001F1E0-\U0001F1FF"
            "]+", flags=re.UNICODE)
    
    def extract_features(self, df):
        """
        Extracts comprehensive features from tweet dataframe
        """
        try:
            features_df = df.copy()
            
            features_df['text_length'] = features_df['tweet_text'].str.len()
            features_df['word_count'] = features_df['tweet_text'].str.split().str.len()
            features_df['avg_word_length'] = features_df['text_length'] / features_df['word_count']
            
            features_df['hashtag_count'] = features_df['hashtags'].apply(lambda x: len(x) if x else 0)
            features_df['url_count'] = features_df['tweet_text'].apply(lambda x: len(self.url_pattern.findall(x)))
            features_df['mention_count'] = features_df['tweet_text'].apply(lambda x: len(self.mention_pattern.findall(x)))
            features_df['emoji_count'] = features_df['tweet_text'].apply(lambda x: len(self.emoji_pattern.findall(x)))
            
            features_df['has_question'] = features_df['tweet_text'].str.contains('?', regex=False).astype(int)
            features_df['has_exclamation'] = features_df['tweet_text'].str.contains('!', regex=False).astype(int)
            features_df['is_all_caps'] = features_df['tweet_text'].apply(lambda x: x.isupper()).astype(int)
            
            features_df['hour_of_day'] = pd.to_datetime(features_df['timestamp']).dt.hour
            features_df['day_of_week'] = pd.to_datetime(features_df['timestamp']).dt.dayofweek
            features_df['is_weekend'] = features_df['day_of_week'].apply(lambda x: 1 if x >= 5 else 0)
            
            features_df['uppercase_ratio'] = features_df['tweet_text'].apply(
                lambda x: sum(1 for c in x if c.isupper()) / len(x) if len(x) > 0 else 0
            )
            
            features_df['digit_ratio'] = features_df['tweet_text'].apply(
                lambda x: sum(1 for c in x if c.isdigit()) / len(x) if len(x) > 0 else 0
            )
            
            return features_df
        except Exception as e:
            logger.error(f"Error in feature engineering: {e}")
            return df
    
    def get_engagement_features(self, df):
        """
        Calculates engagement-related features
        """
        try:
            df['engagement_score'] = (
                df['hashtag_count'] * 2 +
                df['mention_count'] * 1.5 +
                df['url_count'] * 1 +
                df['has_question'] * 0.5
            )
            return df
        except Exception as e:
            logger.error(f"Error calculating engagement features: {e}")
            return df