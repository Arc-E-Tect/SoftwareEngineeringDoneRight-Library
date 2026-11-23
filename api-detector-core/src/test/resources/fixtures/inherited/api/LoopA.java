package com.example.inherited.api;

// Not valid Java together with LoopB, but parseable: the scanner must not loop forever on it.
public interface LoopA extends LoopB {
}
