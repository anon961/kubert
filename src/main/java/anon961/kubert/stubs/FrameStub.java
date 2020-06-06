package anon961.kubert.stubs;

import anon961.kubert.ModelInfo;
import anon961.kubert.UMLRTSLibrary;
import anon961.kubert.generators.ActionCodeGenerator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.*;
import org.eclipse.uml2.uml.*;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Package;

import java.util.*;

public class FrameStub {
    private ModelInfo.CapsuleInstance owner;
    private Package pkg;
    private Class stubClass;

    private ActionCodeGenerator actionCodeGenerator;
    private Map<String, Object> actionCodeValues = new HashMap<>();
    private Collection<EObject> rtStereotypeElements = new HashSet<>();

    public FrameStub(String name, ModelInfo.CapsuleInstance owner) {
        this.owner = owner;

        pkg = UMLFactory.eINSTANCE.createPackage();
        pkg.setName(name);

        stubClass = UMLFactory.eINSTANCE.createClass();
        stubClass.setName(name);
        stubClass.setIsActive(true);
        pkg.getPackagedElements().add(stubClass);

        PassiveClassProperties stubClassProps =
                RTCppPropertiesFactory.eINSTANCE.createPassiveClassProperties();
        stubClassProps.setBase_Class(stubClass);
        stubClassProps.setHeaderPreface(
                "#include \"umlrtframeprotocol.hh\"\n" +
                "struct UMLRTCapsulePart;");

        stubClassProps.setImplementationPreface(
                "#include <ctype.h>\n" +
                "#include \"umlrtcapsulepart.hh\"\n" +
                "#include \"umlrtcapsule.hh\"");

        rtStereotypeElements.add(stubClassProps);

        actionCodeGenerator = new ActionCodeGenerator(this);
        actionCodeValues.put("ownerFQN", owner.getFullyQualifiedName().toLowerCase());
        actionCodeValues.put("ownerFQNCapital", owner.getFullyQualifiedName()
                .toUpperCase().replace('-', '_'));

//        createCapsuleIdStub();
        createAttributes();
        createConstructor();
        createIncarnate(1);
        createIncarnate(2);
        createDestroy(1);
        createDestroy(2);
        createImport(1);
        createImport(2);
        createDeport();
    }

    public void createAttributes() {
        Property framePort = UMLFactory.eINSTANCE.createProperty();
        framePort.setName("framePort");
        framePort.setType(UMLRTSLibrary.getUmlType("Integer"));
        stubClass.getOwnedAttributes().add(framePort);

        AttributeProperties framePortProperties =  RTCppPropertiesFactory.eINSTANCE.createAttributeProperties();
        framePortProperties.setBase_Property(framePort);
        framePortProperties.setType("UMLRTFrameProtocol_baserole *");
        rtStereotypeElements.add(framePortProperties);
    }

    public void createConstructor() {
        Parameter p1 = createParameter("port", "UMLRTFrameProtocol_baserole *");
        Parameter ret = createParameter("", "/*constructor*/");
        ret.setDirection(ParameterDirectionKind.RETURN_LITERAL);

        Behavior bv = createBehaviour(stubClass.getName());
        Operation op = createOperation(stubClass.getName(), bv, p1, ret);
        stubClass.getOwnedOperations().add(op);
        stubClass.getOwnedBehaviors().add(bv);
    }

    public Parameter createParameter(String name, Type type, String defaultValue) {
        Parameter param = UMLFactory.eINSTANCE.createParameter();
        param.setType(type);
        param.setDefault(defaultValue);
        param.setName(name);
        return param;
    }

    public Parameter createParameter(String name, Type type) {
        return createParameter(name, type, null);
    }

    public Parameter createParameter(String name, String type) {
        Parameter param = createParameter(name, UMLRTSLibrary.getUmlType("Integer"));
        ParameterProperties paramProps = RTCppPropertiesFactory.eINSTANCE.createParameterProperties();
        paramProps.setType(type);
        paramProps.setBase_Parameter(param);
        rtStereotypeElements.add(paramProps);
        return param;
    }

    public Behavior createBehaviour(String name) {
        OpaqueBehavior behaviour = UMLFactory.eINSTANCE.createOpaqueBehavior();
        behaviour.getLanguages().add("C++");
        behaviour.getBodies().add(actionCodeGenerator.getAction(name).load(actionCodeValues));
        return behaviour;
    }

    public Operation createOperation(String name, Behavior behavior, Parameter...params) {
        Operation operation = UMLFactory.eINSTANCE.createOperation();
        operation.setName(name);
        operation.getOwnedParameters().addAll(Arrays.asList(params));

        behavior.setSpecification(operation);
        operation.getMethods().add(behavior);
        return operation;
    }

    public Operation createBooleanOperation(String name, Behavior behavior, Parameter...params) {
        Operation operation = createOperation(name, behavior, params);
        Parameter ret = createParameter("", UMLRTSLibrary.getUmlType("Boolean"));
        ret.setDirection(ParameterDirectionKind.RETURN_LITERAL);
        operation.getOwnedParameters().add(ret);
        return operation;
    }

    public void createIncarnate(int version) {
        Parameter p1 = createParameter("part", "const UMLRTCapsulePart *");
        Parameter p2 = version == 2 ? createParameter("capsuleClass", "const UMLRTCapsuleClass &") : null;
        Behavior bv = createBehaviour("incarnate"+version);
        Parameter ret = createParameter("", "const UMLRTCapsuleId");
        ret.setDirection(ParameterDirectionKind.RETURN_LITERAL);

        Operation op = version == 1 ? createOperation("incarnate", bv, p1, ret)
                : createOperation("incarnate", bv, p1, p2, ret);
        stubClass.getOwnedBehaviors().add(bv);
        stubClass.getOwnedOperations().add(op);
    }

    public void createDestroy(int version) {
        Parameter p1 = version == 1 ? createParameter("part", "const UMLRTCapsulePart *")
                : createParameter("id", "const UMLRTCapsuleId");

        Behavior bv = createBehaviour("destroy"+version);
        Operation op = createBooleanOperation("destroy", bv, p1);
        stubClass.getOwnedBehaviors().add(bv);
        stubClass.getOwnedOperations().add(op);
    }

    public void createImport(int version) {
        Parameter p1 = version == 1 ? createParameter("srcPart", "const UMLRTCapsulePart *")
                : createParameter("id", "const UMLRTCapsuleId &");
        Parameter p2 = createParameter("destPart", "const UMLRTCapsulePart *");
        Parameter p3 = createParameter("index",
                UMLRTSLibrary.getUmlType("Integer"), "-1");

        Behavior bv = createBehaviour("import"+version);
        Operation op = createBooleanOperation("import", bv, p1, p2, p3);
        stubClass.getOwnedBehaviors().add(bv);
        stubClass.getOwnedOperations().add(op);
    }

    public void createDeport() {
        Parameter p1 = createParameter("id", "const UMLRTCapsuleId &");
        Parameter p2 = createParameter("part", "const UMLRTCapsulePart * const &");

        Behavior bv = createBehaviour("deport");
        Operation op = createBooleanOperation("deport", bv, p1, p2);
        stubClass.getOwnedBehaviors().add(bv);
        stubClass.getOwnedOperations().add(op);
    }

    public Package getPackage() {
        return pkg;
    }

    public Type getType() {
        return stubClass;
    }

    public Collection<EObject> getRTElements() {
        return rtStereotypeElements;
    }
}
