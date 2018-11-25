package com.example.kyungsoo.thinkwise;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import net.alhazmy13.wordcloud.ColorTemplate;
import net.alhazmy13.wordcloud.WordCloud;
import net.alhazmy13.wordcloud.WordCloudView;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Analysis_result extends AppCompatActivity {
    Button Topic_Button, Trend_Button;
    String data_string;
    WordCloudView wordCloud;

    // after pasring wcText
    List<WordCloud> wcList;
    String[] wcData;
    int[] wcWeight;

    // 이미지 저장에 사용 할 view, bitmap 변수
    View container;
    Bitmap captureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_result);

        Topic_Button = (Button) findViewById(R.id.Topic_Button);
        Trend_Button = (Button) findViewById(R.id.Trend_Button);
        wordCloud = (WordCloudView) findViewById(R.id.wordCloud);

        // 뒤로가기 버튼 생성
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        data_string = getIntent().getStringExtra("data_string");

        // 연관 주제어 분석
        Thread threadRecog = new Thread(new Runnable() {
            public void run() {
                try {
                    Topic_anal(data_string);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        threadRecog.start();

        // 연관주제어 버튼 눌렀을 때
        Topic_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "연관주제어 버튼 눌렀을 때", Toast.LENGTH_LONG).show();
                Thread threadRecog = new Thread(new Runnable() {
                    public void run() {
                        try {
                            // 연관 주제어 분석
                            Topic_anal(data_string);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                threadRecog.start();
            }
        });


        // 트렌드분석 버튼 눌렀을 때
        Trend_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "트렌드분석 버튼 눌렀을 때", Toast.LENGTH_LONG).show();
                Thread threadRecog = new Thread(new Runnable() {
                    public void run() {
                        try {
                            // 트렌드 분석
                            Trend_anal(data_string);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                threadRecog.start();

                // 워드 클라우딩 실행
                do {
                    try {
                        // make word cloud
                        wcList = new ArrayList<>();
                        for (int i=0; i < wcData.length; i++){
                            wcList.add(new WordCloud(wcData[i], wcWeight[i]));
                            Log.v("LOG","1");
                            wordCloud.setDataSet(wcList);
                            Log.v("LOG","2");
                            wordCloud.setSize(600,300);
                            Log.v("LOG","3");
                            wordCloud.setColors(ColorTemplate.MATERIAL_COLORS);
                            Log.v("LOG","4");
                            wordCloud.notifyDataSetChanged();  // update view
                            Log.v("LOG","5");
                        }
                    } catch (Exception e) {
                        Log.v("LOG", String.valueOf(e));
                        e.printStackTrace();
                    }
                }while (threadRecog.getState() != Thread.State.TERMINATED);
            }
        });
    }

    // 연관 주제어 분석
    private void Topic_anal(String data_string) throws IOException, JSONException {
        try{
            /* set up */
            URL url = new URL("http://api.adams.ai/datamixiApi/deeptopicrankTrend?key=3249915959609597769&target=blog&keyword=" + data_string);
            HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
            urlConn.setRequestProperty("Accept", "application/json;charset=UTF-8");
            urlConn.setRequestMethod("GET");

            int responseCode = urlConn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            String inputLine;
            while((inputLine = br.readLine()) != null){
                try {
                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObject = (JSONObject)jsonParser.parse(inputLine);
                    JSONObject jsonObject2 = (JSONObject)jsonObject.get("return_object");
                    JSONArray memberArray = (JSONArray)jsonObject2.get("trends");

                    JSONObject lastmember = (JSONObject) memberArray.get(2);
                    JSONArray node = (JSONArray) lastmember.get("nodes");

                    // 연관 주제어 가져오기
                    for(int i=0 ; i < node.size(); i++){
                        JSONObject temp = (JSONObject) node.get(i);
                        String topic_result = (String) temp.get("name");
                        Log.v("LOG", topic_result);
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

    // 트렌드 분석
    private void Trend_anal(final String data_string) throws IOException, JSONException {
        try{
            // word cloud set up
            /* set up */
            URL url = new URL("http://api.adams.ai/datamixiApi/topictrend?key=3249915959609597769&target=blog&keyword=" + data_string + "&from=&to=&timeunit=month");
            HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
            urlConn.setRequestProperty("Accept", "application/json;charset=UTF-8");
            urlConn.setRequestMethod("GET");

            int responseCode = urlConn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            String inputLine;
            while((inputLine = br.readLine()) != null){
                try {
                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObject = (JSONObject)jsonParser.parse(inputLine);
                    JSONObject jsonObject2 = (JSONObject)jsonObject.get("return_object");
                    JSONArray memberArray = (JSONArray)jsonObject2.get("trends");

                    Log.e("LOG", memberArray.toJSONString());
                    JSONObject lastmember = (JSONObject) memberArray.get(2);
                    JSONArray node = (JSONArray) lastmember.get("nodes");

                    // 트렌드 가져오기
                    wcWeight = new int[node.size()+3];
                    wcData = new String[node.size()+3];
                    for(int i=0 ; i < node.size(); i++){
                        JSONObject temp = (JSONObject) node.get(i);
                        String Trend_result = (String) temp.get("name");
                        int Trend_result_weight = Integer.parseInt(String.valueOf(Math.round((Double) temp.get("weight"))));
                        wcData[i] = Trend_result;                           // word for wc
                        wcWeight[i] = Trend_result_weight;                  // weight for wc

                        Log.v("LOG", Trend_result);
                        Log.v("LOG", String.valueOf(wcWeight.length));
                        Log.v("LOG", String.valueOf(node.size()));
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
        Log.i("LOG", "Back Pressed..");
        finish();
    }

    // 아이템을 선택했을 때
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // toolbar의 뒤로가기 버튼 눌렀을 때
            case android.R.id.home:
                Log.i("LOG", "Back Pressed..");
                finish();
                break;
            case R.id.save_image:
                container = getWindow().getDecorView();
                container.buildDrawingCache();
                captureView = container.getDrawingCache();

                try {
                    // 퍼미션 확인
                    boolean isGrantStorage = grantExternalStoragePermission();

                    // 승인 시
                    if (isGrantStorage) {
                        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.ThinkWise";
                        File file = new File(dir);

                        // 일치하는 폴더가 없으면 생성
                        if (!file.exists()) {
                            file.mkdirs();
                        }

                        FileOutputStream fos;
                        try {
                            fos = new FileOutputStream(dir + "/" + System.currentTimeMillis() + ".jpeg");
                            captureView.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        } catch (FileNotFoundException e) {
                            Log.e("LOG", e.toString());
                        }

                        Toast.makeText(getApplicationContext(), "저장을 완료 했습니다.", Toast.LENGTH_LONG).show();
                        break;
                    }
                    return super.onOptionsItemSelected(item);
                } catch (Exception e) {
                    Log.e("LOG", e.toString());
                    Toast.makeText(getApplicationContext(), "저장을 실패 했습니다.", Toast.LENGTH_LONG).show();
                }
            case R.id.image_share:
                container = getWindow().getDecorView();
                container.buildDrawingCache();
                captureView = container.getDrawingCache();

                try {
                    boolean isGrantStorage = grantExternalStoragePermission();

                    if(isGrantStorage){

                        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.ThinkWise";
                        File file = new File(dir);

                        // 일치하는 폴더가 없으면 생성
                        if( !file.exists() ) {
                            file.mkdirs();
                        }

                        FileOutputStream fos;
                        try {
                            fos = new FileOutputStream(dir + "/temp.jpeg");
                            captureView.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        } catch (FileNotFoundException e) {
                            Log.e("LOG", e.toString());
                        }

                        Uri uri = FileProvider.getUriForFile(getApplicationContext(), "com.bignerdranch.android.test.fileprovider", new File(dir + "/temp.jpeg"));

                        Intent intent = new Intent(Intent.ACTION_SEND);

                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.setType("image/+");
                        startActivity(Intent.createChooser(intent, "분석 결과 공유"));
                    }
                } catch (Exception e) {
                    Log.e("LOG", e.toString());
                    Toast.makeText(getApplicationContext(), "이미지 저장을 실패 했습니다.", Toast.LENGTH_LONG).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean grantExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v("LOG","Permission is granted");
                return true;
            }else{
                Log.v("LOG","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

                return false;
            }
        }else{
            Toast.makeText(this, "External Storage Permission is Grant", Toast.LENGTH_SHORT).show();
            Log.d("LOG", "External Storage Permission is Grant ");
            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        Log.i("LOG", "On Destroy");

        // 임시파일 삭제!
        try {
            String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.ThinkWise";
            File file = new File(dir + "/temp.jpeg");

            if( file.exists() ) {
                file.delete();
            }
        } catch (Exception e) {
            Log.e("LOG", e.toString());
        }
        super.onDestroy();
    }
}
