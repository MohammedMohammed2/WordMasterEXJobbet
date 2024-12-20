package com.gritacademy.exjobbet;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GuessTheSynonymActivity extends AppCompatActivity {

    private TextView wordTextView;
    private Button option1Button, option2Button, option3Button, option4Button, nextQuestionButton;
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

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();
        wordDocuments = new ArrayList<>();

        // Initialize Views
        wordTextView = findViewById(R.id.wordTextView);
        option1Button = findViewById(R.id.option1Button);
        option2Button = findViewById(R.id.option2Button);
        option3Button = findViewById(R.id.option3Button);
        option4Button = findViewById(R.id.option4Button);
        nextQuestionButton = findViewById(R.id.nextQuestionButton);
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
    }

    private void fetchFlashcards() {
        Log.d("Firestore", "Fetching flashcards...");

        firestore.collection("flashcards").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // Log the number of documents and details for each document
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Log.d("Firestore", "Fetched document: " + doc.getId());
                                Log.d("Firestore", "Word: " + doc.getString("word"));

                                // Get synonyms field as an Object and cast it to a List
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

                            // Ensure UI updates happen on the main thread
                            runOnUiThread(() -> loadNextQuestion());  // Load the first question on the UI thread
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

    private boolean answeredCorrectly = false; // Track if the player answered correctly

    private void loadNextQuestion() {
        if (wordDocuments.isEmpty()) {
            Log.d("Game", "No more questions available!");
            Toast.makeText(this, "No more questions available!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Select a random document (word)
        Random random = new Random();
        DocumentSnapshot document = wordDocuments.get(random.nextInt(wordDocuments.size()));

        // Extract the word and synonyms
        String word = document.getString("word");

        // Get the synonyms, if available
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
            return;  // Skip this question if synonyms are missing or malformed
        }

        // Select the first synonym as the correct answer
        correctAnswer = synonyms.get(0);

        Log.d("Game", "Word: " + word + ", Correct Answer: " + correctAnswer);

        // Generate incorrect options from other words' synonyms
        List<String> incorrectOptions = new ArrayList<>();

        while (incorrectOptions.size() < 3) {
            DocumentSnapshot randomDoc = wordDocuments.get(random.nextInt(wordDocuments.size()));
            List<String> randomSynonyms = (List<String>) randomDoc.get("synonyms");

            if (randomSynonyms != null && !randomSynonyms.isEmpty()) {
                String randomSynonym = randomSynonyms.get(0);  // Get the first synonym
                if (!randomSynonym.equals(correctAnswer) && !incorrectOptions.contains(randomSynonym)) {
                    incorrectOptions.add(randomSynonym);
                }
            }
        }

        // Combine correct and incorrect options, shuffle them
        List<String> options = new ArrayList<>(incorrectOptions);
        options.add(correctAnswer);
        Collections.shuffle(options);

        // Update UI
        wordTextView.setText(word);
        option1Button.setText(options.get(0));
        option2Button.setText(options.get(1));
        option3Button.setText(options.get(2));
        option4Button.setText(options.get(3));

        Log.d("Game", "Options: " + options);

        // Reset the "answeredCorrectly" flag for the next question
        answeredCorrectly = false;
    }

    // Update the checkAnswer method to automatically move to the next question
    private void checkAnswer(String selectedAnswer) {
        Log.d("Game", "Selected answer: " + selectedAnswer);
        if (selectedAnswer.equals(correctAnswer)) {
            score++;
            answeredCorrectly = true; // Player answered correctly
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
            // Automatically load the next question
            loadNextQuestion();
        } else {
            Toast.makeText(this, "Wrong! Point is lost", Toast.LENGTH_SHORT).show();
            score--;
        }

        // Update the score
        scoreTextView.setText("Score: " + score);

        // Set listener for Next Question button
        nextQuestionButton.setOnClickListener(v -> {
            if (!answeredCorrectly) {
                // If the player didn't answer correctly, lose a point
                score--;
                Toast.makeText(this, "You lost a point! Moving to next question.", Toast.LENGTH_SHORT).show();
            }
            // Always move to the next question
            loadNextQuestion();
        });
    }

}


