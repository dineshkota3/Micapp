package com.example.dineshkota.randd;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class StudentSendActivity extends ActionBarActivity {


    private int SocketServerPort = 6000; // this port is used for creating the socket used for the primary connection with student
    private static final String REQUEST_CONNECT_CLIENT = "request-connect-client"; // Request string made to connect to prof
    private static final String REQUEST_TALK_CLIENT = "request-to-talk"; //Request string made to talk to prof once its connected
    private static final String STOP_CLIENT = "stop-talk"; // request string made to stop talking with prof
    private String profip; // to store the prof ip
    private String student_name; // string to store the students name
    private Button startButton,help; // Button that is used to connect to prof, start talking with prof and stop talking with prof.
    private EditText ip_text; // Field to get the given ip input.
    private EditText name_text; // text field to get given student name input
    private ProgressDialog progressDialog;

    private boolean status = true; // Status is used to run a loop to send packets continously unless student wants to stop.
    private AudioRecord recorder;
    private int sampleRate = 11025; // Number of packets sent per second
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int port=1342; // port is used to send packets
    private int minBufSize; // size of packets , this depends on the sampleRate
    Socket socket = null;
    DatagramSocket socketnew= null;
    //public Long tsLong;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_send);
        setTitle("Connect");
        startButton = (Button) findViewById(R.id.talkbutton);
        ip_text = (EditText) findViewById(R.id.editText);
        name_text = (EditText) findViewById(R.id.nameofstu);
        startButton.setText("Connect");
        minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,sampleRate,channelConfig,audioFormat,minBufSize);



        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  the below thing happends when you click on start button.
                if (startButton.getText().equals("Connect")) {
                    //if the startButton text is Connect then it sends request string to connect with prof.
                    profip = ip_text.getText().toString();
                    student_name = name_text.getText().toString();
                    connectToHost(REQUEST_CONNECT_CLIENT);
                } else if (startButton.getText().equals("Start")) {
                    //if the startButton text is start then it sends request string to start talking with prof by setting status to true
                    status = true;
                    connectToHost(REQUEST_TALK_CLIENT);
                    startButton.setText("Stop");
                    // Log.e("start", String.valueOf(status));
                } else if (startButton.getText().equals("Stop")) {
                    //if the startButton text is Connect then it sends request string to stop talking with prof by setting status to false, thus exiting out of loop.
                    status = false;
                    connectToHost(STOP_CLIENT);
                    startButton.setText("Start");
                    //Log.e("stop", String.valueOf(status));
                    //recorder.release();
                }
            }
        });

    }

    private void connectToHost(String msg) {
        //this function is used to establish connection with the prof.

        if (profip == null) {
            // checks for the availability of prof address and dont send any packet.
          //  Log.e("TAG", "Host Address is null");
            return;
        }

        String ipAddress = getLocalIpAddress();
        JSONObject jsonData = new JSONObject();
        // create a json object and put msg , students name and ip address of student in the data and send it to prof.
        try {
            jsonData.put("request",msg);
            jsonData.put("name", student_name);
            jsonData.put("ipAddress", ipAddress);
        } catch (JSONException e) {
            e.printStackTrace();
           // Log.e("TAG", "can't put request");
            return;
        }
      //  progressDialog = ProgressDialog.show(this,"","Connecting Please wait....",true,false);
        //progressDialog.show();
        new SocketServerTask().execute(jsonData);
    }

    private String getLocalIpAddress() {
        // this function is used to get the ip address of the current machine
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }

    private class SocketServerTask extends AsyncTask<JSONObject, Void, Void> {
        private JSONObject jsonData; // declaring a json object
        private String responseMsg; // used to store the response it got from prof.

        @Override
        protected Void doInBackground(JSONObject... params) {

            DataInputStream dataInputStream = null; // used to store the data coming in from prof
            DataOutputStream dataOutputStream = null; //used to store the data to send to prof
            jsonData = params[0];

            try {
                // Create a new Socket instance and connect to host
              //  Log.i("TAG", profip);
                socket = new  Socket(InetAddress.getByName(profip), SocketServerPort); // creating socket to receive data and sending data
               // Log.e("TAG", String.valueOf(jsonData));


                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream()); // getting data from socket
                dataInputStream = new DataInputStream(socket.getInputStream()); // writing data and sending to socket.

                // transfer JSONObject as String to the server
                dataOutputStream.writeUTF(jsonData.toString()); // wring the data stream to UTF8 characters
               // Log.i("TAG", "waiting for response from host");

                // Thread will wait till server replies
                String response = dataInputStream.readUTF(); //wrting data into string response which we get from prof
                if (response != null && response.equals("Connection Accepted")) {
                    StudentSendActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            // if the prof accepts the connection.
                            startButton.setText("Start");
                        }
                    });
                    responseMsg = "Connected";
                }
                else if (response != null && response.equals("Please connect first")){
                    StudentSendActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            // if there is no connection established or the previous connection is closed and it is needed to establish a new connection
                            startButton.setText("Connect");
                        }
                    });
                    responseMsg = "Connect again";
                }
                else if (response != null && response.equals("Not your turn")) {
                    //if the prof doesnt allow the particular student to talk , this is the message student receives from prof
                    responseMsg = "Please wait for your turn";
                }
                else if(response != null && response.equals("Start Talking")){

                    StudentSendActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // if the prof accepts the student to talk.
                            startButton.setText("Stop");
                        }
                    });

                    try {
                       // Log.d("TAG","Entered Try");
                       // Log.d("TAG","after minbuf");
                        socketnew = new DatagramSocket();
                        // a new datagram socket is created to send packets to prof
                      //  Log.d("VS", "Socket Created");

                        byte[] buffer = new byte[minBufSize]; // buffer is initilized with minbufsize

                      //  Log.d("VS","Buffer created of size " + minBufSize);
                        DatagramPacket packet;

                        final InetAddress destination = InetAddress.getByName(profip); //destination is the address of the prof
                      //  Log.d("VS", profip);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            //this requires an api16 hence it is surrounded in this if statement
                            AcousticEchoCanceler.create(recorder.getAudioSessionId());
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            //this requires an api16 hence it is surrounded in this if statement
                            NoiseSuppressor.create(recorder.getAudioSessionId());
                        }
                        // Log.d("VS", "Recorder initialized");

                        recorder.startRecording(); // recorder started
                       // Log.d("VR", "Start talking");
                        while(status == true) {

                            //reading data from MIC into buffer
                            minBufSize = recorder.read(buffer, 0, buffer.length);
                            //putting buffer in the packet and setting the destiantion as well as port.
                            packet = new DatagramPacket (buffer,buffer.length,destination,port);


                            socketnew.send(packet); // packet has been sent
                        }
                    } catch(UnknownHostException e) {
                        Log.e("VS", "UnknownHostException");
                    }
                }
                else if(response != null && response.equals("You are disconnected")){
                    // this happens when prof is disconnected
                    responseMsg = "Stopped";
                }
                else {
                    // cant connect to prof
                    responseMsg = "Connection failed";
                }

            } catch (IOException e) {
                e.printStackTrace();
                responseMsg = "Connection failed";
            } finally {

                // close socket
                if (socket != null) {
                    try {
                       // Log.i("TAG", "closing the socket");
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // close input stream
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // close output stream
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
           // progressDialog.dismiss();
            Toast.makeText(StudentSendActivity.this, responseMsg, Toast.LENGTH_SHORT).show();
        }
    }

    public void onBackPressed(){
        try {
            socket.close();
            socketnew.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        finish();
        return;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_student_send, menu);
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