package com.aneeq.aneeqrider.service;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import com.aneeq.aneeqrider.common.Common;
import com.aneeq.aneeqrider.models.Token;

public class MyFirebaseIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("Refreshed token: ", ""+refreshedToken);
        updateTokenToServer(refreshedToken); // When have refresh token, we need update to our realtime database

    }

    private void updateTokenToServer(String refreshedToken) {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);

        Token token = new Token(refreshedToken);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) // if already login , mus update Token
            tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
            .setValue(token);


    }
}

