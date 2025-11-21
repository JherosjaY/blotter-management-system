package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.Officer;
import com.example.blottermanagementsystem.ui.adapters.ReportAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class OfficerViewAllReportsActivity_New extends BaseActivity {
    
    private RecyclerView recyclerReports;
    private ReportAdapter adapter;
    private View emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private Chip chipAll, chipPending, chipOngoing, chipResolved;
    private EditText etSearch;
    private TextView tvTotalCount, tvPendingCount, tvOngoingCount, tvResolvedCount;
    private ImageButton btnSort;
    private ImageView emptyStateIcon;
    private TextView emptyStateTitle, emptyStateMessage;
    private List<BlotterReport> allReports = new ArrayList<>();
    private List<BlotterReport> filteredReports = new ArrayList<>();
    private PreferencesManager preferencesManager;
    private int officerId = -1;
    private String searchQuery = "";
    private String currentSort = "Newest First";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_view_all_reports_new);
        
        preferencesManager = new PreferencesManager(this);
        BlotterDatabase database = BlotterDatabase.getDatabase(this);
        
        initViews();
        setupToolbar();
        setEmptyStateIcon();
        setupListeners();
        setupRecyclerView();
        
        // Get officer ID on background thread
        int userId = preferencesManager.getUserId();
        Executors.newSingleThreadExecutor().execute(() -> {
            Officer officer = database.officerDao().getOfficerByUserId(userId);
            if (officer != null) {
                officerId = officer.getId();
            }
            runOnUiThread(this::loadReports);
        });
    }
    
    private void setEmptyStateIcon() {
        // Set icon for "All" screen (default)
        updateEmptyStateIcon("ALL");
    }
    
    private void updateEmptyStateIcon(String chipType) {
        if (emptyStateIcon == null) return;
        
        switch (chipType) {
            case "ALL":
                emptyStateIcon.setImageResource(R.drawable.ic_clipboard);
                break;
            case "ASSIGNED":
                emptyStateIcon.setImageResource(R.drawable.ic_folder);
                break;
            case "ONGOING":
                emptyStateIcon.setImageResource(R.drawable.ic_cases);
                break;
            case "RESOLVED":
                emptyStateIcon.setImageResource(R.drawable.ic_check_circle);
                break;
            default:
                emptyStateIcon.setImageResource(R.drawable.ic_clipboard);
        }
    }
    
    private void initViews() {
        recyclerReports = findViewById(R.id.recyclerReports);
        emptyState = findViewById(R.id.emptyState);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        etSearch = findViewById(R.id.etSearch);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvOngoingCount = findViewById(R.id.tvOngoingCount);
        tvResolvedCount = findViewById(R.id.tvResolvedCount);
        btnSort = findViewById(R.id.btnSort);
        emptyStateIcon = findViewById(R.id.emptyStateIcon);
        emptyStateTitle = findViewById(R.id.emptyStateTitle);
        emptyStateMessage = findViewById(R.id.emptyStateMessage);
        
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipOngoing = findViewById(R.id.chipOngoing);
        chipResolved = findViewById(R.id.chipResolved);
    }
    
    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }
    
    private void setupListeners() {
        // Search
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
        
        // Sort button
        if (btnSort != null) {
            btnSort.setOnClickListener(v -> showSortDialog());
        }
        
        // Chip listeners
        if (chipAll != null) {
            chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    updateEmptyStateIcon("ALL");
                    filterReports();
                }
            });
        }
        
        if (chipPending != null) {
            chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    updateEmptyStateIcon("ASSIGNED");
                    filterReports();
                }
            });
        }
        
        if (chipOngoing != null) {
            chipOngoing.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    updateEmptyStateIcon("ONGOING");
                    filterReports();
                }
            });
        }
        
        if (chipResolved != null) {
            chipResolved.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    updateEmptyStateIcon("RESOLVED");
                    filterReports();
                }
            });
        }
    }
    
    private void setupRecyclerView() {
        recyclerReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(filteredReports, report -> {
            // Open case detail
            Intent intent = new Intent(this, OfficerCaseDetailActivity.class);
            intent.putExtra("reportId", report.getId());
            startActivity(intent);
        });
        recyclerReports.setAdapter(adapter);
    }
    
    private void loadReports() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(this);
                List<BlotterReport> reports = database.blotterReportDao().getAllReports();
                
                allReports.clear();
                // Filter only cases assigned to this officer
                for (BlotterReport report : reports) {
                    // Check if officer is assigned (either single or multiple officers)
                    boolean isAssignedToOfficer = false;
                    
                    // Check single officer assignment
                    if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId().intValue() == officerId) {
                        isAssignedToOfficer = true;
                    }
                    
                    // Check multiple officers assignment
                    if (!isAssignedToOfficer && report.getAssignedOfficerIds() != null && !report.getAssignedOfficerIds().isEmpty()) {
                        String[] officerIds = report.getAssignedOfficerIds().split(",");
                        for (String id : officerIds) {
                            try {
                                if (Integer.parseInt(id.trim()) == officerId) {
                                    isAssignedToOfficer = true;
                                    break;
                                }
                            } catch (NumberFormatException e) {
                                // Ignore invalid IDs
                            }
                        }
                    }
                    
                    if (isAssignedToOfficer) {
                        allReports.add(report);
                    }
                }
                
                runOnUiThread(() -> {
                    updateStatistics();
                    filterReports();
                });
            } catch (Exception e) {
                android.util.Log.e("OfficerViewAll", "Error loading reports: " + e.getMessage());
            }
        });
    }
    
    private void updateStatistics() {
        int total = allReports.size();
        int pending = 0, ongoing = 0, resolved = 0;
        
        for (BlotterReport report : allReports) {
            String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
            if ("PENDING".equals(status)) {
                pending++;
            } else if ("ASSIGNED".equals(status) || "ONGOING".equals(status) || "IN PROGRESS".equals(status)) {
                ongoing++;
            } else if ("RESOLVED".equals(status)) {
                resolved++;
            }
        }
        
        if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(total));
        if (tvPendingCount != null) tvPendingCount.setText(String.valueOf(pending));
        if (tvOngoingCount != null) tvOngoingCount.setText(String.valueOf(ongoing));
        if (tvResolvedCount != null) tvResolvedCount.setText(String.valueOf(resolved));
    }
    
    private void filterReports() {
        filteredReports.clear();
        
        String selectedFilter = "All";
        if (chipPending != null && chipPending.isChecked()) selectedFilter = "Pending";
        else if (chipOngoing != null && chipOngoing.isChecked()) selectedFilter = "Ongoing";
        else if (chipResolved != null && chipResolved.isChecked()) selectedFilter = "Resolved";
        
        for (BlotterReport report : allReports) {
            String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
            boolean matchesFilter = false;
            
            if ("All".equals(selectedFilter)) {
                matchesFilter = true;
            } else if ("Pending".equals(selectedFilter) && "PENDING".equals(status)) {
                matchesFilter = true;
            } else if ("Ongoing".equals(selectedFilter) && 
                       ("ASSIGNED".equals(status) || "ONGOING".equals(status) || "IN PROGRESS".equals(status))) {
                matchesFilter = true;
            } else if ("Resolved".equals(selectedFilter) && "RESOLVED".equals(status)) {
                matchesFilter = true;
            }
            
            if (matchesFilter && (searchQuery.isEmpty() || 
                (report.getCaseNumber() != null && report.getCaseNumber().toLowerCase().contains(searchQuery)) ||
                (report.getIncidentType() != null && report.getIncidentType().toLowerCase().contains(searchQuery)))) {
                filteredReports.add(report);
            }
        }
        
        sortReports();
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        updateEmptyState();
    }
    
    private void sortReports() {
        if ("Newest First".equals(currentSort)) {
            Collections.sort(filteredReports, (a, b) -> Long.compare(b.getDateFiled(), a.getDateFiled()));
        } else {
            Collections.sort(filteredReports, (a, b) -> Long.compare(a.getDateFiled(), b.getDateFiled()));
        }
    }
    
    private void updateEmptyState() {
        if (filteredReports.isEmpty()) {
            if (recyclerReports != null) recyclerReports.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            
            String title = "No Cases Found";
            String message = "Cases assigned to you\nwill appear here.";
            
            if (chipPending != null && chipPending.isChecked()) {
                title = "No Pending Cases";
                message = "All your pending cases\nhave been processed.";
            } else if (chipOngoing != null && chipOngoing.isChecked()) {
                title = "No Ongoing Cases";
                message = "No active cases at the moment.";
            } else if (chipResolved != null && chipResolved.isChecked()) {
                title = "No Resolved Cases";
                message = "Resolved cases will\nappear here.";
            }
            
            if (emptyStateTitle != null) emptyStateTitle.setText(title);
            if (emptyStateMessage != null) emptyStateMessage.setText(message);
        } else {
            if (recyclerReports != null) recyclerReports.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }
    
    private void showSortDialog() {
        String[] options = {"Newest First", "Oldest First"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Sort By")
            .setSingleChoiceItems(options, "Newest First".equals(currentSort) ? 0 : 1, (dialog, which) -> {
                currentSort = options[which];
                sortReports();
                if (adapter != null) adapter.notifyDataSetChanged();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
        dialog.show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }
}
