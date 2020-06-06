package anon961.kubert;

import anon961.kubert.proxies.ProxyCapsule;
import anon961.kubert.proxies.TCPProxyCapsule;
import anon961.kubert.proxies.TCPServerProxyCapsule;
import anon961.kubert.stubs.FrameStub;
import anon961.kubert.utils.NameUtils;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.*;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.*;
import org.eclipse.papyrusrt.umlrt.profile.statemachine.UMLRTStateMachines.*;
import org.eclipse.uml2.uml.*;
import org.eclipse.uml2.uml.Class;

import java.util.*;

public class SingletonModel {
    private ModelInfo.ReplicatedPart replicatedPart;
    private ModelInfo.CapsuleInstance capsuleInstance;

    private Model model;
    private Capsule topCapsule;
    private Class topCapsuleClass;
    private Class wrapperCapsuleClass;

    private Capsule singletonCapsule;
    private Class singletonCapsuleClass;
    private CapsulePart singletonPart;
    private Property singletonProperty;
    private CapsulePart parentProxyPart;
    private Collection<CapsulePart> siblingProxyParts = new ArrayList<>();
    private Collection<CapsulePart> childrenProxyParts = new ArrayList<>();
    private Collection<Artifact> otherArtifacts = new ArrayList<>();

    private Map<Capsule, ProxyCapsule> capsuleTCPProxyCapsuleMap = new HashMap<>();
    private Map<Capsule, ProxyCapsule> capsuleTCPServerProxyCapsuleMap = new HashMap<>();
    private Map<ModelInfo.ReplicatedPart, ProxyCapsule> partProxyCapsuleMap = new HashMap<>();
    private Map<ModelInfo.ReplicatedPart, Property> partProxyPropertyMap = new HashMap<>();

    List<EObject> rtElements = new ArrayList<>();

    private int connectorId = 1;

    private EcoreUtil.Copier copier = new EcoreUtil.Copier() {
        /*
         * We will use URI fragments instead of IDs
         */
        @Override
        public EObject copy(EObject eObject) {
            EObject cpy = super.copy(eObject);
            if(EcoreUtil.getID(cpy) != null)
                EcoreUtil.setID(cpy, null);
            return cpy;
        }
    };

    private class RTElementComparator implements Comparator<EObject> {
        @Override
        public int compare(EObject o1, EObject o2) {
            int result = EcoreUtil.getURI(getBase(o1)).toString()
                    .compareTo(EcoreUtil.getURI(getBase(o2)).toString());

            /*
             * special case for CapsuleProperties
             */
            if(result == 0)
                return o1.eClass().getName().compareTo(o2.eClass().getName());

            return result;
        }

        public NamedElement getBase(EObject o) {
            if(o instanceof Capsule)
                return ((Capsule)o).getBase_Class();
            if(o instanceof CapsulePart)
                return ((CapsulePart)o).getBase_Property();
            if(o instanceof RTPort)
                return ((RTPort)o).getBase_Port();
            if(o instanceof Protocol)
                return ((Protocol)o).getBase_Collaboration();
            if(o instanceof ProtocolContainer)
                return ((ProtocolContainer)o).getBase_Package();
            if(o instanceof RTMessageSet)
                return ((RTMessageSet)o).getBase_Interface();
            if(o instanceof RTConnector)
                return ((RTConnector)o).getBase_Connector();
            if(o instanceof RTStateMachine)
                return ((RTStateMachine)o).getBase_StateMachine();
            if(o instanceof RTState)
                return ((RTState)o).getBase_State();
            if(o instanceof RTPseudostate)
                return ((RTPseudostate)o).getBase_Pseudostate();
            if(o instanceof RTTrigger)
                return ((RTTrigger)o).getBase_Operation();
            if(o instanceof RTGuard)
                return ((RTGuard)o).getBase_Constraint();
            if(o instanceof RTRegion)
                return ((RTRegion)o).getBase_Region();
            if(o instanceof AttributeProperties)
                return ((AttributeProperties)o).getBase_Property();
            if(o instanceof CapsuleProperties)
                return ((CapsuleProperties)o).getBase_Class();
            if(o instanceof PassiveClassProperties)
                return ((PassiveClassProperties)o).getBase_Class();
            if(o instanceof ParameterProperties)
                return ((ParameterProperties)o).getBase_Parameter();
            if(o instanceof ArtifactProperties)
                return ((ArtifactProperties)o).getBase_Artifact();
            throw new IllegalArgumentException("Unexpected EObject");
        }
    }

