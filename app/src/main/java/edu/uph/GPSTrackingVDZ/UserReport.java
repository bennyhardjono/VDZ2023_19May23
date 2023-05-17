package edu.uph.GPSTrackingVDZ;

import android.annotation.SuppressLint;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UserReport {
    public int id;
    public double latitude;
    public double longitude;
    public double speed;
    public String status;
    public String createdAt;

    public String getCoordinate() {
        return "(" + latitude + ", " + longitude + ")";
    }

    public String getSpeed() {
        return String.valueOf(speed);
    }

    public String getID() {
        return String.valueOf(id);
    }

    @SuppressLint("SimpleDateFormat")
    UserReport(JSONObject obj) throws JSONException {
        if (obj.has("id")) id = obj.getInt("id");
        else id = -1;

        if (obj.has("user_lat")) latitude = obj.getDouble("user_lat");
        else latitude = 0;

        if (obj.has("user_long")) longitude = obj.getDouble("user_long");
        else longitude = 0;

        if (obj.has("speed")) speed = obj.getDouble("speed");
        else speed = 0;

        if (obj.has("status")) status = obj.getString("status");
        else status = "Null";

        String rawDate;
        if (obj.has("created_at")) rawDate = obj.getString("created_at");
        else rawDate = "0000-00-00T00:00:00.00Z";

        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(rawDate);
            assert date != null;
            createdAt = new SimpleDateFormat("EEEE, dd MMM yyyy HH:mm:ss").format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "UserReport{" +
                "id=" + id +
                ", coordinate=" + getCoordinate() +
                ", speed=" + speed +
                ", status='" + status + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
