package PIVA;

import robocode.ScannedRobotEvent;

public class EnemyBot {
    double bearing;
    double distance;
    double energy;
    double heading;
    double velocity;
    String name;

    int nMean;
    int nAccuracy;

    double oldEnemyHeading;
    ArrayList<Double> velocityList;
    ArrayList<Double> turnRateList;

    double accuracy;
    ArrayList<Double> accuracyList;

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
        return velocity;
    }
    public String getName(){
        return name;
    }
    public void update(ScannedRobotEvent bot){
        bearing = bot.getBearingRadians();
        distance = bot.getDistance();
        energy = bot.getEnergy();
        oldEnemyHeading = heading; // the current heading becomes previously
        heading = bot.getHeadingRadians();
        velocity = bot.getVelocity();
        name = bot.getName();
    }
    public void reset(){
        bearing = 0.0;
        distance = 0.0;
        energy= 0.0;
        heading =0.0;
        velocity = 0.0;
        name = null;
    }

    public Boolean none(){
        if (name == null || name == "")
            return true;
        else
            return false;
    }

    public EnemyBot(){
        nMean = 5;
        nAccuracy = 10;
        velocityList = new ArrayList<Double>();
        turnRateList = new ArrayList<Double>();
        accuracyList = new ArrayList<Double>();
        for (int i = 0; i < nMean; i++) {
            velocityList.add(5.0);
            turnRateList.add(5.0);
        }
        for (int i = 0; i < nAccuracy; i++) {
            accuracyList.add(0.0);
        }
        reset();
    }

    public double getAccuracy(){
        return accuracy;
    }
    public void updateAccuracy(double value){
        accuracyList.remove(0);
        accuracyList.add(value);

        int i;

        for (accuracy = 0, i = 0; i < accuracyList.size(); i++) {
            accuracy += accuracyList.get(i);
        }

    }

}
