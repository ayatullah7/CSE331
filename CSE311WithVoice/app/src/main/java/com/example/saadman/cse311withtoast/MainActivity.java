package com.example.saadman.cse311withtoast;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.BluetoothAdapter;

import android.widget.Button;
import android.widget.ImageButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private ImageButton btnSpeak;
    BluetoothSocket mbluetoothSocket;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice targetDevice;
    CommunicationThread communicationThread;
    BluetoothConnectThread bluetoothConnectThread;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        btnSpeak = (ImageButton)findViewById(R.id.btnVoice);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }


        btnSpeak.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                promptSpeechInput();

            }

        });


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.ConnectToBluetooth) {

            Set<BluetoothDevice> deviceArray = mBluetoothAdapter.getBondedDevices();
            for(BluetoothDevice device: deviceArray){
                if(device.getName().contains("HC-05")){
                    targetDevice = device;
                    break;
                }
            }
            bluetoothConnectThread = new BluetoothConnectThread(targetDevice);
            bluetoothConnectThread.start();


        }

        return super.onOptionsItemSelected(item);
    }



    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_US");

        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    communicationThread.write(result.get(0).getBytes());

                    Toast.makeText(getApplicationContext(), result.get(0), Toast.LENGTH_LONG).show();

                }
                break;
            }

        }
    }


    private  class BluetoothConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;
        public BluetoothConnectThread(BluetoothDevice bluetoothDevice)  {
            this.bluetoothDevice = bluetoothDevice;
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(myUUID);

            }catch(IOException e){

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Device Not Found. Please Try Again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        public void run(){
            try {
                bluetoothSocket.connect();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(getApplicationContext(), "Connected To Device", Toast.LENGTH_SHORT).show();

                    }
                });

            } catch (IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(getApplicationContext(), "Device Not Found. Please Try Again.", Toast.LENGTH_SHORT).show();

                    }
                });

            }
            communicationThread = new CommunicationThread(bluetoothSocket);

        }

        public void cancel(){
            try{
                bluetoothSocket.close();
            }catch(IOException e){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Something went wrong turning off the ConnectThread", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

    }

    private class CommunicationThread{
        BluetoothSocket connectedBluetoothSocket;
        InputStream connectedInputStream;
        OutputStream mmOutStream;
        PrintWriter out;
        CommunicationThread(BluetoothSocket bluetoothSocket) {
            connectedBluetoothSocket = bluetoothSocket;

            try {
                mmOutStream = connectedBluetoothSocket.getOutputStream();

            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "mmOutStream Failed", Toast.LENGTH_LONG).show();
            }

        }

        public void write(byte[] bytes) {
            try {

                mmOutStream.write(bytes);
            }
             catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Communication Failed", Toast.LENGTH_LONG).show();
            }
        }

    }


    protected void onDestroy() {
        super.onDestroy();
        bluetoothConnectThread.cancel();

    }


}
