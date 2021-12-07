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

import android.content.Context;
import android.graphics.RectF;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpirocheteDetector {

    public static final float DEFAULT_THRESHOLD = 0.3f;

    private float threshold;
    private final Context context;

    SpirocheteDetector(Context context) {
        this.context = context;
        this.threshold = DEFAULT_THRESHOLD;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    /**
     * Detect spirochetes in image.
     *
     * @param scimage The spirochete image on which to run the image recognition.
     * @return A list of all the spirochetes detected.
     */
    public List<RectF> runObjectDetection(SpirocountImage scimage) {
        TensorImage image = scimage.tensorImage();
        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .build();

        ObjectDetector detector;
        try {
            detector = ObjectDetector.createFromFileAndOptions(
                    context,
                    "model.tflite",
                    options);
        } catch (IOException e) {
            return new ArrayList<>();
        }

        List<RectF> boundingBoxes = new ArrayList<>();
        List<Detection> results = detector.detect(image);
        for (Detection result : results) {
            boundingBoxes.add(result.getBoundingBox());
        }

        return boundingBoxes;
    }
}
