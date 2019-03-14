FROM python:2

RUN pip install --upgrade pip
RUN pip install tweepy
RUN pip install --upgrade google-api-python-client
RUN pip install --upgrade oauth2client
RUN pip install python-dateutil

ADD twitter-to-pubsub.py /twitter-to-pubsub.py
ADD utils.py /utils.py

ENV twitterAPIKey [insert_here]
ENV twitterAPISecretKey [insert_here]
ENV twitterAccessToken [insert_here]
ENV twitterAccessTokenSecret [insert_here]
ENV gcpProjectId [insert_here]

CMD python twitter-to-pubsub.py