package org.firstinspires.ftc.teamcode.Subsystems;

import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cDevice;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;

public class VisionSystem implements Subsystem {

    //Hardware
    public RevColorSensorV3 intakeColorSensor;
    public AnalogInput leftBackDistance;
    public AnalogInput rightBackDistance;
    public Servo rightLight;
    public DriveSystem driveSystem;

    //Software
    NormalizedRGBA colors;
    private boolean camInited = false;

    HardwareMap hardwareMap;
    public double leftBackDistVal;
    public double rightBackDistVal;
    public boolean willStopAtObstacle;
    public boolean isCloseEnough = false;


    //Constructor
    public VisionSystem(HardwareMap map) {
        intakeColorSensor = map.get(RevColorSensorV3.class, "intake_color_sensor");
        leftBackDistance = map.get(AnalogInput.class, "left_back_distance");
        rightBackDistance = map.get(AnalogInput.class, "right_back_distance");
        rightLight = map.get(Servo.class, "right_light");
    }

    //Methods

    public double getColorPWN(String color){
        if (color.equals("red")){
            return 0.279;
        } else if (color.equals("blue")){
            return 0.611;
        } else if (color.equals("yellow")){
            return 0.388;
        } else {
            return 0;
        }
    }

    public String getColorVal(){
        if (colors.red > 0.07 && colors.green > 0.07){
            return "yellow";
        } else if (colors.red > 0.07){
            return "red";
        } else if (colors.blue > 0.05){
            return "blue";
        } else {
            return "none";
        }
    }

    public boolean isColor(String color){
        return getColorVal().equals(color);
    }

    public boolean isSomething(){
        return !getColorVal().equals("none");
    }

    public void detectColor(){
        colors = intakeColorSensor.getNormalizedColors();
    }

    public void setLightColor(String chosenColor) {
        rightLight.setPosition(getColorPWN(chosenColor));
    }

    public void getDistances() {
        leftBackDistVal = leftBackDistance.getVoltage();
        leftBackDistVal = (leftBackDistVal/3.3) * 4000;

        rightBackDistVal = rightBackDistance.getVoltage();
        rightBackDistVal = (rightBackDistVal/3.3) * 4000;
    }

    public void switchWillStop() {
        willStopAtObstacle = !willStopAtObstacle;
    }

    public void stopAtObstacle() {
        isCloseEnough = leftBackDistVal <= 145 || rightBackDistVal <= 145;
    }

    //Interface Methods
    @Override
    public void toInit() {
        willStopAtObstacle = false;
    }

    @Override
    public void update(){
        detectColor();
        setLightColor(getColorVal());
        getDistances();

        if (willStopAtObstacle) {
            stopAtObstacle();
        }

    }
}
