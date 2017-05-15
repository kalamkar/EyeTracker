package care.dovetail.tracker.ui;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class FruitFragment extends Fragment implements Gesture.Observer {

    private EyeEvent.Source eyeEventSource;

    private final Map<String, MediaPlayer> players = new HashMap<>();
    private Settings settings;

    private boolean animationRunning = false;

    private ImageView leftFruit;
    private ImageView rightFruit;

    private Timer fixationResetTimer;

    private int latestColumn = 1;

    @Override
    public void setEyeEventSource(EyeEvent.Source eyeEventSource) {
        this.eyeEventSource = eyeEventSource;
        eyeEventSource.addObserver(new Gesture("blink")
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 2000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 2000))
                .addObserver(this));
        eyeEventSource.addObserver(new Gesture("fixation")
                .add(EyeEvent.Criterion.fixation(1000))
                .addObserver(this));
//        eyeEventSource.addObserver(new Gesture("position")
//                .add(new EyeEvent.Criterion(EyeEvent.Type.POSITION))
//                .addObserver(this));
        // TODO(abhi): Move adding following gestures post first blink.
        addDirections(1500);
    }

    private void addDirections(int amplitude) {
        eyeEventSource.addObserver(new Gesture("left")
                .add(EyeEvent.Criterion.fixation(1000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.LEFT, amplitude))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.RIGHT, amplitude))
                .addObserver(this));
        eyeEventSource.addObserver(new Gesture("right")
                .add(EyeEvent.Criterion.fixation(1000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.RIGHT, amplitude))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.LEFT, amplitude))
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
        return inflater.inflate(R.layout.fragment_fruit, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        leftFruit = (ImageView) view.findViewById(R.id.leftImage);
        rightFruit = (ImageView) view.findViewById(R.id.rightImage);

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
        resetFixation();
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
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (animationRunning) {
                    return;
                }
                switch (gestureName) {
                    case "left":
                        play(gestureName);
                        leftFruit.setImageResource(R.drawable.apple_left);
                        rightFruit.setImageResource(R.drawable.apple_left);
                        resetImage(Config.GESTURE_VISIBILITY_MILLIS);
                        resetFixation();
                        animationRunning = true;
                        break;
                    case "right":
                        play(gestureName);
                        leftFruit.setImageResource(R.drawable.apple_right);
                        rightFruit.setImageResource(R.drawable.apple_right);
                        resetImage(Config.GESTURE_VISIBILITY_MILLIS);
                        resetFixation();
                        animationRunning = true;
                        break;
                    case "blink":
                        play(gestureName);
                        leftFruit.setImageResource(R.drawable.apple_hole);
                        rightFruit.setImageResource(R.drawable.apple_hole);
                        resetImage(Config.FIXATION_VISIBILITY_MILLIS);
                        resetFixation();
                        animationRunning = true;
                        break;
                    case "fixation":
                        resetFixation();
//                        if (latestColumn == 0) {
                            play(gestureName);
                            setFixation(new int[]{R.id.leftLeftKnife, R.id.rightLeftKnife});
//                        } else  if (latestColumn == 2) {
//                            play(gestureName);
                            setFixation(new int[]{R.id.leftRightKnife, R.id.rightRightKnife});
//                        }
                        resetFixation(Config.FIXATION_VISIBILITY_MILLIS);
                        break;
                    case "position":
                        latestColumn = events.get(0).column;
                        break;
                }
            }
        });
    }

    private void resetImage(int delay) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity == null) {
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        leftFruit.setImageResource(R.drawable.apple);
                        rightFruit.setImageResource(R.drawable.apple);
                        animationRunning = false;
                    }
                });
            }
        }, delay);
    }

    private void setFixation(int knives[]) {
        for (int id : knives) {
            getView().findViewById(id).setAlpha(1.0f);
        }
    }

    private void resetFixation() {
        int knives[] = new int[] {R.id.leftLeftKnife, R.id.leftRightKnife, R.id.rightLeftKnife,
                R.id.rightRightKnife};
        for (int id : knives) {
            getView().findViewById(id).setAlpha(0.5f);
        }
    }

    private void resetFixation(int delay) {
        if (fixationResetTimer != null) {
            fixationResetTimer.cancel();
        }
        fixationResetTimer = new Timer();
        fixationResetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity == null) {
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resetFixation();
                    }
                });
            }
        }, delay);
    }

    private void play(String name) {
        MediaPlayer player = players.get(name);
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.start();
        }
    }
}
