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

//    protected boolean clockwise = true;

    ArrayList<Predictor.Predicted> lastCandidates = new ArrayList<Predictor.Predicted>();

    ArrayList<BulletWave> waves = new ArrayList<BulletWave>();
    ArrayList<BulletWave> surfWaves = new ArrayList<BulletWave>();
    static double[] guesses = new double[31];
    static double[] surfGuesses = new double[31];

    // to be painted
    protected double scannedX;
    protected double scannedY;

    protected double smoothedTarget;
    protected double vecX;
    protected double vecY;
    protected double dX;
    protected double dY;

    int merda = 0;

    int scanned = 0;
    long scanTurns = 0;

    static ArrayList <EnemyInfo> enemy = new ArrayList<EnemyInfo>();

    EnemyBot scannedEnemy;
//    EnemyBot target = null;

    //    double distancing = 0;
    static final double maxReverseChance = 0.05;
    static final double minReverseChance = 0;
    static final double stepReverseChance = 0.001;
    double reverseChance = 0;

//    int clockDirection = 1;
    int best = 0;

    public void run() {
//        for (int i = 0; i < enemyBotArrayList.size(); i++) {
//            enemyBotArrayList.get(i).rise();
//        }
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
//            out.println(clockwise);

            if(getRadarTurnRemainingRadians() == 0){
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            }

//            if(getDistanceRemaining() == 0)
//                setAhead(Double.POSITIVE_INFINITY);

            //maybeChangeDirection();

            //changeReverseChance(1);

            surf();

//            wallSmoothing();

            //setAhead(Double.POSITIVE_INFINITY);

            execute();
        }
    }

    public int getClockDirection(double velocity, double heading, double absoluteBearing) {
        int clockDirection = 0;
        if(velocity != 0) {
            clockDirection = (Math.sin(heading-absoluteBearing) * velocity < 0 ? -1 : 1);
        }

        return clockDirection;
    }


    public void surf() {
        Predictor.Predicted cur = new Predictor.Predicted();
        cur.x = getX();
        cur.y = getY();
        cur.velocity = getVelocity();
        cur.heading = getUniqueFrontHeadingRadians();
        cur.time = getTime();

        lastCandidates.clear();

        BulletWave closest = null;
        for(int i = 0; i < surfWaves.size(); i++) {
            BulletWave wave = surfWaves.get(i);
            if(!wave.hasHit(getTime(), getX(), getY())) {
                if(closest == null || wave.timeToImpact(getX(), getY(), getTime()) < closest.timeToImpact(getX(), getY(), getTime())) {
                    closest = wave;
                }
            } else if(wave.curRadius(getTime()) > 1000) {
                surfWaves.remove(wave);
                i--;
            }
        }

        if(closest == null) return;

        double absoluteBearing = Math.atan2(getX() - closest.fireX, getY() - closest.fireY);

        double cwHeading = Utils.normalAbsoluteAngle(absoluteBearing+Math.PI/2);
        double ccwHeading = Utils.normalAbsoluteAngle(absoluteBearing-Math.PI/2);

        Predictor predictor = new Predictor(getBattleFieldWidth(), getBattleFieldHeight());

        Predictor.Predicted cw = predictor.surf(cur, closest, cwHeading, Double.POSITIVE_INFINITY, true);
        Predictor.Predicted ccw = predictor.surf(cur, closest, ccwHeading, Double.POSITIVE_INFINITY, false);
        Predictor.Predicted brake = predictor.surf(cur, closest, ccwHeading, 0,
                getClockDirection(cur.velocity, cur.heading, absoluteBearing) == +1);

        lastCandidates.add(cw);
        lastCandidates.add(ccw);
        lastCandidates.add(brake);

        double distance = Point2D.distance(getX(), getY(), closest.fireX, closest.fireY);

        double[] smoothedGuesses = closest.getSmoothedGuesses(distance);

        double dangerCw = evaluateDanger(closest, cw, smoothedGuesses);
        double dangerCcw = evaluateDanger(closest, ccw, smoothedGuesses);
        double dangerBrake = evaluateDanger(closest, brake, smoothedGuesses);

        if(dangerCw < dangerCcw && dangerCw < dangerBrake) {
            setBetterTurnTargetRadians(predictor.wallSmoothing(cur, cwHeading, true), true);
            setMaxVelocity(8);
        } else if(dangerCcw < dangerBrake){
            setBetterTurnTargetRadians(predictor.wallSmoothing(cur, ccwHeading, false), true);
            setMaxVelocity(8);
        }  else {
            boolean clockwise = getClockDirection(cur.velocity, cur.heading, absoluteBearing) == +1;
            setBetterTurnTargetRadians(predictor.wallSmoothing(cur, ccwHeading, clockwise), true);
            setMaxVelocity(0);
        }

        setAhead(Double.POSITIVE_INFINITY);

    }

    public double evaluateDanger(BulletWave wave, Predictor.Predicted impact, double[] guesses) {
        double correctDirection = Math.atan2(impact.x - wave.fireX, impact.y - wave.fireY);
        double movementOffSet = Utils.normalRelativeAngle((correctDirection - wave.fireAbsoluteBearing));
        double guessFactor = Math.max(-1, Math.min(1, movementOffSet / wave.maxEscapeAngle())) * wave.clockDirection;
        int index = (int)Math.round((guesses.length - 1) / 2 * (guessFactor + 1));

        if(index < 0 || index >= guesses.length)
            return Double.POSITIVE_INFINITY;

        return guesses[index];
    }


    void changeReverseChance(int w) {
        reverseChance = Math.max(minReverseChance, Math.min(maxReverseChance, reverseChance+stepReverseChance*w));
    }

    void setMinReverseChance() {
        reverseChance = minReverseChance;
    }

