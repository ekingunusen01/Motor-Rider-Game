package com.example.motobiker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    Button gameStartButton, noviceLevelButton, intermediateLevelButton, advancedLevelButton;
    ImageView motorRider, roadBackground1, roadBackground2, logoView, obstacle1, obstacle2;
    float screenWidth, screenHeight, roadSpeed, roadLeftPavement, roadRightPavement;
    float motorRiderX = 0f;
    SensorManager sensorManager;
    Sensor accelerometer;
    Handler roadHandler = new Handler();
    Handler carHandler = new Handler();
    Runnable roadRunnable;
    Runnable carRunnable;
    boolean isGameActive = false;
    float road1Y = 0f;
    float road2Y;
    int currentLevel, carSpawnDelay;
    ArrayList<ImageView> cars = new ArrayList<>();
    Random random = new Random();
    float[] lanePositions;
    int[] carImages = {
            R.drawable.enemycarred,
            R.drawable.enemycarwhite,
            R.drawable.policecar
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        gameStartButton         = findViewById(R.id.startGameButton);
        noviceLevelButton       = findViewById(R.id.noviceLevelButton);
        intermediateLevelButton = findViewById(R.id.intermediateLevelButton);
        advancedLevelButton     = findViewById(R.id.advancedLevelButton);

        motorRider      = findViewById(R.id.motorRider);
        roadBackground1 = findViewById(R.id.backgroundRoad1);
        roadBackground2 = findViewById(R.id.backgroundRoad2);
        logoView        = findViewById(R.id.logo);
        obstacle1       = findViewById(R.id.obstacle1);
        obstacle2       = findViewById(R.id.obstacle2);

        // after clicking the start button,
        // start button goes invisible, level buttons set to visible
        gameStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameStartButton.setVisibility(View.GONE);
                noviceLevelButton.setVisibility(View.VISIBLE);
                intermediateLevelButton.setVisibility(View.VISIBLE);
                advancedLevelButton.setVisibility(View.VISIBLE);
            }
        });
        // level buttons have listeners with lambda functions that forwards to the setLevel function
        noviceLevelButton.setOnClickListener(v -> setLevel(1));
        intermediateLevelButton.setOnClickListener(v -> setLevel(2));
        advancedLevelButton.setOnClickListener(v -> setLevel(3));

        // initializing the sensor manager to manage motor movement
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);

        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        road2Y = -(screenHeight);

        // defining the "left and right pavement" locations for the crash check of the motor
        roadLeftPavement = screenWidth * 0.05f;
        roadRightPavement = screenWidth * 0.9f;

        motorRiderX = screenWidth * 0.5f - motorRider.getWidth() * 0.5f;
        motorRider.setX(motorRiderX);

        // helper array for the generation of cars in 3 different layers
        lanePositions = new float[]{screenWidth * 0.125f, screenWidth * 0.4f, screenWidth * 0.690f};

        // when user touches the MAIN screen, this code runs
        View mainView = findViewById(R.id.main);
        mainView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    // in case of touch
                    case MotionEvent.ACTION_DOWN:
                        roadSpeed += 5f;
                        return true;

                    // in case of removal
                    case MotionEvent.ACTION_UP:
                        if(currentLevel == 1){
                            roadSpeed = 10f;
                        }else if(currentLevel == 2){
                            roadSpeed = 15f;
                        }else if(currentLevel == 3){
                            roadSpeed = 20f;
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // this method handles the road animation with runnable
    private void roadAnimation(){
        roadRunnable = new Runnable() {
            @Override
            public void run() {
                road1Y += roadSpeed;
                road2Y += roadSpeed;

                // after the road passes the screen height, replace the other road with previous one
                // to create a looping effect
                if(road1Y >= screenHeight){
                    road1Y = road2Y - screenHeight;
                }
                if(road2Y >= screenHeight){
                    road2Y = road1Y - screenHeight;
                }

                roadBackground1.setY(road1Y);
                roadBackground2.setY(road2Y);

                // in each 16ms, function calls itself
                roadHandler.postDelayed(this, 16);
            }
        };
        roadHandler.post(roadRunnable);
    }

    // level initialization method
    private void setLevel(int level){
        currentLevel = level;
        isGameActive = true;

        // activate the sensor at the beginning of the level
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);

        // handle the visibility of the buttons and other image views
        noviceLevelButton.setVisibility(View.GONE);
        intermediateLevelButton.setVisibility(View.GONE);
        advancedLevelButton.setVisibility(View.GONE);
        logoView.setVisibility(View.GONE);
        obstacle1.setVisibility(View.GONE);
        obstacle2.setVisibility(View.GONE);

        // manually assign the road speed and carSpawnDelay(ms)
        switch (currentLevel){
            case 1:
                roadSpeed = 10f;
                carSpawnDelay = 3000;
                break;
            case 2:
                roadSpeed = 15f;
                carSpawnDelay = 2000;
                break;
            case 3:
                roadSpeed = 20f;
                carSpawnDelay = 1500;
                break;
        }
        // start the animation and car spawn
        roadAnimation();
        carSpawner();
    }

    private void carSpawner(){
        carRunnable = new Runnable() {
            @Override
            public void run() {
                // choose a random lane to spawn a car
                int randomLane = random.nextInt(lanePositions.length);
                float carX = lanePositions[randomLane];

                // select a random car from the imageView id list
                int randomCar = random.nextInt(carImages.length);
                int selectedCar = carImages[randomCar];

                // create the car as imageView
                ImageView car = new ImageView(MainActivity.this);
                car.setImageResource(selectedCar);
                car.setX(carX);
                // Y was set to -200f to initialize the car off-screen
                car.setY(-200f);
                cars.add(car);
                ((ViewGroup) findViewById(R.id.main)).addView(car);

                carHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        car.setY(car.getY()+roadSpeed);

                        // if a car passes the screen height, remove the car
                        if(car.getY() > screenHeight){
                            ((ViewGroup) findViewById(R.id.main)).removeView(car);
                            cars.remove(car);
                        }
                        // check if there is collision
                        else if(isCollision(motorRider, car)){
                            gameOver();
                        }
                        // repeat the game until one of these conditions met
                        else{
                            carHandler.postDelayed(this, 16);
                        }
                    }
                },16);
                carHandler.postDelayed(this, carSpawnDelay);
            }
        };
        carHandler.post(carRunnable);
    }

    // method to check if there is collision
    private boolean isCollision(ImageView motorRider, ImageView obstacle){
        // get obstacle coordinates
        float obstacleLeft = obstacle.getX();
        float obstacleTop = obstacle.getY();
        float obstacleRight = obstacleLeft + obstacle.getWidth();
        float obstacleBottom = obstacleTop + obstacle.getHeight();

        // get player's(motor) coordinates
        float motorLeft = motorRider.getX();
        float motorTop = motorRider.getY();
        float motorRight = motorLeft + motorRider.getWidth();

        // compare the coordinates
        return motorRight > obstacleLeft && motorLeft < obstacleRight && motorTop < obstacleBottom;
    }

    // method to end the game
    private void gameOver(){
        isGameActive = false;

        // stopping running animations
        roadHandler.removeCallbacks(roadRunnable);
        carHandler.removeCallbacks(carRunnable);

        // removşng all cars from the screen
        for(ImageView car:cars){
            ((ViewGroup)findViewById(R.id.main)).removeView(car);
        }
        cars.clear();

        // sets the pregame condition
        motorRider.setX(motorRiderX);
        noviceLevelButton.setVisibility(View.VISIBLE);
        intermediateLevelButton.setVisibility(View.VISIBLE);
        advancedLevelButton.setVisibility(View.VISIBLE);
        logoView.setVisibility(View.VISIBLE);
        obstacle1.setVisibility(View.VISIBLE);
        obstacle2.setVisibility(View.VISIBLE);

        // closes listener for the accelerator
        sensorManager.unregisterListener(this);
    }

    // accelerometer method
    public void onSensorChanged(SensorEvent event){
        // based on device's tilt, move the motor horizontally
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && isGameActive){
            float x = event.values[0];
            motorRiderX -= x*7;  // change this value to make more slower or faster
            motorRiderX = Math.max(0, Math.min(screenWidth - motorRider.getWidth(), motorRiderX));
            motorRider.setX(motorRiderX);

            // end game if rider meets with the pavement
            if(motorRiderX < roadLeftPavement || motorRiderX + motorRider.getWidth() > roadRightPavement){
                gameOver();
            }
        }
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy){}

    // this method helps to stop sensor updates when the app is paused
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    // this method helps to restart sensor updates when the app becomes active
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }
}