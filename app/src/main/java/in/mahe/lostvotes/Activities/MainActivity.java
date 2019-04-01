package in.mahe.lostvotes.Activities;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import in.mahe.lostvotes.R;

public class MainActivity extends AppCompatActivity {
    private LinearLayout dataShow,dataCapture;
    private EditText nameEditText, domicileEditText, constituencyEditText, voterIDEditText, aadharEditText;
    private String name, domicile, constituency, voterID, aadhar, currentUserID;
    private TextView nameTextView, domicileTextView, constituencyTextView, voterIDTextView, aadharTextView;
    private Button submit,edit,vote;
    FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        db=FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        try {
            currentUserID = user.getUid();
        }catch (Exception e){
            Log.d("MainActivity", "onCreate: getUid returned null");
        }
        boolean isNewUser=getIntent().getBooleanExtra("isNewUser",false);

        if(isNewUser) //TODO Refine this if-clause to include new users without their data captured
            dataCapture.setVisibility(View.VISIBLE);
        else
            dataShow.setVisibility(View.GONE); //TODO Write a separate method for setting layout visibility and showing data

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitDetailsToDB();
            }
        });
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editDetails();
            }
        });
        vote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                proceedToVote();
            }
        });
    }

    private void submitDetailsToDB(){
        try {
            name = nameEditText.getText().toString();
            domicile = domicileEditText.getText().toString();
            constituency = constituencyEditText.getText().toString();
            voterID = voterIDEditText.getText().toString();
            aadhar = aadharEditText.getText().toString();

            Map<String, Object> user = new HashMap<>();
            user.put("name", name);
            user.put("domicile", domicile);
            user.put("constituency", constituency);
            user.put("voterID", voterID);
            user.put("aadharID", aadhar);

            db.collection("Users")
                    .document(currentUserID)
                    .set(user)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //TODO Make an alert dialog pop-up here, clicking okay hides data capture and shows data
                            Log.d("MainActivity", "DocumentSnapshot added with ID: " + currentUserID);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("MainActivity", "Error adding document", e);
                        }
                    });
        }catch (Exception e){
            //TODO: Set an alert dialog for the missing value
        }
    }
    private void editDetails(){

    }
    private void proceedToVote(){

    }

    private void initViews(){
        dataShow=findViewById(R.id.data_show_layout);
        dataCapture=findViewById(R.id.data_capture_layout);
        nameEditText=findViewById(R.id.name_edit_text);
        domicileEditText=findViewById(R.id.domicile_edit_text);
        constituencyEditText=findViewById(R.id.constituency_edit_text);
        voterIDEditText=findViewById(R.id.voterid_edit_text);
        aadharEditText=findViewById(R.id.aadhar_edit_text);
        nameTextView=findViewById(R.id.name_tv);
        domicileTextView=findViewById(R.id.domicile_tv);
        constituencyTextView=findViewById(R.id.constituency_tv);
        voterIDTextView=findViewById(R.id.voter_tv);
        aadharTextView=findViewById(R.id.aadhar_tv);
        submit=findViewById(R.id.submit_deets);
        edit=findViewById(R.id.edit_deets);
        vote=findViewById(R.id.vote_button);
    }
}
