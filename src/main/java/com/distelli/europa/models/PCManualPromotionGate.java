package com.distelli.europa.models;

import lombok.Data;
import lombok.extern.log4j.Log4j;

/**
 * Pipeline component representing a manual promotion gate
 */
@Log4j
@Data
public class PCManualPromotionGate extends PipelineComponent {
    /**
     * Always returns {@code false}
     *
     * @param srcRepo Ignored
     * @param srcTag Ignored
     * @param manifestDigestSha Ignored
     * @return Always {@code false}
     */
    @Override
    public PipelineComponentResult execute(ContainerRepo srcRepo, String srcTag, String manifestDigestSha) {
        return (new PipelineComponentResult(false, srcRepo, srcTag, manifestDigestSha));
    }
}
