package com.example.myproject;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import java.util.Date;
import java.util.HashMap;

import static android.app.Activity.RESULT_OK;

public class MyAccountFragment extends AccountFragment {

    private static  final  int IMAGE_REQUEST=1;
    private Uri imageUri;
    private StorageTask uploadTask;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater,container,savedInstanceState);
    }

    @Override
    void setToolbar() {
        toolbar.inflateMenu(R.menu.account_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return clickToolbarItems(item);
            }
        });
    }

    @Override
    void setEditButton() {
        editButton.setText(getResources().getString(R.string.edit_account));
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditDialog editDialog = new EditDialog();
                editDialog.show(getFragmentManager(),null);
            }
        });
    }


    @Override
    void setPhotoImageView() {
        photoImageView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                openImage();
            }
        });
    }

    @Override
    boolean clickToolbarItems(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout: {
                User.getCurrentUser().setStatus("offline");
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getActivity(), LogActivity.class));
                getActivity().finish();
            }
            break;
            case R.id.delete_account:{
                reference.removeEventListener(imageEventListener);
                FirebaseAuth.getInstance().getCurrentUser().delete();
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(getActivity(), "Completed", Toast.LENGTH_SHORT);
                FirebaseDatabase.getInstance().getReference("users").child(User.getCurrentUser().getUuid()).removeValue();
                User.setCurrentUser(null,null);
                startActivity(new Intent(getActivity(), LogActivity.class));
                getActivity().finish();
            }
            break;
            case R.id.blocklist:{
                Intent intent=new Intent(getActivity(),BlockListActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.panel_admin:{
                Toast.makeText(getContext(),"YOU admin",Toast.LENGTH_SHORT).show();
                Intent intent=new Intent(getActivity(),AdminActivity.class);
                startActivity(intent);
            }
            break;
        }
        return true;
    }

    @Override
    void setUser() {
        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setMessage(getResources().getString(R.string.uploading));
        pd.show();
        String uuid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        imageEventListener=reference.child(uuid).addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                User.setCurrentUser(user, uuid);
                if (user.getAdmin().equals("true")){
                    toolbar.getMenu().getItem(3).setVisible(true);
                }
                if(user.getPhoto_url().equals("default")){
                    photoImageView.setImageResource(R.drawable.unnamed);
                }
                else {
                    if (isAdded()) Glide.with(getContext()).load(user.getPhoto_url()).into(photoImageView);
                }
                setAllTextView(user);
                setEditButton();
                pd.dismiss();
                //ref.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    @Override
    void deleteImage(User user) {
        if (!user.getPhoto_url().equals("default")) {
            StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(user.getPhoto_url());
            photoRef.delete();
        }
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        status("online");
    }


    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,IMAGE_REQUEST);
    }



    private String getFileExtension(Uri uri){
        ContentResolver contentResolver = getContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage(){
        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setMessage(getResources().getString(R.string.uploading));
        pd.show();

        if (imageUri != null){
            final StorageReference fileReference= storageReference.child(System.currentTimeMillis()+
                    "."+getFileExtension(imageUri));
            uploadTask = fileReference.putFile(imageUri);
            uploadTask.continueWithTask((Continuation<UploadTask.TaskSnapshot, Task<Uri>>) task -> {
                if (!task.isSuccessful()){
                    throw  task.getException();
                }
                return fileReference.getDownloadUrl();
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()){
                        Uri downloadUri = task.getResult();
                        String mUri = downloadUri.toString();

                        reference = FirebaseDatabase.getInstance().getReference("users").child(User.getCurrentUser().getUuid());
                        HashMap<String,Object> map = new HashMap<>();
                        map.put("photo_url",mUri);
                        deleteImage(User.getCurrentUser());
                        reference.updateChildren(map);
                        User.getCurrentUser().setPhoto_url(mUri);
                        pd.dismiss();
                    }
                    else {
                        Toast.makeText(getContext(),R.string.failed_update_photo,Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        }
        else {
            Toast.makeText(getContext(),R.string.no_image_selected,Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
        && data!=null && data.getData() !=null
        ){
            imageUri = data.getData();
            uploadImage();
        }
    }


    private void status(String status){
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("status", status);
        if (status.equals("offline")){
            hashMap.put("online_time",(new Date()).getTime());
        }
        FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getUid()).updateChildren(hashMap);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        reference.child(User.getCurrentUser().getUuid()).removeEventListener(imageEventListener);
    }
}