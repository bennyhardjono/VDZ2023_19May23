package edu.uph.GPSTrackingVDZ;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomAdapter extends BaseAdapter {
    private ArrayList<UserReport> listReport;
    private Context context;
    private int layout;

    CustomAdapter(ArrayList<UserReport> listReport, Context context, int layout) {
        this.listReport = listReport;
        this.context = context;
        this.layout = layout;
    }

    @Override
    public int getCount() {
        return listReport.size();
    }

    @Override
    public Object getItem(int i) {
        return listReport.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    private class ViewHolder {
        TextView report_id, user_coordinate, user_speed, user_status, created_at;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ViewHolder viewHolder = new ViewHolder();
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(layout, null, false);
        }

        viewHolder.report_id = convertView.findViewById(R.id.report_id);
        viewHolder.user_coordinate = convertView.findViewById(R.id.user_coordinate);
        viewHolder.user_speed = convertView.findViewById(R.id.user_speed);
        viewHolder.user_status = convertView.findViewById(R.id.user_status);
        viewHolder.created_at = convertView.findViewById(R.id.created_at);

        UserReport report = listReport.get(getCount() - i - 1);
        viewHolder.report_id.setText(String.valueOf(getCount() - i));
        viewHolder.user_coordinate.setText(report.getCoordinate());
        viewHolder.user_speed.setText(report.getSpeed());
        viewHolder.user_status.setText(report.status);
        viewHolder.created_at.setText(report.createdAt);
        return convertView;
    }
}
