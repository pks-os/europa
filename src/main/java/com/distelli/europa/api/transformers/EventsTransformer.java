/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api.transformers;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.api.models.ApiEvent;
import com.distelli.europa.models.RepoEvent;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class EventsTransformer extends TransformerBase
{
    public EventsTransformer()
    {

    }

    public ApiEvent transform(RepoEvent event)
    {
        if(event == null)
            return null;

        return ApiEvent
        .builder()
        .repoId(event.getRepoId())
        .id(event.getId())
        .eventType(event.getEventType())
        .eventTime(toISODateTime(event.getEventTime()))
        .imageSize(event.getImageSize())
        .tags(event.getImageTags())
        .sha(event.getImageSha())
        .build();
    }

    public List<ApiEvent> transform(List<RepoEvent> events)
    {
        List<ApiEvent> eventList = new ArrayList<ApiEvent>();
        if(events == null || events.size() == 0)
            return eventList;
        for(RepoEvent event : events)
        {
            ApiEvent apiEvent = transform(event);
            if(apiEvent != null)
                eventList.add(apiEvent);
        }
        return eventList;
    }
}
