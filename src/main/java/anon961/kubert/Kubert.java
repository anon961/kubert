package anon961.kubert;

import anon961.kubert.generators.DeploymentGenerator;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.RTCppPropertiesPackage;
import org.eclipse.papyrusrt.umlrt.profile.UMLRealTime.*;
import org.eclipse.papyrusrt.umlrt.profile.statemachine.UMLRTStateMachines.*;
import org.eclipse.uml2.uml.*;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

@Command(name = "anon961.kubert.kubert", mixinStandardHelpOptions = true, version = "1.0",
        description = "Deploy UML-RT models to Kubernetes clusters.")
public class Kubert implements Callable<Integer> {

    public class DashedEdge extends DefaultEdge {

    }

    public class DashedRedEdge extends DefaultEdge {

    }

    public class DashedDirectedEdge extends DefaultEdge {

    }

    @Parameters(index = "0", description = "DockerHub username to push containers to.")
    private String dockerHubUser;

    @Parameters(index = "1", description = "The UML-RT model to deploy.")
    private File modelFile;

    @Parameters(index = "2..*", description = "Program arguments.")
    List<String> programArgs;

    @Option(names = {"-o", "--output-dir"}, description = "Output directory for deployment files.")
    private File outputDir = new File("out");

    @Option(names = {"-t", "--top"}, description = "Name of the Top capsule for input models.")
    private String topName = "Top";

    @Option(names = {"-n", "--namespace"}, description = "Namespace to use for Kubernetes resources.")
    private String namespace = "kubert";

    @Option(names = {"-x", "--show-ui"}, description = "Show the user interface.")
    private boolean showUI = false;

