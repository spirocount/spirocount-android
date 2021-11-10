package local.no10.spriocount;

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
