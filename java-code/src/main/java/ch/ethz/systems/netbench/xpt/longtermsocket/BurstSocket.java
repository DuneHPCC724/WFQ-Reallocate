package ch.ethz.systems.netbench.xpt.longtermsocket;

import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.xpt.WFQTCP.WFQTcpSocket;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;

public class BurstSocket extends WFQTcpSocket {
    private final Longtermsocket longtermsocket;

    public BurstSocket (TransportLayer transportLayer, long flowId, int sourceId, int destinationId, long flowSizeByte, float weight, int flowset_num,Longtermsocket longtermsocket){
        super(transportLayer, flowId, sourceId, destinationId, flowSizeByte,weight,flowset_num);
        this.longtermsocket = longtermsocket;
    }

    public BurstSocket(TransportLayer transportLayer, long flowId, int sourceId, int destinationId, long flowSizeByte, float weight,int flowset_num)
    {
        super(transportLayer, flowId, sourceId, destinationId, flowSizeByte,weight,flowset_num);
        this.longtermsocket = null;
    }

    @Override
    protected void handleAcknowledgment(FullExtTcpPacket packet){
        super.handleAcknowledgment(packet);
        if(this.longtermsocket != null){
            if(isAllFlowConfirmed()) {
                this.longtermsocket.Start_Rest();//register next burst event
            }
        }
    }

}
