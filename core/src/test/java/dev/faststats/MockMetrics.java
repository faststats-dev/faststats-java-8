package dev.faststats;

import com.google.gson.JsonObject;

import java.net.URI;

final class MockMetrics extends SimpleMetrics {
    MockMetrics(final Factory factory) {
        super(factory, URI.create("http://localhost:5000/v1/collect"));
    }

    @Override
    protected boolean preSubmissionStart() {
        return true;
    }

    void startTestSubmitting() {
        startSubmitting();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
    }
}
