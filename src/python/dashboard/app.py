import streamlit as st
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from datetime import datetime, timedelta
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(__file__)))

from data.database_manager import DatabaseManager
from analytics.sentiment_analyzer import SentimentAnalyzer
from analytics.topic_clustering import TopicClusterer
from analytics.feature_engineering import FeatureEngineer

st.set_page_config(
    page_title="Social Media Analytics Dashboard",
    page_icon="📊",
    layout="wide"
)

@st.cache_resource
def get_db_manager():
    return DatabaseManager(
        host=os.getenv('POSTGRES_HOST', 'localhost'),
        port=os.getenv('POSTGRES_PORT', '5432'),
        database=os.getenv('POSTGRES_DB', 'twitter_analytics'),
        user=os.getenv('POSTGRES_USER', 'postgres'),
        password=os.getenv('POSTGRES_PASSWORD', 'postgres')
    )

def main():
    st.title("Real-Time Social Media Analytics Dashboard")
    
    db = get_db_manager()
    
    st.sidebar.header("Dashboard Controls")
    time_window = st.sidebar.slider("Time Window (hours)", 1, 168, 24)
    auto_refresh = st.sidebar.checkbox("Auto Refresh", value=True)
    
    if auto_refresh:
        st.sidebar.text(f"Last updated: {datetime.now().strftime('%H:%M:%S')}")
    
    col1, col2, col3 = st.columns(3)
    
    with col1:
        st.metric("Active Time Window", f"{time_window} hours")
    
    with col2:
        sentiment_df = db.get_sentiment_distribution(hours=time_window)
        total_analyzed = sentiment_df['count'].sum() if not sentiment_df.empty else 0
        st.metric("Tweets Analyzed", f"{total_analyzed:,}")
    
    with col3:
        hashtag_df = db.get_recent_hashtag_trends(hours=time_window)
        unique_hashtags = len(hashtag_df) if not hashtag_df.empty else 0
        st.metric("Unique Hashtags", unique_hashtags)
    
    st.markdown("---")
    
    tab1, tab2, tab3, tab4 = st.tabs([
        "Sentiment Analysis", 
        "Topic Clusters", 
        "Hashtag Trends", 
        "Tweet Analytics"
    ])
    
    with tab1:
        st.subheader("Sentiment Distribution")
        if not sentiment_df.empty:
            fig = px.pie(
                sentiment_df, 
                values='count', 
                names='sentiment',
                title='Sentiment Distribution',
                color='sentiment',
                color_discrete_map={
                    'positive': '#00cc96',
                    'neutral': '#636efa',
                    'negative': '#ef553b'
                }
            )
            st.plotly_chart(fig, use_container_width=True)
            
            col1, col2 = st.columns(2)
            with col1:
                fig_bar = px.bar(
                    sentiment_df,
                    x='sentiment',
                    y='count',
                    title='Sentiment Counts',
                    color='sentiment',
                    color_discrete_map={
                        'positive': '#00cc96',
                        'neutral': '#636efa',
                        'negative': '#ef553b'
                    }
                )
                st.plotly_chart(fig_bar, use_container_width=True)
        else:
            st.info("No sentiment data available for the selected time window")
    
    with tab2:
        st.subheader("Topic Clustering Analysis")
        
        tweets_df = db.fetch_unprocessed_tweets(limit=100)
        
        if not tweets_df.empty and len(tweets_df) >= 5:
            clusterer = TopicClusterer(n_clusters=5, method='kmeans')
            clusters, coords = clusterer.fit_predict(tweets_df['tweet_text'].tolist())
            
            cluster_df = pd.DataFrame({
                'x': coords[:, 0],
                'y': coords[:, 1],
                'cluster': clusters,
                'tweet': tweets_df['tweet_text'].str[:100]
            })
            
            fig = px.scatter(
                cluster_df,
                x='x',
                y='y',
                color='cluster',
                hover_data=['tweet'],
                title='Tweet Clusters (2D Projection)',
                labels={'x': 'Component 1', 'y': 'Component 2'}
            )
            st.plotly_chart(fig, use_container_width=True)
            
            st.subheader("Cluster Keywords")
            keywords = clusterer.get_cluster_keywords(n_words=5)
            for cluster_id, words in keywords.items():
                st.write(f"**Cluster {cluster_id}:** {', '.join(words)}")
        else:
            st.info("Not enough tweets for clustering analysis (minimum 5 required)")
    
    with tab3:
        st.subheader("Trending Hashtags")
        if not hashtag_df.empty:
            fig = px.bar(
                hashtag_df.head(15),
                x='total_count',
                y='hashtag',
                orientation='h',
                title='Top 15 Hashtags',
                labels={'total_count': 'Count', 'hashtag': 'Hashtag'}
            )
            fig.update_layout(yaxis={'categoryorder': 'total ascending'})
            st.plotly_chart(fig, use_container_width=True)
            
            st.dataframe(
                hashtag_df,
                use_container_width=True,
                column_config={
                    "hashtag": "Hashtag",
                    "total_count": st.column_config.NumberColumn("Total Count", format="%d"),
                    "last_seen": st.column_config.DatetimeColumn("Last Seen")
                }
            )
        else:
            st.info("No hashtag data available for the selected time window")
    
    with tab4:
        st.subheader("Tweet Analytics")
        
        if not tweets_df.empty:
            engineer = FeatureEngineer()
            features_df = engineer.extract_features(tweets_df)
            
            col1, col2 = st.columns(2)
            
            with col1:
                fig = px.histogram(
                    features_df,
                    x='text_length',
                    title='Tweet Length Distribution',
                    labels={'text_length': 'Characters', 'count': 'Frequency'}
                )
                st.plotly_chart(fig, use_container_width=True)
            
            with col2:
                fig = px.histogram(
                    features_df,
                    x='hashtag_count',
                    title='Hashtags per Tweet',
                    labels={'hashtag_count': 'Number of Hashtags', 'count': 'Frequency'}
                )
                st.plotly_chart(fig, use_container_width=True)
            
            st.subheader("Feature Statistics")
            stats = features_df[[
                'text_length', 'word_count', 'hashtag_count', 
                'url_count', 'mention_count', 'emoji_count'
            ]].describe()
            st.dataframe(stats, use_container_width=True)
        else:
            st.info("No tweet data available for analysis")
    
    if auto_refresh:
        import time
        time.sleep(10)
        st.rerun()

if __name__ == "__main__":
    main()