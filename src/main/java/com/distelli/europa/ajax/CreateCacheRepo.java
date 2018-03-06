package com.distelli.europa.ajax;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.AjaxRequest;
import com.distelli.webserver.HTTPMethod;

public class CreateCacheRepo extends CreateLocalRepo {
    public CreateCacheRepo() {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    @Override
    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext) {
        _permissionCheck.check(ajaxRequest.getOperation(), requestContext);

        String ownerDomain = requestContext.getOwnerDomain();
        String sourceRepoId = ajaxRequest.getParam("sourceRepoId", true);

        ContainerRepo sourceRepo = _repoDb.getRepo(ownerDomain, sourceRepoId);
        if (sourceRepo == null) {
            throw new AjaxClientException("Could not find repository with the specified sourceRepoId",
                                          AjaxErrors.Codes.RepoNotFound,
                                          400);
        }
        if (sourceRepo.isLocal()) {
            throw new AjaxClientException("Cannot use a local repository as the source",
                                          AjaxErrors.Codes.BadRepoType,
                                          400);
        }
        ContainerRepo destinationRepo = (ContainerRepo)super.get(ajaxRequest, requestContext);
        sourceRepo.getSyncDestinationContainerRepoIds().add(destinationRepo.getId());
        _repoDb.save(sourceRepo);
        return destinationRepo;
    }
}
