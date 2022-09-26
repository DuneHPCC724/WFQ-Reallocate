package ch.ethz.systems.netbench.xpt.WFQ.PCQ;

import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class PCQQueue implements Queue {

    private final ArrayList<ArrayBlockingQueue> queueList;
    private final Map flowBytesSent;
    private long bytesPerRound;// <yuxin> in theory, bytesPerRound = size of a FIFO in Bytes
    private long currentRound;
    private long servingQueue;
    private ReentrantLock reentrantLock;
    private int ownId;

    public PCQQueue(long numQueues, long bytesPerRound, int ownId){
        long perQueueCapacity = 320;// <yuxin> size of a FIFO in packets
        this.queueList = new ArrayList((int)numQueues);
        ArrayBlockingQueue fifo;
        for (int i=0; i<(int)numQueues; i++){
            fifo = new ArrayBlockingQueue((int)perQueueCapacity);
            queueList.add(fifo);
        }

        this.flowBytesSent = new HashMap();
        this.bytesPerRound = bytesPerRound;
        this.currentRound = 0;
        this.servingQueue = 0;
        this.reentrantLock = new ReentrantLock();
        this.ownId = ownId;
    }

    @Override
    public boolean offer(Object o){

        this.reentrantLock.lock();
        FullExtTcpPacket p = (FullExtTcpPacket) o;
        boolean result = true;

        try {

            // Compute the packet bid (when will the last byte be transmitted) as the max. between the current round (in bytes) and the last bid of the flow
            long bid = this.currentRound * this.bytesPerRound;

            if(flowBytesSent.containsKey(p.getFlowId())){
                if(bid < (Long)flowBytesSent.get(p.getFlowId())){
                    bid = (Long)flowBytesSent.get(p.getFlowId());
                }
            }
            bid = bid + (p.getSizeBit()/8);

            double weight = 1.0;// <yuxin> flow weight
            long packetRound = (long) (bid/(this.bytesPerRound*weight));

            if((packetRound - this.currentRound) > queueList.size()){
                result = false; // Packet dropped since computed round is too far away
            } else {
                result = queueList.get((int)packetRound%(queueList.size())).offer(p);
                if (!result){
                } else {
                    flowBytesSent.put(p.getFlowId(), bid);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Probably the bid size has been exceeded, transmit less packets ");
            System.out.println("Exception PCQ offer: " + e.getMessage() + e.getLocalizedMessage());
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
                        return p;
                    } else {
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
