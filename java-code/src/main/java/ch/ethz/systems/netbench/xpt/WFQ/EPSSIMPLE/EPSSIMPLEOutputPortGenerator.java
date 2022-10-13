package ch.ethz.systems.netbench.xpt.WFQ.EPSSIMPLE;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;
import ch.ethz.systems.netbench.xpt.WFQ.EPSSIMPLE.EPSSIMPLEOutputPort;

public class EPSSIMPLEOutputPortGenerator extends OutputPortGenerator {

    private final long numQueues;

    private final long bytesPerRound;

    public EPSSIMPLEOutputPortGenerator(long numQueues, long bytesPerRound) {
        this.numQueues = numQueues;
        this.bytesPerRound = bytesPerRound;
        SimulationLogger.logInfo("Port", "EPSSIMPLE(numQueues=" + numQueues +  ", bytesPerRound=" + bytesPerRound + ")");
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {
        return new EPSSIMPLEOutputPort(ownNetworkDevice, towardsNetworkDevice, link, numQueues, bytesPerRound);
    }

}