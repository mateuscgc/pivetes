package PIVA;
import robocode.*;
import robocode.util.Utils;

import java.awt.Color;
import java.awt.Graphics2D;

import java.awt.geom.*;
import java.util.ArrayList;

public class Pipoca extends TwoFrontsRobot {

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

    double oldEnemyHeading;
    boolean movingForward;
    ArrayList<Double> velocityList;
    ArrayList<Double> turnRateList;
    int nMean;

    //    double distancing = 0;
    double reverseChance = 0.03;


    public void run() {
        // Set colors
        setBodyColor(new Color(188, 75, 200));
        setRadarColor(new Color(31, 212, 132));
        setGunColor(new Color(0, 0, 0));
        setBulletColor(new Color(255, 255, 255));
        setScanColor(new Color(201, 91, 214));

        nMean = 5;
        velocityList = new ArrayList<Double>();
        turnRateList = new ArrayList<Double>();
        for (int i = 0; i < nMean; i++) {
            velocityList.add(5.0);
            turnRateList.add(5.0);
        }

        fHeight = getBattleFieldHeight();
        fWidth = getBattleFieldWidth();

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        tracked = null;

//        double[] WD = { fHeight-getY(), fWidth-getX(), getY(), getX() };
//        double[] WH = { 0, Math.PI/2, Math.PI, 3*Math.PI/2 };
//        int bestw = 0;
//        for(int i = 0; i < WD.length; i++) {
//            if(WD[i] < WD[bestw]) bestw = i;
//        }
//        setBetterTurnTargetRadians(WH[bestw], true);
//        setAhead(Double.POSITIVE_INFINITY);

        while(true) {
            out.println("ALERT!!!");
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
        out.println("Chance "+reverseChance);

        storeScanned(e);

        if(tracked != null && !tracked.equals(e.getName())) {
            if(e.getDistance() / trackedDistance >= 0.5)
                return;
            tracked = e.getName();
        }
        if(tracked == null) tracked = e.getName();


        double absoluteBearing = (getUniqueFrontHeadingRadians() + e.getBearingRadians()) % (Math.PI*2);

        // Turn radar to continue tracking opponent
        double radarTurn = absoluteBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(2.0*Utils.normalRelativeAngle(radarTurn));

        // Musashi trick (circle the opponent) (1v1)
        // Chance to change direction of circle movement
        if(Math.random() < reverseChance) {
            clockwise = !clockwise;
            reverseChance = Math.max(reverseChance-0.01, 0.01);
        } else{
            reverseChance = Math.min(reverseChance+0.002, 0.05);
        }

        if(getOthers() == 1) {
            // Turn robot to be perpendicular to opponent
            double perpendicularHeading = absoluteBearing;
            if(clockwise) perpendicularHeading -= Math.PI/2;
            if(!clockwise) perpendicularHeading += Math.PI/2;
            setBetterTurnTargetRadians(perpendicularHeading, true); // (optimized with two fronts)
        }


        setAhead(Double.POSITIVE_INFINITY);
//        distancing = Math.max(distancing-Math.PI/540, -Math.PI/12); // Tend to get closer to opponent (check out onHitByBullet)


        // Wall Smoothing
        double movimentDirection = getMovementHeadingRadians();
        movimentDirection += getTurnRemainingRadians();
        dX = stick*Math.sin(movimentDirection);
        dY = stick*Math.cos(movimentDirection);
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

        atira(e);

        execute();
    }

    public void onHitByBullet(HitByBulletEvent e) {
//        distancing = Math.min(distancing+Math.PI/30*e.getPower(), Math.PI/6); // If hit, tend to get far from opponent
        clockwise = !clockwise;
        reverseChance = Math.max(reverseChance-0.02, 0.01);
    }
    public void onHitRobot(HitRobotEvent e) {
        clockwise = !clockwise;
        reverseChance = Math.max(reverseChance-0.03, 0.01);
    }
    public void onHitWall(HitWallEvent e) {
        clockwise = !clockwise;
        reverseChance = Math.max(reverseChance-0.03, 0.01);
    }
    public void onRobotDeath(RobotDeathEvent e) {
        if(e.getName() == tracked) {
            tracked = null;
            setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
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
    public void atira(ScannedRobotEvent e) {

        double fire = Math.min(400 / e.getDistance(), 3);
        if (getEnergy() <= 15) fire /= 2;

        pointGun(
                e.getBearingRadians(),
                e.getHeadingRadians(),
                e.getVelocity(),
                Rules.getBulletSpeed(2.0),
                fire,
                e.getDistance()
        );
        if (getEnergy() > fire) setFire(fire);

    }

    private void pointGun(double bearingRadians, double headingRadians, double velocity, double bulletSpeed,double bulletPower, double distance) {
        // double bulletPower = Math.min(3.0,getEnergy());

        // Circular mean
        int i;
        double turnRate;
        velocityList.remove(0);
        velocityList.add(velocity);
        turnRateList.remove(0);
        turnRateList.add(headingRadians - oldEnemyHeading);

        for (velocity = 0, i = 0; i < velocityList.size(); i++) {
            velocity += velocityList.get(i)/velocityList.size();
        }
        for (turnRate = 0, i = 0; i < turnRateList.size(); i++) {
            turnRate += turnRateList.get(i)/turnRateList.size();
        }
        // /Circular mean

        double myX = getX();
        double myY = getY();
        double absoluteBearing = getUniqueFrontHeadingRadians() + bearingRadians;
        double enemyX = getX() + distance * Math.sin(absoluteBearing);
        double enemyY = getY() + distance * Math.cos(absoluteBearing);
        double enemyHeading = headingRadians;
        // double enemyHeadingChange =  turnRate; // headingRadians - oldEnemyHeading;
        double enemyHeadingChange =  headingRadians - oldEnemyHeading;
        double enemyVelocity = velocity;
        oldEnemyHeading = enemyHeading;


        double deltaTime = 0;
        double battleFieldHeight = getBattleFieldHeight(),
                battleFieldWidth = getBattleFieldWidth();
        double predictedX = enemyX, predictedY = enemyY;
        while((++deltaTime) * (20.0 - 3.0 * bulletPower) <
                Point2D.Double.distance(myX, myY, predictedX, predictedY)){
            predictedX += Math.sin(enemyHeading) * enemyVelocity;
            predictedY += Math.cos(enemyHeading) * enemyVelocity;
            enemyHeading += enemyHeadingChange;
            if(	predictedX < 18.0
                    || predictedY < 18.0
                    || predictedX > battleFieldWidth - 18.0
                    || predictedY > battleFieldHeight - 18.0){

                predictedX = Math.min(Math.max(18.0, predictedX),
                        battleFieldWidth - 18.0);
                predictedY = Math.min(Math.max(18.0, predictedY),
                        battleFieldHeight - 18.0);
                break;
            }
        }
        double theta = Utils.normalAbsoluteAngle(Math.atan2(
                predictedX - getX(), predictedY - getY()));
        // if(clockwise)
        setTurnGunRightRadians(Utils.normalRelativeAngle(
                theta - getGunHeadingRadians()));
        // else setTurnGunRightRadians(Utils.normalRelativeAngle(
        //     theta - getGunHeadingRadians()));

    }
}