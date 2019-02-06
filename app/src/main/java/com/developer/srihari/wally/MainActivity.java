package com.developer.srihari.wally;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public DatabaseReference dbref;
    private static final int REQUEST_CODE = 1;
    public Button upload;
    public RecyclerView downlist;
    private StorageReference mstor;
    private DatabaseReference dstor;
    public ImageView imgv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        upload=findViewById(R.id.upload);
        imgv=(ImageView)findViewById(R.id.image);

        downlist=findViewById(R.id.uplst);
        GridLayoutManager layman=new GridLayoutManager(this,2,GridLayoutManager.VERTICAL,false);
        downlist.setHasFixedSize(true);
        downlist.setLayoutManager(layman);

        mstor= FirebaseStorage.getInstance().getReference();
        dstor= FirebaseDatabase.getInstance().getReference().child("Images");

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),REQUEST_CODE);

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<ImageDetails,imgviewholder> firebaseRecyclerAdapter=new FirebaseRecyclerAdapter<ImageDetails, imgviewholder>(
                ImageDetails.class,
                R.layout.row,
                imgviewholder.class,
                dstor
        ) {
            @Override
            protected void populateViewHolder(imgviewholder viewHolder, ImageDetails model, int position) {
                viewHolder.setImage(getApplication(),model.getUrl());
            }
        };
        downlist.setAdapter(firebaseRecyclerAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(REQUEST_CODE == requestCode && resultCode == RESULT_OK){
            if(data.getClipData() != null){
                int totalselected=data.getClipData().getItemCount();
                for(int i=0;i<totalselected;i++){
                    Uri fileuri=data.getClipData().getItemAt(i).getUri();
                    StorageReference filestoupload=mstor.child("Images").child(fileuri.getLastPathSegment());
                    filestoupload.putFile(fileuri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            notificationcall();
                            Uri downloadurl=taskSnapshot.getDownloadUrl();
                            String struri=downloadurl.toString();
                            addImageToDatabase(struri);
                        }
                    });
                }
            }
        }
    }

    public void notificationcall(){
        NotificationCompat.Builder notibuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_stat_name))
                .setBadgeIconType(R.drawable.ic_stat_name)
                .setContentTitle("New Notification")
                .setContentText("Upload Successfull");
        NotificationManager notiman=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notiman.notify(1,notibuilder.build());
    }

    public static class imgviewholder extends RecyclerView.ViewHolder{
        View mview;
        public LinearLayout llayout;

        public imgviewholder(View itemview){
            super(itemview);
            mview=itemview;
            llayout=itemview.findViewById(R.id.linearlayout);
        }

        public void setImage(Context ctx,String url){
            ImageView post_img=mview.findViewById(R.id.image);
            Picasso.with(ctx).load(url).into(post_img);
        }
    }

    private void addImageToDatabase(String struri){
        ImageDetails imgdetials= new ImageDetails(struri);
        dstor.push().setValue(imgdetials);
    }

}
