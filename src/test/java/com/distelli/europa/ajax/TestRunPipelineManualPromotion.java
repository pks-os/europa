package com.distelli.europa.ajax;

import com.distelli.europa.EuropaConfiguration;
import com.distelli.europa.guice.EuropaInjectorModule;
import com.distelli.europa.models.PCCopyToRepository;
import com.distelli.europa.models.Pipeline;
import com.distelli.europa.models.PipelineComponent;
import com.distelli.objectStore.impl.ObjectStoreModule;
import com.distelli.persistence.impl.PersistenceModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestRunPipelineManualPromotion {
    private static Injector INJECTOR = createInjector();

    private static Injector createInjector() {
        String path = System.getenv("EUROPA_CONFIG");
        EuropaConfiguration config = null;
        if(path != null) {
            File file = new File(path);
            if(!file.exists())
                throw(new IllegalStateException("Invalid value for EUROPA_CONFIG env var: "+path));
            config = EuropaConfiguration.fromFile(file);
        } else {
            config = EuropaConfiguration.fromEnvironment();
        }
        return Guice.createInjector(
            new PersistenceModule(),
            new ObjectStoreModule(),
            new EuropaInjectorModule(
                config));
    }

    @Inject
    private RunPipelineManualPromotion _runPipelineManualPromotion;

    @Before
    public void before() throws Exception {
        if ( null == INJECTOR ) {
            throw new RuntimeException("EUROPA_CONFIG environment variable must point to valid file");
        }
        INJECTOR.injectMembers(this);
    }

    @Test
    public void testGetComponentsToRun() {
        Pipeline pipeline = Pipeline.builder()
            .name("super-pipes")
            .domain(UUID.randomUUID().toString())
            .components(
                Arrays.asList(
                    PCCopyToRepository.builder()
                        .id(UUID.randomUUID().toString())
                        .destinationContainerRepoId("A")
                        .build(),
                    PCCopyToRepository.builder()
                        .id(UUID.randomUUID().toString())
                        .destinationContainerRepoId("B")
                        .build(),
                    PCCopyToRepository.builder()
                        .id(UUID.randomUUID().toString())
                        .destinationContainerRepoId("C")
                        .build(),
                    PCCopyToRepository.builder()
                        .id(UUID.randomUUID().toString())
                        .destinationContainerRepoId("D")
                        .build(),
                    PCCopyToRepository.builder()
                        .id(UUID.randomUUID().toString())
                        .destinationContainerRepoId("E")
                        .build()))
            .build();

        List<PipelineComponent> expectedResult = pipeline.getComponents().subList(1,5);
        String componentId = pipeline.getComponents()
            .stream()
            .filter((pipelineComponent) -> ((PCCopyToRepository)pipelineComponent).getDestinationContainerRepoId().equalsIgnoreCase("B"))
            .map(PipelineComponent::getId)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Can't find component for some reason"));
        List<PipelineComponent> actualResult = _runPipelineManualPromotion.getComponentsToRun(pipeline, componentId);
        assertThat(actualResult, equalTo(expectedResult));
    }
}
