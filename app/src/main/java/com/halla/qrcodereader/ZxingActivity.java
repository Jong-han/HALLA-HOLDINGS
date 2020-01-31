package com.halla.qrcodereader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.journeyapps.barcodescanner.CaptureActivity;

public class ZxingActivity extends CaptureActivity {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    BackPressHandler backPressHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backPressHandler = new BackPressHandler(this);
        sharedPreferences = getSharedPreferences("login",MODE_PRIVATE);
        editor = sharedPreferences.edit();
        String welcome = sharedPreferences.getString("company","") + " " + sharedPreferences.getString("name","") + "님" + "\nQR코드 또는 바코드를 읽어주세요!";

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        linearLayout.setLayoutParams(layoutParams);

        TextView title_view = new TextView(this);
        LinearLayout.LayoutParams layoutParams2  = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams2.weight = 12;
        layoutParams2.gravity = Gravity.TOP | Gravity.CENTER;
        layoutParams2.topMargin = 100;
        title_view.setLayoutParams(layoutParams2);
        title_view.setTextColor(Color.parseColor("#D3D3D3"));
        title_view.setTextSize(30);
        title_view.setText(welcome);

        Button selfButton = new Button(this);
        LinearLayout.LayoutParams layoutParams3  = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0);
        layoutParams3.weight = 1;
        layoutParams3.gravity = Gravity.BOTTOM | Gravity.CENTER;
        layoutParams3.bottomMargin= 100;
        selfButton.setLayoutParams(layoutParams3);
        selfButton.setPadding(10,0,10,0);
        selfButton.setTextSize(30);
        selfButton.setBackgroundColor(Color.parseColor("#D3D3D3"));
        selfButton.setText("배차번호 수동 입력");

        linearLayout.addView(title_view);
        linearLayout.addView(selfButton);
        this.addContentView(linearLayout,layoutParams);

        selfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callSelfDialog();
            }
        });
    }

    @Override
    public void onBackPressed() {
        backPressHandler.onBackPressed("뒤로가기 버튼 한번 더 누르면 종료", 2000);
    }

    void callSelfDialog()
    {
        final EditText editText = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("배차번호 입력");
        builder.setMessage("배차번호를 입력하세요.");
        builder.setView(editText);
        builder.setPositiveButton("완료",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editText.getText().toString();
                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                        intent.putExtra("selfcarid",text);
                        editor.putInt("isself",0);
                        editor.commit();
                        intent.putExtra("isSelf",0);
                        startActivity(intent);
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.show();
    }
}
