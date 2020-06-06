package anon961.kubert;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.*;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.*;
import org.eclipse.papyrusrt.umlrt.profile.statemachine.UMLRTStateMachines.RTStateMachine;
import org.eclipse.papyrusrt.umlrt.profile.statemachine.UMLRTStateMachines.UMLRTStateMachinesPackage;
import org.eclipse.uml2.uml.*;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Package;

import java.util.*;

public class ModelInfo {

    private Resource resource;
    private Model model;
    private String topName;

    private List<ReplicatedPart> capsuleInstances = new ArrayList<>();
    private Map<Class, Capsule> classCapsuleMap = new HashMap<>();
    private Map<Property, CapsulePart> propertyCapsulePartMap = new HashMap<>();
    private Map<Collaboration, Protocol> collaborationProtocolMap = new HashMap<>();
    private Map<Package, ProtocolContainer> packageProtocolContainerMap = new HashMap<>();
    private Map<Interface, RTMessageSet> interfaceRTMessageSetMap = new HashMap<>();
    private Map<StateMachine, RTStateMachine> stateMachineRTStateMachineMap = new HashMap<>();
    private Map<Port, RTPort> portRTPortMap = new HashMap<>();
    private Map<Connector, RTConnector> connectorRTConnectorMap = new HashMap<>();

    private Map<Capsule, Collection<Protocol>> capsuleProtocolsMap = new HashMap<>();
    private Map<Capsule, Collection<ProtocolContainer>> capsuleProtocolContainersMap = new HashMap<>();
    private Map<Capsule, Collection<RTMessageSet>> capsuleRTMessageSetsMap = new HashMap<>();
    private Map<Capsule, Collection<RTPort>> capsuleRTPortsMap = new HashMap<>();
    private Map<Capsule, Collection<RTStateMachine>> capsuleRTStateMachinesMap = new HashMap<>();
    private Map<Capsule, Collection<EObject>> capsuleRTStateMachineStereotypes = new HashMap<>();
    private Map<Capsule, Collection<CallEvent>> capsuleCallEventsMap = new HashMap<>();
    private Map<Capsule, Collection<RTConnector>> capsuleRTConnectorsMap = new HashMap<>();
    private Map<Capsule, Collection<CapsuleProperties>> capsuleCapsulePropertiesMap = new HashMap<>();
    private Map<Capsule, Collection<AttributeProperties>> capsuleAttributePropertiesMap = new HashMap<>();
    private Map<Capsule, Collection<ReplicatedPart>> capsuleReplicatedPartsMap = new HashMap<>();
    private Map<Capsule, Collection<ArtifactProperties>> capsuleArtifactPropertiesMap = new HashMap<>();
    private Map<Capsule, Collection<PassiveClassProperties>> capsulePassiveClassPropertiesMap = new HashMap<>();

    public ReplicatedPart topPart;

    public class ReplicatedPart {
        private String name;
        private String fullyQualifiedName;
        private Property property;
        private CapsuleInstance parent;
        public Set<ReplicatedPart> siblings = new HashSet<>();

        private int instanceCount = 0;
        private List<CapsuleInstance> instances = new ArrayList<>();

        public ReplicatedPart(Property property, CapsuleInstance parent) {
            this.property = property;
            this.parent = parent;
            this.name = property.getName();
            this.fullyQualifiedName = this.name;

            if(parent != null) {
                this.fullyQualifiedName = parent.getFullyQualifiedName()+ "-" + this.fullyQualifiedName;
                parent.addChild(this);
            }
        }

        public CapsuleInstance createInstance() {
            CapsuleInstance capsuleInstance = new CapsuleInstance(this, instanceCount++);
            instances.add(capsuleInstance);
            return capsuleInstance;
        }

        public List<CapsuleInstance> getInstances() {
            return instances;
        }

        public Set<ReplicatedPart> getSiblings() {
            return siblings;
        }

        public CapsulePart getCapsulePart() {
            return propertyCapsulePartMap.get(property);
        }

        public Capsule getCapsule() {
            return classCapsuleMap.get(_getClass());
        }

        public Class _getClass() {
            return (Class) property.getType();
        }

        public Property getProperty() {
            return property;
        }

        public CapsuleInstance getParent() {
            return parent;
        }

        public boolean isBehavioural() {
            return !getRTStateMachines().isEmpty();
        }

