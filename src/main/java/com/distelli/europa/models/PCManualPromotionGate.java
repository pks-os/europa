package com.distelli.europa.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * Pipeline component representing a manual promotion gate
 */
@Log4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PCManualPromotionGate extends PipelineComponent {
    /**
     * Set this to {@code true} to make this component execute successfully.
     */
    private boolean wasManuallyTriggered = false;

    /**
     * Returns {@code true} or {@code false} depending on whether the component was manually triggered.
     *
     * @param srcRepo Ignored
     * @param srcTag Ignored
     * @param manifestDigestSha Ignored
     * @param destinationTag Ignored
     * @return {@code true} if this component was manually triggered, {@code false} otherwise
     */
    @Override
    public boolean execute(ContainerRepo srcRepo, String srcTag, String manifestDigestSha, String destinationTag) {
        return wasManuallyTriggered;
    }

    protected PCManualPromotionGate(String id) {
        super(id);
    }

    protected PCManualPromotionGate(String id, boolean wasManuallyTriggered) {
        super(id);
        this.wasManuallyTriggered = wasManuallyTriggered;
    }

    public static class Builder<T extends Builder<T>> extends PipelineComponent.Builder<T> {
        protected boolean wasManuallyTriggered = false;

        public T wasManuallyTriggered(boolean wasManuallyTriggered) {
            this.wasManuallyTriggered = wasManuallyTriggered;
            return self();
        }
        public PCManualPromotionGate build() {
            return new PCManualPromotionGate(id, wasManuallyTriggered);
        }
    }

    public static Builder<?> builder() {
        return new Builder();
    }
}
