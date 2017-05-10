package care.dovetail.tracker.ui;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.Gesture;
import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 4/24/17.
 */

public class GestureFragment extends Fragment implements Gesture.Observer {

    private final Set<Gesture> gestures = new HashSet<>();

    private final Map<String, MediaPlayer> players = new HashMap<>();
    private Settings settings;

    private GestureView leftContent;
    private GestureView rightContent;

    private Timer resetTimer;

    public GestureFragment() {
        gestures.add(new Gesture("left")
                .add(new EyeEvent.Criterion(EyeEvent.Type.FIXATION, 1000L))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.LEFT, 1500))
                .addObserver(this));
        gestures.add(new Gesture("right")
                .add(new EyeEvent.Criterion(EyeEvent.Type.FIXATION, 1000L))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.RIGHT, 1500))
                .addObserver(this));
        gestures.add(new Gesture("fixation")
                .add(new EyeEvent.Criterion(EyeEvent.Type.FIXATION, 1000L))
                .addObserver(this));
        gestures.add(new Gesture("blink")
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.UP, 3000))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.DOWN, 3000))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.UP, 3000))
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
        return inflater.inflate(R.layout.fragment_gesture, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        leftContent = (GestureView) view.findViewById(R.id.leftContent);
        rightContent = (GestureView) view.findViewById(R.id.rightContent);

        if (settings.isDayDream()) {
            view.findViewById(R.id.left).setPadding(
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_left),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
            view.findViewById(R.id.right).setPadding(
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_right),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
        }
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
    public Set<Gesture> getGestures() {
        return gestures;
    }

    public void onGesture(final Gesture gesture) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        MediaPlayer player = players.get(gesture.name);
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.start();
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (gesture.name) {
                    case "blink":
                        leftContent.showSquare(true);
                        rightContent.showSquare(true);
                        reset(Config.FIXATION_VISIBILITY_MILLIS);
                        break;
                    case "fixation":
                        leftContent.showCircle(true);
                        rightContent.showCircle(true);
                        reset(Config.FIXATION_VISIBILITY_MILLIS);
                        break;
                    case "left":
                        leftContent.showArrow(EyeEvent.Direction.LEFT, false);
                        rightContent.showArrow(EyeEvent.Direction.LEFT, false);
                        reset(Config.GESTURE_VISIBILITY_MILLIS);
                        break;
                    case "right":
                        leftContent.showArrow(EyeEvent.Direction.RIGHT, false);
                        rightContent.showArrow(EyeEvent.Direction.RIGHT, false);
                        reset(Config.GESTURE_VISIBILITY_MILLIS);
                        break;
                }
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
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        leftContent.clear();
                        rightContent.clear();
                    }
                });
            }
        }, delay);
    }
}
