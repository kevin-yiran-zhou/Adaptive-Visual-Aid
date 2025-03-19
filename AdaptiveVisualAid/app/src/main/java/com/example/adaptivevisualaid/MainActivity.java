package com.example.adaptivevisualaid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import android.Manifest;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    public TextToSpeech tts;
    public boolean isTtsReady = false;
    boolean heyAvaEnabled;
    boolean tapGlassEnabled;
    boolean cameraAudioEnabled;
    boolean usbCameraEnabled;
    boolean usbMicFound = false;
    public VoiceAssistant voiceAssistant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }


        tts = new TextToSpeech(this, this, "com.google.android.tts"); // Use Google TTS
        TextView txtSpeechResult = findViewById(R.id.txtSpeechResult);
        voiceAssistant = new VoiceAssistant(this, txtSpeechResult);

        findViewById(R.id.btnNavigation).setOnClickListener(v -> speakAndLaunch("Going to Google Maps", this::openGoogleMaps));
        findViewById(R.id.btnWhatIsInFront).setOnClickListener(v -> speakAndLaunch("Going to Envision AI", this::openEnvisionAI));
        findViewById(R.id.btnNeedHelp).setOnClickListener(v -> speakAndLaunch("Going to Be My Eyes", this::openBeMyEyes));

        // Microphone button long press to start voice recognition
        ImageButton btnMicrophone = findViewById(R.id.btnMicrophone);
        btnMicrophone.setOnLongClickListener(v -> {
            Log.d("VoiceAssistant", "Microphone button long pressed");
            // Reset TTS to avoid accidental execution of old actions
            tts.setOnUtteranceProgressListener(null);
            speak("Hi, how can I help you?");
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
        heyAvaEnabled = sharedPreferences.getBoolean("heyAvaEnabled", false);
        tapGlassEnabled = sharedPreferences.getBoolean("tapGlassEnabled", false);
        cameraAudioEnabled = sharedPreferences.getBoolean("cameraAudioEnabled", false);
        usbCameraEnabled = sharedPreferences.getBoolean("usbCameraEnabled", false);
        // Apply settings
        if (heyAvaEnabled) {
            startWakeWordDetection();  // Start "Hey Ava" listening
        }
        if (tapGlassEnabled) {
            // Implement tap detection later if needed
        }
        if (cameraAudioEnabled) {
            checkUsbMicrophoneAvailability();
        }
        if (usbCameraEnabled) {
            // Implement later if needed
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Microphone Permission Denied", Toast.LENGTH_SHORT).show();
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

    private void checkUsbMicrophoneAvailability() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        String message = "Checking USB Microphone...";  // Initialize message
        Boolean micFound = false;

        if (audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).length > 0) {
            for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                if (device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                    message = "USB Microphone Found: " + device.getProductName();
                    micFound = true;
                    break;
                }
            }
        }
        usbMicFound = micFound;
        if (!micFound) {
            message = "No USB Microphone Detected";
        }
        // Show a Toast message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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