package PIVA;
import robocode.*;
import robocode.util.Utils;

public class Predictor {
    private static double STICK_LENGTH = 150;

    double width, height;

    public Predictor(double width, double height) {
        this.width = width;
        this.height = height;
    }

    static class Predicted {
        double x, y, heading, velocity;
        long time;
    }

    boolean isInSafeArea(double x, double y) {
        return x > 18 && x < width - 18 && y > 18 && y < height - 18;
    }

    public double wallSmoothing(Predicted point, double angle, boolean clockwise) {
        int signal = clockwise ? 1 : -1;
        int iterations = 0;

        do {
            double dx = point.x + Math.sin(angle) * STICK_LENGTH;
            double dy = point.y + Math.cos(angle) * STICK_LENGTH;

            if(isInSafeArea(dx, dy))
                break;

            angle += 0.05 * signal;
        } while(++iterations < 50);

        return Utils.normalAbsoluteAngle(angle);
    }

    public Predicted newPredicted(Predicted last, double angle, double distance, double maxVelocity) {
        double offset  = Utils.normalRelativeAngle(angle - last.heading);
        double turn = offset;

        if(Math.abs(offset) > Math.PI/2)
            turn = Utils.normalRelativeAngle(offset + Math.PI);

        Predicted next = new Predicted();
        next.heading = getNewHeading(last.heading, turn, last.velocity);

        int ahead = offset == turn ? 1 : -1;

        next.velocity = getNewVelocity(last.velocity * ahead, maxVelocity, Double.POSITIVE_INFINITY) * ahead;
        next.x = last.x + next.velocity*Math.sin(next.heading);
        next.y = last.y + next.velocity*Math.cos(next.heading);
        next.time = last.time + 1;

        return next;
    }

    public Predicted surf(Predicted initial, BulletWave wave, double perpendiculator, double maxVelocity, boolean clockwise) {
        while(!wave.hasHit(initial.time, initial.x, initial.y)) {
            double angle = wallSmoothing(initial, perpendiculator, clockwise);
            initial = newPredicted(initial, angle, maxVelocity == 0 ? 0 : Double.POSITIVE_INFINITY, maxVelocity);
        }
        return initial;
    }

    public static double getNewHeading(double heading, double turn, double velocity) {
        double turnRate = Rules.getTurnRateRadians(velocity);
        return Utils.normalAbsoluteAngle(heading + Math.min(+turnRate, Math.max(-turnRate, turn)));
    }

    public static double getNewVelocity(double velocity, double maxVelocity, double distance) {
        if (distance < 0) {
            // If the distanceToEdges is negative, then change it to be positive
            // and change the sign of the input velocity and the result
            return -getNewVelocity(-velocity, maxVelocity, -distance);
        }

        final double goalVel;

        if (distance == Double.POSITIVE_INFINITY) {
            goalVel = maxVelocity;
        } else {
            goalVel = Math.min(getMaxVelocity(distance), maxVelocity);
        }

        if (velocity >= 0) {
            return Math.max(velocity - Rules.DECELERATION, Math.min(goalVel, velocity + Rules.ACCELERATION));
        }
        // else
        return Math.max(velocity - Rules.ACCELERATION, Math.min(goalVel, velocity + maxDecel(-velocity)));
    }

    private final static double getMaxVelocity(double distance) {
        final double decelTime = Math.max(1, Math.ceil(
                (Math.sqrt((4 * 2 / Rules.DECELERATION) * distance + 1) - 1) / 2));

        if (decelTime == Double.POSITIVE_INFINITY) {
            return Rules.MAX_VELOCITY;
        }

        final double decelDist = (decelTime / 2.0) * (decelTime - 1)
                * Rules.DECELERATION;

        return ((decelTime - 1) * Rules.DECELERATION) + ((distance - decelDist) / decelTime);
    }

    private static double maxDecel(double speed) {
        double decelTime = speed / Rules.DECELERATION;
        double accelTime = (1 - decelTime);

        return Math.min(1, decelTime) * Rules.DECELERATION + Math.max(0, accelTime) * Rules.ACCELERATION;
    }
}
