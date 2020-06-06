package anon961.kubert.generators;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class TemplatesManager {
    protected PebbleEngine engine = new PebbleEngine.Builder().newLineTrimming(false).build();
    protected Map<String, PebbleTemplate> templates = new HashMap<>();

    protected TemplatesManager(String templatesLocation) {
        try {
            Path templatesPath = Paths.get(getClass().getClassLoader().getResource(templatesLocation).toURI());
            Files.walkFileTree(templatesPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
                    templates.put(path.getFileName().toString(), engine.getTemplate(path.toString()));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    public TemplateFile getTemplate(String name) {
        return new TemplateFile(templates.get(name));
    }
}
