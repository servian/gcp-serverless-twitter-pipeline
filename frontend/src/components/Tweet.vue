<template>
  <div class="card">
    <!-- <div class="card-image">
      <figure class="image is-4by3">
        <img src="https://bulma.io/images/placeholders/1280x960.png" alt="Placeholder image">
      </figure>
    </div>-->
    <div class="card-content">
      <div class="media">
        <div class="media-left">
          <figure class="image is-48x48">
            <img :src="data.user.profile_image_url_https">
          </figure>
        </div>
        <div class="media-content">
          <p class="title is-4">{{ data.user.name }}</p>
          <p class="subtitle is-6">@{{ data.user.screen_name }}</p>
        </div>
      </div>

      <div class="content">
        <p v-html="tweetContents"></p>
        <em>
          <time
            class="is-size-7"
            :datetime="timestamp"
          >{{ timestamp.toLocaleDateString("en-US", dateOptions) }}</time>
        </em>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "Tweet",
  props: {
    data: Object,
    config: Object
  },
  data() {
    return {
      dateOptions: {
        year: "numeric",
        month: "long",
        day: "numeric",
        hour: "numeric",
        minute: "numeric"
      }
    }
  },
  computed: {
    timestamp() {
      return new Date(parseInt(this.data.timestamp_ms))
    },
    tweetContents() {
      var pattern = new RegExp(`(${this.config.tags.join("|")})`, "gi")
      return this.data.text.replace(pattern, "<span class='term'>$&</span>")
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.card {
  margin-bottom: 50px;
  border-radius: 5px;
}
</style>
