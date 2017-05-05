package care.dovetail.tracker.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 4/24/17.
 */

public class FruitFragment extends Fragment implements EyeEvent.Observer {
    private Settings settings;

    private boolean animationRunning = false;

    private ImageView leftFruit;
    private ImageView rightFruit;

    private Timer gazeResetTimer;

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
        resetGaze();
    }

    public void onEyeEvent(final EyeEvent event) {
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
                if (event.type == EyeEvent.Type.SACCADE) {
                    switch (event.direction) {
                        case LEFT:
                            leftFruit.setImageResource(R.drawable.apple_left);
                            rightFruit.setImageResource(R.drawable.apple_left);
                            resetImage(Config.GESTURE_VISIBILITY_MILLIS);
                            resetGaze();
                            animationRunning = true;
                            break;
                        case RIGHT:
                            leftFruit.setImageResource(R.drawable.apple_right);
                            rightFruit.setImageResource(R.drawable.apple_right);
                            resetImage(Config.GESTURE_VISIBILITY_MILLIS);
                            resetGaze();
                            animationRunning = true;
                            break;
                    }
                } else if (event.type == EyeEvent.Type.LARGE_BLINK) {
                    leftFruit.setImageResource(R.drawable.apple_hole);
                    rightFruit.setImageResource(R.drawable.apple_hole);
                    resetImage(Config.GAZE_VISIBILITY_MILLIS);
                    resetGaze();
                    animationRunning = true;
//                } else if (event.type == EyeEvent.Type.GAZE) {
//                    switch (event.direction) {
//                        case LEFT:
//                            resetGaze();
//                            setGaze(new int[] {R.id.leftLeftKnife, R.id.leftRightKnife});
//                            resetGaze(Config.GAZE_VISIBILITY_MILLIS);
//                            break;
//                        case RIGHT:
//                            resetGaze();
//                            setGaze(new int[] {R.id.rightLeftKnife, R.id.rightRightKnife});
//                            resetGaze(Config.GAZE_VISIBILITY_MILLIS);
//                            break;
//                    }
                }
            }
        });
    }

    private void resetImage(int delay) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
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

    private void setGaze(int knives[]) {
        for (int id : knives) {
            getView().findViewById(id).setVisibility(View.VISIBLE);
        }
    }

    private void resetGaze() {
        int knives[] = new int[] {R.id.leftLeftKnife, R.id.leftRightKnife, R.id.rightLeftKnife,
                R.id.rightRightKnife};
        for (int id : knives) {
            getView().findViewById(id).setVisibility(View.INVISIBLE);
        }
    }

    private void resetGaze(int delay) {
        if (gazeResetTimer != null) {
            gazeResetTimer.cancel();
        }
        gazeResetTimer = new Timer();
        gazeResetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resetGaze();
                    }
                });
            }
        }, delay);
    }
}
