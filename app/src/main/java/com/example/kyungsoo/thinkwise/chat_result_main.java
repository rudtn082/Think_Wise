package com.example.kyungsoo.thinkwise;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Calendar;

public class chat_result_main extends AppCompatActivity {
    static final String[] LIST_MENU = {"LIST1", "LIST2", "LIST3", "LIST4", "LIST5", "LIST6", "LIST7", "LIST8"} ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_result_main);
        Intent intent = new Intent(this.getIntent());
        String fileContent = intent.getStringExtra("fileContent");

        // 라인으로 split하기
        String[] split_line = fileContent.split("\n");
        String whole_chat_content = "";

        // 단어로 split해서 대화 내용만 합치기
        for(int i = 2; i < split_line.length; i++) {
            if(split_line[i] == "") continue;
            String[] split_ws = split_line[i].split("\\s");

            if(split_ws.length < 7) continue;
            else {
                for(int j = 7; j < split_ws.length; j++) {
                    whole_chat_content = whole_chat_content.concat(split_ws[j]);
                    whole_chat_content = whole_chat_content.concat(" ");
                }
                whole_chat_content = whole_chat_content.concat("\n");
            }
        }


        // 리스트뷰 생성
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, LIST_MENU) ;

        ListView listview = (ListView) findViewById(R.id.listview1) ;
        listview.setAdapter(adapter) ;
    }
}
