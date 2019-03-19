<template>
  <div id="app">
    <section class="section" v-if="false">
      <div class="container">
        <div class="tabs is-centered">
          <ul>
            <li :class="{'is-active': filter === 'all'}">
              <a @click="filter = 'all'">All</a>
            </li>
            <li :class="{'is-active': filter === 'sql'}">
              <a @click="filter = 'sql'">#SQL</a>
            </li>
          </ul>
        </div>
      </div>
    </section>
    <transition name="fade">
      <section class="section vip-tweets" v-if="showVipTweets">
        <div class="backdrop-invis" @click="emptyVipTweets()"></div>
        <div class="backdrop"></div>
        <div class="container">
          <div class="columns is-8">
            <div class="column">
              <transition-group name="list" tag="div">
                <Tweet
                  :data="tweet.data"
                  :config="config"
                  v-for="tweet in vipTweets"
                  :key="tweet.id"
                />
              </transition-group>
            </div>
          </div>
        </div>
      </section>
    </transition>
    <section class="section">
      <div class="container">
        <div class="columns is-8">
          <div class="column">
            <transition-group name="list" tag="div">
              <Tweet
                :data="tweet.data"
                :config="config"
                v-for="tweet in tweets[filter]"
                :key="tweet.id"
              />
            </transition-group>
          </div>
        </div>
      </div>
    </section>
    <div class="page-fade">
      <nav class="level tweet-stats">
        <div class="level-item has-text-centered is-hidden-mobile">
          <img class="logo" src="./assets/servian.png">
        </div>
        <div class="level-item has-text-centered">
          <div>
            <p class="heading">Tweets</p>
            <p class="title">{{ compactInteger(counter[filter]) }}</p>
          </div>
        </div>
        <div class="level-item has-text-centered is-hidden-mobile">
          <img class="logo" src="./assets/gcp2.png">
        </div>
      </nav>
    </div>
  </div>
</template>

<script>
import Tweet from "./components/Tweet.vue"
import { db } from "./firebase"

var humanize = require("humanize-plus")

export default {
  name: "app",
  components: {
    Tweet
  },
  data() {
    return {
      loadTimestamp: Date.now(),
      filter: "all",
      tweets: {
        all: [],
        sql: []
      },
      counter: {
        all: 0,
        sql: 0
      },
      config: {
        delay: 500,
        tags: [],
        popupTags: []
      },
      vipTweetUserIds: [1730415624],
      vipTweets: []
    }
  },
  created() {
    this.getConfig()
    this.subscribeToTweets("all", this.tweets.all)
  },
  computed: {
    startTimestamp() {
      return (this.loadTimestamp - 120000).toString()
    },
    showVipTweets() {
      return this.vipTweets.length > 0
    }
  },
  methods: {
    compactInteger: val => humanize.compactInteger(val),
    sleep: ms => {
      return new Promise(resolve => setTimeout(resolve, ms))
    },
    emptyVipTweets: function() {
      this.vipTweets.splice(0, this.vipTweets.length)
    },
    getConfig: function() {
      db.collection("config")
        .doc("web")
        .get()
        .then(doc => {
          if (doc.exists) {
            this.config = doc.data()
          }
        })
    },
    popupTweet: function(tweet) {
      this.debugVip = false
      this.vipTweets.pop()
      this.vipTweets.unshift(tweet)
      this.sleep(10000).then(() => this.vipTweets.pop())
    },
    subscribeToTweets: function(collectionName, collectionArray) {
      db.collection("tweets")
        .orderBy("timestamp_ms", "desc")
        .limit(20)
        .onSnapshot(querySnapshot => {
          var docChanges = querySnapshot.docChanges().reverse()
          docChanges.forEach(change => {
            if (change.type !== "added") {
              return
            }
            var sleepMs = this.config.delay
            this.sleep(sleepMs).then(() => {
              var tweet = {
                data: change.doc.data(),
                id: change.doc.id
              }
              // Check whether tweet contains popup tags
              this.config.popupTags.forEach(val => {
                if (tweet.data.text.toLowerCase().includes(val)) {
                  this.popupTweet(tweet)
                }
              })
              // Check whether tweet from vip users
              if (this.vipTweetUserIds.includes(tweet.data.user.id)) {
                this.popupTweet(tweet)
              }
              collectionArray.unshift(tweet)
              this.counter[collectionName]++
              if (collectionArray.length > 20) {
                collectionArray.pop()
              }
            })
          })
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.fade-enter-active,
.fade-leave-active {
  transition: 1s cubic-bezier(0, 1, 0.5, 1);
  transition-property: opacity, transform;
}
.fade-enter,
.fade-leave-to {
  opacity: 0;
}
.list-enter-active,
.list-leave-active,
.list-move {
  transition: 500ms cubic-bezier(0, 1, 0.5, 1);
  transition-property: opacity, transform;
}

.list-enter {
  opacity: 0;
  transform: translateY(-100px) scaleY(1);
}

.list-enter-to {
  opacity: 1;
  transform: translateY(0) scaleY(1);
}

.list-leave-active {
  position: absolute;
}

.list-leave-to {
  opacity: 0;
  transform: scaleY(0);
  transform-origin: center top;
}
.page-fade {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40%;
  background: linear-gradient(
    to bottom,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 1) 60%
  );
  z-index: 60;
}
.tweet-stats {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding-bottom: 20px;
}
img.logo {
  max-height: 25px;
}
.tabs {
  font-weight: 500;
}
.backdrop,
.backdrop-invis,
.vip-tweets {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}
.backdrop {
  background: #fff;
  opacity: 0.9;
  z-index: 50;
}
.vip-tweets {
  height: 90%;
  z-index: 40;

  .container {
    z-index: 51;
    top: 50%;
    transform: translateY(-50%);
  }

  .card {
    margin-bottom: 0;
    transform: scale(1.2);
  }
}
.backdrop-invis {
  opacity: 0;
  z-index: 999;
}
</style>