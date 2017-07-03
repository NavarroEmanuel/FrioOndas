package com.frioondas.frioondas;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class btControl extends Activity {

    private static final String TAG = "bluetooth2";

    Button btnOn, btnOff, btnDis, btnFast1, btnFast2, btnFast3, btnFast4;
    TextView txtArduino, txtArduino2, txtEntrada;
    EditText entrada;
    Handler h;

    //GPS
    private Button buttonGetLocation;
    private LocationManager locManager;
    private LocationListener locListener;
    private Location mobileLocation;

    //Proximidad
    SensorManager sm;
    Sensor proxSensor;
    private ProximityDetector mProximityDetector;

    //Shake
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    //Bluethoot
    final int RECIEVE_MESSAGE = 1;		// Status  for Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();

    public int modo = 1;

    private ConnectedThread mConnectedThread;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)
    //private static String address = "20:17:02:27:17:20";
    private String address;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        setContentView(R.layout.activity_bt_control);

        btnOn = (Button) findViewById(R.id.button2);					// button LED ON
        btnOff = (Button) findViewById(R.id.button3);				// button LED OFF
        btnDis = (Button) findViewById(R.id.button4);
        btnFast1 = (Button) findViewById(R.id.button5);
        btnFast2 = (Button) findViewById(R.id.button6);
        btnFast3 = (Button) findViewById(R.id.button7);
        btnFast4 = (Button) findViewById(R.id.button8);
        txtArduino = (TextView) findViewById(R.id.textView9);		// for display the received data from the Arduino
        txtArduino2 = (TextView) findViewById(R.id.textView6);
        entrada = (EditText) findViewById(R.id.editText);
        txtEntrada = (TextView) findViewById(R.id.textView7);

        buttonGetLocation = (Button) findViewById(R.id.button9);
        buttonGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localizacion();
            }
        });


        //Spinner de seleccion de entrada
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.opciones_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        //Creo el listener del selector para cambiar el textView
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView Texto = (TextView)findViewById(R.id.textView4);
                switch (position) {
                    case 0:
                        Texto.setText("Tiempo para enfriar");
                        txtEntrada.setText("Ingrese el tiempo en segundos");
                        modo = 1;
                        break;
                    case 1:
                        Texto.setText("Temperatura deseada");
                        txtEntrada.setText("Ingrese la temperatura en ºC");
                        modo = 2;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

                // sometimes you need nothing here
            }
        });

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:													// if receive massage
                        //byte[] readBuf = (byte[]) msg.obj;
                        //String strIncom = new String(readBuf, 0, msg.arg1);					// create string from bytes array
                        String strIncom = (String) msg.obj;
                        sb.append(strIncom);												// append string
                        int endOfLineIndex = sb.indexOf("\r\n");							// determine the end-of-line
                        if (endOfLineIndex > 0) { 											// if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);				// extract string
                            sb.delete(0, sb.length());										// and clear
                            //txtArduino.setText("Data from Arduino: " + sbprint);
                            String aux[];
                            aux = sbprint.split(";");
                            if(aux.length == 3) {


                                txtArduino.setText(aux[0]); 	        // update TextView
                                //txtArduino.setText("Data from Arduino: " + aux[0]); 	        // update TextView
                                if(modo == 1) {
                                    int time;
                                    try {
                                        time = Integer.parseInt(aux[1]);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                        time = 0;
                                    }
                                    Integer minutes = time/60;
                                    Integer seconds = time%60;
                                    if(seconds >= 10) {
                                        txtArduino2.setText(minutes.toString() + ":" + seconds.toString());
                                    } else {
                                        if(aux[1].equals("-100")) {
                                            txtArduino2.setText("Finalizado");
                                        } else {
                                            if(aux[1].equals("-404")) {
                                                txtArduino2.setText("-");
                                            } else {
                                                txtArduino2.setText(minutes.toString() + ":0" + seconds.toString());
                                            }
                                        }
                                    }
                                    //txtArduino2.setText(aux[1]);
                                } else {
                                    if(aux[2].equals("-100")) {
                                        txtArduino2.setText("Finalizado");
                                    } else {
                                        if(aux[2].equals("-404")) {
                                            txtArduino2.setText("-");
                                        } else {
                                            txtArduino2.setText(aux[2]);
                                        }
                                    }
                                }
                            }
                            btnOff.setEnabled(true);
                            btnOn.setEnabled(true);
                            btnFast1.setEnabled(true);
                            btnFast2.setEnabled(true);
                            btnFast3.setEnabled(true);
                            btnFast4.setEnabled(true);
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            };
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();		// get Bluetooth adapter
        checkBTState();

        btnOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                btnOn.setEnabled(false);
                //mConnectedThread.write("20;-404\n");	// Send "1" via Bluetooth
                String aux;
                Integer valor;
                aux = entrada.getText().toString();
                try {
                    valor = Integer.parseInt(aux);
                } catch (NumberFormatException e) {
                    valor = 0;
                }

                if(modo == 1) {
                    mConnectedThread.write(valor.toString() + ";-404\n");
                } else {
                    mConnectedThread.write("-404;" + valor.toString() + "\n");
                }
                //Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnOff.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                btnOff.setEnabled(false);
                mConnectedThread.write("0;-404\n");	// Send "0" via Bluetooth
                //Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnFast1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                btnFast1.setEnabled(false);
                mConnectedThread.write("+60;-404\n");	// Send "+1 min"
            }
        });

        btnFast2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                btnFast2.setEnabled(false);
                mConnectedThread.write("+300;-404\n");	// Send "+5 min"
            }
        });

        btnFast3.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                btnFast3.setEnabled(false);
                mConnectedThread.write("-404;+1\n");	// Send "- 1 Celsious Degree"
            }
        });

        btnFast4.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                btnFast4.setEnabled(false);
                mConnectedThread.write("-404;+5\n");	// Send "- 5 Celsious Degree"
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });

        //Sensor de proximidad
        sm         = (SensorManager) getSystemService(SENSOR_SERVICE);
        proxSensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        mProximityDetector = new ProximityDetector();
        mProximityDetector.setOnProximityListener(new ProximityDetector.OnProximityListener() {

            @Override
            public void onProximity(float count) {
                mConnectedThread.write("0;-404\n");	// Send apagar
            }
        });



        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
				/*
				 * The following method, "handleShakeEvent(count):" is a stub //
				 * method you would use to setup whatever you want done once the
				 * device has been shook.
				 */
                //handleShakeEvent(count);

                mConnectedThread.write("+30;-404\n");	// Send +30 seg
            }
        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
            return (BluetoothSocket) m.invoke(device, MY_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();

        //Proximidad
        sm.registerListener(mProximityDetector, proxSensor,	SensorManager.SENSOR_DELAY_UI);

        //Shake
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

    /*try {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }*/

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "....Connection ok...");

        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        //Proximidad
        sm.unregisterListener(mProximityDetector);

        //Shake
        mSensorManager.unregisterListener(mShakeDetector);

        Log.d(TAG, "...In onPause()...");

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }




    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() { //hilo background
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            //aca hay que transfomar los bytes en texto

            while (true) {


                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);		// Get number of bytes and message in "buffer"
                    String readMessage = new String(buffer, 0, bytes);
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, readMessage).sendToTarget();		// Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }

    public void localizacion() {
        AlertDialog.Builder alert = new AlertDialog.Builder(btControl.this);
        alert.setTitle("Posicion actual");
        // disallow cancel of AlertDialog on click of back button and outside touch
        alert.setCancelable(false);

        getCurrentLocation(); // gets the current location and update mobileLocation variable

        if (mobileLocation != null) {
            locManager.removeUpdates(locListener); // This needs to stop getting the location data and save the battery power.

            String londitude = "Londitude: " + mobileLocation.getLongitude();
            String latitude = "Latitude: " + mobileLocation.getLatitude();
            String altitiude = "Altitiude: " + mobileLocation.getAltitude();
            String accuracy = "Accuracy: " + mobileLocation.getAccuracy();
            String time = "Time: " + mobileLocation.getTime();

            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> list = geocoder.getFromLocation(mobileLocation.getLatitude(), mobileLocation.getLongitude(), 1);
                if (!list.isEmpty()) {
                    Address address = list.get(0);
                    String lugar = address.getLocality();
                    alert.setMessage(lugar);
                    if(lugar.equals("San Justo")) {
                        mConnectedThread.write("-1001;20\n");
                    }
                    if(lugar.equals("Gregorio de Laferrere")) {
                        mConnectedThread.write("-1002;15\n");
                    } else {
                        if(lugar.equals("Isidro Casanova")) {
                            mConnectedThread.write("-1003;19\n");
                        }
                    }
                    //messageTextView2.setText("Mi direcci—n es: \n" + address.getAddressLine(0));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            //alert.setMessage(londitude + "\n" + latitude + "\n"
            //        + altitiude + "\n" + accuracy + "\n" + time);
        } else {
            alert.setMessage("Sorry, location is not determined");
        }

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = alert.create();
        dialog.show();
    }

    /** Gets the current location and update the mobileLocation variable*/
    private void getCurrentLocation() {
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locListener = new LocationListener() {
            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProviderEnabled(String provider) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProviderDisabled(String provider) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onLocationChanged(Location location) {
                // TODO Auto-generated method stub
                mobileLocation = location;
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
    }
}

