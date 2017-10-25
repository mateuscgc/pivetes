package PIVA;

import java.awt.geom.Point2D;
import robocode.util.Utils;

public class BulletWave {
    private long fireTime;
    private double fireX, fireY;
    private double bulletPower;

    private double fireAbsoluteBearing;
    private int clockDirection;

    //private int[] guesses;
    private double[] guesses;

    public BulletWave(long fireTime, double fireX, double fireY, double bulletPower, double fireAbsoluteBearing, int clockDirection, double[] guesses) {
        this.fireTime = fireTime;
        this.fireX = fireX;
        this.fireY = fireY;
        this.bulletPower = bulletPower;
        this.fireAbsoluteBearing = fireAbsoluteBearing;
        this.clockDirection = clockDirection;
        this.guesses = guesses;
    }

    public double getBulletVelocity() {
        return 20 - bulletPower*3;
    }
    public double maxEscapeAngle() {
        return Math.asin(8 / getBulletVelocity());
    }

    public boolean checkHit(long currentTime, double enemyX, double enemyY) {
        // If bulletWave has not reached robot
        if ((currentTime -  fireTime) * getBulletVelocity()
                < Point2D.distance(fireX, fireY, enemyX, enemyY))
            return false;

        logWave(enemyX, enemyY);

        return true;
    }

    public double curRay(long currentTime) {
        return getBulletVelocity()*(currentTime-fireTime);
    }

    public boolean possibleBullet(double hitX, double hitY, long currentTime) {
        if(Math.abs(curRay(currentTime) - Point2D.distance(fireX, fireY, hitX, hitY)) > 30) return false;

        logWave(hitX, hitY);

        return true;
    }

    private void logWave(double hitX, double hitY) {
        double correctDirection = Math.atan2(hitX - fireX, hitY - fireY);
        double movementOffSet = Utils.normalRelativeAngle((correctDirection - fireAbsoluteBearing));
        double guessFactor = Math.max(-1, Math.min(1, movementOffSet / maxEscapeAngle())) * clockDirection;
        int index = (int)Math.round((guesses.length - 1) / 2 * (guessFactor + 1));
        for(int i = 0; i < guesses.length; i++)
            guesses[i] *= 0.7;
        guesses[index]++;
    }

}
