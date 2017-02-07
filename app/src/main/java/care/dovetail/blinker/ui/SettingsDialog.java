package care.dovetail.blinker.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import care.dovetail.blinker.Config;
import care.dovetail.blinker.MainActivity;
import care.dovetail.blinker.R;

/**
 * Created by abhi on 2/6/17.
 */

public class SettingsDialog extends DialogFragment {
    private final static String TAG = "SettingsDialog";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_settings, container);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ToggleButton showChart = (ToggleButton) view.findViewById(R.id.showChart);
        showChart.setChecked(getActivity().getSharedPreferences(getActivity().getPackageName(), 0)
                .getBoolean(Config.SHOW_CHART, true));
        showChart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getActivity().getSharedPreferences(getActivity().getPackageName(), 0)
                        .edit().putBoolean(Config.SHOW_CHART, isChecked).apply();
            }
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        ((MainActivity) getActivity()).startBluetooth();
        getActivity().findViewById(R.id.binocular).setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        super.onDismiss(dialog);
    }
}
