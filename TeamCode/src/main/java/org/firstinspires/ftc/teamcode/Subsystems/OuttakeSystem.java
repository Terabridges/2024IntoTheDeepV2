package org.firstinspires.ftc.teamcode.Subsystems;

import com.arcrobotics.ftclib.controller.PIDController;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;

import org.firstinspires.ftc.teamcode.Utility.AbsoluteAnalogEncoder;

public class OuttakeSystem implements Subsystem {

    //Hardware
    public DcMotor outtakeTopVertical;
    public DcMotor outtakeBottomVertical;
    public DcMotor outtakeMiddleVertical;
    public CRServo outtakeLeftSwivel;
    public CRServo outtakeRightSwivel;
    public Servo outtakeWrist;
    public Servo outtakeClaw;
    public AnalogInput outtakeSwivelAnalog;
    public AbsoluteAnalogEncoder outtakeSwivelEnc;
    public TouchSensor limit;

    //SOFTWARE
    private int servoOffset = 15;
    private int motorOffset = 20;
    private int oServoOffset = 12;
    public boolean highBasketMode = true;
    public boolean manualOuttake = false;
    public double outtakeSwivelGearRatio = 40.0/30.0;
    public boolean isClawOpen = false;
    private double outtakeSwivelOffset = 180.0;
    public boolean usePIDF = false;
    public boolean useLimitSwitch = false;

    //Positions
    public double CLAW_OPEN = 0.5;
    private double CLAW_CLOSE = 0.175;
    private double WRIST_DOWN = 0.6;
    private double WRIST_TRANSFER = 0.35; //0.3
    private double WRIST_UP = 0.9;
    private double WRIST_GRAB = 0.15;
    private double WRIST_LOCK = 0.55;
    //LIMIT BACK: 0
    //LIMIT FORWARD: 1
    //SPECIMEN LIMIT: 0.35
    private int OUTTAKE_SWIVEL_UP = 435; //412;
    private int OUTTAKE_SWIVEL_MID = 370;
    private int OUTTAKE_SWIVEL_DOWN = 180;
    private int OUTTAKE_SWIVEL_TRANSFER = 220; //210;
    private int OUTTAKE_SWIVEL_GRAB = 184; //146;
    private int OUTTAKE_SWIVEL_LOCK = 124;
    //SLIDES
    private int OUTTAKE_SLIDES_HIGH = -2400; //-3290;
    private int OUTTAKE_SLIDES_LOW = -1100;//-1600;
    private int OUTTAKE_SLIDES_DOWN = -100; //-35;
    private int OUTTAKE_SLIDES_REST = -600; //-750;
    private int OUTTAKE_SLIDES_GRAB_1 = OUTTAKE_SLIDES_DOWN; //-35;
    private int OUTTAKE_SLIDES_SCORE_1 = -1160; //-1700
    private int OUTTAKE_SLIDES_SCORE_2 = -700; //-1020

    private int OUTTAKE_SLIDES_PARK = -452; //-600
    private int OUTTAKE_SWIVEL_PARK = 283;

    public int outtakeCounter = 0;

    //Max
    private double OUTTAKE_SLIDES_MAX_POWER = 1.0;
    private double OUTTAKE_SWIVEL_MAX_POWER = 1.0;

    //PIDF
    private double ticks_in_degree = 144.0 / 180.0;

    //Third PID for outtake slides
    public PIDController outtakeSlidesController;
    public static double p3 = 0.008, i3 = 0.00003, d3 = 0.00001;
    public static double f3 = 0.0;
    public static int outtakeSlidesTarget;
    double outtakeSlidesPos;
    double pid3, targetOuttakeSlidesAngle, ff3, currentOuttakeSlidesAngle, outtakeSlidesPower;

    //Fourth PID for outtake swivel
    public PIDController outtakeSwivelController;
    public static double p4 = 0.003, i4 = 0.001, d4 = 0.00005;
    public static double f4 = -0.02;
    public static int outtakeSwivelTarget;
    double outtakeSwivelPos;
    double pid4, targetOuttakeSwivelAngle, ff4, currentOuttakeSwivelAngle, outtakeSwivelPower;

    //Constructor
    public OuttakeSystem(HardwareMap map) {
        outtakeTopVertical = map.get(DcMotor.class, "outtake_top_vertical");
        outtakeBottomVertical = map.get(DcMotor.class, "outtake_bottom_vertical");
        outtakeMiddleVertical = map.get(DcMotor.class, "outtake_middle_vertical");
        outtakeLeftSwivel = map.get(CRServo.class, "outtake_left_swivel");
        outtakeRightSwivel = map.get(CRServo.class, "outtake_right_swivel");
        outtakeWrist = map.get(Servo.class, "outtake_wrist");
        outtakeClaw = map.get(Servo.class, "outtake_claw");
        outtakeSwivelAnalog = map.get(AnalogInput.class, "outtake_right_swivel_analog");
        outtakeSwivelEnc = new AbsoluteAnalogEncoder(outtakeSwivelAnalog, 3.3, outtakeSwivelOffset, outtakeSwivelGearRatio);
        limit = map.get(TouchSensor.class, "limit");

        outtakeTopVertical.setDirection(DcMotorSimple.Direction.REVERSE);
        outtakeBottomVertical.setDirection(DcMotorSimple.Direction.REVERSE);

        outtakeLeftSwivel.setDirection(DcMotorSimple.Direction.REVERSE);

        outtakeSlidesController = new PIDController(p3, i3, d3);
        outtakeSwivelController = new PIDController(p4, i4, d4);
    }

