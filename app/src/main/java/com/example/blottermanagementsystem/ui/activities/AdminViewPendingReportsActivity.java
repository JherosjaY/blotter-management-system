package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
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
import androidx.cardview.widget.CardView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminViewPendingReportsActivity extends BaseActivity {
    
    private RecyclerView recyclerReports;
    private ReportAdapter adapter;
    private View emptyState;
    private CardView emptyStateCard;
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
    private BlotterDatabase database;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_admin_view_pending_reports);
            database = BlotterDatabase.getDatabase(this);
            preferencesManager = new PreferencesManager(this);
            
            initializeViews();
            setupToolbar();
            setupListeners();
            loadReports();
            startPeriodicRefresh();
            
            android.util.Log.d("AdminViewPending", "onCreate completed successfully");
        } catch (Exception e) {
            android.util.Log.e("AdminViewPending", "Error in onCreate: " + e.getMessage(), e);
            e.printStackTrace();
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
            
            chipAll = findViewById(R.id.chipAll);
            chipPending = findViewById(R.id.chipPending);
            chipAssigned = findViewById(R.id.chipAssigned);
            chipOngoing = findViewById(R.id.chipOngoing);
            chipResolved = findViewById(R.id.chipResolved);
            chipClosed = findViewById(R.id.chipClosed);
            
            emptyStateIcon = findViewById(R.id.emptyStateIcon);
            emptyStateTitle = findViewById(R.id.emptyStateTitle);
            emptyStateMessage = findViewById(R.id.emptyStateMessage);
            if (emptyStateMessage == null) {
                emptyStateMessage = findViewById(R.id.emptyStateSubtitle);
            }
            
            if (recyclerReports != null) {
                adapter = new ReportAdapter(filteredReports, report -> {
                    try {
                        Intent intent = new Intent(this, AdminCaseDetailActivity.class);
                        intent.putExtra("REPORT_ID", report.getId());
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("AdminViewPending", "Error opening report detail: " + e.getMessage());
                    }
                });
                recyclerReports.setLayoutManager(new LinearLayoutManager(this));
                recyclerReports.setAdapter(adapter);
            }
        } catch (Exception e) {
            android.util.Log.e("AdminViewPending", "Error initializing views: " + e.getMessage());
            throw e;
        }
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Pending Reports");
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
            
            if (chipAll != null) {
                chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        navigateToScreen(AdminViewAllReportsActivity.class);
                    }
                });
            }
            
            if (chipPending != null) {
                chipPending.setChecked(true);
            }
            
            if (chipAssigned != null) {
                chipAssigned.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        navigateToScreen(AdminViewAssignedReportsActivity.class);
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
        } catch (Exception e) {
            android.util.Log.e("AdminViewPending", "Error setting up listeners: " + e.getMessage());
        }
    }
    
    private void navigateToScreen(Class<?> activityClass) {
        try {
            android.util.Log.d("AdminViewPending", "Navigating to: " + activityClass.getSimpleName());
            Intent intent = new Intent(this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } catch (Exception e) {
            android.util.Log.e("AdminViewPending", "Error navigating to screen: " + e.getMessage(), e);
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
                BlotterDatabase db = BlotterDatabase.getDatabase(AdminViewPendingReportsActivity.this);
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
                        android.util.Log.e("AdminViewPendingReports", "Error saving API data: " + e.getMessage());
                        loadReportsFromDatabase();
                    }
                }).start();
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.w("AdminViewPendingReports", "API error: " + errorMessage);
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
                    filterReports();
                    updateStatistics();
                });
            } catch (Exception e) {
                android.util.Log.e("AdminViewPendingReports", "Error loading from database: " + e.getMessage());
            }
        }).start();
    }
    
    private void updateStatistics() {
        try {
            List<BlotterReport> allSystemReports = database.blotterReportDao().getAllReports();
            int total = allSystemReports.size();
            int pending = 0, ongoing = 0, resolved = 0, closed = 0;
            for (BlotterReport report : allSystemReports) {
                String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
                if ("PENDING".equals(status)) {
                    pending++;
                } else if ("ONGOING".equals(status) || "IN PROGRESS".equals(status)) {
                    ongoing++;
                } else if ("RESOLVED".equals(status)) {
                    resolved++;
                } else if ("CLOSED".equals(status)) {
                    closed++;
                for (BlotterReport report : allSystemReports) {
                    String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
                    if ("PENDING".equals(status)) {
                        pending++;
                    } else if ("ONGOING".equals(status) || "IN PROGRESS".equals(status)) {
                        ongoing++;
                    } else if ("RESOLVED".equals(status)) {
                        resolved++;
                    } else if ("CLOSED".equals(status)) {
                        closed++;
                    }
                }
                final int fp = pending, fo = ongoing, fr = resolved, fc = closed, ft = total;
                runOnUiThread(() -> {
                    if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(ft));
                    if (tvPendingCount != null) tvPendingCount.setText(String.valueOf(fp));
                    if (tvOngoingCount != null) tvOngoingCount.setText(String.valueOf(fo));
                    if (tvResolvedCount != null) tvResolvedCount.setText(String.valueOf(fr));
                    if (tvClosedCount != null) tvClosedCount.setText(String.valueOf(fc));
                });
            } catch (Exception e) {
                android.util.Log.e("AdminViewPending", "Error updating statistics: " + e.getMessage());
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
                if (caseNumber.contains(searchQuery) || incidentType.contains(searchQuery) || complainant.contains(searchQuery)) {
                    filteredReports.add(report);
                }
            }
        }
        sortReports();
        if (adapter != null) adapter.notifyDataSetChanged();
        updateEmptyState();
    }
    
    private void sortReports() {
        switch (currentSort) {
            case "Newest First":
                Collections.sort(filteredReports, (r1, r2) -> Long.compare(r2.getDateFiled(), r1.getDateFiled()));
                break;
            case "Oldest First":
                Collections.sort(filteredReports, (r1, r2) -> Long.compare(r1.getDateFiled(), r2.getDateFiled()));
                break;
        }
    }
    
    private void updateEmptyState() {
        if (filteredReports.isEmpty()) {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerReports != null) recyclerReports.setVisibility(View.GONE);
            if (emptyStateIcon != null) emptyStateIcon.setImageResource(R.drawable.ic_pending_modern);
            if (emptyStateTitle != null) emptyStateTitle.setText("No Pending Reports");
            if (emptyStateMessage != null) emptyStateMessage.setText("New reports awaiting review\nwill appear here.");
        } else {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerReports != null) recyclerReports.setVisibility(View.VISIBLE);
        }
    }
    
    private void showSortDialog() {
        String[] sortOptions = {"Newest First", "Oldest First"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sort Reports")
            .setSingleChoiceItems(sortOptions, currentSort.equals("Newest First") ? 0 : 1, (dialog, which) -> {
                currentSort = sortOptions[which];
                filterReports();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
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
                List<BlotterReport> reports = database.blotterReportDao().getAllReports();
                List<BlotterReport> pendingReports = new ArrayList<>();
                for (BlotterReport report : reports) {
                    String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
                    // Only show PENDING status - ASSIGNED cases go to Assigned chip
                    if ("PENDING".equals(status)) {
                        pendingReports.add(report);
                    }
                }
                boolean dataChanged = hasDataChanged(pendingReports, allReports);
                if (dataChanged) {
                    runOnUiThread(() -> {
                        allReports.clear();
                        allReports.addAll(pendingReports);
                        filterReports();
                        updateStatistics();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("AdminViewPending", "Error in quiet refresh: " + e.getMessage());
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
            String newStatus = newReport.getStatus() != null ? newReport.getStatus().toLowerCase() : "";
            String oldStatus = oldReport.getStatus() != null ? oldReport.getStatus().toLowerCase() : "";
            if (newReport.getId() != oldReport.getId() || !newStatus.equals(oldStatus) || newReport.getDateFiled() != oldReport.getDateFiled()) {
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