package com.example.moneychange1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class MainActivity extends AppCompatActivity {

    private TextView tvJPNtoKRW;
    private TextView tvVTNtoKRW;
    private TextView tvEURtoKRW;
    private TextView tvUSDtoKRW;
    private Retrofit retrofit;
    private ExchangeRateService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvJPNtoKRW = findViewById(R.id.tvJPNtoKRW);
        tvVTNtoKRW = findViewById(R.id.tvVTNtoKRW);
        tvEURtoKRW = findViewById(R.id.tvEURtoKRW);
        tvUSDtoKRW = findViewById(R.id.tvUSDtoKRW);

        retrofit = new Retrofit.Builder()
                .baseUrl("https://v6.exchangerate-api.com/v6/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(ExchangeRateService.class);
    }

    public void fetchRates(View view) {
        Log.d("MainActivity", "Fetch Rates Button Clicked");
        fetchCurrencyAmountAndConvert("JPY", "1000jpn");
        fetchCurrencyAmountAndConvert("VND", "10000vtn");
        fetchCurrencyAmountAndConvert("EUR", "5eur");
        fetchCurrencyAmountAndConvert("USD", "10usd");
    }

    private void fetchCurrencyAmountAndConvert(String currencyCode, String field) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("money").document("5s2JLMcIzivtRoFp0xMm")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double amount = documentSnapshot.getDouble(field);
                        if (amount != null) {
                            Log.d("FirebaseData", currencyCode + " amount fetched: " + amount);
                            getExchangeRate(currencyCode, amount);
                        } else {
                            Log.e("FirebaseData", "Amount for " + field + " is null");
                        }
                    } else {
                        Log.e("FirebaseData", "Document does not exist");
                    }
                })
                .addOnFailureListener(e -> Log.e("FirebaseData", "Error fetching document", e));
    }

    private void getExchangeRate(String currencyCode, double amount) {
        String apiKey = "6e761c76a1d8ec90bd806478"; // 실제 API 키 사용
        service.getExchangeRates(apiKey, currencyCode).enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Double> rates = response.body().getConversionRates();
                    Double rateToKRW = rates.get("KRW");
                    if (rateToKRW != null) {
                        double convertedAmount = amount * rateToKRW;
                        Log.d("APIResponse", currencyCode + " to KRW rate fetched: " + rateToKRW);
                        updateUI(currencyCode, convertedAmount);
                    } else {
                        Log.e("APIError", "Rate for KRW not found or rates map is null");
                    }
                } else {
                    Log.e("APIError", "Response not successful or response body is null. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                Log.e("APIError", "Exchange rate API call failed", t);
            }
        });
    }

    private void updateUI(String currencyCode, double convertedAmount) {
        String text = String.format("%s to KRW: %.2f", currencyCode, convertedAmount);
        runOnUiThread(() -> {
            switch (currencyCode) {
                case "JPY":
                    tvJPNtoKRW.setText(text);
                    break;
                case "VND":
                    tvVTNtoKRW.setText(text);
                    break;
                case "EUR":
                    tvEURtoKRW.setText(text);
                    break;
                case "USD":
                    tvUSDtoKRW.setText(text);
                    break;
            }
        });
    }
}

interface ExchangeRateService {
    @GET("{apiKey}/latest/{currency}")
    Call<ExchangeRateResponse> getExchangeRates(
            @Path("apiKey") String apiKey,
            @Path("currency") String currency);
}

class ExchangeRateResponse {
    private Map<String, Double> conversion_rates;

    public Map<String, Double> getConversionRates() {
        return conversion_rates;
    }
}
