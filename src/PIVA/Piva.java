package PIVA;
import robocode.*;
import robocode.util.Utils;

import java.awt.Color;
import java.awt.Graphics2D;

import java.awt.geom.*;
import java.util.ArrayList;

public class Piva extends TwoFrontsRobot {

    protected String tracked;
    protected double trackedDistance;

    protected double fHeight;
    protected double fWidth;
    protected static final double fat = 25;
    protected static final double stick = 150;

    protected boolean clockwise = true;


    protected double scannedX;
    protected double scannedY;

    protected double smoothedTarget;
    protected double vecX;
    protected double vecY;
    protected double dX;
    protected double dY;

    boolean movingForward;

    double upperDistance = 400;



    double distancing = 0;
    ArrayList <EnemyBot> enemyBotArrayList;

    EnemyBot enemy;


    public void run() {

        // Set colors
         setBodyColor(new Color(255,192,203));
         setGunColor(new Color(255,192,203));
         setRadarColor(new Color(255,192,203));
         setBulletColor(new Color(255,192,203));
         setScanColor(new Color(255,192,203));

        fHeight = getBattleFieldHeight();
        fWidth = getBattleFieldWidth();

        enemyBotArrayList = new ArrayList<EnemyBot>();

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);
       setAdjustRadarForGunTurn(true);
        tracked = null;

