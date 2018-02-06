package com.distelli.europa.models;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestPCManualPromotionGate {
    @Test
    public void test() throws Exception {
        PipelineComponent pipelineComponent = new PCManualPromotionGate();
        PipelineComponent.PipelineComponentResult result = pipelineComponent.execute(null, null, null);
        assertThat(result.isSuccessful(), equalTo(false));
    }
}
