package care.dovetail.blinker;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import care.dovetail.blinker.ChartView.Chart;
import care.dovetail.blinker.SignalProcessor.Feature;

public class ChartFragment extends Fragment {
    private static final String TAG = "ChartFragment";

    private ChartView chartView;

    private Chart channel1;
    private Chart channel2;
    private Chart blinks;
    private Chart median;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chartView = ((ChartView) getView().findViewById(R.id.eog));
        channel1 = chartView.makeLineChart(Color.BLUE, 2);
        channel1.setXRange(0, Config.GRAPH_LENGTH);
//        channel1.setYRange(Config.SHORT_GRAPH_MIN, Config.SHORT_GRAPH_MAX);

        channel2 = chartView.makeLineChart(Color.GREEN, 2);
        channel2.setXRange(0, Config.GRAPH_LENGTH);
//        channel2.setYRange(Config.SHORT_GRAPH_MIN, Config.SHORT_GRAPH_MAX);

        blinks = chartView.makePointsChart(
                getResources().getColor(android.R.color.holo_orange_dark), 5);
        blinks.setXRange(0, Config.GRAPH_LENGTH);
//        blinks.setYRange(Config.SHORT_GRAPH_MIN, Config.SHORT_GRAPH_MAX);

        median = chartView.makeLineChart(getResources().getColor(android.R.color.darker_gray), 2);
        median.setXRange(0, Config.GRAPH_LENGTH);
//        median.setYRange(Config.SHORT_GRAPH_MIN, Config.SHORT_GRAPH_MAX);
    }

    public void clear() {
        chartView.clear();
    }

    public void updateData(int data1[], int data2[], List<Feature> blinks, int medianAmplitude) {
        List<Pair<Integer, Integer>> points = new ArrayList<Pair<Integer, Integer>>(data1.length);
        for (int i = 0; i < data1.length; i++) {
            points.add(Pair.create(i, data1[i]));
        }
        channel1.setData(points);

        points = new ArrayList<Pair<Integer, Integer>>(data2.length);
        for (int i = 0; i < data2.length; i++) {
            points.add(Pair.create(i, data2[i]));
        }
        channel2.setData(points);

        List<Pair<Integer, Integer>> medianPoints = new ArrayList<Pair<Integer, Integer>>(2);
        medianPoints.add(Pair.create(0, medianAmplitude));
        medianPoints.add(Pair.create(Config.GRAPH_LENGTH - 1, medianAmplitude));
        median.setData(medianPoints);

        if (blinks != null) {
            List<Pair<Integer, Integer>> blinkPoints = new ArrayList<Pair<Integer, Integer>>();
            for (int i = 0; i < blinks.size(); i++) {
                Feature blink = blinks.get(i);
                blinkPoints.add(Pair.create(blink.index, blink.min + blink.min == 0 ? 5 : 0));
                blinkPoints.add(Pair.create(blink.index, blink.max - blink.max == 0 ? 5 : 0));
            }
            this.blinks.setData(blinkPoints);
        }
    }

    public void updateUI() {
        chartView.invalidate();
    }
}
