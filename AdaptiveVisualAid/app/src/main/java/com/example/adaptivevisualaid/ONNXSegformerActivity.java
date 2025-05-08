package com.example.adaptivevisualaid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ai.onnxruntime.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ONNXSegformerActivity extends AppCompatActivity {

    private static final String TAG = "ONNXSegformerActivity";
    private static final String MODEL_NAME = "segformer-b2-finetuned-ade-512-512.onnx";
    private static final int PICK_IMAGE_REQUEST = 1001;

    private OrtEnvironment env;
    private OrtSession session;

    private ImageView originalImageView;
    private ImageView segImageView;
    private TextView inferenceTimeText;
    private TextView totalTimeText;

    private Bitmap originalBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onnx_segformer);

        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        originalImageView = findViewById(R.id.imageOriginal);
        segImageView = findViewById(R.id.imageDepth); // reuse this ID
        inferenceTimeText = findViewById(R.id.txtInferenceTime);
        totalTimeText = findViewById(R.id.txtTotalTime);

        btnSelectImage.setOnClickListener(v -> selectImageFromGallery());

        new Thread(() -> {
            try {
                File modelFile = copyAssetToFile(MODEL_NAME);
                env = OrtEnvironment.getEnvironment();
                OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
                if (OrtEnvironment.getAvailableProviders().contains("NNAPI")) {
                    sessionOptions.addNnapi();
                    Log.d(TAG, "NNAPI enabled for ONNX Runtime.");
                } else {
                    Log.d(TAG, "NNAPI not available.");
                }
                session = env.createSession(modelFile.getAbsolutePath(), sessionOptions);

                Log.d(TAG, MODEL_NAME + " loaded successfully!");
                runOnUiThread(() -> Toast.makeText(this, MODEL_NAME + " loaded successfully!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e(TAG, "Failed to load " + MODEL_NAME, e);
                runOnUiThread(() -> Toast.makeText(this, MODEL_NAME + " load failed", Toast.LENGTH_SHORT).show());
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
                matrix.postRotate(90); break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270); break;
            default:
                return bitmap;
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap blendBitmaps(Bitmap base, Bitmap mask, float alpha) {
        Bitmap result = Bitmap.createBitmap(base.getWidth(), base.getHeight(), Bitmap.Config.ARGB_8888);

        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int basePixel = base.getPixel(x, y);
                int maskPixel = mask.getPixel(x, y);

                int r1 = (basePixel >> 16) & 0xFF;
                int g1 = (basePixel >> 8) & 0xFF;
                int b1 = basePixel & 0xFF;

                int r2 = (maskPixel >> 16) & 0xFF;
                int g2 = (maskPixel >> 8) & 0xFF;
                int b2 = maskPixel & 0xFF;

                int r = (int) (r1 * (1 - alpha) + r2 * alpha);
                int g = (int) (g1 * (1 - alpha) + g2 * alpha);
                int b = (int) (b1 * (1 - alpha) + b2 * alpha);

                int blended = 0xFF000000 | (r << 16) | (g << 8) | b;
                result.setPixel(x, y, blended);
            }
        }

        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                long totalStart = System.currentTimeMillis();
                originalBitmap = loadBitmapWithCorrectOrientation(imageUri);
                originalImageView.setImageBitmap(originalBitmap);
                runSegmentationInference(originalBitmap);
                long totalEnd = System.currentTimeMillis();
                float seconds = (totalEnd - totalStart) / 1000f;
                totalTimeText.setText(String.format("Total time: %.2f seconds", seconds));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runSegmentationInference(Bitmap bitmap) {
        if (session == null) {
            Toast.makeText(this, "Model not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
        float[] input = preprocess(resized);

        try {
            long start = System.currentTimeMillis();
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), new long[]{1, 3, 512, 512});
            OrtSession.Result output = session.run(Collections.singletonMap("pixel_values", inputTensor));
            long end = System.currentTimeMillis();

            float[][][][] logits = (float[][][][]) output.get(0).getValue(); // [1, 150, H, W]
            int[][] segMap = argmax2D(logits[0]);

            Bitmap segBitmap = decodeSegmentationMap(segMap);
            Bitmap scaledSeg = Bitmap.createScaledBitmap(segBitmap, originalBitmap.getWidth(), originalBitmap.getHeight(), true);
            Bitmap overlay = blendBitmaps(originalBitmap, scaledSeg, 0.75f);  // 75% mask transparency
            segImageView.setImageBitmap(overlay);

            // ⬇️ Build filtered legend
            LinearLayout legendLayout = findViewById(R.id.legendLayout);
            legendLayout.removeAllViews();

            int[][] colors = getADE20KColors();
            String[] labels = getADE20KLabels();

            // Step 1: Gather used class IDs
            Set<Integer> usedClasses = new HashSet<>();
            for (int y = 0; y < segMap.length; y++) {
                for (int x = 0; x < segMap[0].length; x++) {
                    usedClasses.add(segMap[y][x]);
                }
            }

            // Step 2: Add legend entries for only used classes
            for (int classId : usedClasses) {
                if (classId >= labels.length || classId >= colors.length) continue;

                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setPadding(10, 0, 10, 0);
                itemLayout.setGravity(Gravity.CENTER_HORIZONTAL);

                View colorBox = new View(this);
                int size = (int) (getResources().getDisplayMetrics().density * 40); // 24dp
                LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(size, size);
                colorBox.setLayoutParams(boxParams);
                colorBox.setBackgroundColor(Color.rgb(colors[classId][0], colors[classId][1], colors[classId][2]));

                TextView label = new TextView(this);
                label.setText(labels[classId]);
                label.setTextSize(10f);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(1);

                itemLayout.addView(colorBox);
                itemLayout.addView(label);
                legendLayout.addView(itemLayout);
            }

            float seconds = (end - start) / 1000f;
            inferenceTimeText.setText(String.format("Inference time: %.2f seconds", seconds));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float[] preprocess(Bitmap bmp) {
        int width = bmp.getWidth(), height = bmp.getHeight();
        float[] data = new float[3 * width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int px = bmp.getPixel(x, y);
                float r = ((px >> 16) & 0xFF) / 255.0f;
                float g = ((px >> 8) & 0xFF) / 255.0f;
                float b = (px & 0xFF) / 255.0f;
                int idx = y * width + x;
                data[idx] = r;
                data[width * height + idx] = g;
                data[2 * width * height + idx] = b;
            }
        }
        return data;
    }

    private int[][] argmax2D(float[][][] logits) {
        int channels = logits.length;
        int height = logits[0].length;
        int width = logits[0][0].length;
        int[][] classMap = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float maxVal = logits[0][y][x];
                int maxIdx = 0;
                for (int c = 1; c < channels; c++) {
                    if (logits[c][y][x] > maxVal) {
                        maxVal = logits[c][y][x];
                        maxIdx = c;
                    }
                }
                classMap[y][x] = maxIdx;
            }
        }

        return classMap;
    }

    private Bitmap decodeSegmentationMap(int[][] segMap) {
        int height = segMap.length;
        int width = segMap[0].length;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[][] COLORS = getADE20KColors();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int classId = segMap[y][x];
                int[] color = classId < COLORS.length ? COLORS[classId] : new int[]{0, 0, 0};
                int rgb = 0xFF000000 | (color[0] << 16) | (color[1] << 8) | color[2];
                bmp.setPixel(x, y, rgb);
            }
        }

        return bmp;
    }

    private String[] getADE20KLabels() {
        return new String[] {
                "wall", "building", "sky", "floor", "tree", "ceiling", "road", "bed", "windowpane", "grass",
                "cabinet", "sidewalk", "person", "earth", "door", "table", "mountain", "plant", "curtain", "chair",
                "car", "water", "painting", "sofa", "shelf", "house", "sea", "mirror", "rug", "field",
                "armchair", "seat", "fence", "desk", "rock", "wardrobe", "lamp", "bathtub", "railing", "cushion",
                "base", "box", "column", "signboard", "chest of drawers", "counter", "sand", "sink", "skyscraper", "fireplace",
                "refrigerator", "grandstand", "path", "stairs", "runway", "case", "pool table", "pillow", "screen door", "stairway",
                "river", "bridge", "bookcase", "blind", "coffee table", "toilet", "flower", "book", "hill", "bench",
                "countertop", "stove", "palm", "kitchen island", "computer", "swivel chair", "boat", "bar", "arcade machine", "hovel",
                "bus", "towel", "light", "truck", "tower", "chandelier", "awning", "streetlight", "booth", "television receiver",
                "airplane", "dirt track", "apparel", "pole", "land", "bannister", "escalator", "ottoman", "bottle", "buffet",
                "poster", "stage", "van", "ship", "fountain", "conveyer belt", "canopy", "washer", "plaything", "swimming pool",
                "stool", "barrel", "basket", "waterfall", "tent", "bag", "minibike", "cradle", "oven", "ball",
                "food", "step", "tank", "trade name", "microwave", "pot", "animal", "bicycle", "lake", "dishwasher",
                "screen", "blanket", "sculpture", "hood", "sconce", "vase", "traffic light", "tray", "ashcan", "fan",
                "pier", "crt screen", "plate", "monitor", "bulletin board", "shower", "radiator", "glass", "clock", "flag"
        };
    }

    private int[][] getADE20KColors() {
        return new int[][] {
                {120, 120, 120}, {180, 120, 120}, {6, 230, 230}, {80, 50, 50}, {4, 200, 3},
                {120, 120, 80}, {140, 140, 140}, {204, 5, 255}, {230, 230, 230}, {4, 250, 7},
                {224, 5, 255}, {235, 255, 7}, {150, 5, 61}, {120, 120, 70}, {8, 255, 51},
                {255, 6, 82}, {143, 255, 140}, {204, 255, 4}, {255, 51, 7}, {204, 70, 3},
                {0, 102, 200}, {61, 230, 250}, {255, 6, 51}, {11, 102, 255}, {255, 7, 71},
                {255, 9, 224}, {9, 7, 230}, {220, 220, 220}, {255, 9, 92}, {112, 9, 255},
                {8, 255, 214}, {7, 255, 224}, {255, 184, 6}, {10, 255, 71}, {255, 41, 10},
                {7, 255, 255}, {224, 255, 8}, {102, 8, 255}, {255, 61, 6}, {255, 194, 7},
                {255, 122, 8}, {0, 255, 20}, {255, 8, 41}, {255, 5, 153}, {6, 51, 255},
                {235, 12, 255}, {160, 150, 20}, {0, 163, 255}, {140, 140, 140}, {250, 10, 15},
                {20, 255, 0}, {31, 255, 0}, {255, 31, 0}, {255, 224, 0}, {153, 255, 0},
                {0, 0, 255}, {255, 71, 0}, {0, 235, 255}, {0, 173, 255}, {31, 0, 255},
                {11, 200, 200}, {255, 82, 0}, {0, 255, 245}, {0, 61, 255}, {0, 255, 112},
                {0, 255, 133}, {255, 0, 0}, {255, 163, 0}, {255, 102, 0}, {194, 255, 0},
                {0, 143, 255}, {51, 255, 0}, {0, 82, 255}, {0, 255, 41}, {0, 255, 173},
                {10, 0, 255}, {173, 255, 0}, {0, 255, 153}, {255, 92, 0}, {255, 0, 255},
                {255, 0, 245}, {255, 0, 102}, {255, 173, 0}, {255, 0, 20}, {255, 184, 184},
                {0, 31, 255}, {0, 255, 61}, {0, 71, 255}, {255, 0, 204}, {0, 255, 194},
                {0, 255, 82}, {0, 10, 255}, {0, 112, 255}, {51, 0, 255}, {0, 194, 255},
                {0, 122, 255}, {0, 255, 163}, {255, 153, 0}, {0, 255, 10}, {255, 112, 0},
                {143, 255, 0}, {82, 0, 255}, {163, 255, 0}, {255, 235, 0}, {8, 184, 170},
                {133, 0, 255}, {0, 255, 92}, {184, 0, 255}, {255, 0, 31}, {0, 184, 255},
                {0, 214, 255}, {255, 0, 112}, {92, 255, 0}, {0, 224, 255}, {112, 224, 255},
                {70, 184, 160}, {163, 0, 255}, {153, 0, 255}, {71, 255, 0}, {255, 0, 163},
                {255, 204, 0}, {255, 0, 143}, {0, 255, 235}, {133, 255, 0}, {255, 0, 235},
                {245, 0, 255}, {255, 0, 122}, {255, 245, 0}, {10, 190, 212}, {214, 255, 0},
                {0, 204, 255}, {20, 0, 255}, {255, 255, 0}, {0, 153, 255}, {0, 41, 255},
                {0, 255, 204}, {41, 0, 255}, {41, 255, 0}, {173, 0, 255}, {0, 245, 255},
                {71, 0, 255}, {122, 0, 255}, {0, 255, 184}, {0, 92, 255}, {184, 255, 0},
                {0, 133, 255}, {255, 214, 0}, {25, 194, 194}, {102, 255, 0}, {92, 0, 255}
        };
    }
}
