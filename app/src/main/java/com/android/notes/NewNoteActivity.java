package com.android.notes;

/*
 * Created by Ankita on 21/10/2020.
 */

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class NewNoteActivity extends AppCompatActivity {

    Context context;
    private FirebaseAuth fAuth;
    private RecyclerView mNotesList;
    private DatabaseReference fNotesDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_note_activity);
        context = this;
        if (!NetworkAccess.isOnline(context)) {
            //When network is unAvailable

            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_box);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            Objects.requireNonNull(dialog.getWindow()).setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;

            Button btnRetry = dialog.findViewById(R.id.retry);
            btnRetry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    recreate();
                }
            });
            dialog.show();


        }


        mNotesList = findViewById(R.id.notes_list);
        ImageView newNote = findViewById(R.id.new_note_btn);
        newNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(NewNoteActivity.this, CreateNoteActivity.class);
                startActivity(newIntent);

            }
        });

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);

        mNotesList.setHasFixedSize(true);
        mNotesList.setLayoutManager(gridLayoutManager);
        //gridLayoutManager.setReverseLayout(true);
        //gridLayoutManager.setStackFromEnd(true);
        mNotesList.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(), true));

        fAuth = FirebaseAuth.getInstance();
        if (fAuth.getCurrentUser() != null) {
            fNotesDatabase = FirebaseDatabase.getInstance().getReference().child("Notes").child(fAuth.getCurrentUser().getUid());
            updateUI();
            loadData();
            CardView img = findViewById(R.id.loadingImg);
            img.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private void loadData() {
        CardView img = findViewById(R.id.loadingImg);
        img.setVisibility(View.GONE);
        Query query = fNotesDatabase.orderByValue();
        FirebaseRecyclerAdapter<NoteModel, NoteViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<NoteModel, NoteViewHolder>(

                NoteModel.class,
                R.layout.single_note_layout,
                NoteViewHolder.class,
                query


        ) {
            @Override
            protected void populateViewHolder(final NoteViewHolder viewHolder, NoteModel model, int position) {
                final String noteId = getRef(position).getKey();
                CardView img = findViewById(R.id.loadingImg);
                img.setVisibility(View.GONE);
                assert noteId != null;
                fNotesDatabase.child(noteId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild("title") && dataSnapshot.hasChild("timestamp")) {
                            String title = Objects.requireNonNull(dataSnapshot.child("title").getValue()).toString();
                            String timestamp = Objects.requireNonNull(dataSnapshot.child("timestamp").getValue()).toString();

                            viewHolder.setNoteTitle(title);
                            //viewHolder.setNoteTime(timestamp);

                            viewHolder.setNoteTime(GetTimeAgo.getTimeAgo(Long.parseLong(timestamp), getApplicationContext()));

                            viewHolder.noteCard.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(NewNoteActivity.this, CreateNoteActivity.class);
                                    intent.putExtra("noteId", noteId);
                                    startActivity(intent);
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });


            }
        };
        mNotesList.setAdapter(firebaseRecyclerAdapter);
    }

    private void updateUI() {

        if (fAuth.getCurrentUser() != null) {
            Log.i("NewNoteActivity", "fAuth != null");
        } else {
            Intent startIntent = new Intent(NewNoteActivity.this, MainActivity.class);
            startActivity(startIntent);
            finish();
            Log.i("NewNoteActivity", "fAuth == null");
        }

    }


    /**
     * Converting dp to pixel
     */
    private int dpToPx() {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics()));
    }

}