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
        DUAL_ALL_TO_ALL_SERVER_FRACTION
    }
    private final FlowSizeDistribution flowSizeDistribution;
    private final Random ownIndependentRng;
    private final RandomCollection<Pair<Integer, Integer>> randomPairGenerator;
    private final int TotalFlowNumber;
    private final WeightDistribution wd;

    public UniformWeightPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, FlowSizeDistribution flowSizeDistribution, int FlowNum,PairDistribution pairDistribution) {
        super(idToTransportLayerMap);
        this.flowSizeDistribution = flowSizeDistribution;
        this.TotalFlowNumber = FlowNum;
        this.wd = new WeightDistribution("uniform",FlowNum);
        this.ownIndependentRng = Simulator.selectIndependentRandom("uniform_arrival"+Integer.toString(FlowNum));
        this.randomPairGenerator = new RandomCollection<>(Simulator.selectIndependentRandom("pair_probabilities_draw"+Integer.toString(FlowNum)));
        switch (pairDistribution) {

            case ALL_TO_ALL:
                this.setPairProbabilitiesAllToAll();
                break;
            default:
                throw new IllegalArgumentException("Invalid pair distribution given: " + pairDistribution + ".");

        }
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

    @Override
    public void createPlan(long durationNs) {
        this.createPlan_Incast();
    }
    public void createPlan_Incast() {
        int[] weights = this.wd.get_weights();
        int total_weight = this.wd.getTotal_weight();
        for(int i=0;i<weights.length;i++){
            double weight_current = 1.0*weights[i]/(double)total_weight;
            registerFlow(0, 0, 2, flowSizeDistribution.generateFlowSizeByte(),(float) weight_current,0);
        }
    }

    //copy from poisson planner
    private Pair<Integer, Integer> choosePair() {
        return this.randomPairGenerator.next();
    }


}
