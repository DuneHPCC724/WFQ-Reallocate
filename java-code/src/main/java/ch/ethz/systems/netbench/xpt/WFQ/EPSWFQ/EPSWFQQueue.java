package ch.ethz.systems.netbench.xpt.WFQ.EPSWFQ;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class EPSWFQQueue implements Queue {

    public final ArrayList<ArrayBlockingQueue> queueList;
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

//    private long queueLength = 15000;
    private ReentrantLock reentrantLock;
    private int ownId;

    private int targetId;

    private boolean islogswitch;

    private double alpha;//move average factor

    private double BandwidthBitPerNs;

    private double rho;//control factor

    private boolean head_bpr_limit;

    public EPSWFQQueue(long numQueues, long bytesPerRound, int ownId, int targetId, double BandwidthBitPerNs){
        long perQueueCapacity = 8192;// <yuxin> physical size of a FIFO in packets
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
        if (ownId == 10 && targetId == 11){
            islogswitch = true;
        }
        else if (ownId == 16 && targetId == 17){
            islogswitch = true;
        }
        else {
            islogswitch = false;
        }
        this.rho = Simulator.getConfiguration().getDoublePropertyWithDefault("esprho",0.1);
        this.alpha = Simulator.getConfiguration().getDoublePropertyWithDefault("alpha_factor", 0.2);
        this.head_bpr_limit = Simulator.getConfiguration().getBooleanPropertyWithDefault("headqueue_bpr_limit", false);
    }

    public int offerPacket(Object o){

        this.reentrantLock.lock();
        FullExtTcpPacket p = (FullExtTcpPacket) o;
        int result = -1;

        try {

            UpdateST(p);//<yuxin> update s and t
            long FinalQueue;
            String Id = p.getDiffFlowId3();
            long bid;
//            if(p.isSYN() || FlowTimeInterval.get(Id) == (long)(-1) || p.isACK()){//<yuxin>if SYN or first data packet ,enqueue the head queue
            if(p.isSYN() || p.isACK()){//<yuxin>if SYN or first data packet ,enqueue the head queue
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
                        AlphaFactor *= rho;
                        if (AlphaFactor < 1){
                            AlphaFactor = 1;
                        }

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
                result = -1;
                if (islogswitch) {
                    if (fullDrop(p)) {
                        SimulationLogger.logDropEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8, 0);
                    } else {
                        SimulationLogger.logDropEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8, 1);
                    }
                }
                rounddrop += 1;
            }
            else {
                boolean TailDropMark = true;
                for (int i = (int)FinalQueue; i<queueList.size(); i++){
                    int QueueToSend = (int)((i+this.servingQueue)%(queueList.size()));
                    long FIFOSizeEstimate = p.getSizeBit()/8 + FIFOBytesOccupied.get(QueueToSend);
                    if(FIFOSizeEstimate <= this.bytesPerRound){//<yuxin> find a available FIFO
                        result = QueueToSend;
                        if (islogswitch) {
                            SimulationLogger.logEnqueueEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8);
//                            SimulationLogger.logEnqueueEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), 0);

                        }
                        TailDropMark = false;
                        flowBytesSent.put(Id, bid);
                        FIFOBytesOccupied.put(QueueToSend, FIFOSizeEstimate);
                        long FIFOBytesSendEstimate = p.getSizeBit()/8 + FIFOBytesSend.get(QueueToSend);
                        FIFOBytesSend.put(QueueToSend, FIFOBytesSendEstimate);
                        break;
                    }
                }
                if (TailDropMark){
                    if (islogswitch) {
                        if (fullDrop(p)) {
                            SimulationLogger.logDropEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8, 0);
                        } else {
                            SimulationLogger.logDropEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8, 1);
                        }
                    }
                    taildrop += 1;
                    result = -1;
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
    public boolean offer(Object o){return false;}

