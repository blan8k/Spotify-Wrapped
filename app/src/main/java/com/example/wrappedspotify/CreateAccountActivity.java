package com.example.wrappedspotify;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class CreateAccountActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText editTextEmail, editTextPassword;
    private Button buttonCreateAccount;
    private TextView textViewSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextNewEmail);
        editTextPassword = findViewById(R.id.editTextNewPassword);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);
        textViewSignIn = findViewById(R.id.textViewSignIn);

        buttonCreateAccount.setOnClickListener(view -> createAccount(editTextEmail.getText().toString().trim(),
                editTextPassword.getText().toString().trim()));

        textViewSignIn.setOnClickListener(view -> {
            Intent intent = new Intent(CreateAccountActivity.this, SignInActivity.class);
            startActivity(intent);
        });
    }

    private void createAccount(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(CreateAccountActivity.this, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(CreateAccountActivity.this, "Account created successfully. Please link your Spotify account.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CreateAccountActivity.this, LinkSpotifyActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = "Account creation failed.";
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "An account with this email already exists.";
                        }
                        Toast.makeText(CreateAccountActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
