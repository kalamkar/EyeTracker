package care.dovetail.blinker;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import care.dovetail.blinker.ChartView.Chart;

public class ChartFragment extends Fragment {
    private static final String TAG = "ChartFragment";

    private int lightBlue;
    private int darkBlue;
    private int lightGreen;
    private int darkGreen;

    private ChartView chartView;

    private Chart channel1;
    private Chart channel2;
    private Chart median1;
    private Chart median2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        darkBlue = getResources().getColor(android.R.color.holo_blue_dark);
        lightBlue = getResources().getColor(android.R.color.holo_blue_light);
        darkGreen = getResources().getColor(android.R.color.holo_green_dark);
        lightGreen = getResources().getColor(android.R.color.holo_green_light);
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

        median1 = chartView.makeLineChart(lightBlue, 2);
        median1.setXRange(0, Config.GRAPH_LENGTH);

        median2 = chartView.makeLineChart(lightGreen, 2);
        median2.setXRange(0, Config.GRAPH_LENGTH);
    }

    public void clear() {
        chartView.clear();
    }

    public void updateChannel1(int data1[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data1.length);
        for (int i = 0; i < data1.length; i++) {
            points.add(Pair.create(i, data1[i]));
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

    public void updateChannel2(int data2[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data2.length);
        for (int i = 0; i < data2.length; i++) {
            points.add(Pair.create(i, data2[i]));
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

    public void updateUI() {
        chartView.invalidate();
    }
}
