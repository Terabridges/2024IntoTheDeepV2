package org.firstinspires.ftc.teamcode.Subsystems;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.Utility.AbsoluteAnalogEncoder;

public class OuttakeSystem implements Subsystem {

    //Hardware
    public DcMotor outtakeTopVertical;
    public DcMotor outtakeBottomVertical;
    public CRServo outtakeLeftSwivel;
    public CRServo outtakeRightSwivel;
    public Servo outtakeWrist;
    public Servo outtakeClaw;
    public AnalogInput outtakeRightSwivelAnalog;
    public AbsoluteAnalogEncoder outtakeRightSwivelEnc;

    //SOFTWARE
    private int servoOffset = 0;
    private int motorOffset = 0;
    public boolean highBasketMode = true;

    //Positions
    private double CLAW_OPEN;
    private double CLAW_CLOSE;
    private double WRIST_PERP;
    private double WRIST_PAR;
    private int OUTTAKE_SWIVEL_UP;
    private int OUTTAKE_SWIVEL_DOWN;
    private int OUTTAKE_SLIDES_HIGH;
    private int OUTTAKE_SLIDES_LOW;
    private int OUTTAKE_SLIDES_DOWN;
    private int OUTTAKE_SLIDES_REST;

    //Targets
    public int outtakeSlidesTarget;
    public int outtakeSwivelTarget;
    public double clawTarget;
    public double wristTarget;

    //Max
    private double OUTTAKE_SLIDES_MAX_POWER = 1.0;
    private double OUTTAKE_SWIVEL_MAX_POWER = 1.0;

    //PIDF

    //Constructor
    public OuttakeSystem(HardwareMap map) {
        outtakeTopVertical = map.get(DcMotor.class, "outtake_top_vertical");
        outtakeBottomVertical = map.get(DcMotor.class, "outtake_bottom_vertical");
        outtakeLeftSwivel = map.get(CRServo.class, "outtake_left_swivel");
        outtakeRightSwivel = map.get(CRServo.class, "outtake_right_swivel");
        outtakeWrist = map.get(Servo.class, "outtake_wrist");
        outtakeClaw = map.get(Servo.class, "outtake_claw");
        outtakeRightSwivelAnalog = map.get(AnalogInput.class, "outtake_right_swivel_analog");
        outtakeRightSwivelEnc = new AbsoluteAnalogEncoder(outtakeRightSwivelAnalog, 3.3, 0);
    }

    //METHODS
    //Core Methods
    public void outtakeSetSlides(double pow) {
        if(pow > OUTTAKE_SLIDES_MAX_POWER) pow = OUTTAKE_SLIDES_MAX_POWER;
        if(pow < -OUTTAKE_SLIDES_MAX_POWER) pow = -OUTTAKE_SLIDES_MAX_POWER;
        outtakeBottomVertical.setPower(pow);
        outtakeTopVertical.setPower(pow);
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

    public void outtakeSwivelUp() {
        outtakeSwivelTarget = OUTTAKE_SWIVEL_UP;
    }

    public void outtakeSwivelDown() {
        outtakeSwivelTarget = OUTTAKE_SWIVEL_DOWN;
    }

    public void openClaw() {
        clawTarget = CLAW_OPEN;
    }

    public void closeClaw() {
        clawTarget = CLAW_CLOSE;
    }

    public void wristPar() {
        wristTarget = WRIST_PAR;
    }

    public void wristPerp() {
        wristTarget = WRIST_PERP;
    }

    //isPositions
    public boolean isSlidesDown(){
        return Math.abs(outtakeTopVertical.getCurrentPosition() - OUTTAKE_SLIDES_DOWN) <= motorOffset;
    }

    public boolean isSlidesRest(){
        return Math.abs(outtakeTopVertical.getCurrentPosition() - OUTTAKE_SLIDES_REST) <= motorOffset;
    }

    public boolean isSlidesHigh(){
        return Math.abs(outtakeTopVertical.getCurrentPosition() - OUTTAKE_SLIDES_HIGH) <= motorOffset;
    }

    public boolean isSlidesLow(){
        return Math.abs(outtakeTopVertical.getCurrentPosition() - OUTTAKE_SLIDES_LOW) <= motorOffset;
    }

    public boolean isSwivelUp() {
        return true;
        //Fix this //////////////////////////////////////////////////////////
    }
    public boolean isSwivelDown() {
        return true;
        //Fix this //////////////////////////////////////////////////////////
    }

    //PIDF
    private int setOuttakeSlidesPIDF(int target) {
        return 0;
    }

    private int setOuttakeSwivelPIDF(int target) {
        return 0;
    }

    //Interface Methods
    @Override
    public void toInit(){
        outtakeSlidesDown();
        outtakeSwivelDown();
        closeClaw();
        wristPar();
    }

    //TODO MAKE REGULAR SERVOS ONLY SET ONCE
    @Override
    public void update(){
        outtakeSetSlides(setOuttakeSlidesPIDF(outtakeSlidesTarget));
        outtakeSetSwivel(setOuttakeSwivelPIDF(outtakeSwivelTarget));
        setClaw(clawTarget);
        setWrist(wristTarget);
    }

}
