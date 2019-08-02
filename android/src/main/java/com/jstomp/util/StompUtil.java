package com.jstomp.util;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @company Shanghai Daoqiao Information Technology-->
 * @anthor created by jingzhanwu
 * @date 2018/2/26 0026
 * @change
 * @describe Some tools of stomp
 **/
public class StompUtil {
    public final static String TAG = "StompUtil";

    /**
     * Determine if the program is running, including the foreground or background
     *
     * @param context
     * @return
     */
    public static boolean isAppRunning(Context context) {
        String packageName = context.getPackageName();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(packageName) && info.baseActivity.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is it running in the foreground?
     *
     * @param ctx
     * @return
     */
    public static boolean isAppRunningForeground(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
            if (tasks == null || tasks.size() < 1) {
                return false;
            }
            boolean b = ctx.getPackageName().equalsIgnoreCase(tasks.get(0).baseActivity.getPackageName());
            Log.d("utils", "app running in foregroud：" + (b));
            return b;
        } catch (SecurityException e) {
            Log.d(TAG, "Apk doesn't hold GET_TASKS permission");
            e.printStackTrace();
        }
        return false;
    }

    public static String getTopActivityName(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
            if (tasks == null || tasks.size() < 1) {
                return "";
            }
            return tasks.get(0).topActivity.getClassName();
        } catch (SecurityException e) {
            Log.d(TAG, "Apk doesn't hold GET_TASKS permission");
            e.printStackTrace();
        }
        return "";
    }


    /**
     * Determine if an interface is in the foreground
     *
     * @param context   Context
     * @param className Interface class name
     * @return Whether to display in the foreground
     */
    public static boolean isActivityForeground(Context context, String className) {
        if (context == null || TextUtils.isEmpty(className))
            return false;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        for (ActivityManager.RunningTaskInfo taskInfo : list) {
            if (taskInfo.topActivity.getShortClassName().contains(className)) { // Explain that it has been started
                return true;
            }
        }
        return false;
    }


    public static boolean isSingleActivity(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = null;
        try {
            tasks = activityManager.getRunningTasks(1);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        if (tasks == null || tasks.size() < 1) {
            return false;
        }
        return tasks.get(0).numRunning == 1;
    }

    public static List<String> getRunningApps(Context ctx) {
        List<String> list = new ArrayList<String>();
        try {
            ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> infos = activityManager.getRunningAppProcesses();
            if (infos == null) {
                return list;
            }
            for (ActivityManager.RunningAppProcessInfo info : infos) {
                String appName = info.processName;
                if (appName.contains(":"))
                    appName = appName.substring(0, appName.indexOf(":"));
                if (!list.contains(appName))
                    list.add(appName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @SuppressLint("SimpleDateFormat")
    public static String getTimeStamp() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyyMMddHHmmss");
        return dateFormat.format(date);
    }


    public static boolean isExistApp(Context context, String packageName) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(
                    packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        return packageInfo != null;
    }


    public static String getCurProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager
                .getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }
}
