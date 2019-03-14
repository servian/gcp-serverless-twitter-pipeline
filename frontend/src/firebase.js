import Firebase from 'firebase'

require('firebase/firestore')

const firebaseApp = Firebase.initializeApp({
    apiKey: "AIzaSyBXTfR_fk30K-66ymwHQnLM9hoO9Cyh8iU",
    authDomain: "serverless-twitter-pipeline.firebaseapp.com",
    databaseURL: "https://serverless-twitter-pipeline.firebaseio.com",
    projectId: "serverless-twitter-pipeline",
    storageBucket: "serverless-twitter-pipeline.appspot.com",
    messagingSenderId: "2892640778"
})

export const db = firebaseApp.firestore()