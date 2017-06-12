package care.dovetail.tracker.ui;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.ojo.EyeEvent;
import care.dovetail.ojo.Gesture;
import care.dovetail.tracker.Config;
import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 4/24/17.
 */

public class SpectaclesFragment extends Fragment implements Gesture.Observer {

    private final Map<String, MediaPlayer> players = new HashMap<>();
    private Settings settings;

    private GestureView content;

    private TextView text;

    private Timer resetTimer;

    @Override
    public void setEyeEventSource(EyeEvent.Source eyeEventSource) {
//        eyeEventSource.add(new Gesture("blink")
//                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 2000))
//                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, 4000))
//                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 2000))
//                .addObserver(this));
        eyeEventSource.add(new Gesture("fixation")
                .add(EyeEvent.Criterion.fixation(1000))
                .addObserver(this));
        eyeEventSource.add(new Gesture("left")
                .add(EyeEvent.Criterion.fixation(1000, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, 1500))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 1500))
                .addObserver(this));
        eyeEventSource.add(new Gesture("right")
                .add(EyeEvent.Criterion.fixation(1000, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 1500))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, 1500))
                .addObserver(this));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        settings = new Settings(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_spectacles, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        content = (GestureView) view.findViewById(R.id.content);
        text = (TextView) view.findViewById(R.id.text);
    }

    @Override
    public void onStart() {
        super.onStart();
        players.put("left", MediaPlayer.create(getContext(), R.raw.slice));
        players.put("right", MediaPlayer.create(getContext(), R.raw.slice));
        players.put("fixation", MediaPlayer.create(getContext(), R.raw.beep));
        players.put("blink", MediaPlayer.create(getContext(), R.raw.beep));
    }

    @Override
    public void onStop() {
        for (MediaPlayer player : players.values()) {
            player.release();
        }
        players.clear();
        super.onStop();
    }

    @Override
    public void onGesture(final String gestureName, final List<EyeEvent> events) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        MediaPlayer player = players.get(gestureName);
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.start();
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String debug = "";
                switch (gestureName) {
                    case "blink":
                        content.showSquare(true);
                        debug = String.format("%d\n%d\n%d", events.get(0).amplitude,
                                events.get(1).amplitude, events.get(2).amplitude);
                        reset(2000);
                        break;
                    case "fixation":
                        debug = String.format("%d", events.get(0).durationMillis);
                        content.showCircle(true);
                        reset(Config.FIXATION_VISIBILITY_MILLIS);
                        break;
                    case "left":
                        debug = String.format("%d\n%d", events.get(0).durationMillis,
                                events.get(1).amplitude);
                        content.showArrow(EyeEvent.Direction.LEFT, false);
                        reset(Config.GESTURE_VISIBILITY_MILLIS);
                        break;
                    case "right":
                        debug = String.format("%d\n%d", events.get(0).durationMillis,
                                events.get(1).amplitude);
                        content.showArrow(EyeEvent.Direction.RIGHT, false);
                        reset(Config.GESTURE_VISIBILITY_MILLIS);
                        break;
                }
                text.setText(debug);
            }
        });
    }

    private void reset(int delay) {
        if (resetTimer != null) {
            resetTimer.cancel();
        }
        resetTimer = new Timer();
        resetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity == null) {
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        content.clear();
                        text.setText(null);
                    }
                });
            }
        }, delay);
    }
}
