package com.example.blottermanagementsystem.ui.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.ImageAdapter;
import com.example.blottermanagementsystem.ui.adapters.VideoAdapter;
import com.example.blottermanagementsystem.utils.MediaManager;
import com.example.blottermanagementsystem.utils.NotificationHelper;
import com.example.blottermanagementsystem.utils.PermissionHelper;
import com.example.blottermanagementsystem.utils.PhoneNumberValidator;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import androidx.cardview.widget.CardView;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EditReportActivity extends BaseActivity {
    
    private BlotterDatabase database;
    private BlotterReport report;
    private int reportId;
    private PreferencesManager preferencesManager;
    private NotificationHelper notificationHelper;
    
    // UI Components
    private Toolbar toolbar;
    private TextInputEditText etIncidentDate, etIncidentTime;
    private TextInputEditText etComplainantName, etComplainantAddress;
    private TextInputEditText etComplainantContact, etNarrative, etIncidentLocation;
    private TextInputEditText etRespondentName, etRespondentAlias, etRespondentAddress, etRespondentContact, etAccusation;
    private AutoCompleteTextView actvIncidentType, actvRelationship;
    private TextView tvCaseNumber;
    private TextView tvImagesLabel, tvVideosLabel;
    private Button btnSave;
    private CardView btnTakePhoto, btnChooseImages, btnChooseVideos;
    private CardView emptyStateImages, emptyStateVideos;
    
    // RecyclerViews
    private RecyclerView recyclerImages, recyclerVideos;
    
    // Adapters
    private ImageAdapter imageAdapter;
    private VideoAdapter videoAdapter;
    
    // Data
    private List<Uri> imageList = new ArrayList<>();
    private List<Uri> videoList = new ArrayList<>();
    
    // Utilities
    private MediaManager mediaManager;
    private Uri currentPhotoUri;
    private boolean isPickingImages = true; // Track if picking images or videos
    
    // Activity Result Launchers
    private final ActivityResultLauncher<Uri> cameraLauncher = 
        registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && currentPhotoUri != null) {
                imageList.add(0, currentPhotoUri);
                imageAdapter.notifyItemInserted(0);
                recyclerImages.scrollToPosition(0);
                updateImageView();
                Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
            }
        });
    
    private final ActivityResultLauncher<Intent> mediaPickerLauncher = 
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                
                if (data.getClipData() != null) {
                    ClipData clipData = data.getClipData();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        Uri uri = clipData.getItemAt(i).getUri();
                        
                        // Grant persistent URI permission for gallery items
                        try {
                            getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (Exception e) {
                            // Ignore - camera photos don't support this
                        }
                        
                        if (isVideoUri(uri)) {
                            videoList.add(0, uri);
                        } else {
                            imageList.add(0, uri);
                        }
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    
                    // Grant persistent URI permission for gallery items
                    try {
                        getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception e) {
                        // Ignore - camera photos don't support this
                    }
                    
                    if (isVideoUri(uri)) {
                        videoList.add(uri);
                    } else {
                        imageList.add(uri);
                    }
                }
                
                imageAdapter.notifyDataSetChanged();
                videoAdapter.notifyDataSetChanged();
                updateImageView();
                updateVideoView();
            }
        });
    
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        });
    
    private final ActivityResultLauncher<String> storagePermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                if (isPickingImages) {
                    openImagePicker();
                } else {
                    openVideoPicker();
                }
            } else {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
            }
        });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report);
        
        database = BlotterDatabase.getDatabase(this);
        preferencesManager = new PreferencesManager(this);
        notificationHelper = new NotificationHelper(this);
        mediaManager = new MediaManager();
        reportId = getIntent().getIntExtra("REPORT_ID", -1);
        
        if (reportId == -1) {
            Toast.makeText(this, "Invalid report", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupMediaListeners();
        loadReportData();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvCaseNumber = findViewById(R.id.tvCaseNumber);
        etIncidentDate = findViewById(R.id.etIncidentDate);
        etIncidentTime = findViewById(R.id.etIncidentTime);
        etComplainantName = findViewById(R.id.etComplainantName);
        etComplainantContact = findViewById(R.id.etContactNumber);
        etComplainantAddress = findViewById(R.id.etAddress);
        actvIncidentType = findViewById(R.id.actvIncidentType);
        etIncidentLocation = findViewById(R.id.etIncidentLocation);
        etNarrative = findViewById(R.id.etDescription);
        etRespondentName = findViewById(R.id.etRespondentName);
        etRespondentAlias = findViewById(R.id.etRespondentAlias);
        etRespondentAddress = findViewById(R.id.etRespondentAddress);
        etRespondentContact = findViewById(R.id.etRespondentContact);
        etAccusation = findViewById(R.id.etAccusation);
        actvRelationship = findViewById(R.id.actvRelationship);
        
        // Evidence
        recyclerImages = findViewById(R.id.recyclerImages);
        recyclerVideos = findViewById(R.id.recyclerVideos);
        tvImagesLabel = findViewById(R.id.tvImagesLabel);
        tvVideosLabel = findViewById(R.id.tvVideosLabel);
        emptyStateImages = findViewById(R.id.emptyStateImages);
        emptyStateVideos = findViewById(R.id.emptyStateVideos);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        btnChooseVideos = findViewById(R.id.btnChooseVideos);
        
        btnSave = findViewById(R.id.btnSubmitBlotterReport);
        btnSave.setText("Update Report");
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Report");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupRecyclerViews() {
        // Images
        imageAdapter = new ImageAdapter(imageList, new ImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(Uri uri) {
                viewImage(uri);
            }
            
            @Override
            public void onImageDelete(int position) {
                imageList.remove(position);
                imageAdapter.notifyItemRemoved(position);
                updateImageView();
            }
        });
        LinearLayoutManager imageLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerImages.setLayoutManager(imageLayoutManager);
        recyclerImages.setAdapter(imageAdapter);
        
        // Videos RecyclerView
        videoAdapter = new VideoAdapter(videoList, new VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(Uri videoUri) {
                playVideo(videoUri);
            }
            
            @Override
            public void onVideoDelete(int position) {
                videoList.remove(position);
                videoAdapter.notifyItemRemoved(position);
                updateVideoView();
            }
        });
        LinearLayoutManager videoLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerVideos.setLayoutManager(videoLayoutManager);
        recyclerVideos.setAdapter(videoAdapter);
    }
    
    private void setupMediaListeners() {
        btnTakePhoto.setOnClickListener(v -> {
            if (PermissionHelper.hasCameraPermission(this)) {
                openCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
        
        btnChooseImages.setOnClickListener(v -> {
            isPickingImages = true;
            if (PermissionHelper.hasStoragePermission(this)) {
                openImagePicker();
            } else {
                storagePermissionLauncher.launch(PermissionHelper.getStoragePermission());
            }
        });
        
        btnChooseVideos.setOnClickListener(v -> {
            isPickingImages = false;
            if (PermissionHelper.hasStoragePermission(this)) {
                openVideoPicker();
            } else {
                storagePermissionLauncher.launch(PermissionHelper.getStoragePermission());
            }
        });
        
        btnSave.setOnClickListener(v -> saveChanges());
    }
    
    private void loadReportData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            report = database.blotterReportDao().getReportById(reportId);
            
            runOnUiThread(() -> {
                if (report != null) {
                    populateFields();
                    loadExistingMedia();
                } else {
                    Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }
    
    private void populateFields() {
        tvCaseNumber.setText(report.getCaseNumber());
        
        // Convert timestamp to date string
        if (report.getIncidentDate() > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateString = dateFormat.format(new Date(report.getIncidentDate()));
            etIncidentDate.setText(dateString);
        }
        if (report.getIncidentTime() != null) {
            etIncidentTime.setText(report.getIncidentTime());
        }
        if (report.getComplainantName() != null) {
            etComplainantName.setText(report.getComplainantName());
        }
        if (report.getComplainantContact() != null) {
            etComplainantContact.setText(report.getComplainantContact());
        }
        if (report.getComplainantAddress() != null) {
            etComplainantAddress.setText(report.getComplainantAddress());
        }
        if (report.getIncidentType() != null) {
            actvIncidentType.setText(report.getIncidentType());
        }
        if (report.getIncidentLocation() != null) {
            etIncidentLocation.setText(report.getIncidentLocation());
        }
        if (report.getNarrative() != null) {
            etNarrative.setText(report.getNarrative());
        }
        if (report.getRespondentName() != null) {
            etRespondentName.setText(report.getRespondentName());
        }
        if (report.getRespondentAlias() != null) {
            etRespondentAlias.setText(report.getRespondentAlias());
        }
        if (report.getRespondentAddress() != null) {
            etRespondentAddress.setText(report.getRespondentAddress());
        }
        if (report.getRespondentContact() != null) {
            etRespondentContact.setText(report.getRespondentContact());
        }
        if (report.getAccusation() != null) {
            etAccusation.setText(report.getAccusation());
        }
        if (report.getRelationshipToComplainant() != null) {
            actvRelationship.setText(report.getRelationshipToComplainant());
        }
    }
    
    private void loadExistingMedia() {
        // Load existing images
        if (report.getImageUris() != null && !report.getImageUris().isEmpty()) {
            String[] uris = report.getImageUris().split(",");
            for (String uriString : uris) {
                Uri uri = Uri.parse(uriString);
                
                // Try to grant persistent permission for existing URIs
                try {
                    getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception e) {
                    // Ignore - may not be supported
                }
                
                imageList.add(uri);
            }
            imageAdapter.notifyDataSetChanged();
            updateImageView();
        }
        
        // Load existing videos
        if (report.getVideoUris() != null && !report.getVideoUris().isEmpty()) {
            String[] uris = report.getVideoUris().split(",");
            for (String uriString : uris) {
                Uri uri = Uri.parse(uriString);
                
                // Try to grant persistent permission for existing URIs
                try {
                    getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception e) {
                    // Ignore - may not be supported
                }
                
                videoList.add(uri);
            }
            videoAdapter.notifyDataSetChanged();
            updateVideoView();
        }
        
    }
    
    private void openCamera() {
        try {
            File photoFile = new File(getExternalFilesDir(null), 
                "photo_" + System.currentTimeMillis() + ".jpg");
            currentPhotoUri = FileProvider.getUriForFile(this, 
                getPackageName() + ".provider", photoFile);
            cameraLauncher.launch(currentPhotoUri);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        mediaPickerLauncher.launch(intent);
    }
    
    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        mediaPickerLauncher.launch(intent);
    }
    
    private boolean isVideoUri(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        return mimeType != null && mimeType.startsWith("video/");
    }
    
    private void viewImage(Uri uri) {
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
            android.util.Log.e("EditReportActivity", "Error showing image: " + e.getMessage());
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
    
    private void updateImageView() {
        if (imageList.isEmpty()) {
            recyclerImages.setVisibility(View.GONE);
            emptyStateImages.setVisibility(View.VISIBLE);
        } else {
            recyclerImages.setVisibility(View.VISIBLE);
            emptyStateImages.setVisibility(View.GONE);
        }
    }
    
    private void updateVideoView() {
        if (videoList.isEmpty()) {
            recyclerVideos.setVisibility(View.GONE);
            emptyStateVideos.setVisibility(View.VISIBLE);
        } else {
            recyclerVideos.setVisibility(View.VISIBLE);
            emptyStateVideos.setVisibility(View.GONE);
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
    
    private void saveChanges() {
        // Validate
        if (etComplainantName.getText().toString().trim().isEmpty()) {
            etComplainantName.setError("Required");
            return;
        }
        
        String complainantContact = etComplainantContact.getText().toString().trim();
        
        // Validate complainant phone number
        if (!complainantContact.isEmpty() && !PhoneNumberValidator.isValidPhilippineNumber(complainantContact)) {
            etComplainantContact.setError(PhoneNumberValidator.getErrorMessage(complainantContact));
            etComplainantContact.requestFocus();
            Toast.makeText(this, "Invalid complainant contact number. " + 
                PhoneNumberValidator.getSupportedNetworks(), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Update report fields
        // Note: Incident date/time are read-only in edit mode (set during creation)
        report.setComplainantName(etComplainantName.getText().toString().trim());
        report.setComplainantContact(complainantContact);
        report.setComplainantAddress(etComplainantAddress.getText().toString().trim());
        report.setIncidentType(actvIncidentType.getText().toString().trim());
        report.setIncidentLocation(etIncidentLocation.getText().toString().trim());
        report.setNarrative(etNarrative.getText().toString().trim());
        
        String respondentName = etRespondentName.getText().toString().trim();
        String respondentAlias = etRespondentAlias.getText().toString().trim();
        String respondentAddress = etRespondentAddress.getText().toString().trim();
        String respondentContact = etRespondentContact.getText().toString().trim();
        String accusation = etAccusation.getText().toString().trim();
        String relationship = actvRelationship.getText().toString().trim();
        
        // Validate respondent phone number if provided
        if (!respondentContact.isEmpty() && !PhoneNumberValidator.isValidPhilippineNumber(respondentContact)) {
            etRespondentContact.setError(PhoneNumberValidator.getErrorMessage(respondentContact));
            etRespondentContact.requestFocus();
            Toast.makeText(this, "Invalid respondent contact number. " + 
                PhoneNumberValidator.getSupportedNetworks(), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Update ALL respondent fields (even if empty - allows clearing values)
        report.setRespondentName(respondentName);
        report.setRespondentAlias(respondentAlias);
        report.setRespondentAddress(respondentAddress);
        report.setRespondentContact(respondentContact);
        report.setAccusation(accusation);
        report.setRelationshipToComplainant(relationship);
        
        // Update media
        if (!imageList.isEmpty()) {
            StringBuilder uris = new StringBuilder();
            for (int i = 0; i < imageList.size(); i++) {
                uris.append(imageList.get(i).toString());
                if (i < imageList.size() - 1) uris.append(",");
            }
            report.setImageUris(uris.toString());
        } else {
            report.setImageUris("");
        }
        
        if (!videoList.isEmpty()) {
            StringBuilder uris = new StringBuilder();
            for (int i = 0; i < videoList.size(); i++) {
                uris.append(videoList.get(i).toString());
                if (i < videoList.size() - 1) uris.append(",");
            }
            report.setVideoUris(uris.toString());
        } else {
            report.setVideoUris("");
        }
        
        // Show loading for report update
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Updating report...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Save to local database first
                database.blotterReportDao().updateReport(report);
                
                // Check if online and sync to API
                NetworkMonitor networkMonitor = new NetworkMonitor(EditReportActivity.this);
                if (networkMonitor.isNetworkAvailable()) {
                    // Sync to API
                    ApiClient.updateReport(reportId, report, new ApiClient.ApiCallback<BlotterReport>() {
                        @Override
                        public void onSuccess(BlotterReport result) {
                            android.util.Log.d("EditReport", "✅ Report synced to API: " + result.getId());
                            // Update local database with API response
                            database.blotterReportDao().updateReport(result);
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            android.util.Log.w("EditReport", "⚠️ API sync failed: " + errorMessage);
                        }
                    });
                } else {
                    android.util.Log.i("EditReport", "Offline mode: Report updated locally, will sync when online");
                }
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error updating report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaManager != null) {
            mediaManager.release();
        }
    }
}
