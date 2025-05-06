package com.example.adaptivevisualaid;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SegDepActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("TODO: Seg + Dep");
        textView.setTextSize(30);
        textView.setPadding(40, 100, 40, 40);
        setContentView(textView);
    }
}
