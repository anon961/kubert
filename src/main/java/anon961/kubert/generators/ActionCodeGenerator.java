package anon961.kubert.generators;

import org.eclipse.uml2.uml.NamedElement;

import java.io.File;

public class ActionCodeGenerator extends TemplatesManager {
    private static final String ACTION_CODES_DIRECTORY = "actions";
    private static final String ACTION_CODE_EXTENSION = "cpp";

    public ActionCodeGenerator(Object owner) {
        super(ACTION_CODES_DIRECTORY + File.separator + owner.getClass().getSimpleName());
    }

    public TemplateFile getAction(String name) {
        String templateName = name + "." + ACTION_CODE_EXTENSION;
        return getTemplate(templateName);
    }

    public TemplateFile getAction(NamedElement element) {
        return getAction(element.getName());
    }
}
