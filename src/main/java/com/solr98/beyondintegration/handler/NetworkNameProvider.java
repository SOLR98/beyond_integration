package com.solr98.beyondintegration.handler;

public interface NetworkNameProvider {
    default String getCustomName() { return ""; }
}
