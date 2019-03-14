import Firebase from 'firebase'

require('firebase/firestore')

const firebaseApp = Firebase.initializeApp({
    apiKey: "AIzaSyCZsgsvH-b002URKTpmpcsCw_via2Lxbz0",
    authDomain: "chris-tippett-app-gartner-2019.firebaseapp.com",
    databaseURL: "https://chris-tippett-app-gartner-2019.firebaseio.com",
    projectId: "chris-tippett-app-gartner-2019",
    storageBucket: "chris-tippett-app-gartner-2019.appspot.com",
    messagingSenderId: "1066438672635"
})

export const db = firebaseApp.firestore()