//    public void changeDirection() {
//        clockwise = !clockwise;
//        double movimentDirection = getMovementHeadingRadians();
//        out.println("######################## DIRECTION CHANGED");
//        setBetterTurnTargetRadians((movimentDirection+Math.PI)%(Math.PI*2), true);
//    }

//    public void maybeChangeDirection() {
//        if(Math.random() < reverseChance) {
//            changeDirection();
//            setMinReverseChance();
//        }
//    }

//    double antiGravityAngle(){
//        double xForce=0, yForce= 0;
//        for(int i=0;i<enemyBotArrayList.size();i++){
//            if(enemyBotArrayList.get(i).alive){
//                double absBearing=enemyBotArrayList.get(i).absoluteBearing;
//                double distance=enemyBotArrayList.get(i).distance;
//                xForce -= Math.sin(absBearing) / (distance * distance);
//                yForce -= Math.cos(absBearing) / (distance * distance);
//            }
//        }
//        return Math.atan2(xForce, yForce)/4;
//    }

//    public void storeScanned(ScannedRobotEvent e) {
//        double absoluteBearing = getUniqueFrontHeadingRadians() + e.getBearingRadians();
//        scannedX = getX() + Math.sin(absoluteBearing)*e.getDistance();
//        scannedY = getY() + Math.cos(absoluteBearing)*e.getDistance();
//
//        int enemyIndex = getEnemy(e.getName());
//
//        if (enemyIndex == -1){
//            EnemyBot aux = new EnemyBot();
//            aux.update(e,absoluteBearing);
//            out.println("Found :" + aux.getName());
//
//            enemyBotArrayList.add(aux);
//            scannedEnemy = aux;
//        }
//        else {
//            scannedEnemy = enemyBotArrayList.get(enemyIndex);
//            scannedEnemy.update(e,absoluteBearing);
//
////            out.println("Scanning " + getEnemy(e.getName()) + " :" + e.getName());
//        }
//
//        if(!scannedEnemy.scanned) {
//            scannedEnemy.scanned = true;
//            scanned++;
//        }
//
//        if(scanned == getOthers()) { // If scanned all robots
//            if(getOthers() > 1 && getTime() - scanTurns <= 4) {
//                setTurnRadarRightRadians(getRadarTurnRemainingRadians() > 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
//            }
//
//            scanTurns = getTime();
//
//            scanned = 1;
//            for (int i = 0; i < enemyBotArrayList.size(); i++) {
//                if(enemyBotArrayList.get(i) != scannedEnemy)
//                    enemyBotArrayList.get(i).scanned = false;
//            }
//        }
//    }

