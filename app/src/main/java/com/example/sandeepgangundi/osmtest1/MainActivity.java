package com.example.sandeepgangundi.osmtest1;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.os.Environment;
import android.content.res.AssetManager;
import android.view.View;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//public class MainActivity extends ActionBarActivity {
public class MainActivity extends Activity implements SensorEventListener {
    //TAG for log messages
    private static final String TAG = "FRIDE";

    // The MapView variable:
    private MapView mMapView;

    // Default map zoom level:
    private int MAP_ZOOM = 15;

    // Default map Latitude:
    private double MAP_LATITUDE = 37.556022;

    // Default map Longitude:
    private double MAP_LONGITUDE = -121.986528;

    private String osmDirName = "osmdroid";

    private String appDirName = "frideData";

    private GeoPoint currentLocation;
    private ItemizedIconOverlay mMyLocationOverlay = null;
    private OverlayItem myCurrentLocationOverlayItem;
    private Drawable myCurrentLocationMarker;
    private DefaultResourceProxyImpl resourceProxy;

    private OverlayItem mDefaultOverlayItem;
    private ItemizedIconOverlay mDefaultOverlay = null;

    private HashMap<String, GeoPoint> attractionsMap = new HashMap<String, GeoPoint>();
    private HashMap<String, Integer> attractionsPlayed = new HashMap<String, Integer>();
    private Boolean checkAttraction = false;

    public float azimuth;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        copyTilesToSDCard();

        // Initialize attractionsMap with all the attraction GeoPoints
        initializeAttractionsMap();

        // Specify the XML layout to use:
        setContentView(R.layout.activity_main);

        // Find the MapView controller in that layout:
        mMapView = (MapView) findViewById(R.id.mapview);

        mMapView.setTileSource(new XYTileSource("MapQuest",
                ResourceProxy.string.mapquest_osm, 0, 18, 256, ".jpg", new String[]{
                "http://otile1.mqcdn.com/tiles/1.0.0/map/",
                "http://otile2.mqcdn.com/tiles/1.0.0/map/",
                "http://otile3.mqcdn.com/tiles/1.0.0/map/",
                "http://otile4.mqcdn.com/tiles/1.0.0/map/"}));

        // Setup the mapView controller:
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        mMapView.setClickable(true);

        // To prevent download online tiles using the network connection.
        mMapView.setUseDataConnection(false);
        IMapController mapController = mMapView.getController();
        mapController.setZoom(15);

        createInitialOverlay();

        currentLocation = new GeoPoint(MAP_LATITUDE, MAP_LONGITUDE);

