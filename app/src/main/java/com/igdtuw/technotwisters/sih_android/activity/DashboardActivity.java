package com.igdtuw.technotwisters.sih_android.activity;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.igdtuw.technotwisters.sih_android.OtherFiles.CircleTransform;
import com.igdtuw.technotwisters.sih_android.OtherFiles.GPSService;
import com.igdtuw.technotwisters.sih_android.OtherFiles.GPSTracker;
import com.igdtuw.technotwisters.sih_android.OtherFiles.NotificationReceiver;
import com.igdtuw.technotwisters.sih_android.OtherFiles.P2PTracker;
import com.igdtuw.technotwisters.sih_android.OtherFiles.P2PTracker.Scanner;
import com.igdtuw.technotwisters.sih_android.OtherFiles.TrackGPS;
import com.igdtuw.technotwisters.sih_android.R;
import com.igdtuw.technotwisters.sih_android.OtherFiles.SharedPreferencesUtils;
import com.igdtuw.technotwisters.sih_android.api.ApiClient;
import com.igdtuw.technotwisters.sih_android.fragments.Dashboard_HomeFragment;
import com.igdtuw.technotwisters.sih_android.fragments.Dashboard_NotificationFragment;
import com.igdtuw.technotwisters.sih_android.fragments.Dashboard_SettingFragment;
import com.igdtuw.technotwisters.sih_android.fragments.Dashboard_ToDoFragment;
import com.igdtuw.technotwisters.sih_android.fragments.FingerprintAuthenticationDialogFragment;
import com.igdtuw.technotwisters.sih_android.model.Result;
import com.igdtuw.technotwisters.sih_android.todo.ToDo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    int mark;
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private View navHeader;
    private ImageView imgNavHeaderBg, imgProfile, text_mark, text_track, text_profile, text_todo;
    private TextView txtName, txtWebsite;
    private Toolbar toolbar;

    SharedPreferencesUtils spUtils;

    // urls to load navigation header background image
    // and profile image
    private static final String urlNavHeaderBg = "http://api.androidhive.info/images/nav-dashboard_toolbar_menu-header-bg.jpg";
    private static final String urlProfileImg = "https://lh3.googleusercontent.com/eCtE_G34M9ygdkmOpYvCag1vBARCmZwnVS6rS5t4JLzJ6QgQSBquM0nuTsCpLhYbKljoyS-txg";

    // index to identify current nav dashboard_toolbar_menu item
    public static int navItemIndex = 0;

    Call<Result> logoutUser;

    private static final int REQUEST_CODE_LOCATION_FINE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_activity);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nav_view);


        // Navigation view header
        navHeader = navigationView.getHeaderView(0);
        txtName = (TextView) navHeader.findViewById(R.id.name);
        txtWebsite = (TextView) navHeader.findViewById(R.id.website);

        initView();
        initClickListener();

        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        //***********NOTIFICATION GENERATOR************
        AlarmManager alarmMgr;
        PendingIntent alarmIntent;

        alarmMgr = (AlarmManager) DashboardActivity.this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(DashboardActivity.this, NotificationReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(DashboardActivity.this, 0, intent, 0);

        // Set the alarm to start at 8:30 a.m.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 00);
        calendar.set(Calendar.SECOND, 00);
        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);


        //*************** For tracking location randomly
        Thread myThread = null;

        Runnable myRunnableThread = new CountDownRunner();
        myThread = new Thread(myRunnableThread);
        myThread.start();

        if (savedInstanceState == null) {
            navItemIndex = 0;
            Dashboard_HomeFragment homeFragment = new Dashboard_HomeFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_dashboard, homeFragment).commit();
        }

        askLocationPermission();

    }

    private void initView(){
        text_mark = (ImageView) findViewById(R.id.text_mark);
        text_profile = (ImageView) findViewById(R.id.text_profile);
        text_track = (ImageView) findViewById(R.id.text_track);
        text_todo = (ImageView) findViewById(R.id.text_rem);
        spUtils = new SharedPreferencesUtils(DashboardActivity.this);
    }

    private void initClickListener(){
        text_mark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spUtils.isSchoolAdded()) {
                    // TODO: first check if user is within the time period to mark attendance
                    onCreateDialogSingleChoice().show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
                    builder.setTitle("You aren't allowed this action!");
                    builder.setMessage("Click ok to add school first");
                    LayoutInflater inflater = getLayoutInflater();
                    v = inflater.inflate(R.layout.dialog_confirm_logout, null);
                    builder.setView(v);
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setClass(DashboardActivity.this, AddSchoolActivity.class);
                            startActivity(intent);
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.create().show();
                }
            }
        });
        text_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DashboardActivity.this, ProfileChangeActivity.class);
                startActivity(i);
            }
        });
        text_track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DashboardActivity.this, Self_track.class);
                startActivity(i);
            }
        });
        text_todo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DashboardActivity.this, ToDo.class);
                startActivity(i);
            }
        });
    }

    public void doWork() {

        runOnUiThread(new Runnable() {
            public void run() {
                try {

                    Calendar c = Calendar.getInstance();
                    int seconds = c.get(Calendar.SECOND);
                    int minutes = c.get(Calendar.MINUTE);
                    int hours = c.get(Calendar.HOUR_OF_DAY);
                    String curTime = hours + ":" + minutes + ":" + seconds;
                    GPSTracker gps = new GPSTracker(DashboardActivity.this);
                } catch (Exception e) {
                }
            }

        });


    }


    class CountDownRunner implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    doWork();
                    long names[] = {30, 20, 40, 60};
                    Random Dice = new Random();
                    int n = Dice.nextInt(names.length);

                    Thread.sleep(n * 60 * 1000); // Pause of random tme in  Second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                }
            }
        }
    }

    /***
     * Load navigation dashboard_toolbar_menu header information
     * like background image, profile image
     * name, website, notifications action view (dot)
     */
    private void loadNavHeader() {
        // name, website
        txtName.setText("Ravi Tamada");
        txtWebsite.setText("www.androidhive.info");

        // loading header background image
        Glide.with(this).load(urlNavHeaderBg)
                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgNavHeaderBg);

        // Loading profile image
        Glide.with(this).load(urlProfileImg)
                .crossFade()
                .thumbnail(0.5f)
                .bitmapTransform(new CircleTransform(this))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgProfile);

        // showing dot next to notifications label
        navigationView.getMenu().getItem(3).setActionView(R.layout.menu_dot);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            Dashboard_HomeFragment homeFragment = new Dashboard_HomeFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_dashboard, homeFragment).commit();
        } else if (id == R.id.nav_today_attendance) {
            /*if (schoolAdded) {
                // TODO: first check if user is within the time period to mark attendance
                onCreateDialogSingleChoice().show();
            } else {*/
            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
            builder.setTitle("You aren't allowed this action!");
            builder.setMessage("Click ok to add school first");
            LayoutInflater inflater = getLayoutInflater();
            View v = inflater.inflate(R.layout.dialog_confirm_logout, null);
            builder.setView(v);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent();
                    intent.setClass(DashboardActivity.this, AddSchoolActivity.class);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.create().show();

        } else if (id == R.id.nav_track_attendance) {
            /*if (schoolAdded) {
                Dashboard_TrackFragment trackFragment = new Dashboard_TrackFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_dashboard, trackFragment).commit();
            } else {*/
            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
            builder.setTitle("You aren't allowed this action!");
            builder.setMessage("Click ok to add school first");
            LayoutInflater inflater = getLayoutInflater();
            View v = inflater.inflate(R.layout.dialog_confirm_logout, null);
            builder.setView(v);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent();
                    intent.setClass(DashboardActivity.this, AddSchoolActivity.class);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.create().show();

        } else if (id == R.id.nav_to_do) {
            Dashboard_ToDoFragment toDoFragment = new Dashboard_ToDoFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_dashboard, toDoFragment).commit();
        } else if (id == R.id.nav_notifications) {
            Dashboard_NotificationFragment notificationFragment = new Dashboard_NotificationFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_dashboard, notificationFragment).commit();
        } else if (id == R.id.nav_settings) {
            Dashboard_SettingFragment settingFragment = new Dashboard_SettingFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_dashboard, settingFragment).commit();
        } else if (id == R.id.nav_fingerprint_record) {
            if (Build.VERSION_CODES.M <= VERSION.SDK_INT) {
                Intent intent = new Intent();
                intent.setClass(DashboardActivity.this, FpActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), "Only works for Android M and above", Toast.LENGTH_LONG).show();
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            Intent i = new Intent(this, ProfileChangeActivity.class);
            startActivity(i);
        } else if (id == R.id.action_logout) {
            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
            builder.setTitle("Confirm");
            builder.setMessage("Are you sure you want to logout?");
            LayoutInflater inflater = getLayoutInflater();
            View v = inflater.inflate(R.layout.dialog_confirm_logout, null);
            builder.setView(v);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    logoutUser = ApiClient.getInterface().logoutUser(spUtils.getUsername(), spUtils.getAccessToken());
                    logoutUser.enqueue(new Callback<Result>() {
                        @Override
                        public void onResponse(Call<Result> call, Response<Result> response) {
                            if (response.isSuccessful()) {
                                Intent i = new Intent(DashboardActivity.this, LoginActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);
                                ActivityCompat.finishAffinity(DashboardActivity.this);
                            } else {
                                Toast.makeText(DashboardActivity.this, "Logout failed: " + response.errorBody(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Result> call, Throwable t) {
                            Toast.makeText(DashboardActivity.this, "Logout failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.create().show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Dialog onCreateDialogSingleChoice() {
        mark = 1;
        String present = "Present";
        String absent = "Absent";
        String holiday = "Holiday";
        AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
        CharSequence[] array = {present, absent, holiday};
        builder.setTitle("Mark your Attendance");

        builder.setSingleChoiceItems(array, 1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    Calendar c = Calendar.getInstance();
                    int seconds = c.get(Calendar.SECOND);
                    int minutes = c.get(Calendar.MINUTE);
                    int hours = c.get(Calendar.HOUR_OF_DAY);

                    if (hours > 20 && minutes > 30) {
                        mark = -1;

                        Toast.makeText(getApplicationContext(), "You cannot mark your attendance after 8:30 am", Toast.LENGTH_LONG).show();
                    } else {
                        mark = 0;
                    }
                } else if (which == 1) // absent
                {
                    mark = 1;
                    AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
                    CharSequence[] array = {"Casual Leave", "Child Care Leave", "Hospital Leave", "Half Pay leaves", "Others"};

                    builder.setTitle("Reason for your absence");
                    builder.setSingleChoiceItems(array, 0, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                } else                // holiday
                    mark = 2;
            }
        });
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener()

        {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (mark == 0) {
                    getFingerPrint();
                }
            }


        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()

        {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        return builder.create();
    }

    public void startService(View view) {
        startService(new Intent(getBaseContext(), GPSService.class));
    }

    // Method to stop the service
    public void stopService(View view) {
        stopService(new Intent(getBaseContext(), GPSService.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_LOCATION_FINE: {
                //TODO : In Case of not accepting location permission
                break;
            }
        }
    }


    /** Private Methods **/

    /**
     * Ask FINE Location Running Permission
     */
    private void askLocationPermission() {
        //For Fine Location Permission in API Level 23 and above (Running Permission)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_FINE);
        }
    }

    private void getListOfBluetoothDevices() {

        List<String> addresses = new ArrayList<>();

        addresses.add("72:9A:31:38:A9:3D");

        final Context context = this;

//        new P2PTracker().validateAddresses(getApplicationContext(), addresses, new ScannerValidation() {
//            @Override
//            public void validationComplete(boolean output) {
//                Toast.makeText(context, "Address validated " + Boolean.toString(output), Toast.LENGTH_LONG).show();
//                System.out.println("Address validated " + Boolean.toString(output));
//            }
//        });

        new P2PTracker().startAllScan(context, new Scanner() {
            @Override
            public void scanningComplete(final List<String> listOfAddress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println(listOfAddress.size());
                        Toast.makeText(context, "Bluetooth Devices Fetching finished!!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

    }

    private void getFingerPrint() {
        if (Build.VERSION_CODES.M > VERSION.SDK_INT) {
            triggerAction();
            return;
        }

        showFingerprintDialog();
    }

    public void triggerAction() {
        getListOfBluetoothDevices();
        TrackGPS gps = new TrackGPS(DashboardActivity.this);
        if (gps.canGetLocation()) {

            double longitude = gps.getLongitude();
            double latitude = gps.getLatitude();
            Toast.makeText(getApplicationContext(), "Longitude:" + Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude), Toast.LENGTH_SHORT).show();
        } else {
            askLocationPermission();
        }
    }

    private void showFingerprintDialog() {
        FingerprintAuthenticationDialogFragment fragment
                = new FingerprintAuthenticationDialogFragment();
        fragment.setStage(
                FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
        fragment.show(getFragmentManager(), "myFragment");
    }

}
