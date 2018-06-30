package com.aneeq.aneeqrider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aneeq.aneeqrider.common.Common;
import com.aneeq.aneeqrider.models.DataMessage;
import com.aneeq.aneeqrider.models.FCMResponse;
import com.aneeq.aneeqrider.models.Rider;
import com.aneeq.aneeqrider.models.Token;
import com.aneeq.aneeqrider.remote.IFCMService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallDriver extends AppCompatActivity {

    CircleImageView avatar_image;
    TextView txt_name, txt_phone, txt_rate;
    Button btn_call_driver, btn_call_driver_phone;

    String driverId;
    Location mLastLocation;

    IFCMService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_driver);

        mService = Common.getFCMService();

        // init views
        avatar_image = findViewById(R.id.avatar_image);
        txt_name = findViewById(R.id.txt_name);
        txt_phone = findViewById(R.id.txt_phone);
        txt_rate = findViewById(R.id.txt_rate);
        btn_call_driver = findViewById(R.id.btn_call_driver);
        btn_call_driver_phone = findViewById(R.id.btn_call_driver_phone);

        btn_call_driver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (driverId != null && !driverId.isEmpty()) {
                    Common.sendRequestToDriver(Common.driverId, mService, getBaseContext(), mLastLocation);
                }

            }
        });

        btn_call_driver_phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + txt_phone.getText().toString()));
                if (ActivityCompat.checkSelfPermission(getBaseContext(), android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.

                    return;
                }
                startActivity(intent);
            }
        });

        if (getIntent() != null) {
            driverId = getIntent().getStringExtra("driverId");
            Log.e("driverId", driverId);
            double lat = getIntent().getDoubleExtra("lat", -1.0);
            double lng = getIntent().getDoubleExtra("lng", -1.0);

            mLastLocation = new Location("");
            mLastLocation.setLatitude(lat);
            mLastLocation.setLongitude(lng);

            loadDriverInfo(driverId);


        }

    }

    private void loadDriverInfo(final String driverId) {
        FirebaseDatabase.getInstance()
                .getReference(Common.user_driver_tbl)
                .child(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Rider driverUser = dataSnapshot.getValue(Rider.class);

                        if (!driverUser.getAvatarUrl().isEmpty()) {
                            Picasso.with(getBaseContext())
                                .load(driverUser.getAvatarUrl())
                                .into(avatar_image);
                        }
                        txt_name.setText(driverUser.getName());
                        txt_phone.setText(driverUser.getPhone());
                        txt_rate.setText(driverUser.getRates());

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }


}
