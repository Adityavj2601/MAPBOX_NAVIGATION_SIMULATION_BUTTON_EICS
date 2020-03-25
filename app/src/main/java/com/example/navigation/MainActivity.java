/*-------------MainActivity for Establishing BT Connection,
    Map initialization and Navigation------------*/


//package Info
package com.example.navigation;

//classes needed to establish Bluetooth Conection with HC05 Module
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import java.util.UUID;
//classes need for View Component
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
// classes needed to add a marker
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
// classes needed to add the location component
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
// classes needed to initialize map
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
//classes need for Navigation In Application
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
//classes needed to access the the Logcat
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
//Utility Classes
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {

    //variable for UUID(BT Connection) and TAG(for debugging)
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "DirectionsActivity";
    static int i = 0;
    String MY_INSTRUCTION;

    //variable for establish and Handling the Connection
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;

    //variable for Storing the Announcement
    List<LegStep> steps;
    ArrayList announcement_list = new ArrayList();

    //variable for storing the MAC Address of HC05 Bluetooth Module
    String address = "00:19:09:26:0E:68";

    // variables for adding location layer
    private MapView mapView;
    private MapboxMap mapboxMap;

    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;

    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private DirectionsRoute currentRoute2;
    private DirectionsResponse directionsResponse;
    private NavigationMapRoute navigationMapRoute;

    // variables needed to initialize navigation
    private Button button;
    private List<Milestone> milestones;

    // Variables needed to add the location engine
    private LocationEngine locationEngine;
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;

    //variable for creating and Handling Thread
    private Handler handler = new Handler();
    private boolean isBtConnected = false;
    public ToggleButton tb ;

    //variable for displaying the process of Bluetooth Connection
    private ProgressDialog progress;
    public boolean check_simulation =false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        clear();
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        ConnectBT b = new ConnectBT();
        b.execute();
        tb = (ToggleButton)findViewById(R.id.toggle);
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    check_simulation = true;//simulation Button is ON
                } else {
                    check_simulation = false;//simulation Button is OFF
                }




            }
        });


    }

    //For Preparing the MAP in Application
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);

                addDestinationIconSymbolLayer(style);

                mapboxMap.addOnMapClickListener(MainActivity.this);
                button = findViewById(R.id.startButton);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {



                        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                                .directionsRoute(currentRoute)
                                .shouldSimulateRoute(check_simulation)
                                .build();



                        NavigationLauncher.startNavigation(MainActivity.this, options);
                        Thread t1 = new Thread(new Runnable() {

                            @Override
                            public void run() {

                                String check_string = "";
                                int size_of_announcement_list = announcement_list.size();
                                int index_of_announcement_list = 0;


                                while (index_of_announcement_list != size_of_announcement_list) {

                                    //variable needed for accessing the Logcat
                                    Process logcat;

                                    final StringBuilder log = new StringBuilder();
                                    try {

                                        logcat = Runtime.getRuntime().exec("logcat *:W");
                                        BufferedReader br = new BufferedReader(new InputStreamReader(logcat.getInputStream()), 8 * 4096);
                                        String line;

                                        while ((line = br.readLine()) != null) {

                                            if (line.contains(" mediaplayer went away with unhandled events")) {

                                                if (!check_string.equals(line)) {

                                                    String val = (String) announcement_list.get(index_of_announcement_list);
                                                    Log.v("Announcement : ", (String) announcement_list.get(index_of_announcement_list));
                                                    index_of_announcement_list++;
                                                    check_string = line;

                                                    if (!val.contains(",")) {

                                                        if (val.contains("left")) {

                                                            if (btSocket != null)
                                                                btSocket.getOutputStream().write("1".getBytes());

                                                        } else if (val.contains("right")) {

                                                            if (btSocket != null)
                                                                btSocket.getOutputStream().write("2".getBytes());
                                                        }
                                                    }

                                                } else {
                                                    Log.v("else ", "part");

                                                }
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                        t1.start();

                    }
                });//end of button.setOnClickListener()

            }
        });//end of mapboxMap.setStyle()
    }

    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    @SuppressWarnings({"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        Point destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }

        getRoute(originPoint, destinationPoint);

        button.setEnabled(true);
        button.setBackgroundResource(R.color.mapboxBlue);
        return true;
    }

    public void getDirection(DirectionsRoute dr) {

        String[] instruction;
        Log.d("man", dr.voiceLanguage());
    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);


                        steps = response.body().routes().get(0).legs().get(0).steps();


                        for (LegStep l : steps) {

                            for (VoiceInstructions voiceInstruction : l.voiceInstructions()) {
                                if (voiceInstruction.announcement() != null) {

                                    announcement_list.add(voiceInstruction.announcement()); }
                            }

                        }


                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }


    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        mapView.onDestroy();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }


    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    //class need  for establishing a Bluetooth Connection
    class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection


                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");

            } else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

    }//end of ConnectBT class

    //method for clearing Chache for better execution of Application
    public void clear(){

        PackageManager pm = getPackageManager();
        Method[] methods = pm.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().equals("freeStorage")) {
                // Found the method I want to use
                try {
                    long desiredFreeStorage = 8 * 1024 * 1024 * 1024; // Request for 8GB of free space
                    m.invoke(pm, desiredFreeStorage , null);
                } catch (Exception e) {
                    // Method invocation failed. Could be a permission problem
                }
                break;
            }
        }
    }
}

