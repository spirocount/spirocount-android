// Copyright 2021 Dominic Ewing
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.github.spirocount.spirocount_android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import io.github.spirocount.spirocount_android.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private TempFileManager tempFileManager;
    private SpirocountImage currentImage = null;

    private SpirocheteDetector detector = null;

    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                currentImage = new SpirocountImage(uri, binding.imageView);
                currentImage.loadImage();
                new Thread(() -> runObjectDetection(currentImage)).start();
            }
    );

    private final ActivityResultLauncher<Uri> captureImageLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), result -> {
                if (!result) {
                    tempFileManager.deleteTempFile();
                } else {
                    currentImage = new SpirocountImage(tempFileManager.getTempFileUri(), binding.imageView);
                    currentImage.loadImage();
                    new Thread(() -> runObjectDetection(currentImage)).start();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View mainView = binding.getRoot();
        setContentView(mainView);

        tempFileManager = new TempFileManager();

        int defaultThresholdProgress = Math.round(SpirocheteDetector.DEFAULT_THRESHOLD * 100);
        binding.thresholdBar.setProgress(defaultThresholdProgress);

        String thresholdDisplayText = String.format(
                Locale.getDefault(),
                "%.2f",
                SpirocheteDetector.DEFAULT_THRESHOLD);
        binding.thresholdDisplay.setText(thresholdDisplayText);

        detector = new SpirocheteDetector(this);

        binding.loadButton.setOnClickListener(view -> selectImageLauncher.launch("image/*"));
        binding.captureButton.setOnClickListener(view -> captureImage());
        binding.thresholdBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float threshold = i / 100.0f;
                String displayText = String.format(Locale.getDefault(), "%.2f", threshold);
                binding.thresholdDisplay.setText(displayText);
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

    @Override
    protected void onDestroy() {
        tempFileManager.deleteTempFile();
        super.onDestroy();
    }


    /**
     * Create the options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Handle selection of menu items.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == R.id.open_source_menu_item) {
            Intent ossLicenseIntent = new Intent(this, OssLicensesMenuActivity.class);
            startActivity(ossLicenseIntent);
            return true;
        } else if (itemID == R.id.about_menu_item) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Launch camera to capture new image.
     */
    private void captureImage() {
        try {
            File file = File.createTempFile("spirocount_image", null, this.getCacheDir());
            Uri imageUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileProvider", file);
            tempFileManager.storeTempFile(file, imageUri);
        } catch (IOException e) {
            return;
        }

        captureImageLauncher.launch(tempFileManager.getTempFileUri());
    }

    /**
     * Detect spirochetes in image.
     *
     * @param scimage The spirochete image on which to run the image recognition.
     */
    public void runObjectDetection(SpirocountImage scimage) {
        runOnUiThread(() -> binding.countDisplay.setText(R.string.detecting_object_notification));

        List<RectF> results = detector.runObjectDetection(currentImage);

        String count = String.format(Locale.getDefault(), "%d", results.size());
        runOnUiThread(() -> binding.countDisplay.setText(count));

        Bitmap display = scimage.drawDetectionResults(results);
        runOnUiThread(() -> binding.imageView.setImageBitmap(display));
    }
}