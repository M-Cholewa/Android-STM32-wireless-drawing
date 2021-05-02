package com.example.i_fireworks.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.i_fireworks.MainActivity;
import com.example.i_fireworks.R;
import com.example.i_fireworks.utils.Util;
import com.example.i_fireworks.views.OledGrid;

import java.lang.invoke.ConstantCallSite;
import java.util.ArrayList;
import java.util.Arrays;

public class DrawFragment extends Fragment {
    private static final String TAG = "DrawFragment";

    OledGrid oledGrid;
    ToggleButton showGridTb;
    ToggleButton zoomBtn, eraseBtn, brushBtn;
    ConstraintLayout drawContainer;
    ArrayList<ToggleButton> toggleButtons = new ArrayList<>();
    MainActivity mainActivity;


    public DrawFragment(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_draw, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    private void initViews(View v) {
        oledGrid = v.findViewById(R.id.oledGrid);
        drawContainer = v.findViewById(R.id.drawContainer);
        final ViewTreeObserver vto = drawContainer.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @SuppressWarnings("SuspiciousNameCombination")
                @Override
                public void onGlobalLayout() {
                    int viewHeight = drawContainer.getHeight();
                    int viewWidth = drawContainer.getWidth();
                    // handle viewWidth here...
                    Log.d(TAG, "onGlobalLayout: " + viewHeight);
                    if (viewHeight > 0) {
                        drawContainer.setRotation(90);
                        drawContainer.getLayoutParams().width = viewHeight;
                        drawContainer.getLayoutParams().height = viewWidth;
                        drawContainer.requestLayout();
                        oledGrid.getLayoutParams().width = viewHeight;
                        drawContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }
        showGridTb = v.findViewById(R.id.showGridTb);
        zoomBtn = v.findViewById(R.id.zoomBtn);
        eraseBtn = v.findViewById(R.id.eraseBtn);
        brushBtn = v.findViewById(R.id.brushBtn);
        toggleButtons.add(zoomBtn);
        toggleButtons.add(eraseBtn);
        toggleButtons.add(brushBtn);
        initListeners();
    }

    private void toggleButton(ToggleButton toggleButton) {
        toggleButton.setChecked(true);
        for (ToggleButton btn : toggleButtons) {
            if (btn.getId() != toggleButton.getId())
                btn.setChecked(false);
        }
    }

    private void initListeners() {
        byte[] xArr= new byte[1];
        byte[] yArr= new byte[1];
        byte[] sArr= new byte[1];
        byte[] cArr= new byte[1];

        oledGrid.setOnPixelChangeListener(new OledGrid.onPixelChangeListener() {
            @Override
            public void pixelDrawn(int x, int y, int color) {
                xArr[0] = Util.intToByteArray(x)[3];
                yArr[0] = Util.intToByteArray(y)[3];
                sArr[0] = Util.intToByteArray(x+y)[3];
                mainActivity.sendMsg(xArr);
                mainActivity.sendMsg(Util.stringToByteArray("x"));

                mainActivity.sendMsg(yArr);
                mainActivity.sendMsg(Util.stringToByteArray("y"));

                mainActivity.sendMsg(sArr);
                mainActivity.sendMsg(Util.stringToByteArray("s"));
            }

            @Override
            public void brushChanged(int brush) {
                cArr[0] = Util.intToByteArray(brush)[3];
                mainActivity.sendMsg(cArr);
                mainActivity.sendMsg(cArr);
            }

            @Override
            public void gridCleared() {
                cArr[0] = Util.intToByteArray(10)[3];
                mainActivity.sendMsg(cArr);
                mainActivity.sendMsg(cArr);
            }
        });

        showGridTb.setOnCheckedChangeListener((buttonView, isChecked) -> oledGrid.setShouldDrawGrid(isChecked));
        eraseBtn.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                oledGrid.setBrush(OledGrid.BrushConstants.BRUSH_ERASER);
                toggleButton(eraseBtn);
            }
        });
        eraseBtn.setOnLongClickListener(v -> {
            oledGrid.clearDisplay();
            return true;
        });
        brushBtn.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                oledGrid.setBrush(OledGrid.BrushConstants.BRUSH_DRAW);
                toggleButton(brushBtn);
            }
        });
        zoomBtn.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                oledGrid.setBrush(OledGrid.BrushConstants.BRUSH_ZOOM);
                toggleButton(zoomBtn);
            }
        });
    }

    public void zoomIn(View view) {
//        float rotation = oledGrid.getRotation();
//        if ((rotation == 0)) {
////            oledGrid.animate().rotation(90).setDuration(15000).start();
//            oledGrid.setRotation(90);
//        } else {
//            oledGrid.setRotation(0);
////            oledGrid.animate().rotation(0).setDuration(15000).start();
//        }
    }
}
