package com.gritacademy.exjobbet;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView welcomeTextView, leaderboardScoreTextView, totalWordsTextView, progressLabelTextView;
    private LinearProgressIndicator progressBar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        welcomeTextView = findViewById(R.id.welcomeTextView);
        leaderboardScoreTextView = findViewById(R.id.leaderboardScoreTextView);
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

        // Access header views
        TextView headerUserName = navigationView.getHeaderView(0).findViewById(R.id.nav_header_user_name);
        TextView headerEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);

        // Get the current user and update UI
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            headerEmail.setText(currentUser.getEmail());
            String userId = currentUser.getUid();

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
                        headerUserName.setText("Error");
                    });

            // Fetch and display user score from the leaderboard
            loadUserScore(userId);
            fetchTotalWordsAndUpdateProgress(userId);
        }
    }

    private void loadUserScore(String userId) {
        db.collection("leaderboard").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long score = documentSnapshot.getLong("score");
                        if (score != null) {
                            leaderboardScoreTextView.setText("Score: " + score);
                        } else {
                            leaderboardScoreTextView.setText("Score: --");
                            Toast.makeText(DashboardActivity.this, "Score not available.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        leaderboardScoreTextView.setText("Score: --");
                        Toast.makeText(DashboardActivity.this, "No leaderboard entry found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(DashboardActivity.this, "Failed to load leaderboard score.", Toast.LENGTH_SHORT).show());
    }

    private void fetchTotalWordsAndUpdateProgress(String userId) {
        db.collection("flashcards").get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalWords = querySnapshot.size();  // Get the total number of flashcards
                    totalWordsTextView.setText("Total Words in Flashcards: " + totalWords);

                    // Fetch user score and calculate progress
                    db.collection("leaderboard").document(userId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Long score = documentSnapshot.getLong("score");
                                    int progress = (int) ((score != null ? score : 0) * 100.0 / totalWords);
                                    progressBar.setProgress(progress);
                                    progressLabelTextView.setText("Progress: " + progress + "%");
                                }
                            });
                })
                .addOnFailureListener(e -> {
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
        // Uncomment and add your logic to navigate to the profile screen
        // startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
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
