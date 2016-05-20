package com.example.dineshkota.randd;


import android.content.Intent;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class SelectionActivity extends ActionBarActivity {

    Button bt_stu;
    //button to select student
    Button bt_prof;
    //button to select prof
    WifiManager wifi_ip;
    String ip ;
    // used to store the student ip_address
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        wifi_ip= (WifiManager) getSystemService(WIFI_SERVICE);
        ip = Formatter.formatIpAddress(wifi_ip.getConnectionInfo().getIpAddress());//storing ip address of student

        bt_stu = (Button) findViewById(R.id.student);
        bt_prof = (Button) findViewById(R.id.professor);

        bt_stu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //This happens when student button is clicked.
                Intent i = new Intent(SelectionActivity.this, StudentSendActivity.class);
                i.putExtra("Stu_ip", ip);
                startActivity(i);
                //redirected to StudentSendActivity class. This instance will    be opened.
            }
        });
        bt_prof.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // this happens when prof button is clicked
                Intent j = new Intent(SelectionActivity.this, ProfReceiveActivity.class);
                startActivity(j);
                //redirected to ProfReceiveActivity class. This instance will be opened.
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