    //METHODS
    //Core Methods
    public void outtakeSetSlides(double pow) {
        if(pow > OUTTAKE_SLIDES_MAX_POWER) pow = OUTTAKE_SLIDES_MAX_POWER;
        if(pow < -OUTTAKE_SLIDES_MAX_POWER) pow = -OUTTAKE_SLIDES_MAX_POWER;
        outtakeBottomVertical.setPower(pow);
        outtakeTopVertical.setPower(pow);
        outtakeMiddleVertical.setPower(pow);
    }

    public void outtakeSetSwivel(double pow) {
        if(pow > OUTTAKE_SWIVEL_MAX_POWER) pow = OUTTAKE_SWIVEL_MAX_POWER;
        if(pow < -OUTTAKE_SWIVEL_MAX_POWER) pow = -OUTTAKE_SWIVEL_MAX_POWER;
        outtakeLeftSwivel.setPower(pow);
        outtakeRightSwivel.setPower(pow);
    }

    public void setClaw(double pos) {
        outtakeClaw.setPosition(pos);
    }

    public void setWrist(double pos) {
        outtakeWrist.setPosition(pos);
    }

    //Other Methods
    public void outtakeSlidesHigh(){
        outtakeSlidesTarget = OUTTAKE_SLIDES_HIGH;
    }

    public void outtakeSlidesLow(){
        outtakeSlidesTarget = OUTTAKE_SLIDES_LOW;
    }

    public void outtakeSlidesDown(){
        outtakeSlidesTarget = OUTTAKE_SLIDES_DOWN;
    }

    public void outtakeSlidesRest() {
        outtakeSlidesTarget = OUTTAKE_SLIDES_REST;
    }

    public void outtakeSlidesGrab1() {
        outtakeSlidesTarget = OUTTAKE_SLIDES_GRAB_1;
    }

    public void outtakeSlidesScore1() {
        outtakeSlidesTarget = OUTTAKE_SLIDES_SCORE_1;
    }

    public void outtakeSlidesScore2() {
        outtakeSlidesTarget = OUTTAKE_SLIDES_SCORE_2;
    }

    public void outtakeSwivelUp() {
        outtakeSwivelTarget = OUTTAKE_SWIVEL_UP;
    }

    public void outtakeSwivelDown() {
        outtakeSwivelTarget = OUTTAKE_SWIVEL_DOWN;
    }

    public void outtakeSwivelMid() {
        outtakeSwivelTarget = OUTTAKE_SWIVEL_MID;
    }

    public void outtakeSwivelGrab() {
        outtakeSwivelTarget = OUTTAKE_SWIVEL_GRAB;
    }

    public void outtakeSwivelLock() {
        outtakeSwivelTarget = OUTTAKE_SWIVEL_LOCK;
    }

    public void outtakeSwivelTransfer(){outtakeSwivelTarget = OUTTAKE_SWIVEL_TRANSFER;}

    public void openClaw() {
        setClaw(CLAW_OPEN);
    }

    public void closeClaw() {
        setClaw((CLAW_CLOSE));
    }

    public void wristUp() {
        setWrist(WRIST_UP);
    }

    public void wristDown() {
        setWrist(WRIST_DOWN);
    }

    public void wristTransfer() {setWrist(WRIST_TRANSFER);}

    public void wristGrab() {setWrist(WRIST_GRAB);}

    public void wristLock() {setWrist(WRIST_LOCK);}

    public void outtakeSwivelPark() {
        outtakeSwivelTarget = OUTTAKE_SWIVEL_PARK;
    }

    public void outtakeSlidesPark(){
        outtakeSlidesTarget = OUTTAKE_SLIDES_PARK;
    }

    public void resetSlideEncoders() {
        outtakeMiddleVertical.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }

    public void resetEncodersButton() {
        outtakeMiddleVertical.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }

    //isPositions
    public boolean isSlidesDown(){
        return Math.abs(outtakeMiddleVertical.getCurrentPosition() - OUTTAKE_SLIDES_DOWN) <= motorOffset;
    }

    public boolean isSlidesAlmostDown(){
        return outtakeMiddleVertical.getCurrentPosition() >= -60; //-100
    }

