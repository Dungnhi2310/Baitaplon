package org.asdtm.goodweather;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.asdtm.goodweather.utils.Constants;
import org.asdtm.goodweather.utils.Utils;

// yêu cầu

public class WeatherRequest
{
    private static final String TAG = "WeatherRequest";

    byte[] getWeatherByte(URL url) throws IOException
    {


        HttpURLConnection connection = (HttpURLConnection) url.openConnection(); // tạo ket noi thong qua url

        try {
            // de doc
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // đầu vào
            InputStream inputStream = connection.getInputStream();
            // mã phản hồi
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];

            while ((bytesRead = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();

            return outputStream.toByteArray();
        } finally {
            connection.disconnect();
        }
    }
    // nhận kq dưới dạng chuỗi
    public String getResultAsString(URL url) throws IOException
    {
        return new String(getWeatherByte(url));
    }

    public String getItems(String lat, String lon, String units, String lang) throws IOException
    {
        return getResultAsString(Utils.getWeatherForecastUrl(Constants.WEATHER_ENDPOINT, lat, lon, units, lang));
    }
}