//    public void setNewTarget() {
//        target = null;
//        for (int i = 0; i < enemyBotArrayList.size(); i++) {
//            EnemyBot enem = enemyBotArrayList.get(i);
//            if (enem.alive && (target == null || enem.distance < target.distance)) {
//                target = enem;
//            }
//        }
//
//        best = (target.guesses.length - 1) / 2;
//        for(int i = 0; i < target.guesses.length; i++)
//            if(target.guesses[i] > target.guesses[best])
//                best = i;
//    }

//    public void wallSmoothing() {
//        double movementDirection = getMovementHeadingRadians();
//        //add anti-gravity?
////        movementDirection += antiGravityAngle();
//        movementDirection += getTurnRemainingRadians();
//
//        dX = stick*Math.sin(movementDirection);
//        dY = stick*Math.cos(movementDirection);
//        double finalX = getX() + dX;
//        smoothedTarget = movementDirection;
//        if(finalX >= fWidth-fat) {
//            out.println("Closing up to RIGHT wall");
//            vecX = fWidth-fat-getX();
//            vecY = stick*Math.cos(Math.asin(vecX/stick));
//            out.println(vecY);
//            out.println(clockwise);
//            if(clockwise)
//                vecY *= -1;
//            out.println(vecY);
//            double dot = vecX*dX + vecY*dY;
//            double cross = vecX*dY - vecY*dX;
//            smoothedTarget += Math.atan2(cross, dot);
//        } else if(finalX <= fat) {
//            out.println("Closing up to LEFT wall");
//            vecX = fat-getX();
//            vecY = stick*Math.cos(Math.asin(vecX/stick));
//            out.println(vecY);
//            out.println(clockwise);
//            if(!clockwise)
//                vecY *= -1;
//            out.println(vecY);
//            double dot = vecX*dX + vecY*dY;
//            double cross = vecX*dY - vecY*dX;
//            smoothedTarget += Math.atan2(cross, dot);
//        }
//
//        dX = stick*Math.sin(smoothedTarget);
//        dY = stick*Math.cos(smoothedTarget);
//        double finalY = getY() + dY;
//
//        if(finalY >= fHeight-fat) {
//            out.println("Closing up to UPPER wall");
//            vecY = fHeight-fat-getY();
//            vecX = stick*Math.sin(Math.acos(vecY/stick));
//            out.println(vecX);
//            out.println(clockwise);
//            if(!clockwise)
//                vecX *= -1;
//            out.println(vecX);
//            double dot = vecX*dX + vecY*dY;
//            double cross = vecX*dY - vecY*dX;
//            smoothedTarget += Math.atan2(cross, dot);
//        } else if(finalY <= fat) {
//            out.println("Closing up to BOTTOM wall");
//            vecY = fat-getY();
//            vecX = stick*Math.sin(Math.acos(vecY/stick));
//            out.println(vecX);
//            out.println(clockwise);
//            if(clockwise)
//                vecX *= -1;
//            out.println(vecX);
//            double dot = vecX*dX + vecY*dY;
//            double cross = vecX*dY - vecY*dX;
//            smoothedTarget += Math.atan2(cross, dot);
//        }
//        if(smoothedTarget != movementDirection) {
////            EnemyInfo cur = null;
////            if(!enemy.isEmpty()) cur = enemy.get(enemy.size()-1);
////            if (cur != null && Math.abs(target.absoluteBearing - smoothedTarget) < Math.PI / 6) {
////                changeDirection();
////                setMinReverseChance();
////            } else {
//                setBetterTurnTargetRadians(smoothedTarget, false);
//
////            }
//        }
//
//    }

    public void lockRadar(double absoluteBearing) {
        double radarTurn = absoluteBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(2.0 * Utils.normalRelativeAngle(radarTurn));
    }

