package com.android.notes;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CreateNoteActivity extends AppCompatActivity {
    Button btnCreate;
    private EditText etTitle, etContent;

    private FirebaseAuth fAuth;
    private DatabaseReference fNotesDatabase;

    private String noteID;

    private boolean isExist;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.new_note_menu, menu);
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_note_activity);

        try {
            noteID = getIntent().getStringExtra("noteId");

            //Toast.makeText(this, noteID, Toast.LENGTH_SHORT).show();

            assert noteID != null;
            isExist = !noteID.trim().equals("");

        } catch (Exception e) {
            e.printStackTrace();
        }


        btnCreate = findViewById(R.id.new_note_btn);
        etTitle = findViewById(R.id.new_note_title);
        etContent = findViewById(R.id.new_note_content);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        fAuth = FirebaseAuth.getInstance();
        fNotesDatabase = FirebaseDatabase.getInstance().getReference().child("Notes").child(Objects.requireNonNull(fAuth.getCurrentUser()).getUid());

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String title = etTitle.getText().toString().trim();
                String content = etContent.getText().toString().trim();

                if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(content)) {
                    createNote(title, content);
                } else {
                    Snackbar.make(view, "Fill empty fields", Snackbar.LENGTH_SHORT).show();
                }

            }
        });

        putData();
    }

    private void putData() {

        if (isExist) {
            fNotesDatabase.child(noteID).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild("title") && dataSnapshot.hasChild("content")) {
                        String title = Objects.requireNonNull(dataSnapshot.child("title").getValue()).toString();
                        String content = Objects.requireNonNull(dataSnapshot.child("content").getValue()).toString();

                        etTitle.setText(title);
                        etContent.setText(content);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    private void createNote(String title, String content) {

        if (fAuth.getCurrentUser() != null) {

            if (isExist) {
                // UPDATE A NOTE
                Map updateMap = new HashMap();
                updateMap.put("title", etTitle.getText().toString().trim());
                updateMap.put("content", etContent.getText().toString().trim());
                updateMap.put("timestamp", ServerValue.TIMESTAMP);

                fNotesDatabase.child(noteID).updateChildren(updateMap);

                Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // CREATE A NEW NOTE
                final DatabaseReference newNoteRef = fNotesDatabase.push();

                final Map noteMap = new HashMap();
                noteMap.put("title", title);
                noteMap.put("content", content);
                noteMap.put("timestamp", ServerValue.TIMESTAMP);

                Thread mainThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        newNoteRef.setValue(noteMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(CreateNoteActivity.this, "Note added to database", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(CreateNoteActivity.this, "ERROR: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                            }
                        });
                    }
                });
                mainThread.start();
            }


        } else {
            Toast.makeText(this, "USERS IS NOT SIGNED IN", Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.new_note_delete_btn:
                if (isExist) {
                    deleteNote();
                } else {
                    Toast.makeText(this, "Nothing to delete", Toast.LENGTH_SHORT).show();
                }
                break;
        }

        return true;
    }

    private void deleteNote() {

        fNotesDatabase.child(noteID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(CreateNoteActivity.this, "Note Deleted", Toast.LENGTH_SHORT).show();
                    noteID = "no";
                    finish();
                } else {
                    Log.e("NewNoteActivity", Objects.requireNonNull(task.getException()).toString());
                    Toast.makeText(CreateNoteActivity.this, "ERROR: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

}
