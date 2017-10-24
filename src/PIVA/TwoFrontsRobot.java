package PIVA;
import robocode.*;

import java.awt.Color;
import java.awt.Graphics2D;

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
        radians = normalRelativeAngle(radians % (Math.PI*2));
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


    public void onPaint(Graphics2D g) {
        g.setColor(new Color(31, 212, 132));
        g.drawLine(
                (int)getX(), (int)getY(),
                (int)(getX()+100*Math.sin(getHeadingRadians())),
                (int)(getY()+100*Math.cos(getHeadingRadians()))
        );

//        // Set the paint color to a red half transparent color
//        g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
//        // Draw a line from our robot to the scanned robot
//        g.drawLine((int)scannedX, (int)scannedY, (int)getX(), (int)getY());
//        // Draw a filled square on top of the scanned robot that covers it
//        g.fillRect((int)scannedX - 20, (int)scannedY - 20, 40, 40);
//
//        g.setColor(new Color(31, 212, 132));
//        g.drawOval((int)(getX()-stick),(int)(getY()-stick),(int)(stick*2),(int)(stick*2));
//
////        double dX = stick*Math.sin(getHeadingRadians());
////        double dY = stick*Math.cos(getHeadingRadians());
////        double finalX = lastStatus.getX() + dX;
////        double finalY = lastStatus.getY() + dY;
////        g.setColor(new Color(31, 212, 132));
////        g.drawLine((int)finalX, (int)finalY, (int)finalX, (int)finalY);
////        g.setColor(new Color(31, 212, 132, 0x60));
////        g.fillRect((int)finalX - 20, (int)finalY - 20, 40, 40);
////
////        dX = stick*Math.sin(getHeadingRadians()+smoothedTurn);
////        dY = stick*Math.cos(getHeadingRadians()+smoothedTurn);
////        finalX = lastStatus.getX() + dX;
////        finalY = lastStatus.getY() + dY;
////        g.setColor(new Color(51, 212, 0));
////        g.drawLine((int)finalX, (int)finalY, (int)finalX, (int)finalY);
////        g.setColor(new Color(51, 212, 0, 0x60));
////        g.fillRect((int)finalX - 20, (int)finalY - 20, 40, 40);
//
//
//
//        g.drawLine((int)getX(), (int)getY(), (int)(getX()+vecX), (int)(getY()+vecY));
//        g.setColor(new Color(255, 49, 163));
//        g.drawLine((int)getX(), (int)getY(), (int)(getX()+dX), (int)(getY()+dY));
    }
}
