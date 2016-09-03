package care.dovetail.blinker;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import care.dovetail.blinker.ChartView.Chart;
import care.dovetail.blinker.SignalProcessor.Feature;

public class ChartFragment extends Fragment {
	private static final String TAG = "ChartFragment";

	private Chart eog;
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

		ChartView ecgView = ((ChartView) getView().findViewById(R.id.eog));
		eog = ecgView.makeLineChart(Color.BLUE, 2);
		eog.setXRange(0, Config.GRAPH_LENGTH);
		eog.setYRange(Config.SHORT_GRAPH_MIN, Config.SHORT_GRAPH_MAX);

		blinks =	ecgView.makePointsChart(
				getResources().getColor(android.R.color.holo_orange_dark), 5);
		blinks.setXRange(0, Config.GRAPH_LENGTH);
		blinks.setYRange(Config.SHORT_GRAPH_MIN, Config.SHORT_GRAPH_MAX);

		median = ecgView.makeLineChart(getResources().getColor(android.R.color.darker_gray), 2);
		median.setXRange(0, Config.GRAPH_LENGTH);
		median.setYRange(Config.SHORT_GRAPH_MIN, Config.SHORT_GRAPH_MAX);
	}

	public void clear() {
		((ChartView) getView().findViewById(R.id.eog)).clear();
	}

	public void update(int data[], List<Feature> blinks, int medianAmplitude) {
		List<Pair<Integer, Integer>> points = new ArrayList<Pair<Integer, Integer>>(data.length);
		for (int i = 0; i < data.length; i++) {
			points.add(Pair.create(i, data[i]));
		}
		eog.setData(points);

		List<Pair<Integer, Integer>> medianPoints = new ArrayList<Pair<Integer, Integer>>(2);
		medianPoints.add(Pair.create(0, medianAmplitude));
		medianPoints.add(Pair.create(Config.GRAPH_LENGTH - 1, medianAmplitude));
		median.setData(medianPoints);

		List<Pair<Integer, Integer>> blinkPoints = new ArrayList<Pair<Integer, Integer>>();
		for (int i = 0; i < blinks.size(); i++) {
			Feature blink = blinks.get(i);
			blinkPoints.add(Pair.create(blink.index, blink.min + blink.min == 0 ? 5 : 0));
			blinkPoints.add(Pair.create(blink.index, blink.max - blink.max == 0 ? 5 : 0));
		}
		this.blinks.setData(blinkPoints);

		getView().findViewById(R.id.eog).invalidate();
	}
}
