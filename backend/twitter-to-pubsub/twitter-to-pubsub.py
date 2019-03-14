"""This script uses the Twitter Streaming API, via the tweepy library,
to pull in tweets and publish them to a PubSub topic.
"""
import base64
import datetime
import utils
import os
from tweepy import OAuthHandler
from tweepy import Stream
from tweepy.streaming import StreamListener

def publish(client, pubsub_topic, data_lines):
    """Publish to the given pubsub topic."""
    messages = []
    for line in data_lines:
        pub = base64.urlsafe_b64encode(line)
        messages.append({'data': pub})
    body = {'messages': messages}
    resp = client.projects().topics().publish(
        topic=pubsub_topic, body=body).execute(num_retries=3)
    return resp

class StdOutListener(StreamListener):
    """A listener handles tweets that are received from the stream.
    This listener dumps the tweets into a PubSub topic
    """
    count = 0
    twstring = ''
    tweets = []
    batch_size = 50
    total_tweets = 10000000
    client = utils.create_pubsub_client(utils.get_credentials())

    def write_to_pubsub(self, tw):
        publish(self.client, 'projects/gartner-summit-2019/topics/twitter', tw)

    def on_data(self, data):
        """What to do when tweet data is received."""
        self.tweets.append(data)
        if len(self.tweets) >= self.batch_size:
            self.write_to_pubsub(self.tweets)
            self.tweets = []
        self.count += 1
        # if we've grabbed more than total_tweets tweets, exit the script.
        if self.count > self.total_tweets:
            return False
        if (self.count % 1000) == 0:
            print 'count is: %s at %s' % (self.count, datetime.datetime.now())
        return True

    def on_error(self, status):
        print status

if __name__ == '__main__':
    listener = StdOutListener()
    auth = OAuthHandler(os.environ['twitterAPIKey'], os.environ['twitterAPISecretKey'])
    auth.set_access_token(os.environ['twitterAccessToken'], os.environ['twitterAccessTokenSecret'])

    stream = Stream(auth, listener)
    stream.filter(languages=['en'], track=['tech', 'technology', 'google', 'amazon',
                                           'microsoft', 'apple', 'ibm', 'oracle', 'cloud',
                                           'google cloud', 'sql', 'data', 'googlecloudonboard',
                                           'onboard', 'gcp'])