package com.example.i_fireworks.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.i_fireworks.MainActivity;
import com.example.i_fireworks.R;

public class BoardFragment  extends Fragment {

    EditText lcdMsgEt;
    Button sendLcdMsgBtn, connectBtn;
    MainActivity mainActivity;

    public BoardFragment(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_board, container,false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    private void initViews(View v){
        lcdMsgEt = v.findViewById(R.id.lcdMsgEt);
        sendLcdMsgBtn = v.findViewById(R.id.sendLcdMsgBtn);
        connectBtn = v.findViewById(R.id.connectBtn);

        initListeners();
    }

    private void initListeners(){
        connectBtn.setOnClickListener(v -> {
            mainActivity.startSocket();
        });

        sendLcdMsgBtn.setOnClickListener(v -> {
            String str0 = String.valueOf(lcdMsgEt.getText());
            mainActivity.sendMsg(str0.getBytes());
        });
    }
}
