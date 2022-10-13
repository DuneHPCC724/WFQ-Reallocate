package ch.ethz.systems.netbench.xpt.WFQ.PCQBURST;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;

public class PCQBURSTOutputPortGenerator extends OutputPortGenerator {

    private final long numQueues;

    private final long bytesPerRound;

    public PCQBURSTOutputPortGenerator(long numQueues, long bytesPerRound) {
        this.numQueues = numQueues;
        this.bytesPerRound = bytesPerRound;
        SimulationLogger.logInfo("Port", "PCQBURST(numQueues=" + numQueues +  ", bytesPerRound=" + bytesPerRound + ")");
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {
        return new PCQBURSTOutputPort(ownNetworkDevice, towardsNetworkDevice, link, numQueues, bytesPerRound);
    }

}
