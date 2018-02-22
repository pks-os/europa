package com.distelli.europa.pipeline;

import com.distelli.europa.EuropaConfiguration;
import com.distelli.europa.guice.EuropaInjectorModule;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.PipelineComponent;
import com.distelli.objectStore.impl.ObjectStoreModule;
import com.distelli.persistence.impl.PersistenceModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestRunPipeline {
    private static Injector INJECTOR = createInjector();

    @Inject
    private RunPipeline _runPipeline;

    private static Injector createInjector() {
        String path = System.getenv("EUROPA_CONFIG");
        EuropaConfiguration config = null;
         if (path != null) {
            File file = new File(path);
             if (!file.exists())
                 throw (new IllegalStateException("Invalid value for EUROPA_CONFIG env var: " + path));
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


    @Before
    public void before() throws Exception {
         if (null == INJECTOR) {
             throw new RuntimeException("EUROPA_CONFIG environment variable must point to valid file");

        }
        INJECTOR.injectMembers(this);

    }

    @Test
    public void testRunPipeline_simple() {
        List<PCTestComponent> components = getComponents(true, true, true);
        PipelineComponent.PromotedImage image = new PipelineComponent.PromotedImage(new ContainerRepo(),
                                                                                    "dummy",
                                                                                    "dummy");
        _runPipeline.runPipeline(components,
                                 image.getRepo(),
                                 image.getTag(),
                                 image.getManifestDigestSha());
        for (int i = 0; i < components.size(); i++) {
            int actualCallCount = components.get(i).getCalls().size();
            int expectedCallCount = 1;
            assertThat(String.format("Component %d was called %d times, expected %d time(s)", i, actualCallCount, expectedCallCount),
                       actualCallCount, equalTo(expectedCallCount));
        }
    }

    @Test
    public void testRunPipeline_failure() {
        List<PCTestComponent> components = getComponents(true, true, false, true);
        List<Integer> callCounts = Arrays.asList(1, 1, 1, 0);
        PipelineComponent.PromotedImage image = new PipelineComponent.PromotedImage(new ContainerRepo(),
                                                                                    "dummy",
                                                                                    "dummy");
        _runPipeline.runPipeline(components,
                                 image.getRepo(),
                                 image.getTag(),
                                 image.getManifestDigestSha());
        for (int i = 0; i < components.size(); i++) {
            int actualCallCount = components.get(i).getCalls().size();
            int expectedCallCount = callCounts.get(i);
            assertThat(String.format("Component %d was called %d times, expected %d time(s)", i, actualCallCount, expectedCallCount),
                       actualCallCount, equalTo(expectedCallCount));
        }
    }

    @Test
    public void testRunPipeline_changeTag() {
        List<PCTestComponent> components = getComponents(true, true, true);
        components.get(1).setTag("overridden");
        PipelineComponent.PromotedImage image = new PipelineComponent.PromotedImage(new ContainerRepo(),
                                                                                    "dummy",
                                                                                    "dummy");
        _runPipeline.runPipeline(components,
                                 image.getRepo(),
                                 image.getTag(),
                                 image.getManifestDigestSha());
        for (int i = 0; i < components.size(); i++) {
            int actualCallCount = components.get(i).getCalls().size();
            int expectedCallCount = 1;
            assertThat(String.format("Component %d was called %d times, expected %d time(s)", i, actualCallCount, expectedCallCount),
                       actualCallCount, equalTo(expectedCallCount));
        }
        assertComponentCalledWithTag(components.get(0), 0, "dummy");
        assertComponentCalledWithTag(components.get(1), 1, "dummy");
        assertComponentCalledWithTag(components.get(2), 2, "overridden");
    }

    private void assertComponentCalledWithTag(PCTestComponent component, int componentIndex, String tag) {
        String actualTag = component.getCalls().get(0).getTag();
        String message = String.format("Component %d was called with tag '%s', expected tag '%s'", componentIndex, actualTag, tag);
        assertThat(message, actualTag, equalTo(tag));
    }

    @Data
    @RequiredArgsConstructor
    public static class PCTestComponent extends PipelineComponent {
        private final boolean succeed;
        private String tag;
        private List<PromotedImage> calls = new ArrayList<>();

        /**
         * Run the pipeline stage
         *
         * @param promotedImage the metadata about the previous image
         * @return the input if {@code succeed} is true, or an empty Optional if not
         * @throws Exception
         */
        @Override
        public Optional<PromotedImage> execute(PromotedImage promotedImage) throws Exception {
            calls.add(promotedImage);
            if (succeed) {
                return Optional.of(new PromotedImage(promotedImage.getRepo(),
                                                     (tag == null) ? promotedImage.getTag() : tag,
                                                     promotedImage.getManifestDigestSha()));
            } else {
                return Optional.empty();
            }
        }
    }

    /**
     * Generate a list of dummy components
     *
     * @param values any number of booleans representing whether the component at that position should succeed ({@code true}) or fail ({@code false})
     * @return a list of components which will succeed or fail based on the input
     */
    private List<PCTestComponent> getComponents(boolean... values) {
        List<PCTestComponent> components = new ArrayList<>(values.length);
        for (boolean success : values) {
            components.add(new PCTestComponent(success));
        }
        return components;
    }
}
