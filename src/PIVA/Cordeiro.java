package PIVA;
import robocode.*;
import robocode.util.Utils;

import java.awt.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Cordeiro extends TwoFrontsRobot {

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
    static final double maxReverseChance = 0.05;
    static final double minReverseChance = 0;
    static final double stepReverseChance = 0.001;
    double reverseChance = 0;

    int clockDirection = 1;
    int best = 0;

    public void run() {
        for (int i = 0; i < enemyBotArrayList.size(); i++) {
            enemyBotArrayList.get(i).rise();
        }
        // Set colors
        setBodyColor(new Color(32, 200, 197));
        setRadarColor(new Color(31, 212, 132));
        setGunColor(new Color(0, 0, 0));
        setBulletColor(new Color(255, 255, 255));
        setScanColor(new Color(201, 91, 214));

        fHeight = getBattleFieldHeight();
        fWidth = getBattleFieldWidth();

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while(true) {
            out.println("### MAIN LOOP ###");
            out.println(clockwise);

            if(getRadarTurnRemainingRadians() == 0){
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            }

//            if(getDistanceRemaining() == 0)
//                setAhead(Double.POSITIVE_INFINITY);

            maybeChangeDirection();

            changeReverseChance(1);

            wallSmoothing();

            setAhead(Double.POSITIVE_INFINITY);

            execute();
        }
    }

    void changeReverseChance(int w) {
        reverseChance = Math.max(minReverseChance, Math.min(maxReverseChance, reverseChance+stepReverseChance*w));
    }

    void setMinReverseChance() {
        reverseChance = minReverseChance;
    }

    public void changeDirection() {
        clockwise = !clockwise;
        double movimentDirection = getMovementHeadingRadians();
        out.println("######################## DIRECTION CHANGED");
        setBetterTurnTargetRadians((movimentDirection+Math.PI)%(Math.PI*2), true);
    }

    public void maybeChangeDirection() {
        if(Math.random() < reverseChance) {
            changeDirection();
            setMinReverseChance();
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

        if(scanned == getOthers()) { // If scanned all robots
            if(getOthers() > 1 && getTime() - scanTurns <= 4) {
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
        double movementDirection = getMovementHeadingRadians();
        //add anti-gravity?
//        movementDirection += antiGravityAngle();
        movementDirection += getTurnRemainingRadians();

        dX = stick*Math.sin(movementDirection);
        dY = stick*Math.cos(movementDirection);
        double finalX = getX() + dX;
        smoothedTarget = movementDirection;
        if(finalX >= fWidth-fat) {
            out.println("Closing up to RIGHT wall");
            vecX = fWidth-fat-getX();
            vecY = stick*Math.cos(Math.asin(vecX/stick));
            out.println(vecY);
            out.println(clockwise);
            if(clockwise)
                vecY *= -1;
            out.println(vecY);
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        } else if(finalX <= fat) {
            out.println("Closing up to LEFT wall");
            vecX = fat-getX();
            vecY = stick*Math.cos(Math.asin(vecX/stick));
            out.println(vecY);
            out.println(clockwise);
            if(!clockwise)
                vecY *= -1;
            out.println(vecY);
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        }

        dX = stick*Math.sin(smoothedTarget);
        dY = stick*Math.cos(smoothedTarget);
        double finalY = getY() + dY;

        if(finalY >= fHeight-fat) {
            out.println("Closing up to UPPER wall");
            vecY = fHeight-fat-getY();
            vecX = stick*Math.sin(Math.acos(vecY/stick));
            out.println(vecX);
            out.println(clockwise);
            if(!clockwise)
                vecX *= -1;
            out.println(vecX);
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        } else if(finalY <= fat) {
            out.println("Closing up to BOTTOM wall");
            vecY = fat-getY();
            vecX = stick*Math.sin(Math.acos(vecY/stick));
            out.println(vecX);
            out.println(clockwise);
            if(clockwise)
                vecX *= -1;
            out.println(vecX);
            double dot = vecX*dX + vecY*dY;
            double cross = vecX*dY - vecY*dX;
            smoothedTarget += Math.atan2(cross, dot);
        }
        if(smoothedTarget != movementDirection) {
            if (target != null && Math.abs(target.absoluteBearing - smoothedTarget) < Math.PI / 6) {
                changeDirection();
                setMinReverseChance();
            } else {
                setBetterTurnTargetRadians(smoothedTarget, false);

            }
        }

    }

    public void lockRadar(double absoluteBearing) {
        double radarTurn = absoluteBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(2.0 * Utils.normalRelativeAngle(radarTurn));
    }

    public void enemyBasedMovement(double absoluteBearing) {
        if (clockwise)
            absoluteBearing -= Math.PI / 2;
        if (!clockwise)
            absoluteBearing += Math.PI / 2;

        setBetterTurnTargetRadians(absoluteBearing, true); // (optimized with two fronts)
    }

    public void onScannedRobot(ScannedRobotEvent e) {

        storeScanned(e);

        if(target == null)
            setNewTarget();

        double absoluteBearing = (getUniqueFrontHeadingRadians() + e.getBearingRadians()) % (Math.PI*2);

        if(getOthers() == 1) {
            lockRadar(absoluteBearing);
            enemyBasedMovement(absoluteBearing);
        }

//        maybeChangeDirection();
//        setAhead(Double.POSITIVE_INFINITY);
//        wallSmoothing();

        // Guess Factor
        double enemyX = getX() + Math.sin(absoluteBearing)*e.getDistance();
        double enemyY = getY() + Math.cos(absoluteBearing)*e.getDistance();

        //out.println(scannedEnemy.waves.size());
        for(int i = 0; i < scannedEnemy.waves.size(); i++) {
            BulletWave wave = scannedEnemy.waves.get(i);
            if(wave.checkHit(getTime(), enemyX, enemyY)) {
                scannedEnemy.waves.remove(wave);
                i--;
            }
        }

        if(e.getVelocity() != 0) {
            clockDirection = (Math.sin(e.getHeadingRadians()-absoluteBearing)*e.getVelocity() < 0 ? -1 : 1);
        }

        double bulletPower = calculateBulletPower(e.getDistance());

        BulletWave newWave = new BulletWave(getTime(), getX(), getY(), bulletPower, absoluteBearing, clockDirection, scannedEnemy.guesses);

        // Shoot
        if(target == scannedEnemy) {
            double guessFactor = (double)(best-(scannedEnemy.guesses.length-1)/2) / ((scannedEnemy.guesses.length-1)/2);
            double predictedOffSet = newWave.maxEscapeAngle() * guessFactor * clockDirection;
            double gunRightTurn = Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians() + predictedOffSet);
            if(getOthers() < 3) {
                setTurnGunRightRadians(gunRightTurn);

                if(getGunHeat() == 0 && gunRightTurn < Math.atan2(9, e.getDistance())) {
                    setFire(bulletPower);
                }
            }
            else shoot(e);

            setNewTarget();
        }

        scannedEnemy.waves.add(newWave);
//        execute();
    }

    @Override
	public void onBulletMissed(BulletMissedEvent e) {
        enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).updateAccuracy(false);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).getAccuracy());
	}
	@Override
	public void onBulletHit(BulletHitEvent e) {
        enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).updateAccuracy(true);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).getAccuracy());

	}

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
        enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).updateAccuracy(false);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).getAccuracy());
    }


    public void onHitByBullet(HitByBulletEvent e) {
        changeReverseChance(5);
    }
    public void onHitRobot(HitRobotEvent e) {
        changeDirection();
        setMinReverseChance();

//        double absoluteBearing = getUniqueFrontHeadingRadians() + e.getBearingRadians();
//        setTurnRadarRightRadians(absoluteBearing);
//        setTurnGunRightRadians(absoluteBearing);
    }
    public void onHitWall(HitWallEvent e) {
        changeDirection();
        setMinReverseChance();
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
//            if(getOthers() > 3)
        return Math.min(Math.min(400/enemyDistance, 3), getEnergy()-0.1);
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

    public void onPaint(Graphics2D g) {
        super.onPaint(g);

        g.setColor(new Color(255, 255, 255));
        g.drawRect((int)fat, (int)fat, (int)(fWidth-fat*2), (int)(fHeight-fat*2));

        g.drawLine((int)getX(), (int)getY(), (int)(getX()+vecX), (int)(getY()+vecY));
        g.setColor(new Color(255, 49, 163));
        g.drawLine((int)getX(), (int)getY(), (int)(getX()+dX), (int)(getY()+dY));
    }
}

