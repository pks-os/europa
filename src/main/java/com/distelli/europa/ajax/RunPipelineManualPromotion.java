package com.distelli.europa.ajax;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.db.PipelineDb;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.PCCopyToRepository;
import com.distelli.europa.models.Pipeline;
import com.distelli.europa.models.PipelineComponent;
import com.distelli.europa.models.RegistryManifest;
import com.distelli.europa.pipeline.RunPipeline;
import com.distelli.europa.util.PermissionCheck;
import com.distelli.europa.util.Tag;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.AjaxHelper;
import com.distelli.webserver.AjaxRequest;
import com.distelli.webserver.HTTPMethod;

import javax.inject.Inject;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class RunPipelineManualPromotion extends AjaxHelper<EuropaRequestContext> {
    @Inject
    protected PermissionCheck _permissionCheck;
    @Inject
    private ContainerRepoDb _repoDb;
    @Inject
    private PipelineDb _pipelineDb;
    @Inject
    private RegistryManifestDb _manifestDb;
    @Inject
    private RunPipeline _runPipeline;

    public RunPipelineManualPromotion() {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    @Override
    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext) {
        String pipelineId = ajaxRequest.getParam("pipelineId", true);
        String componentId = ajaxRequest.getParam("componentId", true);
        String sourceRepoId = ajaxRequest.getParam("sourceRepoId", true);
        String sourceTag = ajaxRequest.getParam("sourceTag", true);
        String destinationTag = ajaxRequest.getParam("destinationTag", true);

        _permissionCheck.check(ajaxRequest.getOperation(), requestContext, pipelineId);

        if (!Tag.isValid(destinationTag)) {
            throw(new AjaxClientException("The specified destination tag is not valid",
                                          AjaxErrors.Codes.BadTagName, 400));
        }

        String domain = requestContext.getOwnerDomain();
        Pipeline pipeline = _pipelineDb.getPipeline(pipelineId);
        ContainerRepo sourceRepo = _repoDb.getRepo(domain, sourceRepoId);
        RegistryManifest manifest = _manifestDb.getManifestByRepoIdTag(domain, sourceRepoId, sourceTag);
        String manifestId = (manifest == null) ? null : manifest.getManifestId();

        if (pipeline == null) {
            throw(new AjaxClientException("The specified Pipeline was not found",
                                          AjaxErrors.Codes.PipelineNotFound, 404));
        }
        if (sourceRepo == null) {
            throw(new AjaxClientException("The specified source repo was not found",
                                          AjaxErrors.Codes.RepoNotFound, 404));
        }

        List<PipelineComponent> componentsToRun = getComponentsToRun(pipeline, componentId);

        // Configure the destinationTag where appropriate
        if ((destinationTag != null) && (componentsToRun.get(0) instanceof PCCopyToRepository)) {
            ((PCCopyToRepository) componentsToRun.get(0)).setTag(destinationTag);
        }

        _runPipeline.runPipeline(componentsToRun, sourceRepo, sourceTag, sourceTag);

        return _pipelineDb.getPipeline(pipelineId);
    }

    protected List<PipelineComponent> getComponentsToRun(Pipeline pipeline, String componentId) {
        List<PipelineComponent> components = pipeline.getComponents();
        OptionalInt componentIndex = IntStream.range(0, components.size())
            .filter((i) -> componentId.equalsIgnoreCase(components.get(i).getId()))
            .findFirst();
        if (!componentIndex.isPresent()) {
            throw(new AjaxClientException("The specified PipelineComponent is not in the specified Pipeline",
                                          AjaxErrors.Codes.BadPipelineComponent, 400));
        }
        return components.subList(componentIndex.getAsInt(), components.size());

    }
}
