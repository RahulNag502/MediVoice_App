package com.example.medivoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<SymptomHistory> historyList;

    public HistoryAdapter(List<SymptomHistory> list) { this.historyList = list; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SymptomHistory item = historyList.get(position);
        holder.symptom.setText(item.symptom);
        holder.advice.setText(item.advice);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        holder.date.setText(sdf.format(new Date(item.timestamp)));

        // CLICK TO EXPAND LOGIC
        holder.itemView.setOnClickListener(v -> {
            if (holder.advice.getMaxLines() == 2) {
                // Expand
                holder.advice.setMaxLines(100);
                holder.readMore.setText("Tap to collapse");
            } else {
                // Collapse
                holder.advice.setMaxLines(2);
                holder.readMore.setText("Tap to read full report");
            }
        });
    }

    @Override
    public int getItemCount() { return historyList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView symptom, advice, date, readMore;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            symptom = itemView.findViewById(R.id.histSymptom);
            advice = itemView.findViewById(R.id.histAdvice);
            date = itemView.findViewById(R.id.histDate);
            readMore = itemView.findViewById(R.id.tvReadMore);
        }
    }
}