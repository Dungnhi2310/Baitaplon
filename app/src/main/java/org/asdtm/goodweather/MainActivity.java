package org.asdtm.goodweather;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.asdtm.goodweather.model.CitySearch;
import org.asdtm.goodweather.model.Weather;
import org.asdtm.goodweather.service.CurrentWeatherService;
import org.asdtm.goodweather.utils.AppPreference;
import org.asdtm.goodweather.utils.Constants;
import org.asdtm.goodweather.utils.Utils;
import java.util.Locale;



public class MainActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener {

    private static final String TAG = "MainActivity";

//    private static final long LOCATION_TIMEOUT_IN_MS = 30000L;

    private TextView mIconWeatherView;
    private TextView mTemperatureView;
    private TextView mDescriptionView;
    private TextView mHumidityView;
    private TextView mWindSpeedView;
    private TextView mPressureView;
    private TextView mCloudinessView;
    private TextView mLastUpdateView;
    private TextView mSunriseView;
    private TextView mSunsetView;
    private AppBarLayout mAppBarLayout;

    private ConnectionDetector connectionDetector;
    private Boolean isNetworkAvailable;
    private ProgressDialog mProgressDialog;
    private LocationManager locationManager;
    private SwipeRefreshLayout mSwipeRefresh;
    private Menu mToolbarMenu;
    private BroadcastReceiver mWeatherUpdateReceiver;

    private String mSpeedScale;
    private String mIconWind;
    private String mIconHumidity;
    private String mIconPressure;
    private String mIconCloudiness;
    private String mIconSunrise;
    private String mIconSunset;
    private String mPercentSign;
    private String mPressureMeasurement;

    private SharedPreferences mPrefWeather;
    private SharedPreferences mSharedPreferences;

    public static Weather mWeather;
    public static CitySearch mCitySearch;

