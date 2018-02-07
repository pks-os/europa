package com.distelli.europa.ajax;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.PipelineDb;
import com.distelli.europa.models.PCCopyToRepository;
import com.distelli.europa.models.PCManualPromotionGate;
import com.distelli.europa.models.PipelineComponent;
import com.distelli.europa.util.PermissionCheck;
import com.distelli.webserver.AjaxHelper;
import com.distelli.webserver.AjaxRequest;
import com.distelli.webserver.HTTPMethod;
import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Log4j
@Singleton
public class AddPipelineComponent extends AjaxHelper<EuropaRequestContext>
{
    private static final Map<String, Class<? extends PipelineComponent>> TYPES = new HashMap<>();
    static {
        TYPES.put("CopyToRepository", PCCopyToRepository.class);
        TYPES.put("ManualPromotionGate", PCManualPromotionGate.class);
    }

    @Inject
    private PipelineDb _db;
    @Inject
    protected PermissionCheck _permissionCheck;

    public AddPipelineComponent()
    {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext)
    {
        String typeName = ajaxRequest.getParam("type", true);
        String pipelineId = ajaxRequest.getParam("pipelineId", true);
        _permissionCheck.check(ajaxRequest.getOperation(), requestContext, pipelineId);

        Class<? extends PipelineComponent> type = TYPES.get(typeName);

        PipelineComponent component = ajaxRequest.convertContent(type, true);
        component.validate("content@"+typeName);

        _db.addPipelineComponent(
            pipelineId,
            component,
            ajaxRequest.getParam("beforeComponentId"));

        return _db.getPipeline(pipelineId);
    }
}
