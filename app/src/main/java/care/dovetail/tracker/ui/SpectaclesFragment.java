package care.dovetail.tracker.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import care.dovetail.tracker.EyeEvent;

/**
 * Created by abhi on 4/24/17.
 */

public class SpectaclesFragment extends Fragment implements EyeEvent.Observer {

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public EyeEvent.Criteria getCriteria() {
        return new EyeEvent.AnyCriteria()
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.LEFT, 2000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.RIGHT, 2000));
    }

    public void onEyeEvent(final EyeEvent event) {

    }
}
