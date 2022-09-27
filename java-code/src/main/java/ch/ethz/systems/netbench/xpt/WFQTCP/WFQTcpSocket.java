package ch.ethz.systems.netbench.xpt.WFQTCP;

import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.xpt.newreno.newrenotcp.NewRenoTcpSocket;

public class WFQTcpSocket extends NewRenoTcpSocket {
    private float weight;
    public WFQTcpSocket(TransportLayer transportLayer, long flowId, int sourceId, int destinationId, long flowSizeByte, float weight) {
        super(transportLayer, flowId, sourceId, destinationId, flowSizeByte);
        this.weight = weight;
    }

    //need createPacket

}