    public boolean isSlidesRest(){
        return Math.abs(outtakeMiddleVertical.getCurrentPosition() - OUTTAKE_SLIDES_REST) <= motorOffset;
    }

    public boolean isSlidesHigh(){
        return Math.abs(outtakeMiddleVertical.getCurrentPosition() - (OUTTAKE_SLIDES_HIGH)) <= motorOffset;
    }

    public boolean isSlidesAlmostHigh(){
        return outtakeMiddleVertical.getCurrentPosition() <= (OUTTAKE_SLIDES_HIGH + 200); //350
    }

    public boolean isSlidesGrab1(){
        return Math.abs(outtakeMiddleVertical.getCurrentPosition() - OUTTAKE_SLIDES_GRAB_1) <= motorOffset;
    }
    public boolean isSlidesScore1(){
        return Math.abs(outtakeMiddleVertical.getCurrentPosition() - OUTTAKE_SLIDES_SCORE_1) <= motorOffset;
    }
    public boolean isSlidesScore2(){
        return Math.abs(outtakeMiddleVertical.getCurrentPosition() - OUTTAKE_SLIDES_SCORE_2) <= motorOffset;
    }

    public boolean isSlidesLow(){
        return Math.abs(outtakeMiddleVertical.getCurrentPosition() - OUTTAKE_SLIDES_LOW) <= motorOffset;
    }

    public boolean isSwivelUp() {
        return Math.abs(outtakeSwivelEnc.getCurrentPosition() - OUTTAKE_SWIVEL_UP) <= servoOffset;
    }
    public boolean isSwivelDown() {
        return Math.abs(outtakeSwivelEnc.getCurrentPosition() - OUTTAKE_SWIVEL_DOWN) <= servoOffset;
    }

    public boolean isSwivelGrab() {
        return Math.abs(outtakeSwivelEnc.getCurrentPosition() - OUTTAKE_SWIVEL_GRAB) <= servoOffset;
    }

    public boolean isSwivelTransfer() {
        return Math.abs(outtakeSwivelEnc.getCurrentPosition() - OUTTAKE_SWIVEL_TRANSFER) <= oServoOffset;
    }

    public void setOuttakeHigher(){
        OUTTAKE_SLIDES_HIGH -= 40; //50
        OUTTAKE_SLIDES_LOW -= 40;
        outtakeCounter += 1;
    }

    public void setOuttakeLower(){
        OUTTAKE_SLIDES_HIGH += 40; //50
        OUTTAKE_SLIDES_LOW += 40;
        outtakeCounter -= 1;
    }

    //PIDF
    public double setOuttakeSlidesPIDF(int target) {
        outtakeSlidesController.setPID(p3, i3, d3);
        outtakeSlidesPos = outtakeMiddleVertical.getCurrentPosition();
        pid3 = outtakeSlidesController.calculate(outtakeSlidesPos, target);
        targetOuttakeSlidesAngle = target;
        ff3 = (Math.sin(Math.toRadians(targetOuttakeSlidesAngle))) * f3;
        currentOuttakeSlidesAngle = Math.toRadians((outtakeSlidesPos) / ticks_in_degree);

        outtakeSlidesPower = pid3 + ff3;

        return outtakeSlidesPower;
    }

    public double setOuttakeSwivelPIDF(int target) {
        outtakeSwivelController.setPID(p4, i4, d4);
        outtakeSwivelPos = outtakeSwivelEnc.getCurrentPosition();
        pid4 = outtakeSwivelController.calculate(outtakeSwivelPos, target);
        targetOuttakeSwivelAngle = target;
        ff4 = (Math.sin(Math.toRadians(targetOuttakeSwivelAngle))) * f4;
        currentOuttakeSwivelAngle = Math.toRadians((outtakeSwivelPos) / ticks_in_degree);

        outtakeSwivelPower = pid4 + ff4;

        return outtakeSwivelPower;
    }

    //Interface Methods
    @Override
    public void toInit(){
        if (!(outtakeMiddleVertical.getCurrentPosition() < OUTTAKE_SLIDES_LOW)){
            outtakeSlidesRest();
        }

        outtakeSwivelDown();
        closeClaw();
        wristDown();
    }

    @Override
    public void update(){
        if (!manualOuttake) {
            outtakeSetSlides(setOuttakeSlidesPIDF(outtakeSlidesTarget));
        }
//        if (usePIDF){
//            outtakeSetSlides(setOuttakeSlidesPIDF(outtakeSlidesTarget));
//        }
        outtakeSetSwivel(setOuttakeSwivelPIDF(outtakeSwivelTarget));

        if (useLimitSwitch && (limit.isPressed() && (Math.abs(outtakeMiddleVertical.getCurrentPosition()) > 100))){ //150
            resetEncodersButton();
        }

        //IDK ABOUT THIS
        if(outtakeMiddleVertical.getMode().equals(DcMotor.RunMode.STOP_AND_RESET_ENCODER)){
            outtakeMiddleVertical.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
    }
}
