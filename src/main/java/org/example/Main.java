package org.example;

class Main {
    public static void main(String[] args) {
        System.out.println("=== Java FFM Demo with Rust Native Library ===\n");

        try {
            // Initialize the native library
            NativeLib nativeLib = new NativeLib();
            System.out.println("Native library loaded successfully\n");

            // Test the add function
            long a = 42;
            long b = 58;
            long result = nativeLib.add(a, b);
            System.out.println("Add in Rust: " + a + " + " + b + " = " + result);

            String greeting = nativeLib.helloWorld();
            System.out.println("Received from Rust: \"" + greeting + "\"");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}