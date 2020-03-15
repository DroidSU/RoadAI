/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cooperativeai.R;
import com.cooperativeai.utils.SharedPreferenceManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    @BindView(R.id.profile_user_dp)
    CircleImageView civUserDP;
    @BindView(R.id.textView_profile_name)
    TextView textViewUserName;
    @BindView(R.id.textView_profile_total_steps)
    TextView textViewTotalSteps;
    @BindView(R.id.textView_profile_level)
    TextView textViewLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);

        setupViews();
    }

    private void setupViews() {
        textViewUserName.setText(SharedPreferenceManager.getUserName(ProfileActivity.this));
        textViewTotalSteps.setText(SharedPreferenceManager.getUserCoins(ProfileActivity.this));
        textViewLevel.setText("Level " + SharedPreferenceManager.getUserLevel(ProfileActivity.this));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        finish();
    }
}