    public SingletonModel(Resource resource, ModelInfo.CapsuleInstance capsuleInstance) {
        this.capsuleInstance = capsuleInstance;
        this.replicatedPart = capsuleInstance.getPart();

        model = UMLFactory.eINSTANCE.createModel();
        model.setName(capsuleInstance.getFullyQualifiedName().toLowerCase());

        EAnnotation languageAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        languageAnnotation.setSource("http://www.eclipse.org/papyrus-rt/language/1.0.0");
        languageAnnotation.getDetails().put("language", "umlrt-cpp");
        model.getEAnnotations().add(languageAnnotation);

        createTop();
        createProtocols();
        createSingleton();
        createProxies();
        createConnectors();

        partProxyCapsuleMap.values().forEach(ProxyCapsule::commit);
        copier.copyReferences();

        String newTypeDefs = "";
        for (Map.Entry<ModelInfo.ReplicatedPart, ProxyCapsule> entry : partProxyCapsuleMap.entrySet()) {
            ModelInfo.ReplicatedPart proxiedPart = entry.getKey();
            ProxyCapsule value = entry.getValue();
            String originalTypeName = proxiedPart._getClass().getName();
            String newTypeName = value.getCapsuleClass().getName();
            newTypeDefs += "#define " + originalTypeName + " " + newTypeName+"\n";
        }

        if(singletonCapsuleClass.getName().equalsIgnoreCase("Room")) {
            newTypeDefs += "#define Player PlayerTCPProxy\n";
            newTypeDefs += "#define CPU CPUTCPProxy\n";
        }

        if(!newTypeDefs.isEmpty()) {
            if(!replicatedPart.getCapsuleProperties().isEmpty()) {
                for (CapsuleProperties capsuleProperties : replicatedPart.getCapsuleProperties()) {
                    CapsuleProperties real = (CapsuleProperties) copier.get(capsuleProperties);
                    String oldPreface = real.getImplementationPreface() != null ? real.getImplementationPreface() : "";
                    real.setImplementationPreface(oldPreface + "\n" + newTypeDefs);
                }
            } else {
//                CapsuleProperties properties = RTCppPropertiesFactory.eINSTANCE.createCapsuleProperties();
//                properties.setBase_Class(singletonCapsuleClass);
//                properties.setImplementationPreface(newTypeDefs);
//                rtElements.add(properties);
            }

        }

        FrameStub frameStub = new FrameStub("FrameStub", capsuleInstance);
        Collection<Property> stubProperties = new ArrayList<>();
        singletonCapsuleClass.getOwnedPorts().forEach(port -> {
            if(port.getType().equals(UMLRTSLibrary.getProtocol(UMLRTSLibrary.SystemProtocols.Frame))) {
                String newPortName = port.getName() + NameUtils.randomString(5);

                Property stubProperty = UMLFactory.eINSTANCE.createProperty();
                stubProperty.setAggregation(port.getAggregation());
                stubProperty.setVisibility(port.getVisibility());
                stubProperty.setType(frameStub.getType());

                OpaqueExpression oe = UMLFactory.eINSTANCE.createOpaqueExpression();
                oe.getLanguages().add("C++");
                oe.getBodies().add("&"+newPortName);
                stubProperty.setDefaultValue(oe);

                stubProperty.setName(port.getName());
                port.setName(newPortName);
                stubProperties.add(stubProperty);
            }
        });

        if(stubProperties.size() > 0) {
            singletonCapsuleClass.getOwnedAttributes().addAll(stubProperties);
            rtElements.addAll(frameStub.getRTElements());
            model.getPackagedElements().add(frameStub.getPackage());
        }

        model.getPackagedElements().add(wrapperCapsuleClass);
        model.getPackagedElements().add(singletonCapsuleClass);

        if(topCapsuleClass != wrapperCapsuleClass)
            model.getPackagedElements().add(topCapsuleClass);

        capsuleTCPProxyCapsuleMap.values().forEach(proxyCapsule -> {
            model.getPackagedElements().add(proxyCapsule.getCapsuleClass());
        });

        capsuleTCPServerProxyCapsuleMap.values().forEach(proxyCapsule -> {
            model.getPackagedElements().add(proxyCapsule.getCapsuleClass());
        });

        model.getPackagedElements().addAll(otherArtifacts);

        rtElements.add(topCapsule);
        rtElements.add(singletonCapsule);
        rtElements.add(singletonPart);
        rtElements.addAll(siblingProxyParts);
        rtElements.addAll(childrenProxyParts);

        capsuleTCPProxyCapsuleMap.values().forEach(proxyCapsule -> {
            rtElements.addAll(proxyCapsule.getRtStereotypeElements());
        });
        capsuleTCPServerProxyCapsuleMap.values().forEach(proxyCapsule -> {
            rtElements.addAll(proxyCapsule.getRtStereotypeElements());
        });

        if(parentProxyPart != null)
            rtElements.add(parentProxyPart);

        Collections.sort(rtElements, new RTElementComparator());
        ECollections.sort(singletonCapsuleClass.getOwnedAttributes(),
                Comparator.comparing(NamedElement::getName));

        partProxyCapsuleMap.values().forEach(proxyCapsule -> {
            ECollections.sort(proxyCapsule.getCapsuleClass().getOwnedAttributes(),
                    Comparator.comparing(NamedElement::getName));
        });

        resource.getContents().add(model);
        resource.getContents().addAll(rtElements);
    }

