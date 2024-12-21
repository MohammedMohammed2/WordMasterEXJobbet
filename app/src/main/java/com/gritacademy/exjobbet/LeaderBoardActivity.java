package com.gritacademy.exjobbet;

import android.os.Bundle;
import android.text.format.DateFormat;
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
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(10) // Fetch only the top 10 entries
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                String username = document.getString("username");
                                Long score = document.getLong("score");
                                Timestamp timestampObj = document.getTimestamp("timestamp");

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
                                    leaderboardLayout.addView(cardView);
                                }
                            }
                        } else {
                            Toast.makeText(this, "No leaderboard data found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error fetching leaderboard data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to return different background color based on the score
    private int getCardBackgroundColor(Long score) {
        if (score >= 50) {
            return getResources().getColor(android.R.color.holo_green_light); // Green for high scores
        } else if (score >= 30) {
            return getResources().getColor(android.R.color.holo_orange_light); // Orange for medium scores
        } else {
            return getResources().getColor(android.R.color.holo_purple); // Red for low scores
        }
    }
}



