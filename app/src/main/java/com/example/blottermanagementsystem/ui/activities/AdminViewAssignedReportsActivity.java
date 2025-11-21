package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class AdminViewAssignedReportsActivity extends BaseActivity {
    
    private RecyclerView recyclerReports;
    private ReportAdapter adapter;
    private View emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private Chip chipAll, chipPending, chipAssigned, chipOngoing, chipResolved, chipClosed;
    private TextInputEditText etSearch;
    private TextView tvTotalCount, tvPendingCount, tvOngoingCount, tvResolvedCount;
    private ImageButton btnSort;
    private ImageView emptyStateIcon;
    private TextView emptyStateTitle, emptyStateMessage;
    private List<BlotterReport> allReports = new ArrayList<>();
    private List<BlotterReport> filteredReports = new ArrayList<>();
    private String searchQuery = "";
    private String currentSort = "Newest First";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_view_assigned_reports);
        
        initializeViews();
        setupToolbar();
        setupListeners();
        loadReports();
        startPeriodicRefresh();
    }
    
    private void initializeViews() {
        recyclerReports = findViewById(R.id.recyclerReports);
        emptyState = findViewById(R.id.emptyState);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        etSearch = findViewById(R.id.etSearch);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvOngoingCount = findViewById(R.id.tvOngoingCount);
        tvResolvedCount = findViewById(R.id.tvResolvedCount);
        btnSort = findViewById(R.id.btnSort);
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipAssigned = findViewById(R.id.chipAssigned);
        chipOngoing = findViewById(R.id.chipOngoing);
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
                    android.util.Log.e("AdminViewAssigned", "Error opening report detail: " + e.getMessage());
                }
            });
            recyclerReports.setLayoutManager(new LinearLayoutManager(this));
            recyclerReports.setAdapter(adapter);
        }
    }
    
    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Assigned Reports");
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
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().toLowerCase();
                    filterReports();
                }
                
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
        
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
        
        if (chipAssigned != null) {
            chipAssigned.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    loadReports();
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
        
        if (chipClosed != null) {
            chipClosed.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    navigateToScreen(AdminViewClosedReportsActivity.class);
                }
            });
        }
        
        if (btnSort != null) {
            btnSort.setOnClickListener(v -> showSortDialog());
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
                BlotterDatabase db = BlotterDatabase.getDatabase(AdminViewAssignedReportsActivity.this);
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
                            filterReports();
                            updateStatistics();
                        });
                    } catch (Exception e) {
                        android.util.Log.e("AdminViewAssigned", "Error saving API data: " + e.getMessage());
                        loadReportsFromDatabase();
                    }
                }).start();
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.w("AdminViewAssigned", "API error: " + errorMessage);
                loadReportsFromDatabase();
            }
        });
    }
    
    private void loadReportsFromDatabase() {
        new Thread(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(this);
                List<BlotterReport> reports = database.blotterReportDao().getAllReports();
                runOnUiThread(() -> {
                    allReports.clear();
                    allReports.addAll(reports);
                    filterReports();
                    updateStatistics();
                });
            } catch (Exception e) {
                android.util.Log.e("AdminViewAssigned", "Error loading from database: " + e.getMessage());
            }
        }).start();
    }
    
    private void filterReports() {
        filteredReports.clear();
        
        if (searchQuery.isEmpty()) {
            for (BlotterReport report : allReports) {
                String status = report.getStatus();
                if (status != null && status.equalsIgnoreCase("Assigned")) {
                    filteredReports.add(report);
                }
            }
        } else {
            for (BlotterReport report : allReports) {
                String status = report.getStatus();
                if (status != null && status.equalsIgnoreCase("Assigned")) {
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
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerReports != null) recyclerReports.setVisibility(View.GONE);
            
            if (emptyStateIcon != null) emptyStateIcon.setImageResource(R.drawable.ic_clipboard);
            if (emptyStateTitle != null) emptyStateTitle.setText("No Assigned Cases");
            if (emptyStateMessage != null) emptyStateMessage.setText("Cases assigned to officers will appear here.");
        } else {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerReports != null) recyclerReports.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateStatistics() {
        int total = 0, pending = 0, ongoing = 0, resolved = 0, closed = 0;
        
        for (BlotterReport report : allReports) {
            total++;
            switch (report.getStatus()) {
                case "Pending":
                    pending++;
                    break;
                case "In Progress":
                case "Ongoing":
                    ongoing++;
                    break;
                case "Resolved":
                    resolved++;
                    break;
                case "Closed":
                    closed++;
                    break;
            }
        }
        
        if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(total));
        if (tvPendingCount != null) tvPendingCount.setText(String.valueOf(pending));
        if (tvOngoingCount != null) tvOngoingCount.setText(String.valueOf(ongoing));
        if (tvResolvedCount != null) tvResolvedCount.setText(String.valueOf(resolved));
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
    
    private void navigateToScreen(Class<?> activityClass) {
        try {
            android.util.Log.d("AdminViewAssigned", "Navigating to: " + activityClass.getSimpleName());
            Intent intent = new Intent(this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            startActivity(intent);
            // Use fade transition for smooth navigation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } catch (Exception e) {
            android.util.Log.e("AdminViewAssigned", "Error navigating to screen: " + e.getMessage(), e);
            android.widget.Toast.makeText(this, "Error navigating: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    private void startPeriodicRefresh() {
        android.os.Handler handler = new android.os.Handler();
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    loadReportsQuietly();
                }
                handler.postDelayed(this, 15000);
            }
        };
        handler.postDelayed(refreshRunnable, 15000);
    }
    
    private void loadReportsQuietly() {
        new Thread(() -> {
            try {
                List<BlotterReport> reports = BlotterDatabase.getDatabase(this).blotterReportDao().getAllReports();
                List<BlotterReport> assignedReports = new ArrayList<>();
                for (BlotterReport report : reports) {
                    String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
                    if ("ASSIGNED".equals(status)) {
                        assignedReports.add(report);
                    }
                }
                boolean dataChanged = hasDataChanged(assignedReports, allReports);
                if (dataChanged) {
                    runOnUiThread(() -> {
                        allReports.clear();
                        allReports.addAll(assignedReports);
                        filterReports();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("AdminViewAssigned", "Error in quiet refresh: " + e.getMessage());
            }
        }).start();
    }
    
    private boolean hasDataChanged(List<BlotterReport> newReports, List<BlotterReport> oldReports) {
        if (newReports.size() != oldReports.size()) {
            return true;
        }
        for (int i = 0; i < newReports.size(); i++) {
            BlotterReport newReport = newReports.get(i);
            BlotterReport oldReport = oldReports.get(i);
            if (newReport.getId() != oldReport.getId() || newReport.getAssignedOfficerId() != oldReport.getAssignedOfficerId()) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }
}