//    @Override
//    public boolean offer(Object o){
//
//        this.reentrantLock.lock();
//        FullExtTcpPacket p = (FullExtTcpPacket) o;
//        boolean result = true;
//
//        try {
//
//            UpdateST(p);//<yuxin> update s and t
//            long FinalQueue;
//            String Id = p.getDiffFlowId3();
//            long bid;
//            if(p.isSYN() || p.isACK()){//<yuxin>if SYN or first data packet ,enqueue the head queue
//                FinalQueue = 0;
//                bid = 0;
//            }
//
//            //<yuxin> phase 1 start
//            else{
//                float weight = p.getWeight();// <yuxin> flow weight
//                // Compute the packet bid (when will the last byte be transmitted) as the max. between the current round (in bytes) and the last bid of the flow
//                bid = (long)(this.currentRound * this.bytesPerRound * weight);
//
//                if(flowBytesSent.containsKey(Id)){
//                    if(bid < (Long)flowBytesSent.get(Id)){
//                        bid = (Long)flowBytesSent.get(Id);
//                    }
//                }
//                bid = bid + (p.getSizeBit()/8);
//
//                long packetRound = (long) (bid/(this.bytesPerRound*weight));
//                long AnchorQueue = (packetRound - this.currentRound);
//
//                //<yuxin> phase 2 start
//                if(AnchorQueue > (long)0){//<yuxin> not head queue
//                    double s = FlowBytesArrived.get(Id)*1.0/FlowPacketsArrived.get(Id);//<yuxin>average packet size
//                    double t = FlowTimeInterval.get(Id)*(BandwidthBitPerNs/8)/bytesPerRound;//<yuxin>average interval per round
//                    double speed = s/t;
//                    double prediction = bytesPerRound*weight;
//                    double AlphaFactor;
//                    if(speed < prediction){
//                        //System.out.println("slow");
//                        AlphaFactor = 1;
//                    }
//                    else {
//                        //System.out.println("fast");
//                        AlphaFactor = Math.pow((speed/prediction), 1.0/AnchorQueue);
//                        AlphaFactor *= rho;
//                        if (AlphaFactor < 1){
//                            AlphaFactor = 1;
//                        }
//
//                    }
//                    double PromoteWeight = weight*AlphaFactor;
//                    if(PromoteWeight > 1){//<yuxin> can't exceed 1
//                        PromoteWeight = 1;
//                    }
//                    FinalQueue = (long) (bid/(this.bytesPerRound*PromoteWeight) - this.currentRound);
//                    if(FinalQueue < 0){
//                        FinalQueue = 0;
//                    }
//                }
//                else {//<yuxin> head queue
//                    FinalQueue = 0;
//                }
//            }
//
//            //<yuxin> phase 3 start
//            if (FinalQueue > queueList.size()-1){//<yuxin> Packet dropped since computed round is too far away
//                result = false;
//                if (islogswitch) {
//                    if (fullDrop(p)) {
//                        SimulationLogger.logDropEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8, 0);
//                    } else {
//                        SimulationLogger.logDropEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8, 1);
//                    }
//                }
//                rounddrop += 1;
//            }
//            else {
//                boolean TailDropMark = true;
//                for (int i = (int)FinalQueue; i<queueList.size(); i++){
//                    int QueueToSend = (int)((i+this.servingQueue)%(queueList.size()));
//                    long FIFOSizeEstimate = p.getSizeBit()/8 + FIFOBytesOccupied.get(QueueToSend);
//                    if(FIFOSizeEstimate <= this.bytesPerRound){//<yuxin> find a available FIFO
//                        result = queueList.get(QueueToSend).offer(p);
//                        if (islogswitch) {
//                            SimulationLogger.logEnqueueEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8);
////                            SimulationLogger.logEnqueueEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), 0);
//
//                        }
//                        TailDropMark = false;
//                        if (!result) {
//                            System.out.println("!!!maybe value perQueueCapacity should be larger");
//                        } else {
//                            flowBytesSent.put(Id, bid);
//                            FIFOBytesOccupied.put(QueueToSend, FIFOSizeEstimate);
//                            long FIFOBytesSendEstimate = p.getSizeBit()/8 + FIFOBytesSend.get(QueueToSend);
//                            FIFOBytesSend.put(QueueToSend, FIFOBytesSendEstimate);
//                        }
//                        break;
//                    }
//                }
//                if (TailDropMark){
//                    if (islogswitch) {
//                        if (fullDrop(p)) {
//                            SimulationLogger.logDropEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8, 0);
//                        } else {
//                            SimulationLogger.logDropEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8, 1);
//                        }
//                    }
//                    taildrop += 1;
//                    result = false;
//                }
//            }
//        } catch (Exception e){
//            e.printStackTrace();
//            System.out.println("Probably the bid size has been exceeded, transmit less packets ");
//            System.out.println("Exception EPSWFQ offer: " + e.getMessage() + e.getLocalizedMessage());
//        } finally {
//            this.reentrantLock.unlock();
//            return result;
//        }
//    }

    @Override
    public Packet poll(){
        this.reentrantLock.lock();
        try {
            Packet p = null;
            while (p == null){
                if (this.size() != 0) {
                    if (!queueList.get((int) this.servingQueue).isEmpty()) {
                        p = (Packet) queueList.get((int) this.servingQueue).poll();
                        if (islogswitch) {
                            SimulationLogger.logDequeueEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), ((FullExtTcpPacket) p).getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8, BufferUtil());
                        }
                        if(!head_bpr_limit) {
                            long FIFOSizeDecreaseEstimate = FIFOBytesOccupied.get((int) this.servingQueue) - p.getSizeBit() / 8;
                            FIFOBytesOccupied.put((int) this.servingQueue, FIFOSizeDecreaseEstimate);//<yuxin> decrease when send a packet
                        }
                        return p;
                    } else {
                        SimulationLogger.logDropRate(ownId, targetId, currentRound, taildrop * 1.0 / totalpackets, rounddrop * 1.0 / totalpackets, (taildrop + rounddrop) * 1.0 / totalpackets);
                        if(head_bpr_limit) {
                            FIFOBytesOccupied.put((int) this.servingQueue, (long) 0);
                        }
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

    public boolean fullDrop(FullExtTcpPacket p){
        boolean result = true;
        for (int i=0; i<this.queueList.size(); i++){
            if (p.getSizeBit()/8+FIFOBytesOccupied.get(i) <= this.bytesPerRound){
                result = false;
                break;
            }
        }
        return result;
    }

    public void logEnDeEvent(FullExtTcpPacket p){
        if (islogswitch) {
            SimulationLogger.logEnqueueEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8);
//            SimulationLogger.logEnqueueEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), p.getSequenceNumber(), currentRound, Simulator.getCurrentTime(), 1   );
            SimulationLogger.logDequeueEvent(ownId, targetId, ((FullExtTcpPacket) p).getDiffFlowId3(), ((FullExtTcpPacket) p).getSequenceNumber(), currentRound, Simulator.getCurrentTime(), p.getSizeBit() / 8, (p.getSizeBit() / 8) * 1.0 / (this.queueList.size() * this.bytesPerRound));
        }
    }

    public double BufferUtil(){
        long occupy = 0;
        for(int i=0; i<this.queueList.size(); i++){
            occupy += FIFOBytesOccupied.get(i);
        }
        double util = occupy*1.0/(this.queueList.size()*this.bytesPerRound);
        return util;
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
        } else if (p.isACK()) {
            FlowTimeInterval.put(Id, (long)(-1));
            FlowTimeLastArrive.put(Id, Simulator.getCurrentTime());
            FlowBytesArrived.put(Id, (long)(0));
            FlowPacketsArrived.put(Id, (long)(0));
        } else{//<yuxin> data packets
            long Bytes = FlowBytesArrived.get(Id) + p.getSizeBit()/8;
            FlowBytesArrived.put(Id, Bytes);
            long Packets = FlowPacketsArrived.get(Id) + 1;
            FlowPacketsArrived.put(Id, Packets);
            if(FlowTimeInterval.get(Id) == (long)(-1)){//<yuxin> second date packet, compute interval at first time
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
