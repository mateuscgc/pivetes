package PIVA;
import robocode.*;
import robocode.util.Utils;

import java.awt.Color;
import java.awt.Graphics2D;

public class Piva extends TwoFrontsRobot {

    protected String tracked;
    protected double trackedDistance;

    protected double fHeight;
    protected double fWidth;
    protected static final double fat = 25;
    protected static final double stick = 150;

    protected boolean clockwise = true;

    // to be painted
    protected double scannedX;
    protected double scannedY;

    protected double smoothedTarget;
    protected double vecX;
    protected double vecY;
    protected double dX;
    protected double dY;

    double distancing = 0;


    public void run() {
        // Set colors
        setBodyColor(new Color(31, 212, 132));
        setGunColor(new Color(188, 75, 200));
        setRadarColor(new Color(242, 108, 35));
        setBulletColor(new Color(255, 255, 255));
        setScanColor(new Color(201, 91, 214));

        fHeight = getBattleFieldHeight();
        fWidth = getBattleFieldWidth();

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);
//        setAdjustRadarForGunTurn(true);
        tracked = null;

        while(true) {
            out.println("ALERT!!!");
            setAdjustRadarForGunTurn(false);
            setTurnGunRightRadians(Double.POSITIVE_INFINITY);
            setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

            execute();
        }
    }

    public void storeScanned(ScannedRobotEvent e) {
        double absoluteBearing = getUniqueFrontHeadingRadians() + e.getBearingRadians();
        scannedX = getX() + Math.sin(absoluteBearing)*e.getDistance();
        scannedY = getY() + Math.cos(absoluteBearing)*e.getDistance();
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        storeScanned(e);

        if(tracked != null && !tracked.equals(e.getName())) {
            if(e.getDistance() / trackedDistance >= 0.5)
                return;
            tracked = e.getName();
        }
        if(tracked == null) tracked = e.getName();

        setAdjustRadarForGunTurn(true);

        double absoluteBearing = (getUniqueFrontHeadingRadians() + e.getBearingRadians()) % (Math.PI*2);

        // Turn radar to continue tracking opponent
        double radarTurn = absoluteBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(2.0*Utils.normalRelativeAngle(radarTurn));

        // Musashi trick (circle the opponent) (1v1)
        // Chance to change direction of circle movement
        if(Math.random() > 0.97) clockwise = !clockwise;
        // Turn robot to be perpendicular to opponent
        double perpendicularHeading = absoluteBearing;
        out.println("Distancing: "+distancing);
        if(clockwise) perpendicularHeading -= Math.PI/2;
        if(!clockwise) perpendicularHeading += Math.PI/2;
        setBetterTurnTargetRadians(perpendicularHeading, true); // (optimized with two fronts)
        setAhead(Double.POSITIVE_INFINITY);
        distancing = Math.max(distancing-Math.PI/540, -Math.PI/12); // Tend to get closer to opponent (check out onHitByBullet)


        // Wall Smoothing
        double movimentDirection = getMovementHeadingRadians();
        movimentDirection += getTurnRemainingRadians();
        dX = stick*Math.sin(movimentDirection);
        dY = stick*Math.cos(movimentDirection);
        out.println(perpendicularHeading + "   " + movimentDirection);
        double finalX = lastStatus.getX() + dX;
        smoothedTarget = movimentDirection;
        if(finalX >= fWidth-fat) {
            out.println("Closing up to RIGHT wall");
            vecX = fWidth-fat-getX();
            vecY = stick*Math.cos(Math.asin(vecX/stick));
            if(clockwise)
                vecY *= -1;
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        } else if(finalX <= fat) {
            out.println("Closing up to LEFT wall");
            vecX = fat-getX();
            vecY = stick*Math.cos(Math.asin(vecX/stick));
            if(!clockwise)
                vecY *= -1;
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        }

        dX = stick*Math.sin(smoothedTarget);
        dY = stick*Math.cos(smoothedTarget);
        double finalY = lastStatus.getY() + dY;

        if(finalY >= fHeight-fat) {
            out.println("Closing up to UPPER wall");
            vecY = fHeight-fat-getY();
            vecX = stick*Math.sin(Math.acos(vecY/stick));
            if(!clockwise)
                vecX *= -1;
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        } else if(finalY <= fat) {
            out.println("Closing up to BOTTOM wall");
            vecY = fat-getY();
            vecX = stick*Math.sin(Math.acos(vecY/stick));
            if(clockwise)
                vecX *= -1;
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        }
        if(Math.abs(absoluteBearing - smoothedTarget) >= Math.PI/3)
            setBetterTurnTargetRadians(smoothedTarget, true);
        else {
            clockwise = !clockwise;
        }

        trackedDistance = e.getDistance();
        execute();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        distancing = Math.min(distancing+Math.PI/30*e.getPower(), Math.PI/6); // If hit, tend to get far from opponent
        clockwise = !clockwise;
    }
    public void onHitRobot(HitRobotEvent e) {
        clockwise = !clockwise;
    }
    public void onHitWall(HitWallEvent e) {
        clockwise = !clockwise;
    }


    public void onPaint(Graphics2D g) {
        // Set the paint color to a red half transparent color
        g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
        // Draw a line from our robot to the scanned robot
        g.drawLine((int)scannedX, (int)scannedY, (int)getX(), (int)getY());
        // Draw a filled square on top of the scanned robot that covers it
        g.fillRect((int)scannedX - 20, (int)scannedY - 20, 40, 40);

        g.setColor(new Color(31, 212, 132));
        g.drawOval((int)(getX()-stick),(int)(getY()-stick),(int)(stick*2),(int)(stick*2));

//        double dX = stick*Math.sin(getHeadingRadians());
//        double dY = stick*Math.cos(getHeadingRadians());
//        double finalX = lastStatus.getX() + dX;
//        double finalY = lastStatus.getY() + dY;
//        g.setColor(new Color(31, 212, 132));
//        g.drawLine((int)finalX, (int)finalY, (int)finalX, (int)finalY);
//        g.setColor(new Color(31, 212, 132, 0x60));
//        g.fillRect((int)finalX - 20, (int)finalY - 20, 40, 40);
//
//        dX = stick*Math.sin(getHeadingRadians()+smoothedTurn);
//        dY = stick*Math.cos(getHeadingRadians()+smoothedTurn);
//        finalX = lastStatus.getX() + dX;
//        finalY = lastStatus.getY() + dY;
//        g.setColor(new Color(51, 212, 0));
//        g.drawLine((int)finalX, (int)finalY, (int)finalX, (int)finalY);
//        g.setColor(new Color(51, 212, 0, 0x60));
//        g.fillRect((int)finalX - 20, (int)finalY - 20, 40, 40);

        g.setColor(new Color(255, 255, 255));
        g.drawRect((int)fat, (int)fat, (int)(fWidth-fat*2), (int)(fHeight-fat*2));

        g.drawLine((int)getX(), (int)getY(), (int)(getX()+vecX), (int)(getY()+vecY));
        g.setColor(new Color(255, 49, 163));
        g.drawLine((int)getX(), (int)getY(), (int)(getX()+dX), (int)(getY()+dY));
    }
}
