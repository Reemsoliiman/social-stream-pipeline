import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans, DBSCAN
from sklearn.decomposition import PCA
import logging

logger = logging.getLogger(__name__)

class TopicClusterer:
    def __init__(self, n_clusters=5, method='kmeans'):
        self.n_clusters = n_clusters
        self.method = method
        self.vectorizer = TfidfVectorizer(
            max_features=100,
            stop_words='english',
            ngram_range=(1, 2)
        )
        self.model = None
        self.pca = PCA(n_components=2)
    
    def preprocess_text(self, text):
        """Basic text preprocessing"""
        text = text.lower()
        text = ''.join([c for c in text if c.isalnum() or c.isspace()])
        return text
    
    def fit_predict(self, texts):
        """
        Fits clustering model and predicts clusters
        Returns: cluster labels and cluster coordinates for visualization
        """
        try:
            preprocessed = [self.preprocess_text(text) for text in texts]
            
            tfidf_matrix = self.vectorizer.fit_transform(preprocessed)
            
            if self.method == 'kmeans':
                self.model = KMeans(
                    n_clusters=min(self.n_clusters, len(texts)),
                    random_state=42,
                    n_init=10
                )
            elif self.method == 'dbscan':
                self.model = DBSCAN(eps=0.5, min_samples=2)
            else:
                raise ValueError(f"Unknown clustering method: {self.method}")
            
            clusters = self.model.fit_predict(tfidf_matrix)
            
            coords = self.pca.fit_transform(tfidf_matrix.toarray())
            
            return clusters, coords
        except Exception as e:
            logger.error(f"Error in clustering: {e}")
            return np.zeros(len(texts)), np.zeros((len(texts), 2))
    
    def get_cluster_keywords(self, n_words=5):
        """
        Extracts top keywords for each cluster
        """
        if self.model is None or self.method != 'kmeans':
            return {}
        
        try:
            feature_names = self.vectorizer.get_feature_names_out()
            cluster_keywords = {}
            
            for i in range(self.n_clusters):
                center = self.model.cluster_centers_[i]
                top_indices = center.argsort()[-n_words:][::-1]
                keywords = [feature_names[idx] for idx in top_indices]
                cluster_keywords[i] = keywords
            
            return cluster_keywords
        except Exception as e:
            logger.error(f"Error extracting keywords: {e}")
            return {}