package net.aucutt.howhighru;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager = null;
    private String provider;
    private static String TAG = "net.aucut.howhighRU";
   // private MyHandler mHandler;
    private static final String LAT="latitude";
    private static final String LONG="longitude";
    private static final String ELE="elevation";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        setTitle(provider);
        setUpMapIfNeeded();
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        Location location = locationManager.getLastKnownLocation(provider);
        if( location != null ){
            //mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("Marker"));
            mMap.setOnMapClickListener( new MYMapClickListener());
            LatLng latlng = new LatLng( location.getLatitude(), location.getLongitude() );
            CameraUpdate setCamera = CameraUpdateFactory.newLatLngZoom( latlng, 15.0f  );
            mMap.moveCamera( setCamera);
        }else{
            mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        }
    }

    class MYMapClickListener  implements OnMapClickListener{

          public void onMapClick(LatLng point){
              CameraUpdate setCamera = CameraUpdateFactory.newLatLngZoom( point, 15.0f  );
              mMap.moveCamera( setCamera);
              //double alt = getElevationFromGoogleMaps( point.longitude, point.latitude);
            //  mMap.addMarker(new MarkerOptions().position( point).title(alt + " ft."));
              new DownloadElevationTask().execute( point );
          }
    }

    private class DownloadElevationTask extends AsyncTask<LatLng, Integer, Double>{

        private LatLng input;
        @Override
        protected Double doInBackground(LatLng... latLngs) {
            double result = Double.NaN;
            input = latLngs[0];
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            String url = "http://maps.googleapis.com/maps/api/elevation/"
                    + "xml?locations=" + String.valueOf(latLngs[0].latitude)
                    + "," + String.valueOf(latLngs[0].longitude)
                    + "&sensor=true";
            HttpGet httpGet = new HttpGet(url);
            try {
                HttpResponse response = httpClient.execute(httpGet, localContext);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream instream = entity.getContent();
                    int r = -1;
                    StringBuffer respStr = new StringBuffer();
                    while ((r = instream.read()) != -1)
                        respStr.append((char) r);
                    String tagOpen = "<elevation>";
                    String tagClose = "</elevation>";
                    if (respStr.indexOf(tagOpen) != -1) {
                        int start = respStr.indexOf(tagOpen) + tagOpen.length();
                        int end = respStr.indexOf(tagClose);
                        String value = respStr.substring(start, end);
                        result = (double) (Double.parseDouble(value) * 3.2808399); // convert from meters to feet
                    }
                    instream.close();
                }
            } catch (ClientProtocolException e) {
                Log.e(TAG, " dang " + e.getLocalizedMessage());
            } catch (IOException e) {
                Log.e(TAG, "io " + e.getLocalizedMessage());
            }

            return result;
        }

        @Override
        protected void onPostExecute(Double result){
            mMap.clear();
            Marker myMarker =
                    mMap.addMarker(new MarkerOptions().position(input).title(result.toString() + " ft."));
            myMarker.showInfoWindow();
        }
    }




}
