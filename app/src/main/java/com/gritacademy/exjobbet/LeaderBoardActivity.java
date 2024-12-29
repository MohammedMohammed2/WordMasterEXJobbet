package com.gritacademy.exjobbet;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.List;
import java.util.Map;

public class LeaderBoardActivity extends AppCompatActivity {

    private LinearLayout leaderboardLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leader_board);

        leaderboardLayout = findViewById(R.id.leaderboardLayout);

        // Fetch leaderboard data from Firestore
        fetchLeaderboardData();
    }

    private void fetchLeaderboardData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get the top 10 leaderboard entries sorted by score
        db.collection("leaderboard")
                .orderBy("guessTheSynonyms.score", Query.Direction.DESCENDING) // Sort by the score inside the 'guessTheSynonyms' map
                .limit(10) // Fetch only the top 10 entries
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                // Extract the 'guessTheSynonyms' map from the document
                                Map<String, Object> guessTheSynonyms = (Map<String, Object>) document.get("guessTheSynonyms");

                                if (guessTheSynonyms != null) {
                                    // Extract username, score, and timestamp from the 'guessTheSynonyms' map
                                    String username = (String) guessTheSynonyms.get("username");
                                    Long score = (Long) guessTheSynonyms.get("score");
                                    Timestamp timestampObj = (Timestamp) guessTheSynonyms.get("date");

                                    // Log values to check if they are correct
                                    Log.d("Leaderboard", "Username: " + username);
                                    Log.d("Leaderboard", "Score: " + score);
                                    Log.d("Leaderboard", "Timestamp: " + timestampObj);

                                    // Make sure we have valid data before proceeding
                                    if (username != null && score != null && timestampObj != null) {
                                        // Convert timestamp to a readable format
                                        String timestamp = DateFormat.format("MM/dd/yyyy hh:mm:ss", timestampObj.toDate()).toString();

                                        // Create the leaderboard entry card
                                        CardView cardView = new CardView(this);
                                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        layoutParams.setMargins(0, 0, 0, 16);
                                        cardView.setLayoutParams(layoutParams);

                                        // Set the card background color
                                        int backgroundColor = getCardBackgroundColor(score);
                                        cardView.setCardBackgroundColor(backgroundColor);

                                        // Set the card elevation and radius
                                        cardView.setCardElevation(8f);
                                        cardView.setRadius(12f);

                                        // Create a TextView for displaying the leaderboard entry
                                        TextView textView = new TextView(this);
                                        textView.setText(username + "\nScore: " + score + "\nTimestamp: " + timestamp);
                                        textView.setPadding(16, 16, 16, 16);
                                        textView.setTextSize(16f);

                                        // Add the TextView to the CardView
                                        cardView.addView(textView);

                                        // Add the CardView to the leaderboard layout
                                        if (leaderboardLayout != null) {
                                            leaderboardLayout.addView(cardView);
                                        } else {
                                            Log.e("Leaderboard", "Leaderboard layout is null!");
                                        }
                                    } else {
                                        Log.e("Leaderboard", "Invalid data for leaderboard entry: " + document.getId());
                                    }
                                } else {
                                    Log.e("Leaderboard", "No 'guessTheSynonyms' map found in document: " + document.getId());
                                }
                            }
                        } else {
                            Log.e("Leaderboard", "No leaderboard data found");
                            Toast.makeText(this, "No leaderboard data found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("Leaderboard", "Error fetching leaderboard data: " + task.getException());
                        Toast.makeText(this, "Error fetching leaderboard data", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    // Method to return different background color based on the score
    private int getCardBackgroundColor(Long score) {
        if (score >= 50) {
            return getResources().getColor(android.R.color.holo_green_light);
        } else if (score >= 30) {
            return getResources().getColor(android.R.color.holo_orange_light);
        } else {
            return getResources().getColor(android.R.color.holo_purple);
        }
    }
}



