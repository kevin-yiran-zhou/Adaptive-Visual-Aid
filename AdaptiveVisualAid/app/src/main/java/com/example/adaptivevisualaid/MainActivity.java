package com.example.adaptivevisualaid;

import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Button;
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
        voiceAssistant.handleVoiceRecognitionResult(requestCode, resultCode, data);
    }
}