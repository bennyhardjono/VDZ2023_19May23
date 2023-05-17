package edu.uph.GPSTrackingVDZ;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings.Secure;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.*;
import java.text.*;

public class MainActivity extends AppCompatActivity {
    public static final String API_URL = "http://213.190.4.69:3000/";

    // Update in second
    public static final int DEFAULT_UPDATE_INTERVAL = 10;
    public static final int FAST_UPDATE_INTERVAL = 8;

    private static String ANDROID_ID;

    private final static int LOCATION_REQUEST_CODE = 100;
    private final static int BACKGROUND_LOCATION_REQUEST_CODE = 200;
    private static final double earthRadiusKm = 6378.137;

    // in Minutes
    private static final double SELF_REPORT_CD = 5;
    private static final double UPDATE_CD = 5;
    private static final double BACKGROUND_LOCATION_CD = 2;
    // ============

    // Time Control
    private Date lastReport = null;
    private Date lastUpdate = null;
    // ============

    public VDZModel lockedVDZ;

    private TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates,
            tv_address, tv_distance, tv_fetch_info, tv_result;
    View view;
    ListView lv;
    CustomAdapter customAdapter;
    ArrayList<UserReport> userReports;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch sw_locationUpdates, sw_gps, sw_bgs;

//    private LocationManager locationManager;
//    private LocationListener locationListener;

    private LocationRequest locationRequest;
    private LocationCallback locationCallBack;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Geocoder geocoder;

//    private Timer timer;

    private Location previous_location;

    @Override
    protected void onPause() {
        stopLocationUpdates(true);
        super.onPause();
    }

    @Override
    protected void onResume() {
        startLocationUpdates();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        Log.d("DESTROYER", "Destroying the app...");
        stopLocationUpdates(false);
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void askPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ANDROID_ID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        Log.d("[[ANDROID-ID]]", "Your ANDROID-ID is: " + ANDROID_ID);

        setContentView(R.layout.activity_main);
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_address = findViewById(R.id.tv_lbladdress);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_speed = findViewById(R.id.tv_speed);
        tv_updates = findViewById(R.id.tv_updates);
        tv_distance = findViewById(R.id.tv_distance);
        sw_bgs = findViewById(R.id.sw_bgs);
        sw_gps = findViewById(R.id.sw_gps);
        sw_locationUpdates = findViewById(R.id.sw_locationsupdates);
        tv_result = findViewById(R.id.tv_result);
        tv_fetch_info = findViewById(R.id.fetch_info);

        userReports = new ArrayList<>();
        view = LayoutInflater.from(this).inflate(R.layout.user_data_listview, null);
        lv = view.findViewById(R.id.list_view);
        customAdapter = new CustomAdapter(userReports, this, R.layout.user_data_tile);
        lv.setAdapter(customAdapter);

        findViewById(R.id.tv_button).setOnClickListener(tv -> {
            fetchUserData();
        });

//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        locationListener = new LocationListener() {
//            @Override
//            public void onLocationChanged(@NonNull Location location) {
//                updateUIValues(location);
//
//                if (lastUpdate != null && new Date().getTime() - lastUpdate.getTime() >= UPDATE_CD * 60000) {
//                    fetchVDZData();
//                }
//            }
//
//            @Override
//            public void onProviderDisabled(@NonNull String provider) {
//                Log.d("[BG-LOCATION]", "Provider Disabled!");
//                LocationListener.super.onProviderDisabled(provider);
//            }
//        };

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(1000 * FAST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                updateUIValues(locationResult.getLastLocation());

                if (lastUpdate != null && new Date().getTime() - lastUpdate.getTime() >= UPDATE_CD * 60000) {
                    fetchVDZData();
                }
            }
        };

