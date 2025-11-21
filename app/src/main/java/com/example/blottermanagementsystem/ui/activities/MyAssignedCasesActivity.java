package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.ReportAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MyAssignedCasesActivity extends BaseActivity {
    
    private RecyclerView recyclerCases;
    private LinearLayout emptyState;
    private BlotterDatabase database;
    private PreferencesManager preferencesManager;
    private ReportAdapter adapter;
    private List<BlotterReport> casesList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assigned_cases);
        
        database = BlotterDatabase.getDatabase(this);
        preferencesManager = new PreferencesManager(this);
        
        setupToolbar();
        initViews();
        setupRecyclerView();
        loadAssignedCases();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Assigned Cases");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initViews() {
        recyclerCases = findViewById(R.id.recyclerReports);
        emptyState = findViewById(R.id.emptyState);
    }
    
    private void setupRecyclerView() {
        adapter = new ReportAdapter(casesList, report -> {
            Intent intent = new Intent(this, OfficerCaseDetailActivity.class);
            intent.putExtra("REPORT_ID", report.getId());
            startActivity(intent);
        });
        recyclerCases.setLayoutManager(new LinearLayoutManager(this));
        recyclerCases.setAdapter(adapter);
    }
    
    private void loadAssignedCases() {
        int officerId = preferencesManager.getUserId();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            // Get all reports
            List<BlotterReport> allReports = database.blotterReportDao().getAllReports();
            List<BlotterReport> assignedReports = new ArrayList<>();
            
            // Filter reports assigned to this officer
            for (BlotterReport report : allReports) {
                if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId() == officerId) {
                    assignedReports.add(report);
                }
            }
            
            runOnUiThread(() -> {
                casesList.clear();
                casesList.addAll(assignedReports);
                adapter.updateReports(casesList);
                
                if (assignedReports.isEmpty()) {
                    emptyState.setVisibility(android.view.View.VISIBLE);
                    recyclerCases.setVisibility(android.view.View.GONE);
                } else {
                    emptyState.setVisibility(android.view.View.GONE);
                    recyclerCases.setVisibility(android.view.View.VISIBLE);
                }
            });
        });
    }
    
    // Quiet loading method to prevent black screen flicker
    private void loadAssignedCasesQuietly() {
        int officerId = preferencesManager.getUserId();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get all reports
                List<BlotterReport> allReports = database.blotterReportDao().getAllReports();
                List<BlotterReport> assignedReports = new ArrayList<>();
                
                // Filter reports assigned to this officer
                for (BlotterReport report : allReports) {
                    if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId() == officerId) {
                        assignedReports.add(report);
                    }
                }
                
                // Only update if data actually changed
                if (assignedReports.size() != casesList.size()) {
                    runOnUiThread(() -> {
                        casesList.clear();
                        casesList.addAll(assignedReports);
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        
                        if (assignedReports.isEmpty()) {
                            if (emptyState != null) emptyState.setVisibility(android.view.View.VISIBLE);
                            if (recyclerCases != null) recyclerCases.setVisibility(android.view.View.GONE);
                        } else {
                            if (emptyState != null) emptyState.setVisibility(android.view.View.GONE);
                            if (recyclerCases != null) recyclerCases.setVisibility(android.view.View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("MyAssignedCases", "Error in quiet loading: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAssignedCasesQuietly(); // Use quiet refresh to prevent black screen flicker
    }
}
