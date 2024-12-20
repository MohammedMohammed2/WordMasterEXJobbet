package com.gritacademy.exjobbet.flashcard;

import android.util.Log;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gritacademy.exjobbet.Datamuse.DatamuseService;
import com.gritacademy.exjobbet.Datamuse.DatamuseWord;
import com.gritacademy.exjobbet.Retrofit.RetrofitClient;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class FlashcardGenerator {

    private final DatamuseService service;
    private final FirebaseFirestore db;

    public FlashcardGenerator() {
        service = RetrofitClient.getRetrofitInstance().create(DatamuseService.class);
        db = FirebaseFirestore.getInstance();
    }

    // Generate flashcards for multiple words
    public void generateFlashcards() {
        fetchRandomWordsFromApi();
    }

    // Fetch multiple random words
    private void fetchRandomWordsFromApi() {
        OkHttpClient client = new OkHttpClient();
        String url = "https://random-word-api.vercel.app/api?words=10"; // Fetch 10 random words

        Request request = new Request.Builder().url(url).get().build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    List<String> words = new Gson().fromJson(responseBody, new TypeToken<List<String>>() {}.getType());
                    if (!words.isEmpty()) {
                        Log.d("FlashcardGenerator", "Fetched random words: " + words);
                        processWords(words); // Process each word
                    }
                } else {
                    Log.e("FlashcardGenerator", "Failed to fetch random words. Response Code: " + response.code());
                }
            } catch (Exception e) {
                Log.e("FlashcardGenerator", "Error fetching random words: " + e.getMessage());
            }
        }).start();
    }

    // Process each word: Fetch synonyms
    private void processWords(List<String> words) {
        for (String word : words) {
            Map<String, Object> flashcardData = new HashMap<>();
            flashcardData.put("word", word);

            fetchSynonyms(word, flashcardData);
        }
    }

    // Fetch synonyms
    private void fetchSynonyms(String word, Map<String, Object> flashcardData) {
        service.getSynonyms(word).enqueue(new retrofit2.Callback<List<DatamuseWord>>() {
            @Override
            public void onResponse(retrofit2.Call<List<DatamuseWord>> call, retrofit2.Response<List<DatamuseWord>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> synonyms = extractUniqueWords(response.body());

                    // Only save the flashcard if synonyms exist
                    if (!synonyms.isEmpty()) {
                        flashcardData.put("synonyms", synonyms);
                        saveFlashcardToDatabase(flashcardData);
                    } else {
                        Log.d("FlashcardGenerator", "No synonyms found for word: " + word + ". Skipping upload.");
                    }
                } else {
                    Log.e("API Error", "Failed to fetch synonyms for word: " + word + ". Response Code: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<DatamuseWord>> call, Throwable t) {
                Log.e("API Error", "Failed to fetch synonyms for word: " + word + ". Error: " + t.getMessage());
            }
        });
    }

    // Extract unique words from Datamuse API response
    private List<String> extractUniqueWords(List<DatamuseWord> words) {
        List<String> wordList = new ArrayList<>();
        for (DatamuseWord wordData : words) {
            wordList.add(wordData.getWord());
        }
        return new ArrayList<>(new HashSet<>(wordList)); // Remove duplicates
    }

    // Save flashcard to Firestore
    private void saveFlashcardToDatabase(Map<String, Object> flashcardData) {
        DocumentReference flashcardRef = db.collection("flashcards").document();
        flashcardRef.set(flashcardData)
                .addOnSuccessListener(aVoid -> Log.d("FlashcardGenerator", "Flashcard saved: " + flashcardData.get("word")))
                .addOnFailureListener(e -> Log.e("FlashcardGenerator", "Error saving flashcard: " + e.getMessage()));
    }
}


