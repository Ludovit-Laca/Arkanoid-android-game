package com.example.android.arkanoid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;

public class Game extends View implements SensorEventListener, View.OnTouchListener {

    private Bitmap pozadie;
    private Bitmap redBall;
    private Bitmap roztiahnuty;
    private Bitmap paddle_p;

    private Display display;
    private Point size;
    private Paint paint;

    private Ball lopticka;
    private ArrayList<Brick> zoznam;
    private Paddle paddle;

    private RectF r;

    private SensorManager sManager;
    private Sensor accelerometer;

    private int lifes;
    private int score;
    private int level;
    private boolean start;
    private boolean gameOver;
    private Context context;

    public Game(Context context, int lifes, int score) {
        super(context);
        paint = new Paint();

        // nastavi context, zivoty, skore a level
        this.context = context;
        this.lifes = lifes;
        this.score = score;
        level = 0;

        // start a gameOver na zistenie ci hra stoji a ci je hráč neprehral
        start = false;
        gameOver = false;

        // vytvorí akcelerometer a SensorManager
        sManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        nacitajPozadie(context);

        // vytvori bitmap pre lopticku a pádlo
        redBall = BitmapFactory.decodeResource(getResources(), R.drawable.redball);
        paddle_p = BitmapFactory.decodeResource(getResources(), R.drawable.paddle);

        // vytvorí novú lopticku, pádlo, a zoznam tehliciek
        lopticka = new Ball(size.x / 2, size.y - 480);
        paddle = new Paddle(size.x / 2, size.y - 400);
        zoznam = new ArrayList<Brick>();

        vygenerujBricks(context);
        this.setOnTouchListener(this);

    }

    // naplni zoznam tehlickami
    private void vygenerujBricks(Context context) {
        for (int i = 3; i < 7; i++) {
            for (int j = 1; j < 6; j++) {
                zoznam.add(new Brick(context, j * 150, i * 100));
            }
        }
    }

    // nastavi pozadie
    private void nacitajPozadie(Context context) {
        pozadie = Bitmap.createBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.pozadie_score));
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        display = wm.getDefaultDisplay();
        size = new Point();
        display.getSize(size);
    }

    protected void onDraw(Canvas canvas) {
        // vytvori pozadie iba raz
        if (roztiahnuty == null) {
            roztiahnuty = Bitmap.createScaledBitmap(pozadie, size.x, size.y, false);
        }
        canvas.drawBitmap(roztiahnuty, 0, 0, paint);

        // vykresli lopticku
        paint.setColor(Color.RED);
        canvas.drawBitmap(redBall, lopticka.getX(), lopticka.getY(), paint);

        // vykresli padlo
        paint.setColor(Color.WHITE);
        r = new RectF(paddle.getX(), paddle.getY(), paddle.getX() + 200, paddle.getY() + 40);
        canvas.drawBitmap(paddle_p, null, r, paint);

        // vykresli tehlicky
        paint.setColor(Color.GREEN);
        for (int i = 0; i < zoznam.size(); i++) {
            Brick b = zoznam.get(i);
            r = new RectF(b.getX(), b.getY(), b.getX() + 100, b.getY() + 80);
            canvas.drawBitmap(b.getBrick(), null, r, paint);
        }

        // vykresli text
        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        canvas.drawText("" + lifes, 400, 100, paint);
        canvas.drawText("" + score, 700, 100, paint);

        // v pripade prehry vykresli "Game over!"
        if (gameOver) {
            paint.setColor(Color.RED);
            paint.setTextSize(100);
            canvas.drawText("Game over!", size.x / 4, size.y / 2, paint);
        }
    }

    // skontroluje či sa lopticka nedotkla okraju
    private void skontrolujOkraje() {
        if (lopticka.getX() + lopticka.getxRychlost() >= size.x - 60) {
            lopticka.zmenSmer("prava");
        } else if (lopticka.getX() + lopticka.getxRychlost() <= 0) {
            lopticka.zmenSmer("lava");
        } else if (lopticka.getY() + lopticka.getyRychlost() <= 150) {
            lopticka.zmenSmer("hore");
        } else if (lopticka.getY() + lopticka.getyRychlost() >= size.y - 200) {
            skontrolujZivoty();
        }
    }

    // skontroluje stav hry. či ma životy alebo či hra konči
    private void skontrolujZivoty() {
        if (lifes == 1) {
            gameOver = true;
            start = false;
            invalidate();
        } else {
            lifes--;
            lopticka.setX(size.x / 2);
            lopticka.setY(size.y - 480);
            lopticka.vytvorRychlost();
            lopticka.zvysRychlost(level);
            start = false;
        }
    }

    // kazdy krok kontroluje ci nedoslo ku kolizii, k prehre alebo k vyhre atd
    public void update() {
        if (start) {
            vyhra();
            skontrolujOkraje();
            lopticka.NarazPaddle(paddle.getX(), paddle.getY());
            for (int i = 0; i < zoznam.size(); i++) {
                Brick b = zoznam.get(i);
                if (lopticka.NarazBrick(b.getX(), b.getY())) {
                    zoznam.remove(i);
                    score = score + 80;
                }
            }
            lopticka.pohni();
        }
    }

    public void zastavSnimanie() {
        sManager.unregisterListener(this);
    }

    public void spustiSnimanie() {
        sManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    // zmena akcelerometera
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            paddle.setX(paddle.getX() - event.values[0] - event.values[0]);

            if (paddle.getX() + event.values[0] > size.x - 240) {
                paddle.setX(size.x - 240);
            } else if (paddle.getX() - event.values[0] <= 20) {
                paddle.setX(20);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // sluzi na pozastavenie hry v pripade novej hry
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (gameOver == true && start == false) {
            score = 0;
            lifes = 3;
            resetLevel();
            gameOver = false;

        } else {
            start = true;
        }
        return false;
    }

    // nastavi hru na zaciatok
    private void resetLevel() {
        lopticka.setX(size.x / 2);
        lopticka.setY(size.y - 480);
        lopticka.vytvorRychlost();
        zoznam = new ArrayList<Brick>();
        vygenerujBricks(context);
    }

    // zisti ci hrac vyhral alebo nie
    private void vyhra() {
        if (zoznam.isEmpty()) {
            ++level;
            resetLevel();
            lopticka.zvysRychlost(level);
            start = false;
        }
    }
}
