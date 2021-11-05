package local.no10.spriocount;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView countDisplay;

    private Uri imageUri = null;
    private SpirocountImage currentImage = null;

    private SpirocheteDetector detector = null;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
            }
    );

    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                imageUri = uri;
                currentImage = new SpirocountImage(imageUri, imageView);
                currentImage.loadImage();
                new Thread(() -> runObjectDetection(currentImage)).start();
            }
    );

    private final ActivityResultLauncher<Uri> captureImageLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), result -> {
                if (!result) {
                    imageUri = null;
                } else {
                    currentImage = new SpirocountImage(imageUri, imageView);
                    currentImage.loadImage();
                    new Thread(() -> runObjectDetection(currentImage)).start();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button loadImageButton = findViewById(R.id.load_button);
        Button captureImageButton = findViewById(R.id.capture_button);
        imageView = findViewById(R.id.image_view);
        countDisplay = findViewById(R.id.count_display);

        SeekBar thresholdBar = findViewById(R.id.threshold_bar);
        thresholdBar.setProgress(Math.round(SpirocheteDetector.DEFAULT_THRESHOLD * 100));

        detector = new SpirocheteDetector(this);

        loadImageButton.setOnClickListener(view -> selectImageLauncher.launch("image/*"));
        captureImageButton.setOnClickListener(view -> captureImage());
        thresholdBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float threshold = i / 100.0f;
                detector.setThreshold(threshold);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (currentImage != null) {
                    new Thread(() -> runObjectDetection(currentImage)).start();
                }
            }
        });
    }

    /**
     * Launch camera to capture new image.
     */
    private void captureImage() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);

            permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File file;
        try {
            file = File.createTempFile(fileName, ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            imageUri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".fileProvider", file);
        } catch (IOException | IllegalArgumentException e) {
            return;
        }

        captureImageLauncher.launch(imageUri);
    }

    /**
     * Detect spirochetes in image.
     *
     * @param scimage The spirochete image on which to run the image recognition.
     */
    public void runObjectDetection(SpirocountImage scimage) {
        runOnUiThread(() -> countDisplay.setText("0"));

        List<RectF> results = detector.runObjectDetection(currentImage);

        String count = String.format(Locale.getDefault(), "%d", results.size());
        runOnUiThread(() -> countDisplay.setText(count));

        Bitmap display = scimage.drawDetectionResults(results);
        runOnUiThread(() -> imageView.setImageBitmap(display));
    }
}