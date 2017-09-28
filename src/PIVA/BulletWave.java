package PIVA;

import java.awt.geom.Point2D;
import robocode.util.Utils;

public class BulletWave {
    private long fireTime;
    private double fireX, fireY;
    private double bulletPower;

    private double fireEnemyAbsoluteBearing;
    private int clockDirection;

    //private int[] guesses;
    private double[] guesses;

    public BulletWave(long fireTime, double fireX, double fireY, double bulletPower, double fireEnemyAbsoluteBearing, int clockDirection, double[] guesses) {
        this.fireTime = fireTime;
        this.fireX = fireX;
        this.fireY = fireY;
        this.bulletPower = bulletPower;
        this.fireEnemyAbsoluteBearing = fireEnemyAbsoluteBearing;
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

        double correctDirection = Math.atan2(enemyX - fireX, enemyY - fireY);
        double movementOffSet = Utils.normalRelativeAngle((correctDirection - fireEnemyAbsoluteBearing));
        double guessFactor = Math.max(-1, Math.min(1, movementOffSet / maxEscapeAngle())) * clockDirection;
        int index = (int)Math.round((guesses.length - 1) / 2 * (guessFactor + 1));
        for(int i = 0; i < guesses.length; i++)
            guesses[i] *= 0.7;
        guesses[index]++;

        return true;
    }
}
