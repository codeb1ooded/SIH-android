package com.igdtuw.technotwisters.sih_android.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.igdtuw.technotwisters.sih_android.R;
import com.igdtuw.technotwisters.sih_android.OtherFiles.SharedPreferencesUtils;
import com.igdtuw.technotwisters.sih_android.api.ApiClient;
import com.igdtuw.technotwisters.sih_android.model.Result;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Admin on 22-03-2017.
 */

public class ProfileChangeActivity extends AppCompatActivity {
    private EditText _name;
    private EditText _age;
    private EditText _email;
    private EditText _contactNum;
    private EditText _address;
    private EditText _city;
    private EditText _state;
    private EditText _expertise;
    private EditText _qualif;
    private EditText _experience;
    private EditText _pref_location;
    private Button _submit;
    ProgressDialog progressDialog;

    Call<Result> updateDetails;
    String name,email,address,city,state,expertise,qualification,preferredlocation;
    int age,teachingExperience;
    long contactNum;

    SharedPreferencesUtils spUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_change);
        _name = (EditText) findViewById(R.id.name_profile);
        _email = (EditText) findViewById(R.id.email_profile);
        _age = (EditText) findViewById(R.id.age);
        _contactNum = (EditText) findViewById(R.id.contact_no);
        _address = (EditText) findViewById(R.id.address);
        _city = (EditText) findViewById(R.id.city);
        _state = (EditText) findViewById(R.id.state);
        _expertise = (EditText) findViewById(R.id.expertise);
        _qualif = (EditText) findViewById(R.id.qualif);
        _experience = (EditText) findViewById(R.id.experience);
        _pref_location = (EditText) findViewById(R.id.pref_location);
        _submit = (Button) findViewById(R.id.submit);
        _submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        spUtils = new SharedPreferencesUtils(ProfileChangeActivity.this);
        setupToolbar();
    }

    public void submit() {
        _submit.setEnabled(false);

        progressDialog = new ProgressDialog(ProfileChangeActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        String name = _name.getText().toString().trim();
        int age = Integer.parseInt(_name.getText().toString());
        long contactNum = Long.parseLong(_name.getText().toString().trim());
        String email = _email.getText().toString().trim();
        String address = _address.getText().toString().trim();
        String city = _city.getText().toString().trim();
        String state = _state.getText().toString().trim();
        String qualification = _qualif.getText().toString().trim();
        String expertise = _expertise.getText().toString().trim();
        int teachingExperience = Integer.parseInt(_experience.getText().toString().trim());
        String preferredLocation = _pref_location.getText().toString().trim();

        if(name==null||age==0||contactNum==0||email==null||address==null||city==null||state==null||qualification==null||expertise==null||teachingExperience==0||preferredLocation==null){
            Toast.makeText(ProfileChangeActivity.this,
                    "All fields Mandatory", Toast.LENGTH_LONG).show();
        } else {
            updateDetails = ApiClient.getInterface().updateUserDetails(spUtils.getUsername(), spUtils.getAccessToken(), name, age,
                    contactNum, email, expertise, address, city, state, preferredLocation, qualification, teachingExperience);
            updateDetails.enqueue(new Callback<Result>() {
                @Override
                public void onResponse(Call<Result> call, Response<Result> response) {
                    if (response.isSuccessful()) {
                        onSubmitSuccess(response.body());
                    } else {
                        Log.i("UpdateFailed: ", "not granted " + response.errorBody());
                        onUpdateFailed();
                    }
                }

                @Override
                public void onFailure(Call<Result> call, Throwable t) {
                    progressDialog.dismiss();
                    Log.i("AccessToken: ", "not granted " + t);
                    onUpdateFailed();
                }
            });
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // disable going back to the MainActivity
//        moveTaskToBack(true);
    }


    public void onSubmitSuccess(Result result) {
        name = result.name_;
        age = result.age_;
        contactNum = result.contact;
        email = result.email_;
        address = result.address_;
        city = result.city_;
        state = result.state_;
        expertise = result.expertise_;
        teachingExperience = result.experience;
        qualification = result.qualification_;
        preferredlocation = result.pref_location;

        spUtils.addProfileDetails(result.name_, result.age_, result.email_, result.contact, result.address_, result.city_,
                result.state_, result.expertise_, result.experience, result.qualification_, result.pref_location);

        progressDialog.dismiss();
        _submit.setEnabled(true);
        finish();
        Intent i = new Intent();
        i.setClass(ProfileChangeActivity.this, DashboardActivity.class);
        startActivity(i);
    }

    public void onUpdateFailed() {
        Toast.makeText(getBaseContext(), "Update failed", Toast.LENGTH_LONG).show();
        _submit.setEnabled(true);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
