package org.firstinspires.ftc.teamcode.Autonomous;


import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.pedropathing.pathgen.PathBuilder;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Subsystems.IntakeSystem;
import org.firstinspires.ftc.teamcode.Subsystems.OuttakeSystem;
import org.firstinspires.ftc.teamcode.Subsystems.Robot;
import org.firstinspires.ftc.teamcode.Subsystems.VisionSystem;
import org.firstinspires.ftc.teamcode.TeleOp.MainTeleOp;
import org.firstinspires.ftc.teamcode.pedroPathing.constants.FConstants;
import org.firstinspires.ftc.teamcode.pedroPathing.constants.LConstants;

import com.sfdev.assembly.state.StateMachine;
import com.sfdev.assembly.state.StateMachineBuilder;

import com.pedropathing.follower.Follower;
import com.pedropathing.localization.Pose;
import com.pedropathing.pathgen.BezierLine;
import com.pedropathing.pathgen.PathChain;
import com.pedropathing.pathgen.Point;
import com.pedropathing.util.Constants;

@Config
@Autonomous(name="BucketAuto", group="Auto")
public class BucketAuto extends LinearOpMode
{
    //Subsystems
    Robot r;
    IntakeSystem i;
    OuttakeSystem o;
    //VisionSystem v;

    //State Factory
    StateMachine main;
    StateMachine score;
    StateMachine pickup;

    //Pedro
    private Follower follower;

    Pose startPose = new Pose(AConstants.BOT_CENTER_X, 96+ AConstants.BOT_CENTER_Y, Math.toRadians(0));
    Pose scorePose = new Pose(24-9, 120+9, Math.toRadians(315));

    Pose firstSampleStart = new Pose(AConstants.START_X, AConstants.FIRST_SAMPLE.getY(), Math.toRadians(0));
    Pose secondSampleStart = new Pose(AConstants.START_X, AConstants.SECOND_SAMPLE.getY(), Math.toRadians(0));
    Pose thirdSampleStart = new Pose(AConstants.START_X, AConstants.THIRD_SAMPLE.getY(), Math.toRadians(31));
    Pose firstSampleEnd = new Pose(AConstants.END_X, AConstants.FIRST_SAMPLE.getY(), Math.toRadians(0));
    Pose secondSampleEnd = new Pose(AConstants.END_X, AConstants.SECOND_SAMPLE.getY(), Math.toRadians(0));
    Pose thirdSampleEnd = new Pose(AConstants.END_X+4, AConstants.THIRD_SAMPLE.getY()+3, Math.toRadians(28.5));

    Pose[] samples = {firstSampleStart, secondSampleStart, thirdSampleStart};
    Pose[] scoreFrom = {firstSampleEnd, secondSampleEnd, thirdSampleEnd};

    private PathChain goToScore, goToScorePreload, goToSample, intakeSample, failedIntake;

    //Enums
    enum mainStates
    {
        SCORE,
        PICKUP,
        STOP
    }

    enum scoreStates
    {
        GO_TO_SCORE,
        //RAISE_OUTTAKE, (If you want to split up movement + raising into two.)
        DUNK,
        OPEN_CLAW,
        CLOSE_CLAW,
        //LOWER_OUTTAKE,
        STOP
    }

    enum pickupStates
    {
        GO_TO_PICKUP,
        EXTEND_INTAKE,
        MOVE_FORWARD,
        RETRACT_INTAKE,

        //Transfer
        SWIVEL_DOWN,
        INTAKE_TRANSFER,
        OUTTAKE_TRANSFER,
        OUTTAKE_RESET,
        INTAKE_RESET,

        FAILED_PICKUP,
        STOP

    }

    //Other
    public ElapsedTime runtime = new ElapsedTime();
    int curSample = 0;
    boolean failedPickup = false;
    boolean isPreload = true;

    @Override
    public void runOpMode() throws InterruptedException
    {
        r = new Robot(hardwareMap, telemetry);
        i = new IntakeSystem(hardwareMap);
        o = new OuttakeSystem(hardwareMap);
        //v = new VisionSystem(hardwareMap);

        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        Constants.setConstants(FConstants.class, LConstants.class);
        follower = new Follower(hardwareMap);
        follower.setStartingPose(startPose);

        follower.setMaxPower(AConstants.STANDARD_POWER);

        buildPaths();
        buildStateMachines();

        //Press Start
        waitForStart();

        runtime.reset();
        r.toInit();
        main.start();

        //Main Loop
        while (opModeIsActive())
        {
            follower.update();
            main.update();
            r.update();

            telemetry();

            i.intakeSetSpin(i.intakeSpinTarget);

            if (main.getStateString().equals("STOP"))
            {
                main.stop();
                main.reset();
                return;
            }
        }
    }

    public void buildPaths()
    {
        goToScore = buildLinearPath(scoreFrom[curSample], scorePose);
        goToScorePreload = buildLinearPath(startPose, scorePose);
        goToSample = buildLinearPath(scorePose, samples[curSample]);
        intakeSample = buildLinearPath(samples[curSample], scoreFrom[curSample]);
        failedIntake = buildLinearPath(scoreFrom[curSample], samples[curSample]);
    }

