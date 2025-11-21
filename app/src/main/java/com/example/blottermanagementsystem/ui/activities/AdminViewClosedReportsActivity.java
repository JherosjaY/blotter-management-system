package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminViewClosedReportsActivity extends BaseActivity {
    
    private RecyclerView recyclerReports;
    private ReportAdapter adapter;
    private View emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private Chip chipAll, chipPending, chipAssigned, chipOngoing, chipResolved, chipClosed;
    private TextInputEditText etSearch;
    private TextView tvTotalCount, tvPendingCount, tvOngoingCount, tvResolvedCount, tvClosedCount;
    private ImageButton btnSort;
    private ImageView emptyStateIcon;
    private TextView emptyStateTitle, emptyStateMessage;
    private List<BlotterReport> allReports = new ArrayList<>();
    private List<BlotterReport> filteredReports = new ArrayList<>();
    private PreferencesManager preferencesManager;
    private String searchQuery = "";
    private String currentSort = "Newest First";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            android.util.Log.d("AdminViewClosed", "Starting onCreate...");
            setContentView(R.layout.activity_admin_view_closed_reports);
            
            preferencesManager = new PreferencesManager(this);
            
            initializeViews();
            setupToolbar();
            setupListeners();
            loadReports();
            startPeriodicRefresh();
            
            android.util.Log.d("AdminViewClosed", "onCreate completed successfully");
        } catch (Exception e) {
            android.util.Log.e("AdminViewClosed", "Error in onCreate: " + e.getMessage(), e);
            showErrorState();
        }
    }
    
    private void initializeViews() {
        try {
            recyclerReports = findViewById(R.id.recyclerReports);
            emptyState = findViewById(R.id.emptyState);
            emptyStateCard = findViewById(R.id.emptyStateCard);
            // Try to find statistics views (may not exist in current layout)
            tvTotalCount = findViewById(R.id.tvTotalCount);
            tvPendingCount = findViewById(R.id.tvPendingCount);
            tvOngoingCount = findViewById(R.id.tvOngoingCount);
            tvResolvedCount = findViewById(R.id.tvResolvedCount);
            btnSort = findViewById(R.id.btnSort);
            chipAll = findViewById(R.id.chipAll);
            chipPending = findViewById(R.id.chipPending);
            chipAssigned = findViewById(R.id.chipAssigned);
            chipOngoing = findViewById(R.id.chipOngoing);
            chipResolved = findViewById(R.id.chipResolved);
            chipClosed = findViewById(R.id.chipClosed);
            emptyStateIcon = findViewById(R.id.emptyStateIcon);
            emptyStateTitle = findViewById(R.id.emptyStateTitle);
            emptyStateMessage = findViewById(R.id.emptyStateMessage);
            
            // Setup RecyclerView
            if (recyclerReports != null) {
                adapter = new ReportAdapter(filteredReports, report -> {
                    try {
                        Intent intent = new Intent(this, AdminCaseDetailActivity.class);
                        intent.putExtra("REPORT_ID", report.getId());
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("AdminViewClosed", "Error opening report detail: " + e.getMessage());
                    }
                });
                recyclerReports.setLayoutManager(new LinearLayoutManager(this));
                recyclerReports.setAdapter(adapter);
            }
        } catch (Exception e) {
            android.util.Log.e("AdminViewClosed", "Error initializing views: " + e.getMessage());
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
                getSupportActionBar().setTitle("Closed Reports");
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
            
            // Setup chip listeners with navigation to separate activities
            if (chipAll != null) {
                chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        navigateToScreen(AdminViewAllReportsActivity.class);
                    }
                });
            }
            
            if (chipPending != null) {
                chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        navigateToScreen(AdminViewPendingReportsActivity.class);
                    }
                });
            }
            
            if (chipOngoing != null) {
                chipOngoing.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        navigateToScreen(AdminViewOngoingReportsActivity.class);
                    }
                });
            }
            
            if (chipResolved != null) {
                chipResolved.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        navigateToScreen(AdminViewResolvedReportsActivity.class);
                    }
                });
            }
            
            if (chipAssigned != null) {
                chipAssigned.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        navigateToScreen(AdminViewAssignedReportsActivity.class);
                    }
                });
            }
            
            if (chipClosed != null) {
                chipClosed.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        // Already on Closed screen, just refresh
                        loadReports();
                    }
                });
                chipClosed.setChecked(true); // Set checked AFTER listener
            }
            
            // Setup sort button
            if (btnSort != null) {
                btnSort.setOnClickListener(v -> showSortDialog());
            }
        } catch (Exception e) {
            android.util.Log.e("AdminViewClosed", "Error setting up listeners: " + e.getMessage());
        }
    }
    
    private void navigateToScreen(Class<?> activityClass) {
        try {
            android.util.Log.d("AdminViewClosed", "Navigating to: " + activityClass.getSimpleName());
            Intent intent = new Intent(this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            startActivity(intent);
            // Use fade transition for smooth navigation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } catch (Exception e) {
            android.util.Log.e("AdminViewClosed", "Error navigating to screen: " + e.getMessage(), e);
            android.widget.Toast.makeText(this, "Error navigating: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadReports() {
        NetworkMonitor networkMonitor = new NetworkMonitor(this);
        if (networkMonitor.isNetworkAvailable()) {
            loadReportsFromApi();
        } else {
            loadReportsFromDatabase();
        }
    }
    
    private void loadReportsFromApi() {
        ApiClient.getAllReports(new ApiClient.ApiCallback<List<BlotterReport>>() {
            @Override
            public void onSuccess(List<BlotterReport> apiReports) {
                BlotterDatabase db = BlotterDatabase.getDatabase(AdminViewClosedReportsActivity.this);
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
                            allReports.clear();
                            allReports.addAll(apiReports);
                            updateStatistics();
                            filterReports();
                        });
                    } catch (Exception e) {
                        android.util.Log.e("AdminViewClosed", "Error saving API data: " + e.getMessage());
                        loadReportsFromDatabase();
                    }
                }).start();
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.w("AdminViewClosed", "API error: " + errorMessage);
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
                    allReports.clear();
                    allReports.addAll(reports);
                    updateStatistics();
                    filterReports();
                });
            } catch (Exception e) {
                android.util.Log.e("AdminViewClosed", "Error loading from database: " + e.getMessage());
            }
        }).start();
    }
    
    private void updateStatistics() {
        // For closed reports, we show all statistics but highlight closed count
        new Thread(() -> {
            try {
                BlotterDatabase db = BlotterDatabase.getDatabase(this);
                List<BlotterReport> allSystemReports = db.blotterReportDao().getAllReports();
                
                int total = allSystemReports.size();
                int pending = 0;
                int ongoing = 0;
                int resolved = 0;
                int closed = 0;
                
                for (BlotterReport report : allSystemReports) {
                    String status = report.getStatus();
                    if (status != null) {
                        String statusLower = status.toLowerCase();
                        if (statusLower.equals("pending")) {
                            pending++;
                        } else if (statusLower.equals("ongoing") || statusLower.equals("in progress")) {
                            ongoing++;
                        } else if (statusLower.equals("resolved")) {
                            resolved++;
                        } else if (statusLower.equals("closed")) {
                            closed++;
                        }
                    }
                }
                
                final int finalTotal = total;
                final int finalPending = pending;
                final int finalOngoing = ongoing;
                final int finalResolved = resolved;
                final int finalClosed = closed;
                
                runOnUiThread(() -> {
                    if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(finalTotal));
                    if (tvPendingCount != null) tvPendingCount.setText(String.valueOf(finalPending));
                    if (tvOngoingCount != null) tvOngoingCount.setText(String.valueOf(finalOngoing));
                    if (tvResolvedCount != null) tvResolvedCount.setText(String.valueOf(finalResolved));
                    if (tvClosedCount != null) tvClosedCount.setText(String.valueOf(finalClosed));
                });
            } catch (Exception e) {
                android.util.Log.e("AdminViewClosed", "Error updating statistics: " + e.getMessage());
            }
        }).start();
    }
    
    private void filterReports() {
        filteredReports.clear();
        
        if (searchQuery.isEmpty()) {
            filteredReports.addAll(allReports);
        } else {
            for (BlotterReport report : allReports) {
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
        
        sortReports();
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        updateEmptyState();
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
    
    private void updateEmptyState() {
        if (filteredReports.isEmpty()) {
            // Keep CardView always visible, only show empty state content
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerReports != null) recyclerReports.setVisibility(View.GONE);
            
            // Set Closed reports empty state content with modern icon
            if (emptyStateIcon != null) emptyStateIcon.setImageResource(R.drawable.ic_closed_modern);
            if (emptyStateTitle != null) emptyStateTitle.setText("No Closed Reports");
            if (emptyStateMessage != null) emptyStateMessage.setText("Finalized and archived cases\nwill appear here.");
        } else {
            // Keep CardView visible, only hide empty state content
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerReports != null) recyclerReports.setVisibility(View.VISIBLE);
        }
    }
    
    private void showSortDialog() {
        String[] sortOptions = {"Newest First", "Oldest First"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
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
    
    private void startPeriodicRefresh() {
        // Periodic refresh disabled to prevent flickering
        // Data will be refreshed when user returns to this screen via onResume()
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when user returns to this screen
        loadReports();
    }
    
    private void loadReportsQuietly() {
        // Background refresh without UI disruption
        new Thread(() -> {
            try {
                BlotterDatabase db = BlotterDatabase.getDatabase(this);
                List<BlotterReport> reports = db.blotterReportDao().getAllReports();
                
                List<BlotterReport> closedReports = new ArrayList<>();
                for (BlotterReport report : reports) {
                    String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
                    if ("CLOSED".equals(status)) {
                        closedReports.add(report);
                    }
                }
                
                // Only update if data actually changed
                if (closedReports.size() != allReports.size()) {
                    runOnUiThread(() -> {
                        allReports.clear();
                        allReports.addAll(closedReports);
                        filterReports();
                        updateStatistics();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("AdminViewClosed", "Error in quiet refresh: " + e.getMessage());
            }
        }).start();
    }
}
