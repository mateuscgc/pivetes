package PIVA;
import robocode.*;

import static robocode.util.Utils.normalRelativeAngle;

public abstract class TwoFrontsRobot extends AdvancedRobot {
    protected RobotStatus lastStatus;
    protected int basf = 0;
    protected boolean changeFront = false;

    // Support methods
    public double RadiansFromCurrentFront(double sup) {
        return (sup + basf*Math.PI) % (2*Math.PI);
    }
    public double DegreesFromCurrentFront(double sup) {
        return (sup + basf*180) % 360;
    }
    public double DistanceFromCurrentFront(double sup) {
        return basf == 0 ? sup : -sup;
    }
    public double RadiansFromOtherFront(double radians) {
        return radians - (radians/Math.abs(radians))*Math.PI;
    }
    public double ChangeFrontIfNeeded(double radians) {
        if(Math.abs(radians) > Math.PI/2) {
            radians = RadiansFromOtherFront(radians);
            basf ^= 1;
        }
        return radians;
    }
    public double MarkFrontChangeIfNeeded(double radians) {
        if(Math.abs(radians) > Math.PI/2) {
            radians = RadiansFromOtherFront(radians);
            changeFront = true;
        }
        return radians;
    }
    public void MarkChangeFront() {
        changeFront = true;
    }
    public void changeFront() {
        basf ^= 1;
        setAhead(getDistanceRemaining());
        changeFront = false;
    }

    @Override
    public double getHeadingRadians() {
        return RadiansFromCurrentFront(super.getHeadingRadians());
    }
    public double getUniqueFrontHeadingRadians() {
        return super.getHeadingRadians();
    }
    public double getHeading() {
        return DegreesFromCurrentFront(super.getHeadingRadians());
    }
    public void setAhead(double distance) {
        super.setAhead(DistanceFromCurrentFront(distance));
    }
    public void ahead(double distance) {
        super.ahead(DistanceFromCurrentFront(distance));
    }
    public void setBack(double distance) {
        super.setBack(DistanceFromCurrentFront(distance));
    }
    public void back(double distance) {
        super.back(DistanceFromCurrentFront(distance));
    }
    public double getDistanceRemaining() {
        double fromUniqueFront = super.getDistanceRemaining();
        return basf == 1 ? -fromUniqueFront : fromUniqueFront;
    }
    public double getMovementHeadingRadians() {
        double fromCurrentFront = getHeadingRadians();
        return getDistanceRemaining() < 0 ? (fromCurrentFront+Math.PI)%(Math.PI*2) : fromCurrentFront;
    }
    public void onStatus(StatusEvent e) {
        lastStatus = e.getStatus();
    }
    public void execute() {
        if(changeFront) {
            changeFront();
        }
        super.execute();
    }

    // New methods
    public void setBetterTurnRightRadians(double radians, boolean newFront) {
        radians = normalRelativeAngle(radians % (Math.PI*2));
        if(Math.abs(radians) > Math.PI/2) {
            radians = RadiansFromOtherFront(radians);
            if(newFront) changeFront = true;
        }
        super.setTurnRightRadians(radians);
    }
    public void setBetterTurnLeftRadians(double radians, boolean newFront) {
        if(Math.abs(radians) > Math.PI/2) {
            radians = RadiansFromOtherFront(radians);
            if(newFront) changeFront = true;
        }
        super.setTurnLeftRadians(radians);
    }
    public void setBetterTurnTargetRadians(double target, boolean newFront) {
        target %= (Math.PI*2);
        setBetterTurnRightRadians(target - getHeadingRadians(), newFront);
    }
//    public void setBetterTurnPerpendicular(double target) {
//        //out.println(target);
//        setBetterTurnRightRadians(normalRelativeAngle(Math.PI/2 - (getHeadingRadians() - target)));
//    }
}