    public void buildStateMachines()
    {
        main = new StateMachineBuilder()
                .state(mainStates.SCORE)
                .onEnter(() -> score.start())
                .loop(() -> score.update())
                .transition(() -> curSample > 2 && score.getStateString().equals("STOP"), mainStates.STOP) //TD: Park here
                .transition(() -> score.getStateString().equals("STOP"), mainStates.PICKUP)
                .onExit(() -> score.reset())

                .state(mainStates.PICKUP)
                .onEnter(() -> pickup.start())
                .loop(() -> pickup.update())
                .transition(() -> pickup.getStateString().equals("STOP") && !failedPickup, mainStates.SCORE)
                .transition(() -> pickup.getStateString().equals("STOP") && failedPickup, mainStates.PICKUP)
                .onExit(() -> {
                    pickup.reset();
                    failedPickup = false;
                })

                .state(mainStates.STOP)

                .build();

        score = new StateMachineBuilder()
                .state(scoreStates.GO_TO_SCORE)
                .onEnter(() -> {
                    buildPaths();
                    if (isPreload)
                    {
                        follower.followPath(goToScore, true);
                    }
                    else
                        follower.followPath(goToScorePreload, true);
                    o.outtakeSlidesHigh();
                    o.outtakeSwivelMid();
                })
                .transition(() -> o.isSlidesHigh() && !follower.isBusy(), scoreStates.DUNK)

                .state(scoreStates.DUNK)
                .onEnter(() -> {
                    o.outtakeSwivelUp();
                    o.wristUp();
                })
                .transition(() -> o.isSwivelUp(), scoreStates.OPEN_CLAW)

                .state(scoreStates.OPEN_CLAW)
                .onEnter(() -> o.openClaw())
                .transitionTimed(AConstants.DROP_TIME, scoreStates.CLOSE_CLAW)

                .state(scoreStates.CLOSE_CLAW)
                .onEnter(() -> {
                    o.closeClaw();
                    o.outtakeSwivelDown();
                    o.wristTransfer();
                    if (!isPreload)
                        curSample++;
                    isPreload = false;
                })
                .transition(() -> o.isSwivelDown(), scoreStates.STOP)

                .state(scoreStates.STOP)

                .build();

        pickup = new StateMachineBuilder()
                .state(pickupStates.GO_TO_PICKUP)
                .onEnter(() -> {
                    buildPaths();
                    o.outtakeSlidesRest();
                    i.intakeSwivelRest();
                    follower.followPath(goToSample, true);
                })
                .transition(() -> !follower.isBusy() && o.isSlidesRest(), pickupStates.EXTEND_INTAKE)

                .state(pickupStates.EXTEND_INTAKE)
                .onEnter(() -> {
                    i.intakeSlidesExtend();
                    i.intakeSwivelDown();
                })
                .transition(() -> i.isIntakeExtended(), pickupStates.MOVE_FORWARD)

                .state(pickupStates.MOVE_FORWARD)
                .onEnter(() -> {
                    i.intakeSpinTarget = -0.75;
                    //i.intakeSpin.setPower(-0.75);
                    follower.setMaxPower(AConstants.LOW_POWER);
                    buildPaths();
                    follower.followPath(intakeSample, true);
                })
                .transition(() -> /*v.isSomething() && */ !follower.isBusy(), pickupStates.INTAKE_TRANSFER)
                //.transition(() -> !v.isSomething() && !follower.isBusy(), pickupStates.FAILED_PICKUP)
                .onExit(() -> {
                    i.intakeSpinTarget = 0;
                    i.intakeSwivelRest();
                    follower.setMaxPower(AConstants.STANDARD_POWER);
                })

                //TRANSFER
                .state(pickupStates.INTAKE_TRANSFER)
                .onEnter( () -> {
                    i.intakeSlidesRetract();
                    o.openClaw();
                })
                .transition(() -> i.isIntakeRetracted(), pickupStates.SWIVEL_DOWN)

                .state(pickupStates.SWIVEL_DOWN)
                .onEnter(() -> i.intakeSwivelTransfer())
                .transition( () -> i.isSwivelTransfer(), pickupStates.OUTTAKE_TRANSFER, () -> {
                    o.outtakeSwivelTransfer();
                    o.outtakeSlidesDown();
                    o.wristTransfer();
                })

                .state(pickupStates.OUTTAKE_TRANSFER)
                .transition( () -> o.isSlidesDown(), pickupStates.OUTTAKE_RESET, () -> o.closeClaw())

                .state(pickupStates.OUTTAKE_RESET)
                .transitionTimed(0.2, pickupStates.INTAKE_RESET)

                .state(pickupStates.INTAKE_RESET)
                .onEnter( () -> o.outtakeSlidesRest())
                .transition( () -> o.isSlidesRest(), pickupStates.STOP, () -> {
                    o.outtakeSwivelDown();
                    o.wristDown();
                })
                //TRANSFER END

                .state(pickupStates.FAILED_PICKUP)
                .onEnter(() ->
                {
                    if (curSample < 2)
                    {
                        failedPickup = true;
                        buildPaths();
                        follower.followPath(failedIntake, true);
                    }
                })
                .transition(() -> !follower.isBusy(), pickupStates.STOP)

                .state(pickupStates.STOP)

                .build();
    }

    public void telemetry()
    {
        telemetry.addData("main state", main.getStateString());
        telemetry.addData("pickup state", pickup.getStateString());
        telemetry.addData("score state", score.getStateString());
        telemetry.addData("x", follower.getPose().getX());
        telemetry.addData("y", follower.getPose().getY());
        telemetry.addData("heading", follower.getPose().getHeading());
        //telemetry.addData("see something", v.isSomething());
        telemetry.addData("current sample", curSample);
        telemetry.addData("spinTarget", i.intakeSpinTarget);
        telemetry.update();
    }

    private PathChain buildLinearPath(Pose start, Pose end) {
        return new PathBuilder()
                .addPath(new BezierLine(new Point(start), new Point(end)))
                .setLinearHeadingInterpolation(start.getHeading(), end.getHeading())
                .build();
    }
}
