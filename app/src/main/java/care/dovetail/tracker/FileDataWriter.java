package care.dovetail.tracker;

/**
 * Created by abhi on 11/30/16.
 */

import android.app.DownloadManager;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileDataWriter {
    private static final String TAG = "FileDataWriter";

    private static final SimpleDateFormat FILE_NAME_FORMAT =
            new SimpleDateFormat("MMM-dd-kk-mm-ss", Locale.US);

    private final Context context;
    private File file;
    private DataOutputStream output;

    public FileDataWriter(Context context) {
        this.context = context;
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        file = new File(dir,
                String.format("%s-%s.raw", context.getResources().getString(R.string.app_name),
                        FILE_NAME_FORMAT.format(new Date())));
        try {
            file.createNewFile();
            output = new DataOutputStream(new FileOutputStream(file));
        } catch (Exception e){
            Log.e(TAG, "Error opening output RAW file.", e);
        }
    }

    public void write(int channel1, int channel2) {
        if (output == null) {
            return;
        }
        try {
            output.writeInt(channel1);
            output.writeInt(channel2);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to RAW output file.", e);
        }
    }

    public void close() {
        try {
            output.flush();
            output.close();
            output = null;
            DownloadManager downloads =
                    (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            downloads.addCompletedDownload(file.getName(), "EOG Raw Data", true,
                    "application/x-binary", file.getAbsolutePath(), file.length(), false);
            MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null,
                    null);
        } catch (Exception e){
            Log.e(TAG, "Error closing output stream and codec.", e);
        }
    }
}