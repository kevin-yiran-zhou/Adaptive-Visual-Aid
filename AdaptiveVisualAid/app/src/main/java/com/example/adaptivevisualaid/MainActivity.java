package com.example.adaptivevisualaid;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean isTtsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Text-to-Speech
        tts = new TextToSpeech(this, this);  // Call onInit() when ready

        // Find buttons
        Button btnNavigation = findViewById(R.id.btnNavigation);
        Button btnWhatIsInFront = findViewById(R.id.btnWhatIsInFront);
        Button btnNeedHelp = findViewById(R.id.btnNeedHelp);

        // Set onClickListeners with voice feedback
        btnNavigation.setOnClickListener(v -> speakAndLaunch("Sending you to Google Maps", this::openGoogleMaps));
        btnWhatIsInFront.setOnClickListener(v -> speakAndLaunch("Opening Envision AI", this::openEnvisionAI));
        btnNeedHelp.setOnClickListener(v -> speakAndLaunch("Opening Be My Eyes", this::openBeMyEyes));
    }

    // Ensuring TTS is initialized before speaking
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            isTtsReady = true;
        } else {
            isTtsReady = false;
        }
    }

    // Speak the message, then launch the intent
    private void speakAndLaunch(String message, Runnable action) {
        if (isTtsReady) {
            System.out.println("Speaking");
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            new android.os.Handler().postDelayed(action, 1000);  // Delay action to let audio play first
        } else {
            System.out.println("Not Speaking");
            action.run();  // If TTS isn't ready, just launch the app
        }
    }

    private void openGoogleMaps() {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps"));
            startActivity(browserIntent);
        }
    }

    private void openEnvisionAI() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.letsenvision.envisionai");
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.letsenvision.envisionai"));
            startActivity(playStoreIntent);
        }
    }

    private void openBeMyEyes() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.bemyeyes.bemyeyes");
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.bemyeyes.bemyeyes"));
            startActivity(playStoreIntent);
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
