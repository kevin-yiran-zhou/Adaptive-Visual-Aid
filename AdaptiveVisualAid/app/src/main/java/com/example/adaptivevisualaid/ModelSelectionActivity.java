package com.example.adaptivevisualaid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ModelSelectionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_selection);

        Button btnOnnx = findViewById(R.id.btn_depth_anything_onnx);
        Button btnTflite = findViewById(R.id.btn_depth_anything_tflite);

        btnOnnx.setOnClickListener(v -> {
            Intent intent = new Intent(ModelSelectionActivity.this, ONNXDepthAnythingActivity.class);
            startActivity(intent);
        });

        btnTflite.setOnClickListener(v -> {
            Intent intent = new Intent(ModelSelectionActivity.this, TFLiteDepthAnythingActivity.class);
            startActivity(intent);
        });
    }
}