    @Override
    public Integer call() throws IOException, URISyntaxException {
        outputDir.mkdirs();

        /*
         * create copy of input model file that is safe to modify
         */
        File modelFileCopy = new File(outputDir, modelFile.getName());
        if(modelFileCopy.exists())
            modelFileCopy.delete();
        Files.copy(modelFile.toPath(), modelFileCopy.toPath());
        modelFile = modelFileCopy;

        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getPackageRegistry().put(UMLRealTimePackage.eNS_URI, UMLRealTimePackage.eINSTANCE);
        resourceSet.getPackageRegistry().put(UMLRTStateMachinesPackage.eNS_URI, UMLRTStateMachinesPackage.eINSTANCE);
        resourceSet.getPackageRegistry().put(RTCppPropertiesPackage.eNS_URI, RTCppPropertiesPackage.eINSTANCE);

        UMLResourcesUtil.init(resourceSet);
        UMLRTSLibrary.load(resourceSet);

        XMIResource resource = (XMIResource) resourceSet
                .getResource(URI.createFileURI(modelFile.getAbsolutePath()), true);

        /*
         * strip IDs so that gradle can properly detect changes
         */
        resource.getEObjectToIDMap().clear();
        resource.save(Collections.EMPTY_MAP);

        ModelInfo modelInfo = ModelInfo.load(resource, topName);

        if(namespace == null)
            namespace = modelInfo.getModel().getName();

        Collection<String> partNames = new ArrayList<>();
        Map<String, Object> codegenConfig = new HashMap<>();

        codegenConfig.put("namespace", namespace);
        codegenConfig.put("appName", modelFile.getName());
        codegenConfig.put("partNames", partNames);
        codegenConfig.put("codegenPath", Paths.get(getClass().getClassLoader()
                .getResource("codegen").toURI()).toAbsolutePath()
                .toString().replace("\\", "/"));

        String uArgs = "";
        if(programArgs != null) {
            for (int i = 0; i< programArgs.size(); i++) {
                uArgs += programArgs.get(i) + " ";
            }
        }
        codegenConfig.put("arguments", uArgs);

        Collection<ModelInfo.CapsuleInstance> capsuleInstances = new ArrayList<>();
        modelInfo.getCapsuleInstances().forEach(mCapsulePart -> {
            if(mCapsulePart.isBehavioural())
                capsuleInstances.addAll(mCapsulePart.getInstances());
        });

        Map<ModelInfo.CapsuleInstance, Resource> capsuleInstanceResourceMap = new HashMap<>();
        capsuleInstances.forEach(capsuleInstance -> {
            File partDirectory = new File(outputDir, capsuleInstance.getFullyQualifiedName());
            partNames.add(capsuleInstance.getFullyQualifiedName());

            Map<String, Object> configuration = new HashMap<>(codegenConfig);
            configuration.put("partName", capsuleInstance.getFullyQualifiedName().toLowerCase());
            configuration.put("optional", capsuleInstance.getPart().isOptional());
            configuration.put("isTop", capsuleInstance.getPart().isTopBehaviour());

            if(capsuleInstance.getPart().getParent() != null
                    && capsuleInstance.getPart().getParent().getPart().isBehavioural())
                configuration.put("parentName",
                        capsuleInstance.getPart().getParent().getFullyQualifiedName().toLowerCase());

            boolean hasChildren = false, hasFixedChildren = false;
            for (ModelInfo.ReplicatedPart replicatedPart : capsuleInstance.getChildren()) {
                if (replicatedPart.isBehavioural()) {
                    hasChildren = true;
                    hasFixedChildren |= replicatedPart.isFixed();
                }
            }
            configuration.put("hasChildren", hasChildren);
            configuration.put("hasFixedChildren", hasFixedChildren);

            if (dockerHubUser != null)
                configuration.put("imageName",
                        (dockerHubUser + "/" + namespace + "-" + capsuleInstance.getFullyQualifiedName().toLowerCase()));

            File partModelFile = new File(partDirectory, "model.uml");
            XMIResource resource1 = (XMIResource) resourceSet
                    .createResource(URI.createFileURI(partModelFile.getAbsolutePath()));
            capsuleInstanceResourceMap.put(capsuleInstance, resource1);

            SingletonModel singletonModel = new SingletonModel(resource1, capsuleInstance);
            configuration.put("topName", singletonModel.getTopName());

            Map<String, Integer> servicePorts = singletonModel.createServicePorts();
            if(!servicePorts.isEmpty())
                configuration.put("servicePorts", servicePorts);

            Model partModel = singletonModel.getModel();
            for (ProfileApplication ap : modelInfo.getModel().getAllProfileApplications()) {
                partModel.getProfileApplications().add(EcoreUtil.copy(ap));
            }

            /*
             * clear EObject ids to force URI fragments instead
             */
            resource1.getEObjectToIDMap().clear();

            try {
                resource1.save(Collections.emptyMap());
                DeploymentGenerator.generateDeployment(configuration, partDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        DeploymentGenerator.generateNamespace(codegenConfig, outputDir);
        DeploymentGenerator.generateRoles(codegenConfig, outputDir);
        DeploymentGenerator.generateRootGradleFiles(codegenConfig, outputDir);

        if(showUI) {
            Map<ModelInfo.CapsuleInstance, ModelInfo> modelInfos = new HashMap<>();
            capsuleInstanceResourceMap.forEach((capsuleInstance, resource1) -> {
                modelInfos.put(capsuleInstance, ModelInfo.load(resource1, null));
            });

            drawGraph(Collections.emptyList(), Collections.singletonMap(null, modelInfo), false);
            drawGraph(capsuleInstances, modelInfos, true);
            for(;;);
        }

        return 0;
    }

    public void drawGraph(Collection<ModelInfo.CapsuleInstance> capsuleInstances,
                          Map<ModelInfo.CapsuleInstance, ModelInfo> modelInfos, boolean drawProxyEdges) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Graph<ModelInfo.CapsuleInstance, DefaultEdge> graph
                = new DefaultUndirectedGraph<>(DefaultEdge.class);

        modelInfos.values().forEach(modelInfo -> {
            constructGraph(graph, modelInfo);
        });

        if(drawProxyEdges) {
            capsuleInstances.forEach(myInstance -> {
                myInstance.portFarEndMap.values().forEach(portInstances -> {
                    portInstances.forEach(farPortInstance -> {
                        farPortInstance.getPart().getInstances().forEach(farInstance -> {
                            ModelInfo myInfo = modelInfos.get(myInstance);
                            ModelInfo farInfo = modelInfos.get(farInstance);

                            ModelInfo.CapsuleInstance myProxyInstance
                                    = myInfo.getCapsuleInstanceWithPartAnnotation(farInstance.getFullyQualifiedName());

                            ModelInfo.CapsuleInstance farProxyInstance
                                    = farInfo.getCapsuleInstanceWithPartAnnotation(myInstance.getFullyQualifiedName());

                            if(myProxyInstance != null && farProxyInstance != null)
                                graph.addEdge(myProxyInstance, farProxyInstance, new DashedRedEdge());
                        });
                    });
                });
            });
        }


        JGraphXAdapter<ModelInfo.CapsuleInstance, DefaultEdge> jgxAdapter
                = new JGraphXAdapter<>(graph);

        mxGraphComponent component = new mxGraphComponent(jgxAdapter);
        component.getViewport().setBackground(Color.white);


        component.getGraph().setAllowDanglingEdges(false);
        component.getGraph().setCellsResizable(false);
        component.getGraph().setCellsEditable(false);
        component.getGraph().setCellsMovable(false);
        component.getGraph().setEdgeLabelsMovable(false);
        component.getGraph().setLabelsClipped(false);
        component.getGraph().setResetEdgesOnMove(true);
        component.getGraph().setVertexLabelsMovable(false);
        component.getGraph().getStylesheet().getDefaultEdgeStyle().put(mxConstants.STYLE_NOLABEL, true);
        component.getGraph().getStylesheet().getDefaultEdgeStyle().put(mxConstants.STYLE_STROKECOLOR, "#000");

        Map<String, Object> dashededDirectedEdge
                = new HashMap<>(component.getGraph().getStylesheet().getDefaultEdgeStyle());
        dashededDirectedEdge.put(mxConstants.STYLE_DASHED, true);

        Map<String, Object> dashedEdge
                = new HashMap<>(component.getGraph().getStylesheet().getDefaultEdgeStyle());
        dashedEdge.put(mxConstants.STYLE_DASHED, true);
        dashedEdge.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);

        Map<String, Object> dashedRedEdge
                = new HashMap<>(component.getGraph().getStylesheet().getDefaultEdgeStyle());
        dashedRedEdge.put(mxConstants.STYLE_DASHED, true);
        dashedRedEdge.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
        dashedRedEdge.put(mxConstants.STYLE_STROKECOLOR, "#ff0000");


        Map<String, Object> optionalCapsuleStyle
                = new HashMap<>(component.getGraph().getStylesheet().getDefaultVertexStyle());
        optionalCapsuleStyle.put(mxConstants.STYLE_STROKECOLOR, "#000");
        optionalCapsuleStyle.put(mxConstants.STYLE_FILLCOLOR, "#fff");
        optionalCapsuleStyle.put(mxConstants.STYLE_DASHED, false);

        Map<String, Object> pluginCapsuleStyle
                = new HashMap<>(component.getGraph().getStylesheet().getDefaultVertexStyle());
        pluginCapsuleStyle.put(mxConstants.STYLE_STROKECOLOR, "#000");
        pluginCapsuleStyle.put(mxConstants.STYLE_FILLCOLOR, "#fff");
        pluginCapsuleStyle.put(mxConstants.STYLE_DASHED, true);

        component.getGraph().getStylesheet().putCellStyle("dashedEdge", dashedEdge);
        component.getGraph().getStylesheet().putCellStyle("dashedRedEdge", dashedRedEdge);
        component.getGraph().getStylesheet().putCellStyle("dashedDirectedEdge", dashededDirectedEdge);
        component.getGraph().getStylesheet().putCellStyle("optionalCapsule", optionalCapsuleStyle);
        component.getGraph().getStylesheet().putCellStyle("pluginCapsule", pluginCapsuleStyle);

        jgxAdapter.getCellToVertexMap().forEach((mxICell, capsuleInstance) -> {
            component.getGraph().updateCellSize(mxICell);
            mxICell.getGeometry().setWidth(mxICell.getGeometry().getWidth()+20);

            if(capsuleInstance.getPart().isOptional())
                mxICell.setStyle("optionalCapsule");
            else if(capsuleInstance.getPart().isPlugin())
                mxICell.setStyle("pluginCapsule");
        });

        jgxAdapter.getCellToEdgeMap().forEach((mxICell, edge) -> {
            if(edge instanceof DashedEdge)
                mxICell.setStyle("dashedEdge");
            if(edge instanceof DashedRedEdge)
                mxICell.setStyle("dashedRedEdge");
            if(edge instanceof DashedDirectedEdge)
                mxICell.setStyle("dashedDirectedEdge");
        });

        component.getGraph().refresh();

        mxHierarchicalLayout layout = new mxHierarchicalLayout(jgxAdapter);
        layout.execute(jgxAdapter.getDefaultParent());

        frame.getContentPane().add(component);
        frame.pack();
        frame.setVisible(true);
    }

    public void constructGraph(Graph<ModelInfo.CapsuleInstance, DefaultEdge> graph, ModelInfo modelInfo) {
        modelInfo.getCapsuleInstances().forEach(replicatedPart -> {
            replicatedPart.getInstances().forEach(capsuleInstance -> {
                graph.addVertex(capsuleInstance);
            });
        });

        constructGraph(graph, modelInfo.getTopPart());
    }

    public void constructGraph(Graph<ModelInfo.CapsuleInstance, DefaultEdge> graph,
                               ModelInfo.ReplicatedPart part) {
        part.getInstances().forEach(capsuleInstance -> {
            constructGraph(graph, capsuleInstance);

            capsuleInstance.getChildren().forEach(childPart -> {
                constructGraph(graph, childPart);
            });
        });

        part.getSiblings().forEach(siblingPart -> {
            part.getInstances().forEach(capsuleInstance -> {
                siblingPart.getInstances().forEach(siblingInstance -> {
                    if(!graph.containsEdge(capsuleInstance, siblingInstance))
                        graph.addEdge(capsuleInstance, siblingInstance, new DashedEdge());
                });
            });
        });
    }

    public void constructGraph(Graph<ModelInfo.CapsuleInstance, DefaultEdge> graph,
                               ModelInfo.CapsuleInstance instance) {
        ModelInfo.CapsuleInstance parent = instance.getPart().getParent();
        if(parent != null) {
            if(instance.isConnectedTo(parent))
                graph.addEdge(parent, instance, new DashedDirectedEdge());
            else
                graph.addEdge(parent, instance);
        }
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Kubert()).execute(args);
        System.exit(exitCode);
    }
}