        public boolean isOptional() {
            return property.getLower() == 0 && property.getAggregation().equals(AggregationKind.COMPOSITE_LITERAL);
        }

        public boolean isPlugin() {
            return property.getLower() == 0 && property.getAggregation().equals(AggregationKind.SHARED_LITERAL);
        }

        public boolean couldBePlugged() {
            if(!isOptional())
                return false;

            for (ReplicatedPart replicatedPart : capsuleReplicatedPartsMap.get(getCapsule())) {
                if(replicatedPart.isPlugin())
                    return true;
            }
            return false;
        }

        public boolean isTopBehaviour() {
            if(!isBehavioural())
                return false;

            CapsuleInstance parent;
            ReplicatedPart current = this;
            while((parent = current.getParent()) != null) {
                current = parent.getPart();
                if(current.isBehavioural())
                    return false;
            }

            return true;
        }

        public boolean isFixed() {
            return !isPlugin() && !isOptional();
        }

        public Collection<ProtocolContainer> getProtocolContainers() {
            return capsuleProtocolContainersMap.get(getCapsule());
        }

        public Collection<Protocol> getProtocols() {
            return capsuleProtocolsMap.get(getCapsule());
        }

        public Collection<RTMessageSet> getRTMessageSets() {
            return capsuleRTMessageSetsMap.get(getCapsule());
        }

        public Collection<RTPort> getRTPorts() {
            return capsuleRTPortsMap.get(getCapsule());
        }

        public Collection<CapsuleProperties> getCapsuleProperties() {
            return capsuleCapsulePropertiesMap.get(getCapsule());
        }

        public Collection<AttributeProperties> getCapsuleAttributeProperties() {
            return capsuleAttributePropertiesMap.get(getCapsule());
        }

        public Collection<ArtifactProperties> getCapsuleArtifactProperties() {
            return capsuleArtifactPropertiesMap.get(getCapsule());
        }

        public Collection<PassiveClassProperties> getCapsulePassiveClassProperties() {
            return capsulePassiveClassPropertiesMap.get(getCapsule());
        }

        public Collection<RTStateMachine> getRTStateMachines() {
            return capsuleRTStateMachinesMap.get(getCapsule());
        }

        public Collection<EObject> getRTStateMachineStereotypes() {
            return capsuleRTStateMachineStereotypes.get(getCapsule());
        }

        public String getName() {
            return name;
        }

        public String getFullyQualifiedName() {
            return fullyQualifiedName;
        }

        public String getPartAnnotation() {
            String ret = "";
            EAnnotation partAnnotation = _getClass().getEAnnotation("kubert");
            if(partAnnotation != null)
                ret = partAnnotation.getDetails().get("part");
            return ret;
        }

        @Override
        public String toString() {
            return this.fullyQualifiedName;
        }
    }

    public class CapsuleInstance {
        private String name;
        private int index;
        private String fullyQualifiedName;
        private ReplicatedPart replicatedPart;
        private Set<ReplicatedPart> children = new HashSet<>();
        public Map<PortInstance, Set<PortInstance>> portFarEndMap = new HashMap<>();

        public CapsuleInstance(ReplicatedPart replicatedPart, int index) {
            this.replicatedPart = replicatedPart;
            this.index = index;
            this.name = replicatedPart.getName() + index;
            this.fullyQualifiedName = replicatedPart.getFullyQualifiedName() + index;
        }

        public void addChild(ReplicatedPart child) {
            children.add(child);
        }

        public Set<ReplicatedPart> getChildren() {
            return children;
        }

        public ReplicatedPart getChildWithProperty(Property property) {
            for (ReplicatedPart childPart : getChildren()) {
                if (childPart.getProperty().equals(property))
                    return childPart;
            }
            return null;
        }

        public boolean isConnectedTo(CapsuleInstance instance) {
            for (Set<PortInstance> portInstances : portFarEndMap.values()) {
                for (PortInstance portInstance : portInstances) {
                    if(portInstance.getPart().equals(instance.getPart()))
                        return true;
                }
            }

            return false;
        }

        public Collection<ReplicatedPart> getSiblings() {
            return replicatedPart.getSiblings();
        }

        public Property getProperty() {
            return replicatedPart.getProperty();
        }

        public ReplicatedPart getPart() {
            return replicatedPart;
        }

        public String getName() {
            return name;
        }

        public int getIndex() {
            return index;
        }

        public String getFullyQualifiedName() {
            return fullyQualifiedName;
        }

