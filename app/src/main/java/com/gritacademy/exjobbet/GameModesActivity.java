package com.gritacademy.exjobbet;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;


public class GameModesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_modes);


        Button guessTheSynonymButton = findViewById(R.id.guessTheSynonymButton);

        Button synonymSniper = findViewById(R.id.flashcardGameButton);

       Button crossword = findViewById(R.id.wordle);

        guessTheSynonymButton.setOnClickListener(v -> startActivity(new Intent(GameModesActivity.this, GuessTheSynonymActivity.class)));
        synonymSniper.setOnClickListener(v-> startActivity(new Intent(GameModesActivity.this, SynonymSniperActivity.class)));
        crossword.setOnClickListener(v->startActivity(new Intent(GameModesActivity.this , WordleActivity.class)));

    }

}



