package com.example;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.model.ChecksumRequest;
import com.example.model.ChecksumResponse;
import com.example.webservice.APIClient;
import com.example.webservice.APIInterface;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPGService;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.Constants.BASE_URL;

public class MainActivity extends AppCompatActivity {

    // https://github.com/Paytm-Payments/Paytm_App_Checksum_Kit_PHP
    private TextView textViewPrice;
    private ProgressBar progressBar;
    private LinearLayout successView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        Button buttonBuy = findViewById(R.id.buttonBuy);
        textViewPrice = findViewById(R.id.textViewPrice);
        progressBar = findViewById(R.id.progressBar);
        successView = findViewById(R.id.successView);
        buttonBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStartTransaction();
            }
        });

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS}, 101);

        }


    }

    public void onStartTransaction() {

        getCheckSumFromServer();
    }

    private void getCheckSumFromServer() {

        final ChecksumRequest paytmRequestParam = new ChecksumRequest(
                Constants.M_ID,
                Constants.CHANNEL_ID,
                textViewPrice.getText().toString(),
                Constants.WEBSITE,
                Constants.CALLBACK_URL,
                Constants.INDUSTRY_TYPE_ID
        );


        APIInterface apiService = APIClient.getClient(BASE_URL).create(APIInterface.class);

        progressBar.setVisibility(View.VISIBLE);

        Call<ChecksumResponse> call = apiService.getChecksumRespose(paytmRequestParam.getmId(),
                paytmRequestParam.getOrderId(),
                paytmRequestParam.getCustId(),
                paytmRequestParam.getChannelId(),
                paytmRequestParam.getTxnAmount(),
                paytmRequestParam.getWebsite(),
                paytmRequestParam.getCallBackUrl(),
                paytmRequestParam.getIndustryTypeId()
        );


        //making the call to generate checksum
        call.enqueue(new Callback<ChecksumResponse>() {
            @Override
            public void onResponse(Call<ChecksumResponse> call, Response<ChecksumResponse> response) {
                Log.e("TESTING ", " onResponse  " + response.body());
                initializePaytmPayment(response.body().getChecksumHash(), paytmRequestParam);
            }

            @Override
            public void onFailure(Call<ChecksumResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }


    public void initializePaytmPayment(String checksumHash, ChecksumRequest paytmRequestParam) {

        PaytmPGService Service = PaytmPGService.getStagingService("");

        HashMap<String, String> paramMap = new HashMap<String, String>();
        //these are mandatory parameters
        paramMap.put("MID", paytmRequestParam.getmId()); //MID provided by paytm
        paramMap.put("ORDER_ID", paytmRequestParam.getOrderId());
        paramMap.put("CUST_ID", paytmRequestParam.getCustId());
        paramMap.put("INDUSTRY_TYPE_ID", "Retail");
        paramMap.put("CHANNEL_ID", paytmRequestParam.getChannelId());
        paramMap.put("TXN_AMOUNT", paytmRequestParam.getTxnAmount());
        paramMap.put("WEBSITE", paytmRequestParam.getWebsite());
        paramMap.put("CALLBACK_URL", paytmRequestParam.getCallBackUrl());
        paramMap.put("CHECKSUMHASH", checksumHash);


        progressBar.setVisibility(View.GONE);

        PaytmOrder Order = new PaytmOrder(paramMap);

        Service.initialize(Order, null);
        // start payment service call here
        Service.startPaymentTransaction(this, true, true, new PaytmPaymentTransactionCallback() {
            @Override
            public void onTransactionResponse(Bundle inResponse) {
                Log.e("TESTING ", " onTransactionResponse  " + inResponse.toString());

                successView.setVisibility(View.VISIBLE);
            }

            @Override
            public void networkNotAvailable() {
                Log.e("TESTING ", " networkNotAvailable  ");
            }

            @Override
            public void clientAuthenticationFailed(String inErrorMessage) {
                Log.e("TESTING ", " clientAuthenticationFailed  ");
            }

            @Override
            public void someUIErrorOccurred(String inErrorMessage) {
                Log.e("TESTING ", " someUIErrorOccurred  ");
            }

            @Override
            public void onErrorLoadingWebPage(int iniErrorCode, String inErrorMessage, String inFailingUrl) {
                Log.e("TESTING ", " onErrorLoadingWebPage  ");
            }

            @Override
            public void onBackPressedCancelTransaction() {
                Log.e("TESTING ", " onBackPressedCancelTransaction  ");
            }

            @Override
            public void onTransactionCancel(String inErrorMessage, Bundle inResponse) {
                Log.e("TESTING ", " onTransactionCancel  ");
            }
        });

    }

}
