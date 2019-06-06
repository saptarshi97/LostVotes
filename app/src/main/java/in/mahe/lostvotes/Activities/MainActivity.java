package in.mahe.lostvotes.Activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.mahe.lostvotes.R;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    private LinearLayout dataShow,dataCapture;
    private EditText nameEditText, voterIDEditText, aadharEditText;
    private String name, domicile, city, constituency, voterID, aadhar, currentUserID, voteFlag;
    private TextView nameTextView, domicileTextView, constituencyTextView, voterIDTextView, aadharTextView;
    private Button submit,edit,vote;
    private Spinner dSpinner,conSpinner,citySpinner;
    private boolean returnVal;
    public static String PREF_NAME="shared values";
    private Typeface typeface;
    private Dialog dialog;
    FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        dSpinner.setOnItemSelectedListener(this);
        conSpinner.setOnItemSelectedListener(this);
        citySpinner.setOnItemSelectedListener(this);
        db=FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();  // Getting the current user
        try {
            currentUserID = user.getUid();//getting the unique firebase generated user ID for logged-in user
        }catch (Exception e){
            Log.d("MainActivity", "onCreate: getUid returned null");
        }

        boolean isNewUser=getIntent().getBooleanExtra("isNewUser",false);
        if(isNewUser) {
            setDataCapture();
        }
        else
            loadAndShowData();

        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        Uri deepLink=null;
                        if(pendingDynamicLinkData!=null){
                            deepLink=pendingDynamicLinkData.getLink();
                            Log.d("MainActivity", "onSuccess: deepLink:"+deepLink);
                            Log.d("MainActivity", "onSuccess: data="+deepLink.getQueryParameter("requestID"));
                            Log.d("MainActivity", "onSuccess: data="+deepLink.getQueryParameter("extra1"));
                            Log.d("MainActivity", "onSuccess: data="+deepLink.getQueryParameter("extra2"));
                        }
                    }
                });

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

    private void setDataCapture(){
        //Call to Firebase to get list of States
        DocumentReference docRef = db.collection("States").document("stateList");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
        @Override
        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
            if (task.isSuccessful()) {
                List<String> list = new ArrayList<>();
                Map<String, Object> map = task.getResult().getData();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    list.add(entry.getValue().toString());
                    Log.d("MainActivity", entry.getKey());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),  android.R.layout.simple_spinner_dropdown_item, list);
                adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
                dSpinner.setAdapter(adapter);
                dataCapture.setVisibility(View.VISIBLE);
            }
        }
        });
    }

    private void submitDetailsToDB(){   //Method to push data to db
        try {
            name = nameEditText.getText().toString();
            voterID = voterIDEditText.getText().toString();
            aadhar = aadharEditText.getText().toString();

            if(name.isEmpty()){
                showAlert("Name field cannot be empty");
                return;
            }
            if(voterID.isEmpty()){
                showAlert("VoterID cannot be empty");
                return;
            }
            if(aadhar.isEmpty()){
                showAlert("aadharID cannot be empty");
                return;
            }


            Map<String, Object> user = new HashMap<>();  // Creating an object for a document
            user.put("name", name);
            user.put("domicile", domicile);
            user.put("city",city);
            user.put("constituency", constituency);
            user.put("voterID", voterID);
            user.put("aadharID", aadhar);
            user.put("voted",0);

            SharedPreferences.Editor editor=getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
            editor.putString("name",name);
            editor.putString("domicile", domicile);
            editor.putString("city",city);
            editor.putString("constituency", constituency);
            editor.putString("voterID", voterID);
            editor.putString("aadharID", aadhar);
            editor.putString("voteFlag","0");
            editor.apply();

            setDialog("Pushing details to db");
            db.collection("Users") //References the collection in the Db
                    .document(currentUserID)    // setting the document ID to user ID generated by Firebase
                    .set(user)                  // adding the object to the db
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if(dialog.isShowing()&& dialog!=null)
                                dialog.dismiss();
                            loadAndShowData(); //Loading the data pushed to the db
                            Log.d("MainActivity", "DocumentSnapshot added with ID: " + currentUserID);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if(dialog.isShowing()&& dialog!=null)
                                dialog.dismiss();
                            Log.w("MainActivity", "Error adding document", e);
                        }
                    });
        }catch (Exception e){
            showAlert("Error pushing to db");
        }
    }

    private void editDetails(){
        if(!voteFlag.isEmpty() && voteFlag.equals("0")) {
            dataShow.setVisibility(View.GONE);
            setDataCapture();
        }else
            showAlert("You have already voted. Thank you!\nIf you haven't voted please contact support@SomeGovSite.in");
    }

    private void proceedToVote(){
        if(!voteFlag.isEmpty() && voteFlag.equals("0"))
            startActivity(new Intent(this, PermissionAcquisitionActivity.class));
        else
            showAlert("You have already voted. Thank you!\nIf you haven't voted please contact support@SomeGovSite.in");
    }

    private void loadAndShowData(){
        dataCapture.setVisibility(View.GONE);
        setDialog("Loading saved data...");
        //Call to firebase and data setting
        DocumentReference docRef = db.collection("Users").document(currentUserID); //Creating a document reference for the required doc in a collection
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() { //getting data using the docref
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("MainActivity", "DocumentSnapshot data: " + document.getData());
                        nameTextView.setText(document.get("name").toString());
                        domicileTextView.setText(document.get("domicile").toString());
                        constituencyTextView.setText(document.get("constituency").toString());
                        aadharTextView.setText(document.get("aadharID").toString());
                        voterIDTextView.setText(document.get("voterID").toString());
                        voteFlag=document.get("voted").toString();

                        SharedPreferences.Editor editor=getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                        editor.putString("name",document.get("name").toString());
                        editor.putString("domicile", document.get("domicile").toString());
                        editor.putString("city",document.get("city").toString());
                        editor.putString("constituency", document.get("constituency").toString());
                        editor.putString("voterID", document.get("voterID").toString());
                        editor.putString("aadharID", document.get("aadharID").toString());
                        editor.putString("voteFlag",voteFlag);
                        editor.apply();

                        nameTextView.setTypeface(typeface);
                        domicileTextView.setTypeface(typeface);
                        constituencyTextView.setTypeface(typeface);
                        aadharTextView.setTypeface(typeface);
                        voterIDTextView.setTypeface(typeface);

                        if(dialog.isShowing()&& dialog!=null)
                            dialog.dismiss();
                        dataShow.setVisibility(View.VISIBLE);
                    } else {
                        Log.d("MainActivity", "No such document");
                    }
                } else {
                    Log.d("MainActivity", "get failed with ", task.getException());
                }
            }
        });
    }

    private void showAlert(String message){
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Alert")
                .setIcon(R.drawable.ic_error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).show();
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

    @Override
    public void onItemSelected(AdapterView<?> argz, View arg1, int position, long id) {
        String item=argz.getItemAtPosition(position).toString();
        switch(argz.getId()){
            case R.id.state_spinner:
                domicile=item;
                Log.d("MainActivity", "onItemSelected: "+domicile);
                db.collection("States").document(domicile).collection("Cities").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> list = new ArrayList<>();
                            try {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    list.add(document.getId());
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            if(!list.isEmpty()) {
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, list);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                citySpinner.setAdapter(adapter);
                                citySpinner.setEnabled(true);
                                Log.d("MainActivity", "onItemSelected: citySpinner enable status" + citySpinner.isEnabled());
                            }
                        }
                    }
                });
                break;

            case R.id.city_spinner:
                city=item;
                DocumentReference docRef = db.collection("States").document(domicile).collection("Cities").document(city);
                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> list = new ArrayList<>();
                            try {
                                Map<String, Object> map = task.getResult().getData();
                                for (Map.Entry<String, Object> entry : map.entrySet()) {
                                    list.add(entry.getKey());
                                    Log.d("MainActivity", entry.getKey());
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            if(!list.isEmpty()) {
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, list);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                conSpinner.setAdapter(adapter);
                                conSpinner.setEnabled(true);
                            }
                        }
                    }
                });
                break;

            case R.id.constituency_spinner:
                constituency=item;
                break;

        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> argz) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.test_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        switch (id){
            case R.id.shareLink:
                generateContentLink();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void generateContentLink() {
        Task<ShortDynamicLink> link=FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLongLink(constructURL())
                .buildShortDynamicLink()
                .addOnCompleteListener(new OnCompleteListener<ShortDynamicLink>() {
                    @Override
                    public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                        if(task.isSuccessful()){
                            Uri shortLink=task.getResult().getShortLink();
                            Intent intent=new Intent();
                            intent.setAction(Intent.ACTION_SEND);
                            String msg=" Here's your invitation link: "+shortLink.toString();
                            intent.putExtra(Intent.EXTRA_TEXT, msg);
                            intent.setType("text/plain");
                            startActivity(Intent.createChooser(intent, "Share Link"));
                        }
                    }
                });
    }

    private Uri constructURL() {
        Uri baseURI=Uri.parse("https://youtube.com/");
        Uri APP_URI = baseURI.buildUpon().appendQueryParameter("requestID", "200").
                appendQueryParameter("extra1", "value").
                appendQueryParameter("extra2", "value").build();
        String encodedUri = null;
        try {
            encodedUri = URLEncoder.encode(APP_URI.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d("MainActivity", "constructURL: "+e.getLocalizedMessage());
        }
        Uri deepLink = Uri.parse("https://lostvotes.page.link/?link="+encodedUri+"&apn="+"in.mahe.lostvotes");
        Log.d("MainActivity", "constructURL: "+deepLink);
        return deepLink;
    }

    private void initViews(){
        voteFlag="0";
        dataShow=findViewById(R.id.data_show_layout);
        dataCapture=findViewById(R.id.data_capture_layout);
        nameEditText=findViewById(R.id.name_edit_text);
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
        dSpinner=findViewById(R.id.state_spinner);
        conSpinner=findViewById(R.id.constituency_spinner);
        citySpinner=findViewById(R.id.city_spinner);
        conSpinner.setEnabled(false);
        citySpinner.setEnabled(false);
        typeface=Typeface.createFromAsset(getAssets(), "fonts/Gilroy-Light.otf");
    }
}
