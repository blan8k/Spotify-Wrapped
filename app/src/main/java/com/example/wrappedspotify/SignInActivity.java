package com.example.wrappedspotify;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText editTextEmail, editTextPassword;
    private Button buttonSignIn;
    private TextView textViewCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        textViewCreateAccount = findViewById(R.id.buttonGoToCreateAccount);

        buttonSignIn.setOnClickListener(view -> {
            signInUser(editTextEmail.getText().toString().trim(), editTextPassword.getText().toString().trim());
        });

        textViewCreateAccount.setOnClickListener(view -> {
            Intent intent = new Intent(SignInActivity.this, CreateAccountActivity.class);
            startActivity(intent);
        });
    }

    private void signInUser(String email, String password) {
        if (!email.isEmpty() && !password.isEmpty()) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(SignInActivity.this, HomePageActivity.class);
                            startActivity(intent);

                            finish();

                        } else {
                            Toast.makeText(SignInActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(SignInActivity.this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
        }
    }


}