//    public surf(){
//        BulletWave closestWave = null;
//        for (int i = 0; i < surfWaves.get(i); i++) {
//            if(closestWave.curRadius() > surfWaves.get(i) || closestWave == null) {
//                closestWave = surfWaves.get(i);
//            }
//
//        }
//
//
//    }
//
//    public void enemyBasedMovement(double absoluteBearing) {
//        if (clockwise)
//            absoluteBearing -= Math.PI / 2;
//        if (!clockwise)
//            absoluteBearing += Math.PI / 2;
//
//        setBetterTurnTargetRadians(absoluteBearing, true); // (optimized with two fronts)
//    }

    public void onScannedRobot(ScannedRobotEvent e) {
        boolean ehTiroZe = !enemy.isEmpty() ? enemy.get(enemy.size()-1).checkShot(e) : false;
        double enemyBulletPower = !enemy.isEmpty() ? enemy.get(enemy.size()-1).bulletPower(e) : 1;
        double lastEnemyX = !enemy.isEmpty() ? enemy.get(enemy.size()-1).x : 0;
        double lastEnemyY = !enemy.isEmpty() ? enemy.get(enemy.size()-1).y : 0;

//        storeScanned(e);
        enemy.add(EnemyInfo.newEnemy(e, this));

//        if(target == null)
//            setNewTarget();

        double absoluteBearing = (getUniqueFrontHeadingRadians() + e.getBearingRadians()) % (Math.PI*2);

        if(getOthers() == 1) {
            lockRadar(absoluteBearing);
//            enemyBasedMovement(absoluteBearing);
        }

        // Guess Factor
        double enemyX = getX() + Math.sin(absoluteBearing)*e.getDistance();
        double enemyY = getY() + Math.cos(absoluteBearing)*e.getDistance();

        //out.println(scannedEnemy.waves.size());
        for(int i = 0; i < waves.size(); i++) {
            BulletWave wave = waves.get(i);
            if(wave.checkHit(getTime(), enemyX, enemyY)) {
                waves.remove(wave);
                i--;
            }
        }

        int clockDirection = 0;
        if(e.getVelocity() != 0) {
            clockDirection = (Math.sin(e.getHeadingRadians()-absoluteBearing)*e.getVelocity() < 0 ? -1 : 1);
        }

        double bulletPower = calculateBulletPower(e.getDistance());

        BulletWave newWave = new BulletWave(getTime(), getX(), getY(), bulletPower, absoluteBearing, clockDirection, guesses);

        best = (guesses.length - 1) / 2;
        for(int i = 0; i < guesses.length; i++)
            if(guesses[i] > guesses[best])
                best = i;


        // Shoot
//        if(target == scannedEnemy) {
            double guessFactor = (double)(best-(guesses.length-1)/2) / ((guesses.length-1)/2);
            double predictedOffSet = newWave.maxEscapeAngle() * guessFactor * clockDirection;
            double gunRightTurn = Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians() + predictedOffSet);
            if(getOthers() < 3) {
                setTurnGunRightRadians(gunRightTurn);

                if(getGunHeat() == 0 && gunRightTurn < Math.atan2(9, e.getDistance())) {
                    setFire(bulletPower);

                }
            }
//            else shoot(e);

//            setNewTarget();
            waves.add(newWave);
//        }

        // Wave Surfing
//        if(enemy == scannedEnemy) {
            if(ehTiroZe) {
                out.println("NEWWWWW SURFWAVEEEEEEE");
                BulletWave surfWave = new BulletWave(getTime()-1, lastEnemyX, lastEnemyY,
                                                    enemyBulletPower,
                                                    (absoluteBearing+Math.PI) % (2*Math.PI),
                                                    getClockDirection(getVelocity(), getUniqueFrontHeadingRadians(), (absoluteBearing+Math.PI) % (2*Math.PI)) >= 0 ? 1 : -1,
                                                    surfGuesses);
                surfWaves.add(surfWave);
            }
