package anon961.kubert.proxies;

import anon961.kubert.ModelInfo;
import anon961.kubert.UMLRTSLibrary;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.RTPort;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.UMLRealTimeFactory;
import org.eclipse.uml2.uml.*;

import java.util.HashMap;
import java.util.Map;

public class TCPServerProxyCapsule extends TCPProxyCapsule {
    private Port serverPort;
    private String serviceName;

    public TCPServerProxyCapsule(ModelInfo.CapsuleInstance singletonInstance,
                                 ModelInfo.ReplicatedPart proxiedPart) {
        super(proxiedPart._getClass().getName()+"TCPServerProxy", singletonInstance, proxiedPart);
        this.serviceName = singletonInstance.getServiceName();
    }

    @Override
    public void createInternalPorts() {
        super.createInternalPorts();
        serverPort = UMLRTSLibrary.createPort("server", UMLRTSLibrary.SystemProtocols.TCP);
        serverPort.setIsService(true);
        capsuleClass.getOwnedPorts().add(serverPort);

        RTPort serverRTPort = UMLRealTimeFactory.eINSTANCE.createRTPort();
        serverRTPort.setBase_Port(serverPort);
        serverRTPort.setIsWired(false);
        rtStereotypeElements.add(serverRTPort);
        systemPortsCount++;
    }

    @Override
    protected Transition createConnectingConnectedTransition(Vertex source, Vertex target) {
        Transition transition = super.createConnectingConnectedTransition(source, target);
        transition.getTriggers().get(0).getPorts().clear();
        transition.getTriggers().get(0).getPorts().add(serverPort);
        return transition;
    }

    @Override
    protected Transition createInitialConnectingTransition(Vertex source, Vertex target) {
        Transition transition = createTransition(source, target);
        Map<String, Object> params = new HashMap<>();
        params.put("serviceName", serviceName);
        transition.setEffect(createEffect(transition, params));
        return transition;
    }
}