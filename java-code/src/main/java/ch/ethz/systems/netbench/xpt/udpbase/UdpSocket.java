package ch.ethz.systems.netbench.xpt.udpbase;

import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.Socket;
import ch.ethz.systems.netbench.core.network.TransportLayer;

import java.util.logging.Logger;

public class UdpSocket extends Socket {
    private Logger UdpLogger;   //need to be updated to type of UdpLogger

    private static final long FIRST_PKT_NUMBER = 0;
    private static final long FIRST_OFF_NUMBER = 0;

    private static final long MAXIMUM_FLOW_SIZE = 1000000000000L;   //maybe can be deleted

    enum State{
        LISTENING,
        SENDING
    };

    private final long MAX_SEGMENT_SIZE = 1500-20-8;    //MTU-IP_HEADER-UDP_HEADER

    //window size and thresh not supported

    private State currentState;     //sending or listening

    private long NextPktNumber;     //NextPktNumber

    private long NextOffsetNumber;  //NextOffset Number

    private long[] PKTIATs;        //the IATs of Packets

    private long CurrentTIme;       //the Time Current Pkt sent

    private double FlowRate;        //ave rate of this flow

    private double burst_strength;      //the strengsh of burst(ave burst rate/ ave rate)

    //burst state machine
    private double[] state_p;       //4 element: p00,p01,p10,p11

    private double weight;

    private final long FlowStartTimeNs;

    public UdpSocket(TransportLayer transportLayer,long flowId,int sourceId, int destinationId, long flowSizeByte,double weight,long FlowStartTimeNs,double burst_strength,double[] state_machine){
        super(transportLayer, flowId, sourceId, destinationId, flowSizeByte);
        this.FlowStartTimeNs = FlowStartTimeNs;
        this.state_p = state_machine;
        this.weight = weight;
        this.burst_strength = burst_strength;
    }

    //start(): insert packet sent events,to send all packets
    @Override
    public void start(){
        //first:generate IATs[]
        //second: insert Events
    }

    @Override
    public void handle(Packet genericPacket) {

    }

}
