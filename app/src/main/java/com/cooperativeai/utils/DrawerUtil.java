/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.utils;

import android.app.Activity;

import androidx.appcompat.widget.Toolbar;

import com.cooperativeai.R;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;

public class DrawerUtil {
    public static void getDrawer(final Activity activity, Toolbar toolbar){
        PrimaryDrawerItem homeItem = new PrimaryDrawerItem().withIdentifier(0)
                .withName("Home");

        PrimaryDrawerItem cameraItem = new PrimaryDrawerItem().withIdentifier(0)
                .withIcon(R.drawable.ic_camera_24dp)
                .withName("Camera");

        String user_email = SharedPreferenceManager.getUserEmail(activity.getBaseContext());
        String user_name = SharedPreferenceManager.getUserName(activity.getBaseContext());


        ProfileDrawerItem profileDrawerItem = new ProfileDrawerItem().withEmail(user_email).withName(user_name);

        AccountHeader accountHeader = new AccountHeaderBuilder()
                .withActivity(activity)
                .addProfiles(profileDrawerItem).build();

        Drawer drawer = new DrawerBuilder()
                .withActivity(activity)
                .withToolbar(toolbar)
                .withCloseOnClick(true)
                .withSelectedItem(-1)
                .withAccountHeader(accountHeader)
                .addDrawerItems(homeItem, cameraItem)
                .build();
    }
}
