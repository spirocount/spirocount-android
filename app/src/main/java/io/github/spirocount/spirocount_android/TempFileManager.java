package io.github.spirocount.spirocount_android;

import android.net.Uri;

import java.io.File;

/**
 * Holds a single file descriptor and associated uri.
 */
public class TempFileManager {
    private File file;
    private Uri uri;

    public TempFileManager() {
        file = null;
        uri = null;
    }

    public boolean empty() {
        return file == null;
    }

    public Uri getTempFileUri() {
        return uri;
    }

    /**
     * Ensuring that the file and uri are related is the responsibility of the caller. Neither should
     * be null.
     */
    public void storeTempFile(File file, Uri uri) {
        if (!empty()) {
            deleteTempFile();
        }
        this.file = file;
        this.uri = uri;
    }

    public void deleteTempFile() {
        if (empty()) {
            return;
        }
        boolean success = file.delete();
        if (success) {
            file = null;
            uri = null;
        }
    }
}
