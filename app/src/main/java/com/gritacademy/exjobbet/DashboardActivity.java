package com.gritacademy.exjobbet;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView welcomeTextView, bestScoreView, worstScoreView, totalWordsTextView, progressLabelTextView;
    private LinearProgressIndicator progressBar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        welcomeTextView = findViewById(R.id.welcomeTextView);
        totalWordsTextView = findViewById(R.id.totalWordsTextView);
        progressBar = findViewById(R.id.progressBar);
        progressLabelTextView = findViewById(R.id.progressLabelTextView);

        // Setup Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchData();
    }

    private void fetchData() {
        // Get the current user and update UI
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Access header views
            NavigationView navigationView = findViewById(R.id.nav_view);
            TextView headerUserName = navigationView.getHeaderView(0).findViewById(R.id.nav_header_user_name);
            TextView headerEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);

            // Set email in header
            headerEmail.setText(currentUser.getEmail());

            // Fetch username and display
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            headerUserName.setText(username);
                            welcomeTextView.setText("Welcome, " + username);
                        } else {
                            headerUserName.setText("No username found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(DashboardActivity.this, "Failed to fetch username.", Toast.LENGTH_SHORT).show();
                    });

            // Fetch and display user progress
            loadUserScore(userId);
            fetchTotalWordsAndUpdateProgress(userId);
        }
    }

    private void loadUserScore(String userId) {
        LinearLayout gameModeContainer = findViewById(R.id.gameModeContainer);

        // Clear existing game mode data to prevent duplication
        gameModeContainer.removeAllViews();

        db.collection("userProgress").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Loop through all game modes in the document
                        for (String gameMode : documentSnapshot.getData().keySet()) {
                            Map<String, Object> gameModeData = (Map<String, Object>) documentSnapshot.get(gameMode);

                            if (gameModeData != null) {
                                Long bestScore = (Long) gameModeData.get("best_score");
                                Long worstScore = (Long) gameModeData.get("worst_score");

                                // Dynamically add a row for this game mode
                                addGameModeRow(gameMode, bestScore, worstScore, gameModeContainer);
                            }
                        }
                    } else {
                        Toast.makeText(DashboardActivity.this, "No progress entry found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DashboardActivity.this, "Failed to load progress.", Toast.LENGTH_SHORT).show();
                });
    }

    private void addGameModeRow(String gameMode, Long bestScore, Long worstScore, LinearLayout container) {
        LinearLayout gameModeContainer = new LinearLayout(this);
        gameModeContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        gameModeContainer.setOrientation(LinearLayout.VERTICAL);
        gameModeContainer.setPadding(16, 16, 16, 16);
        gameModeContainer.setBackgroundResource(R.drawable.container_background);

        // Add the game mode title
        TextView gameModeTitle = new TextView(this);
        gameModeTitle.setText("GameMode: " + gameMode);
        gameModeTitle.setTextSize(18f);
        gameModeTitle.setTextColor(getResources().getColor(R.color.black));
        gameModeTitle.setPadding(0, 0, 0, 8);
        gameModeTitle.setTypeface(null, Typeface.BOLD);
        gameModeContainer.addView(gameModeTitle);

        // Display best score if available
        if (bestScore != null) {
            TextView bestScoreText = new TextView(this);
            bestScoreText.setText("Best Score: " + bestScore);
            bestScoreText.setTextSize(16f);
            bestScoreText.setTextColor(getResources().getColor(R.color.green));
            bestScoreText.setPadding(0, 0, 0, 8);
            gameModeContainer.addView(bestScoreText);
        }

        // Display worst score if available
        if (worstScore != null) {
            TextView worstScoreText = new TextView(this);
            worstScoreText.setText("Worst Score: " + worstScore);
            worstScoreText.setTextSize(16f);
            worstScoreText.setTextColor(getResources().getColor(R.color.red));
            worstScoreText.setPadding(0, 0, 0, 8);
            gameModeContainer.addView(worstScoreText);
        }

        // Add the game mode container to the main container
        container.addView(gameModeContainer);
    }

    private void fetchTotalWordsAndUpdateProgress(String userId) {
        db.collection("flashcards").get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalWords = querySnapshot.size(); // Get the total number of flashcards
                    Log.d("ProgressDebug", "Total Words: " + totalWords);

                    totalWordsTextView.setText("Total Words in Flashcards: " + totalWords);

                    if (totalWords == 0) {
                        progressBar.setProgress(0);
                        progressLabelTextView.setText("Progress: 0%");
                        return; // No flashcards, exit early
                    }

                    // Fetch user progress from leaderboard
                    db.collection("leaderboard").document(userId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    // Access "guessTheSynonyms" data
                                    Map<String, Object> guessTheSynonymsData =
                                            (Map<String, Object>) documentSnapshot.get("guessTheSynonyms");

                                    if (guessTheSynonymsData != null) {
                                        Long score = (Long) guessTheSynonymsData.get("score");

                                        if (score != null) {
                                            int progress = (int) ((score * 100.0) / totalWords);
                                            progressBar.setProgress(progress);
                                            progressLabelTextView.setText("Progress: " + progress + "%");
                                        } else {
                                            progressBar.setProgress(0);
                                            progressLabelTextView.setText("Progress: 0%");
                                        }
                                    } else {
                                        progressBar.setProgress(0);
                                        progressLabelTextView.setText("Progress: 0%");
                                    }
                                } else {
                                    progressBar.setProgress(0);
                                    progressLabelTextView.setText("Progress: 0%");
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ProgressDebug", "Failed to fetch score", e);
                                Toast.makeText(DashboardActivity.this, "Failed to fetch score.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ProgressDebug", "Failed to fetch words", e);
                    Toast.makeText(DashboardActivity.this, "Failed to fetch words.", Toast.LENGTH_SHORT).show();
                });
    }

    // Handle navigation item clicks
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_leaderboard) {
            navigateToLeaderboard();
        } else if (id == R.id.nav_game_modes) {
            navigateToGameModes();
        } else if (id == R.id.nav_profile) {
            navigateToProfile();
        } else if (id == R.id.nav_logout) {
            logoutUser();
        }

        drawerLayout.closeDrawers();
        return true;
    }

    private void navigateToLeaderboard() {
        startActivity(new Intent(DashboardActivity.this, LeaderBoardActivity.class));
    }

    private void navigateToGameModes() {
        startActivity(new Intent(DashboardActivity.this, GameModesActivity.class));
    }

    private void navigateToProfile() {
        startActivity(new Intent(DashboardActivity.this, DashboardActivity.class));
    }

    private void logoutUser() {
        mAuth.signOut();
        startActivity(new Intent(DashboardActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
