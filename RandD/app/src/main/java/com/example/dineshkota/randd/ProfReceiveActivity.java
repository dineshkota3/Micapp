package com.example.dineshkota.randd;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

public class ProfReceiveActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {



    private ListView studentlist;
    private int SocketServerPort = 6000; // this port is used to set up connection first
    private static final String REQUEST_CONNECT_CLIENT = "request-connect-client"; //requesting string used by student to connect to prof
    private static final String REQUEST_TALK_CLIENT = "request-to-talk"; //requesting string used by student to talk to prof
    private static final String STOP_CLIENT = "stop-talk"; // request string used by prof to stop talking
    private List<String> clientnames; // list of names of student
    private List<String > clientIPs; // list of ips of student
    private ServerSocket serverSocket; //socket used for establishing connection
    private SocketServerThread serverstart; // thread for connection establishment
    private AudioThread audioThread; // thread for receiving packets and playing on speaker
    private Handler mHandler;
    private ArrayList<Student> arrayOfStudents; //used to store the student class i.e. student name , student ip and his status
    private StudentListAdapter adapter;
    private int selectedStudent;  // position of student who is selected
    private int sampleRate = 11025; // number of packets he receives per second
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private AudioTrack speaker;
    private boolean status = false; // status is used for receiving packets
    private int minBufSize;
    DatagramSocket audioSocket = null; // socket is created to receive packets which are to be played

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prof_receive);
        setTitle("Students List");
        minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        speaker = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,channelConfig,audioFormat,minBufSize,AudioTrack.MODE_STREAM);//intilizing the speaker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            //this requires an api16 hence it is surrounded in this if statement
            AcousticEchoCanceler.create(speaker.getAudioSessionId());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            //this requires an api16 hence it is surrounded in this if statement
            NoiseSuppressor.create(speaker.getAudioSessionId());
        }

        clientIPs = new ArrayList<String>();
        clientnames = new ArrayList<String>();
        serverstart = new SocketServerThread();
        serverstart.start(); // this thread is started to esablish the primary connection i.e. to gather the info about the students name and ip
        audioThread = new AudioThread();
        audioThread.start(); // this thread is started to receive the packets from student who is active
       // Log.i("TAG", getLocalIpAddress());

        studentlist = (ListView) findViewById(R.id.studentlist);
        arrayOfStudents = new ArrayList<Student>();
        adapter = new StudentListAdapter(this, arrayOfStudents);
        studentlist.setAdapter(adapter); // this is used to set the list view of students
        studentlist.setOnItemClickListener((AdapterView.OnItemClickListener) this);
    }

    private String getLocalIpAddress() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
       // Log.e("TAG", "You clicked Item: " + id + " at position:" + position);
        // if a specific student is selected
        arrayOfStudents.get(selectedStudent).active = false;
        arrayOfStudents.get(position).active = true;
        selectedStudent = position;
        adapter.notifyDataSetChanged();
    }

    private class SocketServerThread extends Thread implements Runnable{

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void run() {

            Socket socket = null;
            DataInputStream dataInputStream = null; // datainputstream is the data received from student
            DataOutputStream dataOutputStream = null; // dataoutputstream is data to be sent to student

            try {
               // Log.i("TAG", "Creating server socket.....");
                serverSocket = new  ServerSocket(SocketServerPort); //serversocket is created to establishing the connections

               // Log.i("TAG", "Socket createdP");

                while (true) {
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(socket.getInputStream()); // received input form the student
                    dataOutputStream = new DataOutputStream(socket.getOutputStream()); // data to be sent to student

                    String messageFromClient, messageToClient, request;

                    //If no message sent from client, this code will block the program
                    messageFromClient = dataInputStream.readUTF(); // reading the data from student into string

                    final JSONObject jsondata;
                    jsondata = new JSONObject(messageFromClient); // which contains the request string from student

                    try {
                        request = jsondata.getString("request");

                        if (request.equals(REQUEST_CONNECT_CLIENT)) {
                            //if request string is to connect to prof
                            final String clientIPAddress = jsondata.getString("ipAddress");
                            final String clientname = jsondata.getString("name");

                            // Add client IP to a list and name to the list
                            clientIPs.add(clientIPAddress);
                            clientnames.add(clientname);

                            messageToClient = "Connection Accepted"; // message to be sent to student if connection is accepted

                            ProfReceiveActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    // used for creating the alert box to add the student
                                    AlertDialog.Builder builder = new AlertDialog.Builder(ProfReceiveActivity.this);
                                    builder.setMessage(clientname+" want to talk!")
                                            .setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                }
                                            })
                                            .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    Student student = new Student(clientname, clientIPAddress);
                                                    if(arrayOfStudents.size()==0) {
                                                        selectedStudent = 0;
                                                        student.active = true;
                                                    }
                                                    arrayOfStudents.add(student);
                                                    adapter.notifyDataSetChanged();
                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                    Toast.makeText(ProfReceiveActivity.this,clientname+" "+clientIPAddress , Toast.LENGTH_SHORT).show();
                                }
                            });
                            dataOutputStream.writeUTF(messageToClient); // writing the message to send to student into outputstream
                        }
                        if(request.equals(REQUEST_TALK_CLIENT)){
                            final String clientIPAddress = jsondata.getString("ipAddress");
                            final String clientname = jsondata.getString("name");

                            messageToClient = "Not your turn"; // if the student is not allowed to talk
                            if(arrayOfStudents.size()==0) messageToClient = "Please connect first"; // if there are no students connected , first connect then start talking
                            else if(clientIPAddress.equals(arrayOfStudents.get(selectedStudent).ip) && clientname.equals(arrayOfStudents.get(selectedStudent).name)){
                                if(!status) messageToClient = "Start Talking"; // its his turn and if he not started to talk yet, then we ask student to start talking
                                else messageToClient = "Stoped"; // if its students turn and he is talking we ask him to stop talking
                                status = !status; // toggling the status
                            }
                            dataOutputStream.writeUTF(messageToClient);
                        }
                        if(request.equals(STOP_CLIENT)){
                            //if the student send request to stop
                            status = false;
                            messageToClient = "You are disconnected";
                            dataOutputStream.writeUTF(messageToClient);
                           // Log.d("VR","Client disconnected");
                        }
                        else {
                            // There might be other queries, but as of now nothing.
                            dataOutputStream.flush();
                        }
                    } catch (JSONException e) {
                        Log.e("TAG", "Unable to get request");
                        dataOutputStream.flush();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();

            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                //closing the socket
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // closing the datainputstream
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //closing the dataoutputstream
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    private class AudioThread extends Thread implements Runnable {

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void run() {
            byte[] buffer;

            try {
                audioSocket = new DatagramSocket(1342); // initilization of socket
                Log.d("VR", "Socketnew Created");
                buffer = new byte[minBufSize];
                //minimum buffer size. need to be careful. might cause problems. try setting manually if any problems faced
                speaker.play();
                //starting the player
                while (true) {
                    try {
                        if(status) {
                            Log.d("prof1234","rec data");
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length); // intilizing the size of packet
                            audioSocket.receive(packet); // receiving the packet
                            Log.d("prof1234", " rec to adhadata");

                            buffer = packet.getData(); //getting the data to write in buffer

                            //sending data to the Audiotrack obj i.e. speaker
                            speaker.write(buffer, 0, minBufSize);
                            //writing buffer content to speaker
                        }
                    } catch (IOException e) {
                        Log.e("VR", "IOException");
                    }
                }
            } catch (SocketException e) {
                Log.e("VR", "SocketException");
            }
            finally {
                //closing the audiosocket
                if (audioSocket != null) {
                    audioSocket.close();
                }
            }
        }
    }


    @Override
    public void onBackPressed(){
        //if back button is pressed , closing the socket
        if(serverSocket!=null){
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(audioSocket!=null){
            audioSocket.close();
        }
        finish();
        return;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_prof_receive, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clearall) {
            // if options selected button is clicked we get this option of clearing the student list we received. The whole array is cleared and adapter is identified
            arrayOfStudents.clear();
            adapter.notifyDataSetChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
