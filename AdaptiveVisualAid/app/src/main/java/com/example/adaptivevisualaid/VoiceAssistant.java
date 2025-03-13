package com.example.adaptivevisualaid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.TextView;
import java.util.List;
import java.util.*;

public class VoiceAssistant {
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 100;
    private boolean isListening = false;
    private final Activity activity;
    private final TextView txtSpeechResult;
    private final Map<String, List<String>> keywordMap;
    public VoiceAssistant(Activity activity, TextView txtSpeechResult) {
        this.activity = activity;
        this.txtSpeechResult = txtSpeechResult;
        keywordMap = new HashMap<>();
        keywordMap.put("Google Maps", Arrays.asList("maps", "google maps", "directions", "navigation", "go somewhere", "place"));
        keywordMap.put("Be My Eyes", Arrays.asList("be my eyes", "be my eye", "help", "assistance", "volunteer"));
        keywordMap.put("Envision AI", Arrays.asList("envision", "object", "in front of me", "describe"));
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
                if (isListening) { // Prevent multiple triggers
                    activity.runOnUiThread(this::startVoiceRecognition);
                }
                try {
                    Thread.sleep(6000); // Avoid rapid triggers
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

    private String findMatchingApp(String text) {
        for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return null; // No match found
    }

    public void textUnderstand(String text) {
        MainActivity mainActivity = (MainActivity) activity;
        text = text.toLowerCase();
        String matchedApp = findMatchingApp(text);
        if (matchedApp == null) {
            mainActivity.speak("Sorry, I don't understand. Can you say it again?");
            return;
        }
        if (matchedApp.equals("Google Maps")) {
            mainActivity.speakAndLaunch("OK. Going to Google Maps", mainActivity::openGoogleMaps);
        } else if (matchedApp.equals("Be My Eyes")) {
            mainActivity.speakAndLaunch("OK. Going to Be My Eyes", mainActivity::openBeMyEyes);
        } else if (matchedApp.equals("Envision AI")) {
            mainActivity.speakAndLaunch("OK. Going to Envision AI", mainActivity::openEnvisionAI);
        } else {
            mainActivity.speak("Sorry, I don't understand.");
        }
    }

    public void handleVoiceRecognitionResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String recognizedText = results.get(0);
                txtSpeechResult.setText("You said: " + recognizedText);
                isListening = false; // Stop listening loop
                textUnderstand(recognizedText);
            }
        }
    }
}
