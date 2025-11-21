package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.ReportAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import android.widget.Toast;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewPendingReportsActivity extends BaseActivity {
    
    private RecyclerView recyclerReports;
    private ReportAdapter adapter;
    private View emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private Chip chipAll, chipPending, chipAssigned, chipOngoing, chipResolved;
    private EditText etSearch;
    private TextView tvTotalCount, tvPendingCount, tvOngoingCount, tvResolvedCount;
    private ImageButton btnSort;
    private ImageView emptyStateIcon;
    private TextView emptyStateTitle, emptyStateMessage;
    private List<BlotterReport> allReports = new ArrayList<>();
    private List<BlotterReport> filteredReports = new ArrayList<>();
    private PreferencesManager preferencesManager;
    private int userId;
    private String searchQuery = "";
    private String currentSort = "Newest First";
    private boolean isOfficerFilter = false;
    private HorizontalScrollView chipScrollView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            android.util.Log.d("ViewPendingReports", "Starting onCreate...");
            setContentView(R.layout.activity_view_pending_reports);
            
            preferencesManager = new PreferencesManager(this);
            userId = preferencesManager.getUserId();
            isOfficerFilter = getIntent().getBooleanExtra("officer_filter", false);
            
            initializeViews();
            setupToolbar();
            setupListeners();
            loadReports();
            startPeriodicRefresh(); // Real-time statistics update every 15 seconds
            
            // Restore scroll position if it was saved
            if (chipScrollView != null) {
                android.content.SharedPreferences prefs = getSharedPreferences("chip_scroll", android.content.Context.MODE_PRIVATE);
                int savedScrollX = prefs.getInt("scroll_position", 0);
                if (savedScrollX > 0) {
                    chipScrollView.post(() -> chipScrollView.scrollTo(savedScrollX, 0));
                    // Clear the saved position after restoring
                    prefs.edit().remove("scroll_position").apply();
                }
            }
            
            android.util.Log.d("ViewPendingReports", "onCreate completed successfully");
        } catch (Exception e) {
            android.util.Log.e("ViewPendingReports", "Error in onCreate: " + e.getMessage(), e);
            showErrorState();
        }
    }
    
    private void initializeViews() {
        try {
            recyclerReports = findViewById(R.id.recyclerReports);
            emptyState = findViewById(R.id.emptyState);
            emptyStateCard = findViewById(R.id.emptyStateCard);
            etSearch = findViewById(R.id.etSearch);
            tvTotalCount = findViewById(R.id.tvTotalCount);
            tvPendingCount = findViewById(R.id.tvPendingCount);
            tvOngoingCount = findViewById(R.id.tvOngoingCount);
            tvResolvedCount = findViewById(R.id.tvResolvedCount);
            btnSort = findViewById(R.id.btnSort);
            chipScrollView = findViewById(R.id.chipScrollView);
            chipAll = findViewById(R.id.chipAll);
            chipPending = findViewById(R.id.chipPending);
            chipAssigned = findViewById(R.id.chipAssigned);
            chipOngoing = findViewById(R.id.chipOngoing);
            chipResolved = findViewById(R.id.chipResolved);
            emptyStateIcon = findViewById(R.id.emptyStateIcon);
            emptyStateTitle = findViewById(R.id.emptyStateTitle);
            emptyStateMessage = findViewById(R.id.emptyStateMessage);
            
            // Setup RecyclerView
            if (recyclerReports != null) {
                adapter = new ReportAdapter(filteredReports, report -> {
                    try {
                        Intent intent = new Intent(this, OfficerCaseDetailActivity.class);
                        intent.putExtra("REPORT_ID", report.getId());
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("ViewPendingReports", "Error opening report detail: " + e.getMessage());
                    }
                });
                recyclerReports.setLayoutManager(new LinearLayoutManager(this));
                recyclerReports.setAdapter(adapter);
            }
        } catch (Exception e) {
            android.util.Log.e("ViewPendingReports", "Error initializing views: " + e.getMessage());
            throw e;
        }
    }
    
    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    private void setupListeners() {
        try {
            // Setup search
            if (etSearch != null) {
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        searchQuery = s.toString().toLowerCase();
                        filterReports();
                    }
                    
                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }
            
            // Setup chip listeners with loading animation
            if (chipAll != null) {
                chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        navigateToScreen(ViewAllReportsActivity.class);
                    }
                });
            }
            
            if (chipPending != null) {
                chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        // Already on Pending screen, just refresh
                        loadReports();
                    }
                });
            }
            
            if (chipAssigned != null) {
                chipAssigned.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        navigateToScreen(ViewAssignedReportsActivity.class);
                    }
                });
            }
            
            if (chipOngoing != null) {
                chipOngoing.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        // Save scroll position and navigate
                        if (chipScrollView != null) {
                            int scrollX = chipOngoing.getLeft() - (chipScrollView.getWidth() / 4);
                            if (scrollX < 0) scrollX = 0;
                            // Save to SharedPreferences
                            android.content.SharedPreferences prefs = getSharedPreferences("chip_scroll", android.content.Context.MODE_PRIVATE);
                            prefs.edit().putInt("scroll_position", scrollX).apply();
                        }
                        navigateToScreen(ViewOngoingReportsActivity.class);
                    }
                });
            }
            
            if (chipResolved != null) {
                chipResolved.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        // Save scroll position and navigate
                        if (chipScrollView != null) {
                            int scrollX = chipResolved.getLeft() - (chipScrollView.getWidth() / 4);
                            if (scrollX < 0) scrollX = 0;
                            // Save to SharedPreferences
                            android.content.SharedPreferences prefs = getSharedPreferences("chip_scroll", android.content.Context.MODE_PRIVATE);
                            prefs.edit().putInt("scroll_position", scrollX).apply();
                        }
                        navigateToScreen(ViewResolvedReportsActivity.class);
                    }
                });
            }
            
            // Setup sort button
            if (btnSort != null) {
                btnSort.setOnClickListener(v -> showSortDialog());
            }
        } catch (Exception e) {
            android.util.Log.e("ViewPendingReports", "Error setting up listeners: " + e.getMessage());
        }
    }
    
    private void scrollToChip(Chip chip) {
        try {
            if (chipScrollView != null && chip != null) {
                // Post delayed to ensure layout is complete
                chipScrollView.postDelayed(() -> {
                    try {
                        // Get the position of the chip relative to the scroll view
                        int chipLeft = chip.getLeft();
                        int scrollViewWidth = chipScrollView.getWidth();
                        
                        // Only scroll if we have valid measurements
                        if (scrollViewWidth > 0 && chipLeft > 0) {
                            // Calculate scroll position to center the chip
                            int scrollX = chipLeft - (scrollViewWidth / 4); // Offset to show chip on right side
                            if (scrollX < 0) scrollX = 0;
                            
                            // Smooth scroll to the chip
                            chipScrollView.smoothScrollTo(scrollX, 0);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ViewPendingReports", "Error scrolling to chip: " + e.getMessage());
                    }
                }, 100); // 100ms delay to ensure layout is ready
            }
        } catch (Exception e) {
            android.util.Log.e("ViewPendingReports", "Error in scrollToChip: " + e.getMessage());
        }
    }
    
    private void navigateToScreen(Class<?> activityClass) {
        try {
            Intent intent = new Intent(this, activityClass);
            intent.putExtra("officer_filter", isOfficerFilter);
            
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } catch (Exception e) {
            android.util.Log.e("ViewPendingReports", "Navigation error: " + e.getMessage(), e);
            android.widget.Toast.makeText(this, "Navigation error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadReports() {
        NetworkMonitor networkMonitor = new NetworkMonitor(this);
        boolean isOnline = networkMonitor.isNetworkAvailable();
        
        if (isOnline) {
            loadReportsFromApi();
        } else {
            loadReportsFromDatabase();
        }
    }
    
    private void loadReportsFromApi() {
        runOnUiThread(() -> {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        });
        
        ApiClient.getAllReports(new ApiClient.ApiCallback<List<BlotterReport>>() {
            @Override
            public void onSuccess(List<BlotterReport> apiReports) {
                BlotterDatabase db = BlotterDatabase.getDatabase(ViewPendingReportsActivity.this);
                new Thread(() -> {
                    try {
                        for (BlotterReport report : apiReports) {
                            BlotterReport existing = db.blotterReportDao().getReportById(report.getId());
                            if (existing == null) {
                                db.blotterReportDao().insertReport(report);
                            } else {
                                db.blotterReportDao().updateReport(report);
                            }
                        }
                        
                        runOnUiThread(() -> {
                            filterReportsByUser(apiReports);
                            updateStatistics();
                            filterReports();
                        });
                    } catch (Exception e) {
                        android.util.Log.e("ViewPendingReports", "Error saving API data: " + e.getMessage());
                        loadReportsFromDatabase();
                    }
                }).start();
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.w("ViewPendingReports", "API error: " + errorMessage);
                loadReportsFromDatabase();
            }
        });
    }
    
    private void loadReportsFromDatabase() {
        new Thread(() -> {
            try {
                BlotterDatabase db = BlotterDatabase.getDatabase(this);
                List<BlotterReport> reports = db.blotterReportDao().getAllReports();
                
                runOnUiThread(() -> {
                    filterReportsByUser(reports);
                    updateStatistics();
                    filterReports();
                });
            } catch (Exception e) {
                android.util.Log.e("ViewPendingReports", "Error loading from database: " + e.getMessage());
            }
        }).start();
    }
    
    private void filterReportsByUser(List<BlotterReport> reports) {
        allReports.clear();
        for (BlotterReport report : reports) {
            if (isOfficerFilter) {
                boolean isAssignedToOfficer = false;
                
                if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId().intValue() == userId) {
                    isAssignedToOfficer = true;
                }
                
                if (!isAssignedToOfficer && report.getAssignedOfficerIds() != null && !report.getAssignedOfficerIds().isEmpty()) {
                    String[] officerIds = report.getAssignedOfficerIds().split(",");
                    for (String id : officerIds) {
                        try {
                            if (Integer.parseInt(id.trim()) == userId) {
                                isAssignedToOfficer = true;
                                break;
                            }
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
                
                if (isAssignedToOfficer) {
                    allReports.add(report);
                }
            } else {
                if (report.getReportedById() == userId) {
                    allReports.add(report);
                }
            }
        }
    }
    
    private void updateStatistics() {
        int total = allReports.size();
        int pending = 0;
        int ongoing = 0;
        int resolved = 0;
        
        for (BlotterReport report : allReports) {
            String status = report.getStatus();
            if (status != null) {
                status = status.toUpperCase();
                if ("PENDING".equals(status)) {
                    pending++;
                } else if ("ASSIGNED".equals(status)) {
                    pending++;
                } else if ("ONGOING".equals(status) || "IN PROGRESS".equals(status)) {
                    ongoing++;
                } else if ("RESOLVED".equals(status)) {
                    resolved++;
                }
            }
        }
        
        if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(total));
        if (tvPendingCount != null) tvPendingCount.setText(String.valueOf(pending));
        if (tvOngoingCount != null) tvOngoingCount.setText(String.valueOf(ongoing));
        if (tvResolvedCount != null) tvResolvedCount.setText(String.valueOf(resolved));
    }
    
    private void filterReports() {
        filteredReports.clear();
        
        // Filter for PENDING reports only (case-insensitive)
        for (BlotterReport report : allReports) {
            String status = report.getStatus() != null ? report.getStatus().toLowerCase() : "";
            if ("pending".equals(status)) {
                if (searchQuery.isEmpty()) {
                    filteredReports.add(report);
                } else {
                    String caseNumber = report.getCaseNumber() != null ? report.getCaseNumber().toLowerCase() : "";
                    String incidentType = report.getIncidentType() != null ? report.getIncidentType().toLowerCase() : "";
                    String complainant = report.getComplainantName() != null ? report.getComplainantName().toLowerCase() : "";
                    
                    if (caseNumber.contains(searchQuery) || 
                        incidentType.contains(searchQuery) || 
                        complainant.contains(searchQuery)) {
                        filteredReports.add(report);
                    }
                }
            }
        }
        
        sortReports();
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (filteredReports.isEmpty()) {
            // Keep CardView always visible, only show empty state content
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerReports != null) recyclerReports.setVisibility(View.GONE);
            
            // Set Pending reports empty state content
            if (emptyStateIcon != null) emptyStateIcon.setImageResource(R.drawable.ic_pending_modern);
            if (emptyStateTitle != null) emptyStateTitle.setText("No Pending Reports");
            if (emptyStateMessage != null) emptyStateMessage.setText("All reports have been\nprocessed or reviewed.");
        } else {
            // Keep CardView visible, only hide empty state content
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerReports != null) recyclerReports.setVisibility(View.VISIBLE);
        }
    }
    
    private void sortReports() {
        switch (currentSort) {
            case "Newest First":
                Collections.sort(filteredReports, (r1, r2) -> 
                    Long.compare(r2.getDateFiled(), r1.getDateFiled()));
                break;
            case "Oldest First":
                Collections.sort(filteredReports, (r1, r2) -> 
                    Long.compare(r1.getDateFiled(), r2.getDateFiled()));
                break;
        }
    }
    
    private void filterReportsByStatus(String status) {
        filteredReports.clear();
        
        for (BlotterReport report : allReports) {
            if (status.equals(report.getStatus())) {
                filteredReports.add(report);
            }
        }
        
        sortReports();
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        updateEmptyState();
    }
    
    private void showSortDialog() {
        String[] sortOptions = {"Newest First", "Oldest First"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Sort Reports")
            .setSingleChoiceItems(sortOptions, currentSort.equals("Newest First") ? 0 : 1, 
                (dialog, which) -> {
                    currentSort = sortOptions[which];
                    filterReports();
                    dialog.dismiss();
                })
            .setNegativeButton("Cancel", null);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
        dialog.show();
    }
    
    private void showErrorState() {
        if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        if (emptyStateTitle != null) emptyStateTitle.setText("Error Loading");
        if (emptyStateMessage != null) emptyStateMessage.setText("Please try again or\ncontact support if issue persists.");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Always refresh when returning to this screen to show latest data
        loadReports();
    }
    
    private void startPeriodicRefresh() {
        android.os.Handler handler = new android.os.Handler();
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Only refresh if activity is visible and not finishing
                if (!isFinishing() && !isDestroyed()) {
                    loadReportsQuietly(); // Use quiet refresh to prevent flicker
                }
                handler.postDelayed(this, 15000);
            }
        };
        handler.postDelayed(refreshRunnable, 15000);
    }
    
    private void loadReportsQuietly() {
        // Background refresh without UI disruption
        new Thread(() -> {
            try {
                BlotterDatabase db = BlotterDatabase.getDatabase(this);
                List<BlotterReport> reports = db.blotterReportDao().getAllReports();
                
                // Filter by user/officer before comparing
                List<BlotterReport> filteredByUser = new ArrayList<>();
                for (BlotterReport report : reports) {
                    if (isOfficerFilter) {
                        if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId() == userId) {
                            filteredByUser.add(report);
                        }
                    } else {
                        if (report.getReportedById() == userId) {
                            filteredByUser.add(report);
                        }
                    }
                }
                
                // Deep comparison: check if data actually changed (not just size)
                boolean dataChanged = hasDataChanged(filteredByUser, allReports);
                
                if (dataChanged) {
                    runOnUiThread(() -> {
                        allReports.clear();
                        allReports.addAll(filteredByUser);
                        filterReports();
                        updateStatistics();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("ViewPendingReports", "Error in quiet refresh: " + e.getMessage());
            }
        }).start();
    }
    
    private boolean hasDataChanged(List<BlotterReport> newReports, List<BlotterReport> oldReports) {
        if (newReports.size() != oldReports.size()) {
            return true;
        }
        
        // Compare each report's key fields
        for (int i = 0; i < newReports.size(); i++) {
            BlotterReport newReport = newReports.get(i);
            BlotterReport oldReport = oldReports.get(i);
            
            String newStatus = newReport.getStatus() != null ? newReport.getStatus().toLowerCase() : "";
            String oldStatus = oldReport.getStatus() != null ? oldReport.getStatus().toLowerCase() : "";
            
            if (newReport.getId() != oldReport.getId() ||
                !newStatus.equals(oldStatus) ||
                newReport.getDateFiled() != oldReport.getDateFiled()) {
                return true;
            }
        }
        
        return false;
    }
}
