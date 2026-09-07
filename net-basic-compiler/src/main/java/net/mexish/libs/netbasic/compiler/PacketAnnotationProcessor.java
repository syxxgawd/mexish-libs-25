package net.mexish.libs.netbasic.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import net.mexish.libs.netbasic.annotation.PacketHandler;
import net.mexish.libs.netbasic.annotation.PacketState;
import net.mexish.libs.netbasic.packet.Packet;
import net.mexish.libs.netbasic.packet.PacketProcessorContext;
import net.mexish.libs.netbasic.packet.dispatcher.DispatcherRegistry;
import net.mexish.libs.netbasic.packet.dispatcher.PacketDispatcher;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.*;

@SupportedAnnotationTypes({"net.mexish.libs.netbasic.annotation.PacketHandler", "net.mexish.libs.netbasic.annotation.PacketState"})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuppressWarnings("unchecked")
@AutoService(Processor.class)
public final class PacketAnnotationProcessor extends AbstractProcessor {

    Filer filer;
    Messager messager;
    ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(final @NonNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.processingEnv = processingEnv;
    }

    private record HandlerInfo(ExecutableElement method, TypeElement logicClass, TypeElement packetClass) {}

    @Override
    public boolean process(final @NonNull Set<? extends TypeElement> annotations, final @NonNull RoundEnvironment roundEnv) {
        val stateToHandlers = new HashMap<TypeElement, List<HandlerInfo>>();
        val allStateClasses = new HashSet<TypeElement>();
        String targetPackage = null;

        for (val element : roundEnv.getElementsAnnotatedWith(PacketHandler.class)) {
            if (!(element instanceof ExecutableElement method)) continue;

            if (method.getParameters().size() != 2 || !isPacket(method.getParameters().get(0)) || !isContext(method.getParameters().get(1))) {
                error(method, "@PacketHandler method must have signature: void methodName(PacketType packet, PacketProcessorContext ctx)");
                continue;
            }

            val logicClass = (TypeElement) method.getEnclosingElement();
            val packetClass = (TypeElement) processingEnv.getTypeUtils().asElement(method.getParameters().get(0).asType());
            val packetStateAnnotation = findAnnotation(packetClass, PacketState.class.getCanonicalName());

            if (packetStateAnnotation == null) {
                error(packetClass, "Packet used in a handler must be annotated with @PacketState");
                continue;
            }

            val stateMirrors = getAnnotationValueAsTypeList(packetStateAnnotation, "value");

            for (val stateMirror : stateMirrors) {
                val stateElement = (TypeElement) processingEnv.getTypeUtils().asElement(stateMirror);
                allStateClasses.add(stateElement);

                if (targetPackage == null) {
                    val packageElement = processingEnv.getElementUtils().getPackageOf(stateElement);
                    targetPackage = packageElement.getQualifiedName() + ".generated";
                }

                stateToHandlers.computeIfAbsent(stateElement, v_ -> new ArrayList<>())
                        .add(new HandlerInfo(method, logicClass, packetClass));
            }
        }

        if (stateToHandlers.isEmpty()) {
            return false;
        }

        for (val entry : stateToHandlers.entrySet()) {
            generateDispatcher(targetPackage, entry.getKey(), entry.getValue());
        }

        generateModuleLoader(targetPackage, allStateClasses);
        return true;
    }

    private void generateDispatcher(final @NonNull String targetPackage, final @NonNull TypeElement stateClass, final @NonNull List<HandlerInfo> handlers) {
        val stateName = stateClass.getSimpleName().toString();
        val dispatcherName = ClassName.get(targetPackage, "Dispatcher_" + stateName);

        val dispatchMethod = MethodSpec.methodBuilder("dispatch")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Object[].class, "logicModules")
                .addParameter(Packet.class, "packet")
                .addParameter(PacketProcessorContext.class, "ctx");

        dispatchMethod.beginControlFlow("switch (packet)");

