package org.firstinspires.ftc.teamcode.Subsystems;

import android.util.Size;
import android.view.contentcapture.DataRemovalRequest;

import java.util.Optional;
import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cDevice;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;


import org.firstinspires.ftc.teamcode.Utility.DataSampler;
import org.firstinspires.ftc.teamcode.Utility.DataSampler.SamplingMethod; //import enum for sampling styles
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.Utility.contourProperties;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.firstinspires.ftc.vision.opencv.ColorBlobLocatorProcessor;
import org.firstinspires.ftc.vision.opencv.ColorRange;
import org.firstinspires.ftc.vision.opencv.ColorSpace;
import org.firstinspires.ftc.vision.opencv.ImageRegion;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class VisionSystem implements Subsystem {

    //Hardware
    public RevColorSensorV3 intakeColorSensor;
    public AnalogInput leftBackDistance;
    public AnalogInput rightBackDistance;
    public Servo rightLight;

    //Software
    public NormalizedRGBA colors;
    private boolean camInited = false;

    public DataSampler rightDistanceSampling;
    public DataSampler leftDistanceSampling;
    public SamplingMethod samplingMethod = SamplingMethod.AVERAGE;
    public int sampleSize = 10;

    //HardwareMap hardwareMap;
    public double leftBackDistVal;
    public double rightBackDistVal;
    public boolean willStopAtObstacle = false;
    public boolean isCloseEnough = false;

    public boolean specimenVisionMode = false;

    //Constructor
    public VisionSystem(HardwareMap map) {
        intakeColorSensor = map.get(RevColorSensorV3.class, "intake_color_sensor");
        leftBackDistance = map.get(AnalogInput.class, "left_back_distance");
        rightBackDistance = map.get(AnalogInput.class, "right_back_distance");
        rightLight = map.get(Servo.class, "right_light");

        colors = new NormalizedRGBA();
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
        if ((colors.red > 0.016 && colors.green > 0.016) && !(Math.abs(colors.red - colors.green) > 0.02)){
            return "yellow";
        } else if ((colors.red > 0.01) && (colors.green < 0.0125)){
            return "red";
        } else if ((colors.blue > 0.008) && (colors.red < 0.01)){
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

        rightDistanceSampling.updateData(rightBackDistVal);
        leftDistanceSampling.updateData(leftBackDistVal);

        rightBackDistVal = rightDistanceSampling.calculateData();
        leftBackDistVal = leftDistanceSampling.calculateData();

    }

    public void switchVisionMode() {
        specimenVisionMode = !specimenVisionMode;
    }

    public boolean isClose() {
        return ((leftBackDistVal <= 143 && leftBackDistVal >= 130) || (rightBackDistVal <= 143 && rightBackDistVal >= 130));
        // NOT ACCURATE
        // WILL FIX WHEN TESTING
    }

    //Interface Methods
    @Override
    public void toInit() {
        willStopAtObstacle = false;

        rightDistanceSampling = new DataSampler(samplingMethod, sampleSize);
        leftDistanceSampling = new DataSampler(samplingMethod, sampleSize);

    }

    @Override
    public void update() {
        detectColor();
        if (!specimenVisionMode) {
            setLightColor(getColorVal());
        }
        getDistances();

        if (specimenVisionMode) {
            if (isClose()) {
                rightLight.setPosition(0.444);
            } else {
                rightLight.setPosition(0);
            }
        }
    }
}