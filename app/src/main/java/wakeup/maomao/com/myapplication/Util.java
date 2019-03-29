package wakeup.maomao.com.myapplication;

import android.app.Activity;
import android.widget.Toast;

public class Util {

    public static boolean isStringBlank(String str){
        return str == null || str.trim().equals("");
    }

    public static void showToast(final Activity activity, String msg){
        if(msg == null){
            msg = "";
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "Fail to send login request.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
