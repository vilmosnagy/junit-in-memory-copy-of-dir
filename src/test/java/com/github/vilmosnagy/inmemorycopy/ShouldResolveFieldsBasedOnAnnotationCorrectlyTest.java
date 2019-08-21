package com.github.vilmosnagy.inmemorycopy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(InMemoryCopyOfDirectoryExtension.class)
class ShouldResolveFieldsBasedOnAnnotationCorrectlyTest {

    @InMemoryCopyOfDirectory("/ShouldResolveFieldsBasedOnAnnotationCorrectlyTest")
    private Path directory;

    @Test
    public void should_resolve_simple_fields_correctly() {
        assertNotNull(directory);
        assertTrue(Files.exists(directory));
        assertTrue(Files.exists(directory.resolve("test_file.json")));
        assertFalse(Files.exists(directory.resolve("not_existing_file.json")));
    }

}