        LocationListener locationListener = new MyLocationListener();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        }

        mapController.setCenter(currentLocation);

        // compass sensor
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

    } // end onCreate()

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    float[] mGravity = new float[3];
    float[] mGeomagnetic = new float[3];

    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //mGravity = event.values;
            mGravity[0] = alpha * mGravity[0] + (1 - alpha)
                    * event.values[0];
            mGravity[1] = alpha * mGravity[1] + (1 - alpha)
                    * event.values[1];
            mGravity[2] = alpha * mGravity[2] + (1 - alpha)
                    * event.values[2];
            //TextView text = (TextView) findViewById(R.id.textView3);
            //text.setText("Gravity: " + mGravity[0] + ":" + mGravity[1] + ":" + mGravity[2]);
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //mGeomagnetic = event.values;
            mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
                    * event.values[0];
            mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
                    * event.values[1];
            mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
                    * event.values[2];
        }

        if (mGravity != null && mGeomagnetic != null) {
            float S[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(S, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(S, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]); // orientation contains: azimuth, pitch and roll
                //double dir = -azimuth * 360/(2 * 3.14159);
                azimuth = (azimuth + 360) % 360;
                TextView text = (TextView) findViewById(R.id.textView3);
                text.setText("Azimuth: " + azimuth);


            }
        }
    }

    private void initializeAttractionsMap() {
        GeoPoint x = new GeoPoint(37.555004, -121.984870);
        attractionsMap.put("WholeFoods", x);
        attractionsPlayed.put("WholeFoods", 0);

        x = new GeoPoint(37.550564, -121.986319);
        attractionsMap.put("SaravanaBhavan", x);
        attractionsPlayed.put("SaravanaBhavan", 0);

    }

    private void copyTilesToSDCard() {
        // save zip to sd
        AssetManager assetManager = this.getAssets();
        InputStream is;
        // the zip file lies in assets root
        String sourceFileName = "FremontHome.zip";
        // the zip file in the scdard
        String destinationFileName = "FremontHome.zip";

        File osmDir = new File(Environment.getExternalStorageDirectory()
                + File.separator + osmDirName);
        if (!osmDir.exists()) {
            osmDir.mkdir();
        }
        String filePath = Environment.getExternalStorageDirectory()
                + File.separator + osmDirName + File.separator
                + destinationFileName;
        try {
            is = assetManager.open(sourceFileName);
            FileOutputStream fo = new FileOutputStream(filePath);

            byte[] b = new byte[1024];
            int length;
            while ((length = is.read(b)) != -1) {
                fo.write(b, 0, length);
            }
            fo.flush();
            fo.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sourceFileName = "WholeFoods.mp3";
        // the zip file in the scdard
        destinationFileName = "WholeFoods.mp3";

        File dataDir = new File(Environment.getExternalStorageDirectory()
                + File.separator + appDirName);
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }
        filePath = Environment.getExternalStorageDirectory()
                + File.separator + appDirName + File.separator
                + destinationFileName;
        try {
            is = assetManager.open(sourceFileName);
            FileOutputStream fo = new FileOutputStream(filePath);

            byte[] b = new byte[1024];
            int length;
            while ((length = is.read(b)) != -1) {
                fo.write(b, 0, length);
            }
            fo.flush();
            fo.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            currentLocation = new GeoPoint(location);
            displayMyCurrentLocationOverlay();
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    private void displayMyCurrentLocationOverlay() {
        if (currentLocation != null) {
            mMapView.getOverlays().clear();
            resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
            myCurrentLocationOverlayItem = new OverlayItem("My Location", "My Location!", currentLocation);
            myCurrentLocationMarker = this.getResources().getDrawable(R.drawable.mapmarker);
            myCurrentLocationOverlayItem.setMarker(myCurrentLocationMarker);
            final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
            items.add(myCurrentLocationOverlayItem);

            mMyLocationOverlay = new ItemizedIconOverlay<OverlayItem>(items,
                    new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                        public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                            return true;
                        }

                        public boolean onItemLongPress(final int index, final OverlayItem item) {
                            return true;
                        }
                    }, resourceProxy);

            mMapView.getOverlays().add(mDefaultOverlay);
            mMapView.getOverlays().add(mMyLocationOverlay);

            calculateDistanceToDestination(currentLocation);

            String nextAttraction = null;
            //if (!checkAttraction) {
            // Find closest attractions
            nextAttraction = getClosestAttraction(currentLocation);
            Log.i(TAG, "Closest Attraction is : " + nextAttraction);
            if (attractionsPlayed.get(nextAttraction) != 1) {
                attractionsPlayed.put(nextAttraction, 1);
                playAttraction(nextAttraction);
                Log.i(TAG, "Done playing audio file for" + nextAttraction);
            }
            //}

            //} else {
            //    myCurrentLocationOverlayItem. setPoint(currentLocation);
            //    currentLocationOverlay.requestRedraw();
            //}
            //mMapView.getController().setCenter(currentLocation);
        }
    }

    private String getClosestAttraction(GeoPoint location) {
        //checkAttraction = true;
        String nextAttraction = null;
        double dist = 1, tmp = 0;

        for (Map.Entry<String, GeoPoint> entry : attractionsMap.entrySet()) {
            String key = entry.getKey();
            GeoPoint value = entry.getValue();

            if ((Math.abs(location.getLatitude() - value.getLatitude()) < 0.01) ||
                    (Math.abs(location.getLongitude() - value.getLongitude()) < 0.01)) {
                tmp = getDistance(location.getLongitude(), location.getLatitude(),
                        value.getLongitude(), value.getLatitude());
                if (tmp < dist) {
                    dist = tmp;
                    nextAttraction = key;
                }
            }
        }

        return nextAttraction;
    }

    private void playAttraction(String attr) {
        String destinationFileName = attr + ".mp3";
        String filePath = Environment.getExternalStorageDirectory()
                + File.separator + appDirName + File.separator
                + destinationFileName;

        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    //checkAttraction = false;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void centerCurrentLocation(View view) {
        mMapView.getController().setCenter(currentLocation);

    }

    private void createInitialOverlay() {

        resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
        final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

        mDefaultOverlayItem = new OverlayItem("Whole Foods", "Whole Foods", attractionsMap.get("WholeFoods"));
        myCurrentLocationMarker = this.getResources().getDrawable(R.drawable.mapmarker);
        mDefaultOverlayItem.setMarker(myCurrentLocationMarker);
        items.add(mDefaultOverlayItem);

        mDefaultOverlayItem = new OverlayItem("Saravana Bhavan", "Saravana Bhavan", attractionsMap.get("SaravanaBhavan"));
        myCurrentLocationMarker = this.getResources().getDrawable(R.drawable.mapmarker);
        mDefaultOverlayItem.setMarker(myCurrentLocationMarker);
        items.add(mDefaultOverlayItem);

        mDefaultOverlay = new ItemizedIconOverlay<OverlayItem>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        return true;
                    }

                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return true;
                    }
                }, resourceProxy);
        mMapView.getOverlays().add(mDefaultOverlay);

    }

    public void calculateDistanceToDestination(GeoPoint location) {
        double currLong = location.getLongitude();
        double currLat = location.getLatitude();

        double destLong = attractionsMap.get("WholeFoods").getLongitude();
        double destLat = attractionsMap.get("WholeFoods").getLatitude();

        double distance = getDistance(currLong, currLat, destLong, destLat);

        TextView text1 = (TextView) findViewById(R.id.textView);
        text1.setText("Dist from WF: " + distance);

        destLong = attractionsMap.get("SaravanaBhavan").getLongitude();
        destLat = attractionsMap.get("SaravanaBhavan").getLatitude();

        distance = getDistance(currLong, currLat, destLong, destLat);

        text1 = (TextView) findViewById(R.id.textView2);
        text1.setText("Dist from SB: " + distance);

    }

    public double getDistance(double long1, double lat1, double long2, double lat2) {
        double earthRadius = 3958.75;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(long2 - long1);

        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);

        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double dist = earthRadius * c;

        return dist;
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
