package com.distelli.europa.models;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestPipeline {
    @Test
    public void testGetComponentIndex() {
        PipelineComponent componentA = PipelineComponent.builder().id("A").build();
        PipelineComponent componentB = PipelineComponent.builder().id("B").build();
        PipelineComponent componentC = PipelineComponent.builder().id("C").build();
        PipelineComponent componentD = PipelineComponent.builder().id("D").build();
        Pipeline pipeline = Pipeline.builder()
            .component(componentA)
            .component(componentB)
            .component(componentC)
            .component(componentD)
            .build();
        assertThat(pipeline.getComponentIndex("A").isPresent(), equalTo(true));
        assertThat(pipeline.getComponentIndex("B").isPresent(), equalTo(true));
        assertThat(pipeline.getComponentIndex("C").isPresent(), equalTo(true));
        assertThat(pipeline.getComponentIndex("D").isPresent(), equalTo(true));
        assertThat(pipeline.getComponentIndex("E").isPresent(), equalTo(false));
        assertThat(pipeline.getComponentIndex("A").getAsInt(), equalTo(0));
        assertThat(pipeline.getComponentIndex("B").getAsInt(), equalTo(1));
        assertThat(pipeline.getComponentIndex("C").getAsInt(), equalTo(2));
        assertThat(pipeline.getComponentIndex("D").getAsInt(), equalTo(3));
    }
}
