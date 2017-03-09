/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api.transformers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.inject.Singleton;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class TransformerBase
{
    public TransformerBase()
    {

    }

    public String toISODateTime(Long dateTime)
    {
        if(dateTime == null)
            return null;
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME
        .withZone(ZoneId.of("UTC"))
        .format(Instant.ofEpochMilli(dateTime.longValue()));
    }
}
