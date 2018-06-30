package com.aneeq.aneeqrider;

import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import com.aneeq.aneeqrider.common.Common;
import com.aneeq.aneeqrider.models.Rate;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class RateActivity extends AppCompatActivity {

    Button btnSubmit;
    MaterialRatingBar ratingBar;
    MaterialEditText edtComment;

    FirebaseDatabase database;
    DatabaseReference rateDetailRef;
    DatabaseReference driverInformationRef;

    double ratingStars = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate);

        // Init firebase
        database = FirebaseDatabase.getInstance();
        rateDetailRef = database.getReference(Common.rate_detail_tbl);
        driverInformationRef = database.getReference(Common.user_driver_tbl);

        // Init views
        btnSubmit = findViewById(R.id.btnSubmit);
        ratingBar = findViewById(R.id.ratingBar);
        edtComment = findViewById(R.id.edtComment);

        // Event
        ratingBar.setOnRatingChangeListener(new MaterialRatingBar.OnRatingChangeListener() {
            @Override
            public void onRatingChanged(MaterialRatingBar ratingBar, float rating) {
                ratingStars = rating;
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitRateDetails(Common.driverId);
            }
        });

    }

    private void submitRateDetails(final String driverId) {
        final android.app.AlertDialog alertDialog = new SpotsDialog(this);
        alertDialog.show();

        Rate rate = new Rate();
        rate.setRate(String.valueOf(ratingStars));
        rate.setComment(edtComment.getText().toString());
        // update new value to firebase
        rateDetailRef.child(driverId)
                .push() // gen unique key
                .setValue(rate)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // update driver info
                        rateDetailRef.child(driverId)
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        double averageStars = 0.0;
                                        int count = 0;
                                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                                            Rate rate = postSnapshot.getValue(Rate.class);
                                            averageStars += Double.parseDouble(rate.getRate());
                                            count++;
                                        }
                                        double finalAverage = averageStars / count;
                                        DecimalFormat df = new DecimalFormat("#.#");
                                        String valueUpdate = df.format(finalAverage);

                                        // create object update
                                        Map<String, Object>  driverUpdateRate = new HashMap<>();
                                        driverUpdateRate.put("rates", valueUpdate);

                                        driverInformationRef.child(Common.driverId)
                                                .updateChildren(driverUpdateRate)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        Toast.makeText(RateActivity.this, "Thank you for rating !", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        alertDialog.dismiss();
                                                        Toast.makeText(RateActivity.this, "Rate updated but cannot change driver info !", Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        alertDialog.dismiss();
                        Toast.makeText(RateActivity.this, "Rating failed !", Toast.LENGTH_SHORT).show();

                    }
                });


    }
}
