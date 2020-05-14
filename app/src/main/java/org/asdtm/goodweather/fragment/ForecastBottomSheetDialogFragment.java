package org.asdtm.goodweather.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import org.asdtm.goodweather.model.WeatherForecast;

public class ForecastBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private WeatherForecast weather;

    public ForecastBottomSheetDialogFragment newInstance(WeatherForecast weather) {
        ForecastBottomSheetDialogFragment fragment = new ForecastBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("weatherForecast", weather);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weather = (WeatherForecast) getArguments().getSerializable("weatherForecast");
    }


}
