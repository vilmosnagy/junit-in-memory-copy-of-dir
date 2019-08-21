package com.github.vilmosnagy.inmemorycopy;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.extension.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;

public class InMemoryCopyOfDirectoryExtension implements ParameterResolver, BeforeEachCallback {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.isAnnotated(InMemoryCopyOfDirectory.class)
            && parameterContext.getParameter().getType().isAssignableFrom(Path.class);
    }

    @Override
    public Path resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        try {
            return tryToResolveParameter(parameterContext.findAnnotation(InMemoryCopyOfDirectory.class).orElseThrow(NoSuchElementException::new), extensionContext.getTestClass().orElseThrow(NoSuchElementException::new));
        } catch (Exception e) {
            throw new ParameterResolutionException("", e);
        }
    }

    private Path tryToResolveParameter(InMemoryCopyOfDirectory annotation, Class<?> testClass) throws IOException, URISyntaxException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        String inMemoryFsSeparator = fs.getSeparator();
        Path inMemoryDir = fs.getPath("/inMemoryDir");
        Files.createDirectory(inMemoryDir);

        Path dirToCopyOnFs = Paths.get(testClass.getResource(annotation.value()).toURI());

        Files
            .walk(dirToCopyOnFs)
            .forEachOrdered(path -> {
                try {
                    String relativePath = dirToCopyOnFs
                        .relativize(path)
                        .toString()
                        .replace(FileSystems.getDefault().getSeparator(), inMemoryFsSeparator);

                    Path resolvedPath = inMemoryDir.resolve(relativePath);
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

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        context
            .getTestClass()
            .map(this::getDeclaredFieldsFromClass)
            .orElse(Collections.emptySet())
            .stream()
            .filter(it -> it.getAnnotation(InMemoryCopyOfDirectory.class) != null)
            .filter(it -> it.getType().isAssignableFrom(Path.class))
            .forEach(field -> {
                try {
                    Path path = tryToResolveParameter(field.getAnnotation(InMemoryCopyOfDirectory.class), context.getTestClass().orElseThrow(NoSuchElementException::new));
                    field.setAccessible(true);
                    field.set(context.getTestInstance().orElseThrow(NoSuchElementException::new), path);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
    }

    private Set<Field> getDeclaredFieldsFromClass(Class<?> testClass) {
        if (testClass.getSuperclass() != null) {
            return Sets.union(
                getDeclaredFieldsFromClass(testClass.getSuperclass()),
                Sets.newHashSet(testClass.getDeclaredFields())
            );
        } else {
            return Sets.newHashSet(testClass.getDeclaredFields());
        }
    }
}
