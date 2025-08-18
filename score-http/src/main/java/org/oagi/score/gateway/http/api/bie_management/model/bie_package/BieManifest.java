package org.oagi.score.gateway.http.api.bie_management.model.bie_package;

import org.oagi.score.gateway.http.common.model.Guid;

public record BieManifest(Guid uuid,
                          String versionId,
                          String den,
                          String displayName) {
}
