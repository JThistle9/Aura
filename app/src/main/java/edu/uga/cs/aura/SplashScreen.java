/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * creates the Splash Screen
 */
public class SplashScreen extends AppCompatActivity {

    private Button loginButton, registerButton;
    private FirebaseAuth auth;
    private FirebaseUser fbUser;

    /**
     * Overrides the onCreate method to set up the display
     * @param savedInstanceState bundle for the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        auth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authenticationScreenIntent(true);
            }
        });
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authenticationScreenIntent(false);
            }
        });

    }

    /**
     * Overrides the onStart method to go to the main activity if the user is already authenticated
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Check auth on Activity start
        fbUser = auth.getCurrentUser();
        if (fbUser != null) {
            SplashToMainActivityIntent();
        }
    }

    /**
     * Creates an intent from the splash screen to the authentication screen
     * @param login if the user wants to login on true and register on false
     */
    void authenticationScreenIntent(boolean login){
        Intent intent = new Intent(this, AuthenticationScreen.class);
        intent.putExtra("login", login);
        startActivity(intent);
    }


    /**
     * Creates an intent from the splash screen to the main activity if the user is already logged in
     */
    void SplashToMainActivityIntent(){
        //go to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
