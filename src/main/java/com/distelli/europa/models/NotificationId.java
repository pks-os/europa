/*
  $Id: $
  @file NotificationId.java
  @brief Contains the NotificationId.java class

  @author Rahul Singh [rsingh]
*/
package com.distelli.europa.models;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.distelli.europa.models.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationId
{
    protected String id;
    protected NotificationType type;

    public String toCanonicalId()
    {
        return String.format("%s:%s",
                             this.type,
                             this.id);
    }

    public static NotificationId fromCanonicalId(String canonicalId)
    {
        String[] parts = canonicalId.split(":", 2);
        NotificationType type = NotificationType.valueOf(parts[0].toUpperCase());
        NotificationId nfId = NotificationId
        .builder()
        .id(parts[1])
        .type(type)
        .build();
        return nfId;
    }
}
