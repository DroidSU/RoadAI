/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cooperativeai.R;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.UtilityMethods;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RegistrationActivity extends AppCompatActivity {

    @BindView(R.id.button_register)
    Button submitButton;
    @BindView(R.id.editText_register_email)
    TextInputEditText editTextEmail;
    @BindView(R.id.editText_register_name)
    TextInputEditText editTextName;
    @BindView(R.id.editText_register_password)
    TextInputEditText editTextPassword;

    private String email;
    private String password;
    private String name;

    private FirebaseAuth firebaseAuth;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        ButterKnife.bind(this);

        firebaseAuth = FirebaseAuth.getInstance();
    }

    @OnClick(R.id.button_register)
    public void onClickRegister(){
        email = Objects.requireNonNull(editTextEmail.getText()).toString().trim();
        password = Objects.requireNonNull(editTextPassword.getText()).toString().trim();
        name = Objects.requireNonNull(editTextName.getText()).toString().trim();

        if (UtilityMethods.isInternetAvailable()) {
            if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() && !password.isEmpty() && !name.isEmpty()) {
                dialog = UtilityMethods.showDialog(RegistrationActivity.this, R.layout.layout_loading_dialog);
                dialog.show();
                startRegistration();
            } else {
                if (email.isEmpty())
                    editTextEmail.setError("Email is required");
                if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches())
                    editTextEmail.setError("Invalid email format");
                if (password.isEmpty())
                    editTextPassword.setError("Password is required");
                if (name.isEmpty())
                    editTextName.setError("Name is required");

                dialog.dismiss();
            }
        }
        else{
            Toast.makeText(this, "No internet connection available", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.textView_sign_in)
    public void goToSignIn(){
        startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
        finish();
    }

    private void startRegistration() {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name).build();

                        if (firebaseUser != null) {
                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                if (dialog != null)
                                                    dialog.dismiss();
                                                if (authResult != null) {
                                                    SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);
                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                    editor.putString(Constants.PREFS_USER_NAME, name);
                                                    if (authResult.getUser() != null) {
                                                        editor.putString(Constants.PREFS_USER_EMAIL, authResult.getUser().getEmail());
                                                        editor.putString(Constants.PREFS_USER_ID, authResult.getUser().getUid());
                                                    }
                                                    editor.apply();
                                                }
                                                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                                                finish();
                                            }
                                            else{
                                                Toast.makeText(RegistrationActivity.this, "User could not be registered", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RegistrationActivity.this, "Failed: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
