package ch.ethz.systems.netbench.xpt.WFQ.OEPSSIMPLE;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.ext.basic.IpHeader;
import ch.ethz.systems.netbench.xpt.WFQ.OEPSSIMPLE.OEPSSIMPLEQueue;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;




public class OEPSSIMPLEOutputPort extends OutputPort {


    public OEPSSIMPLEOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, long numQueues, long bytesPerRound) {
        super(ownNetworkDevice, targetNetworkDevice, link, new OEPSSIMPLEQueue(numQueues, bytesPerRound, ownNetworkDevice.getIdentifier(), targetNetworkDevice.getIdentifier(), link.getBandwidthBitPerNs()));
    }

    /**
     * Enqueue the given packet.
     * Drops it if the queue is full (tail drop).
     *
     * @param packet    Packet instance
     */
    @Override
    public void enqueue(Packet packet) {

        OEPSSIMPLEQueue q = (OEPSSIMPLEQueue)getQueue();
        q.increaseTotalPackets();
        // If it is not sending, then the queue is empty at the moment,
        // so this packet can be immediately send
        if (!getIsSending()) {
            q.UpdateBi((FullExtTcpPacket)packet);
            q.UpdateST((FullExtTcpPacket)packet);
            q.logEnDeEvent((FullExtTcpPacket)packet);

            // Link is now being utilized
            getLogger().logLinkUtilized(true);

            // Add event when sending is finished
            Simulator.registerEvent(new PacketDispatchedEvent(
                    (long)((double)packet.getSizeBit() / getLink().getBandwidthBitPerNs()),
                    packet,
                    this
            ));

            // It is now sending again
            setIsSending();

        } else { // If it is still sending, the packet is added to the queue, making it non-empty

            boolean enqueued = false;
            enqueued = getQueue().offer(packet);


            if (enqueued){
                increaseBufferOccupiedBits(packet.getSizeBit());
                getLogger().logQueueState(getQueue().size(), getBufferOccupiedBits());
            } else {
                SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED");

                // Convert to IP packet
                IpHeader ipHeader = (IpHeader) packet;
                if (ipHeader.getSourceId() == this.getOwnId()) {
                    SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED_AT_SOURCE");
                }
            }
        }

    }
}
