package PIVA;
import PIVA.TwoFrontsRobot;
import robocode.*;
import robocode.util.Utils;

import java.awt.Color;

import java.awt.geom.*;
import java.util.ArrayList;

public class Cordeiro extends TwoFrontsRobot {

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

    ArrayList <EnemyBot> enemyBotArrayList;
    EnemyBot enemy;

    //    double distancing = 0;
    double reverseChance = 0.03;

    ArrayList<BulletWave> waves = new ArrayList<BulletWave>();

    static double[] guesses = new double[31];

    int clockDirection = 1;
    int best = 0;

    public void run() {
        // Set colors
        setBodyColor(new Color(188, 75, 200));
        setRadarColor(new Color(31, 212, 132));
        setGunColor(new Color(0, 0, 0));
        setBulletColor(new Color(255, 255, 255));
        setScanColor(new Color(201, 91, 214));

        enemyBotArrayList = new ArrayList<EnemyBot>();

        fHeight = getBattleFieldHeight();
        fWidth = getBattleFieldWidth();

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        tracked = null;

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

        int enemyIndex = getEnemy(e.getName());

        if (enemyIndex == -1){
            EnemyBot aux = new EnemyBot();
            aux.update(e);
            //out.println("Found :" + aux.getName());
            enemyBotArrayList.add(aux);
        }
        
        else {
            enemyBotArrayList.get(getEnemy(e.getName())).update(e);
            enemy = enemyBotArrayList.get(getEnemy(e.getName()));
//            out.println("Scanning " + getEnemy(e.getName()) + " :" + e.getName());
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        out.println("TIMEEEE: "+getTime());

        storeScanned(e);

        double absoluteBearing = (getUniqueFrontHeadingRadians() + e.getBearingRadians()) % (Math.PI*2);

        double radarTurn = absoluteBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(2.0*Utils.normalRelativeAngle(radarTurn));


        if(Math.random() < reverseChance) {
            clockwise = !clockwise;
            reverseChance = Math.max(reverseChance-0.01, 0.01);
        } else{
            reverseChance = Math.min(reverseChance+0.001, 0.04);
        }

        double perpendicularHeading = absoluteBearing;

        if(clockwise) perpendicularHeading -= Math.PI/2 - Math.random()*Math.PI/4 + Math.random()*Math.PI/8;
        if(!clockwise) perpendicularHeading += Math.PI/2 - Math.random()*Math.PI/4 + Math.random()*Math.PI/8;
        setBetterTurnTargetRadians(perpendicularHeading, true); // (optimized with two fronts)


        setAhead(Double.POSITIVE_INFINITY);
//        distancing = Math.max(distancing-Math.PI/540, -Math.PI/12); // Tend to get closer to opponent (check out onHitByBullet)


        double movimentDirection = getMovementHeadingRadians();
        movimentDirection += getTurnRemainingRadians();
        dX = stick*Math.sin(movimentDirection);
        dY = stick*Math.cos(movimentDirection);
        double finalX = lastStatus.getX() + dX;
        smoothedTarget = movimentDirection;
        if(finalX >= fWidth-fat) {
            //out.println("Closing up to RIGHT wall");
            vecX = fWidth-fat-getX();
            vecY = stick*Math.cos(Math.asin(vecX/stick));
            if(clockwise)
                vecY *= -1;
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        } else if(finalX <= fat) {
            //out.println("Closing up to LEFT wall");
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
            //out.println("Closing up to UPPER wall");
            vecY = fHeight-fat-getY();
            vecX = stick*Math.sin(Math.acos(vecY/stick));
            if(!clockwise)
                vecX *= -1;
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        } else if(finalY <= fat) {
            //out.println("Closing up to BOTTOM wall");
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


        // Guess Factor
        double enemyX = getX() + Math.sin(absoluteBearing)*e.getDistance();
        double enemyY = getY() + Math.cos(absoluteBearing)*e.getDistance();

        //out.println(waves.size());
        for(int i = 0; i < waves.size(); i++) {
            BulletWave wave = waves.get(i);
            if(wave.checkHit(getTime(), enemyX, enemyY)) {
                //out.println("WAVE HIT");
                waves.remove(wave);
                i--;
            }
        }

        if(e.getVelocity() != 0) {
            clockDirection = (Math.sin(e.getHeadingRadians()-absoluteBearing)*e.getVelocity() < 0 ? -1 : 1);
        }
        //out.println(clockDirection);

        double bulletPower = calculateBulletPower(e.getDistance());

        BulletWave newWave = new BulletWave(getTime(), getX(), getY(), bulletPower, absoluteBearing, clockDirection, guesses);

        // Shoot
        int best = (guesses.length - 1) / 2;
        for(int i = 0; i < guesses.length; i++)
            if(guesses[i] > guesses[best])
                best = i;
        //best = (int)(best);
        if(getRoundNum() > 3){
            best += (int)(Math.random()*6)-3;
        }

        double guessFactor = (double)(best-(guesses.length-1)/2) / ((guesses.length-1)/2);
        double predictedOffSet = newWave.maxEscapeAngle() * guessFactor * clockDirection;
        double gunRightTurn = Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians() + predictedOffSet);
        setTurnGunRightRadians(gunRightTurn);

        if(getGunHeat() == 0 && gunRightTurn < Math.atan2(9, e.getDistance())) {
            out.println(best);
            for(int i = 0; i < guesses.length; i++) {
                out.printf("%.1f ", guesses[i]);
            }
            out.println();
            setFire(bulletPower);
            waves.add(newWave);
//            best++;
//            best %= guesses.length;
        }

        execute();
    }

//    @Override
//    public void onSkippedTurn(SkippedTurnEvent event) {
//        out.println("Deu merda");
//    }

    @Override
	public void onBulletMissed(BulletMissedEvent e) {
		// accuracy = Math.max(accuracy - 0.05, 0);
//        out.println("Errou");
        enemyBotArrayList.get(getEnemy(enemy.getName())).updateAccuracy(false);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(enemy.getName())).getAccuracy());
	}
	@Override
	public void onBulletHit(BulletHitEvent e) {
//        out.println("Acertou");

        enemyBotArrayList.get(getEnemy(enemy.getName())).updateAccuracy(true);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(enemy.getName())).getAccuracy());

	}

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
//        out.println("BulletOnBullet");
        enemyBotArrayList.get(getEnemy(enemy.getName())).updateAccuracy(false);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(enemy.getName())).getAccuracy());
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

    public int getEnemy(String enemy){
        for (int i = 0; i < enemyBotArrayList.size(); i++) {
            if(enemyBotArrayList.get(i).getName() == enemy) return i;
        }
        return -1;
    }

    public double calculateBulletPower(double enemyDistance) {
        if(getTime() < 200) return 0.1;
        if(true) {
            return Math.min(Math.min(400/enemyDistance, 3), getEnergy()-0.1);
        }
        double fire =
                Math.min(
                        //( 400 / enemy.getDistance())*enemy.accuracy*5 + 0.4
                        (fWidth/2) / enemyDistance,3)
                ;
        if (getEnergy() <= 15) {
            fire /= 2;
        }
        if(enemy.getAccuracy() == 0.0 || fire >= getEnergy()) {
            fire = Math.min(0.1, getEnergy()-0.1);
        }
        return fire;
    }

    public void shoot(ScannedRobotEvent e) {

        // double fire = Math.min((400 / e.getDistance()), 3);

//        enemy.update(e);
//        int enemyIndex
//        out.println("Shooting " + getEnemy(e.getName()) + " :" + e.getName());
        enemyBotArrayList.get(getEnemy(e.getName())).update(e);
        enemy = enemyBotArrayList.get(getEnemy(e.getName()));

        double fire = calculateBulletPower(e.getDistance());

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

