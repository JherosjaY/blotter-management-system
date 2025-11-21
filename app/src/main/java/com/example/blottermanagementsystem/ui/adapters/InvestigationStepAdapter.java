package com.example.blottermanagementsystem.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.model.InvestigationStep;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class InvestigationStepAdapter extends RecyclerView.Adapter<InvestigationStepAdapter.StepViewHolder> {
    
    private List<InvestigationStep> steps;
    private OnStepActionListener listener;
    
    public interface OnStepActionListener {
        void onStepAction(InvestigationStep step);
    }
    
    public InvestigationStepAdapter(List<InvestigationStep> steps, OnStepActionListener listener) {
        this.steps = steps;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_investigation_step, parent, false);
        return new StepViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        InvestigationStep step = steps.get(position);
        
        // Set step title and description
        holder.tvStepTitle.setText(step.getTitle());
        holder.tvStepDescription.setText(step.getDescription());
        
        // Update status indicator
        if (step.isCompleted()) {
            holder.ivStatus.setImageResource(R.drawable.ic_check_circle);
            holder.ivStatus.setColorFilter(holder.itemView.getContext()
                    .getColor(R.color.electric_blue));
        } else if (step.isInProgress()) {
            holder.ivStatus.setImageResource(R.drawable.ic_radio_button_checked);
            holder.ivStatus.setColorFilter(holder.itemView.getContext()
                    .getColor(R.color.electric_blue));
        } else {
            holder.ivStatus.setImageResource(R.drawable.ic_radio_button_unchecked);
            holder.ivStatus.setColorFilter(holder.itemView.getContext()
                    .getColor(android.R.color.darker_gray));
        }
        
        // Show/hide action button
        if (step.getActionText() != null && !step.isCompleted()) {
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setText(step.getActionText());
            holder.btnAction.setIconResource(step.getActionIcon());
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStepAction(step);
                }
            });
        } else {
            holder.btnAction.setVisibility(View.GONE);
        }
        
        // Show/hide timeline lines
        if (position == 0) {
            holder.vTimelineLineTop.setVisibility(View.INVISIBLE);
        } else {
            holder.vTimelineLineTop.setVisibility(View.VISIBLE);
        }
        
        if (position == steps.size() - 1) {
            holder.vTimelineLineBottom.setVisibility(View.INVISIBLE);
        } else {
            holder.vTimelineLineBottom.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public int getItemCount() {
        return steps != null ? steps.size() : 0;
    }
    
    public void updateSteps(List<InvestigationStep> newSteps) {
        this.steps = newSteps;
        notifyDataSetChanged();
    }
    
    static class StepViewHolder extends RecyclerView.ViewHolder {
        View vTimelineLineTop;
        View vTimelineLineBottom;
        ShapeableImageView ivStatus;
        TextView tvStepTitle;
        TextView tvStepDescription;
        MaterialButton btnAction;
        
        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            vTimelineLineTop = itemView.findViewById(R.id.vTimelineLineTop);
            vTimelineLineBottom = itemView.findViewById(R.id.vTimelineLineBottom);
            ivStatus = itemView.findViewById(R.id.ivStatus);
            tvStepTitle = itemView.findViewById(R.id.tvStepTitle);
            tvStepDescription = itemView.findViewById(R.id.tvStepDescription);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
