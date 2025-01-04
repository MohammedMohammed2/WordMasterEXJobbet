package com.gritacademy.exjobbet;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SynonymSniperActivity extends AppCompatActivity {

    private RelativeLayout gameLayout;
    private TextView targetWordTextView, scoreTextView, timerTextView, remainingTextView;
    private int screenHeight, screenWidth;
    private int score = 0;
    private List<String> currentSynonyms = new ArrayList<>();
    private List<String> remainingSynonyms = new ArrayList<>();
    private List<String> decoyWords = new ArrayList<>();
    private FirebaseFirestore db;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synonym_sniper);

        gameLayout = findViewById(R.id.gameLayout);
        targetWordTextView = findViewById(R.id.targetWordTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        timerTextView = findViewById(R.id.timerTextView);
        remainingTextView = findViewById(R.id.remainingTextView);

        db = FirebaseFirestore.getInstance();

        screenHeight = getResources().getDisplayMetrics().heightPixels;
        screenWidth = getResources().getDisplayMetrics().widthPixels;

        startGame();
    }

    private void startGame() {
        score = 0;
        updateScore();
        fetchRandomWordAndSynonyms();
        startNewTimer();
    }

    // timer for game mode and when its done it finishes the game how ever its refreshed when all syonym are down
    private void startNewTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTextView.setText("Time: " + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                endGame();
            }
        };
        countDownTimer.start();
    }

    //fetch 4  or less synonym from flashcards collection
    private void fetchRandomWordAndSynonyms() {
        db.collection("flashcards").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<QueryDocumentSnapshot> flashcardsList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    flashcardsList.add(document);
                }
                if (!flashcardsList.isEmpty()) {
                    Random random = new Random();
                    QueryDocumentSnapshot randomWordDoc = flashcardsList.get(random.nextInt(flashcardsList.size()));
                    String targetWord = randomWordDoc.getString("word");
                    currentSynonyms = (List<String>) randomWordDoc.get("synonyms");

                    if (currentSynonyms.size() > 4) {
                        currentSynonyms = currentSynonyms.subList(0, 4);
                    }

                    if (currentSynonyms.isEmpty()) {
                        fetchRandomWordAndSynonyms();
                        return;
                    }

                    targetWordTextView.setText(targetWord);
                    remainingSynonyms = new ArrayList<>(currentSynonyms);
                    updateRemaining();
                    generateDecoys(flashcardsList);
                    gameLayout.removeAllViews();
                    gameLayout.addView(targetWordTextView);
                    gameLayout.addView(scoreTextView);
                    gameLayout.addView(timerTextView);
                    gameLayout.addView(remainingTextView);

                    spawnBouncingWords();
                }
            } else {
                Log.e("Firestore", "Error fetching flashcards", task.getException());
            }
        });
    }

    private void generateDecoys(List<QueryDocumentSnapshot> allFlashcards) {
        decoyWords.clear();
        for (QueryDocumentSnapshot flashcard : allFlashcards) {
            String word = flashcard.getString("word");
            if (!currentSynonyms.contains(word) && !word.equals(targetWordTextView.getText().toString())) {
                decoyWords.add(word);
            }
        }
    }

    private void spawnBouncingWords() {
        Random random = new Random();
        int totalWords = Math.min(10, currentSynonyms.size() + 5);

        for (int i = 0; i < totalWords; i++) {
            String word;
            boolean isSynonym = i < currentSynonyms.size();

            if (isSynonym) {
                word = currentSynonyms.get(i);
            } else {
                word = decoyWords.get(random.nextInt(decoyWords.size()));
            }

            spawnWord(word, isSynonym);
        }
    }

    private void spawnWord(String word, boolean isSynonym) {
        Button wordButton = new Button(this);
        wordButton.setText(word);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = getRandomX();
        params.topMargin = getRandomY();
        wordButton.setLayoutParams(params);

        gameLayout.addView(wordButton);

        Random random = new Random();
        int randomXMovement = random.nextInt(screenWidth / 2) - screenWidth / 4;
        int randomYMovement = random.nextInt(screenHeight / 2) - screenHeight / 4;

        ObjectAnimator xAnimator = ObjectAnimator.ofFloat(wordButton, "translationX", randomXMovement);
        xAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        xAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        ObjectAnimator yAnimator = ObjectAnimator.ofFloat(wordButton, "translationY", randomYMovement);
        yAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        yAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(xAnimator, yAnimator);
        animatorSet.setDuration(2000 + random.nextInt(1000));
        animatorSet.start();

        wordButton.setOnClickListener(v -> {
            if (isSynonym) {
                score++;
                wordButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                remainingSynonyms.remove(word);
                updateRemaining();
                if (remainingSynonyms.isEmpty()) {
                    fetchRandomWordAndSynonyms();
                    startNewTimer();
                }
            } else {
                score--;
                wordButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            }
            gameLayout.removeView(wordButton);
            updateScore();
        });
    }

    private int getRandomX() {
        return new Random().nextInt(screenWidth - 200);
    }

    private int getRandomY() {
        int y = new Random().nextInt(screenHeight - 300);
        int targetWordEndY = targetWordTextView.getTop() + targetWordTextView.getHeight();

        while (y >= targetWordTextView.getTop() - 100 && y <= targetWordEndY + 100) {
            y = new Random().nextInt(screenHeight - 300);
        }
        return y;
    }

    private void updateScore() {
        scoreTextView.setText("Score: " + score);
    }

    private void updateRemaining() {
        remainingTextView.setText("Remaining: " + remainingSynonyms.size());
    }


    private void updateUserProgressSynonymSniper(int newScore) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid;

        if (auth.getCurrentUser() != null) {
            uid = auth.getCurrentUser().getUid();
        } else {
            Log.e("Firestore", "No authenticated user found.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String gameMode = "synonymSniper";

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


    private void endGame() {

        updateUserProgressSynonymSniper(score);

        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("Your Score: " + score)
                .setPositiveButton("Retry", (dialog, which) -> startGame())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
    }
}








