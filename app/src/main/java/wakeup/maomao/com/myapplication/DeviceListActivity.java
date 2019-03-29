package wakeup.maomao.com.myapplication;

import android.app.Activity;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeviceListActivity extends ListActivity {

    private SharedPreferences shareData;
    private ListView listView;
    private List<String> deviceList = null;

    private void getDeviceList() {
        String token = shareData.getString("token", "");
        String serverConfig = shareData.getString("serverConfig", "");
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
                Util.showToast(DeviceListActivity.this , "Fail to get device list.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseStr = response.body().string();
                Log.d("debug", responseStr);
                try {
                    JSONObject jsonObject = new JSONObject(responseStr);
                    int code = jsonObject.getInt("code");
                    if (code == 30000) {
                        final JSONArray array = jsonObject.getJSONArray("data");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initListView(array.toString());
                            }
                        });
                    } else {
                        Util.showToast(DeviceListActivity.this , jsonObject.getString("message"));
                    }

                } catch (JSONException e) {
                    Log.e(Constant.LOG_TAG, "Error found when processing response of get device list in DeviceListActivity.", e);
                }

            }
        });
    }

    private void initListView(String deviceListJson) {
        this.deviceList = new ArrayList<>();
        try {
            JSONArray deviceNameArray = new JSONArray(deviceListJson);
            for (int i = 0, len = deviceNameArray.length(); i < len; i++) {
                this.deviceList.add(deviceNameArray.get(i).toString());
            }

        } catch (JSONException e) {
            Log.e(Constant.LOG_TAG, "Json format is illegal when init ListView.");
        }
        TextView tvHeader = new TextView(DeviceListActivity.this);
        tvHeader.setText("设备列表");
        this.listView.addHeaderView(tvHeader);
        this.listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, this.deviceList));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        shareData = getSharedPreferences("Data", Activity.MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        this.listView = getListView();

        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            String deviceListInJson = bundle.getString("device_list");
            if (!Util.isStringBlank(deviceListInJson)) {
                initListView(deviceListInJson);
                return;
            }
        }
        getDeviceList();

    }

    @Override
    protected void onListItemClick(ListView parent, View view, int position, long id) {
        String token = shareData.getString("token", "");
        String serverConfig = shareData.getString("serverConfig", "");
        String deviceName = this.deviceList.get(position - 1);

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject postBody = new JSONObject();
        RequestBody body = RequestBody.create(JSON, postBody.toString());
        OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .addHeader("Content-Type", "application/json")
                .addHeader("token", token)
                .url(serverConfig + "/devices/" + deviceName + "/wakeup")
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(Constant.LOG_TAG, "Fail to wakeup device.", e);
                Toast.makeText(DeviceListActivity.this, "Fail to get device list.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseStr = response.body().string();
                Log.d("debug", responseStr);
                try {
                    JSONObject jsonObject = new JSONObject(responseStr);
                    int code = jsonObject.getInt("code");
                    final StringBuilder message = new StringBuilder();
                    if (code == 20000) {
                        message.append("唤醒请求已发出");
                    } else {
                        message.append(jsonObject.getString("message"));
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DeviceListActivity.this, message.toString(), Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (JSONException e) {
                    Log.e(Constant.LOG_TAG, "Error found when wake up device.");
                }

            }
        });

    }
}
