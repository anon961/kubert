package anon961.kubert.generators;

import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TemplateFile {
    private PebbleTemplate template;
    private static Map<String, Object> defaultContext = Collections.singletonMap("debug", false);

    public TemplateFile(PebbleTemplate template) {
        this.template = template;
    }

    public String load() {
        try {
            Writer writer = new StringWriter();
            template.evaluate(writer, defaultContext);
            return writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String load(Map<String, Object> context) {
        try {
            Map<String, Object> actualContext = new HashMap<>(defaultContext);
            actualContext.putAll(context);

            Writer writer = new StringWriter();
            template.evaluate(writer, actualContext);
            return writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