        sw_gps.setOnClickListener(view -> updateGPSStatus());
        sw_locationUpdates.setOnClickListener(view -> {
            if (sw_locationUpdates.isChecked()) startLocationUpdates();
            else stopLocationUpdates(false);
        });

//        sw_bgs.setOnClickListener(v -> {
//            if (sw_bgs.isChecked()) {
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    sw_bgs.setChecked(false);
//                    Log.d("VDZ-PERMISSIONS", "Background Permission not Found!");
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                        ActivityCompat.requestPermissions(
//                                this,
//                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
//                                BACKGROUND_LOCATION_REQUEST_CODE
//                        );
//                    }
//                }
//                else {
//                    startLocationUpdates();
//                }
//            }
//        });

        fetchVDZData();
    }   // end onCreate method

    private void updateGPSStatus() {
        if (sw_locationUpdates.isChecked()) {
            if (sw_gps.isChecked()) {
                // most accurate - use GPS
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                tv_sensor.setText("Using GPS Sensors");
            } else {
                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                tv_sensor.setText("Using Cell Towers + WiFi");
            }
        } else {
            tv_sensor.setText(R.string.no_location);
        }
    }

    private void stopLocationUpdates(boolean isPaused) {
        previous_location = null;

        updateGPSStatus();
//        if (!isPaused) {
//            Log.d("[BG-STUFF]", "removing timer...");
//            timer.cancel();
//            locationManager.removeUpdates(locationListener);
//        }
//        else {
//            if (sw_bgs.isChecked()) {
//                Log.d("[BG-STUFF]", "inside timer creation...");
//                timer = new Timer();
//                timer.scheduleAtFixedRate(new TimerTask() {
//                    @SuppressLint("MissingPermission")
//                    @Override
//                    public void run() {
//                        Log.d("[BG-STUFF]", "timer is running...");
//                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener, Looper.getMainLooper());
//                    }
//                }, 0, 2000);
//            }
//        }
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);

        tv_updates.setText("Location is NOT being tracked");
        tv_lat.setText("Not tracking Latitude");
        tv_lon.setText("Not tracking Longitude");
        tv_speed.setText("Not tracking speed");
        tv_address.setText(R.string.default_tv_address);
        tv_accuracy.setText("Not tracking location");
        tv_altitude.setText("Not tracking altitude");
        tv_sensor.setText(R.string.no_location);
        tv_distance.setText("Not tracking distance");
    }

    private void startLocationUpdates() {
        if (!sw_locationUpdates.isChecked()) {
            return;
        }

        updateGPSStatus();
        tv_updates.setText("Location is being tracked");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                sw_locationUpdates.setChecked(false);

                askPermission();
                return;
            }
        }

