package com.halla.qrcodereader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private IntentIntegrator qrScan;
    private TextView carIdView;
    private TextView dateView;
    private TextView destinationView;
    private TextView companyView;
    private TextView carNumberView;
    private TextView carTypeView;
    private TextView driverView;
    private TextView driverMobileView;
    private TextView isCheckView;
    private Button scanButton;
    private Button checkButton;
    private Button signButton;
    private ImageView signView;

    String ischeck;
    String FETCH_DT;
    String CO_NM;
    String AREA_NM;
    String CAR_NO;
    String VIH_TON_KIND;
    String DRIVER_NAME;
    String DRV_MOBILE;
    String userId;
    String userKey;
    String setResult;
    String signUrl;

    Bitmap signImage;

    int isSelf;

    String carid = "";
    Handler h;

    BackPressHandler backPressHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = getSharedPreferences("login", MODE_PRIVATE);
        backPressHandler = new BackPressHandler(this);

        carIdView = findViewById(R.id.carIdView);
        dateView = findViewById(R.id.dateView);
        destinationView = findViewById(R.id.destinationView);
        companyView = findViewById(R.id.companyView);
        carNumberView = findViewById(R.id.carNumberView);
        carTypeView = findViewById(R.id.carTypeView);
        driverView = findViewById(R.id.driverView);
        driverMobileView = findViewById(R.id.phoneView);
        isCheckView = findViewById(R.id.isCheckView);

        h = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    carIdView.setText(carid);
                    dateView.setText(FETCH_DT);
                    destinationView.setText(AREA_NM);
                    companyView.setText(CO_NM);
                    carNumberView.setText(CAR_NO);
                    carTypeView.setText(VIH_TON_KIND);
                    driverView.setText(DRIVER_NAME);
                    driverMobileView.setText(DRV_MOBILE);
                    if (ischeck.equals("Y")) {
                        isCheckView.setText("확인");
                        isCheckView.setBackgroundColor(Color.parseColor("#006dc6"));
                        isCheckView.setTextColor(Color.parseColor("#ffffff"));
                        checkButton.setEnabled(false);
                        checkButton.setText("이미 처리된 배차입니다.");
                        signButton.setEnabled(true);
                    } else if (ischeck.equals("N")) {
                        isCheckView.setText("미확인");
                        isCheckView.setBackgroundColor(Color.parseColor("#cf1738"));
                        isCheckView.setTextColor(Color.parseColor("#ffffff"));
                        checkButton.setEnabled(true);
                        checkButton.setText("도착 확인");
                        signButton.setEnabled(true);
                    } else {
                        isCheckView.setText(ischeck);
                        isCheckView.setTextColor(Color.parseColor("#000000"));
                        isCheckView.setBackgroundColor(Color.parseColor("#eeeeee"));
                        checkButton.setEnabled(false);
                        checkButton.setText("회원님의 배차 번호가 아닙니다.");
                        signButton.setEnabled(false);
                    }
                } else if (msg.what == 2) {
                    callErrorDialog();
                } else if (msg.what == 400) {
                    if (signImage == null)
                        signView.setImageResource(R.drawable.cant_sign);
                    else
                        signView.setImageBitmap(signImage);
                } else
                    callArriveDialog();
            }
        };

        Intent intent = getIntent();
        userId = sharedPreferences.getString("id", "");
        userKey = sharedPreferences.getString("key", "");
        isSelf = sharedPreferences.getInt("isself", 0);
        qrScan = new IntentIntegrator(this);
        qrScan.setCaptureActivity(ZxingActivity.class);
        qrScan.setOrientationLocked(false);
        if (isSelf != 0) {
            if (NetworkStatus.getConnectivityStatus(getApplicationContext()) == NetworkStatus.TYPE_NOT_CONNECTED) {
                callErrorDialog();
            } else
                qrScan.initiateScan();
        } else {
            carid = intent.getExtras().getString("selfcarid");
            new GetCarInfo(h).start();
        }

        scanButton = findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkStatus.getConnectivityStatus(getApplicationContext()) == NetworkStatus.TYPE_NOT_CONNECTED)
                    callErrorDialog();
                else
                    qrScan.initiateScan();
            }
        });

        checkButton = findViewById(R.id.checkButton);
        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkStatus.getConnectivityStatus(getApplicationContext()) == NetworkStatus.TYPE_NOT_CONNECTED)
                    callErrorDialog();
                else
                    new SetCarArrive(h).start();
            }
        });

        signButton = findViewById(R.id.signButton);
        signButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SignDownloader(h, signUrl).start();
                callSignDialog();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
            } else {
                carid = result.getContents();
                new GetCarInfo(h).start();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        backPressHandler.onBackPressed("뒤로가기 버튼 한번 더 누르면 종료", 2000);
    }

    class GetCarInfo extends Thread {
        String strLine;
        Handler h;

        public GetCarInfo(Handler h) {
            this.h = h;
        }

        public void run() {
            try {
                if (NetworkStatus.getConnectivityStatus(getApplicationContext()) == NetworkStatus.TYPE_NOT_CONNECTED)
                    h.sendEmptyMessage(2);
                else {
                    URL url = new URL("http://whkram.meister.co.kr:9000/AM_GATEService/getCarInfo");
                    Map<String, String> params = new LinkedHashMap<>();
                    params.put("userId", userId);
                    params.put("userKey", userKey);
                    params.put("carfetchno", carid);

                    StringBuilder postData = new StringBuilder();
                    for (Map.Entry<String, String> param : params.entrySet()) {
                        if (postData.length() != 0) postData.append('&');
                        postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                        postData.append('=');
                        postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                    }
                    byte[] postDataBytes = postData.toString().getBytes("UTF-8");

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(postDataBytes); // POST 호출

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));


                    strLine = in.readLine();
                    if (strLine.equals("[")) {
                        carid = "회원님의 배차 번호가 아닙니다.";
                        FETCH_DT = "다시 입력하세요";
                        CO_NM = "";
                        AREA_NM = "";
                        CAR_NO = "";
                        VIH_TON_KIND = "";
                        DRIVER_NAME = "";
                        DRV_MOBILE = "";
                        ischeck = "";
                        signUrl = "";
                    } else {
                        JSONArray jsonArray = new JSONArray(strLine);
                        JSONObject jsonObject = jsonArray.getJSONObject(0);

                        FETCH_DT = jsonObject.getString("FETCH_DT");
                        CO_NM = jsonObject.getString("CO_NM");
                        AREA_NM = jsonObject.getString("AREA_NM");
                        CAR_NO = jsonObject.getString("CAR_NO");
                        VIH_TON_KIND = jsonObject.getString("VIH_TON_KIND");
                        DRIVER_NAME = jsonObject.getString("DRIVER_NAME");
                        DRV_MOBILE = jsonObject.getString("DRV_MOBILE");
                        ischeck = jsonObject.getString("CHECKED");
                        signUrl = jsonObject.getString("SIGN_IMG");

                    }
                    h.sendEmptyMessage(0);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class SetCarArrive extends Thread {
        String strLine;
        Handler h;

        public SetCarArrive(Handler h) {  // setCarArrive 쓰레드
            this.h = h;
        }

        public void run() {
            try {
                URL url = new URL("http://whkram.meister.co.kr:9000/AM_GATEService/setCarArrive");
                Map<String, String> params = new LinkedHashMap<>();
                params.put("userId", userId);
                params.put("userKey", userKey);
                params.put("carfetchno", carid);

                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, String> param : params.entrySet()) {
                    if (postData.length() != 0) postData.append('&');
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }
                byte[] postDataBytes = postData.toString().getBytes("UTF-8");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes); // POST 호출;

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

                strLine = "";
                while (in.readLine() != null) {
                    String readed = in.readLine();
                    strLine += readed;
                }
                if (strLine.contains("setCarArrive fail"))
                    setResult = "fail";
                else
                    setResult = "ok";
                h.sendEmptyMessage(1);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class SignDownloader extends Thread {
        Handler h;
        String url;

        public SignDownloader(Handler h, String url) {
            this.h = h;
            this.url = url;
        }

        public void run() {
            downloadSign(url);
            h.sendEmptyMessage(400);
        }
    }

    void callArriveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("도착 확인 결과");
        if (setResult.equals("fail")) {
            builder.setMessage("처리 권한이 없습니다.");
        } else {
            builder.setMessage("도착 확인 되었습니다.");
            isCheckView.setText("확인");
            isCheckView.setBackgroundColor(Color.parseColor("#006dc6"));
            isCheckView.setTextColor(Color.parseColor("#ffffff"));
            checkButton.setEnabled(false);
            checkButton.setText("이미 처리된 배차입니다.");
        }
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }

    void callErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("인터넷 오류");
        builder.setMessage("인터넷 연결이 되어있지 않습니다.");
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        carIdView.setText("");
                        dateView.setText("");
                        destinationView.setText("");
                        companyView.setText("");
                        carNumberView.setText("");
                        carTypeView.setText("");
                        driverView.setText("");
                        driverMobileView.setText("");
                        isCheckView.setBackgroundColor(Color.parseColor("#eeeeee"));
                        isCheckView.setTextColor(Color.parseColor("#000000"));
                        isCheckView.setText("");
                        checkButton.setEnabled(false);
                        checkButton.setText("인터넷 오류");
                    }
                });
        builder.show();
    }

    void callSignDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (signUrl.equals("")) {
            builder.setTitle("서명 확인");
            builder.setMessage("서명이 존재하지 않습니다!");
            builder.setPositiveButton("확인",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            builder.show();
        } else {
            signView = new ImageView(this);
            builder.setTitle("서명 확인");
            builder.setView(signView);
            builder.setPositiveButton("확인",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            builder.show();
        }
    }

    void downloadSign(String url) {
        URL signUrl = null;
        signImage = null;
        try {
            signUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) signUrl.openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            signImage = BitmapFactory.decodeStream(is);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


