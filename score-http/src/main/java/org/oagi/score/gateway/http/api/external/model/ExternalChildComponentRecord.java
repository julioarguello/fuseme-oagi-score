package org.oagi.score.gateway.http.api.external.model;

import java.sql.Timestamp;

public record ExternalChildComponentRecord(
     String parentGuid,
     String parentDen,
     String childGuid,
     String childDen,
     String childPropertyTerm,
     Timestamp childLastUpdatedTimestamp,
     AssociatedComponentType type,
     String childDataType,
     Integer cardinalityMin,
     Integer cardinalityMax
)

{}

