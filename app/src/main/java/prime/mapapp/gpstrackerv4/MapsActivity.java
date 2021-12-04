package prime.mapapp.gpstrackerv4;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.core.OrderBy;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private String key = "HERE API KEY";
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient,fusedLocationClient;
    private LocationRequest locationRequest;
    private Marker lastpostionmarkerid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        FirstTimeMarker();
        UserGPS(this);
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                say("Aktualisiere");
                mMap.clear();
                FirstTimeMarker();
            }
        });
    }

    private void UserGPS(Context context){
        locationRequest = LocationRequest.create()
                .setInterval(100)
                .setFastestInterval(3000)
                .setFastestInterval(3000)
                .setMaxWaitTime(100);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (lastpostionmarkerid != null) {
                        Marker markerremove = lastpostionmarkerid;
                        markerremove.remove();
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                                .title("Deine Position")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .snippet("Du befindest dich hier"));
                        lastpostionmarkerid = marker;
                    } else {
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                                .title("Deine Position")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .snippet("Du befindest dich hier"));
                        lastpostionmarkerid = marker;
                    }

                }
            });
        }else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 1);
            say("GPS muss noch zugelassen werden");
        }
    }

    private void say(String string){
        Context context = getApplicationContext();
        CharSequence text = string;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    private void FirstTimeMarker(){
        PolylineOptions polylineOptions = new PolylineOptions();
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://gpstrackerv4-default-rtdb.europe-west1.firebasedatabase.app/tracker/gps.json?auth="+key+"&orderBy=\"seconds1970\"&limitToLast=100";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        //System.out.println("HERE Response is: "+ response.substring(0,500));

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            Iterator<String> keys = jsonObject.keys();
                            String str_Name;
                            System.out.println("HERE Länge" + jsonObject.length());
                            for (int i = 0; i < jsonObject.length(); i++) {
                                str_Name = keys.next();
                                System.out.println("HERE Objekt Name: "+str_Name);
                                System.out.println("HERE Gesamter Json Inhalt: " + jsonObject.optString(str_Name));
                                JSONObject jsonObject1 = new JSONObject(jsonObject.optString(str_Name));
                                System.out.println("HERE LAT: "+jsonObject1.getString("lat"));
                                System.out.println("HERE LNG: "+jsonObject1.getString("lat"));
                                System.out.println("HERE Seconds1970: "+jsonObject1.getString("seconds1970"));
                                Date date = new Date((Long.valueOf(jsonObject1.getString("seconds1970")))*1000);
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.GERMANY);
                                String formattedDate = sdf.format(date);
                                if (i == 0){
                                    LatLng location = new LatLng(Double.valueOf(jsonObject1.getString("lat")), Double.valueOf(jsonObject1.getString("lng")));
                                    Marker marker = mMap.addMarker(new MarkerOptions().position(location).title(formattedDate).snippet("Nicht der neueste Standort."));
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 18));

                                    polylineOptions.color(Color.RED);
                                    polylineOptions.width(3);
                                    polylineOptions.add(new LatLng(Double.valueOf(jsonObject1.getString("lat")), Double.valueOf(jsonObject1.getString("lng"))));
                                }

                                if (i != jsonObject.length()-1 && i!=0){
                                    LatLng location = new LatLng(Double.valueOf(jsonObject1.getString("lat")), Double.valueOf(jsonObject1.getString("lng")));
                                    Marker marker = mMap.addMarker(new MarkerOptions().position(location).title(formattedDate).snippet("Nicht der neueste Standort."));
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 18));

                                    polylineOptions.color(Color.RED);
                                    polylineOptions.width(3);
                                    polylineOptions.add(new LatLng(Double.valueOf(jsonObject1.getString("lat")), Double.valueOf(jsonObject1.getString("lng"))));
                                }

                                if (i == jsonObject.length()-1) {
                                    LatLng location = new LatLng(Double.valueOf(jsonObject1.getString("lat")), Double.valueOf(jsonObject1.getString("lng")));
                                    Marker marker = mMap.addMarker(new MarkerOptions().position(location).title(formattedDate).snippet("Letze bekannte Position").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                                    marker.showInfoWindow();
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 18));

                                    polylineOptions.color(Color.RED);
                                    polylineOptions.width(3);
                                    polylineOptions.add(new LatLng(Double.valueOf(jsonObject1.getString("lat")), Double.valueOf(jsonObject1.getString("lng"))));
                                }
                            }
                        } catch (JSONException e) {
                            System.out.println("HERE ERROR "+e);
                        }
                        mMap.addPolyline(polylineOptions);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("HERE That didn't work!");
            }
        });
        queue.add(stringRequest);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (lastpostionmarkerid != null) {
            Marker markerremove = lastpostionmarkerid;
            markerremove.remove();
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .title("Deine Position")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .snippet("Du befindest dich hier"));
            lastpostionmarkerid = marker;
        } else {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .title("Deine Position")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .snippet("Du befindest dich hier"));
            lastpostionmarkerid = marker;
        }
    }
}