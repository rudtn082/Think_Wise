package com.example.kyungsoo.thinkwise;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Toast;
import android.view.WindowManager.LayoutParams;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import net.alhazmy13.wordcloud.ColorTemplate;
import net.alhazmy13.wordcloud.WordCloud;
import net.alhazmy13.wordcloud.WordCloudView;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Analysis_result extends AppCompatActivity {
    Button Topic_Button, Trend_Button;
    String data_string;
    WordCloudView wordCloud; // 워드클라우드
    boolean stats = true; // 통계가 있는지 검사하기 위한 변수
    ArrayList<PieEntry> topic;
    PieChart pieChart; // 파이차트
    TopicTask task;

    // after pasring wcText
    List<WordCloud> wcList;
    String[] wcData;
    int[] wcWeight;

    // 이미지 저장에 사용 할 view, bitmap 변수
    View container;
    Bitmap captureView;

    // 팝업 윈도우
    private PopupWindow mPopupWindow;
    private boolean Popup = true;

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
        Popup = getIntent().getBooleanExtra("Popup", true);
        Log.e("LOG", String.valueOf(Popup));

        task = new TopicTask();
        task.execute();

        // 연관주제어 버튼 눌렀을 때
        Topic_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WordCloudView wordCloudView = (WordCloudView) findViewById(R.id.wordCloud);
                wordCloudView.setVisibility(View.GONE);
                PieChart pieChart = (PieChart) findViewById(R.id.piechart);
                pieChart.setVisibility(View.VISIBLE);

                pieChartGrid();
            }
        });

        // 트렌드분석 버튼 눌렀을 때
        Trend_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WordCloudView wordCloudView = (WordCloudView) findViewById(R.id.wordCloud);
                wordCloudView.setVisibility(View.VISIBLE);
                PieChart pieChart = (PieChart) findViewById(R.id.piechart);
                pieChart.setVisibility(View.GONE);

                TrendTask task = new TrendTask();
                task.execute();
            }
        });


    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus == true && Popup == true) {
            // 가이드 화면(popupWindow)
            View popupView = getLayoutInflater().inflate(R.layout.popupwindow, null);

            mPopupWindow = new PopupWindow(popupView,
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            mPopupWindow.setAnimationStyle(-1); // 애니메이션 설정(-1:설정, 0:설정안함)

            mPopupWindow.showAtLocation(popupView, Gravity.FILL, 0, 0);

            popupView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPopupWindow.dismiss();
                    Popup = false;
                }
            });
        }
    }

    // 연관 주제어 분석
    private void Topic_anal(String data_string) throws IOException, JSONException {
        try {
            /* set up */
            URL url = new URL("http://api.adams.ai/datamixiApi/deeptopicrankTrend?key=3249915959609597769&target=blog&keyword=" + data_string);
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setRequestProperty("Accept", "application/json;charset=UTF-8");
            urlConn.setRequestMethod("GET");

            int responseCode = urlConn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                try {
                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObject = (JSONObject) jsonParser.parse(inputLine);
                    JSONObject jsonObject2 = (JSONObject) jsonObject.get("return_object");
                    JSONArray memberArray = (JSONArray) jsonObject2.get("trends");

                    JSONObject lastmember = (JSONObject) memberArray.get(0);
                    JSONArray node = (JSONArray) lastmember.get("nodes");

                    if (node.size() <= 1) {
                        stats = false;
                        return;
                    } else stats = true;

                    topic = new ArrayList<PieEntry>();

                    // 연관 주제어 가져오기
                    for (int i = 0; i < node.size(); i++) {
                        JSONObject temp = (JSONObject) node.get(i);
                        String topic_result = (String) temp.get("name");
                        double tempWeight = (double) temp.get("weight");
                        tempWeight = (tempWeight * 10) * (tempWeight * 10) * (tempWeight * 10) * (tempWeight * 10) * (tempWeight * 10) * (tempWeight * 10) * (tempWeight * 10) * (tempWeight * 10);

                        int Trend_result_weight = (int) Math.round(tempWeight);
                        Log.e("LOG", String.valueOf(Trend_result_weight));
                        topic.add(new PieEntry(Trend_result_weight, topic_result));
                    }
                } catch (ParseException e) {
                    Log.e("LOG", e.toString());
                    Toast.makeText(getApplicationContext(), "키워드를 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            br.close();
        } catch (Exception e) {
            Log.e("LOG", e.toString());
            Toast.makeText(getApplicationContext(), "키워드를 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // 차트 생성
    public void pieChartGrid() {
        try {
            pieChart = (PieChart) findViewById(R.id.piechart);
            pieChart.setUsePercentValues(true);
            pieChart.getDescription().setEnabled(false);
            pieChart.setExtraOffsets(5, 10, 5, 5);

            pieChart.setDragDecelerationFrictionCoef(0.95f);

            pieChart.setDrawHoleEnabled(false);
            pieChart.setHoleColor(Color.WHITE);
            pieChart.setTransparentCircleRadius(61f);

            Description description = new Description();
            description.setText("연관 주제어 분석결과"); //라벨
            description.setTextSize(15);
            pieChart.setDescription(description);

            pieChart.animateY(1000, Easing.EasingOption.EaseInOutCubic); //애니메이션

            PieDataSet dataSet = new PieDataSet(topic, "주제어");
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);
            dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
            PieData data = new PieData((dataSet));
            data.setValueTextSize(10f);
            data.setValueTextColor(Color.YELLOW);

            pieChart.setData(data);
            pieChart.invalidate();
        } catch (Exception e) {
            Log.e("LOG", e.toString());
            Toast.makeText(getApplicationContext(), "키워드를 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // 트렌드 분석
    private void Trend_anal(final String data_string) throws IOException, JSONException {
        try {
            // word cloud set up
            /* set up */
            URL url = new URL("http://api.adams.ai/datamixiApi/topictrend?key=3249915959609597769&target=blog&keyword=" + data_string + "&from=&to=&timeunit=month");
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setRequestProperty("Accept", "application/json;charset=UTF-8");
            urlConn.setRequestMethod("GET");

            int responseCode = urlConn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                try {
                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObject = (JSONObject) jsonParser.parse(inputLine);
                    JSONObject jsonObject2 = (JSONObject) jsonObject.get("return_object");
                    JSONArray memberArray = (JSONArray) jsonObject2.get("trends");

                    JSONObject firstmember = (JSONObject) memberArray.get(0);
                    JSONArray node = (JSONArray) firstmember.get("nodes");

                    if (node.size() <= 1) {
                        stats = false;
                        return;
                    } else stats = true;

                    // 트렌드 가져오기
                    wcWeight = new int[node.size()];
                    wcData = new String[node.size()];
                    for (int i = 0; i < node.size(); i++) {
                        JSONObject temp = (JSONObject) node.get(i);
                        String Trend_result = (String) temp.get("name");
                        int Trend_result_weight = Integer.parseInt(String.valueOf(Math.round((Double) temp.get("weight"))));
                        wcData[i] = Trend_result;                           // word for wc
                        wcWeight[i] = Trend_result_weight;                  // weight for wc
                    }
                } catch (ParseException e) {
                    Log.e("LOG", e.toString());
                    Toast.makeText(getApplicationContext(), "키워드를 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            br.close();
        } catch (Exception e) {
            Log.e("LOG", e.toString());
            Toast.makeText(getApplicationContext(), "키워드를 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
            finish();
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

                    if (isGrantStorage) {

                        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.ThinkWise";
                        File file = new File(dir);

                        // 일치하는 폴더가 없으면 생성
                        if (!file.exists()) {
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
                Log.v("LOG", "Permission is granted");
                return true;
            } else {
                Log.v("LOG", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

                return false;
            }
        } else {
            Toast.makeText(this, "External Storage Permission is Grant", Toast.LENGTH_SHORT).show();
            Log.d("LOG", "External Storage Permission is Grant ");
            return true;
        }
    }

    // 연관주제어 어싱크테스크
    private class TopicTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog asyncDialog = new ProgressDialog(Analysis_result.this);

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("연관 주제어를 분석중입니다....");
            asyncDialog.setCancelable(false);

            // show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                // 연관 주제어 분석
                Topic_anal(data_string);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (stats == false) {
                Toast.makeText(getApplicationContext(), "통계가 없습니다.", Toast.LENGTH_SHORT).show();
                asyncDialog.dismiss();
                return;
            }
            asyncDialog.dismiss();
        }
    }

    // 트렌드 어싱크테스크
    private class TrendTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog asyncDialog = new ProgressDialog(Analysis_result.this);

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("트렌드를 분석중입니다....");
            asyncDialog.setCancelable(false);

            // show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                // 트렌드 분석
                Trend_anal(data_string);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (stats == false) {
                Toast.makeText(getApplicationContext(), "통계가 없습니다.", Toast.LENGTH_SHORT).show();
                asyncDialog.dismiss();
                return;
            }

            try {
                // make word cloud
                wcList = new ArrayList<>();
                for (int i = 0; i < wcData.length; i++) {
                    wcList.add(new WordCloud(wcData[i], wcWeight[i]));

                    Log.e("LOG", String.valueOf(wcWeight[i]));
                    wordCloud.setDataSet(wcList);
                    wordCloud.setSize((wcWeight[i] + 1) * 140, (wcWeight[i] + 1) * 90);
                    wordCloud.setColors(ColorTemplate.MATERIAL_COLORS);
                }
                wordCloud.notifyDataSetChanged();  // update view
            } catch (Exception e) {
                Log.v("LOG", e.toString());
                e.printStackTrace();
            } finally {
                asyncDialog.dismiss();
            }
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

            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            Log.e("LOG", e.toString());
        }
        super.onDestroy();
    }
}