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
     * @param promotedImage Ignored
     * @return Always an empty Optional
     */
    @Override
    public Optional<PromotedImage> execute(PromotedImage promotedImage) {
        return (Optional.empty());
    }
}