//        }
    }

    public int updateSurfWaves(double hitX, double hitY, long currentTime, double power) {
        int ans = 0;
        for(int i = 0; i < surfWaves.size(); i++) {
            BulletWave wave = surfWaves.get(i);
            if(wave.possibleBullet(hitX, hitY, currentTime, power)) {
                //if(wave.checkHit(getTime(), enemyX, enemyY)) {
                ans++;
                surfWaves.remove(wave);
                i--;
            }
        }
        return ans;
    }

    @Override
	public void onBulletMissed(BulletMissedEvent e) {
//        enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).updateAccuracy(false);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).getAccuracy());
	}
	@Override
	public void onBulletHit(BulletHitEvent e) {
//        enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).updateAccuracy(true);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).getAccuracy());

	}

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent e) {
//        enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).updateAccuracy(false);
        //out.println("Accuracy: " + enemyBotArrayList.get(getEnemy(scannedEnemy.getName())).getAccuracy());

//        EnemyBot meliante = enemyBotArrayList.get(getEnemy(e.getHitBullet().getName()));
//        if (meliante != null) {
            int possibleBullets = updateSurfWaves(e.getHitBullet().getX(), e.getHitBullet().getY(), getTime(), e.getHitBullet().getPower());
            out.println("BULLET HIT BULLET, NUMBER OF SUSPECTS: " + possibleBullets);
            if(possibleBullets > 1)
                merda++;
//        }
    }


    public void onHitByBullet(HitByBulletEvent e) {
        changeReverseChance(5);

//        EnemyBot meliante = enemyBotArrayList.get(getEnemy(e.getName()));
//        if (meliante != null) {
            int possibleBullets = updateSurfWaves(getX(), getY(), getTime(), e.getPower());
            out.println("HIT BY BULLET, NUMBER OF SUSPECTS: " + possibleBullets);
            if(possibleBullets > 1)
                merda++;
//        }
    }

    public void onHitRobot(HitRobotEvent e) {
//        changeDirection();
//        setMinReverseChance();

//        double absoluteBearing = getUniqueFrontHeadingRadians() + e.getBearingRadians();
//        setTurnRadarRightRadians(absoluteBearing);
//        setTurnGunRightRadians(absoluteBearing);
    }
    public void onHitWall(HitWallEvent e) {
//        changeDirection();
//        setMinReverseChance();
    }
//    public void onRobotDeath(RobotDeathEvent e) {
//        EnemyBot dead = enemyBotArrayList.get(getEnemy(e.getName()));
//        dead.alive = false;
//        if(dead.scanned) {
//            dead.scanned = false;
//            scanned--;
//        }
//        if(target == dead)
//            setNewTarget();
//    }

//    public int getEnemy(String enemy){
//        for (int i = 0; i < enemyBotArrayList.size(); i++) {
//            if(enemyBotArrayList.get(i).getName() == enemy) return i;
//        }
//        return -1;
//    }

    public double calculateBulletPower(double enemyDistance) {
        //if(getTime() < 200) return 0.1;
//            if(getOthers() > 3)

        double power = Math.min(Math.min(400/enemyDistance, 3), getEnergy()-0.1);
        if(getEnergy() < 10) power = 0.1;
        if(power >= getEnergy()) power = 0;
        return power;
    }

