from textblob import TextBlob
import logging
from typing import Tuple

logger = logging.getLogger(__name__)

class SentimentAnalyzer:
    def __init__(self):
        self.sentiment_map = {
            'positive': 1,
            'neutral': 0,
            'negative': -1
        }
    
    def analyze_sentiment(self, text: str) -> Tuple[str, float]:
        """
        Analyzes sentiment of text using TextBlob
        Returns: (sentiment_label, confidence_score)
        """
        try:
            blob = TextBlob(text)
            polarity = blob.sentiment.polarity
            
            if polarity > 0.1:
                sentiment = 'positive'
            elif polarity < -0.1:
                sentiment = 'negative'
            else:
                sentiment = 'neutral'
            
            confidence = abs(polarity)
            
            return sentiment, confidence
        except Exception as e:
            logger.error(f"Error analyzing sentiment: {e}")
            return 'neutral', 0.0
    
    def batch_analyze(self, texts):
        """
        Analyzes sentiment for multiple texts
        Returns: list of (sentiment, confidence) tuples
        """
        results = []
        for text in texts:
            sentiment, confidence = self.analyze_sentiment(text)
            results.append((sentiment, confidence))
        return results