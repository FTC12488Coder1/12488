package org.firstinspires.ftc.teamcode;



import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor;

import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

@TeleOp
public class MyControlRobot2 extends LinearOpMode {
    private DcMotor fr;
    private DcMotor fl;
    private DcMotor br;
    private DcMotor bl;
    private DcMotor xOdometer = null;
    private DcMotor yOdometer = null;
    private DcMotor lift;
    private OpticalDistanceSensor ods;
    private ColorSensor color;

    private double tolerance = 0.0; //offset for when to stop stopping | Unused as of now, but might be needed later (needs testing)
    private double x_pos = 0.0;
    private double y_pos = 0.0;
    private double x_offset = 0.0;
    private double y_offset = 0.0;
    private double previous_x = 0.0;
    private double previous_y = 0.0;
    private final int linMax = 3900;
    private double globalAngle;
    private Orientation lastAngles = new Orientation();

    private double val_to_squares(int val) {
        return (val / 2048.) * 4.8 * Math.PI / 60.9; //by square
    }
    @Override
    public void runOpMode() throws InterruptedException {
        // Declare our motors
        // Make sure your ID's match your configuration
        fl = hardwareMap.dcMotor.get("fl");
        bl = hardwareMap.dcMotor.get("bl");
        fr = hardwareMap.dcMotor.get("fr");
        br = hardwareMap.dcMotor.get("br");
        xOdometer = hardwareMap.get(DcMotor.class, "odo1");
        yOdometer = hardwareMap.get(DcMotor.class, "odo2");
        ods = hardwareMap.get(OpticalDistanceSensor.class, "ods");
        color = hardwareMap.get(ColorSensor.class, "color");
        lift = hardwareMap.get(DcMotor.class, "lift");
        // Reverse the right side motors
        // Reverse left motors if you are using NeveRests
        //fr.setDirection(DcMotorSimple.Direction.REVERSE);
        //br.setDirection(DcMotorSimple.Direction.REVERSE);
        fl.setDirection(DcMotorSimple.Direction.REVERSE);
        bl.setDirection(DcMotorSimple.Direction.REVERSE);

        xOdometer.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        yOdometer.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Retrieve the IMU from the hardware map
        BNO055IMU imu = hardwareMap.get(BNO055IMU.class, "imu");
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        // Technically this is the default, however specifying it is clearer
        parameters.angleUnit = BNO055IMU.AngleUnit.RADIANS;
        // Without this, data retrieving from the IMU throws an exception
        imu.initialize(parameters);

        double SLOW = 1.5;
        /*resets encoder, and then sets it back to RUN_WITHOUT_ENCODER mode*/
        waitForStart();
        //lin.setTargetPosition(0);
        if (isStopRequested()) return;


        while (opModeIsActive()) {
            double y = -gamepad1.left_stick_y; // Remember, this is reversed!
            double x = gamepad1.left_stick_x * 1.1; // Counteract imperfect strafing
            double rx = gamepad1.right_stick_x;

            // Read inverse IMU heading, as the IMU heading is CW positive
            double botHeading = -imu.getAngularOrientation().firstAngle;

            double rotX = x * Math.cos(botHeading) - y * Math.sin(botHeading);
            double rotY = x * Math.sin(botHeading) + y * Math.cos(botHeading);

            // Denominator is the largest motor power (absolute value) or 1
            // This ensures all the powers maintain the same ratio, but only when
            // at least one is out of the range [-1, 1]
            double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);
            double frontLeftPower = (rotY + rotX + rx) / (denominator*SLOW);
            double backLeftPower = (rotY - rotX + rx) / (denominator*SLOW);
            double frontRightPower = (rotY - rotX - rx) / (denominator*SLOW);
            double backRightPower = (rotY + rotX - rx) / (denominator*SLOW);

            telemetry.addData("frontLeftPower", frontLeftPower);
            telemetry.addData("backLeftPower", backLeftPower);
            telemetry.addData("frontRightPower", frontRightPower);
            telemetry.addData("backRightPower", backRightPower);

            fl.setPower(frontLeftPower);
            bl.setPower(backLeftPower);
            fr.setPower(frontRightPower);
            br.setPower(backRightPower);

            if(gamepad1.start){
                botHeading = 0.0;
            }
            if (gamepad1.left_trigger > 0.3){
                SLOW = 4;

            }
            if(gamepad1.right_trigger > 0.3) {
                SLOW = 1.5;
            }
            if(Math.abs(lift.getCurrentPosition())<linMax){
                if(gamepad2.left_stick_y!=0){
                    while((gamepad2.left_stick_y!=0)&&(Math.abs(lift.getCurrentPosition())<linMax)){
                        lift.setPower(gamepad2.left_stick_y);
                    }
                    lift.setPower(0);
                }
            }else{
                lift.setPower(0);
                telemetry.addData("Reached max Lin Height",0);
                lift.setPower(0.2);
                sleep(250);
                lift.setPower(0);
            }


            telemetry.addData("Slowness: ", SLOW);
            telemetry.update();

        }
    }
}