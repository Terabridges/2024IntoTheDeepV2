package org.firstinspires.ftc.teamcode.Tests;

/*
 * Copyright (c) 2024 Phil Malone
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import android.util.Size;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.SortOrder;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.opencv.ColorBlobLocatorProcessor;
import org.firstinspires.ftc.vision.opencv.ColorRange;
import org.firstinspires.ftc.vision.opencv.ColorSpace;
import org.firstinspires.ftc.vision.opencv.ImageRegion;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;

import java.util.ArrayList;
import java.util.List;


//@Disabled
@TeleOp(name = "OpenCvColor")
public class OpenCVColor extends LinearOpMode
{
    // initializes the width of the camera as well as its x and y direction
    double cX1 = 0;
    double cX2 = 0;
    private static double widthOfContour = 0;
    private static double heightOfContour = 0;
    double theta = 0;
    private static double blueArea;
    private static double redArea;
    private static double yellowArea;
    private static double smallestBlueArea;
    private static double smallestRedArea;
    private static double smallestYellowArea;
    private static double yellowCount = 0;
    private static double blueCount = 0;
    private static double redCount = 0;
    private static double smallestYellowDistance = 60;
    private static double smallestRedDistance = 60;
    private static double smallestBlueDistance = 60;

    // initializes camera and constants for camera resolution
    private static final int CAMERA_WIDTH = 320;
    private static final int CAMERA_HEIGHT = 240;

    // will be used to calculate distance
    public static final double objectWidthInRealWorld = 1.5; // this is the width of the sample, change if incorrect
    public static final double focalLength = 2.3; //replace with the actual length, as I have no idea what it is.

    public static final ColorRange YELLOW1 = new ColorRange(
            ColorSpace.YCrCb,
            new Scalar(107, 128,  0),
            new Scalar(255, 170, 120)
    );

    public static final ColorRange BLUE1 = new ColorRange(
            ColorSpace.YCrCb,
            new Scalar( 16,   0, 157),
            new Scalar(255, 127, 255)
    );

    public static final ColorRange RED1 = new ColorRange(
            ColorSpace.YCrCb,
            new Scalar( 32, 165,  0),
            new Scalar(255, 255, 132)
    );


    @Override
    public void runOpMode()
    {
        FtcDashboard dashboard = FtcDashboard.getInstance();
        telemetry = new MultipleTelemetry(telemetry, dashboard.getTelemetry());
        ColorBlobLocatorProcessor colorLocatorBlue = new ColorBlobLocatorProcessor.Builder()
                .setTargetColorRange(BLUE1)         // use a predefined color match
                .setContourMode(ColorBlobLocatorProcessor.ContourMode.EXTERNAL_ONLY)    // exclude blobs inside blobs
                .setRoi(ImageRegion.asUnityCenterCoordinates(-0.95, 0.95, 0.95, -0.95))  // search central 1/4 of camera view
                .setDrawContours(true)                        // Show contours on the Stream Preview
                .setBlurSize(5)                               // Smooth the transitions between different colors in image
                .build();

        ColorBlobLocatorProcessor colorLocatorRed = new ColorBlobLocatorProcessor.Builder()
                .setTargetColorRange(RED1)         // use a predefined color match
                .setContourMode(ColorBlobLocatorProcessor.ContourMode.EXTERNAL_ONLY)    // exclude blobs inside blobs
                .setRoi(ImageRegion.asUnityCenterCoordinates(-0.95, 0.95, 0.95, -0.95))  // search central 1/4 of camera view
                .setDrawContours(true)                        // Show contours on the Stream Preview
                .setBlurSize(5)                               // Smooth the transitions between different colors in image
                .build();

        ColorBlobLocatorProcessor colorLocatorYellow = new ColorBlobLocatorProcessor.Builder()
                .setTargetColorRange(YELLOW1)         // use a predefined color match
                .setContourMode(ColorBlobLocatorProcessor.ContourMode.EXTERNAL_ONLY)    // exclude blobs inside blobs
                .setRoi(ImageRegion.asUnityCenterCoordinates(-0.95, 0.95, 0.95, -0.95))  // search central 1/4 of camera view
                .setDrawContours(true)                        // Show contours on the Stream Preview
                .setBlurSize(5)                               // Smooth the transitions between different colors in image
                .build();

        VisionPortal portal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .addProcessor(colorLocatorBlue)
                .addProcessor(colorLocatorRed)
                .addProcessor(colorLocatorYellow)
                .setCameraResolution(new Size(CAMERA_WIDTH, CAMERA_HEIGHT))
                .setStreamFormat(VisionPortal.StreamFormat.YUY2)
                .enableLiveView(true)
                .setAutoStopLiveView(true)
                .build();


        // use to speed up debugging
        //telemetry.setMsTransmissionInterval(50);   // Speed up telemetry updates, Just use for debugging.
        //telemetry.setDisplayFormat(Telemetry.DisplayFormat.MONOSPACE);

        // WARNING:  To be able to view the stream preview on the Driver Station, this code runs in INIT mode.
        while (opModeIsActive() || opModeInInit())
        {
            portal.resumeLiveView();
            //telemetry.addData("preview on/off", "... Camera Stream\n");

            // Read the current list
            List<ColorBlobLocatorProcessor.Blob> blobsBlue = colorLocatorBlue.getBlobs();
            List<ColorBlobLocatorProcessor.Blob> blobsRed = colorLocatorRed.getBlobs();
            List<ColorBlobLocatorProcessor.Blob> blobsYellow = colorLocatorYellow.getBlobs();


            ColorBlobLocatorProcessor.Util.filterByArea(600, 20000, blobsBlue);  // filter out very small blobs.
            ColorBlobLocatorProcessor.Util.filterByArea(600, 20000, blobsRed);
            ColorBlobLocatorProcessor.Util.filterByArea(600, 20000, blobsYellow);


            yellowCount = blobsYellow.size();
            redCount = blobsRed.size();
            blueCount = blobsBlue.size();
            ColorBlobLocatorProcessor.Util.sortByArea(SortOrder.ASCENDING, blobsYellow);


            telemetry.addLine(" Area Density Aspect  Center");
            smallestYellowDistance = 60;
            smallestRedDistance = 60;
            smallestBlueDistance = 60;

            // Display the size (area) and center location for each Blob.
            for(ColorBlobLocatorProcessor.Blob b : blobsBlue)
            {
                RotatedRect boxFit = b.getBoxFit();
                telemetry.addLine(String.format("%5d  %4.2f   %5.2f  (%3d,%3d)",
                        b.getContourArea(), b.getDensity(), b.getAspectRatio(), (int) boxFit.center.x, (int) boxFit.center.y));
                widthOfContour = Math.min(boxFit.size.width, boxFit.size.height);
                heightOfContour = Math.min(boxFit.size.width, boxFit.size.height);
                double dist = getDistance(widthOfContour);
                double edgeDistanceFromCenter = (getDistanceFromCenter(Math.abs((boxFit.center.y) - (CAMERA_HEIGHT / 2.0))));
                //double angleFromCenter = angleFromCenter(edgeDistanceFromCenter, dist);
                telemetry.addData("width", widthOfContour);
                telemetry.addData("height", heightOfContour);
                telemetry.addData("distance", dist);
                blueArea = b.getContourArea();
                if (dist < smallestBlueDistance)
                    smallestBlueDistance = dist;

               // if (b.getContourArea() > 50)
                telemetry.addData("Blue is detected", "!");

            }

            for(ColorBlobLocatorProcessor.Blob b : blobsRed)
            {
                RotatedRect boxFit = b.getBoxFit();
                telemetry.addLine(String.format("%5d  %4.2f   %5.2f  (%3d,%3d)",
                        b.getContourArea(), b.getDensity(), b.getAspectRatio(), (int) boxFit.center.x, (int) boxFit.center.y));
                widthOfContour = Math.min(boxFit.size.width, boxFit.size.height);
                heightOfContour = Math.min(boxFit.size.width, boxFit.size.height);
                double dist = getDistance(widthOfContour);
                double edgeDistanceFromCenter = (getDistanceFromCenter(Math.abs((boxFit.center.y) - (CAMERA_HEIGHT / 2.0))));
                //double angleFromCenter = angleFromCenter(edgeDistanceFromCenter, dist);
                telemetry.addData("width", widthOfContour);
                telemetry.addData("height", heightOfContour);
                telemetry.addData("distance", dist);
                redArea = b.getContourArea();

                if (dist < smallestRedDistance)
                    smallestRedDistance = dist;

                //if (b.getContourArea() > 50)
                telemetry.addData("Red is detected", "!");
            }

            for(ColorBlobLocatorProcessor.Blob b : blobsYellow)
            {
                RotatedRect boxFit = b.getBoxFit();
                   telemetry.addLine(String.format("%5d  %4.2f   %5.2f  (%3d,%3d)",
                          b.getContourArea(), b.getDensity(), b.getAspectRatio(), (int) boxFit.center.x, (int) boxFit.center.y));
                widthOfContour = Math.min(boxFit.size.width, boxFit.size.height);
                heightOfContour = Math.min(boxFit.size.width, boxFit.size.height);
                double dist = getDistance(widthOfContour);
                double edgeDistanceFromCenter = (getDistanceFromCenter(Math.abs((boxFit.center.y) - (CAMERA_HEIGHT / 2.0))));
                //double angleFromCenter = angleFromCenter(edgeDistanceFromCenter, dist);
                telemetry.addData("width", widthOfContour);
                telemetry.addData("height", heightOfContour);
                telemetry.addData("distance", dist);
                yellowArea = b.getContourArea();
                if (dist != smallestYellowDistance)
                    smallestYellowDistance = dist;

               // if (b.getContourArea() > 50)
                telemetry.addData("Yellow is detected", "!");
            }
            telemetry.addData("Number of Red Contours", redCount);
            telemetry.addData("Number of Blue Contours", blueCount);
            telemetry.addData("Number of Yellow Contours", yellowCount);
            telemetry.addData("Smallest Red Distance", smallestRedDistance);
            telemetry.addData("Smallest Blue Distance", smallestBlueDistance);
            telemetry.addData("Smallest Yellow Distance", smallestYellowDistance);
            telemetry.addData("Color to go to", (decideColorForPickup()));
            telemetry.update();
            sleep(50);
        }
    }
    public static double getDistance(double width)
    {  //406
        double distance = 18.8 * (objectWidthInRealWorld * 12.0 * (focalLength))/width;
        return distance;
    }

    public static double getDistanceFromCenter(double distFromCenter)
    {
        return (focalLength * distFromCenter) / 72;
    }

    public static double angleFromCenter(double adjacent, double hypotenuse)
    {
        double angle = (Math.asin(adjacent/hypotenuse));
        return angle * (180.0/Math.PI);
    }

    public static String decideColorForPickup()
    {

        if ((yellowCount >= redCount && yellowCount >= blueCount) || (yellowArea > blueArea && yellowArea > redArea))
        {
            if (redCount == 0 && blueCount == 0) {
                return "Go to Yellow " + smallestYellowDistance;
            }
            else{
                if (((smallestBlueDistance > smallestYellowDistance) || (smallestRedDistance > smallestYellowDistance)) &&
                     (smallestYellowDistance < 30))
                {
                    return "Go to Yellow at" + smallestYellowDistance;
                }
            }
        }
        return "Go to Blue";
    }

}
