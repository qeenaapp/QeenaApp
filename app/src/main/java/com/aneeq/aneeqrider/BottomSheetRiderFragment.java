package com.aneeq.aneeqrider;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.aneeq.aneeqrider.common.Common;
import com.aneeq.aneeqrider.remote.IGoogleAPI;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BottomSheetRiderFragment extends BottomSheetDialogFragment {

    String mLocation, mDestination;

    boolean isTapOnMap;

    IGoogleAPI mService;
    TextView txtCalculate, txtLocation, txtDestination;

    public static BottomSheetRiderFragment newInstances(String location, String destination, boolean isTapOnMap) {
        BottomSheetRiderFragment f = new BottomSheetRiderFragment();
        Bundle args = new Bundle();
        args.putString("location", location);
        args.putString("destination", destination);
        args.putBoolean("isTapOnMap", isTapOnMap);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation = getArguments().getString("location");
        mDestination = getArguments().getString("destination");
        Log.e("14", mLocation +" "+ mDestination);
        isTapOnMap = getArguments().getBoolean("isTapOnMap");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_rider, container, false);
        txtLocation = view.findViewById(R.id.txtLocation);
        txtDestination = view.findViewById(R.id.txtDestination);
        txtCalculate = view.findViewById(R.id.txtCalculate);

        mService = Common.getGoogleService();
        getPrice(mLocation, mDestination);

        if (!isTapOnMap) {
            // Call this fragment from place autocomplete Text View
            txtLocation.setText(mLocation);
            txtDestination.setText(mDestination);
        }

        return view ;

    }

    private void getPrice(String mLocation, String mDestination) {
        String requestUrl = null;
        try{
            requestUrl = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"
                    +"transit_routing_preference=less_driving&"
                    +"origin="+mLocation+"&"
                    +"destination="+mDestination+"&"
                    +"key="+getResources().getString(R.string.google_browser_key);
            Log.e("LINK", requestUrl);
            Log.e("15", mLocation +" "+ mDestination);
            mService.getPath(requestUrl).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    Log.e("response", response.toString());
                    // get Object
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray routes = jsonObject.getJSONArray("routes");

                        JSONObject object = routes.getJSONObject(0);
                        JSONArray legs = object.getJSONArray("legs");

                        JSONObject legsObject = legs.getJSONObject(0);

                        // Get distance
                        JSONObject distance = legsObject.getJSONObject("distance");
                        String distance_text = distance.getString("text");
                        Log.e("distance_text", distance_text);
                        // Use regex to extract double from string
                        // This regex will remove all text not digit
                        Double distance_value = Double.parseDouble(distance_text.replaceAll("[^0-9\\\\?]+", ""));

                        // Get time
                        JSONObject time = legsObject.getJSONObject("duration");
                        String time_text = time.getString("text");
                        Log.e("time_text", time_text);
                        Integer time_value = Integer.parseInt(time_text.replaceAll("\\D+", ""));

                        String final_calculate = String.format("%s + %s = $%.2f ", distance_text, time_text,
                                Common.getPrice(distance_value, time_value));
                        Log.e("final_calculate", final_calculate);
                        txtCalculate.setText(final_calculate);

                        if (isTapOnMap) {
                            String start_address = legsObject.getString("start_address");
                            String end_address = legsObject.getString("end_address");

                            txtLocation.setText(start_address);
                            txtDestination.setText(end_address);



                        }

                    } catch (JSONException e) {
                        Log.e("jsonException", e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("Failure" , t.getMessage());

                }
            });



        } catch (Exception e) {
            Log.e("Exception" , e.getMessage());
        }

    }
}
