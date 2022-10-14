package ch.ethz.systems.netbench.ext.poissontraffic;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.traffic.TrafficPlanner;
import ch.ethz.systems.netbench.ext.poissontraffic.flowsize.FlowSizeDistribution;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Random;

public class UniformWeightPlanner extends TrafficPlanner {
    //copy pair distribute from poisson planner
    public enum PairDistribution {
        ALL_TO_ALL,
        ALL_TO_ALL_FRACTION,
        ALL_TO_ALL_SERVER_FRACTION,
        PARETO_SKEW_DISTRIBUTION,
        PAIRINGS_FRACTION,
        DUAL_ALL_TO_ALL_FRACTION,
        DUAL_ALL_TO_ALL_SERVER_FRACTION,
        Incast
    }

    private final FlowSizeDistribution flowSizeDistribution;
    private final Random ownIndependentRng;
    private final RandomCollection<Pair<Integer, Integer>> randomPairGenerator;
    private final int TotalFlowNumber;
    private final int WeightNumber;
    private final WeightDistribution wd;

    public UniformWeightPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, FlowSizeDistribution flowSizeDistribution,int weight_num ,int FlowNum,PairDistribution pairDistribution,String wdistribution) {
        super(idToTransportLayerMap);
        this.flowSizeDistribution = flowSizeDistribution;
        this.TotalFlowNumber = FlowNum;
        this.WeightNumber = weight_num;
        this.ownIndependentRng = Simulator.selectIndependentRandom("uniform_arrival"+Integer.toString(FlowNum));
        this.randomPairGenerator = new RandomCollection<>(Simulator.selectIndependentRandom("pair_probabilities_draw"+Integer.toString(FlowNum)));
        switch (pairDistribution) {

            case ALL_TO_ALL:
                this.setPairProbabilitiesAllToAll();
                break;
            case Incast:
                this.setPairIncast();
                break;
            default:
                throw new IllegalArgumentException("Invalid pair distribution given: " + pairDistribution + ".");

        }
        this.wd = new WeightDistribution(wdistribution,WeightNumber);
        SimulationLogger.logInfo("Flow planner", "Unifrom_Weight_Traffic(flownumber=" + this.TotalFlowNumber + ", pairDistribution=" + pairDistribution + ")");

    }

    //copy form poisson planner
    private void setPairProbabilitiesAllToAll() {

        System.out.print("Generating all-to-all pair probabilities between all nodes with a transport layer...");

        // Uniform probability for every server pair
        double pdfNumBytes = 1.0 / (this.idToTransportLayerMap.size() * (this.idToTransportLayerMap.size() - 1));

        // Add uniform probability for every pair
        for (Integer src : this.idToTransportLayerMap.keySet()){
            for (Integer dst : this.idToTransportLayerMap.keySet()){
                if(!src.equals(dst)) {
                    this.randomPairGenerator.add(pdfNumBytes, new ImmutablePair<>(src, dst));
                }
            }
        }

        System.out.println(" done.");
    }

    private void setPairIncast(){
        System.out.print("Generating incast pair probabilities between all nodes with a transport layer...");
        double pdfNumBytes = 1.0 /  (this.idToTransportLayerMap.size() - 1);
        int numofserver = this.idToTransportLayerMap.keySet().size();
        int dst = 0;//temp
        int dst_ind = ownIndependentRng.nextInt(numofserver);
        int count = 0;
        for(Integer i:this.idToTransportLayerMap.keySet())
        {
            if(count == dst_ind){
                dst = i;
                break;
            }
            count++;
        }
        for(Integer src:this.idToTransportLayerMap.keySet()){
            if(!src.equals(dst)){
                this.randomPairGenerator.add(pdfNumBytes,new ImmutablePair<>(src,dst));
            }
        }
    }

    @Override
    public void createPlan(long durationNs) {
        this.createPlan_Incast();
    }
    public void createPlan_Incast() {
        double[] weights = this.wd.get_weights_uniformly(this.TotalFlowNumber);
        for(int i=0;i<weights.length;i++){
            double weight_current = weights[i];
            Pair<Integer, Integer> pair = choosePair();
            registerFlow(0, pair.getLeft(), pair.getRight(), flowSizeDistribution.generateFlowSizeByte(),(float) weight_current,0);
        }
    }

    //copy from poisson planner
    private Pair<Integer, Integer> choosePair() {
        return this.randomPairGenerator.next();
    }


}
