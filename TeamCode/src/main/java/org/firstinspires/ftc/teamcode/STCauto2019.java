/* Copyright (c) 2017 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.teamcode.teamcode.vision;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcontroller.external.samples.HardwarePushbot;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.teamcode.pushbothardware;

/**
 * This file illustrates the concept of driving a path based on Gyro heading and encoder counts.
 * It uses the common Pushbot hardware class to define the drive on the robot.
 * The code is structured as a LinearOpMode
 *
 * The code REQUIRES that you DO have encoders on the wheels,
 *   otherwise you would use: PushbotAutoDriveByTime;
 *
 *  This code ALSO requires that you have a Modern Robotics I2C gyro with the name "gyro"
 *   otherwise you would use: PushbotAutoDriveByEncoder;
 *
 *  This code requires that the drive Motors have been configured such that a positive
 *  power command moves them forward, and causes the encoders to count UP.
 *
 *  This code uses the RUN_TO_POSITION mode to enable the Motor controllers to generate the run profile
 *
 *  In order to calibrate the Gyro correctly, the robot must remain stationary during calibration.
 *  This is performed when the INIT button is pressed on the Driver Station.
 *  This code assumes that the robot is stationary when the INIT button is pressed.
 *  If this is not the case, then the INIT should be performed again.
 *
 *  Note: in this example, all angles are referenced to the initial coordinate frame set during the
 *  the Gyro Calibration process, or whenever the program issues a resetZAxisIntegrator() call on the Gyro.
 *
 *  The angle of movement/rotation is assumed to be a standardized rotation around the robot Z axis,
 *  which means that a Positive rotation is Counter Clock Wise, looking down on the field.
 *  This is consistent with the FTC field coordinate conventions set out in the document:
 *  ftc_app\doc\tutorial\FTC_FieldCoordinateSystemDefinition.pdf
 *
 * Use Android Studios to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list
 */

@Autonomous(name="STCauto2019", group="Pushbot")
public class STCauto2019 extends LinearOpMode {

    /* Declare OpMode members. */
    pushbothardware robot = new pushbothardware();             // Use a Pushbot's hardware
    //ModernRoboticsI2cGyro   gyro    = null;                    // Additional Gyro device
    BNO055IMU imu;

    static final double     COUNTS_PER_MOTOR_REV    = 1120 ;    // eg: TETRIX Motor Encoder
    static final double     DRIVE_GEAR_REDUCTION    = 0.5 ;     // This is < 1.0 if geared UP
    static final double     WHEEL_DIAMETER_INCHES   = 4.0 ;     // For figuring circumference
    static final double     COUNTS_PER_INCH         = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
                                                      (WHEEL_DIAMETER_INCHES * 3.1415);

    // These constants define the desired driving/control characteristics
    // The can/should be tweaked to suite the specific robot drive train.
    static final double     DRIVE_SPEED             = 0.7;     // Nominal speed for better accuracy.
    static final double     TURN_SPEED              = 0.5;     // Nominal half speed for better accuracy.

    static final double     HEADING_THRESHOLD       = 1 ;      // As tight as we can make it with an integer gyro
    static final double     P_TURN_COEFF            = 0.1;     // Larger is more responsive, but also less stable
    static final double     P_DRIVE_COEFF           = 0.15;     // Larger is more responsive, but also less stable


    @Override
    public void runOpMode() {

        /*
         * Initialize the standard drive system variables.
         * The init() method of the hardware class does most of the work here
         */
        robot.init(hardwareMap);
        //gyro = (ModernRoboticsI2cGyro)hardwareMap.gyroSensor.get("gyro");

        // Ensure the robot it stationary, then reset the encoders and calibrate the gyro.
        robot.leftfront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.leftback.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.rightfront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.rightback.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Send telemetry message to alert driver that we are calibrating;
        telemetry.addData(">", "Calibrating Gyro");    //
        telemetry.update();

        //gyro.calibrate();

        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator(); // alt + enter for shortcut to import smthn
        parameters.mode = BNO055IMU.SensorMode.IMU;

        imu = hardwareMap.get(BNO055IMU.class, "imu 1");
        imu.initialize(parameters);

        /*// make sure the gyro is calibrated before continuing
        while (!isStopRequested() && gyro.isCalibrating())  {
            sleep(50);
            idle();
        }*/

        telemetry.addData(">", "Robot Ready.");    //
        telemetry.update();

        waitForStart();  // make sure gyro is calibrated before start button

        robot.leftfront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robot.leftback.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robot.rightfront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robot.rightback.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Wait for the game to start (Display Gyro value), and reset gyro before we move..
        while (!isStarted()) {
            telemetry.addData(">", "Robot Heading = %d", imu.getAngularOrientation());
            telemetry.update();
        }

        //gyro.resetZAxisIntegrator();

        // Step through each leg of the path,
        // Note: Reverse movement is obtained by setting a negative distance (not speed)
        // Put a hold after each turn

        gyroHold(0,0,1);
        driveForward(DRIVE_SPEED, true);            //drive forward 52in into glyph pile
        sleep(2400);
        gyroDrive(0, 0, 0);
        sleep(1000);
        strafe(DRIVE_SPEED, true);
        sleep(2400);
        gyroDrive(0,0,0);
        sleep(1000);
        driveForward(DRIVE_SPEED, false);
        sleep(2400);
        gyroDrive(0,0,0);
        sleep(1000);
        strafe(DRIVE_SPEED, false);
        sleep(2400);
        gyroDrive(0,0,0);
        //
        gyroHold(0,0,1);
        driveForward(DRIVE_SPEED, true);            //drive forward 52in into glyph pile
        sleep(2400);
        gyroDrive(0, 0, 0);
        sleep(1000);
        strafe(DRIVE_SPEED, true);
        sleep(2400);
        gyroDrive(0,0,0);
        sleep(1000);
        driveForward(DRIVE_SPEED, false);
        sleep(2400);
        gyroDrive(0,0,0);
        sleep(1000);
        strafe(DRIVE_SPEED, false);
        sleep(2400);
        gyroDrive(0,0,0);

        telemetry.addData("Path", "Complete");
        telemetry.update();
    }


