// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { getDatabase } from "firebase/database";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyCmZTgYhqGX-vmLasS9LCc3kNDzXYhiXvE",
  authDomain: "social-media-673fb.firebaseapp.com",
  projectId: "social-media-673fb",
  storageBucket: "social-media-673fb.firebasestorage.app",
  messagingSenderId: "423328857833",
  appId: "1:423328857833:web:7ebab74bda581b19cc7a78",
  measurementId: "G-DT8MN4JKCS",
  databaseURL: "https://social-media-673fb-default-rtdb.firebaseio.com"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);

// Export instances for use throughout the app
export const db = getDatabase(app);           // Realtime Database (legacy, might be removed)
export const firestore = getFirestore(app);   // Firestore (primary for chat)
export const auth = getAuth(app);             // Firebase Authentication