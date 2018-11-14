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
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
        Log.i("whyexit", "create complete");
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
                            Log.i("whyexit", "split start");
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
        Log.v("whyexit", String.valueOf(split_line.length));

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

    // JSON형태로 받아오기!!!
    public void lastPOST() throws IOException, JSONException {
        String input = "key=3249915959609597769&text=" + temp;


        try{
            /* set up */
            URL naver = new URL("http://api.adams.ai/datamixiApi/keywordextract");
            HttpURLConnection urlConn = (HttpURLConnection)naver.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setDoInput(true);
            urlConn.setRequestProperty("Accept", "application/json;charset=UTF-8");
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
                try {
                    JSONParser jsonParser = new JSONParser();
                    //JSONObject jsonObject = JSONObject.fromObject(inputLine);
                    JSONObject jsonObject = (JSONObject)jsonParser.parse(inputLine);
                    Log.i("readStream", "5555");
                    JSONArray memberArray = (JSONArray)jsonObject.get("return_object");

                    Log.i("readStream", "111111");
                    for(int i=0 ; i < memberArray.size() ; i++){
                        JSONObject tempObj = (JSONObject) memberArray.get(i);
                        Log.i("readStream", ""+(i+1)+"번째 멤버의 term: "+tempObj.get("term"));
                        Log.i("readStream", ""+(i+1)+"번째 멤버의 weight : "+tempObj.get("weight"));
                        System.out.println("----------------------------");
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Log.i("readStream", inputLine);
            }

            br.close();
        }
        catch (Exception e) {
            Log.i("readStream", e.toString());
        }
    }

    // 뒤로가기 버튼 눌렀을 때
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i("whyexit", "back pressed..exit");
        finish();
    }

    @Override
    protected void onDestroy() {
        Log.i("whyexit", "on Destroy - subactivity");
        super.onDestroy();
    }
}