        while(true) {
            // out.println("ALERT!!!");




            setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

            execute();
        }
    }

    public void storeScanned(ScannedRobotEvent e) {
        double absoluteBearing = getUniqueFrontHeadingRadians() + e.getBearingRadians();
        scannedX = getX() + Math.sin(absoluteBearing)*e.getDistance();
        scannedY = getY() + Math.cos(absoluteBearing)*e.getDistance();

        int enemyIndex = getEnemy(e.getName());

        if (enemyIndex == -1){
            EnemyBot aux = new EnemyBot();
            aux.update(e);
            out.println("Found :" + aux.getName());
            enemyBotArrayList.add(aux);
        }
        else {
//            out.println("Scanning " + getEnemy(e.getName()) + " :" + e.getName());
        }
    }



    public void onScannedRobot(ScannedRobotEvent e) {

        storeScanned(e);

        double absoluteBearing = (getUniqueFrontHeadingRadians() + e.getBearingRadians()) % (Math.PI*2);

        //

        double radarTurn = absoluteBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(2.0*Utils.normalRelativeAngle(radarTurn));

        if(Math.random() > 0.97) clockwise = !clockwise;

        if(getOthers() == 1){
            double perpendicularHeading = absoluteBearing;
            if(clockwise) perpendicularHeading -= Math.PI/2;
            if(!clockwise) perpendicularHeading += Math.PI/2;
            setBetterTurnTargetRadians(perpendicularHeading, true);
            setBetterTurnTargetRadians(perpendicularHeading, true);
        }
        setAhead(Double.POSITIVE_INFINITY);

        double movimentDirection = getMovementHeadingRadians();
        movimentDirection += getTurnRemainingRadians();
        dX = stick*Math.sin(movimentDirection);
        dY = stick*Math.cos(movimentDirection);

        double finalX = lastStatus.getX() + dX;
        smoothedTarget = movimentDirection;
        if(finalX >= fWidth-fat) {
            // out.println("Closing up to RIGHT wall");
            vecX = fWidth-fat-getX();
            vecY = stick*Math.cos(Math.asin(vecX/stick));
            if(clockwise)
                vecY *= -1;
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        } else if(finalX <= fat) {
            // out.println("Closing up to LEFT wall");
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
            // out.println("Closing up to UPPER wall");
            vecY = fHeight-fat-getY();
            vecX = stick*Math.sin(Math.acos(vecY/stick));
            if(!clockwise)
                vecX *= -1;
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        } else if(finalY <= fat) {
            // out.println("Closing up to BOTTOM wall");
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

        if(e.getDistance() > upperDistance && getOthers() > 1){
            upperDistance+=50;
        	setTurnRadarRightRadians(40);
            // execute();
            // resume();
        }
        upperDistance = 400;

        shoot(e);

        execute();
    }

    @Override
	public void onBulletMissed(BulletMissedEvent e) {
		// accuracy = Math.max(accuracy - 0.05, 0);
        out.println("Errou");
        enemyBotArrayList.get(getEnemy(enemy.getName())).updateAccuracy(false);
        out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(enemy.getName())).getAccuracy());
	}
	@Override
	public void onBulletHit(BulletHitEvent e) {
        out.println("Acertou");

        enemyBotArrayList.get(getEnemy(enemy.getName())).updateAccuracy(true);
        out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(enemy.getName())).getAccuracy());

	}

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
        out.println("BulletOnBullet");
        enemyBotArrayList.get(getEnemy(enemy.getName())).updateAccuracy(false);
        out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(enemy.getName())).getAccuracy());
    }

    public void onHitByBullet(HitByBulletEvent e) {
        distancing = Math.min(distancing+Math.PI/30*e.getPower(), Math.PI/6);
        clockwise = !clockwise;
    }
    public void onHitRobot(HitRobotEvent e) {

        double absoluteBearing = getUniqueFrontHeadingRadians() + e.getBearingRadians();
        setTurnRadarRightRadians(absoluteBearing);
        execute();
        clockwise = !clockwise;
    }
    public void onHitWall(HitWallEvent e) {
        clockwise = !clockwise;
    }


    public void onPaint(Graphics2D g) {

        g.setColor(new Color(0xff, 0x00, 0x00, 0x80));

        g.drawLine((int)scannedX, (int)scannedY, (int)getX(), (int)getY());

        g.fillRect((int)scannedX - 20, (int)scannedY - 20, 40, 40);

        g.setColor(new Color(31, 212, 132));
        g.drawOval((int)(getX()-stick),(int)(getY()-stick),(int)(stick*2),(int)(stick*2));
        g.setColor(new Color(255, 255, 255));
        g.drawRect((int)fat, (int)fat, (int)(fWidth-fat*2), (int)(fHeight-fat*2));

        g.drawLine((int)getX(), (int)getY(), (int)(getX()+vecX), (int)(getY()+vecY));
        g.setColor(new Color(255, 49, 163));
        g.drawLine((int)getX(), (int)getY(), (int)(getX()+dX), (int)(getY()+dY));
    }

    public int getEnemy(String enemy){
        for (int i = 0; i < enemyBotArrayList.size(); i++) {
            if(enemyBotArrayList.get(i).getName() == enemy) return i;
        }
        return -1;
    }

    public void shoot(ScannedRobotEvent e) {

        // double fire = Math.min((400 / e.getDistance()), 3);

//        enemy.update(e);
//        int enemyIndex
        out.println("Shooting " + getEnemy(e.getName()) + " :" + e.getName());
        enemyBotArrayList.get(getEnemy(e.getName())).update(e);
        enemy = enemyBotArrayList.get(getEnemy(e.getName()));

        double fire =
            Math.min(
            //( 400 / enemy.getDistance())*enemy.accuracy*5 + 0.4
            400 / enemy.getDistance(),3)
            ;
        if (getEnergy() <= 15) {
            fire /= 2;

        }
        if(enemy.getAccuracy() == 0.0 ) {
            fire = 0.1;
        }
        if(fire > getEnergy())
            return;

        pointGun(
                enemy.getBearing(),
                enemy.getHeading(),
                enemy.getVelocity(),
                Rules.getBulletSpeed(2.0),
                fire,
                enemy.getDistance(),
                enemy.getTurnRate()
                );
    //     pointGun(
    //             e.getBearingRadians(),
    //             e.getHeadingRadians(),
    //             e.getVelocity(),
    //             Rules.getBulletSpeed(2.0),
    //             fire,
    //             e.getDistance()
    //             );
        if (getEnergy() > fire) setFire(fire);

    }

    private void pointGun(double bearingRadians, double headingRadians, double velocity, double bulletSpeed,double bulletPower, double distance, double turnRate) {

        double myX = getX();
        double myY = getY();

        double absoluteBearing = getUniqueFrontHeadingRadians() + bearingRadians;
        double enemyX = getX() + distance * Math.sin(absoluteBearing);
        double enemyY = getY() + distance * Math.cos(absoluteBearing);
        double enemyHeading = headingRadians;

        double enemyHeadingChange = turnRate;
        double enemyVelocity = velocity;


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

        setTurnGunRightRadians(Utils.normalRelativeAngle(
            theta - getGunHeadingRadians()));



    }


}