        for (val handler : handlers) {
            dispatchMethod.addCode("case $T p -> ", ClassName.get(handler.packetClass));
            dispatchMethod.beginControlFlow("");

            dispatchMethod.beginControlFlow("for (Object module : logicModules)");
            dispatchMethod.beginControlFlow("if (module instanceof $T logic)", ClassName.get(handler.logicClass));
            dispatchMethod.addStatement("logic.$L(p, ctx)", handler.method.getSimpleName());

            dispatchMethod.endControlFlow(); // closes if
            dispatchMethod.endControlFlow(); // closes for
            dispatchMethod.endControlFlow(); // closes case block
        }

        dispatchMethod.addCode("default -> {}\n");
        dispatchMethod.endControlFlow();

        val dispatcherClass = TypeSpec.classBuilder(dispatcherName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(PacketDispatcher.class)
                .addMethod(dispatchMethod.build())
                .addJavadoc("generated by net-basic annotation processor. do not edit.")
                .build();

        try {
            JavaFile.builder(targetPackage, dispatcherClass).build().writeTo(filer);
        } catch (final IOException e) {
            error(null, "Failed to write dispatcher file for state " + stateName + ": " + e);
        }
    }

    private void generateModuleLoader(final @NonNull String targetPackage, final @NonNull Set<TypeElement> stateClasses) {
        val loaderSimpleName = "GeneratedDispatcherLoader";
        val loaderName = ClassName.get(targetPackage, loaderSimpleName);

        val loadMethod = MethodSpec.methodBuilder("load")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(DispatcherRegistry.class, "registry");

        for (val stateClass : stateClasses) {
            val stateClassName = ClassName.get(stateClass);
            val dispatcherImplName = ClassName.get(targetPackage, "Dispatcher_" + stateClass.getSimpleName().toString());
            loadMethod.addStatement("registry.register($T.class, new $T())", stateClassName, dispatcherImplName);
        }

        val loaderClass = TypeSpec.classBuilder(loaderName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ClassName.get("net.mexish.libs.netbasic.packet.dispatcher", "DispatcherLoader"))
                .addMethod(loadMethod.build())
                .addJavadoc("generated by net-basic annotation processor. do not edit.")
                .build();

        try {
            JavaFile.builder(targetPackage, loaderClass).build().writeTo(filer);

            val resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
                    "META-INF/services/net.mexish.libs.netbasic.packet.dispatcher.DispatcherLoader");

            try (val writer = resource.openWriter()) {
                writer.write(loaderName.toString());
            }
        } catch (final IOException e) {
            error(null, "Failed to write module loader or SPI resource: " + e);
        }
    }

    private @Nullable AnnotationMirror findAnnotation(final @NonNull Element element, final @NonNull String annotationName) {
        return element.getAnnotationMirrors().stream()
                .filter(m -> ((TypeElement) m.getAnnotationType().asElement()).getQualifiedName().toString().equals(annotationName))
                .findFirst().orElse(null);
    }

    private List<TypeMirror> getAnnotationValueAsTypeList(final @NonNull AnnotationMirror annotationMirror, final @NonNull String key) {
        for (val entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                if (entry.getValue().getValue() instanceof List) {
                    val values = (List<? extends AnnotationValue>) entry.getValue().getValue();
                    return values.stream().map(v -> (TypeMirror) v.getValue()).toList();
                } else if (entry.getValue().getValue() instanceof TypeMirror) {
                    return List.of((TypeMirror) entry.getValue().getValue());
                }
            }
        }

        return Collections.emptyList();
    }

    private boolean isPacket(final @NonNull VariableElement param) {
        return processingEnv.getTypeUtils()
                .isSubtype(param.asType(), processingEnv.getElementUtils().getTypeElement(Packet.class.getCanonicalName()).asType());
    }

    private boolean isContext(final @NonNull VariableElement param) {
        return processingEnv.getTypeUtils()
                .isSameType(param.asType(), processingEnv.getElementUtils().getTypeElement(PacketProcessorContext.class.getCanonicalName()).asType());
    }

    private void error(final Element e,
                       final @NonNull String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }
}