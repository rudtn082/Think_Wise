package com.example.kyungsoo.thinkwise;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class chat_result_main extends AppCompatActivity {
    private Handler mHandler; // postDelayed사용을 위한 handler
    private ProgressDialog mProgressDialog; // 프로그레스바 변수
    static final String[] LIST_MENU = {"LIST1", "LIST2", "LIST3", "LIST4", "LIST5", "LIST6", "LIST7", "LIST8"} ;
    String temp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_result_main);


        mHandler = new Handler();

        // 프로그레스바 생성
        mProgressDialog = ProgressDialog.show(chat_result_main.this,"","대화 내용을 분석 중입니다...",true);

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mHandler.postDelayed( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            chat_split();
                        }
                        catch ( Exception e )
                        {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "파읽읽기 오류", Toast.LENGTH_LONG).show();
                        } finally {
                            // 프로그레스 종료
                                        mProgressDialog.dismiss();

                            // lastPOST
                            Thread threadRecog = new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        lastPOST();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            threadRecog.start();
                        }
                    }
                }, 500);
            }
        } );

        // 리스트뷰 생성
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, LIST_MENU) ;



        ListView listview = (ListView) findViewById(R.id.listview1) ;
        listview.setAdapter(adapter) ;
    }

    private void chat_split() {
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
        temp = whole_chat_content;
    }

    // 뒤로가기 버튼 눌렀을 때
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    // JSON형태로 받아오기!!!
    public void lastPOST() throws IOException, JSONException {
        //byte[] strs = temp.getBytes("UTF-8");
        String a = new String(temp.getBytes("UTF-8"),"UTF-8");
        String input = "key=3249915959609597769&text=" + a;


        try{
            /* set up */
            URL naver = new URL("http://api.adams.ai/datamixiApi/keywordextract");
            HttpURLConnection urlConn = (HttpURLConnection)naver.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setDoInput(true);
            urlConn.setRequestProperty("Accept", "application/json");
            urlConn.setRequestMethod("POST");

            Log.i("readStream", "1");
            /* write */
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(urlConn.getOutputStream()));
            bw.write(input);
            bw.flush();
            bw.close();
            Log.i("readStream", "3");

            // 200 성공코드
            // 400 문법에러
            BufferedReader br;
            if (urlConn.getResponseCode() == 200) {
                // 성공시 처리
                Log.i("readStream", String.valueOf(urlConn.getResponseCode()));
                /* read */
                br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            }else{
                // 실패시 처리
                Log.i("readStream", String.valueOf(urlConn.getResponseCode()));
                /* read */
                br = new BufferedReader(new InputStreamReader(urlConn.getErrorStream()));
            }
            String inputLine;
            while((inputLine = br.readLine()) != null){
                Log.i("readStream", inputLine);
            }
            br.close();
        }
        catch (Exception e) {
            Log.i("readStream", e.toString());
        }
    }
}
