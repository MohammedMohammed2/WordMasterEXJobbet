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

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gritacademy.exjobbet.flashcard.FlashcardGenerator;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CircularProgressIndicator progressBar;
    private TextView welcomeTextView, quizzesAnsweredTextView, progressTextView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FlashcardGenerator flashcardGenerator = new FlashcardGenerator();
        flashcardGenerator.generateFlashcards();

        // Initialize UI components
        welcomeTextView = findViewById(R.id.welcomeTextView);
        quizzesAnsweredTextView = findViewById(R.id.quizzesAnsweredTextView);
        progressTextView = findViewById(R.id.progressTextView);
        progressBar = findViewById(R.id.circularProgressBar);

        // Setup Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Set up the drawer toggle for hamburger menu icon
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // Enable the hamburger icon

        // Set the navigation item click listener
        navigationView.setNavigationItemSelectedListener(this);

        // Access header views
        TextView headerUserName = navigationView.getHeaderView(0).findViewById(R.id.nav_header_user_name);
        TextView headerEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);

        // Get the current user and update UI
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {

            // Set user details in the navigation drawer header
            headerEmail.setText(currentUser.getEmail());
            String userId = currentUser.getUid();
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

            welcomeTextView.setText("Welcome, " + currentUser.getEmail());
            loadUserStats(userId);
        }
    }

    private void loadUserStats(String userId) {
        db.collection("userProgress").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String quizzesAnswered = documentSnapshot.getString("quizzesAnswered");
                        String progress = documentSnapshot.getString("progress");

                        quizzesAnsweredTextView.setText("Quizzes Answered: " + quizzesAnswered);
                        progressTextView.setText("Progress: " + progress + "%");

                        int progressValue = Integer.parseInt(progress);
                        progressBar.setProgress(progressValue);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(DashboardActivity.this, "Failed to load stats.", Toast.LENGTH_SHORT).show());
    }

    // Handle action bar item click (like opening and closing the drawer)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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

        // Close the drawer after the action
        drawerLayout.closeDrawers();
        return true;
    }

    private void navigateToLeaderboard() {
        Toast.makeText(this, "Navigating to Leaderboard...", Toast.LENGTH_SHORT).show();
        // Uncomment and add your logic to navigate to the leaderboard screen
        // startActivity(new Intent(DashboardActivity.this, LeaderboardActivity.class));
    }

    private void navigateToGameModes() {
        Intent intent = new Intent(DashboardActivity.this, GameModesActivity.class);
        startActivity(intent);
    }

    private void navigateToProfile() {
        Toast.makeText(this, "Navigating to Profile...", Toast.LENGTH_SHORT).show();
        // Uncomment and add your logic to navigate to the profile screen
        // startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
    }

    private void logoutUser() {
        mAuth.signOut();
        startActivity(new Intent(DashboardActivity.this, MainActivity.class));
        finish();
    }
}
