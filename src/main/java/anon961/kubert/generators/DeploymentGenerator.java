package anon961.kubert.generators;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class DeploymentGenerator {
    private static final TemplatesManager templateManager =
            new TemplatesManager("configurations");

    private static final TemplateFile dockerTemplate =
            templateManager.getTemplate("umlrt-app.docker");

    private static final TemplateFile namespaceTemplate =
            templateManager.getTemplate("umlrt-namespace.yaml");
    private static final TemplateFile rolesTemplate =
            templateManager.getTemplate("umlrt-roles.yaml");
    private static final TemplateFile deploymentTemplate =
            templateManager.getTemplate("umlrt-deployment.yaml");
    private static final TemplateFile serviceTemplate =
            templateManager.getTemplate("umlrt-service.yaml");

    private static final TemplateFile rootGradleScriptTemplate =
            templateManager.getTemplate("root.gradle");
    private static final TemplateFile deploymentScriptTemplate =
            templateManager.getTemplate("deploy.gradle");
    private static final TemplateFile gradleSettingsTemplate =
            templateManager.getTemplate("settings.gradle");
    private static final TemplateFile gradlePropertiesTemplate =
            templateManager.getTemplate("gradle.properties");

    public static void generateDeployment(Map<String, Object> configuration, File outputDir) throws IOException {
        Files.write(new File(outputDir, "Dockerfile").toPath(),
                dockerTemplate.load(configuration).getBytes());

        if(configuration.containsKey("imageName")) {
            Files.write(new File(outputDir, "deployment.yaml").toPath(),
                    deploymentTemplate.load(configuration).getBytes());

            if(configuration.containsKey("servicePorts"))
                Files.write(new File(outputDir, "service.yaml").toPath(),
                        serviceTemplate.load(configuration).getBytes());

            Files.write(new File(outputDir, "build.gradle").toPath(),
                    deploymentScriptTemplate.load(configuration).getBytes());
        } else {
            Files.write(new File(outputDir, "build.gradle").toPath(),
                    deploymentScriptTemplate.load(configuration).getBytes());
        }
    }

    public static void generateNamespace(Map<String, Object> configuration, File outputDir) throws IOException {
        Files.write(new File(outputDir, "namespace.yaml").toPath(),
                namespaceTemplate.load(configuration).getBytes());
    }

    public static void generateRoles(Map<String, Object> configuration, File outputDir) throws IOException {
        Files.write(new File(outputDir, "roles.yaml").toPath(),
                rolesTemplate.load(configuration).getBytes());
    }

    public static void generateRootGradleFiles(Map<String, Object> configuration, File outputDir) throws IOException {
        Files.write(new File(outputDir, "build.gradle").toPath(),
                rootGradleScriptTemplate.load(configuration).getBytes());

        Files.write(new File(outputDir, "settings.gradle").toPath(),
                gradleSettingsTemplate.load(configuration).getBytes());

        Files.write(new File(outputDir, "gradle.properties").toPath(),
                gradlePropertiesTemplate.load(configuration).getBytes());
    }
}
