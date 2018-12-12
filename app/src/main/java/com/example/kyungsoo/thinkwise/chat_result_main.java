package com.example.kyungsoo.thinkwise;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
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

public class chat_result_main extends AppCompatActivity {
    ArrayList<HashMap<String, String>> LIST_MENU; // 리스트 뷰 변수
    ListView listview; // 리스트뷰
    SimpleAdapter adapter; // 리스트뷰 어댑터
    String temp = null;

    // 이미지 저장에 사용 할 view, bitmap 변수
    View container;
    Bitmap captureView;

    // 팝업윈도우 한 번만 출력하기 위한 변수
    private boolean Popup = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_result_main);

        // 뒤로가기 버튼 생성
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        CheckTypesTask task = new CheckTypesTask();
        task.execute();

        // ListView 생성
        LIST_MENU = new ArrayList<HashMap<String, String>>();

        adapter = new SimpleAdapter(this, LIST_MENU, android.R.layout.simple_list_item_2,
                new String[]{"item1", "item2"}, new int[]{android.R.id.text1, android.R.id.text2});
        listview = (ListView) findViewById(R.id.listview1); // 리스트뷰
        listview.setAdapter(adapter);

        // ListView 객체의 특정 아이템 클릭시 처리 추가
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                HashMap<String, String> item = (HashMap<String, String>) adapterView.getItemAtPosition(position);
                String selected_item = item.get("item1");

                Intent Analysis_result = new Intent(getApplicationContext(), com.example.kyungsoo.thinkwise.Analysis_result.class);
                Analysis_result.putExtra("data_string", selected_item);
                Analysis_result.putExtra("Popup", Popup);
                startActivityForResult(Analysis_result, 1);
            }
        });
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

    private class CheckTypesTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog asyncDialog = new ProgressDialog(chat_result_main.this);
        boolean iskakao;

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            asyncDialog.setMessage("대화 내용을 분석중입니다....");
            asyncDialog.setCancelable(false);

            // show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                try {
                    chat_split();
                } catch (Exception e) {
                    Log.e("LOG", e.toString());
                    Toast.makeText(getApplicationContext(), "내용분석 오류", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            asyncDialog.dismiss();

            // 카카오톡 대화내용이 아닐 경우 예외처리 //
            if (iskakao == false) {
                Toast.makeText(getApplicationContext(), "카카오톡 대화내용 파일이 아닙니다.", Toast.LENGTH_SHORT).show();
                return;
            }

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
                adapter.notifyDataSetChanged();
            } while (threadRecog.getState() != Thread.State.TERMINATED);

            super.onPostExecute(result);
        }

        private void chat_split() {
            String fileContent = MainActivity.getContentstring();

            // 라인으로 split하기
            String[] split_line = fileContent.split("\n");
            String whole_chat_content = "";

            // 카카오톡 대화내용이 아닐 경우 예외처리 //
            iskakao = false;
            String[] tempSplitWs = split_line[0].split("\\s");
            for (int j = 0; j < tempSplitWs.length; j++) {
                if (tempSplitWs[j].equals("카카오톡")) {
                    iskakao = true;
                }
            }

            if (iskakao == false) return;
            // 카카오톡 대화내용이 아닐 경우 예외처리 //

            asyncDialog.setMax(split_line.length);

            int i;
            // 라인이 10만줄 이상일 때
            if (split_line.length > 100000) {
                // 단어로 split해서 대화 내용만 합치기
                for (i = split_line.length - 100000; i < split_line.length; i++) {
                    if (split_line[i] == "") continue;
                    String[] split_ws = split_line[i].split("\\s");

                    if (split_ws.length < 7) continue;
                    else {
                        for (int j = 7; j < split_ws.length; j++) {
                            whole_chat_content = whole_chat_content + split_ws[j] + "\n";
                        }
                    }
                    asyncDialog.setProgress(i);
                }
            }

            //  라인이 10만줄 이하일 때
            else {
                // 단어로 split해서 대화 내용만 합치기
                for (i = 2; i < split_line.length; i++) {
                    if (split_line[i] == "") continue;
                    String[] split_ws = split_line[i].split("\\s");

                    if (split_ws.length < 7) continue;
                    else {
                        for (int j = 7; j < split_ws.length; j++) {
                            whole_chat_content = whole_chat_content + split_ws[j] + "\n";
                        }
                    }
                    asyncDialog.setProgress(i);
                }
            }
            temp = whole_chat_content;
        }

        // JSON형태로 받아오기!!!
        public void lastPOST() throws IOException, JSONException {
            String input = "key=3249915959609597769&text=" + temp;

            try {
                /* set up */
                URL url = new URL("http://api.adams.ai/datamixiApi/keywordextract");
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
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
                } else {
                    // 실패시 처리
                    br = new BufferedReader(new InputStreamReader(urlConn.getErrorStream()));
                }
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    try {
                        JSONParser jsonParser = new JSONParser();
                        JSONObject jsonObject = (JSONObject) jsonParser.parse(inputLine);
                        JSONArray memberArray = (JSONArray) jsonObject.get("return_object");
                        // 결과값이 20개 보다 많을 경우
                        if (memberArray.size() > 20) {
                            for (int i = 0; i <= 20; i++) {
                                JSONObject tempObj = (JSONObject) memberArray.get(i);
                                String[] tempstr = tempObj.get("term").toString().split("\\|");

                                HashMap<String, String> item = new HashMap<>();
                                item.put("item1", tempstr[0]);
                                item.put("item2", "빈도수 : " + tempstr[1].substring(0, 5));
                                LIST_MENU.add(item);
                            }
                        }
                        // 결과값이 20개 보다 적을 경우
                        else {
                            for (int i = 0; i < memberArray.size(); i++) {
                                JSONObject tempObj = (JSONObject) memberArray.get(i);
                                String[] tempstr = tempObj.get("term").toString().split("\\|");

                                HashMap<String, String> item = new HashMap<>();
                                item.put("item1", tempstr[0]);
                                item.put("item2", "빈도수 : " + tempstr[1].substring(0, 5));
                                LIST_MENU.add(item);
                            }
                        }
                    } catch (ParseException e) {
                        Log.e("LOG", e.toString());
                    }
                }
                br.close();
            } catch (Exception e) {
                Log.e("LOG", e.toString());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            try {
                Popup = false;
            } catch (Exception e) {
                Log.e("LOG", e.toString());
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