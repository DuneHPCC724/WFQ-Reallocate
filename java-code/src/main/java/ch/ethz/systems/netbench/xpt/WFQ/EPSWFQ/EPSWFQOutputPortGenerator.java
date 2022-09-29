package ch.ethz.systems.netbench.xpt.WFQ.EPSWFQ;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;
import ch.ethz.systems.netbench.xpt.WFQ.EPSWFQ.EPSWFQOutputPort;

public class EPSWFQOutputPortGenerator extends OutputPortGenerator {

    private final long numQueues;

    private final long bytesPerRound;

    public EPSWFQOutputPortGenerator(long numQueues, long bytesPerRound) {
        this.numQueues = numQueues;
        this.bytesPerRound = bytesPerRound;
        SimulationLogger.logInfo("Port", "EPSWFQ(numQueues=" + numQueues +  ", bytesPerRound=" + bytesPerRound + ")");
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {
        return new EPSWFQOutputPort(ownNetworkDevice, towardsNetworkDevice, link, numQueues, bytesPerRound);
    }

}