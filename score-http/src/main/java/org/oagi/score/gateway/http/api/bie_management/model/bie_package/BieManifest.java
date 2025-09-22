package org.oagi.score.gateway.http.api.bie_management.model.bie_package;

import org.oagi.score.gateway.http.api.bie_management.model.TopLevelAsbiepSummaryRecord;
import org.oagi.score.gateway.http.common.model.Guid;

public record BieManifest(Guid uuid,
                          String libraryName,
                          String branch,
                          String den,
                          String displayName,
                          String versionId) {

    public static BieManifest newBieManifest(TopLevelAsbiepSummaryRecord topLevelAsbiep) {
        return new BieManifest(topLevelAsbiep.guid(),
                topLevelAsbiep.library().name(),
                topLevelAsbiep.release().releaseNum(),
                topLevelAsbiep.den(),
                topLevelAsbiep.displayName(),
                topLevelAsbiep.version());
    }

}