//    public void shoot(ScannedRobotEvent e) {
//
//        // double fire = Math.min((400 / e.getDistance()), 3);
//
////        enemy.update(e);
////        int enemyIndex
//        double absoluteBearing = getUniqueFrontHeadingRadians() + e.getBearingRadians();
//        out.println("Shooting " + getEnemy(e.getName()) + " :" + e.getName());
////        enemyBotArrayList.get(getEnemy(e.getName())).update(e,absoluteBearing);
////        enemy = enemyBotArrayList.get(getEnemy(e.getName()));
//
//
//        double fire =
//                Math.min(
//                        //( 400 / enemy.getDistance())*enemy.accuracy*5 + 0.4
//                        400 / scannedEnemy.getDistance(),3)
//                ;
//        if (getTime() > 50 && getTime() < 600 && scannedEnemy.getAccuracy() != 0.0){
//            fire *= 1.3;
//        }
//        if (getEnergy() <= 15) {
//            fire /= 2;
//        }
////        if(scannedEnemy.getAccuracy() == 0.0 ) {
////            fire = 0.1;
////        }
//
//        if(fire >= getEnergy())
//            return;
//
//        pointGun(
//                scannedEnemy.getBearing(),
//                scannedEnemy.getHeading(),
//                scannedEnemy.getVelocity(),
//                Rules.getBulletSpeed(2.0),
//                fire,
//                scannedEnemy.getDistance(),
//                scannedEnemy.getTurnRate()
//        );
//        //     pointGun(
//        //             e.getBearingRadians(),
//        //             e.getHeadingRadians(),
//        //             e.getVelocity(),
//        //             Rules.getBulletSpeed(2.0),
//        //             fire,
//        //             e.getDistance()
//        //             );
//        if (getGunHeat() != 0) return;
//        if (getEnergy() > fire) setFire(fire);
//
//    }
//
//    private void pointGun(double bearingRadians, double headingRadians, double velocity, double bulletSpeed,double bulletPower, double distance, double turnRate) {
//
//        double myX = getX();
//        double myY = getY();
//
//        double absoluteBearing = getUniqueFrontHeadingRadians() + bearingRadians;
//        double enemyX = getX() + distance * Math.sin(absoluteBearing);
//        double enemyY = getY() + distance * Math.cos(absoluteBearing);
//        double enemyHeading = headingRadians;
//
//        double enemyHeadingChange = turnRate;
//        double enemyVelocity = velocity;
//
//
//        double deltaTime = 0;
//        double battleFieldHeight = getBattleFieldHeight(),
//                battleFieldWidth = getBattleFieldWidth();
//        double predictedX = enemyX, predictedY = enemyY;
//        while((++deltaTime) * (20.0 - 3.0 * bulletPower) <
//                Point2D.Double.distance(myX, myY, predictedX, predictedY)){
//            predictedX += Math.sin(enemyHeading) * enemyVelocity;
//            predictedY += Math.cos(enemyHeading) * enemyVelocity;
//            enemyHeading += enemyHeadingChange;
//            if(	predictedX < 18.0
//                    || predictedY < 18.0
//                    || predictedX > battleFieldWidth - 18.0
//                    || predictedY > battleFieldHeight - 18.0){
//
//                predictedX = Math.min(Math.max(18.0, predictedX),
//                        battleFieldWidth - 18.0);
//                predictedY = Math.min(Math.max(18.0, predictedY),
//                        battleFieldHeight - 18.0);
//                break;
//            }
//        }
//        double theta = Utils.normalAbsoluteAngle(Math.atan2(
//                predictedX - getX(), predictedY - getY()));
//        double gunRightTurn = Utils.normalRelativeAngle(
//                theta - getGunHeadingRadians());
//        setTurnGunRightRadians(gunRightTurn);
//
//
//    }

    public void onPaint(Graphics2D g) {
        super.onPaint(g);

//        if(target != null) {
            for(int i = 0; i < surfWaves.size(); i++) {
                BulletWave wave = surfWaves.get(i);
                wave.draw(g, getTime());
            }
//        }

//        for(int i = 0; i < waves.size(); i++) {
//            BulletWave wave = waves.get(i);
//            wave.draw(g, getTime());
//        }

        for(Predictor.Predicted point : lastCandidates) {
            g.drawOval((int)point.x-5, (int)point.y-5, 10, 10);
        }

        g.setColor(new Color(255, 255, 255));
        g.drawRect((int)fat, (int)fat, (int)(fWidth-fat*2), (int)(fHeight-fat*2));

        g.drawLine((int)getX(), (int)getY(), (int)(getX()+vecX), (int)(getY()+vecY));
        g.setColor(new Color(255, 49, 163));
        g.drawLine((int)getX(), (int)getY(), (int)(getX()+dX), (int)(getY()+dY));

        g.drawString(Integer.toString(merda), 10, 10);

    }
}

