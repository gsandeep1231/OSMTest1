package com.example.sandeepgangundi.osmtest1;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.os.Environment;
import android.content.res.AssetManager;
import android.view.View;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

//public class MainActivity extends ActionBarActivity {
public class MainActivity extends Activity {
    // The MapView variable:
    private MapView mMapView;

    // Default map zoom level:
    private int MAP_ZOOM = 15;

    // Default map Latitude:
    private double MAP_LATITUDE = 37.556022;

    // Default map Longitude:
    private double MAP_LONGITUDE = -121.986528;

    private String osmDirName = "osmdroid";

    private GeoPoint currentLocation;
    private ItemizedIconOverlay mMyLocationOverlay = null;
    private OverlayItem myCurrentLocationOverlayItem;
    private Drawable myCurrentLocationMarker;
    private DefaultResourceProxyImpl resourceProxy;

    private OverlayItem mDefaultOverlayItem;
    private ItemizedIconOverlay mDefaultOverlay = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        //copyTilesToSDCard();


    } // end onCreate()

    private void copyTilesToSDCard() {
        // save zip to sd
        AssetManager assetManager = this.getAssets();
        InputStream is;
        // the zip file lies in assets root
        String sourceFileName = "MapQuest.zip";
        // the zip file in the scdard
        String destinationFileName = "osmdroid.zip";

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

            //} else {
            //    myCurrentLocationOverlayItem. setPoint(currentLocation);
            //    currentLocationOverlay.requestRedraw();
            //}
            //mMapView.getController().setCenter(currentLocation);
        }
    }

    public void centerCurrentLocation(View view) {
        mMapView.getController().setCenter(currentLocation);

    }

    private void createInitialOverlay() {
        GeoPoint wholeFoods = new GeoPoint(37.555004, -121.984870);
        GeoPoint saravanaBhavan = new GeoPoint(37.550564, -121.986319);

        resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
        final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

        mDefaultOverlayItem = new OverlayItem("Whole Foods", "Whole Foods", wholeFoods);
        myCurrentLocationMarker = this.getResources().getDrawable(R.drawable.mapmarker);
        mDefaultOverlayItem.setMarker(myCurrentLocationMarker);
        items.add(mDefaultOverlayItem);

        mDefaultOverlayItem = new OverlayItem("Saravana Bhavan", "Saravana Bhavan", saravanaBhavan);
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
