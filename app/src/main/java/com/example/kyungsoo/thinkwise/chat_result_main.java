package com.example.kyungsoo.thinkwise;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class chat_result_main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_result_main);
        Intent intent=new Intent(this.getIntent());
        String fileContent = intent.getStringExtra("fileContent");

        /////////// 대화내용 확인용 지워야함 //////////
        Toast.makeText(this, fileContent, Toast.LENGTH_LONG).show();
    }
}
