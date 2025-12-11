/**
 * Polyfills for SockJS and other browser compatibility issues
 */

// SockJS requires 'global' which doesn't exist in browsers
(window as any).global = window;

// Optional: Add Buffer polyfill if needed
// (window as any).Buffer = (window as any).Buffer || require('buffer').Buffer;

// Optional: Add process polyfill if needed
(window as any).process = {
  env: { DEBUG: undefined },
  version: '',
  nextTick: function (fn: Function) {
    setTimeout(fn, 0);
  }
};
