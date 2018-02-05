package com.distelli.europa.pipeline;

import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.PipelineComponent;
import com.google.inject.Injector;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Log4j
@Singleton
public class RunPipeline {
    @Inject
    private Injector _injector;

    public void runPipeline(List<PipelineComponent> components,
                            ContainerRepo srcRepo,
                            String srcTag,
                            String digest,
                            String destinationTag) {
        for ( PipelineComponent component : components) {
            try {
                _injector.injectMembers(component);
                if ( ! component.execute(srcRepo, srcTag, digest, destinationTag) ) break;
            } catch ( Exception ex ) {
                // Ignore exceptions caused by threads being interrupted:
                if ( ex instanceof java.io.InterruptedIOException ) return;
                log.error(ex.getMessage(), ex);
            }
        }
    }
}
