package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Notification;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.data.entity.Officer;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.concurrent.Executors;

public class SendNotificationActivity extends BaseActivity {
    
    private RadioGroup radioGroupRecipients;
    private RadioButton radioAllUsers, radioSpecificUsers, radioAllAdmins, radioAllOfficers;
    private TextInputEditText etNotificationTitle, etNotificationMessage;
    private MaterialButton btnSendNotification;
    private BlotterDatabase database;
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_notification);
        
        database = BlotterDatabase.getDatabase(this);
        preferencesManager = new PreferencesManager(this);
        
        setupToolbar();
        initViews();
        setupListeners();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Send Notification");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initViews() {
        radioGroupRecipients = findViewById(R.id.radioGroupRecipients);
        radioAllUsers = findViewById(R.id.radioAllUsers);
        radioSpecificUsers = findViewById(R.id.radioSpecificUsers);
        radioAllAdmins = findViewById(R.id.radioAllAdmins);
        radioAllOfficers = findViewById(R.id.radioAllOfficers);
        etNotificationTitle = findViewById(R.id.etNotificationTitle);
        etNotificationMessage = findViewById(R.id.etNotificationMessage);
        btnSendNotification = findViewById(R.id.btnSendNotification);
    }
    
    private void setupListeners() {
        btnSendNotification.setOnClickListener(v -> sendNotification());
        
        // Disable "Specific Users" for now (can be implemented later)
        radioSpecificUsers.setEnabled(false);
        radioSpecificUsers.setAlpha(0.5f);
    }
    
    private void sendNotification() {
        String title = etNotificationTitle.getText().toString().trim();
        String message = etNotificationMessage.getText().toString().trim();
        
        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading
        btnSendNotification.setEnabled(false);
        btnSendNotification.setText("Sending...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int selectedId = radioGroupRecipients.getCheckedRadioButtonId();
                List<User> recipients = null;
                
                if (selectedId == R.id.radioAllUsers) {
                    // Send to all users (exclude Admins and Officers)
                    recipients = database.userDao().getUsersByRole("User");
                } else if (selectedId == R.id.radioAllAdmins) {
                    // Send to all admins
                    recipients = database.userDao().getUsersByRole("Admin");
                } else if (selectedId == R.id.radioAllOfficers) {
                    // Send to all officers (get from Officer table)
                    List<Officer> officers = database.officerDao().getAllOfficers();
                    // Convert officers to users by getting their user accounts
                    recipients = new java.util.ArrayList<>();
                    for (Officer officer : officers) {
                        if (officer.getUserId() != null) {
                            User user = database.userDao().getUserById(officer.getUserId());
                            if (user != null) {
                                recipients.add(user);
                            }
                        }
                    }
                }
                
                // Send notifications
                if (recipients != null && !recipients.isEmpty()) {
                    for (User user : recipients) {
                        Notification notification = new Notification(
                            user.getId(),
                            title,
                            message,
                            "ANNOUNCEMENT"
                        );
                        database.notificationDao().insertNotification(notification);
                    }
                    
                    int finalCount = recipients.size();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Notification sent to " + finalCount + " users!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "No recipients found", Toast.LENGTH_SHORT).show();
                        btnSendNotification.setEnabled(true);
                        btnSendNotification.setText("Send Notification");
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSendNotification.setEnabled(true);
                    btnSendNotification.setText("Send Notification");
                });
            }
        });
    }
}
