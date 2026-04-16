package com.chatrealtime.observability;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomSpanHandler implements ObservationHandler<Observation.Context> {
    @Override
    public void onStart(Observation.Context context) {
        context.addHighCardinalityKeyValue(KeyValue.of("component", "in-chat-backend"));
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }
}
