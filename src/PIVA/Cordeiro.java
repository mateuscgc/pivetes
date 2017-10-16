package PIVA;
import robocode.*;
import robocode.util.Utils;

import java.awt.Color;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Cordeiro extends TwoFrontsRobot {

    protected String tracked;

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

    int scanned = 0;
    long scanTurns = 0;

    static ArrayList <EnemyBot> enemyBotArrayList = new ArrayList<EnemyBot>();
    EnemyBot scannedEnemy;
    EnemyBot target = null;

    //    double distancing = 0;
    double reverseChance = 0.03;

    int clockDirection = 1;
    int best = 0;

    public void run() {
        for (int i = 0; i < enemyBotArrayList.size(); i++) {
            enemyBotArrayList.get(i).rise();
        }
        // Set colors
        setBodyColor(new Color(188, 75, 200));
        setRadarColor(new Color(31, 212, 132));
        setGunColor(new Color(0, 0, 0));
        setBulletColor(new Color(255, 255, 255));
        setScanColor(new Color(201, 91, 214));

        //enemyBotArrayList = new ArrayList<EnemyBot>();

        fHeight = getBattleFieldHeight();
        fWidth = getBattleFieldWidth();

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        tracked = null;

        while(true) {
            out.println("ALERT!!!");

            if(getRadarTurnRemainingRadians() == 0){
                out.println("getRadarTurnRemainingRadians() == 0");
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            }

            if(getDistanceRemaining() == 0)
                setAhead(Double.POSITIVE_INFINITY);

            maybeChangeDirection();
            wallSmoothing();

            execute();
        }
    }

    double antiGravityAngle(){
        double xForce=0, yForce= 0;
        for(int i=0;i<enemyBotArrayList.size();i++){
            if(enemyBotArrayList.get(i).alive){
                double absBearing=enemyBotArrayList.get(i).absoluteBearing;
                double distance=enemyBotArrayList.get(i).distance;
                xForce -= Math.sin(absBearing) / (distance * distance);
                yForce -= Math.cos(absBearing) / (distance * distance);
            }
        }
        return Math.atan2(xForce, yForce)/4;
    }

    public void storeScanned(ScannedRobotEvent e) {
        double absoluteBearing = getUniqueFrontHeadingRadians() + e.getBearingRadians();
        scannedX = getX() + Math.sin(absoluteBearing)*e.getDistance();
        scannedY = getY() + Math.cos(absoluteBearing)*e.getDistance();

        int enemyIndex = getEnemy(e.getName());

        if (enemyIndex == -1){
            EnemyBot aux = new EnemyBot();
            aux.update(e,absoluteBearing);
            out.println("Found :" + aux.getName());

            enemyBotArrayList.add(aux);
            scannedEnemy = aux;
        }
        else {
            scannedEnemy = enemyBotArrayList.get(enemyIndex);
            scannedEnemy.update(e,absoluteBearing);

//            out.println("Scanning " + getEnemy(e.getName()) + " :" + e.getName());
        }

        if(!scannedEnemy.scanned) {
            scannedEnemy.scanned = true;
            scanned++;
        }

        out.println("SCANNED "+scanned);

        if(scanned == getOthers()) { // If scanned all robots
//        if(scanned == 3 || scanned == getOthers()) { // If scanned all robots
            out.println("AAAAAAAAAAAAAAAAAAAAHHHHHHHHHHHHHHH");
            out.println(getTime()+" "+scanTurns);
            if(getOthers() > 1 && getTime() - scanTurns <= 4) {
                out.println("getOthers() > 1 && getTime() - scanTurns <= 4");
                setTurnRadarRightRadians(getRadarTurnRemainingRadians() > 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
            }

            scanTurns = getTime();

            scanned = 1;
            for (int i = 0; i < enemyBotArrayList.size(); i++) {
                if(enemyBotArrayList.get(i) != scannedEnemy)
                    enemyBotArrayList.get(i).scanned = false;
            }
        }
    }

    public void setNewTarget() {
        target = null;
        for (int i = 0; i < enemyBotArrayList.size(); i++) {
            EnemyBot enem = enemyBotArrayList.get(i);
            if (enem.alive && (target == null || enem.distance < target.distance)) {
                target = enem;
            }
        }

        best = (target.guesses.length - 1) / 2;
        for(int i = 0; i < target.guesses.length; i++)
            if(target.guesses[i] > target.guesses[best])
                best = i;
    }

    public void wallSmoothing() {
        double movimentDirection = getMovementHeadingRadians();
        //add anti-gravity?
//        movimentDirection += antiGravityAngle();
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
    }

    public void changeDirection() {
        clockwise = !clockwise;
        double movimentDirection = getMovementHeadingRadians();
        out.println("ChangeDirection()!!");
        setBetterTurnTargetRadians((movimentDirection+Math.PI)%(Math.PI*2), true);
    }

    public void maybeChangeDirection() {
        if(Math.random() < reverseChance) {
            out.println("MUDDDOUUUU");
            changeDirection();
            reverseChance = Math.max(reverseChance-0.08, 0.01);
        } else{
            reverseChance = Math.min(reverseChance+0.001, 0.04);
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        //out.println("TIMEEEE: "+getTime());

        storeScanned(e);

        if(target == null)
            setNewTarget();

        double absoluteBearing = (getUniqueFrontHeadingRadians() + e.getBearingRadians()) % (Math.PI*2);

        if(getOthers() == 1) {
//            double virada = Utils.normalRelativeAngle(
//                    absoluteBearing - getRadarHeadingRadians()
//            );
//            double extraTurn = Math.min(
//                    Math.atan(36.0/e.getDistance()),
//                    Rules.RADAR_TURN_RATE_RADIANS
//            );
//            virada += (virada< 0 ? -extraTurn : extraTurn);
//            setTurnRadarRightRadians(virada);

            double radarTurn = absoluteBearing - getRadarHeadingRadians();
            setTurnRadarRightRadians(2.0 * Utils.normalRelativeAngle(radarTurn));

            double perpendicularHeading = absoluteBearing;

            if (clockwise)
                perpendicularHeading -= Math.PI / 2 ;
            if (!clockwise)
                perpendicularHeading += Math.PI / 2 ;;

            setBetterTurnTargetRadians(perpendicularHeading, false); // (optimized with two fronts)
        }

        maybeChangeDirection();
        setAhead(Double.POSITIVE_INFINITY);
        wallSmoothing();

        if (Math.abs(absoluteBearing - smoothedTarget) >= Math.PI / 3)
            setBetterTurnTargetRadians(smoothedTarget, true);
        else {
            changeDirection();
        }

//        setAhead(Double.POSITIVE_INFINITY);
//        distancing = Math.max(distancing-Math.PI/540, -Math.PI/12); // Tend to get closer to opponent (check out onHitByBullet)


        // Guess Factor
        double enemyX = getX() + Math.sin(absoluteBearing)*e.getDistance();
        double enemyY = getY() + Math.cos(absoluteBearing)*e.getDistance();

        //out.println(scannedEnemy.waves.size());
        for(int i = 0; i < scannedEnemy.waves.size(); i++) {
            BulletWave wave = scannedEnemy.waves.get(i);
            if(wave.checkHit(getTime(), enemyX, enemyY)) {
                //out.println("WAVE HIT");
                scannedEnemy.waves.remove(wave);
                i--;
            }
        }

        if(e.getVelocity() != 0) {
            clockDirection = (Math.sin(e.getHeadingRadians()-absoluteBearing)*e.getVelocity() < 0 ? -1 : 1);
        }
        //out.println(clockDirection);

        double bulletPower = calculateBulletPower(e.getDistance());

        BulletWave newWave = new BulletWave(getTime(), getX(), getY(), bulletPower, absoluteBearing, clockDirection, scannedEnemy.guesses);

        // Shoot
        if(target == scannedEnemy) {
            double guessFactor = (double)(best-(scannedEnemy.guesses.length-1)/2) / ((scannedEnemy.guesses.length-1)/2);
            double predictedOffSet = newWave.maxEscapeAngle() * guessFactor * clockDirection;
            double gunRightTurn = Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians() + predictedOffSet);
            if(getOthers() < 3) {
                setTurnGunRightRadians(gunRightTurn);

//          if(getRoundNum() > 3){
    //          best += (int)(Math.random()*6)-3;
    //      }

                if(getGunHeat() == 0 && gunRightTurn < Math.atan2(9, e.getDistance())) {
                    out.println(best);
                //for(int i = 0; i < scannedEnemy.guesses.length; i++) {
                //    out.printf("%.1f ", scannedEnemy.guesses[i]);
                //}
                    out.println("Time: " +getTime());
                    setFire(bulletPower);
                }


            }
            else shoot(e);

            setNewTarget();
        }

        scannedEnemy.waves.add(newWave);

        out.println(basf);
        execute();
    }

    @Override
	public void onBulletMissed(BulletMissedEvent e) {
		// accuracy = Math.max(accuracy - 0.05, 0);
//        out.println("Errou");
        enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).updateAccuracy(false);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).getAccuracy());
	}
	@Override
	public void onBulletHit(BulletHitEvent e) {
//        out.println("Acertou");

        enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).updateAccuracy(true);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).getAccuracy());

	}

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
//        out.println("BulletOnBullet");
        enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).updateAccuracy(false);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).getAccuracy());
    }


    public void onHitByBullet(HitByBulletEvent e) {
//        distancing = Math.min(distancing+Math.PI/30*e.getPower(), Math.PI/6); // If hit, tend to get far from opponent
        if(getOthers() == 1){
            changeDirection();
            reverseChance = Math.max(reverseChance-0.02, 0.01);
        } else reverseChance = Math.max(reverseChance+0.02, 0.01);

    }
    public void onHitRobot(HitRobotEvent e) {
        changeDirection();
        reverseChance = Math.max(reverseChance-0.03, 0.01);
        double absoluteBearing = getUniqueFrontHeadingRadians() + e.getBearingRadians();
        setTurnRadarRightRadians(absoluteBearing);
        setTurnGunRightRadians(absoluteBearing);

        execute();
    }
    public void onHitWall(HitWallEvent e) {
        changeDirection();
        reverseChance = Math.max(reverseChance-0.03, 0.01);
    }
    public void onRobotDeath(RobotDeathEvent e) {
        EnemyBot dead = enemyBotArrayList.get(getEnemy(e.getName()));
        dead.alive = false;
        if(dead.scanned) {
            dead.scanned = false;
            scanned--;
        }
        if(target == dead)
            setNewTarget();
    }

    public int getEnemy(String enemy){
        for (int i = 0; i < enemyBotArrayList.size(); i++) {
            if(enemyBotArrayList.get(i).getName() == enemy) return i;
        }
        return -1;
    }

    public double calculateBulletPower(double enemyDistance) {
        //if(getTime() < 200) return 0.1;
        if(true) {
//            if(getOthers() > 3)
            return Math.min(Math.min(400/enemyDistance, 3), getEnergy()-0.1);
        }
        double fire =
                Math.min(
                        //( 400 / scannedEnemy.getDistance())*scannedEnemy.accuracy*5 + 0.4
                        (fWidth/2) / enemyDistance,3)
                ;
        if (getEnergy() <= 15) {
            fire /= 2;
        }
        if(scannedEnemy.getAccuracy() == 0.0 || fire >= getEnergy()) {
            fire = Math.min(0.1, getEnergy()-0.1);
        }
        return fire;
    }
    public void shoot(ScannedRobotEvent e) {

        // double fire = Math.min((400 / e.getDistance()), 3);

//        enemy.update(e);
//        int enemyIndex
        double absoluteBearing = getUniqueFrontHeadingRadians() + e.getBearingRadians();
        out.println("Shooting " + getEnemy(e.getName()) + " :" + e.getName());
//        enemyBotArrayList.get(getEnemy(e.getName())).update(e,absoluteBearing);
//        enemy = enemyBotArrayList.get(getEnemy(e.getName()));


        double fire =
                Math.min(
                        //( 400 / enemy.getDistance())*enemy.accuracy*5 + 0.4
                        400 / scannedEnemy.getDistance(),3)
                ;
        if (getTime() > 50 && getTime() < 600 && scannedEnemy.getAccuracy() != 0.0){
            fire *= 1.3;
        }
        if (getEnergy() <= 15) {
            fire /= 2;
        }
//        if(scannedEnemy.getAccuracy() == 0.0 ) {
//            fire = 0.1;
//        }

        if(fire >= getEnergy())
            return;

        pointGun(
                scannedEnemy.getBearing(),
                scannedEnemy.getHeading(),
                scannedEnemy.getVelocity(),
                Rules.getBulletSpeed(2.0),
                fire,
                scannedEnemy.getDistance(),
                scannedEnemy.getTurnRate()
        );
        //     pointGun(
        //             e.getBearingRadians(),
        //             e.getHeadingRadians(),
        //             e.getVelocity(),
        //             Rules.getBulletSpeed(2.0),
        //             fire,
        //             e.getDistance()
        //             );
        if (getGunHeat() != 0) return;
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
        double gunRightTurn = Utils.normalRelativeAngle(
                theta - getGunHeadingRadians());
        setTurnGunRightRadians(gunRightTurn);


    }

}

