package local.no10.spriocount;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import org.tensorflow.lite.support.image.TensorImage;

import java.util.List;

/**
 * Represents an image for use with the application.
 */
public class SpirocountImage {

    private static final int imageSize = 960;

    private final Uri uri;
    private final ImageView view;

    /**
     * Construct a SpirocountImage backed by a URI.
     *
     * @param uri  Uri of the image.
     * @param view Target view for the image.
     */
    SpirocountImage(Uri uri, ImageView view) {
        this.uri = uri;
        this.view = view;
    }

    /**
     * Load the image into the view.
     */
    public void loadImage() {
        Glide.with(view.getContext())
                .load(uri)
                .error(R.drawable.ic_baseline_broken_image)
                .override(imageSize)
                .centerInside()
                .into(view);
    }

    /**
     * Get the bitmap underlying the image.
     *
     * @return Bitmap underlying the image.
     */
    public Bitmap bitmap() {
        // TODO(dje): This is a nasty hack and needs to go!
        while (view.getDrawable() == null) {
            // haha spinlock go burr!
        }

        BitmapDrawable img = (BitmapDrawable) view.getDrawable();
        return img.getBitmap();
    }

    /**
     * Get a tensorImage based on the image.
     *
     * @return The tensorImage representing the SpirocountImage.
     */
    public TensorImage tensorImage() {
        // While grayscale might work better for the image model than RGB, grayscale is not
        // currently supported by TFLite.
        return TensorImage.fromBitmap(bitmap());
    }

    /**
     * Draw boxes around the detected objects.
     *
     * @param detectionResults List of detected objects.
     */
    public void drawDetectionResults(@NonNull List<DetectionResult> detectionResults) {
        Canvas canvas = new Canvas(bitmap());
        Paint pen = new Paint();
        pen.setColor(Color.RED);
        pen.setStrokeWidth(8f);
        pen.setStyle(Paint.Style.STROKE);

        // Since this gets called from the processing thread, there is no performance benefit to the
        // UI thread for returning a new image and then displaying it to the view.
        for (DetectionResult result : detectionResults) {
            RectF box = result.boundingBox;
            canvas.drawRect(box, pen);
        }
    }
}
