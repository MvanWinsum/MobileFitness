package com.example.mobilefitness;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.concurrent.ThreadLocalRandom;

public class BarSprite {
    private Bitmap image;
    private int x, y;
    private int min = 1;
    private int max = 3;
    private int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    private int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

    public BarSprite(Bitmap bmp) {
        image = bmp;
        x = 100;
        y = 0;
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(image, x, y, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void update() {
        y=y+50;
        if (y>screenHeight) {
            int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
            if (randomNum == 1) { x = screenWidth/4; } //middle
            else if (randomNum==2) { x = 0; } // left
            else { x = screenWidth/2; } //right
            y=0;
            System.out.println(randomNum);
            System.out.println(x);
        }
    }
}
