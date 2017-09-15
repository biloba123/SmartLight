package com.lvqingyang.autobr;

import android.content.Context;
import android.widget.Toast;

/**
 * Author：LvQingYang
 * Date：2017/8/27
 * Email：biloba12345@gamil.com
 * Github：https://github.com/biloba123
 * Info：
 */
public class MyToast {
    private static Toast sToast;

    public static void show(Context context,String msg){
        if (sToast!=null) {
            sToast.cancel();
        }
        sToast=Toast.makeText(context, msg,Toast.LENGTH_SHORT);
        sToast.show();
    }
}
