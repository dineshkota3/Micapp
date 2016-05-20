package com.example.dineshkota.randd;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by dineshkota on 5/9/15.
 */
public class StudentListAdapter extends ArrayAdapter<Student> {
    public StudentListAdapter(Context context, ArrayList<Student> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Student user = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.studentlistitem, parent, false);
        }
        // Lookup view for data population
        TextView studentname = (TextView) convertView.findViewById(R.id.name);
        TextView profip = (TextView) convertView.findViewById(R.id.ip);
        // Populate the data into the template view using the data object
        studentname.setText(user.name);
        profip.setText(user.ip);
        //setting up text shown in the list of students where studentname is the name of the student connected and his ip.
        if(user.active) studentname.setTextColor(Color.BLUE); // this coloring scheme is used to know which student is currently speaking.
        else studentname.setTextColor(Color.BLACK);
        // Return the completed view to render on screen
        return convertView;
    }

}
