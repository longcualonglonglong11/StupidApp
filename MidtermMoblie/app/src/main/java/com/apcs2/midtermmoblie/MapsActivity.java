package com.apcs2.midtermmoblie;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.icu.text.Transliterator;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    FusedLocationProviderClient mFusedLocationProviderClient;
    private RequestQueue mQueue;
    private JsonObjectRequest mJsonObjectRequest;
    private GoogleMap mMap;
    private Marker mMarker;
    private TextToSpeech mTextToSpeech;
    private boolean misTextToSpeech = true;
    private int REQUEST_PERMISSION_ACCESS_FINE_LOCATION_AND_INTERNET_CODE = 1235;
    private String TAG_FAIL = "locationFail";
    LinearLayout containerLayout;
    LinearLayout requestForm;
    LinearLayout deltailView;
    TextView detailTitle;
    TextView detailDescription;
    TextView detailPhone;
    TextView detailLocation;
    EditText eTitle;
    EditText eDescription;
    EditText eLocation;
    EditText ePhone;
    Spinner sEmergency;
    CheckBox cbCurrentLocation;
    TextView tEmergency;
    TextView detailEmergency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        loadData();
        initComponent();
    }

    public void initComponent() {
        containerLayout = findViewById(R.id.container);
        requestForm = findViewById(R.id.request_from);
        deltailView = findViewById(R.id.detail_view);
        detailTitle = findViewById(R.id.detail_title);
        detailDescription = findViewById(R.id.detail_description);
        detailPhone = findViewById(R.id.detail_phone);
        detailLocation = findViewById(R.id.detail_location);
        eTitle = findViewById(R.id.eTitle);
        eDescription = findViewById(R.id.eDescription);
        eLocation = findViewById(R.id.eLocation);
        ePhone = findViewById(R.id.ePhone);
        sEmergency = findViewById(R.id.s_emergency);
        tEmergency = findViewById(R.id.t_emergency);
        cbCurrentLocation = findViewById(R.id.current_location);
        detailEmergency = findViewById(R.id.detail_emergency);
        ArrayAdapter<CharSequence> sEmergencyAdapter = ArrayAdapter.createFromResource(getApplicationContext(),
                R.array.emergency_level, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        sEmergencyAdapter.setDropDownViewResource(R.layout.emergency_level_spinner);
// Apply the adapter to the spinner
        sEmergency.setAdapter(sEmergencyAdapter);
        sEmergency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = (String) adapterView.getItemAtPosition(i);
                tEmergency.setText("Emergency Level: " + item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        mTextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                misTextToSpeech = true;
            }
        });
    }

    public String createDirectionUri(LatLng startPosition, LatLng desPositon) {
        String start = String.valueOf(startPosition.longitude) + ',' + String.valueOf(startPosition.latitude);
        String des = String.valueOf(desPositon.longitude) + ',' + String.valueOf(desPositon.latitude);
        return getString(R.string.MAPBOX_URL) + start + ';' + des + getString(R.string.ACCESS_TOKEN);
    }

    private void requestDirection(String url) {
        mQueue = Volley.newRequestQueue(this);
        mJsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject route = response
                                    .getJSONArray(getString(R.string.route))
                                    .getJSONObject(0);
                            ArrayList<LatLng> listPointRoute = decodePoly(route
                                    .getString
                                            (getString
                                                    (R.string.geometry)));
                            drawPolyline(listPointRoute,
                                    getString(R.string.colorPolyLine),
                                    10);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.v(TAG_FAIL, "On respone");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v(TAG_FAIL, "err");
            }
        });
        checkInternetPermission();
    }


    public void addRequestToQueue() {
        mQueue.add(mJsonObjectRequest);
    }

    public void checkInternetPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_GRANTED) {
            addRequestToQueue();
        } else {
            requestLocation(Manifest.permission.INTERNET, REQUEST_PERMISSION_ACCESS_FINE_LOCATION_AND_INTERNET_CODE);
            addRequestToQueue();
        }
    }

    public void loadData() {
//        Intent intent = getIntent();
//        mLandmark = new Landmark(intent.getStringExtra("name"),
//                intent.getStringExtra("des"),
//                intent.getIntExtra("logoID", 0),
//                intent.getDoubleExtra("lat", 0),
//                intent.getDoubleExtra("long", 0));
    }

    public ArrayList<LatLng> decodePoly(String encoded) {
        ArrayList<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the cameram

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
//                if (misTextToSpeech) {
//                    mTextToSpeech.speak(mLandmark.getDescription(),
//                            TextToSpeech.QUEUE_FLUSH, null);
//                    Toast.makeText(getApplicationContext(),
//                            mLandmark.getDescription(),
//                            Toast.LENGTH_SHORT
//                    ).show();
//                }
//                return false;
                containerLayout = findViewById(R.id.container);
                deltailView = findViewById(R.id.detail_view);

                deltailView.setVisibility(View.VISIBLE);
                containerLayout.setGravity(Gravity.BOTTOM);
                detailTitle.setText("Title: " + marker.getTitle());
                LatLng tmpLatLng = marker.getPosition();
                detailLocation.setText("Location: " + String.valueOf(tmpLatLng.latitude) + "N, " + String.valueOf(tmpLatLng.longitude) + "E");
                String spitSign = "~";
                String[] splitedStr = marker.getSnippet().split(spitSign);
                detailDescription.setText("Description: " + splitedStr[0]);
                detailPhone.setText("Phone: " + splitedStr[1]);
                detailEmergency.setText(splitedStr[2]);
                Log.d("DCCC", marker.getTitle());

                return true;
            }
        });
        // nay cua thay k xai
        //  displayMarkers();
    }

