package care.dovetail.blinker.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import care.dovetail.blinker.Config;
import care.dovetail.blinker.R;
import care.dovetail.blinker.ui.ChartView.Chart;

public class ChartFragment extends Fragment {
    private static final String TAG = "ChartFragment";

    private int lightBlue;
    private int darkBlue;
    private int lightGreen;
    private int darkGreen;
    private int lightRed;
    private int orange;
    private int purple;

    private ChartView chartView;

    private Chart channel1;
    private Chart channel2;
    private Chart channel3;
    private Chart median1;
    private Chart median2;
    private Chart feature1;
    private Chart feature2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        darkBlue = getResources().getColor(android.R.color.holo_blue_dark);
        lightBlue = getResources().getColor(android.R.color.holo_blue_light);
        darkGreen = getResources().getColor(android.R.color.holo_green_dark);
        lightGreen = getResources().getColor(android.R.color.holo_green_light);
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

        median1 = chartView.makeLineChart(lightBlue, 2);
        median1.setXRange(0, Config.GRAPH_LENGTH);

        median2 = chartView.makeLineChart(lightGreen, 2);
        median2.setXRange(0, Config.GRAPH_LENGTH);

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

        int median = (range.second - range.first) / 2 + range.first;
        List<Pair<Integer, Integer>> median1Points = new ArrayList<>(2);
        median1Points.add(Pair.create(0, median));
        median1Points.add(Pair.create(Config.GRAPH_LENGTH - 1, median));
        this.median1.setYRange(range.first, range.second);
        this.median1.setData(median1Points);
    }

    public void updateChannel2(int data[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            points.add(Pair.create(i, data[i]));
        }
        channel2.setYRange(range.first, range.second);
        channel2.setData(points);

        int median = (range.second - range.first) / 2 + range.first;
        List<Pair<Integer, Integer>> median2Points = new ArrayList<>(2);
        median2Points.add(Pair.create(0, median));
        median2Points.add(Pair.create(Config.GRAPH_LENGTH - 1, median));
        this.median2.setYRange(range.first, range.second);
        this.median2.setData(median2Points);
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
