package care.dovetail.tracker.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.R;
import care.dovetail.tracker.ui.ChartView.Chart;

public class ChartFragment extends Fragment {
    private static final String TAG = "ChartFragment";

    private int darkBlue;
    private int darkGreen;
    private int lightRed;
    private int orange;
    private int purple;

    private ChartView chartView;

    private Chart channel1;
    private Chart channel2;
    private Chart channel3;
    private Chart feature1;
    private Chart feature2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        darkBlue = getResources().getColor(android.R.color.holo_blue_dark);
        darkGreen = getResources().getColor(android.R.color.holo_green_dark);
        lightRed = getResources().getColor(android.R.color.holo_red_light);
        orange = getResources().getColor(android.R.color.holo_orange_dark);
        purple = getResources().getColor(android.R.color.holo_purple);
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chartView = ((ChartView) getView().findViewById(R.id.eog));
        channel1 = chartView.makeLineChart(darkBlue, 2);
        channel1.setXRange(0, Config.GRAPH_LENGTH);

        channel2 = chartView.makeLineChart(darkGreen, 2);
        channel2.setXRange(0, Config.GRAPH_LENGTH);

        channel3 = chartView.makeLineChart(lightRed, 1);
        channel3.setXRange(0, Config.GRAPH_LENGTH);

        feature1 = chartView.makePointsChart(orange, 5);
        feature1.setXRange(0, Config.GRAPH_LENGTH);

        feature2 = chartView.makePointsChart(purple, 5);
        feature2.setXRange(0, Config.GRAPH_LENGTH);
    }

    public void clear() {
        chartView.clear();
    }

    public void updateChannel1(int data[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            points.add(Pair.create(i, data[i]));
        }
        channel1.setYRange(range.first, range.second);
        channel1.setData(points);
    }

    public void updateChannel2(int data[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            points.add(Pair.create(i, data[i]));
        }
        channel2.setYRange(range.first, range.second);
        channel2.setData(points);
    }

    public void updateChannel3(int data[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            points.add(Pair.create(i, data[i]));
        }
        channel3.setYRange(range.first, range.second);
        channel3.setData(points);
    }

    public void updateFeature1(int data[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0) {
                points.add(Pair.create(i, data[i]));
            }
        }
        feature1.setYRange(range.first, range.second);
        feature1.setData(points);
    }

    public void updateFeature2(int data[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0) {
                points.add(Pair.create(i, data[i]));
            }
        }
        feature2.setYRange(range.first, range.second);
        feature2.setData(points);
    }

    public void updateUI() {
        chartView.invalidate();
    }
}
