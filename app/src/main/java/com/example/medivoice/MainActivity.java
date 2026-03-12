package com.example.medivoice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    // UI Elements
    private TextView tvOutput, tvLocation;
    private FloatingActionButton btnMic, btnSendText;
    private MaterialCardView cardAmbulance, cardHospital, cardPolice, cardDoctor, bottomInputCard;
    private Chip chipHeadache, chipFever, chipChestPain;
    private EditText etSymptom;
    private ImageView btnHistory;
    private ProgressBar aiProgress;

    // SERVICES
    private TextToSpeech tts;
    private FusedLocationProviderClient fusedLocationClient;
    private AppDatabase db;

    // MODE FLAG
    private boolean isDoctorMode = false;

    // CODES
    private static final int SPEECH_REQUEST_CODE = 101;
    private static final int MIC_PERMISSION_CODE = 103;
    private static final int CALL_PERMISSION_CODE = 104;
    private static final int LOCATION_PERMISSION_CODE = 105;

    private final String GEMINI_API_KEY = "AIzaSyBd0oIzkfHK8ITEYS7mbIuLdxtdbxLDpaQ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        tvOutput = findViewById(R.id.tvOutput);
        tvLocation = findViewById(R.id.tvLocation);
        btnMic = findViewById(R.id.btnMic);
        btnSendText = findViewById(R.id.btnSendText);
        etSymptom = findViewById(R.id.etSymptom);
        btnHistory = findViewById(R.id.btnHistory);
        bottomInputCard = findViewById(R.id.bottomInputCard);
        aiProgress = findViewById(R.id.aiProgress);

        cardAmbulance = findViewById(R.id.cardAmbulance);
        cardHospital = findViewById(R.id.cardHospital);
        cardPolice = findViewById(R.id.cardPolice);
        cardDoctor = findViewById(R.id.cardDoctor);

        chipHeadache = findViewById(R.id.chipHeadache);
        chipFever = findViewById(R.id.chipFever);
        chipChestPain = findViewById(R.id.chipChestPain);

        // Services
        tts = new TextToSpeech(this, this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = AppDatabase.getDatabase(this);

        updateLocationDisplay();

        // New Back Button Logic (Crucial for your requirement)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isDoctorMode) {
                    exitDoctorMode();
                } else {
                    setEnabled(false); // Disable this callback to allow app closure
                    finish();
                }
            }
        });

        // Click Listeners
        btnMic.setOnClickListener(v -> {
            if (checkPermission(Manifest.permission.RECORD_AUDIO))
                startVoiceInput();
            else
                requestPermission(Manifest.permission.RECORD_AUDIO, MIC_PERMISSION_CODE);
        });

        btnSendText.setOnClickListener(v -> {
            String text = etSymptom.getText().toString().trim();
            if (!text.isEmpty()) {
                callGeminiAI(text);
                etSymptom.setText("");
            } else {
                Toast.makeText(this, "Please type a symptom", Toast.LENGTH_SHORT).show();
            }
        });

        chipHeadache.setOnClickListener(v -> callGeminiAI("Headache"));
        chipFever.setOnClickListener(v -> callGeminiAI("Fever"));
        chipChestPain.setOnClickListener(v -> callGeminiAI("Chest Pain"));

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, HistoryActivity.class)));

        cardAmbulance.setOnClickListener(v -> tryEmergencyCall("108"));
        cardHospital.setOnClickListener(v -> tryEmergencyCall("102"));
        cardPolice.setOnClickListener(v -> tryEmergencyCall("100"));
        cardDoctor.setOnClickListener(v -> openGoogleMaps());
    }

    // ---------------- AI LOGIC ----------------

    private void callGeminiAI(String symptom) {
        enterDoctorMode();

        tvOutput.setText("Doctor AI is analyzing your symptoms... 🩺");
        if (aiProgress != null) aiProgress.setVisibility(View.VISIBLE);

        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        btnMic.startAnimation(pulse);

        // Fixed Model Name to 1.5-flash
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // ENHANCED DOCTOR PROMPT
        String prompt = "Act as a professional Medical Doctor. Provide a clinical report for: " + symptom + ". " +
                "Response must be strictly in this format, NO asterisks, NO hashtags, NO bold marks: " +
                "PATIENT CLINICAL REPORT. " +
                "1. PRELIMINARY DIAGNOSIS: Explain what this might be clinically. " +
                "2. MEDICINE ADVICE: Suggest specific over the counter medicines or common drugs used for this. " +
                "3. SUGGESTIONS AND DIET: Give 3 medical suggestions for lifestyle or diet. " +
                "4. IMMEDIATE PRECAUTIONS: Safety steps for next 24 hours. " +
                "5. URGENCY STATUS: Clearly state if they must visit an ER or a clinic.";

        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                // Strip all markdown symbols to fix TTS engine stability
                String rawText = result.getText();
                final String cleanText = rawText.replaceAll("[*#_]", "");

                runOnUiThread(() -> {
                    if (aiProgress != null) aiProgress.setVisibility(View.GONE);
                    btnMic.clearAnimation();
                    tvOutput.setText(cleanText);
                    speakText(cleanText);

                    Executors.newSingleThreadExecutor().execute(() ->
                            db.symptomDao().insert(new SymptomHistory(symptom, cleanText, System.currentTimeMillis()))
                    );
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    if (aiProgress != null) aiProgress.setVisibility(View.GONE);
                    btnMic.clearAnimation();
                    tvOutput.setText("Consultation failed. Check your internet connection.");
                });
            }
        }, getMainExecutor());
    }

    // ---------------- UI MODES ----------------

    private void enterDoctorMode() {
        isDoctorMode = true;

        // Hide Everything except the result area
        cardAmbulance.setVisibility(View.GONE);
        cardHospital.setVisibility(View.GONE);
        cardPolice.setVisibility(View.GONE);
        cardDoctor.setVisibility(View.GONE);
        chipHeadache.setVisibility(View.GONE);
        chipFever.setVisibility(View.GONE);
        chipChestPain.setVisibility(View.GONE);
        bottomInputCard.setVisibility(View.GONE);

        // Display the output clearly
        tvOutput.setTextSize(16);
    }

    private void exitDoctorMode() {
        isDoctorMode = false;

        // Stop AI Voice if it's currently speaking
        if (tts != null) tts.stop();

        // Show Home Grid back
        cardAmbulance.setVisibility(View.VISIBLE);
        cardHospital.setVisibility(View.VISIBLE);
        cardPolice.setVisibility(View.VISIBLE);
        cardDoctor.setVisibility(View.VISIBLE);
        chipHeadache.setVisibility(View.VISIBLE);
        chipFever.setVisibility(View.VISIBLE);
        chipChestPain.setVisibility(View.VISIBLE);
        bottomInputCard.setVisibility(View.VISIBLE);

        tvOutput.setText("How can I help you today?");
        tvOutput.setTextSize(20);
    }

    // ---------------- LOCATION & PERMISSIONS ----------------

    private void updateLocationDisplay() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    tvLocation.setText("Location: " + location.getLatitude() + "," + location.getLongitude());
                }
            });
        }
    }

    private boolean checkPermission(String p) {
        return ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String p, int c) {
        ActivityCompat.requestPermissions(this, new String[]{p}, c);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == MIC_PERMISSION_CODE) startVoiceInput();
            if (requestCode == LOCATION_PERMISSION_CODE) updateLocationDisplay();
        }
    }

    // ---------------- SYSTEM HELPERS ----------------

    private void tryEmergencyCall(String number) {
        if (checkPermission(Manifest.permission.CALL_PHONE)) {
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number)));
        } else {
            requestPermission(Manifest.permission.CALL_PHONE, CALL_PERMISSION_CODE);
        }
    }

    private void openGoogleMaps() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=doctors+hospitals")));
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Voice engine missing", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) callGeminiAI(result.get(0));
        }
    }

    // ---------------- TTS ENGINE ----------------

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            tts.setSpeechRate(0.85f); // Professional slow pace
        }
    }

    private void speakText(String text) {
        if (tts != null) {
            // Remove every symbol that is not a letter or number for 100% speech stability
            String cleanForSpeech = text.replaceAll("[^a-zA-Z0-9., ]", "");
            tts.speak(cleanForSpeech, TextToSpeech.QUEUE_FLUSH, null, "MediVoice");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}