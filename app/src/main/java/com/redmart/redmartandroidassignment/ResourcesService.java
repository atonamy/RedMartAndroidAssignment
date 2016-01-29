package com.redmart.redmartandroidassignment;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;

/**
 * Created by archie on 29/1/16.
 */
public class ResourcesService {
    public static Drawable getDrawable(Context context, int resourse_id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return context.getResources().getDrawable(resourse_id, context.getTheme());
        else
            return context.getResources().getDrawable(resourse_id);
    }

    public static int getColor(Context context, int resourse_id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return ContextCompat.getColor(context, resourse_id);
        else
            return context.getResources().getColor(resourse_id);
    }
}
