package ch.ethz.systems.netbench.ext.poissontraffic;

import ch.ethz.systems.netbench.core.Simulator;

import java.util.Random;

//the weight[] should be int[] ,
//get weight ,return double
public class WeightDistribution {
    private int[] weights;
    private int[] multiples;
    private int weight_num;
    private double[] thresh;
    private Random ownRng;
    private int total_weight;
    public WeightDistribution(String distribution, int weight_num){
        this.ownRng = Simulator.selectIndependentRandom("weight_distribute"+distribution);
        switch (distribution) {
            //need to be modified
            case "uniform":
                this.weight_num = weight_num;
                this.weights = new int[this.weight_num];
                int weight_each = 1;
                int totalweight = 0;
                for(int i=0;i<this.weight_num;i++){
                    this.weights[i] = weight_each;
                    totalweight +=1;
                }
                this.total_weight = totalweight;
                this.thresh = new double[weight_num];
                double cd = 1.0/weight_num;
                thresh[0] = cd;
                for(int i=1;i<weight_num;i++){
                    thresh[i] = thresh[i-1]+cd;
                }
                break;
            case "linear":
                this.weight_num = weight_num;
                this.weights = new int[this.weight_num];
                this.multiples = new int[this.weight_num];
                for(int i=0;i<this.weight_num;i++) {
                    this.multiples[i] = i+1;
                }
                int base_weight = 1;
                int ltotalweight = 0;
                for(int i=0;i<this.weight_num;i++){
                    this.weights[i] = base_weight*this.multiples[i];
                    ltotalweight += this.weights[i];
                }
                this.total_weight = ltotalweight;
                this.thresh = new double[weight_num];
                double lcd = 1.0/weight_num;
                thresh[0] = lcd;
                for(int i=1;i<weight_num;i++){
                    thresh[i] = thresh[i-1]+lcd;
                }
                break;
            default:
                break;
        }
    }
    public int[] get_weights(){
        return this.weights;
    }
    public int[] getMultiples(){return this.multiples;}
    public int getTotal_weight(){return this.total_weight;}
    public double get_random_weight_uniform(){
        int random = this.ownRng.nextInt(weight_num);
        return this.weights[random];
    }
}
