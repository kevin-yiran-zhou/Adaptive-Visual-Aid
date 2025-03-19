package com.example.adaptivevisualaid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import java.util.List;
import java.util.*;

public class VoiceAssistant {
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 100;
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    public boolean isListening = false;
    private final Activity activity;
    private final TextView txtSpeechResult;
    private final Map<String, List<String>> keywordMap;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private final SpeechRecognizer speechRecognizer;

    public VoiceAssistant(Activity activity, TextView txtSpeechResult) {
        this.activity = activity;
        this.txtSpeechResult = txtSpeechResult;
        this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        setupSpeechRecognition();

        keywordMap = new HashMap<>();
        keywordMap.put("Google Maps", Arrays.asList("maps", "map", "google maps", "direction", "directions", "navigate", "navigation", "go somewhere", "place"));
        keywordMap.put("Envision AI", Arrays.asList("envision", "object", "in front of me", "describe"));
        keywordMap.put("Be My Eyes", Arrays.asList("be my eyes", "be my eye", "help", "assistance", "volunteer"));
    }

    private void setupSpeechRecognition() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}
            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {}
            @Override
            public void onError(int error) {
                String errorMessage = "Sorry, can you say it again?";
                txtSpeechResult.setText(errorMessage);
                MainActivity mainActivity = (MainActivity) activity;
                mainActivity.speak(errorMessage);
            }
            @Override
            public void onResults(Bundle results) {
                List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    final String recognizedText = matches.get(0);
                    activity.runOnUiThread(() -> {
                        txtSpeechResult.setText("You said: " + recognizedText);
                    });
                    textUnderstand(recognizedText);
                }
            }
            @Override
            public void onPartialResults(Bundle partialResults) {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }


    public void startListening() {
        MainActivity mainActivity = (MainActivity) activity;
        boolean cameraAudioEnabled = mainActivity.cameraAudioEnabled;
        boolean usbMicFound = mainActivity.usbMicFound;

        if (cameraAudioEnabled && usbMicFound) {
            startListeningWithUsbMicrophone();
        } else {
            startListeningWithPhoneMicrophone();
        }
    }

    private void startListeningWithPhoneMicrophone() {
        isListening = true;
        txtSpeechResult.setVisibility(TextView.VISIBLE);
        txtSpeechResult.setText("Listening...");

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

    private void startListeningWithUsbMicrophone() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            Toast.makeText(activity, "Microphone permission required!", Toast.LENGTH_SHORT).show();
            return;  // Stop execution if permission is not granted
        }
        txtSpeechResult.setText("Listening with USB microphone...");

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(activity, "Failed to initialize USB microphone", Toast.LENGTH_SHORT).show();
            startListeningWithPhoneMicrophone(); // Fallback to phone microphone if USB mic fails
            return;
        }

        audioRecord.startRecording();
        isRecording = true;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        speechRecognizer.startListening(intent);
    }

    public void stopListening() {
        if (isRecording && audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null; // Prevent further null reference issues
            isRecording = false;
        }
        isListening = false;
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
                final String recognizedText = results.get(0);
                activity.runOnUiThread(() -> {
                    txtSpeechResult.setText("You said: " + recognizedText);
                });
                isListening = false; // Stop listening loop
                textUnderstand(recognizedText);
            }
        }
    }
}
