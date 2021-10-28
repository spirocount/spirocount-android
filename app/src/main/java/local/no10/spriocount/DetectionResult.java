package local.no10.spriocount;

import android.graphics.RectF;

/**
 * Represents a single object detected by image recognition.
 */
public class DetectionResult {

    /**
     * Coordinates surrounding the object.
     */
    public final RectF boundingBox;

    /**
     * Prediction probability.
     */
    public final String text;

    /**
     * Create a new detection result.
     *
     * @param boundingBox Rectangle of result coordinates.
     * @param text        Prediction probability.
     */
    DetectionResult(RectF boundingBox, String text) {
        this.boundingBox = boundingBox;
        this.text = text;
    }
}
