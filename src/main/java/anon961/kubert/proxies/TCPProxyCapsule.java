package anon961.kubert.proxies;

import anon961.kubert.ModelInfo;
import anon961.kubert.UMLRTSLibrary;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.CapsuleProperties;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.RTCppPropertiesFactory;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.CapsulePart;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.RTPort;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.UMLRealTimeFactory;
import org.eclipse.papyrusrt.umlrt.profile.statemachine.UMLRTStateMachines.*;
import org.eclipse.uml2.uml.*;
import org.eclipse.uml2.uml.Package;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TCPProxyCapsule extends ProxyCapsule {
    private Port tcpPort;
    private Port timerPort;
    private ModelInfo.CapsuleInstance singletonInstance;
    private Collection<ServiceNameEntry> serviceNames = new ArrayList<>();

    private int borderPortCount = 0;
    private int internalPortCount = 0;

    private class ServiceNameEntry {
        public String partName;
        public int partIndex;
        public String serviceName;

        public ServiceNameEntry(String partName, int partIndex, String serviceName) {
            this.partName = partName;
            this.partIndex = partIndex;
            this.serviceName = serviceName;
        }
    }

    protected TCPProxyCapsule(String name, ModelInfo.CapsuleInstance singletonInstance,
                              ModelInfo.ReplicatedPart proxiedPart) {
        super(name, proxiedPart);
        this.singletonInstance = singletonInstance;
        create();
    }

    public TCPProxyCapsule(ModelInfo.CapsuleInstance singletonInstance,
                           ModelInfo.ReplicatedPart proxiedPart) {
        this(proxiedPart._getClass().getName()+"TCPProxy", singletonInstance, proxiedPart);
    }

    public TCPProxyCapsule(ModelInfo.CapsuleInstance singletonInstance,
                           ModelInfo.ReplicatedPart proxiedPart,
                           EcoreUtil.Copier copier) {
        super(proxiedPart, copier);
        this.singletonInstance = singletonInstance;
        create();
    }

    protected void create() {
        createProperties();
        createAttributes();
        createInternalPorts();
    }

    protected void createProperties() {
        CapsuleProperties properties = RTCppPropertiesFactory.eINSTANCE.createCapsuleProperties();
        properties.setBase_Class(capsuleClass);
        properties.setHeaderPreface("#include \"umlrtjsoncoder.hh\";\n"
                + "#include \"umlrtoutsignal.hh\";");
        rtStereotypeElements.add(properties);
    }

    protected void createAttributes() {
        Property hostProperty = UMLFactory.eINSTANCE.createProperty();
        hostProperty.setName("hostname");
        hostProperty.setType(UMLRTSLibrary.getUmlType("String"));
        capsuleClass.getOwnedAttributes().add(hostProperty);

        Property portProperty = UMLFactory.eINSTANCE.createProperty();
        portProperty.setName("port");
        portProperty.setType(UMLRTSLibrary.getUmlType("Integer"));
        capsuleClass.getOwnedAttributes().add(portProperty);
    }

    @Override
    protected void createInternalPorts() {
        super.createInternalPorts();

        tcpPort = UMLRTSLibrary.createPort("tcp", UMLRTSLibrary.SystemProtocols.TCP);
        tcpPort.setIsService(true);
        capsuleClass.getOwnedPorts().add(tcpPort);

        RTPort tcpRTPort = UMLRealTimeFactory.eINSTANCE.createRTPort();
        tcpRTPort.setBase_Port(tcpPort);
        tcpRTPort.setIsWired(false);
        rtStereotypeElements.add(tcpRTPort);

        timerPort = UMLRTSLibrary.createPort("timer", UMLRTSLibrary.SystemProtocols.Timing);
        timerPort.setIsService(true);
        capsuleClass.getOwnedPorts().add(timerPort);

        RTPort rtTimerPort = UMLRealTimeFactory.eINSTANCE.createRTPort();
        rtTimerPort.setBase_Port(timerPort);
        rtTimerPort.setIsWired(false);
        rtStereotypeElements.add(rtTimerPort);
        systemPortsCount +=2;
    }

    protected void createBehaviour() {
        StateMachine stateMachine = UMLFactory.eINSTANCE.createStateMachine();
        stateMachine.setName("StateMachine");
        capsuleClass.getOwnedBehaviors().add(stateMachine);

        RTStateMachine rtStateMachine = UMLRTStateMachinesFactory.eINSTANCE.createRTStateMachine();
        rtStateMachine.setBase_StateMachine(stateMachine);
        rtStateMachine.setIsPassive(false);
        rtStereotypeElements.add(rtStateMachine);

        Region mainRegion = stateMachine.createRegion("main");
        RTRegion rtMainRegion = UMLRTStateMachinesFactory.eINSTANCE.createRTRegion();
        rtMainRegion.setBase_Region(mainRegion);
        rtStereotypeElements.add(rtMainRegion);

        Vertex initialState = createInitialState();
        Vertex connectingState = createState("Connecting");
        Vertex connectedState = createState("Connected");
        Vertex errorState = createState("Error");

        mainRegion.getSubvertices().add(initialState);
        mainRegion.getSubvertices().add(connectingState);
        mainRegion.getSubvertices().add(connectedState);
        mainRegion.getSubvertices().add(errorState);

        mainRegion.getTransitions().add(createInitialConnectingTransition(initialState, connectingState));
        mainRegion.getTransitions().add(createConnectingConnectedTransition(connectingState, connectedState));
        mainRegion.getTransitions().add(createConnectingErrorTransition(connectingState, errorState));
        mainRegion.getTransitions().add(createConnectedErrorTransition(connectedState, errorState));
        mainRegion.getTransitions().add(createErrorConnectingTransition(errorState, connectingState));
        mainRegion.getTransitions().add(createConnectedConnectedOnReceiveTransition(connectedState, connectedState));

        int transitionSuffix = 0;
        for (RTPort rtPort : proxiedPart.getRTPorts()) {
            if (!UMLRTSLibrary.isSystemPort(rtPort)) {
                Transition connectingConnectingTrans = createConnectingConnectingTransition(connectingState, connectingState);
                Transition connectedConnectedTrans = createConnectedConnectedTransition(connectedState, connectedState);
                Transition errorErrorTrans = createErrorErrorTransition(errorState, errorState);

                connectingConnectingTrans.setName(connectingConnectingTrans.getName()+"_"+transitionSuffix);
                connectedConnectedTrans.setName(connectedConnectedTrans.getName()+"_"+transitionSuffix);
                errorErrorTrans.setName(errorErrorTrans.getName()+"_"+transitionSuffix);
                transitionSuffix++;

                Package protocolPkg = rtPort.getBase_Port().getType().getPackage();
                AnyReceiveEvent anyEvent = (AnyReceiveEvent) EcoreUtil.getObjectByType(
                        protocolPkg.getPackagedElements(), UMLPackage.Literals.ANY_RECEIVE_EVENT);
                connectedConnectedTrans.getTriggers().add(createTrigger(rtPort.getBase_Port(), anyEvent));
                connectingConnectingTrans.getTriggers().add(createTrigger(rtPort.getBase_Port(), anyEvent));
                errorErrorTrans.getTriggers().add(createTrigger(rtPort.getBase_Port(), anyEvent));

                mainRegion.getTransitions().add(connectingConnectingTrans);
                mainRegion.getTransitions().add(connectedConnectedTrans);
                mainRegion.getTransitions().add(errorErrorTrans);
            }
        }
    }

    protected Trigger createTrigger(Port port, Event event) {
        Trigger trigger = UMLFactory.eINSTANCE.createTrigger();
        trigger.getPorts().add((Port)copier.get(port));
        trigger.setEvent(event);
        return trigger;
    }

    protected Transition createInitialConnectingTransition(Vertex source, Vertex target) {
        Transition transition = createTransition(source, target);
        Map<String, Object> params = new HashMap<>();
        params.put("serviceNames", serviceNames);
        int index = proxiedPart.getParent().equals(singletonInstance) ? 0 : singletonInstance.getIndex();
        params.put("portName", singletonInstance.getPart().getName().toUpperCase()+"_"+index);
        transition.setEffect(createEffect(transition, params));
        return transition;
    }

    protected Transition createConnectingConnectedTransition(Vertex source, Vertex target) {
        Transition transition = createTransition(source, target);
        Map<String, Object> params = new HashMap<>();
        params.put("borderPortsCount", Integer.toString(borderPortCount));
        params.put("internalPortsCount", Integer.toString(internalPortCount));
        transition.setEffect(createEffect(transition, params));

        Trigger trigger = UMLFactory.eINSTANCE.createTrigger();
        trigger.getPorts().add(tcpPort);
        trigger.setEvent(UMLRTSLibrary.getEvent(UMLRTSLibrary.SystemProtocols.TCP, "connected"));
        transition.getTriggers().add(trigger);
        return transition;
    }

    protected Transition createConnectingErrorTransition(Vertex source, Vertex target) {
        Transition transition = createTransition(source, target);
        transition.setEffect(createEffect(transition));

        Trigger trigger = UMLFactory.eINSTANCE.createTrigger();
        trigger.getPorts().add(tcpPort);
        trigger.setEvent(UMLRTSLibrary.getEvent(UMLRTSLibrary.SystemProtocols.TCP, "error"));
        transition.getTriggers().add(trigger);
        return transition;
    }

    protected Transition createErrorConnectingTransition(Vertex source, Vertex target) {
        Transition transition = createTransition(source, target);
        transition.setEffect(createEffect(transition));

        Trigger trigger = UMLFactory.eINSTANCE.createTrigger();
        trigger.getPorts().add(timerPort);
        trigger.setEvent(UMLRTSLibrary.getEvent(UMLRTSLibrary.SystemProtocols.Timing, "timeout"));
        transition.getTriggers().add(trigger);
        return transition;
    }

    protected Transition createConnectingConnectingTransition(Vertex source, Vertex target) {
        Transition transition = createTransition(source, target);
        transition.setEffect(createEffect(transition));
        return transition;
    }

    protected Transition createConnectedConnectedTransition(Vertex source, Vertex target) {
        Transition transition = createTransition(source, target);
        transition.setEffect(createEffect(transition));
        return transition;
    }

    protected Transition createConnectedErrorTransition(Vertex source, Vertex target) {
        Transition transition = createTransition(source, target);
        Map<String, Object> params = new HashMap<>();
        params.put("internalPortsCount", Integer.toString(internalPortCount));
        transition.setEffect(createEffect(transition, params));

        Trigger trigger1 = UMLFactory.eINSTANCE.createTrigger();
        Trigger trigger2 = UMLFactory.eINSTANCE.createTrigger();
        trigger1.setEvent(UMLRTSLibrary.getEvent(
                UMLRTSLibrary.SystemProtocols.TCP, "disconnected"));
        trigger2.setEvent(UMLRTSLibrary.getEvent(
                UMLRTSLibrary.SystemProtocols.TCP, "error"));
        trigger1.getPorts().add(tcpPort);
        trigger2.getPorts().add(tcpPort);
        transition.getTriggers().add(trigger1);
        transition.getTriggers().add(trigger2);
        return transition;
    }

    protected Transition createErrorErrorTransition(Vertex source, Vertex target) {
        Transition transition = createTransition(source, target);
        transition.setEffect(createEffect(transition));
        Trigger trigger = UMLFactory.eINSTANCE.createTrigger();
        trigger.setEvent(UMLRTSLibrary.getEvent(
                UMLRTSLibrary.SystemProtocols.TCP, "disconnected"));
        trigger.getPorts().add(tcpPort);
        return transition;
    }

    protected Transition createConnectedConnectedOnReceiveTransition(Vertex source, Vertex target) {
        Transition transition = createTransition(source, target);
        transition.setName(transition.getName()+"_received");
        transition.setEffect(createEffect(transition));

        Trigger trigger = UMLFactory.eINSTANCE.createTrigger();
        trigger.setEvent(UMLRTSLibrary.getEvent(
                UMLRTSLibrary.SystemProtocols.TCP, "received"));
        trigger.getPorts().add(tcpPort);
        transition.getTriggers().add(trigger);
        return transition;
    }

    public void addServiceNames(String partName, ModelInfo.ReplicatedPart proxiedPart) {
        proxiedPart.getInstances().forEach(capsuleInstance -> {
            serviceNames.add(new ServiceNameEntry(partName,
                    capsuleInstance.getIndex(), capsuleInstance.getServiceName()));
        });
    }

    @Override
    public CapsulePart createCapsulePart(String partName, ModelInfo.ReplicatedPart proxiedPart) {
        addServiceNames(partName, proxiedPart);
        return super.createCapsulePart(partName, proxiedPart);
    }

    @Override
    public void commit() {
        internalPortCount += systemPortsCount;
        proxiedPart.getRTPorts().forEach(rtPort -> {
            if (!UMLRTSLibrary.isSystemPort(rtPort)) {
                RTPort proxyRTPort = (RTPort) copier.get(rtPort);
                Port proxyPort = (Port) copier.get(rtPort.getBase_Port());
                proxyPort.setLower(1);
                proxyPort.setUpper(1);

                if (proxyPort.isService() && proxyRTPort.isWired())
                    borderPortCount++;
                else
                    internalPortCount++;

            }
        });

        createBehaviour();
        super.commit();
    }
}