package com.example.blottermanagementsystem.ui.dialogs;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.example.blottermanagementsystem.data.entity.Hearing;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ScheduleHearingDialogFragment extends DialogFragment {

    private EditText etDate, etTime, etLocation, etPurpose, etPresidingOfficer, etNotes;
    private MaterialButton btnSave;
    private int reportId;
    private OnHearingSavedListener listener;
    private Calendar selectedDate;

    public interface OnHearingSavedListener {
        void onHearingSaved(Hearing hearing);
    }

    public static ScheduleHearingDialogFragment newInstance(int reportId, OnHearingSavedListener listener) {
        ScheduleHearingDialogFragment fragment = new ScheduleHearingDialogFragment();
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
        selectedDate = Calendar.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_schedule_hearing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        etDate = view.findViewById(R.id.etHearingDate);
        etTime = view.findViewById(R.id.etHearingTime);
        etLocation = view.findViewById(R.id.etHearingLocation);
        etPurpose = view.findViewById(R.id.etHearingPurpose);
        etPresidingOfficer = view.findViewById(R.id.etPresidingOfficer);
        etNotes = view.findViewById(R.id.etHearingNotes);
        btnSave = view.findViewById(R.id.btnScheduleHearing);
    }

    private void setupListeners() {
        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
        btnSave.setOnClickListener(v -> saveHearing());
    }

    private void showDatePicker() {
        // Use primary dark blue theme for date picker (no violet accent)
        DatePickerDialog dialog = new DatePickerDialog(getContext(), R.style.DatePickerTheme,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                etDate.setText(sdf.format(selectedDate.getTime()));
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showTimePicker() {
        // Use primary dark blue theme for time picker (no violet accent)
        TimePickerDialog dialog = new TimePickerDialog(getContext(), R.style.TimePickerTheme,
            (view, hourOfDay, minute) -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                etTime.setText(time);
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            true);
        dialog.show();
    }

    private void saveHearing() {
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String purpose = etPurpose.getText().toString().trim();

        if (date.isEmpty() || time.isEmpty() || location.isEmpty() || purpose.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Hearing hearing = new Hearing();
        hearing.setBlotterReportId(reportId);
        hearing.setHearingDate(date);
        hearing.setHearingTime(time);
        hearing.setLocation(location);
        hearing.setPurpose(purpose);
        hearing.setCreatedAt(System.currentTimeMillis());

        // Save to database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.hearingDao().insertHearing(hearing);
                    hearing.setId((int) id);
                    
                    // Sync to API if network available
                    NetworkMonitor networkMonitor = new NetworkMonitor(getContext());
                    if (networkMonitor.isNetworkAvailable()) {
                        ApiClient.getApiService().createHearing(hearing).enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                if (response.isSuccessful()) {
                                    android.util.Log.d("ScheduleHearing", "✅ Synced to API");
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                android.util.Log.w("ScheduleHearing", "⚠️ API sync failed: " + t.getMessage());
                            }
                        });
                    }
                    
                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onHearingSaved(hearing);
                        }
                        Toast.makeText(getContext(), "Hearing scheduled!", Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error saving hearing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
