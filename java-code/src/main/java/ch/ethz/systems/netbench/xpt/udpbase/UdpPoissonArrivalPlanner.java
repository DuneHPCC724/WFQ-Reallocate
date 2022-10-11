package ch.ethz.systems.netbench.xpt.udpbase;

import ch.ethz.systems.netbench.core.Simulator;

import java.util.Random;

public class UdpPoissonArrivalPlanner {
    private final double LambdaBurstPKTPerSecond;
    private final double LambdaNoBurstPKTPerSecond;
    private final double LambdaTotal;
    private final int PKTSize;
    private final long FlowStartTime;
    private final Random OwnIndependentRng;
    private final Random StateMachineRng;
    private final int deviceId;
    private final int FlowId;

    private final double[] StateMachine;

    private double[] IATs;

    public UdpPoissonArrivalPlanner(double LambdaBurstPKTPerSecond, double LambdaNoBurstPKTPerSecond,double LambdaTotal, int PKTSize,long FlowStartTime ,int deviceId, int FlowId,double[] StateMachine){
        this.LambdaBurstPKTPerSecond = LambdaBurstPKTPerSecond;
        this.LambdaNoBurstPKTPerSecond = LambdaNoBurstPKTPerSecond;
        this.LambdaTotal = LambdaTotal;
        this.PKTSize = PKTSize;
        this.FlowStartTime = FlowStartTime;
        this.deviceId = deviceId;
        this.FlowId = FlowId;
        this.OwnIndependentRng = Simulator.selectIndependentRandom(Integer.toString(FlowId)+Integer.toString(deviceId));   //temp, only use FlowId+deviceId to Generate
        this.StateMachine = StateMachine;
        this.StateMachineRng = Simulator.selectIndependentRandom("State_Machine"+Integer.toString(FlowId)+Integer.toString(deviceId));
        this.IATs = new double[];
    }

    public double[] createPKTPlan(long durationNs){
        System.out.println("Create PKT Plan for flow: "+Integer.toString(this.FlowId));
        long time = this.FlowStartTime;
        int x = 0;
        long sum = 0;
        long nextProgresslog = durationNs/10;

        boolean Burst = false;      //start without burst
        while(time <= durationNs){
            long interArrivalTime;
            if (Burst) {
                interArrivalTime = (long) (-Math.log(this.OwnIndependentRng.nextDouble()) / (this.LambdaBurstPKTPerSecond / 1e9));
                sum+=interArrivalTime;
                double statechange = this.StateMachineRng.nextDouble();
                double p00 = this.StateMachine[0];
                if(statechange <=p00)
                    Burst = false;
                else
                    Burst = true;
            }
            else {
                interArrivalTime = (long) (-Math.log(this.OwnIndependentRng.nextDouble()) / (this.LambdaNoBurstPKTPerSecond / 1e9));
                sum+=interArrivalTime;
                double statechange = this.StateMachineRng.nextDouble();
                double p10 = this.StateMachine[2];
                if(statechange <=p10)
                    Burst = false;
                else
                    Burst = true;
            }
            this.IATs[x] = interArrivalTime;
            sum += interArrivalTime;
            registerPkt(time);
            time += interArrivalTime;
            x++;
            if(time > nextProgresslog){
                System.out.print(" " + (100 * time / durationNs) + "%...");
                nextProgresslog += durationNs / 10;
            }
        }
        System.out.println("Poisson Arrival plan created for flow: "+Integer.toString(this.FlowId));
        System.out.println("Number of packet created: " + x + ".");
        System.out.println("Mean inter-arrival time: " + (sum / x) + " (expectation: "
                + (1 / ( this.LambdaTotal/ 1e9)) + ")");

        return this.IATs;
    }
    private void registerPkt(long time){

    }
}