    public void createTop() {
        topCapsuleClass = UMLFactory.eINSTANCE.createClass();
        topCapsuleClass.setName("Top"+NameUtils.randomString(5));
        topCapsule = UMLRealTimeFactory.eINSTANCE.createCapsule();
        topCapsule.setBase_Class(topCapsuleClass);

        ModelInfo.CapsuleInstance parent = replicatedPart.getParent();
        if(parent != null && parent.getPart().isBehavioural()) {
            ProxyCapsule proxyCapsule =  new TCPServerProxyCapsule(capsuleInstance, parent.getPart());
            capsuleTCPServerProxyCapsuleMap.put(parent.getPart().getCapsule(), proxyCapsule);
            partProxyCapsuleMap.put(parent.getPart(), proxyCapsule);
            wrapperCapsuleClass = proxyCapsule.getCapsuleClass();

            parentProxyPart =  proxyCapsule.createCapsulePart(parent.getPart());
            Property proxyProperty = parentProxyPart.getBase_Property();
            proxyProperty.setUpper(1);
            proxyProperty.setLower(1);
            partProxyPropertyMap.put(parent.getPart(), proxyProperty);
            topCapsuleClass.getOwnedAttributes().add(proxyProperty);
        } else {
            wrapperCapsuleClass = topCapsuleClass;
        }


        EAnnotation topAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        topAnnotation.setSource("UMLRT_Default_top");
        topAnnotation.getDetails().put("top_name", topCapsuleClass.getName());
        model.getEAnnotations().add(topAnnotation);
    }

    public void createProtocols() {
        Collection<Protocol> protocols = new HashSet<>();
        protocols.addAll(replicatedPart.getProtocols());

        Collection<ProtocolContainer> protocolContainers = new HashSet<>();
        protocolContainers.addAll(replicatedPart.getProtocolContainers());

        Collection<RTMessageSet> rtMessageSets = new HashSet<>();
        rtMessageSets.addAll(replicatedPart.getRTMessageSets());

        capsuleInstance.getSiblings().forEach(siblingPart -> {
            protocolContainers.addAll(siblingPart.getProtocolContainers());
        });

        capsuleInstance.getChildren().forEach(childPart -> {
            protocolContainers.addAll(childPart.getProtocolContainers());
        });

        if(replicatedPart.getParent() != null) {
            protocolContainers.addAll(replicatedPart.getParent().getPart().getProtocolContainers());
        }

        protocols.forEach(protocol -> {
            rtElements.add(copier.copy(protocol));
        });

        protocolContainers.forEach(protocolContainer -> {
            rtElements.add(copier.copy(protocolContainer));
        });

        rtMessageSets.forEach(rtMessageSet -> {
            rtElements.add(copier.copy(rtMessageSet));
        });
    }

