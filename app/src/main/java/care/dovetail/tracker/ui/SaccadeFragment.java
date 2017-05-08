package care.dovetail.tracker.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 4/24/17.
 */

public class SaccadeFragment extends Fragment implements EyeEvent.Observer {
    private Settings settings;

    private SaccadeView leftContent;
    private SaccadeView rightContent;

    private Timer resetTimer;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        settings = new Settings(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saccade, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        leftContent = (SaccadeView) view.findViewById(R.id.leftContent);
        rightContent = (SaccadeView) view.findViewById(R.id.rightContent);

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
    public EyeEvent.Criteria getCriteria() {
        return new EyeEvent.AnyCriteria()
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.LEFT, 2000))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.RIGHT, 2000));
    }

    public void onEyeEvent(final EyeEvent event) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (event.type) {
                    case SACCADE:
                        leftContent.show(event.direction, event.amplitude);
                        rightContent.show(event.direction, event.amplitude);
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
