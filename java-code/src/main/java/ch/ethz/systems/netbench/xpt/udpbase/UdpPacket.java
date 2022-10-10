package ch.ethz.systems.netbench.xpt.udpbase;

import ch.ethz.systems.netbench.ext.basic.IpPacket;

public abstract class UdpPacket extends IpPacket implements UdpHeader {
    private static final long UDP_HEADER_SIZE_BIT = 160L;

    private final int sourcePort;
    private final int destinationPort;
    private final long packetnumber;
    private final long offset;
    private final double windowSize;
    private final long dataSizeByte;
    private int nonSequentialHash = -1;

    public UdpPacket(
            long flowId, long dataSizeByte, int sourceId, int destinationId, int TTL,// IP header fields
            int sourcePort, int destinationPort, long packetnumber, long offset, double windowSize
    ) {
        super(flowId, UDP_HEADER_SIZE_BIT + dataSizeByte * 8L, sourceId, destinationId, TTL);
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.packetnumber = packetnumber;
        this.offset = offset;
        this.windowSize = windowSize;
        this.dataSizeByte = dataSizeByte;
    }

    @Override
    public long getDataSizeByte() {
        return dataSizeByte;
    }

    @Override
    public int getSourcePort() {
        return sourcePort;
    }

    @Override
    public int getDestinationPort() {
        return destinationPort;
    }

    @Override
    public long getPacketNumber() {
        return this.packetnumber;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    public double getWindowSize() {
        return windowSize;
    }

    @Override
    public int getNonSequentialHash() {
        return nonSequentialHash;
    }

    @Override
    public void setNonSequentialHash(int nonSeqHash) {
        nonSequentialHash = nonSeqHash;
    }

    public String toString() {
        return "UDPPacket[" + getSourceId() + ":" + getSourcePort() + " -> " + getDestinationId() + ":" + getDestinationPort() + " ,FlowId: " + getFlowId() + ", DATA=" + this.getDataSizeByte();
    }
}