    private static final int REQUEST_LOCATION = 0;
    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};


    public MainActivity(ProgressDialog mProgressDialog) {
        this.mProgressDialog = mProgressDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWeather = new Weather();
        mCitySearch = new CitySearch();

        weatherConditionsIcons();
        initializeTextView();
        initializeWeatherReceiver();

        connectionDetector = new ConnectionDetector(MainActivity.this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mPrefWeather = getSharedPreferences(Constants.PREF_WEATHER_NAME, Context.MODE_PRIVATE);
        mSharedPreferences = getSharedPreferences(Constants.APP_SETTINGS_NAME,
                Context.MODE_PRIVATE);
        setTitle(Utils.getCityAndCountry(this));
    }
    @Override
    protected void onPause() {
        super.onPause();
        mAppBarLayout.removeOnOffsetChangedListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWeatherUpdateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mToolbarMenu = menu;
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_refresh:
                if (connectionDetector.isNetworkAvailableAndConnected()) {
                    startService(new Intent(this, CurrentWeatherService.class));
                    setUpdateButtonState(true);
                } else {
                    Toast.makeText(MainActivity.this,
                            R.string.connection_not_found,
                            Toast.LENGTH_SHORT).show();
                    setUpdateButtonState(false);
                }
                return true;

            case R.id.main_menu_search_city:
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivityForResult(intent, PICK_CITY);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }





    private void preLoadWeather() {
        mSpeedScale = Utils.getSpeedScale(this);
        String lastUpdate = Utils.setLastUpdateTime(this,
                AppPreference.getLastUpdateTimeMillis(this));

        String iconId = mPrefWeather.getString(Constants.WEATHER_DATA_ICON, "01d");
        float temperaturePref = mPrefWeather.getFloat(Constants.WEATHER_DATA_TEMPERATURE, 0);
        String description = mPrefWeather.getString(Constants.WEATHER_DATA_DESCRIPTION,
                "Quang mây");
        int humidity = mPrefWeather.getInt(Constants.WEATHER_DATA_HUMIDITY, 0);
        float pressurePref = mPrefWeather.getFloat(Constants.WEATHER_DATA_PRESSURE, 0);
        float windPref = mPrefWeather.getFloat(Constants.WEATHER_DATA_WIND_SPEED, 0);
        int clouds = mPrefWeather.getInt(Constants.WEATHER_DATA_CLOUDS, 0);
        long sunrisePref = mPrefWeather.getLong(Constants.WEATHER_DATA_SUNRISE, -1);
        long sunsetPref = mPrefWeather.getLong(Constants.WEATHER_DATA_SUNSET, -1);

        String temperature = String.format(Locale.getDefault(), "%.0f", temperaturePref);
        String pressure = String.format(Locale.getDefault(), "%.1f", pressurePref);
        String wind = String.format(Locale.getDefault(), "%.1f", windPref);
        String sunrise = Utils.unixTimeToFormatTime(this, sunrisePref);
        String sunset = Utils.unixTimeToFormatTime(this, sunsetPref);

        mIconWeatherView.setText(Utils.getStrIcon(this, iconId));
        mTemperatureView.setText(getString(R.string.temperature_with_degree, temperature));
        mDescriptionView.setText(description);
        mLastUpdateView.setText(getString(R.string.last_update_label, lastUpdate));
        mHumidityView.setText(getString(R.string.humidity_label,
                String.valueOf(humidity),
                mPercentSign));
        mPressureView.setText(getString(R.string.pressure_label,
                pressure,
                mPressureMeasurement));
        mWindSpeedView.setText(getString(R.string.wind_label, wind, mSpeedScale));
        mCloudinessView.setText(getString(R.string.cloudiness_label,
                String.valueOf(clouds),
                mPercentSign));
        mSunriseView.setText(getString(R.string.sunrise_label, sunrise));
        mSunsetView.setText(getString(R.string.sunset_label, sunset));
        setTitle(Utils.getCityAndCountry(this));
    }

    private void initializeTextView() {
        /**
         *Font chữ Roboto
         */
        Typeface weatherFontIcon = Typeface.createFromAsset(this.getAssets(),
                "fonts/weathericons-regular-webfont.ttf");
        Typeface robotoThin = Typeface.createFromAsset(this.getAssets(),
                "fonts/Roboto-Thin.ttf");
        Typeface robotoLight = Typeface.createFromAsset(this.getAssets(),
                "fonts/Roboto-Light.ttf");

        mIconWeatherView = (TextView) findViewById(R.id.main_weather_icon);
        mTemperatureView = (TextView) findViewById(R.id.main_temperature);
        mDescriptionView = (TextView) findViewById(R.id.main_description);
        mPressureView = (TextView) findViewById(R.id.main_pressure);
        mHumidityView = (TextView) findViewById(R.id.main_humidity);
        mWindSpeedView = (TextView) findViewById(R.id.main_wind_speed);
        mCloudinessView = (TextView) findViewById(R.id.main_cloudiness);
        mLastUpdateView = (TextView) findViewById(R.id.main_last_update);
        mSunriseView = (TextView) findViewById(R.id.main_sunrise);
        mSunsetView = (TextView) findViewById(R.id.main_sunset);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.main_app_bar);

        mIconWeatherView.setTypeface(weatherFontIcon);
        mTemperatureView.setTypeface(robotoThin);
        mWindSpeedView.setTypeface(robotoLight);
        mHumidityView.setTypeface(robotoLight);
        mPressureView.setTypeface(robotoLight);
        mCloudinessView.setTypeface(robotoLight);
        mSunriseView.setTypeface(robotoLight);
        mSunsetView.setTypeface(robotoLight);

        /**
         * set icon
         */
        TextView mIconWindView = (TextView) findViewById( R.id.main_wind_icon );
        mIconWindView.setTypeface(weatherFontIcon);
        mIconWindView.setText(mIconWind);
        TextView mIconHumidityView = (TextView) findViewById( R.id.main_humidity_icon );
        mIconHumidityView.setTypeface(weatherFontIcon);
        mIconHumidityView.setText(mIconHumidity);
        TextView mIconPressureView = (TextView) findViewById( R.id.main_pressure_icon );
        mIconPressureView.setTypeface(weatherFontIcon);
        mIconPressureView.setText(mIconPressure);
        TextView mIconCloudinessView = (TextView) findViewById( R.id.main_cloudiness_icon );
        mIconCloudinessView.setTypeface(weatherFontIcon);
        mIconCloudinessView.setText(mIconCloudiness);
        TextView mIconSunriseView = (TextView) findViewById( R.id.main_sunrise_icon );
        mIconSunriseView.setTypeface(weatherFontIcon);
        mIconSunriseView.setText(mIconSunrise);
        TextView mIconSunsetView = (TextView) findViewById( R.id.main_sunset_icon );
        mIconSunsetView.setTypeface(weatherFontIcon);
        mIconSunsetView.setText(mIconSunset);
    }

    private void weatherConditionsIcons() {
        mIconWind = getString(R.string.icon_wind);
        mIconHumidity = getString(R.string.icon_humidity);
        mIconPressure = getString(R.string.icon_barometer);
        mIconCloudiness = getString(R.string.icon_cloudiness);
        mPercentSign = getString(R.string.percent_sign);
        mPressureMeasurement = getString(R.string.pressure_measurement);
        mIconSunrise = getString(R.string.icon_sunrise);
        mIconSunset = getString(R.string.icon_sunset);
    }

    private void setUpdateButtonState(boolean isUpdate) {
        if (mToolbarMenu != null) {
            MenuItem updateItem = mToolbarMenu.findItem(R.id.main_menu_refresh);
            ProgressBar progressUpdate = (ProgressBar) findViewById(R.id.toolbar_progress_bar);
            if (isUpdate) {
                updateItem.setVisible(false);
                progressUpdate.setVisibility(View.VISIBLE);
            } else {
                progressUpdate.setVisibility(View.GONE);
                updateItem.setVisible(true);
            }
        }
    }

    private void initializeWeatherReceiver() {
        mWeatherUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getStringExtra(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT)) {
                    case CurrentWeatherService.ACTION_WEATHER_UPDATE_OK:
                        mSwipeRefresh.setRefreshing(false);
                        setUpdateButtonState(false);
                        break;
                    case CurrentWeatherService.ACTION_WEATHER_UPDATE_FAIL:
                        mSwipeRefresh.setRefreshing(false);
                        setUpdateButtonState(false);
                        Toast.makeText(MainActivity.this,
                                getString(R.string.toast_parse_error),
                                Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        mSwipeRefresh.setEnabled(verticalOffset == 0);
    }

}
