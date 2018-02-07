package com.distelli.europa.models;

import com.distelli.webserver.AjaxClientException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipelineComponent {
    private String id;

    /**
     * Run the pipeline stage
     *
     * @param repo the source repo with the image being promoted
     * @param tag the tag of the image to be promoted
     * @param manifestDigestSha the SHA digest of the manifest of the image to
     *                          be promoted
     * @return the metadata about the image that was promoted, or an empty
     *         Optional if the stage failed and the pipeline should stop
     * @throws Exception
     */
    public Optional<PromotedImage> execute(ContainerRepo repo, String tag, String manifestDigestSha) throws Exception {
        return (Optional.of(new PromotedImage(repo, tag, manifestDigestSha)));
    }

    @Value
    public static final class PromotedImage {
        private final ContainerRepo repo;
        private final String tag;
        private final String manifestDigestSha;
    }

    // Used by AddPipelineComponent AJAX handler:
    // key is the location within the JSON
    public void validate(String key) throws AjaxClientException {
    }

    public static class Builder<T extends Builder<T>> {
        protected String id;
        @SuppressWarnings("unchecked")
        protected T self() {
            return (T)this;
        }
        public T id(String id) {
            this.id = id;
            return self();
        }
        public PipelineComponent build() {
            return new PipelineComponent(id);
        }
    }

    public static Builder<?> builder() {
        return new Builder();
    }
}
