package ch.ethz.systems.netbench.xpt.WFQ.OEPSSIMPLE;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;
import ch.ethz.systems.netbench.xpt.WFQ.OEPSSIMPLE.OEPSSIMPLEOutputPort;

public class OEPSSIMPLEOutputPortGenerator extends OutputPortGenerator {

    private final long numQueues;

    private final long bytesPerRound;

    public OEPSSIMPLEOutputPortGenerator(long numQueues, long bytesPerRound) {
        this.numQueues = numQueues;
        this.bytesPerRound = bytesPerRound;
        SimulationLogger.logInfo("Port", "OEPSSIMPLE(numQueues=" + numQueues +  ", bytesPerRound=" + bytesPerRound + ")");
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {
        return new OEPSSIMPLEOutputPort(ownNetworkDevice, towardsNetworkDevice, link, numQueues, bytesPerRound);
    }

}