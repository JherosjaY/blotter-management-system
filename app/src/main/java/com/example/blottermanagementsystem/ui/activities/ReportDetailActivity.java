package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.ui.adapters.ImageAdapter;
import com.example.blottermanagementsystem.ui.adapters.VideoAdapter;
import com.example.blottermanagementsystem.utils.MediaManager;
import com.example.blottermanagementsystem.utils.NotificationHelper;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ReportDetailActivity extends BaseActivity {
    
    private BlotterDatabase database;
    private BlotterReport report;
    private int reportId;
    private PreferencesManager preferencesManager;
    private NotificationHelper notificationHelper;
    
    private Toolbar toolbar;
    private TextView tvReportNumber, tvStatus, tvIncidentType, tvIncidentDate;
    private TextView tvComplainantName, tvComplainantContact, tvComplainantAddress;
    private TextView tvNarrative, tvIncidentLocation;
    private Chip chipStatus;
    private MaterialButton btnEdit, btnDelete;
    private LinearLayout layoutAdminActions, layoutKPForms;
    private MaterialButton btnAssignOfficer, btnUpdateStatus, btnStartInvestigation;
    private MaterialButton btnKPForm1, btnKPForm7, btnKPForm16, btnCertification;
    
    private TextView tvRespondentTitle;
    private androidx.cardview.widget.CardView cardRespondent;
    private LinearLayout layoutRespondentName, layoutRespondentAlias, layoutRespondentAddress;
    private LinearLayout layoutRespondentContact, layoutAccusation, layoutRelationship;
    private TextView tvRespondentName, tvRespondentAlias, tvRespondentAddress;
    private TextView tvRespondentContact, tvAccusation, tvRelationship;
    
    private TextView tvEvidenceTitle, tvImagesLabel, tvVideosLabel;
    private RecyclerView recyclerImages, recyclerVideos;
    private CardView cardImages, cardVideos;
    private ImageAdapter imageAdapter;
    private VideoAdapter videoAdapter;
    private MediaManager mediaManager;
    
    private List<Uri> imageList = new ArrayList<>();
    private List<Uri> videoList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_report_detail);
        
        try {
            database = BlotterDatabase.getDatabase(this);
            preferencesManager = new PreferencesManager(this);
            notificationHelper = new NotificationHelper(this);
            reportId = getIntent().getIntExtra("REPORT_ID", -1);
            
            if (reportId == -1) {
                Toast.makeText(this, "Invalid report", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            initViews();
            setupToolbar();
            setupListeners();
            loadReportDetails();
        } catch (Exception e) {
            Toast.makeText(this, "Error loading report: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private TextView tvAssignedOfficers;
    private androidx.cardview.widget.CardView cardAssignedOfficers;
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvReportNumber = findViewById(R.id.tvCaseNumber);
        chipStatus = findViewById(R.id.chipStatus);
        tvAssignedOfficers = findViewById(R.id.tvAssignedOfficers);
        cardAssignedOfficers = findViewById(R.id.cardAssignedOfficers);
        tvIncidentType = findViewById(R.id.tvIncidentType);
        tvIncidentDate = findViewById(R.id.tvIncidentDate);
        tvComplainantName = findViewById(R.id.tvComplainantName);
        tvComplainantContact = findViewById(R.id.tvContactNumber);
        tvComplainantAddress = findViewById(R.id.tvAddress);
        tvNarrative = findViewById(R.id.tvDescription);
        tvIncidentLocation = findViewById(R.id.tvIncidentLocation);
        
        layoutAdminActions = findViewById(R.id.layoutAdminActions);
        btnAssignOfficer = findViewById(R.id.btnAssignOfficer);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        
        layoutKPForms = findViewById(R.id.layoutKPForms);
        btnKPForm1 = findViewById(R.id.btnKPForm1);
        btnKPForm7 = findViewById(R.id.btnKPForm7);
        btnKPForm16 = findViewById(R.id.btnKPForm16);
        btnCertification = findViewById(R.id.btnCertification);
        
        btnStartInvestigation = findViewById(R.id.btnStartInvestigation);
        
        tvRespondentTitle = findViewById(R.id.tvRespondentTitle);
        cardRespondent = findViewById(R.id.cardRespondent);
        layoutRespondentName = findViewById(R.id.layoutRespondentName);
        layoutRespondentAlias = findViewById(R.id.layoutRespondentAlias);
        layoutRespondentAddress = findViewById(R.id.layoutRespondentAddress);
        layoutRespondentContact = findViewById(R.id.layoutRespondentContact);
        layoutAccusation = findViewById(R.id.layoutAccusation);
        layoutRelationship = findViewById(R.id.layoutRelationship);
        tvRespondentName = findViewById(R.id.tvRespondentName);
        tvRespondentAlias = findViewById(R.id.tvRespondentAlias);
        tvRespondentAddress = findViewById(R.id.tvRespondentAddress);
        tvRespondentContact = findViewById(R.id.tvRespondentContact);
        tvAccusation = findViewById(R.id.tvAccusation);
        tvRelationship = findViewById(R.id.tvRelationship);
        
        tvEvidenceTitle = findViewById(R.id.tvEvidenceTitle);
        tvImagesLabel = findViewById(R.id.tvImagesLabel);
        tvVideosLabel = findViewById(R.id.tvVideosLabel);
        recyclerImages = findViewById(R.id.recyclerImages);
        recyclerVideos = findViewById(R.id.recyclerVideos);
        cardImages = findViewById(R.id.cardImages);
        cardVideos = findViewById(R.id.cardVideos);
        
        mediaManager = new MediaManager();
        
        if (recyclerImages != null && recyclerVideos != null) {
            setupEvidenceRecyclerViews();
        }
    }
    
    private void setupEvidenceRecyclerViews() {
        imageAdapter = new ImageAdapter(imageList, new ImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(Uri uri) {
                // Preview image using built-in viewer
                previewImage(uri);
            }
            
            @Override
            public void onImageDelete(int position) {
                // Not allowed in view mode
            }
        }, false);
        LinearLayoutManager imageLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerImages.setLayoutManager(imageLayoutManager);
        recyclerImages.setAdapter(imageAdapter);
        
        videoAdapter = new VideoAdapter(videoList, new VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(Uri uri) {
                // Play video using built-in player
                playVideo(uri);
            }
            
            @Override
            public void onVideoDelete(int position) {
                // Not allowed in view mode
            }
        }, false);
        LinearLayoutManager videoLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerVideos.setLayoutManager(videoLayoutManager);
        recyclerVideos.setAdapter(videoAdapter);
    }
    
    private void previewImage(Uri uri) {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_viewer, null);
            android.widget.ImageView imageView = dialogView.findViewById(R.id.imageView);
            com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);
            
            com.bumptech.glide.Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(imageView);
            
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
            
            btnClose.setOnClickListener(v -> dialog.dismiss());
            
            // Make dialog full screen
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
            
            // Set dialog to full screen after showing
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                               android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            android.util.Log.e("ReportDetailActivity", "Error showing image: " + e.getMessage());
        }
    }
    
    private void playVideo(Uri uri) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_video_player, null);
        android.widget.VideoView videoView = dialogView.findViewById(R.id.videoView);
        android.widget.ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        android.widget.ImageButton btnPlayPause = dialogView.findViewById(R.id.btnPlayPause);
        android.widget.ImageButton btnRewind = dialogView.findViewById(R.id.btnRewind);
        android.widget.ImageButton btnForward = dialogView.findViewById(R.id.btnForward);
        android.view.View videoControlsOverlay = dialogView.findViewById(R.id.videoControlsOverlay);
        android.view.View centerControls = dialogView.findViewById(R.id.centerControls);
        android.view.View bottomControls = dialogView.findViewById(R.id.bottomControls);
        android.widget.SeekBar seekBar = dialogView.findViewById(R.id.seekBar);
        android.widget.TextView tvCurrentTime = dialogView.findViewById(R.id.tvCurrentTime);
        android.widget.TextView tvDuration = dialogView.findViewById(R.id.tvDuration);
        android.widget.ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        
        // Fade Animation Handler - declare early
        android.os.Handler controlsHandler = new android.os.Handler();
        
        // Set video URI and start
        videoView.setVideoURI(uri);
        
        // Show progress while loading
        progressBar.setVisibility(View.VISIBLE);
        
        // Hide progress when ready
        videoView.setOnPreparedListener(mp -> {
            progressBar.setVisibility(View.GONE);
            
            // Set up SeekBar
            int duration = videoView.getDuration();
            seekBar.setMax(duration);
            tvDuration.setText("-" + formatTime(duration));
            
            videoView.start();
            
            // Start updating progress
            updateVideoProgress(videoView, seekBar, tvCurrentTime, tvDuration, controlsHandler);
        });
        
        // Handle errors
        videoView.setOnErrorListener((mp, what, extra) -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        // Fade Out Animation
        Runnable fadeOutControls = () -> {
            centerControls.animate()
                .alpha(0.0f)
                .setDuration(300)
                .start();
            bottomControls.animate()
                .alpha(0.0f)
                .setDuration(300)
                .start();
        };
        
        // Fade In Animation
        Runnable fadeInControls = () -> {
            centerControls.animate()
                .alpha(1.0f)
                .setDuration(300)
                .start();
            bottomControls.animate()
                .alpha(1.0f)
                .setDuration(300)
                .start();
            
            // Auto-hide after 3 seconds
            controlsHandler.removeCallbacks(fadeOutControls);
            controlsHandler.postDelayed(fadeOutControls, 3000);
        };
        
        // Show/hide controls on video tap with fade animation
        videoControlsOverlay.setOnClickListener(v -> {
            if (centerControls.getAlpha() > 0.5f) {
                // Currently visible, fade out
                controlsHandler.removeCallbacks(fadeOutControls);
                fadeOutControls.run();
            } else {
                // Currently hidden, fade in
                fadeInControls.run();
            }
        });
        
        // Play/Pause button with new icons
        btnPlayPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                btnPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                videoView.start();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
            }
            fadeInControls.run(); // Reset fade timer
        });
        
        // Rewind 5 seconds
        btnRewind.setOnClickListener(v -> {
            int currentPosition = videoView.getCurrentPosition();
            int newPosition = Math.max(0, currentPosition - 5000);
            videoView.seekTo(newPosition);
            
            // UPDATE SEEKBAR AND TIMESTAMPS IMMEDIATELY
            int duration = videoView.getDuration();
            int remaining = duration - newPosition;
            seekBar.setProgress(newPosition);
            tvCurrentTime.setText(formatTime(newPosition));
            tvDuration.setText("-" + formatTime(remaining));
            
            fadeInControls.run(); // Reset fade timer
        });
        
        // Forward 5 seconds
        btnForward.setOnClickListener(v -> {
            int currentPosition = videoView.getCurrentPosition();
            int duration = videoView.getDuration();
            int newPosition = Math.min(duration, currentPosition + 5000);
            videoView.seekTo(newPosition);
            
            // UPDATE SEEKBAR AND TIMESTAMPS IMMEDIATELY
            int remaining = duration - newPosition;
            seekBar.setProgress(newPosition);
            tvCurrentTime.setText(formatTime(newPosition));
            tvDuration.setText("-" + formatTime(remaining));
            
            fadeInControls.run(); // Reset fade timer
        });
        
        // SeekBar interaction
        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                    int duration = videoView.getDuration();
                    int remaining = duration - progress;
                    tvCurrentTime.setText(formatTime(progress));
                    tvDuration.setText("-" + formatTime(remaining));
                }
            }
            
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                // PAUSE auto-hide while dragging
                controlsHandler.removeCallbacks(fadeOutControls);
                // Show controls and keep them visible
                centerControls.animate().alpha(1.0f).setDuration(300).start();
                bottomControls.animate().alpha(1.0f).setDuration(300).start();
            }
            
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                // RESUME auto-hide after dragging stops
                fadeInControls.run();
            }
        });
        
        // Close button
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        // Start with controls visible, then auto-hide
        fadeInControls.run();
        
        // Show dialog
        dialog.show();
    }
    
    private void updateVideoProgress(android.widget.VideoView videoView, android.widget.SeekBar seekBar, 
                                   android.widget.TextView tvCurrentTime, android.widget.TextView tvDuration, 
                                   android.os.Handler handler) {
        if (videoView.isPlaying()) {
            int currentPosition = videoView.getCurrentPosition();
            int duration = videoView.getDuration();
            int remaining = duration - currentPosition;
            
            // Update SeekBar progress
            seekBar.setProgress(currentPosition);
            
            // Update timestamps
            tvCurrentTime.setText(formatTime(currentPosition));
            tvDuration.setText("-" + formatTime(remaining));
            
            // Schedule next update
            handler.postDelayed(() -> updateVideoProgress(videoView, seekBar, tvCurrentTime, tvDuration, handler), 1000);
        }
    }
    
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        int hours = (milliseconds / (1000 * 60 * 60));
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report Details");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupListeners() {
        // Edit button listener
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> editReport());
        }
        
        // Delete button listener
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        }
    }
    
    private void editReport() {
        if (report == null) {
            Toast.makeText(this, "Report data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, EditReportActivity.class);
        intent.putExtra("REPORT_ID", reportId);
        startActivity(intent);
    }
    
    private void showDeleteConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to delete this report? This action cannot be undone.")
            .setPositiveButton("DELETE", (dialog, which) -> deleteReport())
            .setNegativeButton("CANCEL", null)
            .show();
    }
    
    private void deleteReport() {
        if (report == null) return;
        
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Deleting report...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                database.blotterReportDao().deleteReport(report);
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Report deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error deleting report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void loadReportDetails() {
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Loading report...");

        // Check if online
        NetworkMonitor networkMonitor = new NetworkMonitor(this);
        if (networkMonitor.isNetworkAvailable()) {
            // Load from API
            ApiClient.getReportById(reportId, new ApiClient.ApiCallback<BlotterReport>() {
                @Override
                public void onSuccess(BlotterReport apiReport) {
                    // Save to local database
                    database.blotterReportDao().updateReport(apiReport);
                    // Display report
                    runOnUiThread(() -> {
                        com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                        report = apiReport;
                        displayReportDetails();
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    // Fallback to local database
                    android.util.Log.w("ReportDetail", "API error: " + errorMessage);
                    loadFromDatabase();
                }
            });
        } else {
            // Load from local database (offline)
            loadFromDatabase();
        }
    }
    
    private void loadFromDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                report = database.blotterReportDao().getReportById(reportId);
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    if (report != null) {
                        displayReportDetails();
                    } else {
                        Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error loading report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void displayReportDetails() {
        if (report == null) return;
        
        tvReportNumber.setText(report.getCaseNumber());
        chipStatus.setText(report.getStatus());
        
        // Display Assigned Officers
        displayAssignedOfficers();
        
        tvIncidentType.setText(report.getIncidentType() != null ? report.getIncidentType() : "N/A");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        tvIncidentDate.setText(report.getIncidentDate() > 0 ? 
            dateFormat.format(new java.util.Date(report.getIncidentDate())) : "N/A");
        
        tvIncidentLocation.setText(report.getIncidentLocation() != null ? 
            report.getIncidentLocation() : "N/A");
        
        tvComplainantName.setText(report.getComplainantName() != null ? 
            report.getComplainantName() : "N/A");
        tvComplainantContact.setText(report.getComplainantContact() != null ? 
            report.getComplainantContact() : "N/A");
        tvComplainantAddress.setText(report.getComplainantAddress() != null ? 
            report.getComplainantAddress() : "N/A");
        
        tvNarrative.setText(report.getNarrative() != null ? 
            report.getNarrative() : "No narrative provided");
        
        // Display Respondent Information if available
        displayRespondentInformation();
        
        // Display Evidence (Images and Videos)
        displayEvidence();
        
        updateButtonVisibility();
    }
    
    private void displayRespondentInformation() {
        if (report == null) return;
        
        // Always show respondent section
        if (tvRespondentTitle != null) tvRespondentTitle.setVisibility(View.VISIBLE);
        if (cardRespondent != null) cardRespondent.setVisibility(View.VISIBLE);
        
        // Always show all respondent fields with N/A if empty
        if (layoutRespondentName != null) {
            layoutRespondentName.setVisibility(View.VISIBLE);
            if (tvRespondentName != null) {
                String name = report.getRespondentName();
                tvRespondentName.setText((name != null && !name.isEmpty()) ? name : "N/A");
            }
        }
        
        if (layoutRespondentAlias != null) {
            layoutRespondentAlias.setVisibility(View.VISIBLE);
            if (tvRespondentAlias != null) {
                String alias = report.getRespondentAlias();
                tvRespondentAlias.setText((alias != null && !alias.isEmpty()) ? alias : "N/A");
            }
        }
        
        if (layoutRespondentAddress != null) {
            layoutRespondentAddress.setVisibility(View.VISIBLE);
            if (tvRespondentAddress != null) {
                String address = report.getRespondentAddress();
                tvRespondentAddress.setText((address != null && !address.isEmpty()) ? address : "N/A");
            }
        }
        
        if (layoutRespondentContact != null) {
            layoutRespondentContact.setVisibility(View.VISIBLE);
            if (tvRespondentContact != null) {
                String contact = report.getRespondentContact();
                tvRespondentContact.setText((contact != null && !contact.isEmpty()) ? contact : "N/A");
            }
        }
        
        if (layoutAccusation != null) {
            layoutAccusation.setVisibility(View.VISIBLE);
            if (tvAccusation != null) {
                String accusation = report.getAccusation();
                tvAccusation.setText((accusation != null && !accusation.isEmpty()) ? accusation : "N/A");
            }
        }
        
        if (layoutRelationship != null) {
            layoutRelationship.setVisibility(View.VISIBLE);
            if (tvRelationship != null) {
                String relationship = report.getRelationshipToComplainant();
                tvRelationship.setText((relationship != null && !relationship.isEmpty()) ? relationship : "N/A");
            }
        }
    }
    
    private void displayEvidence() {
        if (report == null) return;
        
        // Clear lists to prevent duplicates
        imageList.clear();
        videoList.clear();
        
        // Display Images
        if (report.getImageUris() != null && !report.getImageUris().isEmpty()) {
            String[] imageUris = report.getImageUris().split(",");
            for (String uriString : imageUris) {
                if (!uriString.trim().isEmpty()) {
                    imageList.add(android.net.Uri.parse(uriString.trim()));
                }
            }
            
            if (!imageList.isEmpty()) {
                if (tvEvidenceTitle != null) tvEvidenceTitle.setVisibility(View.VISIBLE);
                if (tvImagesLabel != null) tvImagesLabel.setVisibility(View.VISIBLE);
                if (cardImages != null) cardImages.setVisibility(View.VISIBLE);
                // Notify adapter of data changes
                if (imageAdapter != null) {
                    imageAdapter.notifyDataSetChanged();
                }
            }
        }
        
        // Display Videos
        if (report.getVideoUris() != null && !report.getVideoUris().isEmpty()) {
            String[] videoUris = report.getVideoUris().split(",");
            for (String uriString : videoUris) {
                if (!uriString.trim().isEmpty()) {
                    videoList.add(android.net.Uri.parse(uriString.trim()));
                }
            }
            
            if (!videoList.isEmpty()) {
                if (tvEvidenceTitle != null) tvEvidenceTitle.setVisibility(View.VISIBLE);
                if (tvVideosLabel != null) tvVideosLabel.setVisibility(View.VISIBLE);
                if (cardVideos != null) cardVideos.setVisibility(View.VISIBLE);
                // Notify adapter of data changes
                if (videoAdapter != null) {
                    videoAdapter.notifyDataSetChanged();
                }
            }
        }
    }
    
    private void updateButtonVisibility() {
        String userRole = preferencesManager.getUserRole();
        boolean isOfficer = "Officer".equalsIgnoreCase(userRole);
        boolean isAdmin = "Admin".equalsIgnoreCase(userRole);
        
        if (isOfficer) {
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        } else if (isAdmin) {
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        } else {
            String status = report != null && report.getStatus() != null 
                ? report.getStatus().toLowerCase() 
                : "";
            boolean isPending = "pending".equals(status);
            
            if (btnEdit != null) {
                btnEdit.setVisibility(isPending ? View.VISIBLE : View.GONE);
            }
            
            if (btnDelete != null) {
                btnDelete.setVisibility(isPending ? View.VISIBLE : View.GONE);
            }
        }
    }
    
    private void displayAssignedOfficers() {
        if (report == null) return;
        
        String assignedOfficer = report.getAssignedOfficer();
        
        // Show card only if officers are assigned
        if (assignedOfficer != null && !assignedOfficer.isEmpty()) {
            cardAssignedOfficers.setVisibility(View.VISIBLE);
            tvAssignedOfficers.setText(assignedOfficer);
        } else {
            cardAssignedOfficers.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (reportId != -1) {
            loadReportDetails();
        }
    }
}
