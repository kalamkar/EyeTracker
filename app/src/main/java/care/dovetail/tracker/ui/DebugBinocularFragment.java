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

import care.dovetail.ojo.EogProcessor;
import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 4/24/17.
 */

public class DebugBinocularFragment extends Fragment implements DebugUi {
    private static final int GRAPH_UPDATE_MILLIS = 100;

    private Settings settings;
    private Timer chartUpdateTimer;

    private EogProcessor signals;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        settings = new Settings(context);
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
        return inflater.inflate(R.layout.fragment_debug_binocular, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (settings.isDayDream()) {
            view.findViewById(R.id.leftDebug).setPadding(
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_left),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
            view.findViewById(R.id.rightDebug).setPadding(
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_right),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
        }

        view.findViewById(R.id.leftNumber).setVisibility(
                settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);
        view.findViewById(R.id.rightNumber).setVisibility(
                settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);

        chartUpdateTimer = new Timer();
        chartUpdateTimer.schedule(new ChartUpdater(), 0, GRAPH_UPDATE_MILLIS);
    }

    @Override
    public void setDataSource(EogProcessor signals) {
        this.signals = signals;
    }

    @Override
    public void setProgress(int quality) {
        if (getView() == null) {
            return;
        }
        ((ProgressBar) getView().findViewById(R.id.leftProgress)).setProgress(quality);
        ((ProgressBar) getView().findViewById(R.id.rightProgress)).setProgress(quality);
    }

    @Override
    public void showWarning(boolean show) {
        if (getView() == null) {
            return;
        }
        getView().findViewById(R.id.leftWarning).setVisibility(
                show ?  View.VISIBLE : View.INVISIBLE);
        getView().findViewById(R.id.rightWarning).setVisibility(
                show ? View.VISIBLE : View.INVISIBLE);

    }

    @Override
    public void updateStatusUI(final int spinner, final int progress, final int numbers) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getView() == null) {
                    return;
                }
                getView().findViewById(R.id.leftBlue).setVisibility(spinner);
                getView().findViewById(R.id.rightBlue).setVisibility(spinner);

                getView().findViewById(R.id.leftProgress).setVisibility(progress);
                getView().findViewById(R.id.rightProgress).setVisibility(progress);
                getView().findViewById(R.id.leftProgressLabel).setVisibility(progress);
                getView().findViewById(R.id.rightProgressLabel).setVisibility(progress);

                getView().findViewById(R.id.leftNumber).setVisibility(numbers);
                getView().findViewById(R.id.rightNumber).setVisibility(numbers);
            }
        });
    }

    private class ChartUpdater extends TimerTask {
        @Override
        public void run() {
            final ChartFragment leftChart =
                    (ChartFragment) getChildFragmentManager().findFragmentById(R.id.leftChart);
            final ChartFragment rightChart =
                    (ChartFragment) getChildFragmentManager().findFragmentById(R.id.rightChart);
            if (leftChart == null || rightChart == null || getActivity() == null
                    || getView() == null || signals == null) {
                return;
            }
            leftChart.clear();
            rightChart.clear();

            boolean progressVisible =
                    getView().findViewById(R.id.leftProgress).getVisibility() == View.VISIBLE;
            if (settings.shouldShowChart() || progressVisible) {
                leftChart.updateChannel1(signals.horizontal(), signals.horizontalRange());
                leftChart.updateChannel2(signals.vertical(), signals.verticalRange());
                leftChart.updateFeature1(signals.feature1(), signals.feature1Range());
                leftChart.updateFeature1(signals.feature2(), signals.feature2Range());
                rightChart.updateChannel1(signals.horizontal(), signals.horizontalRange());
                rightChart.updateChannel2(signals.vertical(), signals.verticalRange());
                rightChart.updateFeature1(signals.feature1(), signals.feature1Range());
                rightChart.updateFeature2(signals.feature2(), signals.feature2Range());
            }

            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!leftChart.isResumed() || !rightChart.isResumed()) {
                        return;
                    }
                    leftChart.updateUI();
                    rightChart.updateUI();
                    String numbers = signals.getDebugNumbers();
                    ((TextView) getView().findViewById(R.id.leftNumber)).setText(numbers);
                    ((TextView) getView().findViewById(R.id.rightNumber)).setText(numbers);
                }
            });
        }
    }
}
