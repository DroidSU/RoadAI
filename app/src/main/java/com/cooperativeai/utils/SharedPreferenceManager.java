/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.cooperativeai.models.eventmodels.CoinCountChangedModel;

import org.greenrobot.eventbus.EventBus;

public class SharedPreferenceManager {
    private static SharedPreferences sharedPreferences;

    public static void saveUserRegistrationDetails(){
    }

    public static String getUserEmail(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_EMAIL, "");
    }

    public static String getUserName(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_NAME, "");
    }

    public static boolean changeCoinCount(Context context, String mod_type, double value){
        try {
            if (sharedPreferences == null)
                sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);

            double current_al = Double.parseDouble(sharedPreferences.getString(Constants.PREFS_USER_COIN_COUNT, "0"));

            double final_val;

            if (mod_type.equalsIgnoreCase("add"))
                final_val = current_al + value;
            else
                final_val = current_al - value;

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.PREFS_USER_COIN_COUNT, String.valueOf(final_val));
            editor.apply();

            // Throw event every time. Respective catchers will automatically do their job
            CoinCountChangedModel model = new CoinCountChangedModel();
            model.setCoin_val(final_val);
            EventBus.getDefault().post(model);

            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public static String getUserCoins(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_COIN_COUNT, "0");
    }

    public static String getLastUsedDate(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_LAST_ACCESSED, "");
    }

    public static void setLastUsedDate(Context context, String dateAsString){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREFS_USER_LAST_ACCESSED, dateAsString);
        editor.apply();
    }

    public static int getUserLevel(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.PREFS_USER_CURRENT_LEVEL, Constants.DEFAULT_USER_LEVEL);
    }

    public static void setUserLevel(Context context, int level){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.PREFS_USER_CURRENT_LEVEL, level);
        editor.apply();
    }

    public static int getAutoCaptureStatus(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.PREF_AUTO_CAPTURE_PREF, Constants.AUTO_CAPTURE_DISABLED);
    }

    public static void setAutoCaptureStatus(Context context, int status){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.PREF_AUTO_CAPTURE_PREF, status);
        editor.apply();
    }
}
