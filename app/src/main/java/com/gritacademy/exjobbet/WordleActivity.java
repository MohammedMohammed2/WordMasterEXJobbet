package com.gritacademy.exjobbet;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordleActivity extends AppCompatActivity {

    private static final String TAG = "WordleActivity";

    private GridLayout guessGrid;
    private TextView feedbackTextView;
    private Button submitButton;
    private EditText guessInput;
    private String targetWord;
    private List<String> wordList;
    private int maxAttempts = 6;
    private int currentAttempt = 0;

    private FirebaseFirestore db;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wordle);

        db = FirebaseFirestore.getInstance();

        // Bind UI components
        guessGrid = findViewById(R.id.guessGrid);
        feedbackTextView = findViewById(R.id.feedbackTextView);
        guessInput = findViewById(R.id.guessInput);
        submitButton = findViewById(R.id.submitButton);

        fetchWordsFromFirestore();

        // Handle guess submission
        submitButton.setOnClickListener(v -> handleGuess());
    }

    private void fetchWordsFromFirestore() {
        db.collection("flashcards")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        List<String> fetchedWords = new ArrayList<>();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String word = document.getString("word");
                            if (word != null && word.length() == 5) {
                                fetchedWords.add(word);
                            }
                        }

                        if (!fetchedWords.isEmpty()) {
                            wordList = fetchedWords;
                            targetWord = getRandomWord();
                            Log.d("momo",targetWord);
                            setupGuessGrid();
                        } else {
                            showErrorMessage("No 5-letter words found.");
                        }
                    } else {
                        showErrorMessage("No words found.");
                    }
                })
                .addOnFailureListener(e -> showErrorMessage("Failed to fetch words."));
    }

    private void setupGuessGrid() {
        guessGrid.setRowCount(maxAttempts);
        guessGrid.setColumnCount(5);

        // Create grid cells
        for (int i = 0; i < maxAttempts; i++) {
            for (int j = 0; j < 5; j++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 120;
                params.height = 120;
                params.rowSpec = GridLayout.spec(i);
                params.columnSpec = GridLayout.spec(j);
                params.setMargins(4, 4, 4, 4);

                cell.setLayoutParams(params);
                cell.setGravity(Gravity.CENTER);
                cell.setBackgroundResource(R.drawable.cell_background);
                cell.setTextSize(18);

                guessGrid.addView(cell);
            }
        }
    }

    private void handleGuess() {
        String guessedWord = guessInput.getText().toString().toUpperCase();

        // Check if guess is a valid 5-letter word
        if (guessedWord.length() != 5) {
            Toast.makeText(this, "Enter a 5-letter word", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if guessed word is correct
        if (guessedWord.equals(targetWord)) {
            feedbackTextView.setText("You guessed the word!");
            String feedback = "GGGGG";  // All correct letters
            displayFeedback(guessedWord, feedback);
            return;
        }

        // Generate feedback for the guess
        String feedback = generateFeedback(guessedWord);
        displayFeedback(guessedWord, feedback);

        currentAttempt++;
        if (currentAttempt >= maxAttempts) {
            feedbackTextView.setText("Game Over! The word was: " + targetWord);
        }
    }

    private String generateFeedback(String guessedWord) {
        // Normalize to uppercase to avoid case sensitivity issues
        guessedWord = guessedWord.toUpperCase();
        targetWord = targetWord.toUpperCase();

        StringBuilder feedback = new StringBuilder();
        boolean[] usedInWord = new boolean[5];  // Tracks if a letter in the target word is used
        boolean[] usedInGuess = new boolean[5];

        // Check for correct letters in the correct positions (Green)
        for (int i = 0; i < 5; i++) {
            if (guessedWord.charAt(i) == targetWord.charAt(i)) {
                feedback.append('G');
                usedInWord[i] = true;
                usedInGuess[i] = true;
            } else {
                feedback.append('N');  // Default to incorrect
            }
        }

        // Second pass: Check for correct letters in wrong positions (Yellow)
        for (int i = 0; i < 5; i++) {
            if (feedback.charAt(i) == 'N') {  // Only consider letters that are not green
                char guessedChar = guessedWord.charAt(i);
                // Check if guessedChar exists in the target word and hasn't been used already
                for (int j = 0; j < 5; j++) {
                    if (targetWord.charAt(j) == guessedChar && !usedInWord[j] && !usedInGuess[i]) {
                        feedback.setCharAt(i, 'Y');  // Correct letter in wrong position
                        usedInWord[j] = true;  // Mark this position as used for yellow
                        usedInGuess[i] = true;  // Mark this letter as used for yellow
                        break;  // Exit the inner loop once we find a match
                    }
                }
            }
        }

        return feedback.toString();
    }


    private void displayFeedback(String guessedWord, String feedback) {
        Log.d(TAG, "Guess: " + guessedWord + " | Feedback: " + feedback);

        // Loop through each letter and apply colors
        for (int i = 0; i < 5; i++) {
            TextView cell = (TextView) guessGrid.getChildAt(currentAttempt * 5 + i);
            cell.setText(String.valueOf(guessedWord.charAt(i)));  // Set the guessed letter

            char feedbackChar = feedback.charAt(i);  // Get the feedback for this letter

            // Apply color based on feedback
            switch (feedbackChar) {
                case 'G':
                    // Use ContextCompat.getColor() to handle deprecated getColor()
                    cell.setBackgroundColor(ContextCompat.getColor(this, R.color.green));  // Correct letter in correct position
                    break;
                case 'Y':
                    cell.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow));  // Correct letter in wrong position
                    break;
                case 'N':
                default:
                    cell.setBackgroundColor(ContextCompat.getColor(this, R.color.gray));  // Incorrect letter
                    break;
            }
        }
    }




    private String getRandomWord() {
        Random random = new Random();
        return wordList.get(random.nextInt(wordList.size()));
    }

    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
