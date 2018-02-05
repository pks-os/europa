package com.distelli.europa.models;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestPCManualPromotionGate {
    @Test
    public void testFailure() throws Exception {
        PipelineComponent pipelineComponent = PCManualPromotionGate.builder()
            .id("dummy")
            .build();
        boolean result = pipelineComponent.execute(null, null, null, null);
        assertThat(result, equalTo(false));
    }

    @Test
    public void testSuccess() throws Exception {
        PipelineComponent pipelineComponent = PCManualPromotionGate.builder()
            .id("dummy")
            .wasManuallyTriggered(true)
            .build();
        boolean result = pipelineComponent.execute(null, null, null, null);
        assertThat(result, equalTo(true));
    }
}
