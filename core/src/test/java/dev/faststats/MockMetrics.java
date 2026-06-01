package dev.faststats;

import com.google.gson.JsonObject;

final class MockMetrics extends SimpleMetrics {
    MockMetrics(final Factory factory) {
        super(factory);
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
