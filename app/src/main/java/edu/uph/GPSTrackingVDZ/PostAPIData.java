package edu.uph.GPSTrackingVDZ;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class PostAPIData extends AsyncTask<String, String, Void> {
    String url;
    String data;

    PostAPIData(String url, String data) {
        this.url = url;
        this.data = data;
    }

    protected Void doInBackground(String... params) {
        HttpURLConnection con = null;
        try {
            // https://www.baeldung.com/httpurlconnection-post
            URL url = new URL(this.url);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            con.getOutputStream().write(data.getBytes());
            con.getOutputStream().close();

            Log.d("[POST-RESPONSE]", con.getResponseCode() + ": " + con.getResponseMessage());

            con.connect();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            System.out.println("URL ERROR");
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IO ERROR");
            e.printStackTrace();
        } finally {
            assert con != null;
            con.disconnect();
        }
        return null;
    }
}
