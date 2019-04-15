package in.mahe.lostvotes.Activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.mahe.lostvotes.R;

public class SelectingParty extends AppCompatActivity {
    FirebaseFirestore db;
    public static String PREF_NAME="shared values";
    private String name, domicile, city, constituency, voterID, aadhar, currentUserID;
    private ListView lv;
    private Button submitVote;
    private Dialog dialog;
    private Typeface typeface;
    String checkedParty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selecting_party);
        db=FirebaseFirestore.getInstance();
        submitVote =findViewById(R.id.submit_vote);
        lv = findViewById(R.id.party_list);
        typeface=Typeface.createFromAsset(getAssets(), "fonts/Gilroy-Light.otf");
        List<String> l=new ArrayList<>();
        submitVote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SparseBooleanArray sp = lv.getCheckedItemPositions();
                for(int i = 0; i < sp.size(); i++){
                    int pos = sp.keyAt(i);
                    if(sp.valueAt(i))
                        checkedParty = lv.getItemAtPosition(pos).toString();
                }
                new AlertDialog.Builder(SelectingParty.this)
                        .setTitle("Warning")
                        .setMessage("Are you sure you want to vote ?")
                        .setIcon(R.drawable.ic_error)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setDialog("Sending your vote");//call progrss dialog
                                getVoteCountAndCastVote();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        }).show();
            }
        });
        retValCallDB();
    }
    
    public void retValCallDB(){
        SharedPreferences prefs=getSharedPreferences(PREF_NAME,MODE_PRIVATE);
        name=prefs.getString("name","");
        domicile=prefs.getString("domicile", "");
        city=prefs.getString("city","");
        constituency=prefs.getString("constituency", "");
        voterID=prefs.getString("voterID", "");
        aadhar=prefs.getString("aadharID", "");

        getAndSetCandidateList();
    }
    
    
    public void getAndSetCandidateList(){
        DocumentReference docRef = db.collection("States").document(domicile).collection("Cities").document(city);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    try {
                        List<String> list  = (List<String>) task.getResult().get(constituency);
                        ArrayAdapter<String> aa= new ArrayAdapter<>(SelectingParty.this,android.R.layout.simple_list_item_single_choice,list);
                        lv.setAdapter(aa);
                        lv.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                    }catch (Exception e){
                        Log.d("SelectingParty", "Exception thrown for list");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void getVoteCountAndCastVote(){
        DocumentReference docRef = db.collection("VoteCount").document(city); //Creating a document reference for the required doc in a collection
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() { //getting data using the docref
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                int n = checkedParty.lastIndexOf("P");
                String PartyName = checkedParty.substring(n);
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        if(document.get(PartyName)!=null){
                            int i=Integer.parseInt(document.get(PartyName).toString());
                            castVote(i,PartyName);
                        }
                    } else {
                        castVote(0, PartyName);
                    }
                } else {
                    //When task fails
                    if(dialog.isShowing()&& dialog!=null)
                        dialog.dismiss();
                    showAlert(false,R.drawable.ic_sad, "Something went wrong while connecting to the Database, check Internet and try again.");
                }
            }
        });
    }

    private void castVote(int voteCount, String partyName){
        voteCount++;
        Map<String, Object> vc = new HashMap<>();  // Creating an object for a document
        vc.put(partyName, voteCount);
        db.collection("VoteCount") //References the collection in the Db
                .document(city)    // setting the document ID to user ID generated by Firebase
                .set(vc)                  // adding the object to the db
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        setVoteStatus();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if(dialog.isShowing()&& dialog!=null)
                            dialog.dismiss();
                        showAlert(true,R.drawable.ic_sad, "Something went wrong while connecting to the Database, check Internet and try again.");
                        Log.w("SelectingParty", "Error adding document", e);
                    }
                });
    }

    private void setVoteStatus(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserID=user.getUid();
        Map<String, Object> updates = new HashMap<>();  // Creating an object for a document
        updates.put("voted","1");
        db.collection("Users") //References the collection in the Db
                .document(currentUserID)    // setting the document ID to user ID generated by Firebase
                .update(updates)                  // adding the object to the db
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if(dialog.isShowing()&& dialog!=null)
                            dialog.dismiss();
                        showAlert(true,R.drawable.ic_happy, "You've successfully voted !!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if(dialog.isShowing()&& dialog!=null)
                            dialog.dismiss();
                        showAlert(true,R.drawable.ic_sad, "Something went wrong while connecting to the Database, check Internet and try again.");
                    }
                });
    }

    private void setDialog(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.item_progress,null);
        builder.setView(view);
        TextView t=view.findViewById(R.id.loading_msg);
        t.setTypeface(typeface);
        t.setText(message);
        dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void showAlert(final boolean moveForward, int icon, String message){
        new AlertDialog.Builder(this)
                .setTitle("Alert")
                .setIcon(icon)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(moveForward)
                            startActivity(new Intent(SelectingParty.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("isNewUser",false));
                    }
                }).show();
    }
}