        public String getServiceName() {
            return getFullyQualifiedName()
                    .toUpperCase().replaceAll("-", "_");
        }


        @Override
        public String toString() {
            if(getPart().getParent() == null)
                return getPart()._getClass().getNearestPackage().getName()
                        + "\n"
                        + getPart()._getClass().getName();
            return getPart().getName()
                    + "[" + getIndex() + "]: "
                    + getPart()._getClass().getName();
        }
    }

    public class PortInstance {
        private ReplicatedPart part;
        private RTPort port;

        public PortInstance(ReplicatedPart part, RTPort port) {
            this.part = part;
            this.port = port;
        }

        public ReplicatedPart getPart() {
            return part;
        }

        public RTPort getPort() {
            return port;
        }

        public void setPart(ReplicatedPart part) {
            this.part = part;
        }

        public void setPort(RTPort port) {
            this.port = port;
        }

        public boolean isRelay() {
            return port.getBase_Port().isService() && !port.getBase_Port().isBehavior();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PortInstance that = (PortInstance) o;
            return Objects.equals(part, that.part) &&
                    port.equals(that.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(part, port);
        }
    }

    private ModelInfo(Resource resource, String topName) {
        this.resource = resource;
        this.topName = topName;
    }

    public static ModelInfo load(Resource resource, String topName) {
        ModelInfo modelInfo = new ModelInfo(resource, topName);
        modelInfo.load();
        return modelInfo;
    }

