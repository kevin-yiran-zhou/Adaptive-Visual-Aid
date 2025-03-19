package com.example.adaptivevisualaid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchHeyAva, switchTapGlass, switchCameraAudio, switchUsbCamera;
    private SharedPreferences sharedPreferences;

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
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        editor.apply();
    }
}