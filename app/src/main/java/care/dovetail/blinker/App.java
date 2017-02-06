package care.dovetail.blinker;

import android.app.Application;
import android.os.AsyncTask;

/**
 * Created by abhi on 2/6/17.
 */

public class App extends Application {

    private static final String SHOW_CHART = "show_chart";

    private boolean showChart = false;

    @Override
    public void onCreate() {
        super.onCreate();
        showChart = getSharedPreferences(getPackageName(), 0).getBoolean(SHOW_CHART, true);
    }

    @Override
    public void onTerminate() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void[] params) {
                getSharedPreferences(getPackageName(), 0)
                        .edit().putBoolean(SHOW_CHART, showChart).commit();
                return null;
            }
        }.execute();
        super.onTerminate();
    }

    public void setShowChart(boolean showChart) {
        this.showChart = showChart;

    }

    public boolean getShowChart() {
        return showChart;
    }
}
