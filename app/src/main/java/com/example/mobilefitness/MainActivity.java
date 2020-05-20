package com.example.mobilefitness;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button button_face;
    Button button_game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_fragment);

        button_face = (Button) findViewById(R.id.button_face_navigation);
        button_face.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                openFaceActivity();
            }
        });

        button_game = (Button) findViewById(R.id.button_game_navigation);
        button_game.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGameActivity();
            }
        });

    }

    public void openGameActivity() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    public void openFaceActivity() {
        Intent intent = new Intent(this, FaceActivity.class);
        startActivity(intent);
    }
}
