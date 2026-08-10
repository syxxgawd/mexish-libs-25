package net.mexish.libs.impl.compiletime;

import lombok.val;
import net.mexish.libs.impl.Impl;
import net.mexish.libs.impl.ImplModel;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("net.mexish.libs.impl.Impl")
public final class ImplProcessor extends AbstractProcessor {

    private final Map<String, List<ImplModel>> models = new HashMap<>();

    private static String getClassName(TypeElement type) {
        val enclosedIn = type.getEnclosingElement();

        // if class is inner
        if (enclosedIn instanceof TypeElement) {
            return getClassName((TypeElement) enclosedIn) + '$' + type.getSimpleName();
        }

        return type.getQualifiedName().toString();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (val element : roundEnv.getElementsAnnotatedWith(Impl.class)) {
            val type = (TypeElement) element;
            val name = getClassName(type);

            val annotation = element.getAnnotation(Impl.class);

            val priority = annotation.priority();

            String apiName;

            try {
                apiName = annotation.value().getName();
            } catch (MirroredTypeException e) {
                apiName = getClassName(processingEnv.getElementUtils().getTypeElement(e.getTypeMirror().toString()));
            }

            models.computeIfAbsent(apiName, x -> new ArrayList<>()).add(new ImplModel(name, priority));
        }

        if (roundEnv.processingOver()) {
            for (val entry : models.entrySet()) {
                val apiName = entry.getKey();
                val modelList = entry.getValue();

                try {
                    val object = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                            "", "META-INF/impl/" + apiName);

                    try (val writer = object.openWriter()) {
                        for (val model : modelList) {
                            writer.append(model.getImplName());
                            writer.append(':');
                            writer.append(model.getPriority().name());
                            writer.append('\n');
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }
}
