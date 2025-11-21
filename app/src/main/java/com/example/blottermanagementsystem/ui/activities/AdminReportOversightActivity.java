package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.ReportAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.RoleAccessControl;
import java.util.ArrayList;
import java.util.List;

public class AdminReportOversightActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private ReportAdapter adapter;
    private TextView tvEmpty, tvTotalReports, tvPendingReports, tvResolvedReports;
    private BlotterDatabase database;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PreferencesManager preferencesManager = new PreferencesManager(this);
        if (!RoleAccessControl.checkAccess(this, "Admin", preferencesManager)) {
            return;
        }
        
        setContentView(R.layout.activity_admin_view_all_reports);
        
        database = BlotterDatabase.getDatabase(this);
        
        setupToolbar();
        initViews();
        loadReports();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report Oversight");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvTotalReports = findViewById(R.id.tvTotalReports);
        tvPendingReports = findViewById(R.id.tvPendingReports);
        tvResolvedReports = findViewById(R.id.tvResolvedReports);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(new ArrayList<>(), report -> {
            // Open admin case detail activity (FULL SUPERVISORY ACCESS)
            Intent intent = new Intent(this, AdminCaseDetailActivity.class);
            intent.putExtra("reportId", report.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }
    
    private void loadReports() {
        new Thread(() -> {
            List<BlotterReport> reports = database.blotterReportDao().getAllReports();
            
            int total = reports.size();
            long pending = reports.stream().filter(r -> "Pending".equals(r.getStatus())).count();
            long resolved = reports.stream().filter(r -> "Resolved".equals(r.getStatus())).count();
            
            runOnUiThread(() -> {
                tvTotalReports.setText(String.valueOf(total));
                tvPendingReports.setText(String.valueOf(pending));
                tvResolvedReports.setText(String.valueOf(resolved));
                
                if (reports.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                    adapter.setReports(reports);
                }
            });
        }).start();
    }
}
