use std::ffi::CString;
use std::os::raw::c_char;

fn add(left: u64, right: u64) -> u64 {
    left + right
}

/// C-compatible wrapper for `add`.
/// Returns the sum as `uint64_t`.
#[unsafe(no_mangle)]
pub extern "C" fn add_ffi(left: u64, right: u64) -> u64 {
    add(left, right)
}

fn hello_world() -> String {
    "Hello, world!".to_string()
}

/// Returns a pointer to a NUL-terminated C string. Caller must free with `free_rust_string`.
#[unsafe(no_mangle)]
pub extern "C" fn hello_world_ffi() -> *mut c_char {
    let s = hello_world();
    // CString::new only fails if there is an internal NUL; our string is constant and safe.
    let cstr = CString::new(s).expect("failed to create CString");
    cstr.into_raw()
}

/// Frees a string previously returned by `hello_world_ffi` (or any CString::into_raw from Rust).
#[unsafe(no_mangle)]
pub extern "C" fn free_rust_string(s: *mut c_char) {
    if s.is_null() {
        return;
    }
    // Reconstruct and drop the CString to free memory.
    unsafe {
        let _ = CString::from_raw(s);
    }
}
