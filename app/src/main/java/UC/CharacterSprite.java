package UC;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.mobilefitness.GameObject;


public class CharacterSprite implements GameObject {
    private Bitmap image;
    private Rect rectangle;
    private int color;
    private int x, y;
    private int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    private int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

    public CharacterSprite(Bitmap bmp) {
        image = bmp;
        x = 180;
        y = 800;
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(image, x, y, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void update() {
        }
    }
