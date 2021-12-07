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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.tensorflow.lite.support.image.TensorImage;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents an image for use with the application.
 */
public class SpirocountImage {

    private static final int DEFAULT_IMAGE_SIZE = 960;

    private final Uri uri;
    private final ImageView view;
    private Bitmap bitmap = null;

    private final Lock imageLock = new ReentrantLock();
    private boolean imageAvailable = false;
    private final Condition imageCondition = imageLock.newCondition();

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
        Glide.with(view.getContext()).clear(view);
        imageLock.lock();
        imageAvailable = false;
        Glide.with(view.getContext())
                .load(uri)
                .error(R.drawable.ic_baseline_broken_image)
                .placeholder(R.drawable.ic_baseline_insert_photo)
                .override(DEFAULT_IMAGE_SIZE)
                .centerInside()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        bitmap = null;
                        imageAvailable = true;
                        imageCondition.signalAll();
                        imageLock.unlock();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        BitmapDrawable img = (BitmapDrawable) resource;
                        bitmap = img.getBitmap();
                        imageAvailable = true;
                        imageCondition.signalAll();
                        imageLock.unlock();
                        return true;
                    }
                })
                .into(view);
    }

    /**
     * Get the bitmap underlying the image.
     *
     * @return Bitmap underlying the image.
     */
    public Bitmap bitmap() {
        imageLock.lock();
        while (!imageAvailable) {
            try {
                imageCondition.await(100L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {

            }
        }

        Bitmap image = bitmap;
        imageLock.unlock();
        return image;
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
     * @return A new bitmap with the detected Spirochetes outlined.
     */
    public Bitmap drawDetectionResults(@NonNull List<RectF> detectionResults) {
        Bitmap boxedImage = bitmap.copy(bitmap.getConfig(), true);
        Canvas canvas = new Canvas(boxedImage);
        Paint pen = new Paint();
        pen.setColor(Color.RED);
        pen.setStrokeWidth(8.0f);
        pen.setStyle(Paint.Style.STROKE);

        for (RectF result : detectionResults) {
            canvas.drawRect(result, pen);
        }

        return boxedImage;
    }
}
