package com.redmart.redmartandroidassignment;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by archie on 29/1/16.
 */
public class VolleyManager {
    private static RequestQueue volleyQueue = null;

    public static void init(Context context) {
        if(volleyQueue == null)
            volleyQueue = Volley.newRequestQueue(context);
    }

    public static boolean isInitialized() {
        return (volleyQueue != null);
    }

    public static RequestQueue getVolleyQueue() {
        return volleyQueue;
    }
}