//    private void displayMarkers() {
//        Bitmap bmp = BitmapFactory.decodeResource(getResources(), mLandmark.getLogoID());
//        bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / 4, bmp.getHeight() / 4, false);
//        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bmp);
//        mMarker = mMap.addMarker(new MarkerOptions()
//                .position(mLandmark.getLatLng())
//                .icon(bitmapDescriptor)
//                .title(mLandmark.getName())
//                .snippet(mLandmark.getDescription()));
//        CameraPosition newCameraPosition = new CameraPosition.Builder()
//                .target(mLandmark.getLatLng()) // Sets the center of the map to Mountain View
//                .zoom(15)                      // Sets the zoom
//                .bearing(90)                   // Sets the orientation of the camera to east
//                .tilt(30)                      // Sets the tilt of the camera to 30 degrees
//                .build();
//        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition));
//    }

    private boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            return true;
        } else {
            requestLocation(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_PERMISSION_ACCESS_FINE_LOCATION_AND_INTERNET_CODE);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1235:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                    getDeviceLocation();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            Task<Location>[] locationResult = new Task[]{mFusedLocationProviderClient.getLastLocation()};
            locationResult[0].addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        Location lastKnownLocation = task.getResult();
                        if (lastKnownLocation != null) {
                            mMap.setMyLocationEnabled(true);
                            LatLng curLocation = new LatLng(lastKnownLocation.getLatitude(),
                                    lastKnownLocation.getLongitude());


                            curLat = curLocation.latitude;
                            curLong = curLocation.longitude;

                        } else {
                            Toast.makeText(getApplicationContext(), "PLEASE TURN ON YOUR LOCATION !!!", Toast.LENGTH_SHORT).show();
                            Log.v(TAG_FAIL, "Get LOCATION FAIL");

                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "PLEASE TURN ON YOUR LOCATION !!!", Toast.LENGTH_SHORT).show();
                        Log.v(TAG_FAIL, "DEVICE NOT HAVE LOCATION");
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private double curLat;
    private double curLong;


    public void createForm(View view) {
        //     textView.setVisibility(View.VISIBLE);
        // SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        //        .findFragmentById(R.id.map);
        containerLayout.setGravity(Gravity.CENTER);
        requestForm.setVisibility(View.VISIBLE);
        eTitle.setText("");
        //String title = String.valueOf(eTitle.getText());
        eDescription.setText("");
        String description = String.valueOf(eDescription.getText());
        eLocation.setText("");
        ePhone.setText("");
        //  String location = String.valueOf(eLocation.getText());
        // linearLayout.bringChildToFront(textView);

        // if true
//        if (checkPermission()) {
//            Button switchButton = findViewById(R.id.swithButton);
//            if (switchButton.getText().equals("Direct")) {
//                getDeviceLocation();
//                switchButton.setText("Remove path");
//            } else {
//                for (int i = 0; i < polylines.size(); i++)
//                    polylines.get(i).remove();
//                switchButton.setText("Direct");
//            }
//            return;
//        }
//        // if false
//        requestLocation(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_PERMISSION_ACCESS_FINE_LOCATION_AND_INTERNET_CODE);
    }

    private void requestLocation(String accessFineLocation, int requestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{accessFineLocation},
                requestCode);
    }

    private ArrayList<Polyline> polylines = new ArrayList<>();

    public void drawPolyline(ArrayList<LatLng> listPointRoute, String color, int width) {
        for (int i = 0; i < listPointRoute.size() - 1; i++) {
            LatLng src = listPointRoute.get(i),
                    des = listPointRoute.get(i + 1);
            Polyline singleLine = mMap.addPolyline(
                    new PolylineOptions().add(
                            new LatLng(src.latitude, src.longitude),
                            new LatLng(des.latitude, des.longitude)
                    )
                            .color(Color.parseColor(color))
                            .geodesic(true)
                            .width(width)
            );
            polylines.add(singleLine);
            //    Log.d("width", String.valueOf(singleLine.getWidth()));
        }
    }

    /**
     * Demonstrates converting a {@link Drawable} to a {@link BitmapDescriptor},
     * for use as a marker icon.
     */
    private BitmapDescriptor vectorToBitmap(@DrawableRes int id, @ColorInt int color) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(), id, null);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        DrawableCompat.setTint(vectorDrawable, color);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    public void saveMarker(View view) {


        // sEmergency.setOnItemClickListener(this;
        //thiết lập sự kiện chọn phần tử cho Spinner

        String title = String.valueOf(eTitle.getText());
        String description = String.valueOf(eDescription.getText());
        String location = String.valueOf(eLocation.getText());
        String phone = String.valueOf(ePhone.getText());


        Log.d("DCCCC", "No picture");
        BitmapDescriptor bitmapDescriptor = null;
        String level = (String) tEmergency.getText();
        if (level != "") {
            int color;
            if (level.contains("High")) {
                color = Color.RED;
            } else if (level.contains("Moderate")) {
                color = Color.YELLOW;
            } else {
                color = Color.BLUE;
            }
            //    Bitmap bmp = BitmapFactory.decodeResource(getResources(), warning);
            //    bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / 4, bmp.getHeight() / 4, false);
            bitmapDescriptor = vectorToBitmap(R.drawable.warning, color);
//            bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bmp);

        }
        LatLng tmpLatLng = null;
        List<Address> addresses = null;
        if (cbCurrentLocation.isChecked() && checkPermission()) {
            tmpLatLng = new LatLng(curLat, curLong);
        } else {
            Geocoder geocoder = new Geocoder(this);

            try {
                addresses = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                Log.d("DCCCC", "No location");
                e.printStackTrace();
            }
        }
        Address address = null;
        if (addresses != null && addresses.size() > 0) {
            address = addresses.get(0);
            tmpLatLng = new LatLng(address.getLatitude(), address.getLongitude());
        }
        TextView error = findViewById(R.id.error_from);
        if (tmpLatLng != null && !title.equals("") && !description.equals("") && !phone.equals("") && bitmapDescriptor != null) {
            String spitSign = "~";
            String containerStr = description + spitSign + phone + spitSign + level;
            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(tmpLatLng)
                    .icon(bitmapDescriptor)
                    .title(title)
                    .snippet(containerStr));
            CameraPosition newCameraPosition = new CameraPosition.Builder()
                    .target(tmpLatLng) // Sets the center of the map to Mountain View
                    .zoom(15)                      // Sets the zoom
                    .bearing(90)                   // Sets the orientation of the camera to east
                    .tilt(30)                      // Sets the tilt of the camera to 30 degrees
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition));
            error.setVisibility(View.GONE);
            requestForm.setVisibility(View.GONE);
            containerLayout.setGravity(Gravity.BOTTOM);
        } else {
            error = findViewById(R.id.error_from);
            error.setVisibility(View.VISIBLE);
        }

//        mMarker = mMap.addMarker(new MarkerOptions()
//                .position(tmpLatLng)
//                .title("bx q8")
//                .snippet("AAAAAAAAAAAAAAAAAAAAAAAAAAÂ"));
        //10.762913
        //106.6821717


    }


    public void close_form(View view) {
        containerLayout.setGravity(Gravity.BOTTOM);
        requestForm.setVisibility(View.GONE);
        eTitle.setText("");
        eDescription.setText("");
        eLocation.setText("");
        ePhone.setText("");
    }

    public void close_detail_form(View view) {
        deltailView.setVisibility(View.GONE);
    }


    public void choose_current_location(View view) {
        TextView tLocation = findViewById(R.id.t_location);
        if (cbCurrentLocation.isChecked()) {
            getDeviceLocation();
            eLocation.setVisibility(View.GONE);
            tLocation.setVisibility(View.GONE);
        } else {
            eLocation.setVisibility(View.VISIBLE);
            tLocation.setVisibility(View.VISIBLE);

        }
    }
}