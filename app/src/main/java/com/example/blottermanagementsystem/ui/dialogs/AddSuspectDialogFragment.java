package com.example.blottermanagementsystem.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Suspect;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.Executors;

public class AddSuspectDialogFragment extends DialogFragment {

    private EditText etFullName, etAlias, etAddress, etDescription;
    private MaterialButton btnSave;
    private int reportId;
    private OnSuspectSavedListener listener;

    public interface OnSuspectSavedListener {
        void onSuspectSaved(Suspect suspect);
    }

    public static AddSuspectDialogFragment newInstance(int reportId, OnSuspectSavedListener listener) {
        AddSuspectDialogFragment fragment = new AddSuspectDialogFragment();
        Bundle args = new Bundle();
        args.putInt("report_id", reportId);
        fragment.setArguments(args);
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Dialog);
        if (getArguments() != null) {
            reportId = getArguments().getInt("report_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_suspect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        etFullName = view.findViewById(R.id.etSuspectFullName);
        etAlias = view.findViewById(R.id.etSuspectAlias);
        etAddress = view.findViewById(R.id.etSuspectAddress);
        etDescription = view.findViewById(R.id.etSuspectDescription);
        btnSave = view.findViewById(R.id.btnSaveSuspect);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveSuspect());
    }

    private void saveSuspect() {
        String fullName = etFullName.getText().toString().trim();
        String alias = etAlias.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(getContext(), "Full Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Suspect suspect = new Suspect();
        suspect.setBlotterReportId(reportId);
        suspect.setName(fullName);
        suspect.setAlias(alias);
        suspect.setAddress(address);
        suspect.setDescription(description);
        suspect.setDateAdded(System.currentTimeMillis());

        // Save to database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.suspectDao().insertSuspect(suspect);
                    suspect.setId((int) id);
                    
                    // Sync to API if network available
                    NetworkMonitor networkMonitor = new NetworkMonitor(getContext());
                    if (networkMonitor.isNetworkAvailable()) {
                        ApiClient.getApiService().createSuspect(suspect).enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                if (response.isSuccessful()) {
                                    android.util.Log.d("AddSuspect", "✅ Synced to API");
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                android.util.Log.w("AddSuspect", "⚠️ API sync failed: " + t.getMessage());
                            }
                        });
                    }
                    
                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onSuspectSaved(suspect);
                        }
                        Toast.makeText(getContext(), "Suspect added!", Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error saving suspect: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
