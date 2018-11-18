package com.example.kyungsoo.thinkwise;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
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
import java.util.ArrayList;
import java.util.HashMap;

public class chat_result_main extends AppCompatActivity {
    ArrayList<HashMap<String,String>> LIST_MENU; // 리스트 뷰 변수
    ListView listview; // 리스트뷰
    SimpleAdapter adapter; // 리스트뷰 어댑터
    private Handler mHandler; // postDelayed사용을 위한 handler
    private ProgressDialog mProgressDialog; // 프로그레스바 변수
    String temp = null;
    private int mProgressStatus = 0;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_result_main);

        // 뒤로가기 버튼 생성
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
                            Log.e("LOG",e.toString());
                            Toast.makeText(getApplicationContext(), "내용분석 오류", Toast.LENGTH_LONG).show();
                        } finally {
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

                            // 분석이 끝나면 ListView를 업데이트
                            do {
                                // 프로그레스 종료
                                mProgressDialog.dismiss();
                                adapter.notifyDataSetChanged();
                            }while (threadRecog.getState() != Thread.State.TERMINATED);
                        }
                    }
                }, 500);
            }
        } );


        // ListView 생성
        LIST_MENU = new ArrayList<HashMap<String, String>>();

        adapter = new SimpleAdapter(this, LIST_MENU, android.R.layout.simple_list_item_2,
                new String[] {"item1", "item2"}, new int[] {android.R.id.text1, android.R.id.text2});
        listview = (ListView)findViewById(R.id.listview1) ; // 리스트뷰
        listview.setAdapter(adapter);


        // ListView 객체의 특정 아이템 클릭시 처리 추가
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                HashMap<String, String> item = (HashMap<String, String>) adapterView.getItemAtPosition(position);
                String selected_item = item.get("item1");

                // 클릭한 아이템의 문자열을 가져와서
                //String selected_item = (String)adapterView.getItemAtPosition(position);


                Intent Analysis_result = new Intent(getApplicationContext(), com.example.kyungsoo.thinkwise.Analysis_result.class);
                Analysis_result.putExtra("data_string", selected_item);
                startActivity(Analysis_result);
            }
        });
    }

    private void chat_split() {
        String fileContent = MainActivity.getContentstring();

        // 라인으로 split하기
        String[] split_line = fileContent.split("\n");
        String whole_chat_content = "";

        int i;

        // 라인이 10만줄 이상일 때
        if(split_line.length > 100000) {
            // 단어로 split해서 대화 내용만 합치기
            for(i = split_line.length-100000; i < split_line.length; i++) {
                if(split_line[i] == "") continue;
                String[] split_ws = split_line[i].split("\\s");

                if(split_ws.length < 7) continue;
                else {
                    for(int j = 7; j < split_ws.length; j++) {
                        whole_chat_content = whole_chat_content + split_ws[j] + "\n";
                    }
                }
            }
        }
        //  라인이 10만줄 이하일 때
        else {
            // 단어로 split해서 대화 내용만 합치기
            for(i = 2; i < split_line.length; i++) {
                if(split_line[i] == "") continue;
                String[] split_ws = split_line[i].split("\\s");

                if(split_ws.length < 7) continue;
                else {
                    for(int j = 7; j < split_ws.length; j++) {
                        whole_chat_content = whole_chat_content + split_ws[j] + "\n";
                    }
                }
            }
        }
        temp = whole_chat_content;
    }

    // JSON형태로 받아오기!!!
    public void lastPOST() throws IOException, JSONException {
        String input = "key=3249915959609597769&text=" + temp;

        try{
            /* set up */
            URL url = new URL("http://api.adams.ai/datamixiApi/keywordextract");
            HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setDoInput(true);
            urlConn.setRequestProperty("Accept", "application/json;charset=UTF-8");
            urlConn.setRequestMethod("POST");

            /* write */
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(urlConn.getOutputStream()));
            bw.write(input);
            bw.flush();
            bw.close();

            // 200 성공코드 , 400 문법에러
            BufferedReader br;
            if (urlConn.getResponseCode() == 200) {
                // 성공시 처리
                br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            }else{
                // 실패시 처리
                br = new BufferedReader(new InputStreamReader(urlConn.getErrorStream()));
            }

            String inputLine;
            while((inputLine = br.readLine()) != null){
                try {
                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObject = (JSONObject)jsonParser.parse(inputLine);
                    JSONArray memberArray = (JSONArray)jsonObject.get("return_object");

                    // 결과값이 20개 보다 많을 경우
                    if(memberArray.size() > 20) {
                        for(int i=0 ; i <= 20; i++){
                            JSONObject tempObj = (JSONObject) memberArray.get(i);
                            String[] tempstr = tempObj.get("term").toString().split("\\|");

                            HashMap<String,String> item = new HashMap<>();
                            item.put("item1", tempstr[0]);
                            item.put("item2", "빈도수 : " + tempstr[1].substring(0,5));
                            LIST_MENU.add(item);
                        }
                    }
                    // 결과값이 20개 보다 적을 경우
                    else {
                        for(int i=0 ; i < memberArray.size(); i++){
                            JSONObject tempObj = (JSONObject) memberArray.get(i);
                            String[] tempstr = tempObj.get("term").toString().split("\\|");

                            HashMap<String,String> item = new HashMap<>();
                            item.put("item1", tempstr[0]);
                            item.put("item2", "빈도수 : " + tempstr[1].substring(0,5));
                            LIST_MENU.add(item);
                        }
                    }
                } catch (ParseException e) {
                    Log.e("LOG", e.toString());
                }
            }

            br.close();
        }
        catch (Exception e) {
            Log.e("LOG", e.toString());
        }
    }

    // 뒤로가기 버튼 눌렀을 때
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i("LOG", "back pressed..");
        finish();
    }

    // toolbar의 뒤로가기 버튼 눌렀을 때
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:{
                Log.i("LOG", "toolbar back pressed..");
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.i("LOG", "on Destroy - char_result_main");
        super.onDestroy();
    }
}
