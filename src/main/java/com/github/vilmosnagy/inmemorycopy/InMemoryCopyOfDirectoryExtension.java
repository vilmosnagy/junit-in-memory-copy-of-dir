package com.github.vilmosnagy.inmemorycopy;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

public class InMemoryCopyOfDirectoryExtension implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.isAnnotated(InMemoryCopyOfDirectory.class)
            && parameterContext.getParameter().getType().isAssignableFrom(Path.class);
    }

    @Override
    public Path resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        try {
            return tryToResolveParameter(parameterContext, extensionContext);
        } catch (Exception e) {
            throw new ParameterResolutionException("", e);
        }
    }

    private Path tryToResolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws IOException, URISyntaxException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path inMemoryDir = fs.getPath("/inMemoryDir");
        Files.createDirectory(inMemoryDir);

        InMemoryCopyOfDirectory annotation = parameterContext.findAnnotation(InMemoryCopyOfDirectory.class).orElseThrow(NoSuchElementException::new);
        Path dirToCopyOnFs = Paths.get(extensionContext.getTestClass().orElseThrow(NoSuchElementException::new).getResource(annotation.value()).toURI());

        Files
            .walk(dirToCopyOnFs)
            .forEachOrdered(path -> {
                try {
                    Path resolvedPath = inMemoryDir.resolve(dirToCopyOnFs.relativize(path).toString());
                    if (!Files.isDirectory(path)) {
                        Files.copy(path, resolvedPath);
                    } else if (!Files.exists(resolvedPath)) {
                        Files.createDirectory(resolvedPath);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        return inMemoryDir;
    }
}
