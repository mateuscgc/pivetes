package PIVA;
import robocode.ScannedRobotEvent;
import java.util.ArrayList;

public class EnemyBot {
    double bearing;
    double distance;
    double energy;
    double lastEnergy;
    double heading;
    double velocity;
    double absoluteBearing;
    String name;

    double turnRate;
    double meanVelocity;
//    double turnRate;



    int nMean;
    int nAccuracy;

    double oldEnemyHeading;
    ArrayList<Double> velocityList;
    ArrayList<Double> turnRateList;


    double accuracy;
    ArrayList<Boolean> accuracyList;

    boolean scanned;
    boolean alive;

    public double getBearing(){
        return bearing;
    }
    public double getDistance(){
        return distance;
    }
    public double getEnergy(){
        return energy;
    }
    public double getHeading(){
        return heading;
    }
    public double getVelocity(){

        return meanVelocity;
    }
    public String getName(){
        return name;
    }

    public double getTurnRate() {

        return turnRate;
    }




    public void update(ScannedRobotEvent bot,double absoluteBearing){
        int i;

        this.absoluteBearing = absoluteBearing;
        bearing = bot.getBearingRadians();
        distance = bot.getDistance();
        lastEnergy = energy;
        energy = bot.getEnergy();

        oldEnemyHeading = heading; // the current heading becomes previously
        heading = bot.getHeadingRadians();
        turnRateList.remove(0);//update Mean turnRate
        turnRateList.add(heading - oldEnemyHeading);
        for (turnRate = 0, i = 0; i < turnRateList.size(); i++) {
            turnRate += turnRateList.get(i)/turnRateList.size();
        }

        velocity = bot.getVelocity();
        velocityList.remove(0);//update Mean Velocity
        velocityList.add(velocity);
        for (meanVelocity = 0, i = 0; i < velocityList.size(); i++) {
            meanVelocity += velocityList.get(i)/velocityList.size();
        }

        name = bot.getName();
    }
    public void reset(){
        bearing = 0.0;
        distance = Double.POSITIVE_INFINITY;
        energy= 100.0;
        lastEnergy = 100.0;
        heading =0.0;
        velocity = 0.0;

        name = null;
    }

    public double bulletPower() {
        return lastEnergy - energy;
    }

    public boolean ehTiroZe() {
        return 0.09 < bulletPower() && bulletPower() < 3.1;
    }


    public Boolean none(){
        if (name == null || name == "")
            return true;
        else
            return false;
    }

    public EnemyBot(){
        scanned = false;
        alive = true;
        nMean = 1;
        nAccuracy = 10;
        velocityList = new ArrayList<Double>();
        turnRateList = new ArrayList<Double>();
        accuracyList = new ArrayList<Boolean>();
        for (int i = 0; i < nMean; i++) {
            velocityList.add(1.0);
            turnRateList.add(1.0);
        }
        for (int i = 0; i < nAccuracy; i++) {
            accuracyList.add(false);
        }
        reset();
    }

    public void rise() {
        alive = true;
        scanned = false;
    }

    public double getAccuracy(){
        return accuracy;
    }
    public void updateAccuracy(boolean value){
        accuracyList.remove(0);
        accuracyList.add(value);

        int i;

        for (accuracy = 0, i = 0; i < accuracyList.size(); i++) {
            accuracy += accuracyList.get(i) ? 0.2 : 0.0;
        }

    }

    public void setX1Fire(){
        accuracyList.subList(0, accuracyList.size()/2).clear();
    }

}
