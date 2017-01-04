/*
  $Id: $
  @file GetNotificationRecord.java
  @brief Contains the GetNotificationRecord.java class

  @author Rahul Singh [rsingh]
*/
package com.distelli.europa.ajax;

import java.io.IOException;
import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;

import com.distelli.europa.models.*;
import com.distelli.europa.notifiers.*;
import com.distelli.europa.util.*;
import com.distelli.objectStore.*;
import com.distelli.webserver.HTTPMethod;
import com.google.inject.Singleton;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class GetNotificationRecord extends AjaxHelper
{
    @Inject
    protected ObjectStore _objectStore;
    @Inject
    protected ObjectKeyFactory _objectKeyFactory;

    public GetNotificationRecord()
    {
        this.supportedHttpMethods.add(HTTPMethod.GET);
    }

    public Object get(AjaxRequest ajaxRequest)
    {
        String id = ajaxRequest.getParam("notificationId",
                                         true); //throw if missing
        NotificationId notificationId = NotificationId.fromCanonicalId(id);
        NotificationType type = notificationId.getType();
        switch(type)
        {
        case WEBHOOK:
            ObjectKey objectKey = _objectKeyFactory.forWebhookRecord(notificationId);
            try {
                byte[] recordBytes = _objectStore.get(objectKey);
                return WebhookRecord.fromJsonBytes(recordBytes);
            } catch(EntityNotFoundException enfe) {
                return null;
            } catch(IOException ioe) {
                throw(new RuntimeException(ioe));
            }
        case EMAIL:
        case SLACK:
        case HIPCHAT:
        default:
            return null;
        }
    }
}