package com.example.myproject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class DeleteChatDialog extends DialogFragment {
    private RadioButton deleteBox;
    private RadioButton blockBox;
    private DatabaseReference reference;
    private String receiverUuid;
    private ValueEventListener deleteMessageListener;
    private ValueEventListener blockChatListener;
    private String firstKey;
    private String secondKey;

    public DeleteChatDialog(String receiverUuid){
        this.receiverUuid = receiverUuid;
    }
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.delete_chat_dialog,null);
        reference = FirebaseDatabase.getInstance().getReference("chats").child(generateKey());
        deleteBox = view.findViewById(R.id.check_delete_box);
        blockBox = view.findViewById(R.id.check_blocklist_box);

        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        return builder
                .setTitle(getResources().getString(R.string.dialog_title_delete))
                .setView(view)
                .setPositiveButton(R.string.ok_pos_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       deleteChat(deleteBox.isChecked());
                       blockChat(blockBox.isChecked());
                    }
                }).create();
    }

    private void deleteChat(boolean check){
        if (check){
            deleteMessageListener =reference.child("message").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot snapshot1:snapshot.getChildren()){
                        if (User.getCurrentUser().getUuid().equals(firstKey)) {
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("firstDelete", "delete");
                            snapshot1.getRef().updateChildren(hashMap);
                        }
                        else {
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("secondDelete", "delete");
                            snapshot1.getRef().updateChildren(hashMap);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void blockChat(boolean block){
        if (block) {
            blockChatListener = reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot snapshot1:snapshot.getChildren()){
                        if (snapshot1.getKey().equals("firstBlock") && User.getCurrentUser().getUuid().equals(firstKey)){
                            //HashMap<String,Object> map =new HashMap<>();
                            //map.put("firstBlock","block");
                            snapshot1.getRef().setValue("block");
                            //snapshot1.getRef().updateChildren(map);
                        }
                        else if (snapshot1.getKey().equals("secondBlock") && User.getCurrentUser().getUuid().equals(secondKey)){
                            //HashMap<String,Object> map =new HashMap<>();
                            //map.put("firstBlock","block");
                            snapshot1.getRef().setValue("block");
                            //snapshot1.getRef().updateChildren(map);
                        }
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (deleteMessageListener !=null) reference.removeEventListener(deleteMessageListener);
    }

    private String generateKey(){
        ArrayList<String> templist=new ArrayList<>();
        templist.add(User.getCurrentUser().getUuid());
        templist.add(receiverUuid);
        Collections.sort(templist);
        firstKey =templist.get(0);
        secondKey =templist.get(1);
        return templist.get(0)+templist.get(1);
    }
}
