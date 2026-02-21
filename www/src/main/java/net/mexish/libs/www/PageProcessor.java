package net.mexish.libs.www;

import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

public interface PageProcessor extends Route {

    @Override
    Object handle(Request req, Response res);

    default String terminate(final @NonNull Response response,
                             final int code,
                             final @NonNull String message) {
        response.status(code);
        return message;
    }

    default Object respondWithFile(final Response response, final @NotNull File file) {
        if (!file.exists()) {
            return terminate(response, 500, "Internal error");
        }

        response.raw().setStatus(200);
        response.raw().setContentType("image/svg+xml");
        response.raw().setHeader("Content-Disposition", "attachment; filename=" + file.getName());
        response.raw().setContentLengthLong(file.length());

        try (val fis = new FileInputStream(file); val os = response.raw().getOutputStream()) {
            val buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            os.flush();
        } catch (IOException e) {
            return terminate(response, 500, "Internal server error");
        }

        return response.raw();
    }

    default Object displayGif(final Response response, final @NotNull File file) {
        if (!file.exists()) {
            return terminate(response, 500, "Internal error");
        }

        response.header("Cache-Control", "no-cache, no-store, must-revalidate");
        response.header("Pragma", "no-cache");
        response.header("Expires", "0");
        response.header("Content-Length", String.valueOf(file.length()));
        response.type("image/gif");
        response.raw().setStatus(200);

        try {
            response.raw().getOutputStream().write(Files.readAllBytes(file.toPath()));
        } catch (final Throwable t) {
            return terminate(response, 500, "Internal error");
        }

        return response.raw();
    }

}