    public void createSingleton() {
        replicatedPart._getClass().setClassifierBehavior(null);
        singletonCapsuleClass = (Class) copier.copy(replicatedPart._getClass());
        singletonCapsule = (Capsule) copier.copy(replicatedPart.getCapsule());

        EAnnotation partAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        partAnnotation.setSource("kubert");
        partAnnotation.getDetails().put("part", capsuleInstance.getFullyQualifiedName());
        singletonCapsuleClass.getEAnnotations().add(partAnnotation);

        singletonPart = (CapsulePart) copier.copy(replicatedPart.getCapsulePart());
        singletonProperty = (Property) copier.copy(replicatedPart.getProperty());
        wrapperCapsuleClass.getOwnedAttributes().add(singletonProperty);
        singletonProperty.setUpper(1);
        singletonProperty.setLower(1);

        singletonCapsuleClass.getOwnedConnectors().clear();

        capsuleInstance.getChildren().forEach(child -> {
            singletonCapsuleClass.getOwnedAttributes().remove(copier.get(child.getProperty()));
            copier.remove(child.getProperty());
        });

        replicatedPart.getRTPorts().forEach(rtPort -> {
            rtElements.add(copier.copy(rtPort));
        });

        replicatedPart.getCapsuleProperties().forEach(props -> {
            rtElements.add(copier.copy(props));
        });

        replicatedPart.getCapsuleAttributeProperties().forEach(props -> {
            rtElements.add(copier.copy(props));
        });

        replicatedPart.getCapsuleArtifactProperties().forEach(props -> {
            otherArtifacts.add((Artifact) copier.copy(props.getBase_Artifact()));
            rtElements.add(copier.copy(props));
        });

//        replicatedPart.getCapsulePassiveClassProperties().forEach(props -> {
//            rtElements.add(copier.copy(props));
//        });

        if(!replicatedPart.isPlugin()) {
            replicatedPart.getRTStateMachines().forEach(rtStateMachine -> {
                rtElements.add(copier.copy(rtStateMachine));
            });

            replicatedPart.getRTStateMachineStereotypes().forEach(eObject -> {
                rtElements.add(copier.copy(eObject));
            });
        }
        else
            makeProxy();
    }

