package net.mexish.libs.www;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import net.mexish.libs.commons.logging.Logging;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static spark.Spark.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PageManager {

    public static final PageManager IMP = new PageManager();

    ExecutorService executor = Executors.newCachedThreadPool();

    Map<String, PageProcessor> pagesToLoad = new LinkedHashMap<>();

    Map<String, String> pages = new LinkedHashMap<>();

    Map<String, String> pagePaths = new LinkedHashMap<>();

    @NonFinal Path pageDir = Paths.get(".");

    public void loadPages() {
        for (val entry : pagesToLoad.entrySet()) {
            val name = entry.getKey();
            val processor = entry.getValue();

            loadPage(name).whenComplete((loaded, t) -> {
                if (t != null) {
                    Logging.IMP.stacktrace(t, "§c[PAGEMGR/REG] couldn't load page `{}`.html", name);
                    return;
                }

                Logging.IMP.info("§5[PAGEMGR/REG] loaded page `{}`.html", name);
                pages.put(name, loaded);

                if (processor == null) {
                    registerPage((_, _) -> loaded, pagePaths.get(name));
                } else {
                    registerPage(processor);
                }

                Logging.IMP.info("§5[PAGEMGR/REG] registered page `{}`.html", name);
            });
        }
    }

    public void registerPage(final @NonNull String name,
                             final PageProcessor processor) {
        pagesToLoad.put(name, processor);
    }

    public void registerPage(final @NonNull String name,
                             final @NonNull String path) {
        pagePaths.put(name, path);
        registerPage(name, (PageProcessor) null);
    }

    public void bootSpark(final int port) {
        port(port);
    }

    public void setAssetLocation(final @NonNull String path) {
        staticFileLocation(path);
    }

    public void setPageLocation(final @NonNull Path path) {
        pageDir = path;
    }

    public void registerPage(final @NonNull PageProcessor processor) {
        val pathAnnotation = processor.getClass().getAnnotation(PagePath.class);

        if (pathAnnotation == null || pathAnnotation.value().isEmpty()) {
            Logging.IMP.info("§5[PAGEMGR/REG] failed while loading page from {} | no @PagePath annotation present", processor.getClass().getSimpleName());
            return;
        }

        val path = pathAnnotation.value().toLowerCase();
        get(path, processor);

        Logging.IMP.info("§d[PAGEMGR/REG] registered page P: {} | ADDR: 0x{}", path, Integer.toHexString(processor.hashCode()));
    }

    private void registerPage(final @NonNull PageProcessor processor,
                              final @NonNull String path) {
        get(path, processor);
        Logging.IMP.info("§d[PAGEMGR/REG] registered page P: {} | ADDR: 0x{}", path, Integer.toHexString(processor.hashCode()));
    }

    @Contract("_ -> new")
    private @NotNull CompletableFuture<String> loadPage(final String fileName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(new BufferedInputStream(Files.newInputStream(pageDir.resolve(fileName + ".html"))).readAllBytes(), StandardCharsets.UTF_8);
            } catch (final Throwable t) {
                return "empty! // placeholder page";
            }
        }, executor);
    }

    public String getByName(final @NonNull String name) {
        return pages.get(name);
    }

}
