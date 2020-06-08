package com.example.mobilefitness;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import androidx.annotation.RequiresApi;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private MainThread thread;
    private Rect r = new Rect();
    public int face_player_position;
    private RectPlayer player;
    public Point playerPoint;
    private ObstacleManager obstacleManager;
    private boolean gameOver = false;
    private long gameOverTime;

    static final int PLAYER_POSITION_LEFT = 0;
    static final int PLAYER_POSITION_CENTER = 1;
    static final int PLAYER_POSITION_RIGHT = 2;


    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        Constants.CURRENT_CONTEXTS = context;
        thread = new MainThread(getHolder(), this);
        playerPoint = new Point(Constants.SCREEN_WIDTH/2, 3*Constants.SCREEN_HEIGHT/4);
        player = new RectPlayer(new Rect(100, 100, 200, 200), Color.rgb(255,0,0));
        player.update(playerPoint);
        obstacleManager = new ObstacleManager(400, 850, 75, Color.BLACK);

        setFocusable(true);
    }

    public void reset () {
        playerPoint = new Point(Constants.SCREEN_WIDTH/2, 3*Constants.SCREEN_HEIGHT/4);
        player = new RectPlayer(new Rect(100, 100, 200, 200), Color.rgb(255,0,0));
        obstacleManager = new ObstacleManager(400, 850, 75, Color.BLACK);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //barSprite = new BarSprite(BitmapFactory.decodeResource(getResources(), R.drawable.barsmall));
        //characterSprite = new CharacterSprite(BitmapFactory.decodeResource(getResources(), R.drawable.circle));
        thread = new MainThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry) {
            try {
                thread.setRunning(false);
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retry = false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void update () {
        if (!gameOver) {
            player.update(playerPoint);
            obstacleManager.update();
            if(obstacleManager.playerCollide(player)) {
                gameOver = true;
                gameOverTime = System.currentTimeMillis();
            }
            //barSprite.update();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent (MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(gameOver && System.currentTimeMillis() - gameOverTime >= 2000) {
                    reset();
                    gameOver = false;
                }
            case MotionEvent.ACTION_MOVE:
                playerPoint.set((int)event.getX(), (int)event.getY());
        }

        return true;
        //return super.onTouchEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawColor(Color.WHITE);
        //barSprite.draw(canvas);
        player.draw(canvas);
        obstacleManager.draw(canvas);
        if(gameOver) {
            Paint paint = new Paint();
            paint.setTextSize(100);
            paint.setColor(Color.MAGENTA);
            drawCenterText(canvas, paint, "Game Over");
        }
        //characterSprite.draw(canvas);
    }

    public void drawCenterText(Canvas canvas, Paint paint, String text) {
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.getClipBounds(r);
        int cHeight = r.height();
        int cWidth = r.width();
        paint.getTextBounds(text, 0, text.length(), r);
        float x = cWidth / 2f - r.width() / 2f - r.left;
        float y = cHeight / 2f + r.height() /2f - r.bottom;
        canvas.drawText(text, x, y, paint);
    }
}
