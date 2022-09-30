package ch.ethz.systems.netbench.xpt.WFQ.PCQWB;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;

public class PCQWBOutputPortGenerator extends OutputPortGenerator {

    private final long numQueues;

    private final long bytesPerRound;

    public PCQWBOutputPortGenerator(long numQueues, long bytesPerRound) {
        this.numQueues = numQueues;
        this.bytesPerRound = bytesPerRound;
        SimulationLogger.logInfo("Port", "PCQWB(numQueues=" + numQueues +  ", bytesPerRound=" + bytesPerRound + ")");
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {
        return new PCQWBOutputPort(ownNetworkDevice, towardsNetworkDevice, link, numQueues, bytesPerRound);
    }

}
