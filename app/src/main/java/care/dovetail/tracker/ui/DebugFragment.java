package care.dovetail.tracker.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.tracker.EOGProcessor;
import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 4/24/17.
 */

public class DebugFragment extends Fragment implements DebugUi {
    private static final int GRAPH_UPDATE_MILLIS = 100;

    private Settings settings;
    private Timer chartUpdateTimer;

    private EOGProcessor signals;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        settings = new Settings(context);

        chartUpdateTimer = new Timer();
        chartUpdateTimer.schedule(new ChartUpdater(), 0, GRAPH_UPDATE_MILLIS);
    }

    @Override
    public void onDetach() {
        if (chartUpdateTimer != null) {
            chartUpdateTimer.cancel();
        }
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.number).setVisibility(
                settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void setDataSource(EOGProcessor signals) {
        this.signals = signals;
    }

    @Override
    public void setProgress(int quality) {
        if (getView() == null) {
            return;
        }
        ((ProgressBar) getView().findViewById(R.id.progress)).setProgress(quality);
    }

    @Override
    public void showWarning(boolean show) {
        if (getView() == null) {
            return;
        }
        getView().findViewById(R.id.warning).setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void updateStatusUI(final int spinner, final int progress, final int numbers) {
        if (getActivity() == null || getView() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getView().findViewById(R.id.blue).setVisibility(spinner);

                getView().findViewById(R.id.progress).setVisibility(progress);
                getView().findViewById(R.id.progressLabel).setVisibility(progress);

                getView().findViewById(R.id.number).setVisibility(numbers);
            }
        });
    }

    private class ChartUpdater extends TimerTask {
        @Override
        public void run() {
            final ChartFragment chart =
                    (ChartFragment) getChildFragmentManager().findFragmentById(R.id.chart);
            if (chart == null || getActivity() == null || getView() == null) {
                return;
            }
            chart.clear();

            if (settings.shouldShowChart()) {
                chart.updateChannel1(signals.horizontal(), signals.horizontalRange());
                chart.updateChannel2(signals.vertical(), signals.verticalRange());
                chart.updateFeature1(signals.feature1(), signals.feature1Range());
                chart.updateFeature2(signals.feature2(), signals.feature2Range());
            }

            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!chart.isResumed()) {
                        return;
                    }
                    chart.updateUI();
                    String numbers = signals.getDebugNumbers();
                    ((TextView) getView().findViewById(R.id.number)).setText(numbers);
                }
            });
        }
    }
}
