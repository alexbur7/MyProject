package com.example.myproject;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

public class PhotoViewPagerItemFragment extends Fragment {
    public static final String KEY_URL="key_url";
    private ImageView imageView;
    private String url;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.photo_view_pager_item,null);
        imageView = v.findViewById(R.id.photo_view_pager_item);
        //imageView.setOnTouchListener(new ZoomInZoomOut());
        url = getArguments().getString(KEY_URL);
        Glide.with(getActivity()).load(url).into(imageView);
        return v;
    }

    public static PhotoViewPagerItemFragment newInstance(String url){
        PhotoViewPagerItemFragment fragment = new PhotoViewPagerItemFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_URL,url);
        fragment.setArguments(bundle);
        return fragment;
    }
}
