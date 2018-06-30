package com.aneeq.aneeqrider.remote;

import com.aneeq.aneeqrider.models.DataMessage;
import com.aneeq.aneeqrider.models.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAtBxuLGg:APA91bE4NGVZD0LQknIoMiaoG-j1SyxNTFGqy2Qs0zo0q7q2kSIego931pQE6CzAxcwBwP5H2F4nY5TrUG8GLEZwIdZQ84Vsz64MP1rO9O3uJ9SheujNyQLs9jfJ9KeXGn3wAuR0iG_L"
    })
    @POST("fcm/send")
    Call<FCMResponse> sendMessage(@Body DataMessage body);
}

