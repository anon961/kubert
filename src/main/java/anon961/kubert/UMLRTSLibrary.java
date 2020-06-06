package anon961.kubert;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.Protocol;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.RTPort;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.UMLRealTimePackage;
import org.eclipse.uml2.uml.*;
import org.eclipse.uml2.uml.Class;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UMLRTSLibrary {
    final static String UMLRT_LIBRARY_PATHMAP = "pathmap://UMLRTRTSLIB/UMLRT-RTS.uml";
    final static String UMLRT_LIBRARY_LOCATION = "libs/UMLRT-RTS.uml";

    final static String UML_TYPES_LIBRARY_PATHMAP = "pathmap://UML_LIBRARIES/UMLPrimitiveTypes.library.uml";
    final static String UML_TYPES_LIBRARY_LOCATION = "libs/UMLPrimitiveTypes.library.uml";

    private static boolean loaded = false;
    private static Map<String, Collaboration> protocols = new HashMap<>();
    private static Map<String, Class> classes = new HashMap<>();
    private static Map<String, PrimitiveType> umlTypes = new HashMap<>();
    private static Map<Collaboration, Map<String, Event>> protocolEventsMap = new HashMap<>();

    public enum SystemProtocols {
        Log,
        Timing,
        Frame,
        TCP,
        UMLRTBaseCommProtocol;
    }

    public enum SystemClass {
        UMLRTCapsuleId,
        UMLRTMessage,
        UMLRTTimerId,
        UMLRTTimespec;
    }

    private UMLRTSLibrary() {}

    public static void load(ResourceSet set) {
        set.getURIConverter().getURIMap().put(URI.createURI(UMLRT_LIBRARY_PATHMAP),
                URI.createFileURI(new File(UMLRT_LIBRARY_LOCATION).getAbsolutePath()));
        set.getURIConverter().getURIMap().put(URI.createURI(UML_TYPES_LIBRARY_PATHMAP),
                URI.createFileURI(new File(UML_TYPES_LIBRARY_LOCATION).getAbsolutePath()));

        Resource rtsResource = set.getResource(URI.createURI(UMLRT_LIBRARY_PATHMAP), true);

        Collection<Class> rtsClasses =
                EcoreUtil.getObjectsByType(rtsResource.getContents(), UMLPackage.Literals.CLASS);

        rtsClasses.forEach(aClass -> {
            classes.put(aClass.getName(), aClass);
        });

        Collection<Protocol> rtsProtocols =
                EcoreUtil.getObjectsByType(rtsResource.getContents(), UMLRealTimePackage.Literals.PROTOCOL);

        rtsProtocols.forEach(protocol -> {
            Collaboration collaboration = protocol.getBase_Collaboration();
            protocols.put(collaboration.getName(), collaboration);

            Map<String, Event> callEvents = new HashMap<>();
            protocolEventsMap.put(collaboration, callEvents);

            collaboration.getNearestPackage().eAllContents().forEachRemaining(eObject -> {
                if(eObject instanceof CallEvent) {
                    CallEvent event = ((CallEvent) eObject);
                    Operation operation = event.getOperation();
                    if(operation != null)
                        callEvents.put(operation.getName(), event);
                }
            });
        });

        Resource umlTypeResource = set.getResource(URI.createURI(UML_TYPES_LIBRARY_PATHMAP), true);
        Model umlTypeModel = (Model) EcoreUtil.getObjectByType(umlTypeResource.getContents(), UMLPackage.Literals.MODEL);

        Collection<PrimitiveType> primitiveTypes =
                EcoreUtil.getObjectsByType(umlTypeModel.getPackagedElements(), UMLPackage.Literals.PRIMITIVE_TYPE);
        primitiveTypes.forEach(primitiveType -> {
            umlTypes.put(primitiveType.getName(), primitiveType);
        });

        loaded = true;
    }

    public static Collaboration getProtocol(SystemProtocols protocol) {
        if(!loaded)
            throw new IllegalStateException("UMLRT-RTS library is not loaded");
        return protocols.get(protocol.toString());
    }

    public static Class getSystemClass(SystemClass cls) {
        if(!loaded)
            throw new IllegalStateException("UMLRT-RTS library is not loaded");
        return classes.get(cls.toString());
    }

    public static Event getEvent(SystemProtocols protocol, String eventName) {
        if(!loaded)
            throw new IllegalStateException("UMLRT-RTS library is not loaded");

        Collaboration collaboration = getProtocol(protocol);
        return protocolEventsMap.get(collaboration).get(eventName);
    }

    public static Port createPort(String name, SystemProtocols protocol) {
        Port port = UMLFactory.eINSTANCE.createPort();
        port.setName(name);
        port.setType(getProtocol(protocol));
        port.setIsService(false);
        port.setIsConjugated(false);
        port.setIsBehavior(true);
        port.setAggregation(AggregationKind.COMPOSITE_LITERAL);
        port.setIsOrdered(true);
        return port;
    }

    public static boolean isSystemPort(RTPort rtPort) {
        return isSystemPort(rtPort.getBase_Port());
    }

    public static boolean isSystemPort(Port port) {
        if(!loaded)
            throw new IllegalStateException("UMLRT-RTS library is not loaded");
        return protocols.values().contains(port.getType());
    }

    public static boolean isPrimitiveType(Type type) {
        if(!loaded)
            throw new IllegalStateException("UMLPrimitiveTypes library is not loaded");
        return umlTypes.values().contains(type);
    }

    public static PrimitiveType getUmlType(String typeName) {
        if(!loaded)
            throw new IllegalStateException("UMLPrimitiveTypes library is not loaded");
        return umlTypes.get(typeName);
    }
}
