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
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

public class FruitFragment extends Fragment implements Gesture.Observer {

    private final int FRUIT[] = new int[]
            {R.drawable.apple, R.drawable.orange, R.drawable.strawberry};
    private final int RIGHT[] = new int[]
            {R.drawable.apple_right, R.drawable.orange_right, R.drawable.strawberry_right};
    private final int LEFT[] = new int[]
            {R.drawable.apple_left, R.drawable.orange_left, R.drawable.strawberry_left};
    private final int EXPLODED[] = new int[]
            {R.drawable.apple_exploded, R.drawable.orange_exploded, R.drawable.strawberry_exploded};

    private int fruitIndex = 0;
    private long lastFruitChangeTimeMillis = 0;

    private EyeEvent.Source eyeEventSource;

    private final Map<String, MediaPlayer> players = new HashMap<>();
    private Settings settings;

    private final Set<Gesture> directions = new HashSet<>();
    private int blinkCount = 0;
    private final Set<Integer> blinkAmplitudes = new HashSet<>();

    private boolean animationRunning = false;

    private ImageView leftFruit;
    private ImageView rightFruit;
    private TextView leftDebug;
    private TextView rightDebug;

    private Timer fixationResetTimer;

    private int latestColumn = 1;

    @Override
    public void setEyeEventSource(EyeEvent.Source eyeEventSource) {
        this.eyeEventSource = eyeEventSource;
        eyeEventSource.add(new Gesture("blink")
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 2000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 2000))
                .addObserver(this));
        eyeEventSource.add(new Gesture("multiblink")
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 2000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, 2000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, 4000))
                .addObserver(this));
        eyeEventSource.add(new Gesture("fixation")
                .add(EyeEvent.Criterion.fixation(1000))
                .addObserver(this));
        eyeEventSource.add(new Gesture("explode")
                .add(EyeEvent.Criterion.fixation(5000, 5500))
                .addObserver(this));
//        eyeEventSource.add(new Gesture("position")
//                .add(new EyeEvent.Criterion(EyeEvent.Type.POSITION))
//                .add(this));
        replaceDirections(1500);
    }

    private void replaceDirections(int amplitude) {
        amplitude = Math.min(Math.max(amplitude, 800), 2000);
        if (directions.size() > 0) { // Skip the first one that is not called from UI thread.
            leftDebug.setText(Integer.toString(amplitude));
            rightDebug.setText(Integer.toString(amplitude));
        }
        for (Gesture direction : directions) {
            eyeEventSource.remove(direction);
        }
        directions.clear();
        directions.add(new Gesture("left")
                .add(EyeEvent.Criterion.fixation(1000, 5000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.LEFT, amplitude))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.RIGHT, amplitude))
                .addObserver(this));
        directions.add(new Gesture("right")
                .add(EyeEvent.Criterion.fixation(1000, 5000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.RIGHT, amplitude))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.LEFT, amplitude))
                .addObserver(this));
        for (Gesture direction : directions) {
            eyeEventSource.add(direction);
        }
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

        leftFruit.setImageResource(FRUIT[fruitIndex]);
        rightFruit.setImageResource(FRUIT[fruitIndex]);

        leftDebug = (TextView) view.findViewById(R.id.leftText);
        rightDebug = (TextView) view.findViewById(R.id.rightText);

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
        players.put("fixation", MediaPlayer.create(getContext(), R.raw.jump));
        players.put("blink", MediaPlayer.create(getContext(), R.raw.ping));
        players.put("explode", MediaPlayer.create(getContext(), R.raw.explode));
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
                        leftFruit.setImageResource(LEFT[fruitIndex]);
                        rightFruit.setImageResource(LEFT[fruitIndex]);
                        resetImage(Config.GESTURE_VISIBILITY_MILLIS);
                        resetFixation();
                        animationRunning = true;
                        break;
                    case "right":
                        play(gestureName);
                        leftFruit.setImageResource(RIGHT[fruitIndex]);
                        rightFruit.setImageResource(RIGHT[fruitIndex]);
                        resetImage(Config.GESTURE_VISIBILITY_MILLIS);
                        resetFixation();
                        animationRunning = true;
                        break;
                    case "blink":
                        play(gestureName);
                        resetFixation();
                        maybeUpdateDirections(events);
                        break;
                    case "multiblink":
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastFruitChangeTimeMillis < 2000) {
                            // Ignore quick multiblink events.
                            return;
                        }
                        fruitIndex = ++fruitIndex == FRUIT.length ? 0 : fruitIndex;
                        resetImage(0);
                        resetFixation();
                        lastFruitChangeTimeMillis = currentTime;
                        break;
                    case "fixation":
                        if (getView().findViewById(R.id.leftLeftKnife).getAlpha() < 1.0f) {
                            // Play sound only one time when highlighting the knife
                            play(gestureName);
                        }
                        resetFixation();
                        setFixation(new int[]{R.id.leftLeftKnife, R.id.rightLeftKnife});
                        setFixation(new int[]{R.id.leftRightKnife, R.id.rightRightKnife});
                        scheduleResetFixation(Config.FIXATION_VISIBILITY_MILLIS);
                        break;
                    case "explode":
                        play(gestureName);
                        leftFruit.setImageResource(EXPLODED[fruitIndex]);
                        rightFruit.setImageResource(EXPLODED[fruitIndex]);
                        resetImage(Config.GESTURE_VISIBILITY_MILLIS);
                        resetFixation();
                        animationRunning = true;
                        break;
                    case "position":
                        latestColumn = events.get(0).column;
                        break;
                }
            }
        });
    }

    private void maybeUpdateDirections(List<EyeEvent> events) {
        blinkCount++;
        if (blinkCount % 10 == 0 || blinkAmplitudes.size() > 0) {
            if (blinkAmplitudes.size() < 9) {
                blinkAmplitudes.add(events.get(0).amplitude);
                blinkAmplitudes.add(events.get(1).amplitude / 2);
                blinkAmplitudes.add(events.get(2).amplitude);
            } else {
                int sum = 0;
                for (Integer amplitude : blinkAmplitudes) {
                    sum += amplitude;
                }
                replaceDirections((sum / blinkAmplitudes.size()) / 3);
                blinkAmplitudes.clear();
            }
        }
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
                        leftFruit.setImageResource(FRUIT[fruitIndex]);
                        rightFruit.setImageResource(FRUIT[fruitIndex]);
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

    private void scheduleResetFixation(int delay) {
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
