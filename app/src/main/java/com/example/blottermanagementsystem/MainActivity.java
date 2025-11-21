package com.example.blottermanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.ui.activities.BaseActivity;
import com.example.blottermanagementsystem.ui.activities.OnboardingActivity;
import com.example.blottermanagementsystem.ui.activities.AdminDashboardActivity;
import com.example.blottermanagementsystem.ui.activities.OfficerDashboardActivity;
import com.example.blottermanagementsystem.ui.activities.UserDashboardActivity;
import com.example.blottermanagementsystem.ui.activities.ProfilePictureSelectionActivity;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {
    
    private PreferencesManager preferencesManager;
    private BlotterDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // MainActivity is just a router - no layout needed
        android.util.Log.d("MainActivity", "🚀 MainActivity started - routing to appropriate screen");
        
        preferencesManager = new PreferencesManager(this);
        database = BlotterDatabase.getDatabase(this);
        
        createAdminAccountIfNotExists();
        
        // Determine start destination - SYNCED WITH KOTLIN VERSION
        android.util.Log.d("MainActivity", "Checking onboarding: " + preferencesManager.isOnboardingCompleted());
        android.util.Log.d("MainActivity", "Checking permissions: " + preferencesManager.isPermissionsGranted());
        android.util.Log.d("MainActivity", "Checking logged in: " + preferencesManager.isLoggedIn());
        
        if (!preferencesManager.isOnboardingCompleted()) {
            // Show onboarding first (once only)
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        } else if (!preferencesManager.isPermissionsGranted()) {
            // Show permissions screen (once only, after onboarding)
            startActivity(new Intent(this, com.example.blottermanagementsystem.ui.activities.PermissionsSetupActivity.class));
            finish();
        } else if (!preferencesManager.isLoggedIn()) {
            // Not logged in - show login/register
            android.util.Log.d("MainActivity", "✅ Going to AuthActivity (Login/Register)");
            startActivity(new Intent(this, com.example.blottermanagementsystem.ui.activities.WelcomeActivity.class));
            finish();
        } else {
            // Logged in - check role and navigate accordingly
            String role = preferencesManager.getUserRole();
            
            if ("Admin".equals(role)) {
                // Admin - go directly to dashboard
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
            } else if ("Officer".equals(role)) {
                // Officer - check if password has been changed
                if (!preferencesManager.hasPasswordChanged()) {
                    // Password not changed yet - go to welcome screen
                    android.util.Log.d("MainActivity", "✅ Officer password not changed - going to OfficerWelcomeActivity");
                    startActivity(new Intent(this, com.example.blottermanagementsystem.ui.activities.OfficerWelcomeActivity.class));
                } else {
                    // Password already changed - go to dashboard
                    android.util.Log.d("MainActivity", "✅ Officer password changed - going to OfficerDashboardActivity");
                    startActivity(new Intent(this, OfficerDashboardActivity.class));
                }
                finish();
            } else {
                // Regular Users - check if they selected profile picture
                // Need to check database, so do it in background thread
                checkProfilePictureAndNavigate();
            }
        }
    }
    
    private void checkProfilePictureAndNavigate() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int userId = preferencesManager.getUserId();
                User user = database.userDao().getUserById(userId);
                boolean hasProfilePhotoInDb = user != null && user.getProfilePhotoUri() != null && !user.getProfilePhotoUri().isEmpty();
                
                // If user has profile photo in DB, set the flag to true
                if (hasProfilePhotoInDb) {
                    preferencesManager.setHasSelectedProfilePicture(true);
                    android.util.Log.d("MainActivity", "✅ User has profile photo in DB, setting flag to TRUE");
                }
                
                boolean hasSelectedPfp = preferencesManager.hasSelectedProfilePicture();
                android.util.Log.d("MainActivity", "User hasSelectedProfilePicture: " + hasSelectedPfp);
                
                // Navigate on UI thread
                runOnUiThread(() -> {
                    Intent intent;
                    if (!hasSelectedPfp) {
                        android.util.Log.d("MainActivity", "→ Going to ProfilePictureSelectionActivity");
                        intent = new Intent(this, ProfilePictureSelectionActivity.class);
                    } else {
                        android.util.Log.d("MainActivity", "→ Going to UserDashboardActivity");
                        intent = new Intent(this, UserDashboardActivity.class);
                    }
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error checking profile picture: " + e.getMessage());
                e.printStackTrace();
                // Fallback: go to dashboard
                runOnUiThread(() -> {
                    startActivity(new Intent(this, UserDashboardActivity.class));
                    finish();
                });
            }
        });
    }
    
    private void createAdminAccountIfNotExists() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Check if admin account exists
            User existingAdmin = database.userDao().getUserByUsername("admin");
            
            if (existingAdmin == null) {
                // Create built-in admin account with hashed password
                String hashedPassword = hashPassword("BMS2025");
                User admin = new User("System", "Administrator", "admin", hashedPassword, "Admin");
                admin.setActive(true);
                database.userDao().insertUser(admin);
                android.util.Log.d("MainActivity", "✅ Default admin account created: admin/BMS2025");
            } else {
                // Update existing admin password to hashed version if it's still plain text
                if (existingAdmin.getPassword().equals("admin123")) {
                    String hashedPassword = hashPassword("BMS2025");
                    existingAdmin.setPassword(hashedPassword);
                    database.userDao().updateUser(existingAdmin);
                    android.util.Log.d("MainActivity", "✅ Admin password updated to hashed version");
                }
            }
        });
    }
    
    /**
     * Hash password using SHA-256
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error hashing password", e);
            return password; // Fallback to plain text (not recommended)
        }
    }
}
