package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.Officer;
import com.example.blottermanagementsystem.ui.adapters.ImageAdapter;
import com.example.blottermanagementsystem.ui.adapters.SelectableOfficerAdapter;
import com.example.blottermanagementsystem.ui.adapters.VideoAdapter;
import com.example.blottermanagementsystem.utils.GlobalLoadingManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import com.example.blottermanagementsystem.utils.NotificationHelper;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class AdminCaseDetailActivity extends BaseActivity {
    
    // UI Components
    private androidx.appcompat.widget.Toolbar toolbar;
    private TextView tvCaseNumber, tvIncidentType, tvIncidentDate, tvIncidentLocation;
    private TextView tvComplainantName, tvComplainantContact, tvComplainantAddress;
    private TextView tvRespondentName, tvRespondentAlias, tvRespondentAddress, tvAccusation, tvRelationship;
    private TextView tvAssignedOfficers;
    private Chip chipStatus;
    private MaterialButton btnAssignOfficer, btnEdit, btnDelete;
    private TextView tvImagesLabel, tvVideosLabel, tvNarrative;
    private androidx.cardview.widget.CardView cardVideos, cardAssignedOfficers;
    
    // Media Components
    private RecyclerView recyclerImages, recyclerVideos;
    private ImageAdapter imageAdapter;
    private VideoAdapter videoAdapter;
    
    // Data
    private BlotterDatabase database;
    private PreferencesManager preferencesManager;
    private NotificationHelper notificationHelper;
    private BlotterReport currentReport;
    private List<Uri> imageList = new ArrayList<>();
    private List<Uri> videoList = new ArrayList<>();
    private int reportId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_case_detail);
        
        database = BlotterDatabase.getDatabase(this);
        preferencesManager = new PreferencesManager(this);
        notificationHelper = new NotificationHelper(this);
        
        // Get report ID from intent
        reportId = getIntent().getIntExtra("REPORT_ID", -1);
        if (reportId == -1) {
            Toast.makeText(this, "Invalid case ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupToolbar();
        setupListeners();
        setupRecyclerViews();
        loadCaseDetails();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvCaseNumber = findViewById(R.id.tvCaseNumber);
        tvIncidentType = findViewById(R.id.tvIncidentType);
        tvIncidentDate = findViewById(R.id.tvIncidentDate);
        tvIncidentLocation = findViewById(R.id.tvIncidentLocation);
        tvComplainantName = findViewById(R.id.tvComplainantName);
        tvComplainantContact = findViewById(R.id.tvComplainantContact);
        tvComplainantAddress = findViewById(R.id.tvComplainantAddress);
        tvRespondentName = findViewById(R.id.tvRespondentName);
        tvRespondentAlias = findViewById(R.id.tvRespondentAlias);
        tvRespondentAddress = findViewById(R.id.tvRespondentAddress);
        tvAccusation = findViewById(R.id.tvAccusation);
        tvRelationship = findViewById(R.id.tvRelationship);
        tvAssignedOfficers = findViewById(R.id.tvAssignedOfficers);
        cardAssignedOfficers = findViewById(R.id.cardAssignedOfficers);
        chipStatus = findViewById(R.id.chipStatus);
        btnAssignOfficer = findViewById(R.id.btnAssignOfficer);
        recyclerImages = findViewById(R.id.recyclerImages);
        recyclerVideos = findViewById(R.id.recyclerVideos);
        tvImagesLabel = findViewById(R.id.tvImagesLabel);
        tvVideosLabel = findViewById(R.id.tvVideosLabel);
        cardVideos = findViewById(R.id.cardVideos);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Case Details");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupListeners() {
        // Assign Officer button
        btnAssignOfficer.setOnClickListener(v -> showAssignOfficerDialog());
    }
    
    private void setupRecyclerViews() {
        // Images RecyclerView
        imageAdapter = new ImageAdapter(imageList, new ImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(Uri uri) {
                // View image in full screen
                viewImage(uri);
            }
            
            @Override
            public void onImageDelete(int position) {
                // Admin can't delete evidence
            }
        }, false); // Hide delete button for admin
        
        LinearLayoutManager imageLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerImages.setLayoutManager(imageLayoutManager);
        recyclerImages.setAdapter(imageAdapter);
        
        // Videos RecyclerView
        videoAdapter = new VideoAdapter(videoList, new VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(Uri uri) {
                // Play video
                playVideo(uri);
            }
            
            @Override
            public void onVideoDelete(int position) {
                // Admin can't delete evidence
            }
        }, false); // Hide delete button for admin
        
        LinearLayoutManager videoLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerVideos.setLayoutManager(videoLayoutManager);
        recyclerVideos.setAdapter(videoAdapter);
    }
    
    private void loadCaseDetails() {
        GlobalLoadingManager.show(this, "Loading case details...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                currentReport = database.blotterReportDao().getReportById(reportId);
                
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    if (currentReport != null) {
                        populateFields();
                        loadEvidence();
                    } else {
                        Toast.makeText(this, "Case not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error loading case: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
    
    private void populateFields() {
        // Case information
        tvCaseNumber.setText(currentReport.getCaseNumber());
        tvIncidentType.setText(currentReport.getIncidentType());
        tvIncidentDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(currentReport.getIncidentDate())));
        tvIncidentLocation.setText(currentReport.getIncidentLocation());
        
        // Display Assigned Officers
        displayAssignedOfficers();
        
        // Complainant information - Show N/A for empty fields
        tvComplainantName.setText(currentReport.getComplainantName() != null && !currentReport.getComplainantName().isEmpty() ? 
            currentReport.getComplainantName() : "N/A");
        tvComplainantContact.setText(currentReport.getComplainantContact() != null && !currentReport.getComplainantContact().isEmpty() ? 
            currentReport.getComplainantContact() : "N/A");
        tvComplainantAddress.setText(currentReport.getComplainantAddress() != null && !currentReport.getComplainantAddress().isEmpty() ? 
            currentReport.getComplainantAddress() : "N/A");
        
        // Respondent information - Show N/A for empty fields
        tvRespondentName.setText(currentReport.getRespondentName() != null && !currentReport.getRespondentName().isEmpty() ? 
            currentReport.getRespondentName() : "N/A");
        tvRespondentAlias.setText(currentReport.getRespondentAlias() != null && !currentReport.getRespondentAlias().isEmpty() ? 
            currentReport.getRespondentAlias() : "N/A");
        tvRespondentAddress.setText(currentReport.getRespondentAddress() != null && !currentReport.getRespondentAddress().isEmpty() ? 
            currentReport.getRespondentAddress() : "N/A");
        tvAccusation.setText(currentReport.getAccusation() != null && !currentReport.getAccusation().isEmpty() ? 
            currentReport.getAccusation() : "N/A");
        tvRelationship.setText(currentReport.getRelationshipToComplainant() != null && !currentReport.getRelationshipToComplainant().isEmpty() ? 
            currentReport.getRelationshipToComplainant() : "N/A");
        
        // Status chip
        int statusColor = ContextCompat.getColor(this, R.color.text_secondary);
        String status = currentReport.getStatus() != null ? currentReport.getStatus().toLowerCase() : "pending";
        switch (status) {
            case "pending":
                statusColor = ContextCompat.getColor(this, R.color.warning_yellow);
                break;
            case "assigned":
                statusColor = ContextCompat.getColor(this, R.color.electric_blue);
                break;
            case "ongoing":
            case "in-progress":
                statusColor = ContextCompat.getColor(this, R.color.electric_blue);
                break;
            case "resolved":
                statusColor = ContextCompat.getColor(this, R.color.success_green);
                break;
            default:
                statusColor = ContextCompat.getColor(this, R.color.text_secondary);
                break;
        }
        chipStatus.setText(currentReport.getStatus());
        chipStatus.setChipBackgroundColor(ColorStateList.valueOf(statusColor));
        
        // Show assigned officers if any
        if (currentReport.getAssignedOfficerIds() != null && !currentReport.getAssignedOfficerIds().isEmpty()) {
            btnAssignOfficer.setText("Reassign Officers");
        }
    }
    
    private void loadEvidence() {
        // Load images
        imageList.clear();
        boolean hasImages = false;
        if (currentReport.getImageUris() != null && !currentReport.getImageUris().isEmpty()) {
            String[] imageUriStrings = currentReport.getImageUris().split(",");
            for (String uriString : imageUriStrings) {
                String trimmed = uriString.trim();
                if (!trimmed.isEmpty()) {
                    imageList.add(Uri.parse(trimmed));
                    hasImages = true;
                }
            }
        }
        imageAdapter.notifyDataSetChanged();
        
        // Show/hide images section
        if (tvImagesLabel != null) {
            tvImagesLabel.setVisibility(hasImages ? View.VISIBLE : View.GONE);
        }
        if (recyclerImages != null) {
            recyclerImages.setVisibility(hasImages ? View.VISIBLE : View.GONE);
        }
        
        // Load videos
        videoList.clear();
        boolean hasVideos = false;
        if (currentReport.getVideoUris() != null && !currentReport.getVideoUris().isEmpty()) {
            String[] videoUriStrings = currentReport.getVideoUris().split(",");
            for (String uriString : videoUriStrings) {
                String trimmed = uriString.trim();
                if (!trimmed.isEmpty()) {
                    videoList.add(Uri.parse(trimmed));
                    hasVideos = true;
                }
            }
        }
        videoAdapter.notifyDataSetChanged();
        
        // Show/hide videos section
        if (tvVideosLabel != null) {
            tvVideosLabel.setVisibility(hasVideos ? View.VISIBLE : View.GONE);
        }
        if (cardVideos != null) {
            cardVideos.setVisibility(hasVideos ? View.VISIBLE : View.GONE);
        } else if (recyclerVideos != null) {
            recyclerVideos.setVisibility(hasVideos ? View.VISIBLE : View.GONE);
        }
    }
    
    private void showAssignOfficerDialog() {
        GlobalLoadingManager.show(this, "Loading officers...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Officer> officers = database.officerDao().getAllOfficers();
                
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    
                    // Get currently assigned officers
                    Set<Integer> currentlyAssignedIds = new HashSet<>();
                    if (currentReport.getAssignedOfficerIds() != null && !currentReport.getAssignedOfficerIds().isEmpty()) {
                        String[] ids = currentReport.getAssignedOfficerIds().split(",");
                        for (String id : ids) {
                            try {
                                currentlyAssignedIds.add(Integer.parseInt(id.trim()));
                            } catch (NumberFormatException e) {
                                // Ignore invalid IDs
                            }
                        }
                    }
                    
                    // Create modern dialog with RecyclerView
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_assign_officers_modern, null);
                    RecyclerView recyclerOfficers = dialogView.findViewById(R.id.recyclerOfficers);
                    View layoutEmptyState = dialogView.findViewById(R.id.layoutEmptyState);
                    TextView tvSelectedCount = dialogView.findViewById(R.id.tvSelectedCount);
                    MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
                    MaterialButton btnAssign = dialogView.findViewById(R.id.btnAssign);
                    
                    // Setup RecyclerView
                    recyclerOfficers.setLayoutManager(new LinearLayoutManager(this));
                    DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
                    recyclerOfficers.addItemDecoration(divider);
                    
                    // Create adapter
                    SelectableOfficerAdapter adapter = new SelectableOfficerAdapter(
                        officers,
                        currentlyAssignedIds,
                        2, // Max 2 officers
                        selectedCount -> {
                            // Update selected count display
                            if (selectedCount > 0) {
                                tvSelectedCount.setText("(" + selectedCount + "/2 selected)");
                            } else {
                                tvSelectedCount.setText("(Max 2)");
                            }
                        }
                    );
                    recyclerOfficers.setAdapter(adapter);
                    
                    // Show/hide empty state
                    if (officers.isEmpty()) {
                        recyclerOfficers.setVisibility(View.GONE);
                        layoutEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        recyclerOfficers.setVisibility(View.VISIBLE);
                        layoutEmptyState.setVisibility(View.GONE);
                    }
                    
                    // Create dialog
                    androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(true)
                        .create();
                    
                    // Setup button listeners
                    btnCancel.setOnClickListener(v -> dialog.dismiss());
                    btnAssign.setOnClickListener(v -> {
                        List<Officer> selectedOfficers = adapter.getSelectedOfficers();
                        
                        if (selectedOfficers.isEmpty()) {
                            Toast.makeText(this, "Please select at least one officer", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        if (selectedOfficers.size() > 2) {
                            Toast.makeText(this, "Maximum 2 officers can be assigned to a case", Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        dialog.dismiss();
                        assignCaseToOfficers(selectedOfficers);
                    });
                    
                    // Show dialog
                    dialog.show();
                    
                    // Make dialog full width
                    if (dialog.getWindow() != null) {
                        android.view.WindowManager.LayoutParams layoutParams = new android.view.WindowManager.LayoutParams();
                        layoutParams.copyFrom(dialog.getWindow().getAttributes());
                        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
                        layoutParams.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
                        dialog.getWindow().setAttributes(layoutParams);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error loading officers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    android.util.Log.e("AdminCaseDetail", "Error loading officers: " + e.getMessage());
                });
            }
        });
    }
    
    private void assignCaseToOfficers(List<Officer> officers) {
        GlobalLoadingManager.show(this, "Assigning case...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (currentReport != null && !officers.isEmpty()) {
                    // Build assigned officer names and IDs
                    StringBuilder officerNames = new StringBuilder();
                    StringBuilder officerIds = new StringBuilder();
                    
                    android.util.Log.d("AdminAssign", "🔵 Assigning " + officers.size() + " officers to case " + currentReport.getCaseNumber());
                    
                    for (int i = 0; i < officers.size(); i++) {
                        Officer officer = officers.get(i);
                        if (i > 0) {
                            officerNames.append(", ");
                            officerIds.append(",");
                        }
                        officerNames.append(officer.getName());
                        officerIds.append(officer.getId());
                        
                        android.util.Log.d("AdminAssign", "  Officer " + (i+1) + ": " + officer.getName() + " (ID: " + officer.getId() + ")");
                        
                        // Update officer's assigned cases count
                        officer.setAssignedCases(officer.getAssignedCases() + 1);
                        database.officerDao().updateOfficer(officer);
                    }
                    
                    // Update the report with assigned officers
                    currentReport.setAssignedOfficer(officerNames.toString());
                    currentReport.setAssignedOfficerIds(officerIds.toString());
                    if (officers.size() == 1) {
                        currentReport.setAssignedOfficerId(officers.get(0).getId());
                        android.util.Log.d("AdminAssign", "✅ Single officer: assignedOfficerId = " + officers.get(0).getId());
                    } else {
                        currentReport.setAssignedOfficerId(null); // Multiple officers
                        android.util.Log.d("AdminAssign", "✅ Multiple officers: assignedOfficerId = null, assignedOfficerIds = " + officerIds.toString());
                    }
                    
                    // Change status to ASSIGNED when officers are assigned
                    currentReport.setStatus("ASSIGNED");
                    
                    // Update in database
                    database.blotterReportDao().updateReport(currentReport);
                    android.util.Log.d("AdminAssign", "✅ Report updated in database");
                    
                    // Add to sync queue for cloud synchronization
                    try {
                        com.example.blottermanagementsystem.data.entity.SyncQueue syncItem = new com.example.blottermanagementsystem.data.entity.SyncQueue(
                            "BlotterReport",
                            currentReport.getId(),
                            "UPDATE",
                            "Officer assignment: " + officerNames.toString()
                        );
                        database.syncQueueDao().insertSyncItem(syncItem);
                        android.util.Log.d("AdminAssign", "✅ Added to sync queue for cloud sync");
                    } catch (Exception e) {
                        android.util.Log.e("AdminAssign", "⚠️ Failed to add to sync queue: " + e.getMessage());
                    }
                    
                    // Verify the update by reading back from database
                    BlotterReport verifyReport = database.blotterReportDao().getReportById(currentReport.getId());
                    if (verifyReport != null) {
                        android.util.Log.d("AdminAssign", "✅ VERIFICATION - Case: " + verifyReport.getCaseNumber());
                        android.util.Log.d("AdminAssign", "   Status: " + verifyReport.getStatus());
                        android.util.Log.d("AdminAssign", "   assignedOfficerId: " + verifyReport.getAssignedOfficerId());
                        android.util.Log.d("AdminAssign", "   assignedOfficerIds: '" + verifyReport.getAssignedOfficerIds() + "'");
                        android.util.Log.d("AdminAssign", "   assignedOfficer: " + verifyReport.getAssignedOfficer());
                    } else {
                        android.util.Log.e("AdminAssign", "❌ VERIFICATION FAILED - Report not found!");
                    }
                    
                    // Get admin user info for notifications
                    int adminUserId = preferencesManager.getUserId();
                    com.example.blottermanagementsystem.data.entity.User adminUser = database.userDao().getUserById(adminUserId);
                    String adminName = adminUser != null ? adminUser.getFirstName() + " " + adminUser.getLastName() : "Admin";
                    
                    // Send notifications to assigned officers
                    for (Officer officer : officers) {
                        if (officer.getUserId() != null) {
                            notificationHelper.notifyOfficerAssignment(
                                officer.getUserId(),
                                adminUserId,
                                currentReport.getCaseNumber(),
                                officer.getName(),
                                currentReport.getId(),
                                adminName
                            );
                        }
                    }
                    
                    // Notify the user who filed the case about officer assignment
                    if (currentReport.getUserId() > 0) {
                        String officerNamesStr = officerNames.toString();
                        notificationHelper.notifyCaseUpdate(
                            currentReport.getUserId(),
                            adminUserId,
                            currentReport.getCaseNumber(),
                            "Officers assigned: " + officerNamesStr,
                            currentReport.getId(),
                            adminName
                        );
                    }
                    
                    runOnUiThread(() -> {
                        GlobalLoadingManager.hide();
                        // Removed success toast message - no notification shown
                        
                        // Refresh the UI to show updated status
                        populateFields();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error assigning case: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    android.util.Log.e("AdminCaseDetail", "Error assigning case: " + e.getMessage());
                });
            }
        });
    }
    
    private void showUpdateStatusDialog() {
        // TODO: Implement update status dialog
        Toast.makeText(this, "Update Status functionality coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void viewImage(Uri uri) {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_viewer, null);
            ImageView imageView = dialogView.findViewById(R.id.imageView);
            com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);
            
            // Use Glide for better image loading
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
            android.util.Log.e("AdminCaseDetailActivity", "Error showing image: " + e.getMessage());
        }
    }
    
    private void playVideo(Uri uri) {
        // Use the same advanced video player implementation
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
    
    private void displayAssignedOfficers() {
        if (currentReport == null) return;
        
        String assignedOfficer = currentReport.getAssignedOfficer();
        
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
        // Refresh case details when returning to this screen
        if (reportId != -1) {
            loadCaseDetails();
        }
    }
}
