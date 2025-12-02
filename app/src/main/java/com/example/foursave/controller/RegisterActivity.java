package com.example.foursave.controller;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foursave.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirm;
    private Button btnRegister;
    private TextView tvLoginLink;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        // Pastikan URL Database benar sesuai google-services.json Anda
        mDatabase = FirebaseDatabase.getInstance("https://foursave-keren-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mendaftar...");
        progressDialog.setCancelable(false);

        // Binding Views
        etName = findViewById(R.id.regName);
        etEmail = findViewById(R.id.regEmail);
        etPassword = findViewById(R.id.regPassword);
        etConfirm = findViewById(R.id.regConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnRegister.setOnClickListener(v -> performRegister());

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void performRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etName.setError("Nama wajib diisi"); return; }
        if (TextUtils.isEmpty(email)) { etEmail.setError("Email wajib diisi"); return; }
        if (TextUtils.isEmpty(password) || password.length() < 6) { etPassword.setError("Password min 6 karakter"); return; }
        if (!password.equals(confirm)) { etConfirm.setError("Password tidak cocok"); return; }

        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 1. Simpan Nama ke Profil Auth (PENTING untuk Sapaan di Dashboard)
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                                // 2. Simpan data tambahan ke Database
                                saveUserData(user.getUid(), name, email);
                            });
                        }
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Gagal: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(String uid, String name, String email) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);

        mDatabase.child(uid).setValue(userMap)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Gagal simpan database", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}