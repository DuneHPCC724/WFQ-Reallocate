package ch.ethz.systems.netbench.xpt.WFQ.EPSWFQ;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class EPSWFQQueue implements Queue {

    private final ArrayList<ArrayBlockingQueue> queueList;
    private final Map flowBytesSent; //<yuxin> Bi

    private final Map<Integer, Long> FIFOBytesOccupied;//<yuxin> Occupancy of the queue in Bytes

    private final Map <Integer, Long> FIFOBytesSend;
    private final Map<String, Long> FlowBytesArrived;

    private final Map<String, Long> FlowPacketsArrived;

    private final Map<String, Long> FlowTimeInterval;

    private final Map<String, Long> FlowTimeLastArrive;

    private long bytesPerRound;// <yuxin> in theory, bytesPerRound = size of a FIFO in Bytes
    private long currentRound;
    private long servingQueue;

    private long taildrop;

    private long rounddrop;

    private long totalpackets;

    private long queueLength = 20000;
    private ReentrantLock reentrantLock;
    private int ownId;

    private int targetId;

    private float alpha = 1f;

    private double BandwidthBitPerNs;

    public EPSWFQQueue(long numQueues, long bytesPerRound, int ownId, int targetId, double BandwidthBitPerNs){
        long perQueueCapacity = 320;// <yuxin> physical size of a FIFO in packets
        this.FIFOBytesOccupied = new HashMap();
        this.FIFOBytesSend = new HashMap();
        this.queueList = new ArrayList((int)numQueues);
        ArrayBlockingQueue fifo;
        for (int i=0; i<(int)numQueues; i++){
            fifo = new ArrayBlockingQueue((int)perQueueCapacity);
            queueList.add(fifo);
            FIFOBytesOccupied.put(i, (long)0);
            FIFOBytesSend.put(i, (long)0);
        }

        this.flowBytesSent = new HashMap();
        this.FlowBytesArrived = new HashMap();
        this.FlowPacketsArrived = new HashMap();
        this.FlowTimeInterval = new HashMap();
        this.FlowTimeLastArrive = new HashMap();
        this.bytesPerRound = bytesPerRound;
        this.currentRound = 0;
        this.servingQueue = 0;
        this.taildrop = 0;
        this.rounddrop = 0;
        this.totalpackets = 0;
        this.reentrantLock = new ReentrantLock();
        this.ownId = ownId;
        this.targetId = targetId;
        this.BandwidthBitPerNs = BandwidthBitPerNs;
    }

    @Override
    public boolean offer(Object o){

        this.reentrantLock.lock();
        FullExtTcpPacket p = (FullExtTcpPacket) o;
        boolean result = true;

        try {

            UpdateST(p);//<yuxin> update s and t
            long FinalQueue;
            String Id = p.getDiffFlowId3();
            long bid;
            if(p.isSYN() || FlowTimeInterval.get(Id) == (long)(-2)){//<yuxin>if SYN or first data packet ,enqueue the head queue
                FinalQueue = 0;
                bid = 0;
            }

            //<yuxin> phase 1 start
            else{
                float weight = p.getWeight();// <yuxin> flow weight
                // Compute the packet bid (when will the last byte be transmitted) as the max. between the current round (in bytes) and the last bid of the flow
                bid = (long)(this.currentRound * this.bytesPerRound * weight);

                if(flowBytesSent.containsKey(Id)){
                    if(bid < (Long)flowBytesSent.get(Id)){
                        bid = (Long)flowBytesSent.get(Id);
                    }
                }
                bid = bid + (p.getSizeBit()/8);

                long packetRound = (long) (bid/(this.bytesPerRound*weight));
                long AnchorQueue = (packetRound - this.currentRound);

                //<yuxin> phase 2 start
                if(AnchorQueue > (long)0){//<yuxin> not head queue
                    double s = FlowBytesArrived.get(Id)*1.0/FlowPacketsArrived.get(Id);//<yuxin>average packet size
                    double t = FlowTimeInterval.get(Id)*(BandwidthBitPerNs/8)/bytesPerRound;//<yuxin>average interval per round
                    double speed = s/t;
                    double prediction = bytesPerRound*weight;
                    double AlphaFactor;
                    if(speed < prediction){
                        //System.out.println("slow");
                        AlphaFactor = 1;
                    }
                    else {
                        //System.out.println("fast");
                        AlphaFactor = Math.pow((speed/prediction), 1.0/AnchorQueue);
                    }
                    double PromoteWeight = weight*AlphaFactor;
                    if(PromoteWeight > 1){//<yuxin> can't exceed 1
                        PromoteWeight = 1;
                    }
                    FinalQueue = (long) (bid/(this.bytesPerRound*PromoteWeight) - this.currentRound);
                    if(FinalQueue < 0){
                        FinalQueue = 0;
                    }
                }
                else {//<yuxin> head queue
                    FinalQueue = 0;
                }
            }

            //<yuxin> phase 3 start
            if (FinalQueue > queueList.size()-1){//<yuxin> Packet dropped since computed round is too far away
                result = false;
                rounddrop += 1;
            }
            else {
                boolean TailDropMark = true;
                for (int i = (int)FinalQueue; i<queueList.size(); i++){
                    int QueueToSend = (int)((FinalQueue+this.servingQueue)%(queueList.size()));
                    long FIFOSizeEstimate = p.getSizeBit()/8 + FIFOBytesOccupied.get(QueueToSend);
                    if(FIFOSizeEstimate <= this.queueLength){//<yuxin> find a available FIFO
                        result = queueList.get(QueueToSend).offer(p);
                        TailDropMark = false;
                        if (!result) {
                            System.out.println("!!!maybe value perQueueCapacity should be larger");
                        } else {
                            flowBytesSent.put(Id, bid);
                            FIFOBytesOccupied.put(QueueToSend, FIFOSizeEstimate);
                            long FIFOBytesSendEstimate = p.getSizeBit()/8 + FIFOBytesSend.get(QueueToSend);
                            FIFOBytesSend.put(QueueToSend, FIFOBytesSendEstimate);
                        }
                        break;
                    }
                }
                if (TailDropMark){
                    taildrop += 1;
                    result = false;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Probably the bid size has been exceeded, transmit less packets ");
            System.out.println("Exception EPSWFQ offer: " + e.getMessage() + e.getLocalizedMessage());
        } finally {
            this.reentrantLock.unlock();
            return result;
        }
    }

    @Override
    public Packet poll(){
        this.reentrantLock.lock();
        try {
            Packet p = null;
            while (p == null){
                if (this.size() != 0) {
                    if (!queueList.get((int) this.servingQueue).isEmpty()) {
                        p = (Packet) queueList.get((int) this.servingQueue).poll();
                        long FIFOSizeDecreaseEstimate = FIFOBytesOccupied.get((int) this.servingQueue) - p.getSizeBit()/8;
                        FIFOBytesOccupied.put((int) this.servingQueue, FIFOSizeDecreaseEstimate);//<yuxin> decrease when send a packet
                        return p;
                    } else {
                        SimulationLogger.logDropRate(ownId, targetId, currentRound, taildrop * 1.0 / totalpackets, rounddrop * 1.0 / totalpackets, (taildrop + rounddrop) * 1.0 / totalpackets);
//                        FIFOBytesOccupied.put((int)this.servingQueue, (long)0);
                        SimulationLogger.logFIFOsend(ownId, targetId, currentRound, this.FIFOBytesSend.get((int)servingQueue));
                        FIFOBytesSend.put((int)servingQueue, (long)0);
                        this.servingQueue = (this.servingQueue + 1) % this.queueList.size();
                        this.currentRound++;
                    }
                }
            }
            return null;
        }
        finally {
            this.reentrantLock.unlock();
        }
    }

    @Override
    public int size() {
        int size = 0;
        for (int q=0; q<queueList.size(); q++){
            size = size + queueList.get(q).size();
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        boolean empty = true;
        for (int q=0; q<queueList.size(); q++){
            if(!queueList.get(q).isEmpty()){
                empty = false;
                break;// <yuxin> reduce the number of searches
            }
        }
        return empty;
    }

    public long increaseTotalPackets(){
        totalpackets += 1;
        return totalpackets;
    }

    public void UpdateBi(FullExtTcpPacket p){//<yuxin> update Bi for outputport
        long bid = (long)(this.currentRound * this.bytesPerRound * p.getWeight());

        if(flowBytesSent.containsKey(p.getDiffFlowId3())){
            if(bid < (Long)flowBytesSent.get(p.getDiffFlowId3())){
                bid = (Long)flowBytesSent.get(p.getDiffFlowId3());
            }
        }
        bid = bid + (p.getSizeBit()/8);
        flowBytesSent.put(p.getDiffFlowId3(), bid);
    }

    public void UpdateST(FullExtTcpPacket p){
        String Id = p.getDiffFlowId3();
        if (p.isSYN() == true){ //<yuxin> if is SYN, initialize flowtimeinterval as -2, Bytes and Packets as 0
            FlowTimeInterval.put(Id, (long)(-2));//<yuxin> tell next packet that you are first
            FlowBytesArrived.put(Id, (long)(0));
            FlowPacketsArrived.put(Id, (long)(0));
        }
        else{//<yuxin> data packets
            long Bytes = FlowBytesArrived.get(Id) + p.getSizeBit()/8;
            FlowBytesArrived.put(Id, Bytes);
            long Packets = FlowPacketsArrived.get(Id) + 1;
            FlowPacketsArrived.put(Id, Packets);
            if(FlowTimeInterval.get(Id) == (long)(-2)){//<yuxin> if is the first data packet, put current time in
                FlowTimeInterval.put(Id, (long)(-1));//<yuxin> tell next packet that you are second
                FlowTimeLastArrive.put(Id, Simulator.getCurrentTime());
            }
            else if(FlowTimeInterval.get(Id) == (long)(-1)){//<yuxin> second date packet, compute interval at first time
                long time =Simulator.getCurrentTime();
                long Interval = time - FlowTimeLastArrive.get(Id);
                FlowTimeInterval.put(Id, Interval);
                FlowTimeLastArrive.put(Id, time);
            }
            else{//<yuxin> other data packets, compute interval use EMA
                long time =Simulator.getCurrentTime();
                long LastInterval = FlowTimeInterval.get(Id);
                long Interval = (long)((1-alpha)*LastInterval + alpha*(time - FlowTimeLastArrive.get(Id)));
                FlowTimeInterval.put(Id, Interval);
                FlowTimeLastArrive.put(Id, time);
            }
        }
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public Object[] toArray(Object[] objects) {
        return new Object[0];
    }

    @Override
    public boolean add(Object o) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean addAll(Collection collection) {
        return false;
    }

    @Override
    public void clear() { }

    @Override
    public boolean retainAll(Collection collection) {
        return false;
    }

    @Override
    public boolean removeAll(Collection collection) {
        return false;
    }

    @Override
    public boolean containsAll(Collection collection) {
        return false;
    }

    @Override
    public Object remove() {
        return null;
    }

    @Override
    public Object element() {
        return null;
    }

    @Override
    public Object peek() {
        return null;
    }
}
