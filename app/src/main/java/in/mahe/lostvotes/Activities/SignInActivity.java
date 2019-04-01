package in.mahe.lostvotes.Activities;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import in.mahe.lostvotes.R;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG="phoneAuth";
    private EditText phoneNumber,vCode;
    private Button sendButton,resendButton, verCodeButton;

    private String phoneVerificationID;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks verificationCallbacks;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private FirebaseAuth fbAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        phoneNumber=findViewById(R.id.phone_number);
        vCode=findViewById(R.id.verification_code);

        sendButton=findViewById(R.id.send_code);
        resendButton=findViewById(R.id.resend_code);
        verCodeButton=findViewById(R.id.verify_code);

        verCodeButton.setEnabled(false);
        resendButton.setEnabled(false);
        fbAuth=FirebaseAuth.getInstance();
    }

    public void sendCode(View view){
        Log.d(TAG, "sendCode: Inside");
        String pNumber=phoneNumber.getText().toString();
        Log.d(TAG, "sendCode: "+pNumber);
        setUpVerificatonCallbacks();

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                pNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                verificationCallbacks);

    }

    private void setUpVerificatonCallbacks() {
        Log.d(TAG, "setUpVerificatonCallbacks: inside");

        verificationCallbacks =
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(
                            PhoneAuthCredential credential) {

                        //resendButton.setEnabled(false);
                        //verCodeButton.setEnabled(false);
                        //Log.d(TAG, "onVerificationCompleted: ");
                        //signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {

                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            // Invalid request
                            Log.d(TAG, "Invalid credential: "
                                    + e.getLocalizedMessage());
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            // SMS quota exceeded
                            Log.d(TAG, "SMS Quota exceeded.");
                        }
                    }

                    @Override
                    public void onCodeSent(String verificationId,
                                           PhoneAuthProvider.ForceResendingToken token) {
                        Log.d(TAG, "onCodeSent: ");
                        phoneVerificationID = verificationId;
                        resendToken = token;

                        verCodeButton.setEnabled(true);
                        sendButton.setEnabled(false);
                        resendButton.setEnabled(true);
                    }
                };
    }

    public void resendCode(View view){
        String phoneNumber=this.phoneNumber.getText().toString();
        setUpVerificatonCallbacks();

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                verificationCallbacks,
                resendToken);

    }

    public void verifyCode(View view){
        String code=vCode.getText().toString().trim();
        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(phoneVerificationID, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        fbAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            resendButton.setEnabled(false);
                            verCodeButton.setEnabled(false);
                            FirebaseUser user = task.getResult().getUser();
                            boolean isNew=task.getResult().getAdditionalUserInfo().isNewUser();
                            startActivity(new Intent(SignInActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("isNewUser",isNew));

                        } else {
                            if (task.getException() instanceof
                                    FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }
}