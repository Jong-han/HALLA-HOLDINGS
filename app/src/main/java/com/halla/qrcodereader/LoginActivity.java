package com.halla.qrcodereader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    Button loginButton;
    EditText idText;
    EditText pwText;
    CheckBox isSaveCheck;

    String DEALER_NAME;
    String USER_NAME;
    String USER_KEY;
    String SAVED_ID;
    String SAVED_PW;
    Boolean isChecked;

    Handler h;

    BackPressHandler backPressHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        backPressHandler = new BackPressHandler(this);
        SharedPreferences sharedPreferences = getSharedPreferences("login", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        isChecked = sharedPreferences.getBoolean("check",false);
        SAVED_ID = sharedPreferences.getString("save_id","");
        SAVED_PW = sharedPreferences.getString("save_pw","");
        loginButton = findViewById(R.id.loginButton);
        idText = findViewById(R.id.id_editText);
        pwText = findViewById(R.id.pw_editText);
        isSaveCheck = findViewById(R.id.isSaveChecker);

        isSaveCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isSaveCheck.isChecked()){
                    editor.putBoolean("check",true);
                    editor.putString("save_id",idText.getText().toString());
                    editor.putString("save_pw",pwText.getText().toString());
                    editor.commit();
                }else{
                    editor.putBoolean("check",false);
                    editor.commit();
                }
            }
        });

        if(isChecked) {
            isSaveCheck.setChecked(true);
            idText.setText(SAVED_ID);
            pwText.setText(SAVED_PW);
        }
        else {
            isSaveCheck.setChecked(false);
            idText.setText("");
            pwText.setText("");
        }

        h = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    editor.putString("key", USER_KEY);
                    editor.putString("id", idText.getText().toString());
                    editor.putString("pw",pwText.getText().toString());
                    editor.putString("save_id", idText.getText().toString());
                    editor.putString("save_pw",pwText.getText().toString());
                    editor.putString("company", DEALER_NAME);
                    editor.putString("name", USER_NAME);
                    editor.putInt("isself", 1);
                    editor.commit();
                    intent.putExtra("isSelf", 1);
                    startActivity(intent);
                    finish();
                } else if (msg.what == 1) {
                    callLoginErrorDialog();
                }
            }
        };

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkStatus.getConnectivityStatus(getApplicationContext()) == NetworkStatus.TYPE_NOT_CONNECTED) {
                    callInternetErrorDialog();
                } else
                    new GetCarArriveLogin(h).start();
            }
        });
    }

    @Override
    public void onBackPressed() {
        backPressHandler.onBackPressed("뒤로가기 버튼 한번 더 누르면 종료", 2000);
    }

    class GetCarArriveLogin extends Thread {
        String strLine;
        Handler h;

        public GetCarArriveLogin(Handler h) {
            this.h = h;
        }

        public void run() {
            try {
                URL url = new URL("http://whkram.meister.co.kr:9000/AM_GATEService/getCarArriveLogin");
                Map<String, String> params = new LinkedHashMap<>();
                params.put("userId", idText.getText().toString());
                params.put("pw", pwText.getText().toString());

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
                if (strLine.contains("DEALER_NAME")) {
                    JSONArray jsonArray = new JSONArray(strLine);
                    JSONObject jsonObject = jsonArray.getJSONObject(0);

                    DEALER_NAME = jsonObject.getString("DEALER_NAME");
                    USER_KEY = jsonObject.getString("USER_KEY");
                    USER_NAME = jsonObject.getString("USER_NAME");

                    h.sendEmptyMessage(0);
                } else
                    h.sendEmptyMessage(1);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    void callLoginErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("로그인 오류");
        builder.setMessage("회원정보가 올바르지 않습니다.");
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.show();
    }

    void callInternetErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("인터넷 오류");
        builder.setMessage("인터넷 연결이 되어있지 않습니다.");
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.show();
    }
}
