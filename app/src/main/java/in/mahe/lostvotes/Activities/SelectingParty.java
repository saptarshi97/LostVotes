package in.mahe.lostvotes.Activities;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import in.mahe.lostvotes.R;

public class SelectingParty extends AppCompatActivity {
    FirebaseFirestore db;
    public static String PREF_NAME="shared values";
    private String name, domicile, city, constituency, voterID, aadhar, currentUserID;
    private ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selecting_party);
        db=FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        lv = findViewById(R.id.party_list);
        List<String> l=new ArrayList<>();

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
                        Log.d("MainActivity", "Exception thrown for list");
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
