package com.example.adaptivevisualaid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.TextView;
import java.util.List;

public class VoiceAssistant {
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 100;
    private boolean isListening = false;
    private final Activity activity;
    private final TextView txtSpeechResult;

    public VoiceAssistant(Activity activity, TextView txtSpeechResult) {
        this.activity = activity;
        this.txtSpeechResult = txtSpeechResult;
    }

    public void startListening() {
        isListening = true;
        txtSpeechResult.setVisibility(TextView.VISIBLE);
        txtSpeechResult.setText("Listening...");
        listenLoop();
    }

    private void listenLoop() {
        new Thread(() -> {
            while (isListening) {
                activity.runOnUiThread(this::startVoiceRecognition);
                try {
                    Thread.sleep(3000); // Small delay to avoid rapid triggers
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "How can I help you?");

        try {
            activity.startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            txtSpeechResult.setText("Speech recognition not supported");
            isListening = false;
        }
    }

    public void handleVoiceRecognitionResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String recognizedText = results.get(0);
                txtSpeechResult.setText("You said: " + recognizedText);
                isListening = false; // Stop listening loop
            }
        }
    }
}
