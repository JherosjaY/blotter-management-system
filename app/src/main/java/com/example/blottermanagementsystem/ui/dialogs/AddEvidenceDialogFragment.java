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
import com.example.blottermanagementsystem.data.entity.Evidence;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.Executors;

public class AddEvidenceDialogFragment extends DialogFragment {

    private ChipGroup chipGroupType;
    private EditText etTitle, etDescription;
    private MaterialButton btnSave;
    private int reportId;
    private String evidenceType = "photo";
    private OnEvidenceSavedListener listener;

    public interface OnEvidenceSavedListener {
        void onEvidenceSaved(Evidence evidence);
    }

    public static AddEvidenceDialogFragment newInstance(int reportId, OnEvidenceSavedListener listener) {
        AddEvidenceDialogFragment fragment = new AddEvidenceDialogFragment();
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
        return inflater.inflate(R.layout.dialog_add_evidence, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        chipGroupType = view.findViewById(R.id.chipGroupEvidenceType);
        etTitle = view.findViewById(R.id.etEvidenceTitle);
        etDescription = view.findViewById(R.id.etEvidenceDescription);
        btnSave = view.findViewById(R.id.btnSaveEvidence);
    }

    private void setupListeners() {
        chipGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                evidenceType = chip.getText().toString().toLowerCase();
            }
        });
        btnSave.setOnClickListener(v -> saveEvidence());
    }

    private void saveEvidence() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Evidence evidence = new Evidence();
        evidence.setBlotterReportId(reportId);
        evidence.setEvidenceType(evidenceType);
        evidence.setDescription(description);
        evidence.setCollectedBy("Officer");
        evidence.setCollectedDate(System.currentTimeMillis());

        // Save to database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.evidenceDao().insertEvidence(evidence);
                    evidence.setId((int) id);
                    
                    // Sync to API if network available
                    NetworkMonitor networkMonitor = new NetworkMonitor(getContext());
                    if (networkMonitor.isNetworkAvailable()) {
                        ApiClient.getApiService().createEvidence(evidence).enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                if (response.isSuccessful()) {
                                    android.util.Log.d("AddEvidence", "✅ Synced to API");
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                android.util.Log.w("AddEvidence", "⚠️ API sync failed: " + t.getMessage());
                            }
                        });
                    }
                    
                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onEvidenceSaved(evidence);
                        }
                        Toast.makeText(getContext(), "Evidence added!", Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error saving evidence: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
