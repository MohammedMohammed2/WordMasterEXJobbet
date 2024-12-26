package com.gritacademy.exjobbet;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class GameModesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_modes);  // Ensure correct layout file

        // Game mode buttons
        Button guessTheSynonymButton = findViewById(R.id.guessTheSynonymButton);
        // Game mode buttons
        Button synonymSniper = findViewById(R.id.flashcardGameButton);

        // Set listeners for each game mode
        guessTheSynonymButton.setOnClickListener(v -> startActivity(new Intent(GameModesActivity.this, GuessTheSynonymActivity.class)));
        synonymSniper.setOnClickListener(v-> startActivity(new Intent(GameModesActivity.this, SynonymSniperActivity.class)));

    }

}



