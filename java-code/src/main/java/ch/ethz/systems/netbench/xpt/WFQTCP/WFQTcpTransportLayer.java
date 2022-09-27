package ch.ethz.systems.netbench.xpt.WFQTCP;
import ch.ethz.systems.netbench.core.network.Socket;
import ch.ethz.systems.netbench.core.network.TransportLayer;

public class WFQTcpTransportLayer extends TransportLayer {

    public WFQTcpTransportLayer(int indentifier) {
        super(indentifier);
    }
    @Override
    public void startFlow(int destination, long flowSizeByte,float weight){
        Socket socket = createSocket(flowIdCounter, destination, flowSizeByte,weight);
        flowIdToSocket.put(flowIdCounter, socket);
        flowIdCounter++;
        // Start the socket off as initiator
        socket.markAsSender();
        socket.start();
    }

    @Override
    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte) {
        return new WFQTcpSocket(this, flowId, this.identifier, destinationId, flowSizeByte,0);
    }

    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte,float weight) {
        return new WFQTcpSocket(this, flowId, this.identifier, destinationId, flowSizeByte,weight);
    }
}
