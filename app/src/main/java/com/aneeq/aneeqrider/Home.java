package com.aneeq.aneeqrider;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.maps.android.SphericalUtil;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;
import com.aneeq.aneeqrider.common.Common;
import com.aneeq.aneeqrider.helper.CustomInfoWindow;
import com.aneeq.aneeqrider.models.Rider;
import com.aneeq.aneeqrider.models.Token;
import com.aneeq.aneeqrider.remote.IFCMService;
import io.paperdb.Paper;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnInfoWindowClickListener,
        View.OnClickListener, PlaceSelectionListener {

    SupportMapFragment mapFragment;

    // Play services & Location
    private static final int MY_PREMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private static int UPDATE_INTERVAL = 5000;
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    Marker markerDestination;

    Button btnPickupRequest;

    //Send alert
    IFCMService mService;

    // Presence System
    DatabaseReference driversAvailable;

    PlaceAutocompleteFragment place_location, place_destination;
    AutocompleteFilter typeFilter;

    String mPlaceLocation, mPlaceDestination;

    CircleImageView imageAvatar;
    TextView txtRiderName, txtStars;

    FirebaseStorage storage;
    StorageReference storageReference;

    // Car type
    ImageView select_citadine, select_berline;

    View navigationHeaderView;
    NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Init variables
        init();
        loadAvatar();

        typeFilter = new AutocompleteFilter.Builder()
                //.setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                .setCountry("DZ")
                .build();

        setUpLocation();
        updateFirebaseToken();
    }

    public void init() {
        mService = Common.getFCMService();

        // Init storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Init Views
        navigationView = findViewById(R.id.nav_view);
        navigationHeaderView = navigationView.getHeaderView(0);
        txtStars = navigationHeaderView.findViewById(R.id.txtStars);
        imageAvatar = navigationHeaderView.findViewById(R.id.imageAvatar);
        txtRiderName = navigationHeaderView.findViewById(R.id.txtRiderName);


        // Maps
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        select_berline = findViewById(R.id.select_berline);
        select_citadine = findViewById(R.id.select_citadine);
        btnPickupRequest = findViewById(R.id.btnPickupRequest);

        place_location = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_location);
        place_destination = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_destination);


        navigationView.setNavigationItemSelectedListener(this);

        // Setting current user info
        txtStars.setText(String.format("%s", Common.currentUser.getRates()));
        txtRiderName.setText(String.format("%s", Common.currentUser.getName()));;

        btnPickupRequest.setOnClickListener(this);
        select_citadine.setOnClickListener(this);
        select_berline.setOnClickListener(this);
        place_destination.setOnPlaceSelectedListener(this);
        place_location.setOnPlaceSelectedListener(this);
    }

    public void loadAvatar() {
        if (Common.currentUser.getAvatarUrl() != null && !TextUtils.isEmpty(Common.currentUser.getAvatarUrl())) {
            Picasso.with(this)
                    .load(Common.currentUser.getAvatarUrl())
                    .into(imageAvatar);
        }
    }

    @Override
    public void onPlaceSelected(Place place) {
        switch (Integer.parseInt(place.getId())) {
            case R.id.place_location:
                mPlaceLocation = place.getAddress().toString();
                // remove old marker
                Common.mMap.clear();

                // Add marker at new location
                Common.mUserMarker = Common.mMap.addMarker(new MarkerOptions().position(place.getLatLng())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                        .title("Pickup here"));
                Common.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15.0f));
                break;
            case R.id.place_destination:
                mPlaceDestination = place.getAddress().toString();
                // Add new destination marker
                Common.mMap.addMarker(new MarkerOptions()
                        .position(place.getLatLng())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_marker))
                        .title("Destination"));
                Common.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15.0f));

                // Show information in bottom
                BottomSheetRiderFragment mBottomSheet = BottomSheetRiderFragment.newInstances(mPlaceLocation, mPlaceDestination, false);
                mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());
                break;
        }
    }

    @Override
    public void onError(Status status) {

    }

    private void updateFirebaseToken() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);

        Token token = new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(token);
    }

    private void requestPickupHere(String uid) {
        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(Common.pickup_request_tbl);
        GeoFire mGeoFire = new GeoFire(dbRequest);
        mGeoFire.setLocation(uid, new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });

        if (Common.mUserMarker.isVisible()) {
            Common.mUserMarker.remove();
        }

        // Add new marker
        Common.mUserMarker = Common.mMap.addMarker(new MarkerOptions()
                .title("Pickup here")
                .snippet("")
                .position(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        Common.mUserMarker.showInfoWindow();

        btnPickupRequest.setText("Getting you a driver ...");

        findDriver();

    }

    private void findDriver() {
        final DatabaseReference drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gfDrivers = new GeoFire(drivers);

        final GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()), Common.radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // if found
                if (!Common.isDriverFound) {
                    Common.isDriverFound = true;
                    Common.driverId = key;
                    btnPickupRequest.setText("CALL DRIVER");
                    // Toast.makeText(Home.this, ""+key, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                // if still not found driver, increase distance
                if (!Common.isDriverFound && Common.radius < Common.LIMIT) {
                    Common.radius++;
                    findDriver();
                } else {
                    if (Common.isDriverFound) {
                        Toast.makeText(Home.this, "Driver found", Toast.LENGTH_SHORT).show();
                        btnPickupRequest.setText("REQUEST PICKUP");
                        geoQuery.removeAllListeners();
                    }


                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Request runtime permission
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CALL_PHONE
            }, MY_PREMISSION_REQUEST_CODE);
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PREMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }
                break;
        }
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            return;
        }

        Common.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (Common.mLastLocation != null) {

            // Create LatLng from mLastLocation and his is center point
            LatLng center = new LatLng(36.172327,  1.385120);
            // Distance in metters
            // Heading 0 is northSide, 90 is east, 180 is south and 270 is west
            // Base on compact
            LatLng northSide = SphericalUtil.computeOffset(center, 50000, 0);
            LatLng southSide = SphericalUtil.computeOffset(center, 50000, 0);

            LatLngBounds bounds = LatLngBounds.builder()
                    .include(northSide)
                    .include(southSide)
                    .build();

/*  Chlef
new LatLngBounds(
                    new LatLng(36.172327,  1.385120),
                    new LatLng(36.124310, 1.272167)));

                    Sydney
                    new LatLng(-33.880490, 151.184363),
                    new LatLng(-33.858754, 151.229596)));
 */
            place_location.setBoundsBias(new LatLngBounds(
                    new LatLng(36.122831, 1.278811),
                    new LatLng(36.192685, 1.381121)));


            place_location.setFilter(typeFilter);

            place_destination.setBoundsBias(bounds);
            place_destination.setFilter(typeFilter);



            // Presecence system
            driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
            driversAvailable.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    loadAllAvailableDrivers(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            final double latitude = Common.mLastLocation.getLatitude();
            final double longitude = Common.mLastLocation.getLongitude();

            loadAllAvailableDrivers(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));


        } else {
            Log.e("ERROR: ", "Cannot get your current location"  );
        }
    }

    private void loadAllAvailableDrivers(final LatLng location) {

        // Add Marker
        // Here we will clear all mar to delete all position of driver
        Common.mMap.clear();
        Common.mUserMarker = Common.mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                .position(location)
                .title("You"));
        // Move camerea to this position
        Common.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f));



        // Load available drivers in distance 3km
        final DatabaseReference driverLocation;
        if (Common.isCitadine) {
            driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tbl).child("Citadine");
        } else {
            driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tbl).child("Berline");
        }
        GeoFire gf = new GeoFire(driverLocation);

        Log.e("1", "loadDrivers");

        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.latitude, location.longitude), Common.distance);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                // User key to get email from table Users
                // Table users is table when driver register account and update information
                // Just open your Driver to check this table name
                FirebaseDatabase.getInstance().getReference(Common.user_driver_tbl)
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Log.e("2", "loadDrivers");
                                // Beceaus Rider and User model is sameproperties
                                // So we can user Rider model to get User here
                                Rider rider = dataSnapshot.getValue(Rider.class);

                                // Add driver to map
                                Common.mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
                                                .flat(true)
                                                .title(rider.getName())
                                                .snippet(dataSnapshot.getKey())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                                Log.e("3", "loadDrivers");

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if (Common.distance <= Common.LIMIT) {
                    Common.distance++;
                    loadAllAvailableDrivers(location);
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_RES_REQUEST).show();
            } else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_signOut:
                signOut();
                break;
            case R.id.nav_updateInformation:
                showUpdateInformationDialog();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showUpdateInformationDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this);
        alertDialog.setTitle("Update information");
        alertDialog.setMessage("Please use email to register");

        LayoutInflater inflater = LayoutInflater.from(this);
        View update_info_layout = inflater.inflate(R.layout.layout_update_information, null);

        final MaterialEditText edtName = update_info_layout.findViewById(R.id.edtName);
        final MaterialEditText edtPhone = update_info_layout.findViewById(R.id.edtPhone);
        final ImageView imgAvatar = update_info_layout.findViewById(R.id.imgAvatar);

        imgAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImageAndUpload();
            }
        });
        alertDialog.setView(update_info_layout);

        alertDialog.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                final AlertDialog waitingDialog = new SpotsDialog(Home.this);
                waitingDialog.show();

                String name = edtName.getText().toString();
                String phone = edtPhone.getText().toString();

                Map<String, Object> update = new HashMap<>();
                if (!TextUtils.isEmpty(name)) {
                    update.put("name", name);
                }
                if (!TextUtils.isEmpty(phone)) {
                    update.put("phone", phone);
                }

                // Update
                DatabaseReference riderInformation = FirebaseDatabase.getInstance().getReference(Common.user_rider_tbl);
                riderInformation.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .updateChildren(update).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        waitingDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(Home.this, "Information updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Home.this, "Update information failed !", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });


        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.show();


    }

    private void chooseImageAndUpload() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select picture"), Common.PICK_IMAGE_REQUEST);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri saveUri = data.getData();

            if (saveUri != null) {
                final ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Uploading ...");
                progressDialog.show();

                String imageName = UUID.randomUUID().toString(); // random new image name to upload
                final StorageReference imageFolder = storageReference.child("images/"+imageName);
                imageFolder.putFile(saveUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                progressDialog.dismiss();

                                imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // Save url to user information table
                                        Map<String, Object> update= new HashMap<>();
                                        update.put("avatarUrl", uri.toString());

                                        // Update
                                        DatabaseReference riderInformation = FirebaseDatabase.getInstance().getReference(Common.user_rider_tbl);
                                        riderInformation.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                .updateChildren(update).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(Home.this, "Avatar was uploaded", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(Home.this, "Update avatar failed !", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(Home.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                });

                            }
                        })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount() );
                        progressDialog.setMessage("Uploaded " + progress + "%");

                    }
                });
            }
        }
    }

    private void signOut() {
        /// Reset remember value
        Paper.init(this);
        Paper.book().destroy();


        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(Home.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        try {
            boolean isSuccess = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_style_map)
            );

            if (!isSuccess) {
                Log.e("ERROR", "Map style load failed')");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("ResourceException", e.getMessage());
        }


        Common.mMap = googleMap;
        Common.mMap.getUiSettings().setZoomControlsEnabled(true);
        Common.mMap.getUiSettings().setZoomGesturesEnabled(true);
        Common.mMap.setInfoWindowAdapter(new CustomInfoWindow(this));

        Common.mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // First, check markerDestination
                // If it's not null, just remove available marker
                if (markerDestination != null) {
                    markerDestination.remove();
                }
                markerDestination = Common.mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_marker))
                        .position(latLng)
                        .title("Destination"));
                Common.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));

                // Show botomsheet
                BottomSheetRiderFragment mBottomSheet = BottomSheetRiderFragment.newInstances(
                        String.valueOf(Common.mLastLocation.getLatitude()).replace(",",".")+","+String.valueOf(Common.mLastLocation.getLongitude()).replace(",","."),
                        String.valueOf(latLng.latitude).replace(",",".")+","+String.valueOf(latLng.longitude).replace(",","."),
                        true);
                Log.e("12",  Common.mLastLocation.getLatitude() + "," + Common.mLastLocation.getLongitude());
                Log.e("13", latLng.latitude +","+ latLng.longitude);
                mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());


            }
        });

        Common.mMap.setOnInfoWindowClickListener(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Common.mLastLocation = location;
        displayLocation();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (!marker.getTitle().equals("You")) {
            // Call new activity call driver
            Intent intent = new Intent(Home.this, CallDriver.class);
            intent.putExtra("driverId", marker.getSnippet());
            intent.putExtra("lat", Common.mLastLocation.getLatitude());
            intent.putExtra("lng", Common.mLastLocation.getLongitude());
            startActivity(intent);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.select_citadine:
                Common.isCitadine = true;
                select_citadine.setImageResource(R.drawable.car_citadine_select);
                select_berline.setImageResource(R.drawable.car_berline);
                Common.mMap.clear();
                loadAllAvailableDrivers(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));
                break;
            case R.id.select_berline:
                Common.isCitadine = false;
                select_berline.setImageResource(R.drawable.car_berline_select);
                select_citadine.setImageResource(R.drawable.car_citadine);
                Common.mMap.clear();
                loadAllAvailableDrivers(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));
                break;
            case R.id.btnPickupRequest:
                if (!Common.isDriverFound)
                    Common.requestPickupHere(
                            FirebaseAuth.getInstance().getCurrentUser().getUid(),
                            Common.mLastLocation,
                            Common.mUserMarker,
                            Common.mMap,
                            Home.this
                    );
                else
                    Common.sendRequestToDriver(Common.driverId, mService, getBaseContext(), Common.mLastLocation);
                break;
        }

    }


}
