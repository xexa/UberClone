package com.example.uberclone;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uberclone.model.DriverInfo;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int REQUEST_LOGIN_CODE = 7171;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    private FirebaseDatabase database;
    private DatabaseReference driverRefInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);


        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        delaySplashScreen();
    }

    @Override
    protected void onStop() {
        if (firebaseAuth != null && listener != null) {
            firebaseAuth.removeAuthStateListener(listener);
        }
        super.onStop();
    }

    private void init() {

        ButterKnife.bind(this);

        database = FirebaseDatabase.getInstance();
        driverRefInfo = database.getReference(Common.DRIVER_INFO_REFERENCE);

        providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        firebaseAuth = FirebaseAuth.getInstance();

        listener = myFirebaseAuth -> {
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if (user != null) {
                checkUserFromFirebase();
            } else {
                showLoginLayout();
            }
        };

    }

    private void checkUserFromFirebase() {
        driverRefInfo.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(SplashScreenActivity.this, "User already registered", Toast.LENGTH_SHORT).show();
                        } else {
                            showRegisterLayout();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SplashScreenActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterLayout() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null);

        TextInputEditText edtFirstName = itemView.findViewById(R.id.edt_first_name);
        TextInputEditText edtLastName = itemView.findViewById(R.id.edt_last_name);
        TextInputEditText edtPhoneNumber = itemView.findViewById(R.id.edt_phone_number);

        Button continueButton = itemView.findViewById(R.id.btn_register);

        // Set data
        if (FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null &&
                TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber())){
            edtPhoneNumber.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
        }

        //set view
        builder.setView(itemView);

        AlertDialog dialog = builder.create();
        dialog.show();

        continueButton.setOnClickListener(v -> {
            if (TextUtils.isEmpty(edtFirstName.getText().toString())){
                Toast.makeText(this, "Please enter first name", Toast.LENGTH_SHORT).show();
                return;
            }else if (TextUtils.isEmpty(edtLastName.getText().toString())){
                Toast.makeText(this, "Please enter last name", Toast.LENGTH_SHORT).show();
                return;
            }else if (TextUtils.isEmpty(edtPhoneNumber.getText().toString())){
                Toast.makeText(this, "Please enter phone nnumber", Toast.LENGTH_SHORT).show();
                return;
            }else {

                DriverInfo driverInfo = new DriverInfo();

                driverInfo.setFirstName(edtFirstName.getText().toString());
                driverInfo.setLastName(edtLastName.getText().toString());
                driverInfo.setPhoneNumber(edtPhoneNumber.getText().toString());
                driverInfo.setRating(0.0);

                driverRefInfo.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(driverInfo)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                dialog.dismiss();
                                Toast.makeText(SplashScreenActivity.this, "Failure: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(SplashScreenActivity.this, "Register successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        });
            }
        });
    }

    private void showLoginLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.btn_phone_sign_in)
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build();

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setIsSmartLockEnabled(false)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .build(), REQUEST_LOGIN_CODE);
    }

    private void delaySplashScreen() {

        progressBar.setVisibility(View.VISIBLE);

        Completable.timer(5, TimeUnit.SECONDS,
                AndroidSchedulers.mainThread())
                .subscribe(() ->


                        firebaseAuth.addAuthStateListener(listener));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IdpResponse response = IdpResponse.fromResultIntent(data);

        if (requestCode == REQUEST_LOGIN_CODE) {
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            } else {
                Toast.makeText(this, "[Error]: " + response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}