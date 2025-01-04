package com.gritacademy.exjobbet;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GuessTheSynonymActivity extends AppCompatActivity {

    private TextView wordTextView;
    private Button option1Button, option2Button, option3Button, option4Button, nextQuestionButton, giveUpButton;
    private TextView scoreTextView;

    private int score = 0;
    private String correctAnswer;
    private FirebaseFirestore firestore;
    private List<DocumentSnapshot> wordDocuments;
    private int currentQuestionIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guess_the_synonym);

        firestore = FirebaseFirestore.getInstance();
        wordDocuments = new ArrayList<>();

        wordTextView = findViewById(R.id.wordTextView);
        option1Button = findViewById(R.id.option1Button);
        option2Button = findViewById(R.id.option2Button);
        option3Button = findViewById(R.id.option3Button);
        option4Button = findViewById(R.id.option4Button);
        nextQuestionButton = findViewById(R.id.nextQuestionButton);
        giveUpButton = findViewById(R.id.giveUpButton);
        scoreTextView = findViewById(R.id.scoreTextView);

        // Fetch words from Firestore
        fetchFlashcards();

        // Set click listeners for answer buttons
        option1Button.setOnClickListener(v -> checkAnswer(option1Button.getText().toString()));
        option2Button.setOnClickListener(v -> checkAnswer(option2Button.getText().toString()));
        option3Button.setOnClickListener(v -> checkAnswer(option3Button.getText().toString()));
        option4Button.setOnClickListener(v -> checkAnswer(option4Button.getText().toString()));

        // Set listener for Next Question button
        nextQuestionButton.setOnClickListener(v -> loadNextQuestion());

        giveUpButton.setOnClickListener(v -> {
            Toast.makeText(this, "Returning to Game Modes...", Toast.LENGTH_SHORT).show();

            FirebaseAuth auth = FirebaseAuth.getInstance();
            String uid = auth.getCurrentUser().getUid();

            updateUserProgress(score);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot userDocument = task.getResult();
                            if (userDocument.exists()) {
                                String username = userDocument.getString("username");
                                saveLeaderboardEntry(uid, username);
                            } else {
                                Log.e("Firestore", "No user found with UID: " + uid);
                            }
                        } else {
                            Log.e("Firestore", "Error getting username: ", task.getException());
                        }
                    });

            finish();
        });
    }
    private void saveLeaderboardEntry(String uid, String username) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get user's progress
        db.collection("userProgress").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot progressDocument = task.getResult();
                        if (progressDocument.exists()) {
                            // Get the 'guessTheSynonyms' map
                            Map<String, Object> guessTheSynonyms = (Map<String, Object>) progressDocument.get("guessTheSynonyms");

                            if (guessTheSynonyms != null) {
                                // Get the best score
                                Long bestScore = (Long) guessTheSynonyms.get("best_score");

                                if (bestScore != null) {
                                    // Check if leaderboard entry exists
                                    db.collection("leaderboard").document(uid)
                                            .get()
                                            .addOnCompleteListener(leaderboardTask -> {
                                                if (leaderboardTask.isSuccessful()) {
                                                    DocumentSnapshot leaderboardDocument = leaderboardTask.getResult();
                                                    boolean shouldUpdate = false;

                                                    if (leaderboardDocument.exists()) {
                                                        // Compare with existing score
                                                        Map<String, Object> existingGameModeData = (Map<String, Object>) leaderboardDocument.get("guessTheSynonyms");
                                                        if (existingGameModeData != null) {
                                                            Long currentLeaderboardScore = (Long) existingGameModeData.get("score");
                                                            if (currentLeaderboardScore == null || bestScore > currentLeaderboardScore) {
                                                                shouldUpdate = true;
                                                            }
                                                        }
                                                    } else {
                                                        // No entry, create new one
                                                        shouldUpdate = true;
                                                    }

                                                    if (shouldUpdate) {
                                                        // Prepare leaderboard data
                                                        Map<String, Object> leaderboardGameModeData = new HashMap<>();
                                                        leaderboardGameModeData.put("username", username);
                                                        leaderboardGameModeData.put("score", bestScore);
                                                        leaderboardGameModeData.put("date", FieldValue.serverTimestamp());

                                                        Map<String, Object> leaderboardEntry = new HashMap<>();
                                                        leaderboardEntry.put("guessTheSynonyms", leaderboardGameModeData);

                                                        // Update leaderboard
                                                        db.collection("leaderboard").document(uid)
                                                                .set(leaderboardEntry,SetOptions.merge())
                                                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Leaderboard updated for UID: " + uid))
                                                                .addOnFailureListener(e -> Log.e("Firestore", "Error updating leaderboard: ", e));
                                                    } else {
                                                        Log.d("Firestore", "Score not updated.");
                                                    }
                                                } else {
                                                    Log.e("Firestore", "Error fetching leaderboard: ", leaderboardTask.getException());
                                                }
                                            });
                                } else {
                                    Log.e("Firestore", "Best score is null.");
                                }
                            } else {
                                Log.e("Firestore", "guessTheSynonyms data not found.");
                            }
                        } else {
                            Log.e("Firestore", "No userProgress found.");
                        }
                    } else {
                        Log.e("Firestore", "Error fetching userProgress: ", task.getException());
                    }
                });
    }


    private void fetchFlashcards() {
        Log.d("Firestore", "Fetching flashcards...");
        firestore.collection("flashcards").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Log.d("Firestore", "Fetched document: " + doc.getId());
                                Log.d("Firestore", "Word: " + doc.getString("word"));

                                Object synonymsObj = doc.get("synonyms");

                                if (synonymsObj instanceof List) {
                                    List<String> synonyms = (List<String>) synonymsObj;
                                    Log.d("Firestore", "Synonyms: " + synonyms);
                                } else {
                                    Log.e("Firestore", "Synonyms field is not a List for document: " + doc.getId());
                                }
                            }

                            wordDocuments.addAll(querySnapshot.getDocuments());
                            Log.d("Firestore", "Documents fetched successfully. Total docs: " + querySnapshot.size());

                            runOnUiThread(() -> loadNextQuestion());
                        } else {
                            Log.e("Firestore", "No flashcards found!");
                            Toast.makeText(this, "No flashcards found!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("Firestore", "Failed to fetch data: " + task.getException().getMessage());
                        Toast.makeText(this, "Failed to fetch data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean answeredCorrectly = false;

    private void loadNextQuestion() {
        if (wordDocuments.isEmpty()) {
            Log.d("Game", "No more questions available!");
            Toast.makeText(this, "No more questions available!", Toast.LENGTH_SHORT).show();
            return;
        }

        Random random = new Random();
        DocumentSnapshot document = wordDocuments.get(random.nextInt(wordDocuments.size()));

        String word = document.getString("word");

        Object synonymsObj = document.get("synonyms");
        List<String> synonyms = null;

        if (synonymsObj instanceof List) {
            synonyms = (List<String>) synonymsObj;
            Log.d("Game", "Synonyms: " + synonyms);
        } else {
            Log.e("DataError", "'synonyms' field is not a List. Found: " + synonymsObj);
        }

        if (word == null || synonyms == null || synonyms.isEmpty()) {
            Log.e("DataError", "Missing or invalid 'synonyms' for word: " + word);
            return;
        }

        correctAnswer = synonyms.get(0);

        Log.d("Game", "Word: " + word + ", Correct Answer: " + correctAnswer);

        List<String> incorrectOptions = new ArrayList<>();

        // Collect incorrect options from other words (not the current word)
        while (incorrectOptions.size() < 3) {
            DocumentSnapshot randomDoc = wordDocuments.get(random.nextInt(wordDocuments.size()));
            String randomWord = randomDoc.getString("word");

            if (!randomWord.equals(word)) {
                List<String> randomSynonyms = (List<String>) randomDoc.get("synonyms");

                if (randomSynonyms != null && !randomSynonyms.isEmpty()) {
                    String randomSynonym = randomSynonyms.get(0);
                    if (!randomSynonym.equals(correctAnswer) && !incorrectOptions.contains(randomSynonym)) {
                        incorrectOptions.add(randomSynonym);
                    }
                }
            }
        }

        // Add the correct answer to the options
        List<String> options = new ArrayList<>(incorrectOptions);
        options.add(correctAnswer);
        Collections.shuffle(options);

        wordTextView.setText(word);
        option1Button.setText(options.get(0));
        option2Button.setText(options.get(1));
        option3Button.setText(options.get(2));
        option4Button.setText(options.get(3));

        Log.d("Game", "Options: " + options);

        answeredCorrectly = false;
    }

    private void updateUserProgress(int newScore) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid;

        if (auth.getCurrentUser() != null) {
            uid = auth.getCurrentUser().getUid();
        } else {
            Log.e("Firestore", "No authenticated user found.");
            return; // Exit if no user is logged in
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String gameMode = "guessTheSynonyms"; // Current game mode

        db.collection("userProgress").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();

                        int bestScore = newScore;
                        int worstScore = newScore;

                        if (document.exists()) {
                            // Fetch existing scores for the game mode
                            Map<String, Object> gameModeData = (Map<String, Object>) document.get(gameMode);

                            if (gameModeData != null) {
                                Long existingBest = (Long) gameModeData.get("best_score");
                                Long existingWorst = (Long) gameModeData.get("worst_score");

                                // Update best and worst scores
                                bestScore = Math.max(existingBest != null ? existingBest.intValue() : newScore, newScore);
                                worstScore = Math.min(existingWorst != null ? existingWorst.intValue() : newScore, newScore);
                            }
                        }

                        // Prepare data for the game mode
                        Map<String, Object> gameModeData = new HashMap<>();
                        gameModeData.put("best_score", bestScore);
                        gameModeData.put("worst_score", worstScore);

                        // Save progress to Firestore
                        Map<String, Object> progressData = new HashMap<>();
                        progressData.put(gameMode, gameModeData);

                        db.collection("userProgress").document(uid)
                                .set(progressData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "User progress updated successfully"))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error updating user progress", e));
                    } else {
                        Log.e("Firestore", "Error fetching user progress: ", task.getException());
                    }
                });
    }



    private void checkAnswer(String selectedAnswer) {
        Log.d("Game", "Selected answer: " + selectedAnswer);
        if (selectedAnswer.equals(correctAnswer)) {
            score++;
            answeredCorrectly = true;
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
            loadNextQuestion();
        } else {
            Toast.makeText(this, "Wrong! Point is lost", Toast.LENGTH_SHORT).show();
            score--;
        }
        scoreTextView.setText("Score: " + score);

        nextQuestionButton.setOnClickListener(v -> {
            if (!answeredCorrectly) {
                Toast.makeText(this, "You lost a point! Moving to next question.", Toast.LENGTH_SHORT).show();
                score--;
                scoreTextView.setText("Score: " + score);
            }
            loadNextQuestion();
        });
    }
}




