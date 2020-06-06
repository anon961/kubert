package anon961.kubert.proxies;

import anon961.kubert.ModelInfo;
import anon961.kubert.UMLRTSLibrary;
import anon961.kubert.generators.ActionCodeGenerator;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.*;
import org.eclipse.papyrusrt.umlrt.profile.statemachine.UMLRTStateMachines.RTPseudostate;
import org.eclipse.papyrusrt.umlrt.profile.statemachine.UMLRTStateMachines.RTState;
import org.eclipse.papyrusrt.umlrt.profile.statemachine.UMLRTStateMachines.UMLRTStateMachinesFactory;
import org.eclipse.uml2.uml.*;
import org.eclipse.uml2.uml.Class;

import java.util.*;

public abstract class ProxyCapsule {
    protected ModelInfo.ReplicatedPart proxiedPart;
    protected Class capsuleClass;
    protected Capsule capsule;

    protected ActionCodeGenerator actionCodeGenerator;
    protected Collection<EObject> rtStereotypeElements = new HashSet<>();
    protected Collection<ModelInfo.ReplicatedPart> instances = new ArrayList<>();
    protected int systemPortsCount = 0;

    protected EcoreUtil.Copier copier = new EcoreUtil.Copier() {
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

    protected ProxyCapsule(ModelInfo.ReplicatedPart proxiedPart, EcoreUtil.Copier copier) {
        this.proxiedPart = proxiedPart;
        this.copier = copier;

        capsule = (Capsule) copier.get(proxiedPart.getCapsule());
        capsuleClass = (Class) copier.get(proxiedPart._getClass());
        actionCodeGenerator = new ActionCodeGenerator(this);

        EAnnotation partAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        partAnnotation.setSource("kubert");
        partAnnotation.getDetails().put("part", proxiedPart.getFullyQualifiedName());
        capsuleClass.getEAnnotations().add(partAnnotation);
    }

    protected ProxyCapsule(String name, ModelInfo.ReplicatedPart proxiedPart) {
        this.proxiedPart = proxiedPart;
        actionCodeGenerator = new ActionCodeGenerator(this);

        capsuleClass = UMLFactory.eINSTANCE.createClass();
        capsuleClass.setName(name);
        capsuleClass.setIsActive(true);

        capsule = UMLRealTimeFactory.eINSTANCE.createCapsule();
        capsule.setBase_Class(capsuleClass);
        rtStereotypeElements.add(capsule);

        EAnnotation partAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        partAnnotation.setSource("kubert");
        partAnnotation.getDetails().put("part", proxiedPart.getFullyQualifiedName());
        capsuleClass.getEAnnotations().add(partAnnotation);

        proxiedPart.getRTPorts().forEach(rtPort -> {
            if(!UMLRTSLibrary.isSystemPort(rtPort)) {
                Port proxyPort = (Port) copier.copy(rtPort.getBase_Port());
                capsuleClass.getOwnedPorts().add(proxyPort);

                RTPort proxyRTPort = (RTPort) copier.copy(rtPort);
                proxyRTPort.setRegistration(PortRegistrationType.AUTOMATIC);
                proxyRTPort.setIsNotification(false);
                rtStereotypeElements.add(proxyRTPort);
            }
        });

        proxiedPart._getClass().getOwnedAttributes().forEach(property -> {
            if(UMLRTSLibrary.isPrimitiveType(property.getType()))
                capsuleClass.getOwnedAttributes().add((Property) copier.copy(property));
        });
    }

    public CapsulePart createCapsulePart(ModelInfo.ReplicatedPart proxiedPart) {
        return createCapsulePart(proxiedPart.getProperty().getName(), proxiedPart);
    }

    public CapsulePart createCapsulePart(String partName, ModelInfo.ReplicatedPart proxiedPart) {
        Property property = UMLFactory.eINSTANCE.createProperty();
        property.setName(partName);
        property.setUpper(proxiedPart.getProperty().getUpper());
        property.setLower(proxiedPart.getProperty().getLower());
        property.setType(capsuleClass);
        property.setAggregation(AggregationKind.COMPOSITE_LITERAL);
        property.setVisibility(VisibilityKind.PROTECTED_LITERAL);
        property.setIsOrdered(true);

        CapsulePart part = UMLRealTimeFactory.eINSTANCE.createCapsulePart();
        part.setBase_Property(property);
        instances.add(proxiedPart);
        return part;
    }

    public void commit() {
        copier.copyReferences();
    }

    public EcoreUtil.Copier getCopier() {
        return copier;
    }

    public Class getCapsuleClass() {
        return capsuleClass;
    }

    public Capsule getCapsule() {
        return capsule;
    }

    public Collection<EObject> getRtStereotypeElements() {
        return rtStereotypeElements;
    }

    protected void createInternalPorts() {
        Port logPort = UMLRTSLibrary.createPort("log", UMLRTSLibrary.SystemProtocols.Log);
        logPort.setIsService(true);
        capsuleClass.getOwnedPorts().add(logPort);

        RTPort rtLogPort = UMLRealTimeFactory.eINSTANCE.createRTPort();
        rtLogPort.setBase_Port(logPort);
        rtLogPort.setIsWired(false);
        rtStereotypeElements.add(rtLogPort);
        systemPortsCount++;
    }

    protected Vertex createInitialState() {
        Pseudostate initialState = UMLFactory.eINSTANCE.createPseudostate();
        initialState.setName("Initial");
        initialState.setKind(PseudostateKind.INITIAL_LITERAL);

        RTPseudostate rtInitialState = UMLRTStateMachinesFactory.eINSTANCE.createRTPseudostate();
        rtInitialState.setBase_Pseudostate(initialState);
        rtStereotypeElements.add(rtInitialState);
        return initialState;
    }

    protected Vertex createState(String name) {
        State state = UMLFactory.eINSTANCE.createState();
        state.setName(name);

        RTState reState = UMLRTStateMachinesFactory.eINSTANCE.createRTState();
        reState.setBase_State(state);
        rtStereotypeElements.add(reState);
        return state;
    }

    protected Transition createTransition(Vertex source, Vertex target) {
        Transition transition = UMLFactory.eINSTANCE.createTransition();
        transition.setName((source.getName()+"_"+target.getName()).toLowerCase());
        transition.setKind(TransitionKind.EXTERNAL_LITERAL);
        transition.setSource(source);
        transition.setTarget(target);
        return transition;
    }

    protected OpaqueBehavior createEffect(NamedElement element) {
        OpaqueBehavior effect = UMLFactory.eINSTANCE.createOpaqueBehavior();
        effect.getLanguages().add("C++");
        effect.getBodies().add(actionCodeGenerator.getAction(element).load());
        return effect;
    }

    protected OpaqueBehavior createEffect(NamedElement element, Map<String, Object> values) {
        OpaqueBehavior effect = UMLFactory.eINSTANCE.createOpaqueBehavior();
        effect.getLanguages().add("C++");
        effect.getBodies().add(actionCodeGenerator.getAction(element).load(values));
        return effect;
    }
}
