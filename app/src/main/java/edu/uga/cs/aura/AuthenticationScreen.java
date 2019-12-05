/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

/**
 * creates the authentication screen
 */
public class AuthenticationScreen extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private FirebaseAuth auth;
    private boolean login;
    private DatabaseReference dbRootRef;

    /**
     * Overrides the onCreate method to set up the views on the authentication screen
     * @param savedInstanceState bundle for the activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authentication_fragment);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button submitButton = findViewById(R.id.submitButton);
        dbRootRef = FirebaseDatabase.getInstance().getReference();

        Intent intent = getIntent();
        login = intent.getBooleanExtra("login",true);
        auth = FirebaseAuth.getInstance();

        if (login){
            submitButton.setText("Login");
        }
        else {
            submitButton.setText("Register");
        }

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (login){
                    login(emailEditText.getText().toString(), passwordEditText.getText().toString());
                }
                else {
                    createAccount(emailEditText.getText().toString(), passwordEditText.getText().toString());
                }

            }
        });
    }

    /**
     * Creates an account for the user
     * @param email email that the user wants to use to create the account
     * @param password password that the user wants to use to create the account
     */
    private void createAccount(String email, String password) {
        if (!validateForm()) {
            return;
        }

        findViewById(R.id.submitButton).setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d("CreateAccount", "createUserWithEmail:success");
                    FirebaseUser user = auth.getCurrentUser();
                    writeInitialUserDataToDB(user);
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("CreateAccount", "createUserWithEmail:failure", task.getException());
                    Toast.makeText(AuthenticationScreen.this, "Authentication failed. " + task.getException().toString(),
                            Toast.LENGTH_LONG).show();
                }
                findViewById(R.id.submitButton).setEnabled(true);
            }
        });
    }

    /**
     * Logs the user into the account
     * @param email email that the user registered with the app
     * @param password password that the user registered with the app
     */
    private void login(String email, String password) {
        if (!validateForm()) {
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Login", "signInWithEmail:success");
                    FirebaseUser user = auth.getCurrentUser();
                    mainActivityIntent();
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Login", "signInWithEmail:failure", task.getException());
                    Toast.makeText(AuthenticationScreen.this,
                            "Login failed. " + task.getException().toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Makes sure that the user does not leave the email and password fields empty
     */
    private boolean validateForm() {
        boolean valid = true;

        String email = emailEditText.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Required.");
            valid = false;
        } else {
            emailEditText.setError(null);
        }

        String password = passwordEditText.getText().toString();
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Required.");
            valid = false;
        } else {
            passwordEditText.setError(null);
        }

        return valid;
    }

    /**
     * Creates an intent to go to the main activity
     */
    void mainActivityIntent(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Writes the info that the user provides to the database
     * @param fbUser the Firebase user that is trying create an account
     */
    public void writeInitialUserDataToDB(FirebaseUser fbUser){
        HashMap<String, String> userData = new HashMap<>();
        userData.put("uid", fbUser.getUid());
        userData.put("imgUrl", "DEFAULT");
        dbRootRef.child("Users").child(fbUser.getUid()).setValue(userData);
        mainActivityIntent();
    }
}
