package org.firstinspires.ftc.teamcode.Subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import org.firstinspires.ftc.robotcore.external.Telemetry;
//import org.firstinspires.ftc.teamcode.pedroPathing.localization.Pose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config
public class Robot {

    //Objects
    public HardwareMap hardwareMap;
    public Telemetry telemetry;
    public Gamepad gp1;
    public static VoltageSensor voltageSensor;

    //Subsystems
    public IntakeSystem intakeSystem;
    public OuttakeSystem outtakeSystem;
    public VisionSystem visionSystem;
    public DriveSystem driveSystem;

    //Other
    public String currentState = "none";

    //Subsystem List
    public List<Subsystem> subsystems;

    //Constructors
    public Robot(HardwareMap map, Telemetry t, Gamepad gp1){
        hardwareMap = map;
        telemetry = t;

        outtakeSystem = new OuttakeSystem(hardwareMap);
        intakeSystem = new IntakeSystem(hardwareMap);
        driveSystem = new DriveSystem(hardwareMap);
        visionSystem = new VisionSystem(hardwareMap, this);

        subsystems = new ArrayList<>(Arrays.asList(outtakeSystem, intakeSystem, visionSystem, driveSystem));

        this.gp1 = gp1;

        voltageSensor = hardwareMap.voltageSensor.iterator().next();
    }

    public Robot(HardwareMap map, Telemetry t){this(map, t, null);}

    //Methods
    public double getVoltage() {
        return voltageSensor.getVoltage();
    }

    public void rumble(int milliseconds){
        gp1.rumble(milliseconds);
    }

    public void setManualIntakeTrue(){
        intakeSystem.manualIntake = true;
        outtakeSystem.manualOuttake = false;
    }

    public void setManualSlidesTrue(){

        if (!outtakeSystem.manualOuttake) {
            outtakeSystem.manualOuttake = true;
            intakeSystem.manualIntake = false;
        } else {
            outtakeSystem.manualOuttake = false;
            intakeSystem.manualIntake = true;
            outtakeSystem.outtakeSlidesTarget = outtakeSystem.outtakeBottomVertical.getCurrentPosition();
        }
    }

    //Interface Methods
    public void update() {
        for (Subsystem s : subsystems) {
            s.update();
        }
    }

    public void toInit() {
        for (Subsystem s : subsystems) {
            s.toInit();
        }
    }

}