   /**
    *  Method to drive on a fixed compass bearing (angle), based on encoder counts.
    *  Move will stop if either of these conditions occur:
    *  1) Move gets to the desired position
    *  2) Driver stops the opmode running.
    *
    * @param speed      Target speed for forward motion.  Should allow for _/- variance for adjusting heading
    * @param distance   Distance (in inches) to move from current position.  Negative distance means move backwards.
    * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
    *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
    *                   If a relative angle is required, add/subtract from current heading.
    */
    public void gyroDrive ( double speed,
                            double distance,
                            double angle) {

        int     newLeftfrontTarget;
        int     newLeftbackTarget;
        int     newRightfrontTarget;
        int     newRightbackTarget;
        int     moveCounts;
        double  max;
        double  error;
        double  steer;
        double  leftSpeed;
        double  rightSpeed;

        // Ensure that the opmode is still active
        if (opModeIsActive()) {

            // Determine new target position, and pass to motor controller
            moveCounts = (int)(distance * COUNTS_PER_INCH);
            newLeftfrontTarget = robot.leftfront.getCurrentPosition() + moveCounts;
            newLeftbackTarget = robot.leftback.getCurrentPosition() + moveCounts;
            newRightfrontTarget = robot.rightfront.getCurrentPosition() + moveCounts;
            newRightbackTarget = robot.rightback.getCurrentPosition() + moveCounts;

            // Set Target and Turn On RUN_TO_POSITION
            robot.leftfront.setTargetPosition(newLeftfrontTarget);
            robot.leftback.setTargetPosition(newLeftbackTarget);
            robot.rightfront.setTargetPosition(newRightfrontTarget);
            robot.rightback.setTargetPosition(newRightbackTarget);

            robot.leftfront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.leftback.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.rightfront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.rightback.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            // start motion.
            speed = Range.clip(Math.abs(speed), 0.0, 1.0);
            robot.leftfront.setPower(speed);
            robot.leftback.setPower(speed);
            robot.rightfront.setPower(speed);
            robot.rightback.setPower(speed);

            // keep looping while we are still active, and BOTH motors are running.
            while (opModeIsActive() &&
                   (robot.leftfront.isBusy() && robot.leftback.isBusy() && robot.rightfront.isBusy() && robot.rightback.isBusy())) {

                // adjust relative speed based on heading error.
                error = getError(angle);
                steer = getSteer(error, P_DRIVE_COEFF);

                // if driving in reverse, the motor correction also needs to be reversed
                if (distance < 0)
                    steer *= -1.0;

                leftSpeed = speed - steer;
                rightSpeed = speed + steer;

                // Normalize speeds if either one exceeds +/- 1.0;
                max = Math.max(Math.abs(leftSpeed), Math.abs(rightSpeed));
                if (max > 1.0)
                {
                    leftSpeed /= max;
                    rightSpeed /= max;
                }

                robot.leftfront.setPower(leftSpeed);
                robot.leftback.setPower(leftSpeed);
                robot.rightfront.setPower(rightSpeed);
                robot.rightback.setPower(rightSpeed);

                // Display drive status for the driver.
                telemetry.addData("Err/St",  "%5.1f/%5.1f",  error, steer);
                telemetry.addData("Target",  "%7d:%7d",      newLeftfrontTarget,  newLeftbackTarget,  newRightfrontTarget,  newRightbackTarget);
                telemetry.addData("Actual",  "%7d:%7d",      robot.leftfront.getCurrentPosition(),
                                                                            robot.leftback.getCurrentPosition(),
                                                                            robot.rightfront.getCurrentPosition(),
                                                                            robot.rightback.getCurrentPosition());
                telemetry.addData("Speed",   "%5.2f:%5.2f",  leftSpeed, rightSpeed);
                telemetry.update();
            }

            // Stop all motion;
            robot.leftfront.setPower(0);
            robot.leftback.setPower(0);
            robot.rightfront.setPower(0);
            robot.rightback.setPower(0);

            // Turn off RUN_TO_POSITION
            robot.leftfront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.leftback.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.rightfront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.rightback.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
    }

    /**
     *  Method to spin on central axis to point in a new direction.
     *  Move will stop if either of these conditions occur:
     *  1) Move gets to the heading (angle)
     *  2) Driver stops the opmode running.
     *
     * @param speed Desired speed of turn.
     * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
     *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                   If a relative angle is required, add/subtract from current heading.
     */
    public void gyroTurn (  double speed, double angle) {

        // keep looping while we are still active, and not on heading.
        while (opModeIsActive() && !onHeading(speed, angle, P_TURN_COEFF)) {
            // Update telemetry & Allow time for other processes to run.
            telemetry.update();
        }
    }

    /**
     *  Method to obtain & hold a heading for a finite amount of time
     *  Move will stop once the requested time has elapsed
     *
     * @param speed      Desired speed of turn.
     * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
     *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                   If a relative angle is required, add/subtract from current heading.
     * @param holdTime   Length of time (in seconds) to hold the specified heading.
     */
    public void gyroHold( double speed, double angle, double holdTime) {

        ElapsedTime holdTimer = new ElapsedTime();

        // keep looping while we have time remaining.
        holdTimer.reset();
        while (opModeIsActive() && (holdTimer.time() < holdTime)) {
            // Update telemetry & Allow time for other processes to run.
            onHeading(speed, angle, P_TURN_COEFF);
            telemetry.update();
        }

        // Stop all motion;
        robot.leftfront.setPower(0);
        robot.leftback.setPower(0);
        robot.rightfront.setPower(0);
        robot.rightback.setPower(0);
    }

    public void driveForward(double speed, boolean forward){
        if(forward) {
            robot.leftfront.setPower(speed);
            robot.rightfront.setPower(speed);
            robot.leftback.setPower(-speed);
            robot.rightback.setPower(-speed);
        } else {
            robot.leftfront.setPower(-speed);
            robot.rightfront.setPower(-speed);
            robot.leftback.setPower(speed);
            robot.rightback.setPower(speed);
        }
    }

    public void strafe(double speed, boolean right){
        if (right) {
            robot.leftfront.setPower(speed);
            robot.rightfront.setPower(-speed);
            robot.leftback.setPower(speed);
            robot.rightback.setPower(-speed);
        } else {
            robot.leftfront.setPower(-speed);
            robot.rightfront.setPower(speed);
            robot.leftback.setPower(-speed);
            robot.rightback.setPower(speed);
        }
    }

   public void grab(){
        robot.arm1.setPosition(0.5);
        robot.arm2.setPosition(0.5);
    }


    /**
     * Perform one cycle of closed loop heading control.
     *
     * @param speed     Desired speed of turn.
     * @param angle     Absolute Angle (in Degrees) relative to last gyro reset.
     *                  0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                  If a relative angle is required, add/subtract from current heading.
     * @param PCoeff    Proportional Gain coefficient
     * @return
     */
    boolean onHeading(double speed, double angle, double PCoeff) {
        double   error ;
        double   steer ;
        boolean  onTarget = false ;
        double leftSpeed;
        double rightSpeed;

        // determine turn power based on +/- error
        error = getError(angle);

        if (Math.abs(error) <= HEADING_THRESHOLD) {
            steer = 0.0;
            leftSpeed  = 0.0;
            rightSpeed = 0.0;
            onTarget = true;
        }
        else {
            steer = getSteer(error, PCoeff);
            rightSpeed  = speed * steer;
            leftSpeed   = -rightSpeed;
        }

        // Send desired speeds to motors.
        robot.leftfront.setPower(leftSpeed);
        robot.leftback.setPower(leftSpeed);
        robot.rightfront.setPower(rightSpeed);
        robot.rightback.setPower(rightSpeed);

        // Display it for the driver.
        telemetry.addData("Target", "%5.2f", angle);
        telemetry.addData("Err/St", "%5.2f/%5.2f", error, steer);
        telemetry.addData("Speed.", "%5.2f:%5.2f", leftSpeed, rightSpeed);

        return onTarget;
    }

    /**
     * getError determines the error between the target angle and the robot's current heading
     * @param   targetAngle  Desired angle (relative to global reference established at last Gyro Reset).
     * @return  error angle: Degrees in the range +/- 180. Centered on the robot's frame of reference
     *          +ve error means the robot should turn LEFT (CCW) to reduce error.
     */
    public double getError(double targetAngle) {

        double robotError;

        // calculate error in -179 to +180 range  (
        robotError = targetAngle - imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;
        while (robotError > 180)  robotError -= 360;
        while (robotError <= -180) robotError += 360;
        return robotError;
    }

    /**
     * returns desired steering force.  +/- 1 range.  +ve = steer left
     * @param error   Error angle in robot relative degrees
     * @param PCoeff  Proportional Gain Coefficient
     * @return
     */
    public double getSteer(double error, double PCoeff) {
        return Range.clip(error * PCoeff, -1, 1);
    }

}