//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, (long) (60000 * BACKGROUND_LOCATION_CD), 0, locationListener);
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 0, locationListener, Looper.getMainLooper());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, Looper.getMainLooper());
    }
    // Get known Location 31min vid 31:55

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sw_locationUpdates.setChecked(true);
                startLocationUpdates();
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Please enable location permission on your application settings!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permission Required!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == BACKGROUND_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sw_bgs.setChecked(true);
                Toast.makeText(this, "Background Location Permission Granted!", Toast.LENGTH_SHORT).show();
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Please enable background location permission!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Background Location Permission Required!", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Fetch API DATA
    private void fetchVDZData() {
        Toast.makeText(this, "Fetching API Data...", Toast.LENGTH_SHORT).show();
        BigVDZ.clearInstance();

        List<VDZModel> modelList = new ArrayList<>();
        List<VDZNode> nodeList = new ArrayList<>();
        List<BigVDZ> bigVDZList = new ArrayList<>();

        try {
            Log.d("pre-API", "Group");
            String apiResponse = new GetAPIData(API_URL + "getGroupCoordinate", "GET").execute().get();
            JSONObject obj = new JSONObject(apiResponse);
            JSONArray listObj = obj.getJSONArray("data");
            for (int i = 0; i < listObj.length(); ++i) {
                BigVDZ newModel = new BigVDZ(listObj.getJSONObject(i));

                Log.d("pre-API", "Node from GroupID " + newModel.id);
                String nodeResponse = new GetAPIData(API_URL + "getNodeCoordinateByGroupID?id=" + newModel.id, "GET").execute().get();
                JSONObject nodeObj = new JSONObject(nodeResponse);
                JSONArray nodeListObj = nodeObj.getJSONArray("data");
                for (int j = 0; j < nodeListObj.length(); ++j) {
                    VDZNode nodeModel = new VDZNode(nodeListObj.getJSONObject(j));

                    Log.d("pre-API", "Model from NodeID " + newModel.id);
                    String detailResponse = new GetAPIData(API_URL + "getDetailCoordinateByNodeID?id=" + nodeModel.id, "GET").execute().get();
                    JSONObject detailObj = new JSONObject(detailResponse);
                    JSONArray detailListObj = detailObj.getJSONArray("data");
                    for (int k = 0; k < detailListObj.length(); ++k) {
                        VDZModel detailModel = new VDZModel(detailListObj.getJSONObject(k));

                        modelList.add(detailModel);

                        nodeModel.addChild(detailModel);
                    }

                    nodeList.add(nodeModel);

                    newModel.addChild(nodeModel);
                }

                bigVDZList.add(newModel);

                BigVDZ.addInstance(newModel);
            }

//            Log.d("[MFA]", String.valueOf(BigVDZ.getInstance()));
//            Log.d("[MFA]", "Total Data: " + BigVDZ.getInstance().size());

            @SuppressLint("SimpleDateFormat") SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss");
            tv_fetch_info.setText("Last Data Update: " + ft.format(new Date()));
            Toast.makeText(this, "Fetching API Data Successful", Toast.LENGTH_SHORT).show();

            // TODO: REMOVE THIS AFTER USING REAL DATA!
            // Purpose: to Rearrange the vdz data
            BigVDZ.clearInstance();
            for (VDZModel m : modelList) {
                findClosestParent(m, nodeList).addChild(m);
            }
            for (VDZNode n : nodeList) {
                findClosestParent(n, bigVDZList).addChild(n);
            }
            for (BigVDZ b : bigVDZList) {
                BigVDZ.addInstance(b);
            }
//            Log.d("[MAS]", String.valueOf(BigVDZ.getInstance()));
//            Log.d("[MAS]", "Total Data: " + BigVDZ.getInstance().size());
        } catch (Exception e) {
            Toast.makeText(this, "Error occurred while Fetching API Data", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        lastUpdate = new Date();
    }

    private double calculateDistance(VDZModel model_1, VDZModel model_2) {
        Location location1 = new Location("");
        location1.setLatitude(model_1.getLatitude());
        location1.setLongitude(model_1.getLongitude());

        Location location_2 = new Location("");
        location_2.setLatitude(model_2.getLatitude());
        location_2.setLongitude(model_2.getLongitude());

        return location1.distanceTo(location_2);
    }

    private VDZNode findClosestParent(VDZModel child, List<VDZNode> parents) {
        double closestDistance = Double.MAX_VALUE;
        VDZNode closestParent = null;
        for (VDZNode m : parents) {
            double temp = calculateDistance(child, m);
            if (temp < closestDistance) {
                closestDistance = temp;
                closestParent = m;
            }
        }
        return closestParent;
    }

    private BigVDZ findClosestParent(VDZNode child, List<BigVDZ> parents) {
        double closestDistance = Double.MAX_VALUE;
        BigVDZ closestParent = null;
        for (BigVDZ m : parents) {
            double temp = calculateDistance(child, m);
            if (temp < closestDistance) {
                closestDistance = temp;
                closestParent = m;
            }
        }
        return closestParent;
    }

    private void fetchUserData() {
        Toast.makeText(this, "Fetching User Data...", Toast.LENGTH_SHORT).show();
        try {
            String apiResponse = new GetAPIData(API_URL + "getUserReport?user_id=" + ANDROID_ID, "GET").execute().get();
//            String apiResponse = new GetAPIData(API_URL + "getUserReport?user_id=abcd").execute().get();
            JSONObject obj = new JSONObject(apiResponse);
            if (obj.has("data")) {
                userReports.clear();
                JSONArray userData = obj.getJSONArray("data");
                for (int i = 0; i < userData.length(); ++i) {
                    userReports.add(new UserReport(userData.getJSONObject(i)));
                }

                if (view.getParent() != null) {
                    ((ViewGroup) view.getParent()).removeView(view);
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle("User Report");
                builder.setView(view);
                builder.setPositiveButton("OK", null);
                builder.show();
//                System.out.println("SUCCESS!: " + apiResponse);
            } else {
                Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error occurred while Fetching User Data", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void updateLocationInfo(Location location) {
        if (location.hasAltitude()) {
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        } else {
            tv_altitude.setText("Altitude unavailable");
        }
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        // vid  39
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            tv_address.setText(addresses.get(addresses.size() - 1).getAddressLine(0));
        } catch (Exception e) {
            tv_address.setText(R.string.default_tv_address);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateUIValues(Location location) {
        if (location == null) {
            Toast.makeText(this, "Location not found!", Toast.LENGTH_SHORT).show();
            return;
        } else if (BigVDZ.getInstance().isEmpty()) {
            Toast.makeText(this, "Unable to find VDZ Data!", Toast.LENGTH_SHORT).show();
            return;
        }

        updateLocationInfo(location);

        double speedInKMpH = 0;
        if (previous_location != null) {
            // Calculate the time difference in seconds between the two timestamps
            double timeDifferenceInSeconds = (location.getTime() - previous_location.getTime()) / 1000d;
            // Calculate the distance between the two locations in meters
            float distanceInMeters = location.distanceTo(previous_location);
            // Calculate the speed in meters per second
            speedInKMpH = (distanceInMeters / timeDifferenceInSeconds) * 3.6;
            speedInKMpH = Double.parseDouble(String.format("%.3f", speedInKMpH));
//            Log.d("Location-DEBUG", "Current\t: " + location);
//            Log.d("Location-DEBUG", "Previous\t: " + previous_location);
//            Log.d("Location-DEBUG", "Time Diff\t: " + timeDifferenceInSeconds + ", DIST\t:" + distanceInMeters);
//            Log.d("Location-DEBUG", "AUTO:" + location.getSpeed() + ", MANUAL: " + speedInKMpH);
        }

        tv_speed.setText(speedInKMpH + " km/s");
        previous_location = location;

        List<VDZModel> closest = findClosestVDZ(location);
        if (closest.isEmpty()) return;

        VDZModel closestVDZ = closest.get(closest.size() - 1);
        double distance = calculateDistance(location, closestVDZ);

        String status;
        if (distance <= closestVDZ.radius * 1000) {
            // inside VDZ Circle
            if (closestVDZ != lockedVDZ) {
                if (lockedVDZ != null) {
                    sendUserReport(location, "Leaving " + lockedVDZ.getName(), speedInKMpH);
                }
                lockedVDZ = closestVDZ;
                lastReport = new Date();
                sendUserReport(location, "Entering " + lockedVDZ.getName(), speedInKMpH);
            }

            status = (int) distance + "m inside the " + closestVDZ.radius + "km radius of " + closestVDZ.getName();
        } else {
            if (lockedVDZ != null) {
                lastReport = new Date();
                sendUserReport(location, "Leaving " + lockedVDZ.getName(), speedInKMpH);
                lockedVDZ = null;
            }

            status = ((int) distance - closestVDZ.radius) + "m away from " + closestVDZ.radius + "km circle of " + closestVDZ.getName();
        }
        StringBuilder temp = new StringBuilder("[");
        for (int i = 0; i < closest.size(); ++i) {
            temp.append(closest.get(i).getName());
            if (i != closest.size() - 1) {
                temp.append(", ");
            }
        }
        temp.append("]");

        tv_distance.setText(status + " " + temp);

        String tag = "<<SELF-REPORT>> ";
//        Log.d("[CALC]", "result: " + (new Date().getTime() - lastReport.getTime()));
//        Log.d("[CALC]", "bool:" + (new Date().getTime() - lastReport.getTime() >= reportCooldownInMinutes * 60000));
        if (lastReport != null && new Date().getTime() - lastReport.getTime() >= SELF_REPORT_CD * 60000) {
            Log.d(tag, "Self report has been submitted!");
            Toast.makeText(this, "Sending Self-Report...", Toast.LENGTH_SHORT).show();
            sendUserReport(location, tag + status, speedInKMpH);
            lastReport = new Date();
        }

        @SuppressLint("SimpleDateFormat") SimpleDateFormat ft = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy");
        tv_result.setText("Last Updated: " + ft.format(new Date()));
    }

    @SuppressLint("SimpleDateFormat")
    private void sendUserReport(Location location, String status, double speed) {
        System.out.println("Sending data to the internet!");
        try {
            JSONObject object = new JSONObject();
            object.put("user_generated_id", ANDROID_ID);
            object.put("created_at", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
            object.put("speed", speed);
            object.put("user_lat", String.valueOf(location.getLatitude()));
            object.put("user_long", String.valueOf(location.getLongitude()));
            object.put("status", status);
            String data = object.toString();

            new PostAPIData(API_URL + "addUserReport", data).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNewVDZ(Location location, int radius) {
        System.out.println("Sending data to the internet!");
        String lat = String.valueOf(location.getLatitude());
        String lon = String.valueOf(location.getLongitude());
        try {
            JSONObject object = new JSONObject();
            object.put("name", "VDZ(" + lat + ", " + lon + ")");
            object.put("radius", radius);
            object.put("lat", lat);
            object.put("long", lon);
            String data = object.toString();

            new PostAPIData(API_URL + "addVDZGroup", data).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double calculateDistance(Location location, VDZModel model) {
        Location newLocation = new Location("");
        newLocation.setLatitude(model.getLatitude());
        newLocation.setLongitude(model.getLongitude());

        return location.distanceTo(newLocation);
    }

    private List<VDZModel> findClosestVDZ(Location location) {
        List<VDZModel> toReturn = new ArrayList<>();

        double smallestDistance = Double.MAX_VALUE;
        BigVDZ closest_big_vdz = null;
        for (BigVDZ bigModel : BigVDZ.getInstance()) {
            double distance = calculateDistance(location, bigModel);
//            Log.d("[BIG]", distance + "m away from " + bigModel.getName());
            if (smallestDistance == -1 || distance < smallestDistance) {
                closest_big_vdz = bigModel;
                smallestDistance = distance;
            }
        }
//        System.out.println(closest_big_vdz);
        if (closest_big_vdz == null) return toReturn;
        toReturn.add(closest_big_vdz);

        smallestDistance = Double.MAX_VALUE;
        VDZNode closest_node_vdz = null;
        for (VDZNode nodeModel : closest_big_vdz.getListChild()) {
            double distance = calculateDistance(location, nodeModel);
//            Log.d("[NODE]", distance + "m away from " + nodeModel.getName());
            if (smallestDistance == -1 || distance < smallestDistance) {
                closest_node_vdz = nodeModel;
                smallestDistance = distance;
            }
        }
//        System.out.println(closest_node_vdz);
        if (closest_node_vdz == null) return toReturn;
        toReturn.add(closest_node_vdz);

        smallestDistance = Double.MAX_VALUE;
        VDZModel closestVDZ = null;
        for (VDZModel model : closest_node_vdz.getListChild()) {
            double distance = calculateDistance(location, model);
//            Log.d("[DETAIL]", distance + "m away from " + model.getName());
            if (smallestDistance == -1 || distance < smallestDistance) {
                closestVDZ = model;
                smallestDistance = distance;
            }
        }
//        System.out.println(closestVDZ);
        if (closestVDZ == null) return toReturn;
        toReturn.add(closestVDZ);
        return toReturn;
    }

}

