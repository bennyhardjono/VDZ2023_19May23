package edu.uph.GPSTrackingVDZ;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VDZModel {
    protected int id;
    protected String name;
    protected double latitude;
    protected double longitude;
    protected double radius;

    public VDZModel(int id, String name, double latitude, double longitude, double radius) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public VDZModel(JSONObject obj) throws JSONException {
        this(
                obj.getInt("id"),
                obj.getString("name"),
                obj.getDouble("lat"),
                obj.getDouble("long"),
                obj.getDouble("radius") * 1000
        );
    }

    public String getName() {
        return "VDZ " + name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "\n\t\t\t- " + getName();
//        return "{" + getName() + ", lat=" + latitude + ", long=" + longitude + ", radius=" + radius + '}';
    }
}

class VDZNode extends VDZModel {
    private ArrayList<VDZModel> listChild;

    public VDZNode(JSONObject obj) throws JSONException {
        super(obj);
        listChild = new ArrayList<>();
    }

    public void addChild(VDZModel child) {
        listChild.add(child);
    }

    public ArrayList<VDZModel> getListChild() {
        return new ArrayList<>(listChild);
    }

    @Override
    public String getName() {
        return "VDZ Node " + name;
    }

    @Override
    public String toString() {
        return "\n\t\t ->" + getName() + listChild;
//        return "{" + getName() + ", lat=" + latitude + ", lon=" + longitude + ", radius=" + radius + ", " + listChild.size() + "child }";
    }
}

class BigVDZ extends VDZModel {
    private static ArrayList<BigVDZ> instance = new ArrayList<>();

    public static void addInstance(BigVDZ newModel) {
        instance.add(newModel);
    }

    public static ArrayList<BigVDZ> getInstance() {
        return new ArrayList<>(instance);
    }

    public static void clearInstance() {
        instance.clear();
    }

    private ArrayList<VDZNode> listChild;

    public BigVDZ(JSONObject obj) throws JSONException {
        super(obj);
        listChild = new ArrayList<>();
    }

    public void addChild(VDZNode child) {
        listChild.add(child);
    }

    public ArrayList<VDZNode> getListChild() {
        return new ArrayList<>(listChild);
    }

    @Override
    public String getName() {
        return "Big VDZ " + name;
    }

    @Override
    public String toString() {
        return "\n\t >>" + getName() + listChild;
//        return "{" + getName() + ", lat=" + latitude + ", lon=" + longitude + ", radius=" + radius + ", " + listChild.size() + "child }";
    }
}