    public void createProxies() {

        Map<String, Integer> partIdCounterMap = new HashMap<>();
        wrapperCapsuleClass.getOwnedAttributes().forEach(property -> {
            int i = partIdCounterMap.getOrDefault(property.getName(), -1);
            partIdCounterMap.put(property.getName(), i + 1);

            if(i != -1) {
                property.setName(property.getName() + "_" + i);
            }
        });

        capsuleInstance.getSiblings().forEach(siblingPart -> {
//            if(siblingPart.isBehavioural()) {
                Capsule siblingCapsule = siblingPart.getCapsule();
                boolean isServer = capsuleInstance.getFullyQualifiedName()
                        .compareTo(siblingPart.getFullyQualifiedName()) > 0;

                if(isServer && !capsuleTCPServerProxyCapsuleMap.containsKey(siblingCapsule)) {
                    capsuleTCPServerProxyCapsuleMap.put(siblingCapsule,
                            new TCPServerProxyCapsule(capsuleInstance, siblingPart));
                } else if(!isServer && !capsuleTCPProxyCapsuleMap.containsKey(siblingCapsule)) {
                    capsuleTCPProxyCapsuleMap.put(siblingCapsule, new TCPProxyCapsule(capsuleInstance, siblingPart));
                }

                int i = partIdCounterMap.getOrDefault(siblingPart.getProperty().getName(), -1);
                partIdCounterMap.put(siblingPart.getProperty().getName(), i + 1);
                String propertyName = i == -1 ? siblingPart.getProperty().getName()
                        : siblingPart.getProperty().getName() + "_" + i;

                ProxyCapsule proxyCapsule = isServer ? capsuleTCPServerProxyCapsuleMap.get(siblingCapsule)
                        : capsuleTCPProxyCapsuleMap.get(siblingCapsule);

                CapsulePart proxyPart =  proxyCapsule.createCapsulePart(propertyName, siblingPart);
                proxyPart.getBase_Property().setLower(proxyPart.getBase_Property().getUpper());
                partProxyCapsuleMap.put(siblingPart, proxyCapsule);
                partProxyPropertyMap.put(siblingPart, proxyPart.getBase_Property());
                siblingProxyParts.add(proxyPart);
//            }
        });

        capsuleInstance.getChildren().forEach(childPart -> {
//            if(childPart.isBehavioural() || childPart.is) {
                Capsule childCapsule = childPart.getCapsule();
                if(!capsuleTCPProxyCapsuleMap.containsKey(childCapsule)) {
                    ProxyCapsule proxyCapsule = new TCPProxyCapsule(capsuleInstance, childPart);
                    capsuleTCPProxyCapsuleMap.put(childCapsule,  proxyCapsule);
                }

                int i = partIdCounterMap.getOrDefault(childPart.getProperty().getName(), -1);
                partIdCounterMap.put(childPart.getProperty().getName(), i + 1);
                String propertyName = i == -1 ? childPart.getProperty().getName()
                        : childPart.getProperty().getName() + "_" + i;

                ProxyCapsule proxyCapsule = capsuleTCPProxyCapsuleMap.get(childCapsule);
                CapsulePart proxyPart =  proxyCapsule.createCapsulePart(propertyName, childPart);
                partProxyCapsuleMap.put(childPart, proxyCapsule);
                partProxyPropertyMap.put(childPart, proxyPart.getBase_Property());
                childrenProxyParts.add(proxyPart);
//            }
        });

        //mirror
        if(replicatedPart.couldBePlugged()) {
            if(!capsuleTCPServerProxyCapsuleMap.containsKey(singletonCapsule)) {
                capsuleTCPServerProxyCapsuleMap.put(singletonCapsule,
                        new TCPServerProxyCapsule(capsuleInstance, capsuleInstance.getPart()));
            }

            ProxyCapsule proxyCapsule = capsuleTCPServerProxyCapsuleMap.get(singletonCapsule);
            proxyCapsule.getCapsuleClass().getOwnedPorts().forEach(port -> {
                if(!UMLRTSLibrary.isSystemPort(port)) {
                    port.setIsConjugated(!port.isConjugated());
                }
            });

            CapsulePart proxyPart =  proxyCapsule.createCapsulePart("plugin",  capsuleInstance.getPart());
            proxyPart.getBase_Property().setUpper(1);
            proxyPart.getBase_Property().setLower(1);
            partProxyCapsuleMap.put( capsuleInstance.getPart(), proxyCapsule);
            partProxyPropertyMap.put( capsuleInstance.getPart(), proxyPart.getBase_Property());
            siblingProxyParts.add(proxyPart);

            replicatedPart.getRTPorts().forEach(rtPort -> {
                if(!UMLRTSLibrary.isSystemPort(rtPort)) {
                    RTPort myRTPort = (RTPort) copier.get(rtPort);
                    Port myPort = (Port) copier.get(rtPort.getBase_Port());
                    myPort.setLower(10);
                    myPort.setUpper(10);

                    RTPort farRTPort = (RTPort) proxyCapsule.getCopier().get(rtPort);
                    Port farPort = (Port) proxyCapsule.getCopier().get(rtPort.getBase_Port());

                    myPort.setIsService(true);
                    myPort.setVisibility(VisibilityKind.PUBLIC_LITERAL);
                    myRTPort.setIsPublish(true);
                    myRTPort.setIsWired(false);
                    myRTPort.setRegistration(PortRegistrationType.AUTOMATIC);
                    if(myRTPort.getRegistrationOverride().isEmpty())
                        myRTPort.setRegistrationOverride(NameUtils.randomString(8));

                    farPort.setIsService(false);
                    farPort.setVisibility(VisibilityKind.PROTECTED_LITERAL);
                    farRTPort.setIsPublish(false);
                    farRTPort.setIsWired(false);
                    farRTPort.setIsNotification(false);
                    farRTPort.setRegistration(PortRegistrationType.APPLICATION);
                    farRTPort.setRegistrationOverride(myRTPort.getRegistrationOverride());
                }
            });
        }

        siblingProxyParts.forEach(proxyPart -> {
            wrapperCapsuleClass.getOwnedAttributes().add(proxyPart.getBase_Property());
        });

        childrenProxyParts.forEach(proxyPart -> {
            singletonCapsuleClass.getOwnedAttributes().add(proxyPart.getBase_Property());
        });
    }

