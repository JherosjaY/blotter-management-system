package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.blottermanagementsystem.R;
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends BaseActivity {
    
    private MaterialButton btnSignIn, btnSignUp;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        
        initViews();
        setupListeners();
        animateCard();
    }
    
    private void initViews() {
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
    }
    
    private void setupListeners() {
        btnSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close Welcome screen
        });
        
        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish(); // Close Welcome screen
        });
    }
    
    private void animateCard() {
        View welcomeCard = findViewById(R.id.welcomeCard);
        
        if (welcomeCard != null) {
            welcomeCard.setAlpha(0f);
            welcomeCard.setScaleX(0.9f);
            welcomeCard.setScaleY(0.9f);
            welcomeCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(200)
                .start();
        }
    }
}
