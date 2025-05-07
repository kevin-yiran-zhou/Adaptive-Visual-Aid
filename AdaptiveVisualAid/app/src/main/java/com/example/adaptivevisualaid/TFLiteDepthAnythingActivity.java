package com.example.adaptivevisualaid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TFLiteDepthAnythingActivity extends AppCompatActivity {

    private static final String TAG = "TFLiteDepthAnything";
    private static final String MODEL_NAME = "depth_anything.tflite";
    private static final int PICK_IMAGE_REQUEST = 1001;

    private Interpreter tflite;

    private ImageView originalImageView;
    private ImageView depthImageView;
    private TextView inferenceTimeText;
    private TextView totalTimeText;

    private Bitmap originalBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tflite_depth_anything);

        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        originalImageView = findViewById(R.id.imageOriginal);
        depthImageView = findViewById(R.id.imageDepth);
        inferenceTimeText = findViewById(R.id.txtInferenceTime);
        totalTimeText = findViewById(R.id.txtTotalTime);

        btnSelectImage.setOnClickListener(v -> selectImageFromGallery());

        new Thread(() -> {
            try {
                File modelFile = copyAssetToFile(MODEL_NAME);
                tflite = new Interpreter(modelFile);
                Log.d(TAG, "TFLite model loaded successfully!");
                runOnUiThread(() -> Toast.makeText(this, "TFLite model loaded", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e(TAG, "Failed to load TFLite model", e);
                runOnUiThread(() -> Toast.makeText(this, "TFLiteModel load failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private File copyAssetToFile(String assetName) throws Exception {
        File file = new File(getCacheDir(), assetName);
        if (!file.exists()) {
            try (InputStream is = getAssets().open(assetName);
                 FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }
        }
        return file;
    }

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private Bitmap loadBitmapWithCorrectOrientation(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        InputStream exifStream = getContentResolver().openInputStream(uri);
        ExifInterface exif = new ExifInterface(exifStream);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        exifStream.close();

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                long totalStart = System.currentTimeMillis();
                Log.d(TAG, "1");
                originalBitmap = loadBitmapWithCorrectOrientation(imageUri);
                Log.d(TAG, "2");
                originalImageView.setImageBitmap(originalBitmap);
                Log.d(TAG, "3");
                runDepthInference(originalBitmap);
                Log.d(TAG, "4");
                long totalEnd = System.currentTimeMillis();
                float seconds = (totalEnd - totalStart) / 1000f;
                totalTimeText.setText(String.format("Total time: %.2f seconds", seconds));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runDepthInference(Bitmap bitmap) {
        if (tflite == null) {
            Toast.makeText(this, "Model not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 518, 518, true);
        float[][][][] input = preprocess(resized);  // shape: [1, 518, 518, 3]
        float[][][][] output = new float[1][1][518][518]; // adjust based on actual output shape if needed

        long start = System.currentTimeMillis();
        tflite.run(input, output);
        long end = System.currentTimeMillis();
        float[][] depth = output[0][0];

        Bitmap depthBitmap = toGrayscaleBitmap(depth);
        Bitmap scaledDepth = Bitmap.createScaledBitmap(depthBitmap, originalBitmap.getWidth(), originalBitmap.getHeight(), true);
        depthImageView.setImageBitmap(scaledDepth);

        float seconds = (end - start) / 1000f;
        inferenceTimeText.setText(String.format("Inference time: %.2f seconds", seconds));
    }

    private float[][][][] preprocess(Bitmap bmp) {
        int width = bmp.getWidth(), height = bmp.getHeight();
        float[][][][] input = new float[1][height][width][3];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int px = bmp.getPixel(x, y);
                float r = ((px >> 16) & 0xFF) / 255.0f;
                float g = ((px >> 8) & 0xFF) / 255.0f;
                float b = (px & 0xFF) / 255.0f;
                input[0][y][x][0] = r;
                input[0][y][x][1] = g;
                input[0][y][x][2] = b;
            }
        }
        return input;
    }

    private Bitmap toGrayscaleBitmap(float[][] depth) {
        int width = depth[0].length;
        int height = depth.length;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (float[] row : depth) for (float v : row) {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float norm = (depth[y][x] - min) / (max - min + 1e-6f);
                int gray = (int) (norm * 255);
                int color = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
                bmp.setPixel(x, y, color);
            }
        }

        return bmp;
    }
}
