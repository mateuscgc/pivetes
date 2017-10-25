package PIVA;

import robocode.ScannedRobotEvent;

public class EnemyInfo {

    double x, y, heading, velocity, energy;
    long currentTime;

    public static EnemyInfo newEnemy(ScannedRobotEvent e, TwoFrontsRobot me) {
        EnemyInfo enemy = new EnemyInfo();

        double absoluteBearing = me.getUniqueFrontHeadingRadians() + e.getBearingRadians();
        enemy.x = me.getX() + e.getDistance()*Math.sin(absoluteBearing);
        enemy.y = me.getY() + e.getDistance()*Math.cos(absoluteBearing);

        enemy.heading = e.getHeadingRadians();
        enemy.velocity = e.getVelocity();
        enemy.energy = e.getEnergy();
        enemy.currentTime = me.getTime();

        return enemy;
    }

    public boolean checkShot(ScannedRobotEvent e) {
        double delta = energy - e.getEnergy();
        return 0.0999999 < delta && delta < 3.000001;
    }

    public double bulletPower(ScannedRobotEvent e) {
        return energy - e.getEnergy();
    }
}
