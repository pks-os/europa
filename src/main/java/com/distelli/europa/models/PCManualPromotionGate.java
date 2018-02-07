package com.distelli.europa.models;

import lombok.Data;
import lombok.extern.log4j.Log4j;

import java.util.Optional;

/**
 * Pipeline component representing a manual promotion gate
 */
@Log4j
@Data
public class PCManualPromotionGate extends PipelineComponent {
    /**
     * Always ends pipeline execution
     *
     * @param srcRepo Ignored
     * @param srcTag Ignored
     * @param manifestDigestSha Ignored
     * @return Always an empty Optional
     */
    @Override
    public Optional<PromotedImage> execute(ContainerRepo srcRepo, String srcTag, String manifestDigestSha) {
        return (Optional.empty());
    }
}
