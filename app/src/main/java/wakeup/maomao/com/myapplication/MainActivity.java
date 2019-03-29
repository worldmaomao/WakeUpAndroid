package wakeup.maomao.com.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Button loginButton;
    private EditText usernameEdit;
    private EditText passwordEdit;
    private EditText serverConfigEdit;

    private SharedPreferences shareData;

    private void login(String serverUrl, String username, String password){
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject postBody = new JSONObject();
        try{
            postBody.put("username", username);
            postBody.put("password", password);
        }
        catch (Exception e){
            Log.e(Constant.LOG_TAG, "Set login parameter error", e);
        }
        RequestBody body = RequestBody.create(JSON, postBody.toString());
        final Request request = new Request.Builder()
                .addHeader("Content-Type", "application/json")
                .url(serverUrl + "/user/login")
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(Constant.LOG_TAG, "Fail to login.", e);
                Util.showToast(MainActivity.this , "Fail to send login request.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseStr = response.body().string();
                Log.d(Constant.LOG_TAG, responseStr);
                try{
                    final JSONObject responseJson = new JSONObject(responseStr);
                    int code = responseJson.getInt("code");
                    if(code == 10000){
                        String token = responseJson.getJSONObject("data").getString("token");
                        SharedPreferences.Editor editor = shareData.edit();
                        editor.putString("token", token);
                        editor.commit();
                        startDeviceListView(null);
                    }
                    else{
                        Util.showToast(MainActivity.this , responseJson.getString("message"));
                    }

                }
                catch (Exception e){
                    Log.e(Constant.LOG_TAG, "Error found when process login reponse.", e);
                }
            }
        });
    }

    private void startDeviceListView(String deviceNameListInJson){
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, DeviceListActivity.class);
        if(!Util.isStringBlank(deviceNameListInJson)){
            intent.putExtra("device_list", deviceNameListInJson);
        }
        startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shareData = getSharedPreferences("Data", Activity.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        this.usernameEdit = (EditText)findViewById(R.id.userNameEdit);
        this.passwordEdit = (EditText) findViewById(R.id.passwordEdit);
        this.serverConfigEdit = (EditText)findViewById(R.id.serverConfigEdit);
        this.loginButton = (Button)findViewById(R.id.loginBtn);

        String username = shareData.getString("username", "");
        String password = shareData.getString("password", "");
        String serverConfig = shareData.getString("serverConfig", "");
        String token = shareData.getString("token", "");
        this.usernameEdit.setText(username);
        this.passwordEdit.setText(password);
        this.serverConfigEdit.setText(serverConfig);

        this.loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEdit.getText().toString();
                String password = passwordEdit.getText().toString();
                String serverConfig = serverConfigEdit.getText().toString();
                if(Util.isStringBlank(username)){
                    Toast.makeText(MainActivity.this, "username can not be empty.", Toast.LENGTH_LONG).show();
                    return;
                }
                if(Util.isStringBlank(password)){
                    Toast.makeText(MainActivity.this, "password can not be empty.", Toast.LENGTH_LONG).show();
                    return;
                }
                if(Util.isStringBlank(serverConfig)){
                    Toast.makeText(MainActivity.this, "serverConfig can not be empty.", Toast.LENGTH_LONG).show();
                    return;
                }
                SharedPreferences.Editor editor = shareData.edit();
                editor.putString("username", username);
                editor.putString("password", password);
                editor.putString("serverConfig", serverConfig);
                editor.commit();
                login(serverConfig, username, password);
            }
        });
        // if has already logined, fetch device list
        if(!Util.isStringBlank(token) && !Util.isStringBlank(serverConfig)){
            OkHttpClient client = new OkHttpClient();
            final Request request = new Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("token", token)
                    .url(serverConfig + "/devices")
                    .get()
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(Constant.LOG_TAG, "Fail to get device list in MainActivity.", e);
                    Util.showToast(MainActivity.this , "Fail to get device list.");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseStr = response.body().string();
                    Log.d(Constant.LOG_TAG, responseStr);
                    try {
                        JSONObject jsonObject = new JSONObject(responseStr);
                        int code = jsonObject.getInt("code");
                        if(code == 30000){
                            JSONArray array = jsonObject.getJSONArray("data");
                            startDeviceListView(array.toString());
                        }

                    } catch (JSONException e) {
                        Log.e(Constant.LOG_TAG, "Error found when processing response of get device list in MainActivity.", e);
                    }

                }
            });

        }
    }
}
