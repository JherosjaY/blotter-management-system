package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.ui.adapters.UserAdapter;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class UserManagementActivity extends BaseActivity {
    
    private TextInputEditText etSearch;
    private RecyclerView recyclerUsers;
    private android.widget.LinearLayout emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    
    private BlotterDatabase database;
    private List<User> usersList = new ArrayList<>();
    private UserAdapter userAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);
        
        database = BlotterDatabase.getDatabase(this);
        setupToolbar();
        initViews();
        setupRecyclerView();
        setupListeners();
        loadUsers();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("User Management");
        }
    }
    
    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        recyclerUsers = findViewById(R.id.recyclerViewUsers);
        emptyState = findViewById(R.id.emptyState);
        emptyStateCard = findViewById(R.id.emptyStateCard);
    }
    
    private void setupRecyclerView() {
        if (recyclerUsers != null) {
            recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
            userAdapter = new UserAdapter(usersList, user -> showUserOptionsDialog(user));
            recyclerUsers.setAdapter(userAdapter);
        }
    }
    
    private void setupListeners() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterUsers(s.toString());
                }
                
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }
    
    private void loadUsers() {
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Loading users...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                usersList.clear();
                List<User> allUsers = database.userDao().getAllUsers();
                usersList.addAll(allUsers);
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    if (userAdapter != null) {
                        userAdapter.notifyDataSetChanged();
                    }
                    updateEmptyState();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void filterUsers(String query) {
        List<User> filteredList = new ArrayList<>();
        for (User user : usersList) {
            if (user.getFirstName().toLowerCase().contains(query.toLowerCase()) ||
                user.getLastName().toLowerCase().contains(query.toLowerCase()) ||
                user.getUsername().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(user);
            }
        }
        if (userAdapter != null) {
            userAdapter.notifyDataSetChanged();
        }
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (userAdapter != null && userAdapter.getItemCount() == 0) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerUsers != null) recyclerUsers.setVisibility(View.GONE);
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerUsers != null) recyclerUsers.setVisibility(View.VISIBLE);
        }
    }
    
    private void showUserOptionsDialog(User user) {
        Toast.makeText(this, "User: " + user.getFirstName(), Toast.LENGTH_SHORT).show();
    }
    
    private void terminateUser(User user) {
        Toast.makeText(this, "User terminated", Toast.LENGTH_SHORT).show();
    }
    
    private void confirmDeleteUser(User user) {
        Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
    }
}
