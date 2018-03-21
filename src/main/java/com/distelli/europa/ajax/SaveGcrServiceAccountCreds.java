/*
  $Id: $
  @file SaveGcrServiceAccountCreds.java
  @brief Contains the SaveGcrServiceAccountCreds.java class

  @author Rahul Singh [rsingh]
*/
package com.distelli.europa.ajax;

import com.distelli.europa.Constants;
import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.RegistryCredsDb;
import com.distelli.europa.models.RegistryCred;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.util.FieldValidator;
import com.distelli.europa.util.PermissionCheck;
import com.distelli.gcr.GcrClient;
import com.distelli.gcr.GcrIterator;
import com.distelli.gcr.GcrRegion;
import com.distelli.gcr.auth.GcrServiceAccountCredentials;
import com.distelli.gcr.models.GcrRepository;
import com.distelli.utils.CompactUUID;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.AjaxHelper;
import com.distelli.webserver.AjaxRequest;
import com.distelli.webserver.HTTPMethod;
import com.distelli.webserver.JsonError;
import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.List;

@Log4j
@Singleton
public class SaveGcrServiceAccountCreds extends AjaxHelper<EuropaRequestContext>
{
    @Inject
    private RegistryCredsDb _db;
    @Inject
    protected PermissionCheck _permissionCheck;
    @Inject
    private Provider<GcrClient.Builder> _gcrClientBuilderProvider;

    public SaveGcrServiceAccountCreds()
    {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext)
    {
        RegistryCred cred = ajaxRequest.convertContent(RegistryCred.class,
                                                       true); //throw if null
        //Validate that the fields we want are non-null
        FieldValidator.validateNonNull(cred, "provider", "region", "secret");
        FieldValidator.validateMatch(cred, "name", Constants.REGISTRY_CRED_NAME_PATTERN);
        FieldValidator.validateEquals(cred, "provider", RegistryProvider.GCR);
        validateRegistryCreds(cred);
        cred.setCreated(System.currentTimeMillis());
        String id = cred.getId();
        String credDomain = requestContext.getOwnerDomain();
        cred.setDomain(credDomain);
        if(id != null) {
            _permissionCheck.check(ajaxRequest.getOperation(), requestContext, Boolean.TRUE);
            //check that cred with that id exists
            RegistryCred existingCred = _db.getCred(credDomain, id.toLowerCase());
            if(existingCred == null)
                throw(new AjaxClientException("Invalid Registry Cred Id: "+id, JsonError.Codes.BadContent, 400));
        } else {
            _permissionCheck.check(ajaxRequest.getOperation(), requestContext, Boolean.FALSE);
            id = CompactUUID.randomUUID().toString();
            cred.setId(id);
        }

        //save in the db
        _db.save(cred);
        HashMap<String, String> retVal = new HashMap<String, String>();
        retVal.put("id", id);
        return retVal;
    }

    private void validateRegistryCreds(RegistryCred cred) {
        GcrClient gcrClient = _gcrClientBuilderProvider.get()
            .gcrCredentials(new GcrServiceAccountCredentials(cred.getSecret()))
            .gcrRegion(GcrRegion.getRegionByEndpoint(cred.getRegion()))
            .build();

        GcrIterator iter = GcrIterator.builder().pageSize(1).build();
        try {
            List<GcrRepository> repos = gcrClient.listRepositories(iter);
        } catch(Throwable t) {
            throw(new AjaxClientException("Invalid Credentials: "+t.getMessage(), JsonError.Codes.BadContent, 400));
        }
    }
}
