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
import com.example.blottermanagementsystem.data.entity.Witness;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.Executors;

public class AddWitnessDialogFragment extends DialogFragment {

    private EditText etFullName, etAddress, etContactNumber, etStatement;
    private MaterialButton btnSave;
    private int reportId;
    private OnWitnessSavedListener listener;

    public interface OnWitnessSavedListener {
        void onWitnessSaved(Witness witness);
    }

    public static AddWitnessDialogFragment newInstance(int reportId, OnWitnessSavedListener listener) {
        AddWitnessDialogFragment fragment = new AddWitnessDialogFragment();
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
        return inflater.inflate(R.layout.dialog_add_witness, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        etFullName = view.findViewById(R.id.etWitnessFullName);
        etAddress = view.findViewById(R.id.etWitnessAddress);
        etContactNumber = view.findViewById(R.id.etWitnessContactNumber);
        etStatement = view.findViewById(R.id.etWitnessStatement);
        btnSave = view.findViewById(R.id.btnSaveWitness);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveWitness());
    }

    private void saveWitness() {
        String fullName = etFullName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String statement = etStatement.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(getContext(), "Full Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Witness witness = new Witness();
        witness.setBlotterReportId(reportId);
        witness.setName(fullName);
        witness.setAddress(address);
        witness.setContactNumber(contactNumber);
        witness.setStatement(statement);
        witness.setCreatedAt(System.currentTimeMillis());

        // Save to database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.witnessDao().insertWitness(witness);
                    witness.setId((int) id);
                    
                    // Sync to API if network available
                    NetworkMonitor networkMonitor = new NetworkMonitor(getContext());
                    if (networkMonitor.isNetworkAvailable()) {
                        ApiClient.getApiService().createWitness(witness).enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                if (response.isSuccessful()) {
                                    android.util.Log.d("AddWitness", "✅ Synced to API");
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                android.util.Log.w("AddWitness", "⚠️ API sync failed: " + t.getMessage());
                            }
                        });
                    }
                    
                    // Notify on main thread
                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onWitnessSaved(witness);
                        }
                        Toast.makeText(getContext(), "Witness added!", Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error saving witness: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
