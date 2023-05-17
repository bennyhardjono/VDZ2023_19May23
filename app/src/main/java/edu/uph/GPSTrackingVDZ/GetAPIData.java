package edu.uph.GPSTrackingVDZ;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GetAPIData extends AsyncTask<String, String, String> {
    String url;
    String requestMethod;

    GetAPIData(String url, String requestMethod) {
        this.url = url;
        this.requestMethod = requestMethod;
    }

    protected String doInBackground(String... params) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(this.url);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            connection.setRequestMethod(requestMethod);

            InputStream stream;
            if (connection.getResponseCode() != 200) {
                stream = connection.getErrorStream();
            }
            else {
                stream = connection.getInputStream();
            }

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuffer buffer = new StringBuffer();
            String line = "";

            while ((line = reader.readLine()) != null) {
                buffer.append(line+"\n");
            }
            return buffer.toString();

        } catch (MalformedURLException e) {
            System.out.println("URL ERROR");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IO ERROR");
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
