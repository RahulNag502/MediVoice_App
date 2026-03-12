package com.example.medivoice;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "symptom_history")
public class SymptomHistory {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String symptom;
    public String advice;
    public long timestamp;

    public SymptomHistory(String symptom, String advice, long timestamp) {
        this.symptom = symptom;
        this.advice = advice;
        this.timestamp = timestamp;
    }
}