package ch.ethz.systems.netbench.core.run.traffic;

import ch.ethz.systems.netbench.core.network.Event;
import ch.ethz.systems.netbench.core.network.TransportLayer;

public class FlowStartEvent extends Event {

    private final TransportLayer transportLayer;
    private final int targetId;
    private final long flowSizeByte;

    //add by WFQ
    private float weight = 0;

    /**
     * Create event which will happen the given amount of nanoseconds later.
     *
     * @param timeFromNowNs     Time it will take before happening from now in nanoseconds
     * @param transportLayer    Source transport layer that wants to send the flow to the target
     * @param targetId          Target network device identifier
     * @param flowSizeByte      Size of the flow to send in bytes
     */
    public FlowStartEvent(long timeFromNowNs, TransportLayer transportLayer, int targetId, long flowSizeByte) {
        super(timeFromNowNs);
        this.transportLayer = transportLayer;
        this.targetId = targetId;
        this.flowSizeByte = flowSizeByte;
    }

    /**
     * Create event which will happen the given amount of nanoseconds later.
     *
     * @param timeFromNowNs     Time it will take before happening from now in nanoseconds
     * @param transportLayer    Source transport layer that wants to send the flow to the target
     * @param targetId          Target network device identifier
     * @param flowSizeByte      Size of the flow to send in bytes
     * @param weight            weight of this flow
     */
    public FlowStartEvent(long timeFromNowNs, TransportLayer transportLayer, int targetId, long flowSizeByte,float weight) {
        super(timeFromNowNs);
        this.transportLayer = transportLayer;
        this.targetId = targetId;
        this.flowSizeByte = flowSizeByte;
        this.weight = weight;
    }

    @Override
    public void trigger() {
        transportLayer.startFlow(targetId, flowSizeByte);
    }

    //add by WFQ, override for convenioence
    public void trigger(boolean enable_weight){
        if(enable_weight) {
            transportLayer.startFlow(targetId,flowSizeByte,weight);
        }
        else
            this.trigger();

    }

}
