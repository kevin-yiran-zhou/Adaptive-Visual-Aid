package com.example.adaptivevisualaid;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;
import java.util.List;
import android.os.Handler;
import android.os.Looper;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    public TextToSpeech tts;
    public boolean isTtsReady = false;
    public VoiceAssistant voiceAssistant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(this, this, "com.google.android.tts"); // Use Google TTS
        TextView txtSpeechResult = findViewById(R.id.txtSpeechResult);
        voiceAssistant = new VoiceAssistant(this, txtSpeechResult);

        findViewById(R.id.btnNavigation).setOnClickListener(v -> speakAndLaunch("Going to Google Maps", this::openGoogleMaps));
        findViewById(R.id.btnWhatIsInFront).setOnClickListener(v -> speakAndLaunch("Going to Envision AI", this::openEnvisionAI));
        findViewById(R.id.btnNeedHelp).setOnClickListener(v -> speakAndLaunch("Going to Be My Eyes", this::openBeMyEyes));

        // Microphone button long press to start voice recognition
        ImageButton btnMicrophone = findViewById(R.id.btnMicrophone);
        btnMicrophone.setOnLongClickListener(v -> {
            System.out.print("long click detected");
            speak("Hi, how can I help you?");
//            voiceAssistant.startListening();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                voiceAssistant.startListening();
            }, 2000);
            return true;
        });

        // Menu Button Click Listener
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, v);
            popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return true;
                } else if (item.getItemId() == R.id.menu_login) {
                    // Handle login click (future implementation)
                    return true;
                }
                return false;
            });

            popup.show();
        });

        // Load settings from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE);
        boolean heyAvaEnabled = sharedPreferences.getBoolean("heyAvaEnabled", false);
        boolean tapGlassEnabled = sharedPreferences.getBoolean("tapGlassEnabled", false);
        // Apply settings
        if (heyAvaEnabled) {
            startWakeWordDetection();  // Start "Hey Ava" listening
        }
        if (tapGlassEnabled) {
            // Implement tap detection later if needed
        }

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (tts.setLanguage(Locale.US) >= TextToSpeech.LANG_AVAILABLE) {
                isTtsReady = true;
            }
        }
    }

    public void speakAndLaunch(String message, Runnable action) {
        if (!isTtsReady) return;

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onDone(String utteranceId) { runOnUiThread(action); }
            @Override public void onError(String utteranceId) { runOnUiThread(action); }
        });

        speak(message);
    }

    public void speak(String message) {
        if (isTtsReady) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "TTS_MESSAGE_ID");
        }
    }

    public void openGoogleMaps() {
        openAppOrStore("com.google.android.apps.maps", "https://play.google.com/store/apps/details?id=com.google.android.apps.maps");
    }

    public void openBeMyEyes() {
        openAppOrStore("com.bemyeyes.bemyeyes", "https://play.google.com/store/apps/details?id=com.bemyeyes.bemyeyes");
    }

    public void openEnvisionAI() {
        openAppOrStore("com.letsenvision.envisionai", "https://play.google.com/store/apps/details?id=com.letsenvision.envisionai");
    }

    public void openAppOrStore(String packageName, String storeUrl) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        startActivity(launchIntent != null ? launchIntent : new Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl)));
    }

    private void startWakeWordDetection() {
        // Placeholder for "Hey Ava" voice activation
        // We will implement this next!
        Log.d("VoiceAssistant", "'Hey Ava' detection started");
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
//            // Reload preferences when returning from settings
//            SharedPreferences sharedPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE);
//            boolean heyAvaEnabled = sharedPreferences.getBoolean("heyAvaEnabled", false);
//
//            if (heyAvaEnabled) {
//                startWakeWordDetection();
//            } else {
//                stopWakeWordDetection(); // Stop listening if the user turns it off
//            }
//        }
        voiceAssistant.handleVoiceRecognitionResult(requestCode, resultCode, data);
    }
}