    private void load() {
        model = (Model) EcoreUtil.getObjectByType(resource.getContents(), UMLPackage.Literals.MODEL);

        Collection<Capsule> capsules = EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRealTimePackage.Literals.CAPSULE);
        Collection<CapsulePart> capsuleParts = EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRealTimePackage.Literals.CAPSULE_PART);
        Collection<Protocol> protocols = EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRealTimePackage.Literals.PROTOCOL);
        Collection<ProtocolContainer> protocolContainers = EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRealTimePackage.Literals.PROTOCOL_CONTAINER);
        Collection<RTMessageSet> rtMessageSets = EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRealTimePackage.Literals.RT_MESSAGE_SET);
        Collection<RTPort> rtPorts = EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRealTimePackage.Literals.RT_PORT);
        Collection<RTConnector> rtConnectors = EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRealTimePackage.Literals.RT_CONNECTOR);
        Collection<RTStateMachine> rtStateMachines = EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRTStateMachinesPackage.Literals.RT_STATE_MACHINE);

        Collection<CapsuleProperties> capsuleProperties = EcoreUtil.getObjectsByType(
                resource.getContents(), RTCppPropertiesPackage.Literals.CAPSULE_PROPERTIES);
        Collection<AttributeProperties> attributeProperties = EcoreUtil.getObjectsByType(
                resource.getContents(), RTCppPropertiesPackage.Literals.ATTRIBUTE_PROPERTIES);
        Collection<ArtifactProperties> artifactProperties = EcoreUtil.getObjectsByType(
                resource.getContents(), RTCppPropertiesPackage.Literals.ARTIFACT_PROPERTIES);
        Collection<PassiveClassProperties> passiveClassProperties = EcoreUtil.getObjectsByType(
                resource.getContents(), RTCppPropertiesPackage.Literals.PASSIVE_CLASS_PROPERTIES);

        Collection<EObject> rtStateMachineStereotypes = EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRTStateMachinesPackage.Literals.RT_STATE);
        rtStateMachineStereotypes.addAll(EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRTStateMachinesPackage.Literals.RT_PSEUDOSTATE));
        rtStateMachineStereotypes.addAll(EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRTStateMachinesPackage.Literals.RT_REGION));
        rtStateMachineStereotypes.addAll(EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRTStateMachinesPackage.Literals.RT_TRIGGER));
        rtStateMachineStereotypes.addAll(EcoreUtil.getObjectsByType(
                resource.getContents(), UMLRTStateMachinesPackage.Literals.RT_GUARD));

        protocols.forEach(protocol -> {
            collaborationProtocolMap.put(protocol.getBase_Collaboration(), protocol);
        });

        protocolContainers.forEach(protocolContainer -> {
            packageProtocolContainerMap.put(protocolContainer.getBase_Package(), protocolContainer);
        });

        rtMessageSets.forEach(rtMessageSet -> {
            interfaceRTMessageSetMap.put(rtMessageSet.getBase_Interface(), rtMessageSet);
        });

        rtStateMachines.forEach(rtStateMachine -> {
            stateMachineRTStateMachineMap.put(rtStateMachine.getBase_StateMachine(), rtStateMachine);
        });

        rtPorts.forEach(rtPort -> {
            portRTPortMap.put(rtPort.getBase_Port(), rtPort);
        });

        rtConnectors.forEach(rtConnector -> {
            connectorRTConnectorMap.put(rtConnector.getBase_Connector(), rtConnector);
        });

        capsuleParts.forEach(capsulePart -> {
            propertyCapsulePartMap.put(capsulePart.getBase_Property(), capsulePart);
        });

        capsules.forEach(capsule -> {
            classCapsuleMap.put(capsule.getBase_Class(), capsule);

            Set<Protocol> capsuleProtocols = new HashSet<>();
            capsuleProtocolsMap.put(capsule, capsuleProtocols);

            Set<ProtocolContainer> capsuleProtocolContainers = new HashSet<>();
            capsuleProtocolContainersMap.put(capsule, capsuleProtocolContainers);

            Set<RTMessageSet> capsuleRTMessageSets = new HashSet<>();
            capsuleRTMessageSetsMap.put(capsule, capsuleRTMessageSets);

            Set<RTConnector> capsuleRTConnectors = new HashSet<>();
            capsuleRTConnectorsMap.put(capsule, capsuleRTConnectors);

            Set<RTPort> capsuleRTPorts = new HashSet<>();
            capsuleRTPortsMap.put(capsule, capsuleRTPorts);

            Set<RTStateMachine> capsuleRTStateMachines = new HashSet<>();
            capsuleRTStateMachinesMap.put(capsule, capsuleRTStateMachines);

            Set<CallEvent> capsuleCallEvents = new HashSet<>();
            capsuleCallEventsMap.put(capsule, capsuleCallEvents);

            Set<CapsuleProperties> capsuleProps = new HashSet<>();
            capsuleCapsulePropertiesMap.put(capsule, capsuleProps);

            Set<AttributeProperties> capsuleAttrProps = new HashSet<>();
            capsuleAttributePropertiesMap.put(capsule, capsuleAttrProps);

            Set<ArtifactProperties> capsuleArtifactProperties = new HashSet<>();
            capsuleArtifactPropertiesMap.put(capsule, capsuleArtifactProperties);

            Set<PassiveClassProperties> capsulePassiveClassProperties = new HashSet<>();
            capsulePassiveClassPropertiesMap.put(capsule, capsulePassiveClassProperties);

            Set<ReplicatedPart> replicatedParts = new HashSet<>();
            capsuleReplicatedPartsMap.put(capsule, replicatedParts);

            capsule.getBase_Class().getOwnedPorts().forEach(port -> {
                if(!UMLRTSLibrary.isSystemPort(portRTPortMap.get(port))) {
                    capsuleProtocols.add(collaborationProtocolMap.get(port.getType()));
                    capsuleProtocolContainers.add(packageProtocolContainerMap.get(port.getType().getPackage()));
                }
            });

            capsule.getBase_Class().getOwnedPorts().forEach(port -> {
                capsuleRTPorts.add(portRTPortMap.get(port));
            });

            capsule.getBase_Class().getOwnedConnectors().forEach(connector -> {
                if(connector.getEnds().get(0).getPartWithPort() == null
                    || connector.getEnds().get(1).getPartWithPort() == null)
                    capsuleRTConnectors.add(connectorRTConnectorMap.get(connector));
            });

            capsuleProtocolContainers.forEach(protocolContainer -> {
                protocolContainer.getBase_Package().getPackagedElements().forEach(pe -> {
                    if(pe instanceof Interface)
                        capsuleRTMessageSets.add(interfaceRTMessageSetMap.get(pe));
                });
            });

            capsule.getBase_Class().getOwnedBehaviors().forEach(behavior -> {
                if(behavior instanceof StateMachine)
                    capsuleRTStateMachines.add(stateMachineRTStateMachineMap.get(behavior));
            });

            capsuleProperties.forEach(props -> {
                if(props.getBase_Class().equals(capsule.getBase_Class()))
                    capsuleProps.add(props);
            });

            attributeProperties.forEach(props -> {
                if(props.getBase_Property().getClass_().equals(capsule.getBase_Class()))
                    capsuleAttrProps.add(props);
            });

            passiveClassProperties.forEach(props->{
                capsulePassiveClassProperties.add(props);
            });

            artifactProperties.forEach(props -> {
                capsuleArtifactProperties.add(props);
            });

            capsuleRTPorts.forEach(rtPort -> {
                if (!UMLRTSLibrary.isSystemPort(rtPort)) {
                    Package protocolPkg = rtPort.getBase_Port().getType().getPackage();
                    Collection<CallEvent> callEvents = EcoreUtil.getObjectsByType(
                            protocolPkg.getPackagedElements(), UMLPackage.Literals.CALL_EVENT);

                    EcoreUtil.getObjectsByType(protocolPkg.getPackagedElements(),
                            UMLPackage.Literals.INTERFACE).forEach( i -> {
                        RTMessageSet messageSet = interfaceRTMessageSetMap.get(i);
                        if(messageSet != null) {
                            switch (messageSet.getRtMsgKind()) {
                                case OUT:
                                    if(rtPort.getBase_Port().isConjugated())
                                        capsuleCallEvents.addAll(callEvents);
                                    break;
                                default:
                                    capsuleCallEvents.addAll(callEvents);
                            }
                        }
                    });
                }
            });
        });

        capsuleRTStateMachinesMap.forEach((capsule, stateMachines) -> {
            if(!capsuleRTStateMachineStereotypes.containsKey(capsule))
                capsuleRTStateMachineStereotypes.put(capsule, new HashSet<>());
            Collection<EObject> capsuleRTStateMachineStereotype = capsuleRTStateMachineStereotypes.get(capsule);

            stateMachines.forEach(stateMachine -> {
                if(stateMachine != null)
                stateMachine.getBase_StateMachine().eAllContents().forEachRemaining(eObject -> {
                    Collection<EStructuralFeature.Setting> references =
                            EcoreUtil.UsageCrossReferencer.find(eObject, rtStateMachineStereotypes);
                    references.forEach(setting -> {
                        capsuleRTStateMachineStereotype.add(setting.getEObject());
                    });
                });
            });
        });

        Capsule topCapsule = null;
        EAnnotation topAnnotation = model.getEAnnotation("UMLRT_Default_top");
        if(topAnnotation != null && topAnnotation.getDetails().containsKey("top_name")) {
            topName = topAnnotation.getDetails().get("top_name");
        }

        for (Capsule capsule : capsules) {
            if(capsule.getBase_Class().getName().equals(topName)) {
                topCapsule = capsule;
                break;
            }
        }
        if(topCapsule == null)
            throw new RuntimeException("Top capsule not found");

        capsuleInstances = createInstances(topCapsule);
        topPart = capsuleInstances.get(0);
        resolveFarEnds(topPart.getInstances().get(0));
        resolveRelayEnds(topPart.getInstances().get(0));

        capsuleInstances.forEach(replicatedPart -> {
            capsuleReplicatedPartsMap.get(replicatedPart.getCapsule()).add(replicatedPart);
        });

        /*
         * delete relay ports
         */
        capsuleInstances.forEach(replicatedPart -> {
            replicatedPart.getInstances().forEach(instance -> {
                instance.portFarEndMap.keySet().removeIf(PortInstance::isRelay);
            });
        });

        /*
         * fill wired siblings
         */
        capsuleInstances.forEach(replicatedPart -> {
            CapsuleInstance capsuleInstance = replicatedPart.getInstances().get(0);
            capsuleInstance.portFarEndMap.forEach((end, farEnds) -> {
                farEnds.forEach(farEnd -> {
                    if(!end.getPart().getInstances().contains(farEnd.getPart().getParent())
                            && !farEnd.getPart().getInstances().contains(end.getPart().getParent())) {
                        replicatedPart.getSiblings().add(farEnd.getPart());
                    }
                });
            });
        });

        /*
         * fill non-wired siblings
         */
        capsuleRTPortsMap.forEach((capsule, ports) -> {
            ports.forEach(rtPort -> {
                if(!UMLRTSLibrary.isSystemPort(rtPort)
                        && rtPort.getBase_Port().isBehavior()
                        && !rtPort.isWired()) {
                    Set<RTPort> matchingPorts = getMatchingNonWiredPorts(rtPort);
                    matchingPorts.forEach(matchingPort -> {
                        Capsule otherCapsule = classCapsuleMap.get(matchingPort.getBase_Port().getClass_());
                        capsuleReplicatedPartsMap.get(capsule).forEach(myPart -> {
                            capsuleReplicatedPartsMap.get(otherCapsule).forEach(otherPart -> {
                                PortInstance mine = new PortInstance(myPart, rtPort);
                                PortInstance their = new PortInstance(otherPart, matchingPort);

                                myPart.getInstances().forEach(instance -> {
                                    if(!instance.portFarEndMap.containsKey(mine))
                                        instance.portFarEndMap.put(mine, new HashSet<>());
                                    instance.portFarEndMap.get(mine).add(their);
                                });

                                if(!myPart.getParent().getPart().equals(otherPart)
                                        && !otherPart.getParent().equals(myPart))
                                    myPart.getSiblings().add(otherPart);
                            });
                        });
                    });
                }
            });
        });
    }

    private void resolveFarEnds(CapsuleInstance instance) {
        for (ReplicatedPart child : instance.getChildren()) {
            for (CapsuleInstance childInstance : child.getInstances()) {
                resolveFarEnds(childInstance);
            }
        }

        CapsuleInstance parent = instance.getPart().getParent();
        if(parent != null) {
            Class parentCapsuleClass = parent.getPart()._getClass();
            Collection<EStructuralFeature.Setting> references =
                    EcoreUtil.UsageCrossReferencer.find(instance.getProperty(), parentCapsuleClass.getOwnedConnectors());

            references.forEach(setting -> {
                ConnectorEnd connectorEnd = (ConnectorEnd) setting.getEObject();
                Connector connector = (Connector) connectorEnd.getOwner();
                ConnectorEnd farEnd = connectorEnd.equals(connector.getEnds().get(0))
                        ? connector.getEnds().get(1) : connector.getEnds().get(0);

                PortInstance mine = new PortInstance(instance.getPart(), portRTPortMap.get(connectorEnd.getRole()));
                PortInstance their = new PortInstance(parent.getChildWithProperty
                        (farEnd.getPartWithPort()), portRTPortMap.get(farEnd.getRole()));

                if(!instance.portFarEndMap.containsKey(mine))
                    instance.portFarEndMap.put(mine, new HashSet<>());
                instance.portFarEndMap.get(mine).add(their);
            });
        }

        Class myClass = instance.getPart()._getClass();
        myClass.getOwnedConnectors().forEach(connector -> {
            ConnectorEnd connectorEnd = connector.getEnds().get(0).getPartWithPort() == null
                    ? connector.getEnds().get(0) : connector.getEnds().get(1);
            ConnectorEnd farEnd = connectorEnd.equals(connector.getEnds().get(0))
                    ? connector.getEnds().get(1) : connector.getEnds().get(0);

            if(connectorEnd.getPartWithPort() == null) {
                PortInstance mine = new PortInstance(instance.getPart(), portRTPortMap.get(connectorEnd.getRole()));
                PortInstance their = new PortInstance(instance.getChildWithProperty(
                        farEnd.getPartWithPort()), portRTPortMap.get(farEnd.getRole()));

                if(!instance.portFarEndMap.containsKey(mine))
                    instance.portFarEndMap.put(mine, new HashSet<>());
                instance.portFarEndMap.get(mine).add(their);
            }
        });

        for (ReplicatedPart child : instance.getChildren()) {
            for (CapsuleInstance childInstance : child.getInstances()) {
                childInstance.portFarEndMap.forEach((portInstance, farPortInstances) -> {
                    for (PortInstance farPortInstance : farPortInstances) {
                        if (farPortInstance.getPart() == null) {
                            farPortInstance.setPart(instance.getPart());
                        }
                    }
                });
            }
        }
    }

    private void resolveRelayEnds(CapsuleInstance instance) {
        for (ReplicatedPart child : instance.getChildren()) {
            for (CapsuleInstance childInstance : child.getInstances()) {
                resolveRelayEnds(childInstance);
            }
        }

        instance.portFarEndMap.forEach((myEnd, farEnds) -> {
            Set<PortInstance> resolvedEnds = new HashSet<>();
            farEnds.forEach(farEnd -> {
                resolvedEnds.addAll(resolveRelayEnd(instance, farEnd));
            });
            instance.portFarEndMap.put(myEnd, resolvedEnds);
        });
    }

    private Set<PortInstance> resolveRelayEnd(CapsuleInstance instance, PortInstance farEnd) {
        if(!farEnd.isRelay())
            return Collections.singleton(farEnd);

        Set<PortInstance> resolvedEnds = new HashSet<>();
        CapsuleInstance instanceParent = instance.getPart().getParent();

        if(farEnd.getPart().getInstances().contains(instanceParent)) { // child -> parent
            Set<PortInstance> candidates = instanceParent.portFarEndMap.get(farEnd);
            candidates.forEach(newEnd -> {
                if(newEnd.getPart().getParent().equals(instanceParent.getPart().getParent())) {
                    resolvedEnds.addAll(resolveRelayEnd(instanceParent, newEnd));
                }
            });
        } else { // parent -> child
            farEnd.getPart().getInstances().forEach(instance1 -> {
                Set<PortInstance> candidates = instance1.portFarEndMap.get(farEnd);
                candidates.forEach(newEnd -> {
                    if(newEnd.getPart().getParent().equals(instance1)) {
                        resolvedEnds.addAll(resolveRelayEnd(instanceParent, newEnd));
                    }
                });
            });
        }

        return resolvedEnds;
    }

    private Set<RTPort> getMatchingNonWiredPorts(RTPort targetPort) {
        Set<RTPort> matchingPorts = new HashSet<>();
        portRTPortMap.forEach((port, rtPort) -> {
            if(!rtPort.isWired() && port.isBehavior()) {
                if(!rtPort.equals(targetPort)
                        && port.isService() != targetPort.getBase_Port().isService()
                        && port.isConjugated() != targetPort.getBase_Port().isConjugated()
                        && port.getType().equals(targetPort.getBase_Port().getType())
                        && (port.getName().equals(targetPort.getBase_Port().getName()) ||
                            rtPort.getRegistrationOverride().equals(targetPort.getRegistrationOverride()))) {
                    matchingPorts.add(rtPort);
                }
            }
        });
        return matchingPorts;
    }

    private List<ReplicatedPart> createInstances(Capsule topCapsule) {
        List<ReplicatedPart> capsuleInstances = new ArrayList<>();
        Property topProperty = UMLFactory.eINSTANCE.createProperty();
        topProperty.setType(topCapsule.getBase_Class());
        topProperty.setName(topName);

        CapsulePart topPart = UMLRealTimeFactory.eINSTANCE.createCapsulePart();
        topPart.setBase_Property(topProperty);
        propertyCapsulePartMap.put(topProperty, topPart);

        capsuleInstances.addAll(createInstances(topProperty, null));
        return capsuleInstances;
    }

    private Collection<ReplicatedPart> createInstances(Property current, CapsuleInstance parent) {
        List<ReplicatedPart> instances = new ArrayList<>();;
        List<Property> propertyParts = new ArrayList<>();;

        Class cls = (Class) current.getType();
        cls.getOwnedAttributes().forEach(property -> {
            if(isCapsulePart(property)) {
                propertyParts.add(property);
            }
        });

        ReplicatedPart replicatedPart = new ReplicatedPart(current, parent);
        instances.add(replicatedPart);

        for (int i = 0; i < current.getUpper(); i++) {
            CapsuleInstance instance = replicatedPart.createInstance();
            propertyParts.forEach(property -> {
                instances.addAll(createInstances(property, instance));
            });
        }
        return instances;
    }

    public Collection<ReplicatedPart> getCapsuleInstances() {
        return capsuleInstances;
    }

    private boolean isCapsulePart(Property property) {
        return propertyCapsulePartMap.containsKey(property);
    }

    public Model getModel() {
        return model;
    }

    public ReplicatedPart getTopPart() {
        return topPart;
    }

    public CapsuleInstance getCapsuleInstance(String fqn) {
        for (ReplicatedPart replicatedPart : getCapsuleInstances()) {
            for (CapsuleInstance instance : replicatedPart.getInstances()) {
                if(instance.getFullyQualifiedName().equals(fqn))
                    return instance;
            }
        }

        return null;
    }

    public CapsuleInstance getCapsuleInstanceWithPartAnnotation(String fqn) {
        for (ReplicatedPart replicatedPart : getCapsuleInstances()) {
            for (CapsuleInstance instance : replicatedPart.getInstances()) {
                String partialFqn = fqn.indexOf('-') == -1 ? fqn : fqn.substring(fqn.indexOf('-'));
                if(new String(instance.getPart().getPartAnnotation()
                        + instance.getIndex()).endsWith(partialFqn))
                    return instance;
            }
        }

        return null;
    }
}
