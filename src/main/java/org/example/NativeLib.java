package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Java FFM bindings for the Rust native library.
 * Provides access to add_ffi, hello_world_ffi, and free_rust_string functions.
 */
public class NativeLib {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIBRARY;

    // Function descriptors
    private static final FunctionDescriptor ADD_FFI_DESC = FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG
    );

    private static final FunctionDescriptor HELLO_WORLD_FFI_DESC = FunctionDescriptor.of(
            ValueLayout.ADDRESS
    );

    private static final FunctionDescriptor FREE_RUST_STRING_DESC = FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS
    );

    // Method handles
    private final MethodHandle addFfi;
    private final MethodHandle helloWorldFfi;
    private final MethodHandle freeRustString;

    static {
        try {
            String libName = "libnative.dylib"; // macOS dylib name

            // Allow explicit override: -Dnative.lib.path=/abs/path/to/libnative.dylib
            String override = System.getProperty("native.lib.path");
            Path libPath = null;
            if (override != null && !override.isBlank()) {
                Path candidate = Path.of(override);
                if (Files.exists(candidate)) {
                    libPath = candidate;
                } else {
                    throw new RuntimeException("native.lib.path set but file not found: " + candidate);
                }
            }

            // Default to project-relative path: <project>/native/target/release/libnative.dylib
            if (libPath == null) {
                Path base = Path.of(System.getProperty("user.dir"));
                for (int i = 0; i < 4; i++) {
                    Path candidate = base.resolve("native").resolve("target").resolve("release").resolve(libName);
                    if (Files.exists(candidate)) {
                        libPath = candidate;
                        break;
                    }
                    base = base.getParent() != null ? base.getParent() : base;
                }
            }

            if (libPath != null) {
                System.load(libPath.toAbsolutePath().toString());
                LIBRARY = SymbolLookup.loaderLookup();
            } else {
                // Fallback to loading from bundled resources (for packaged distributions)
                String resourcePath = "/native/" + libName;
                Path tempLib = Files.createTempFile("libnative", ".dylib");
                tempLib.toFile().deleteOnExit();
                try (InputStream is = NativeLib.class.getResourceAsStream(resourcePath)) {
                    if (is == null) {
                        throw new RuntimeException(
                                "Native library not found at 'native/target/release/" + libName +
                                        "' and no resource found at '" + resourcePath + "'.\n" +
                                        "Build it with: (cd native && cargo build --release) or pass -Dnative.lib.path");
                    }
                    Files.copy(is, tempLib, StandardCopyOption.REPLACE_EXISTING);
                }
                System.load(tempLib.toAbsolutePath().toString());
                LIBRARY = SymbolLookup.loaderLookup();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library", e);
        }
    }

    public NativeLib() {
        try {
            // Lookup and bind functions
            MemorySegment addFfiSymbol = LIBRARY.find("add_ffi")
                    .orElseThrow(() -> new RuntimeException("add_ffi function not found"));
            this.addFfi = LINKER.downcallHandle(addFfiSymbol, ADD_FFI_DESC);

            MemorySegment helloWorldFfiSymbol = LIBRARY.find("hello_world_ffi")
                    .orElseThrow(() -> new RuntimeException("hello_world_ffi function not found"));
            this.helloWorldFfi = LINKER.downcallHandle(helloWorldFfiSymbol, HELLO_WORLD_FFI_DESC);

            MemorySegment freeRustStringSymbol = LIBRARY.find("free_rust_string")
                    .orElseThrow(() -> new RuntimeException("free_rust_string function not found"));
            this.freeRustString = LINKER.downcallHandle(freeRustStringSymbol, FREE_RUST_STRING_DESC);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize native library bindings", e);
        }
    }

    /**
     * Adds two 64-bit unsigned integers using the Rust implementation.
     *
     * @param left  First operand
     * @param right Second operand
     * @return Sum of left and right
     */
    public long add(long left, long right) {
        try {
            return (long) addFfi.invokeExact(left, right);
        } catch (Throwable e) {
            throw new RuntimeException("Error calling add_ffi", e);
        }
    }

    /**
     * Gets the "Hello, world!" string from Rust.
     * Properly handles memory management by freeing the Rust-allocated string.
     *
     * @return The greeting string from Rust
     */
    public String helloWorld() {
        MemorySegment stringPtr = null;
        try {
            // Call Rust function to get string pointer
            stringPtr = (MemorySegment) helloWorldFfi.invokeExact();

            if (stringPtr.address() == 0) {
                throw new RuntimeException("Received null pointer from hello_world_ffi");
            }

            // Read the C string (null-terminated)
            return stringPtr.reinterpret(Long.MAX_VALUE).getString(0);

        } catch (Throwable e) {
            throw new RuntimeException("Error calling hello_world_ffi", e);
        } finally {
            // Always free the Rust-allocated string
            if (stringPtr != null && stringPtr.address() != 0) {
                freeString(stringPtr);
            }
        }
    }

    /**
     * Frees a Rust-allocated string.
     * This is called automatically by helloWorld() but exposed for advanced usage.
     *
     * @param ptr Pointer to the Rust-allocated string
     */
    private void freeString(MemorySegment ptr) {
        try {
            freeRustString.invokeExact(ptr);
        } catch (Throwable e) {
            throw new RuntimeException("Error calling free_rust_string", e);
        }
    }
}
