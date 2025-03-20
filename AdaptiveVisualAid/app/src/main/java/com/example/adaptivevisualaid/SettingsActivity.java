package com.example.adaptivevisualaid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchHeyAva, switchTapGlass, switchCameraAudio, switchUsbCamera;
    private SharedPreferences sharedPreferences;
    private Button btnSaveAndRestart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE);

        // Find switches in the layout
        switchHeyAva = findViewById(R.id.switchHeyAva);
        switchTapGlass = findViewById(R.id.switchTapGlass);
        switchCameraAudio = findViewById(R.id.switchCameraAudio);
        switchUsbCamera = findViewById(R.id.switchUsbCamera);
        btnSaveAndRestart = findViewById(R.id.btnSaveAndRestart);

        // Load saved switch states
        switchHeyAva.setChecked(sharedPreferences.getBoolean("heyAvaEnabled", false));
        switchTapGlass.setChecked(sharedPreferences.getBoolean("tapGlassEnabled", false));
        switchCameraAudio.setChecked(sharedPreferences.getBoolean("cameraAudioEnabled", false));
        switchUsbCamera.setChecked(sharedPreferences.getBoolean("usbCameraEnabled", false));

        // Listen for switch changes
        switchHeyAva.setOnCheckedChangeListener(this::saveSettings);
        switchTapGlass.setOnCheckedChangeListener(this::saveSettings);
        switchCameraAudio.setOnCheckedChangeListener(this::saveSettings);
        switchUsbCamera.setOnCheckedChangeListener(this::saveSettings);

        // Handle "Save and Restart" button
        btnSaveAndRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAllSettings();
//                Toast.makeText(SettingsActivity.this, "Restarting app...", Toast.LENGTH_SHORT).show();
                restartApp();
            }
        });
    }

    private void saveSettings(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (buttonView.getId() == R.id.switchHeyAva) {
            editor.putBoolean("heyAvaEnabled", isChecked);}
        if (buttonView.getId() == R.id.switchTapGlass) {
            editor.putBoolean("tapGlassEnabled", isChecked);}
        if (buttonView.getId() == R.id.switchCameraAudio) {
            editor.putBoolean("cameraAudioEnabled", isChecked);}
        if (buttonView.getId() == R.id.switchUsbCamera) {
            editor.putBoolean("usbCameraEnabled", isChecked);}
        editor.apply();
    }

    private void saveAllSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("heyAvaEnabled", switchHeyAva.isChecked());
        editor.putBoolean("tapGlassEnabled", switchTapGlass.isChecked());
        editor.putBoolean("cameraAudioEnabled", switchCameraAudio.isChecked());
        editor.putBoolean("usbCameraEnabled", switchUsbCamera.isChecked());
        editor.apply();
    }

    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Runtime.getRuntime().exit(0);
    }
}
