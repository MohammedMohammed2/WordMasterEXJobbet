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
import androidx.appcompat.app.AlertDialog;
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
    private int maxAttempts = 5;
    private int currentAttempt = 0;

    private FirebaseFirestore db;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wordle);

        db = FirebaseFirestore.getInstance();

        guessGrid = findViewById(R.id.guessGrid);
        feedbackTextView = findViewById(R.id.feedbackTextView);
        guessInput = findViewById(R.id.guessInput);
        submitButton = findViewById(R.id.submitButton);

        fetchWordsFromFirestore();

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

        if (guessedWord.length() != 5) {
            Toast.makeText(this, "Enter a 5-letter word", Toast.LENGTH_SHORT).show();
            return;
        }

        if (guessedWord.equals(targetWord)) {
            feedbackTextView.setText("You guessed the word!");
            String feedback = "GGGGG";
            displayFeedback(guessedWord, feedback);
            showEndGameDialog("You guessed the word!");
            return;
        }

        String feedback = generateFeedback(guessedWord);
        displayFeedback(guessedWord, feedback);

        currentAttempt++;
        if (currentAttempt >= maxAttempts) {
            feedbackTextView.setText("Game Over! The word was: " + targetWord);
            showEndGameDialog("Game Over! The word was: " + targetWord);
        }
    }

    private String generateFeedback(String guessedWord) {
        guessedWord = guessedWord.toUpperCase();
        targetWord = targetWord.toUpperCase();

        StringBuilder feedback = new StringBuilder();
        boolean[] usedInWord = new boolean[5];
        boolean[] usedInGuess = new boolean[5];

        for (int i = 0; i < 5; i++) {
            if (guessedWord.charAt(i) == targetWord.charAt(i)) {
                feedback.append('G');
                usedInWord[i] = true;
                usedInGuess[i] = true;
            } else {
                feedback.append('N');
            }
        }

        for (int i = 0; i < 5; i++) {
            if (feedback.charAt(i) == 'N') {
                char guessedChar = guessedWord.charAt(i);
                for (int j = 0; j < 5; j++) {
                    if (targetWord.charAt(j) == guessedChar && !usedInWord[j] && !usedInGuess[i]) {
                        feedback.setCharAt(i, 'Y');
                        usedInWord[j] = true;
                        usedInGuess[i] = true;
                        break;
                    }
                }
            }
        }

        return feedback.toString();
    }

    private void displayFeedback(String guessedWord, String feedback) {
        Log.d(TAG, "Guess: " + guessedWord + " | Feedback: " + feedback);

        for (int i = 0; i < 5; i++) {
            TextView cell = (TextView) guessGrid.getChildAt(currentAttempt * 5 + i);
            cell.setText(String.valueOf(guessedWord.charAt(i)));

            char feedbackChar = feedback.charAt(i);

            switch (feedbackChar) {
                case 'G':
                    cell.setBackgroundColor(ContextCompat.getColor(this, R.color.green));
                    break;
                case 'Y':
                    cell.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow));
                    break;
                case 'N':
                default:
                    cell.setBackgroundColor(ContextCompat.getColor(this, R.color.gray));
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

    private void showEndGameDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> {
                    resetGame();  // Reset game state and restart
                    dialog.dismiss();
                })
                .setNegativeButton("Exit", (dialog, which) -> {
                    finish();  // Close the activity
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void resetGame() {
        // Reset the game variables
        currentAttempt = 0;
        targetWord = getRandomWord();

        guessGrid.removeAllViews();

        // Rebuild the grid with fresh cells
        setupGuessGrid();

        // Clear the feedback and input fields
        feedbackTextView.setText("");
        guessInput.setText("");
    }
}
