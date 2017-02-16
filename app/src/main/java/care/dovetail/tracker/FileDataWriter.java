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
                String.format("%s-%s.csv", context.getResources().getString(R.string.app_name),
                        FILE_NAME_FORMAT.format(new Date())));
        Log.d(TAG, String.format("Creating file %s", file.getName()));
        try {
            file.createNewFile();
            output = new DataOutputStream(new FileOutputStream(file));
            output.write(String.format("Horizontal Raw, Vertical Raw, Horizontal Filtered, " +
                    "Vertical Filtered, Estimated Column, Estimated Row, " +
                    "Actual Column, Actual Row\n").getBytes());
        } catch (Exception e){
            Log.e(TAG, "Error opening output CSV file.", e);
        }
    }

    public void write(int channel1, int channel2, int filtered1, int filtered2, int column, int row,
                      int realColumn, int realRow) {
        if (output == null) {
            return;
        }
        try {
            output.write(String.format("%d,%d,%d,%d,%d,%d,%d,%d\n", channel1, channel2, filtered1,
                    filtered2, column, row, realColumn, realRow).getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error writing to output file.", e);
        }
    }

    public void close() {
        try {
            output.flush();
            output.close();
            output = null;
            DownloadManager downloads =
                    (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            downloads.addCompletedDownload(file.getName(), "CSV EOG Data", true,
                    "text/csv", file.getAbsolutePath(), file.length(), false);
            MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null,
                    null);
        } catch (Exception e){
            Log.e(TAG, "Error closing output stream and codec.", e);
        }
    }
}