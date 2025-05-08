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

        Button btnSegformerOnnx = findViewById(R.id.btn_segformer_onnx);
        Button btnDepthAnythingOnnx = findViewById(R.id.btn_depth_anything_onnx);
        Button btnDepthAnythingTflite = findViewById(R.id.btn_depth_anything_tflite);
        Button btnRealtimeOnnx = findViewById(R.id.btn_realtime_onnx);

        btnSegformerOnnx.setOnClickListener(v -> {
            Intent intent = new Intent(ModelSelectionActivity.this, ONNXSegformerActivity.class);
            startActivity(intent);
        });

        btnDepthAnythingOnnx.setOnClickListener(v -> {
            Intent intent = new Intent(ModelSelectionActivity.this, ONNXDepthAnythingActivity.class);
            startActivity(intent);
        });

        btnDepthAnythingTflite.setOnClickListener(v -> {
            Intent intent = new Intent(ModelSelectionActivity.this, TFLiteDepthAnythingActivity.class);
            startActivity(intent);
        });

        btnRealtimeOnnx.setOnClickListener(v -> {
            Intent intent = new Intent(ModelSelectionActivity.this, ONNXRealtimeActivity.class);
            startActivity(intent);
        });
    }
}