    public void createConnectors() {
        for (Map.Entry<ModelInfo.PortInstance, Set<ModelInfo.PortInstance>>
                entry : capsuleInstance.portFarEndMap.entrySet()) {
            ModelInfo.PortInstance end = entry.getKey();
            Set<ModelInfo.PortInstance> farEnds = entry.getValue();

            for (ModelInfo.PortInstance farEnd : farEnds) {
                Property myProperty = (Property) copier.get(replicatedPart.getProperty());
                Port myPort = (Port) copier.get(end.getPort().getBase_Port());
                RTPort myRTPort = (RTPort) copier.get(end.getPort());

                Property farProperty = partProxyPropertyMap.get(farEnd.getPart());
                ProxyCapsule farProxy = partProxyCapsuleMap.get(farEnd.getPart());
                Port farPort = (Port) farProxy.getCopier().get(farEnd.getPort().getBase_Port());
                RTPort farRTPort = (RTPort) farProxy.getCopier().get(farEnd.getPort());

                if(!farEnd.getPart().isFixed() || !myRTPort.isWired()) {
                    myPort.setIsService(true);
                    myPort.setVisibility(VisibilityKind.PUBLIC_LITERAL);
                    myRTPort.setIsPublish(true);
                    myRTPort.setIsWired(false);
                    myRTPort.setRegistration(PortRegistrationType.AUTOMATIC);
                    if(myRTPort.getRegistrationOverride().isEmpty())
                        myRTPort.setRegistrationOverride(NameUtils.randomString(8));

                    farPort.setIsService(false);
                    farPort.setVisibility(VisibilityKind.PROTECTED_LITERAL);
                    farRTPort.setIsPublish(false);
                    farRTPort.setIsWired(false);
                    farRTPort.setIsNotification(false);
                    farRTPort.setRegistration(PortRegistrationType.APPLICATION);
                    farRTPort.setRegistrationOverride(myRTPort.getRegistrationOverride());
                } else {
                    Connector connector = UMLFactory.eINSTANCE.createConnector();
                    connector.setName("RTConnector_"+connectorId++);
                    ConnectorEnd end1 = connector.createEnd();
                    end1.setRole(myPort);

                    ConnectorEnd end2 = connector.createEnd();
                    end2.setRole(farPort);

                    if (parentProxyPart != null && farProperty.equals(parentProxyPart.getBase_Property())) {
                        end1.setPartWithPort(myProperty);
                        end2.setPartWithPort(null);
                        wrapperCapsuleClass.getOwnedConnectors().add(connector);
                    } else if (farProperty.getOwner().equals(wrapperCapsuleClass)) {
                        end1.setPartWithPort(myProperty);
                        end2.setPartWithPort(farProperty);
                        wrapperCapsuleClass.getOwnedConnectors().add(connector);
                    } else {
                        end1.setPartWithPort(null);
                        end2.setPartWithPort(farProperty);
                        singletonCapsuleClass.getOwnedConnectors().add(connector);
                    }

                    RTConnector rtConnector = UMLRealTimeFactory.eINSTANCE.createRTConnector();
                    rtConnector.setBase_Connector(connector);
                    rtElements.add(rtConnector);
                }
            }
        }
    }

    public void makeProxy() {
        singletonCapsuleClass.setName(singletonCapsuleClass.getName()+"TCPProxy");

        replicatedPart._getClass().getOwnedBehaviors().forEach(behavior -> {
            Behavior realBehavior = (Behavior) copier.get(behavior);
            singletonCapsuleClass.getOwnedBehaviors().remove(realBehavior);
            copier.remove(realBehavior);
        });

        replicatedPart.getRTPorts().forEach(rtPort -> {
            if(UMLRTSLibrary.isSystemPort(rtPort)) {
                RTPort realRTPort = (RTPort) copier.get(rtPort);
                Port realPort = (Port) copier.get(rtPort.getBase_Port());
                singletonCapsuleClass.getOwnedPorts().remove(realPort);
                rtElements.remove(realRTPort);
                copier.remove(realRTPort);
                copier.remove(rtPort);
            }
        });

        TCPProxyCapsule tcpProxyCapsule = new TCPProxyCapsule(capsuleInstance, replicatedPart, copier);
        tcpProxyCapsule.addServiceNames(singletonProperty.getName(), replicatedPart);
        partProxyCapsuleMap.put(replicatedPart, tcpProxyCapsule);
        rtElements.addAll(tcpProxyCapsule.getRtStereotypeElements());
    }

    public Map<String, Integer> createServicePorts() {
        int basePort = 8192;
        int maxPorts = 0;

        for (Property prop : partProxyPropertyMap.values()) {
            maxPorts += prop.getUpper();
        }

        List<Integer> portNumbers = new ArrayList<>(maxPorts);
        for(int i=basePort; i<basePort+maxPorts; i++) {
            portNumbers.add(i);
        }

        Map<String, Integer> servicePorts = new HashMap<>();
        partProxyCapsuleMap.forEach((part, proxyCapsule) -> {
            if(proxyCapsule instanceof TCPServerProxyCapsule) {
                Property proxyProperty = partProxyPropertyMap.get(part);
                for(int i=0; i<proxyProperty.getUpper(); i++) {
                    String portName = proxyProperty.getName().toLowerCase().replace('_', '-') + "-" + i;
                    int port = portNumbers.remove(0);
                    servicePorts.put(portName, port);
                }
            }
        });
        return servicePorts;
    }

    public String getTopName() {
        return topCapsuleClass.getName();
    }

    public Model getModel() {
        return model;
    }
}