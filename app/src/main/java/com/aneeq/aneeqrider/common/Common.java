package com.aneeq.aneeqrider.common;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.aneeq.aneeqrider.Home;
import com.aneeq.aneeqrider.models.DataMessage;
import com.aneeq.aneeqrider.models.FCMResponse;
import com.aneeq.aneeqrider.models.Rider;
import com.aneeq.aneeqrider.models.Token;
import com.aneeq.aneeqrider.remote.FCMClient;
import com.aneeq.aneeqrider.remote.GoogleMapAPI;
import com.aneeq.aneeqrider.remote.IFCMService;
import com.aneeq.aneeqrider.remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Common {

    public static final String driver_tbl = "Drivers";
    public static final String user_driver_tbl = "DriversInformation";
    public static final String user_rider_tbl = "RidersInformation";
    public static final String pickup_request_tbl = "PickupRequest";
    public static final String rate_detail_tbl = "RateDetails";
    public static final String token_tbl = "Tokens";
    public static boolean isDriverFound = false;
    public static String driverId = "";
    public static final String user_field = "rider_user";
    public static final String pwd_field = "rider_pwd";
    public static final int PICK_IMAGE_REQUEST = 9999;
    public static int radius = 1; // 1km
    public static int distance = 1; // 3km
    public static final int LIMIT = 3;
    public static Location mLastLocation;
    public static Marker mUserMarker;
    public static GoogleMap mMap;
    public static boolean isCitadine = true;

    public static Rider currentUser = new Rider();

    private static final String fcmURL = "https://fcm.googleapis.com";
    private static final String googleAPIUrl = "https://maps.googleapis.com";

    private static double base_fare = 2.55;
    private static double time_rate = 0.35;
    private static double distance_rate = 1.75;


    public static double getPrice(double km, int min) {
        return (base_fare + (time_rate*min)+(distance_rate*km));
    }

    public static IFCMService getFCMService () {
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }

    public static IGoogleAPI getGoogleService () {
        return GoogleMapAPI.getClient(googleAPIUrl).create(IGoogleAPI.class);
    }

    public static void sendRequestToDriver(String driverId, final IFCMService mService, final Context context, final Location currentLocation) {
        Log.e("sending", "Sending request");
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference(Common.token_tbl);
        tokens.orderByKey().equalTo(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                            Token token = postSnapshot.getValue(Token.class); // Get token object from database with key

                            // Make raw playload - convert LatLng to json
                            String riderToken = FirebaseInstanceId.getInstance().getToken();
                            Map<String, String> content = new HashMap<>();
                            content.put("customer", riderToken);
                            Log.e("customer", riderToken);
                            content.put("lat", String.valueOf(currentLocation.getLatitude()));
                            Log.e("lat", String.valueOf(currentLocation.getLatitude()));
                            content.put("lng", String.valueOf(currentLocation.getLongitude()));
                            Log.e("lng", String.valueOf(currentLocation.getLongitude()));
                            DataMessage dataMessage = new DataMessage(token.getToken(), content);

                            mService.sendMessage(dataMessage)
                                    .enqueue(new Callback<FCMResponse>() {
                                        @Override
                                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                            if (response.isSuccessful()) {
                                                if (response.body().success == 1)
                                                    Toast.makeText(context, "Request sent !", Toast.LENGTH_SHORT).show();
                                                else
                                                    Toast.makeText(context, "Failed !", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        @Override
                                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                                            Log.e("ANEEQ", t.getMessage());

                                        }
                                    });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    public static void requestPickupHere(String uid, Location mLastLocation, Marker mUserMarker, GoogleMap mMap, Context context) {
        String carType;

        if (isCitadine) {
            carType = "Citadine";
        } else {
            carType = "Berline";
        }
        Toast.makeText(context, carType, Toast.LENGTH_SHORT).show();

        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(Common.pickup_request_tbl).child(carType);
        GeoFire mGeoFire = new GeoFire(dbRequest);
        mGeoFire.setLocation(uid, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });

        if (mUserMarker.isVisible()) {
            mUserMarker.remove();
        }

        // Add new marker
        mUserMarker = mMap.addMarker(new MarkerOptions()
                .title("Pickup here")
                .snippet("")
                .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        mUserMarker.showInfoWindow();

        Toast.makeText(context, "Getting you a driver ...", Toast.LENGTH_SHORT).show();

        findDriver(mLastLocation, context);

    }

    private static void findDriver(final Location mLastLocation, final Context context) {
        String carType;

        if (isCitadine) {
            carType = "Citadine";
        } else {
            carType = "Berline";
        }
        Toast.makeText(context, carType, Toast.LENGTH_SHORT).show();
        Toast.makeText(context, "finding driver", Toast.LENGTH_SHORT).show();

        final DatabaseReference drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl).child(carType);
        GeoFire gfDrivers = new GeoFire(drivers);

        final GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // if found
                if (!Common.isDriverFound) {
                    Common.isDriverFound = true;
                    Common.driverId = key;
                    Toast.makeText(context, "Call driver", Toast.LENGTH_SHORT).show();
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
                if (!Common.isDriverFound && radius < LIMIT) {
                    Toast.makeText(context, "Increase radius", Toast.LENGTH_SHORT).show();
                    radius++;
                    findDriver(mLastLocation, context);
                } else {
                    if (Common.isDriverFound) {
                        Toast.makeText(context, "Driver found", Toast.LENGTH_SHORT).show();
                        geoQuery.removeAllListeners();
                    } else {
                        Toast.makeText(context, "LIMIT", Toast.LENGTH_SHORT).show();
                    }
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Toast.makeText(context, "error " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("error geoquery", error.getMessage());

            }
        });

    }
}
