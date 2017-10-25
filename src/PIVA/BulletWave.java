package PIVA;

import java.awt.*;
import java.awt.geom.Point2D;
import robocode.util.Utils;

public class BulletWave {
    private long fireTime;
    public double fireX, fireY;
    private double bulletPower;

    public double fireAbsoluteBearing;
    public int clockDirection;

    //private int[] guesses;
    public double[] guesses;

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

    public boolean hasHit(long currentTime, double enemyX, double enemyY) {
        return (currentTime -  fireTime) * getBulletVelocity()
                > Point2D.distance(fireX, fireY, enemyX, enemyY);
    }

    public boolean checkHit(long currentTime, double enemyX, double enemyY) {
        // If bulletWave has not reached robot
        if (!hasHit(currentTime, enemyX, enemyY))
            return false;

        logWave(enemyX, enemyY, 0.8);

        return true;
    }

    public double curRadius(long currentTime) {
        return getBulletVelocity()*(currentTime-fireTime);
    }

    public boolean possibleBullet(double hitX, double hitY, long currentTime, double power) {
        if(Math.abs(curRadius(currentTime) - Point2D.distance(fireX, fireY, hitX, hitY)) > 30
                || Math.abs(bulletPower-power) > 0.2) return false;

        logWave(hitX, hitY, 0.7);

        return true;
    }

    private void logWave(double hitX, double hitY, double decay) {
        double correctDirection = Math.atan2(hitX - fireX, hitY - fireY);
        double movementOffSet = Utils.normalRelativeAngle((correctDirection - fireAbsoluteBearing));
        double guessFactor = Math.max(-1, Math.min(1, movementOffSet / maxEscapeAngle())) * clockDirection;
        int index = (int)Math.round((guesses.length - 1) / 2 * (guessFactor + 1));
        for(int i = 0; i < guesses.length; i++)
            guesses[i] *= decay;
        guesses[index]++;
    }

    public void draw(Graphics2D g, long currentTime) {
        //        g.drawOval((int)(getX()-stick),(int)(getY()-stick),(int)(stick*2),(int)(stick*2));
        int recX = (int)(fireX- curRadius(currentTime));
        int recY = (int)(fireY- curRadius(currentTime));
        double ray = curRadius(currentTime);
        double scannedX = fireX + ray*Math.sin(fireAbsoluteBearing);
        double scannedY = fireY + ray*Math.cos(fireAbsoluteBearing);

        g.setColor(new Color(31, 32, 255));
//        g.drawOval(recX, recY, (int)ray*2, (int)ray*2);
//        g.setColor(new Color(255, 36, 135));
//        g.drawLine((int)scannedX,
//                    (int)scannedY,
//                    (int)scannedX,
//                    (int)scannedY);
        g.drawArc(recX, recY, (int)ray*2, (int)ray*2, (int)((fireAbsoluteBearing+maxEscapeAngle())/Math.PI*180), 360-(int)(maxEscapeAngle()/Math.PI*180)*2);
        //g.drawArc(recX, recY, (int)ray*2, (int)ray*2, (int)((fireAbsoluteBearing-maxEscapeAngle())/Math.PI*180), (int)(maxEscapeAngle()/Math.PI*180)*2);

        double[] guesses = getSmoothedGuesses(400); // TODO: corrige essa merda

        double maxi = 0;
        for(int i = 0; i < guesses.length; i++) maxi = Math.max(maxi, guesses[i]);
        for(int i = 0; i < guesses.length; i++) {
            double guessFactor = (double)(i-(guesses.length-1)/2) / ((guesses.length-1)/2);
            double predictedOffSet = maxEscapeAngle() * guessFactor * clockDirection;
//            double gunRightTurn = Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians() + predictedOffSet);
            double guessX = fireX + ray*Math.sin(fireAbsoluteBearing+predictedOffSet);
            double guessY = fireY + ray*Math.cos(fireAbsoluteBearing+predictedOffSet);

            g.setColor(new Color((int)(guesses[i]/maxi*255), 255-(int)(guesses[i]/maxi*255), 0));
            g.drawOval((int)guessX-3, (int)guessY-3, 6, 6);

//            g.drawString(String.format("%.2f" , guesses[i]), (int)guessX-3, (int)guessY-3);
        }

    }

    public double timeToImpact(double hitX, double hitY, long time) {
        double distance = Point2D.distance(hitX, hitY, fireX, fireY);
        return (distance - curRadius(time)) / getBulletVelocity();
    }

    public double getGaussKernel(double x) {
        return Math.exp(-0.5 * x * x);
    }

    public double[] getSmoothedGuesses(double distance) {
        double botWidth = 36 / distance;
        double botWidthInGuessFactor = (botWidth / 2) / maxEscapeAngle();
        double botWidthInIndexes = (botWidthInGuessFactor) * ((guesses.length - 1) / 2);

        double[] smoothed = new double[guesses.length];

        for(int i = 0; i < guesses.length; i++) {
            for(int j = 0; j < guesses.length; j++) {
                smoothed[j] += guesses[i] * getGaussKernel((j-i) / botWidthInIndexes);
            }
        }

        return smoothed;
    }
}
