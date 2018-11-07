package com.example.kyungsoo.thinkwise;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    public static int PICK_FILE = 1; // 1이면 파일선택 activity 실행

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void file_select(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        startActivityForResult(intent, PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                // 파일선택 Activity
                case 1:
                    Uri uri = data.getData();
                    String fileContent = readTextFile(uri);
                    Intent chat_result_main = new Intent(MainActivity.this, chat_result_main.class);
                    chat_result_main.putExtra("fileContent", fileContent);

                    // chat_result_main 열기
                    startActivity(chat_result_main);
                    break;
                default:
                    break;
            }
        } else {
            Toast.makeText(getApplicationContext(), "파일이 선택되지 않았습니다.", Toast.LENGTH_LONG).show();
        }
    }

    // 파일 읽기 메소드
    private String readTextFile(Uri uri) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "대화내용 읽기 오류", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }
}