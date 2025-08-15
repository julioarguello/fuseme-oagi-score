package org.oagi.score.gateway.http.api.bie_management.service;

import org.oagi.score.gateway.http.api.agency_id_management.model.AgencyIdListManifestId;
import org.oagi.score.gateway.http.api.agency_id_management.model.AgencyIdListSummaryRecord;
import org.oagi.score.gateway.http.api.bie_management.model.BieAssociation;
import org.oagi.score.gateway.http.api.bie_management.model.TopLevelAsbiepSummaryRecord;
import org.oagi.score.gateway.http.api.bie_management.model.abie.Abie;
import org.oagi.score.gateway.http.api.bie_management.model.asbie.Asbie;
import org.oagi.score.gateway.http.api.bie_management.model.asbiep.Asbiep;
import org.oagi.score.gateway.http.api.bie_management.model.bbie.Bbie;
import org.oagi.score.gateway.http.api.bie_management.model.bbie_sc.BbieSc;
import org.oagi.score.gateway.http.api.bie_management.model.bbiep.Bbiep;
import org.oagi.score.gateway.http.api.bie_management.model.bie_package.*;
import org.oagi.score.gateway.http.api.bie_management.repository.BiePackageQueryRepository;
import org.oagi.score.gateway.http.api.bie_management.repository.TopLevelAsbiepQueryRepository;
import org.oagi.score.gateway.http.api.cc_management.model.ascc.AsccSummaryRecord;
import org.oagi.score.gateway.http.api.cc_management.model.asccp.AsccpSummaryRecord;
import org.oagi.score.gateway.http.api.cc_management.model.bcc.BccSummaryRecord;
import org.oagi.score.gateway.http.api.cc_management.model.bccp.BccpSummaryRecord;
import org.oagi.score.gateway.http.api.code_list_management.model.CodeListManifestId;
import org.oagi.score.gateway.http.api.code_list_management.model.CodeListSummaryRecord;
import org.oagi.score.gateway.http.api.xbt_management.model.XbtManifestId;
import org.oagi.score.gateway.http.api.xbt_management.model.XbtSummaryRecord;
import org.oagi.score.gateway.http.common.model.Guid;
import org.oagi.score.gateway.http.common.model.ScoreUser;
import org.oagi.score.gateway.http.common.repository.jooq.RepositoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BiePackageManifestService {

    @Autowired
    private RepositoryFactory repositoryFactory;

    @Autowired
    private BieReadService bieReadService;

    public BiePackageManifest getBiePackageManifest(ScoreUser requester,
                                                    BiePackageId biePackageId) {

        BiePackageQueryRepository biePackageQueryRepository = repositoryFactory.biePackageQueryRepository(requester);
        BiePackageSummaryRecord currentPackage = biePackageQueryRepository.getBiePackageSummary(biePackageId);

        TopLevelAsbiepQueryRepository topLevelAsbiepQueryRepository = repositoryFactory.topLevelAsbiepQueryRepository(requester);

        boolean newBiesFromPriorPackageVersion = false;
        boolean removedBiesFromPriorPackageVersion = false;
        boolean changedBiesFromPriorPackageVersion = false;

        BiePackageSummaryRecord prevPackage =
                (currentPackage.prevBiePackageId() != null) ?
                        biePackageQueryRepository.getBiePackageSummary(currentPackage.prevBiePackageId()) : null;

        List<BieMetadata> bieMetadataList = new ArrayList<>();
        List<TopLevelAsbiepSummaryRecord> currentTopLevelAsbiepList =
                biePackageQueryRepository.getTopLevelAsbiepIdListInBiePackage(currentPackage.biePackageId())
                        .stream().map(e -> topLevelAsbiepQueryRepository.getTopLevelAsbiepSummary(e))
                        .collect(Collectors.toList());
        List<TopLevelAsbiepSummaryRecord> prevTopLevelAsbiepList = (prevPackage != null) ?
                biePackageQueryRepository.getTopLevelAsbiepIdListInBiePackage(prevPackage.biePackageId())
                        .stream().map(e -> topLevelAsbiepQueryRepository.getTopLevelAsbiepSummary(e))
                        .collect(Collectors.toList()) : Collections.emptyList();

        for (TopLevelAsbiepSummaryRecord currentTopLevelAsbiep : currentTopLevelAsbiepList) {
            BieMetadata bieMetadata = null;
            for (TopLevelAsbiepSummaryRecord prevTopLevelAsbiep : prevTopLevelAsbiepList) {
                BieTrackContext context = diff(requester, currentTopLevelAsbiep, prevTopLevelAsbiep);
                if (context != null) {
                    bieMetadata = new BieMetadata(
                            currentTopLevelAsbiep.guid().value(),
                            currentTopLevelAsbiep.version(),
                            prevTopLevelAsbiep.guid().value(),
                            currentTopLevelAsbiep.den(),
                            currentTopLevelAsbiep.propertyTerm(),
                            false,
                            context.added > 0,
                            context.removed > 0,
                            context.changed > 0,
                            false
                    );
                    break;
                }
            }

            if (bieMetadata != null) {
                if (bieMetadata.addedElementsFromPriorPackageVersion() ||
                        bieMetadata.removedElementsFromPriorPackageVersion() ||
                        bieMetadata.valueDomainChangeFromPriorPackageVersion() ||
                        bieMetadata.addedElementsReplaceExtensionFromPriorPackageVersion()) {
                    changedBiesFromPriorPackageVersion = true;
                }
            } else {
                newBiesFromPriorPackageVersion = true;
                bieMetadata = new BieMetadata(
                        currentTopLevelAsbiep.guid().value(),
                        currentTopLevelAsbiep.version(),
                        null,
                        currentTopLevelAsbiep.den(),
                        currentTopLevelAsbiep.propertyTerm(),
                        true,
                        false,
                        false,
                        false,
                        false
                );
            }

            bieMetadataList.add(bieMetadata);
        }

        for (TopLevelAsbiepSummaryRecord prevTopLevelAsbiep : prevTopLevelAsbiepList) {
            boolean matched = false;
            for (TopLevelAsbiepSummaryRecord currentTopLevelAsbiep : currentTopLevelAsbiepList) {
                BieDocument current = bieReadService.getBieDocument(requester, currentTopLevelAsbiep.topLevelAsbiepId());
                BieDocument previous = bieReadService.getBieDocument(requester, prevTopLevelAsbiep.topLevelAsbiepId());

                Asbiep currentAsbiep = current.getRootAsbiep();
                Asbiep prevAsbiep = previous.getRootAsbiep();

                AsccpSummaryRecord currentAsccp = current.getCcDocument().getAsccp(currentAsbiep.getBasedAsccpManifestId());
                AsccpSummaryRecord prevAsccp = previous.getCcDocument().getAsccp(prevAsbiep.getBasedAsccpManifestId());

                if (currentAsccp.guid().equals(prevAsccp.guid())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                removedBiesFromPriorPackageVersion = true;
            }
        }

        BiePackageMetadata biePackageMetadata = new BiePackageMetadata(
                currentPackage.name(),
                currentPackage.versionId(),
                currentPackage.versionName(),
                (prevPackage != null) ? prevPackage.versionId() : null,
                newBiesFromPriorPackageVersion,
                removedBiesFromPriorPackageVersion,
                changedBiesFromPriorPackageVersion,
                "10",
                new BieManifest(bieMetadataList));
        BiePackageManifest biePackageManifest = new BiePackageManifest(biePackageMetadata);

        return biePackageManifest;
    }

    private class BieTrackContext {

        BieDocument currentBieDocument;
        BieDocument prevBieDocument;

        int added;
        int removed;
        int changed;

        public BieTrackContext(BieDocument currentBieDocument, BieDocument prevBieDocument) {
            this.currentBieDocument = currentBieDocument;
            this.prevBieDocument = prevBieDocument;
        }
    }

    public BieTrackContext diff(ScoreUser requester,
                                TopLevelAsbiepSummaryRecord currentTopLevelAsbiep,
                                TopLevelAsbiepSummaryRecord prevTopLevelAsbiep) {

        BieDocument current = bieReadService.getBieDocument(requester, currentTopLevelAsbiep.topLevelAsbiepId());
        BieDocument previous = bieReadService.getBieDocument(requester, prevTopLevelAsbiep.topLevelAsbiepId());

        BieTrackContext context = new BieTrackContext(current, previous);

        Asbiep currentAsbiep = current.getRootAsbiep();
        Asbiep prevAsbiep = previous.getRootAsbiep();

        AsccpSummaryRecord currentAsccp = current.getCcDocument().getAsccp(currentAsbiep.getBasedAsccpManifestId());
        AsccpSummaryRecord prevAsccp = previous.getCcDocument().getAsccp(prevAsbiep.getBasedAsccpManifestId());

        if (currentAsccp.guid().equals(prevAsccp.guid())) {
            traverse(context, current.getAbie(currentAsbiep), previous.getAbie(prevAsbiep));
            return context;
        }

        return null;
    }

    private boolean traverse(BieTrackContext context, Asbie currentAsbie, Asbie prevAsbie) {
        AsccSummaryRecord currentAscc = context.currentBieDocument.getCcDocument().getAscc(currentAsbie.getBasedAsccManifestId());
        AsccSummaryRecord prevAscc = context.prevBieDocument.getCcDocument().getAscc(prevAsbie.getBasedAsccManifestId());

        if (currentAscc.guid().equals(prevAscc.guid())) {
            Asbiep currentAsbiep = context.currentBieDocument.getAsbiep(currentAsbie);
            Asbiep prevAsbiep = context.prevBieDocument.getAsbiep(prevAsbie);

            AsccpSummaryRecord currentAsccp = context.currentBieDocument.getCcDocument().getAsccp(currentAsbiep.getBasedAsccpManifestId());
            AsccpSummaryRecord prevAsccp = context.prevBieDocument.getCcDocument().getAsccp(prevAsbiep.getBasedAsccpManifestId());

            if (currentAsccp.guid().equals(prevAsccp.guid())) {
                traverse(context,
                        context.currentBieDocument.getAbie(currentAsbiep),
                        context.prevBieDocument.getAbie(prevAsbiep));
                return true;
            }
        }
        return false;
    }

    private boolean traverse(BieTrackContext context, Bbie currentBbie, Bbie prevBbie) {
        BccSummaryRecord currentBcc = context.currentBieDocument.getCcDocument().getBcc(currentBbie.getBasedBccManifestId());
        BccSummaryRecord prevBcc = context.prevBieDocument.getCcDocument().getBcc(prevBbie.getBasedBccManifestId());

        if (!currentBcc.guid().equals(prevBcc.guid())) return false;

        Bbiep currentBbiep = context.currentBieDocument.getBbiep(currentBbie);
        Bbiep prevBbiep = context.prevBieDocument.getBbiep(prevBbie);

        BccpSummaryRecord currentBccp = context.currentBieDocument.getCcDocument().getBccp(currentBbiep.getBasedBccpManifestId());
        BccpSummaryRecord prevBccp = context.prevBieDocument.getCcDocument().getBccp(prevBbiep.getBasedBccpManifestId());

        if (!currentBccp.guid().equals(prevBccp.guid())) return false;

        if (!hasSameValueDomain(context, currentBbie, prevBbie,
                Bbie::getXbtManifestId, Bbie::getCodeListManifestId, Bbie::getAgencyIdListManifestId)) {
            context.changed++;
        }

        List<BbieSc> currentScs = context.currentBieDocument.getBbieScList(currentBbie);
        List<BbieSc> prevScs = context.prevBieDocument.getBbieScList(prevBbie);

        for (BbieSc currentSc : currentScs) {
            boolean matched = false;

            for (BbieSc prevSc : prevScs) {
                if (currentSc.getGuid().equals(prevSc.getGuid())) {
                    if (!hasSameValueDomain(context, currentSc, prevSc,
                            BbieSc::getXbtManifestId, BbieSc::getCodeListManifestId, BbieSc::getAgencyIdListManifestId)) {
                        context.changed++;
                    }
                    matched = true;
                    break;
                }
            }

            if (!matched) context.added++;
        }

        for (BbieSc prevSc : prevScs) {
            boolean matched = currentScs.stream()
                    .anyMatch(currentSc -> currentSc.getGuid().equals(prevSc.getGuid()));
            if (!matched) context.removed++;
        }

        return true;
    }

    private void traverse(BieTrackContext context, Abie currentAbie, Abie prevAbie) {
        Collection<BieAssociation> currentList = context.currentBieDocument.getAssociations(currentAbie);
        Collection<BieAssociation> prevList = context.prevBieDocument.getAssociations(prevAbie);

        for (BieAssociation current : currentList) {
            boolean matched = false;

            for (BieAssociation prev : prevList) {
                if (current.isAsbie() && prev.isAsbie()) {
                    matched = traverse(context, (Asbie) current, (Asbie) prev);
                } else if (current.isBbie() && prev.isBbie()) {
                    matched = traverse(context, (Bbie) current, (Bbie) prev);
                }

                if (matched) break;
            }

            if (!matched) context.added++;
        }

        for (BieAssociation prev : prevList) {
            boolean matched = false;

            for (BieAssociation current : currentList) {
                if (current.isAsbie() && prev.isAsbie()) {
                    matched = compareAscc(context, (Asbie) current, (Asbie) prev);
                } else if (current.isBbie() && prev.isBbie()) {
                    matched = compareBcc(context, (Bbie) current, (Bbie) prev);
                }

                if (matched) break;
            }

            if (!matched) context.removed++;
        }
    }

    private <T> boolean hasSameValueDomain(
            BieTrackContext context,
            T current,
            T previous,
            Function<T, XbtManifestId> xbtIdGetter,
            Function<T, CodeListManifestId> codeListIdGetter,
            Function<T, AgencyIdListManifestId> agencyIdListIdGetter) {

        return compareGuid(
                () -> context.currentBieDocument.getCcDocument().getXbt(xbtIdGetter.apply(current)),
                () -> context.prevBieDocument.getCcDocument().getXbt(xbtIdGetter.apply(previous)),
                XbtSummaryRecord::guid
        ) || compareGuid(
                () -> context.currentBieDocument.getCcDocument().getCodeList(codeListIdGetter.apply(current)),
                () -> context.prevBieDocument.getCcDocument().getCodeList(codeListIdGetter.apply(previous)),
                CodeListSummaryRecord::guid
        ) || compareGuid(
                () -> context.currentBieDocument.getCcDocument().getAgencyIdList(agencyIdListIdGetter.apply(current)),
                () -> context.prevBieDocument.getCcDocument().getAgencyIdList(agencyIdListIdGetter.apply(previous)),
                AgencyIdListSummaryRecord::guid
        );
    }

    private <T> boolean compareGuid(Supplier<T> currentSupplier, Supplier<T> prevSupplier, Function<T, Guid> guidGetter) {
        try {
            T current = currentSupplier.get();
            T prev = prevSupplier.get();
            if (current != null && prev != null) {
                return guidGetter.apply(current).equals(guidGetter.apply(prev));
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean compareAscc(BieTrackContext context, Asbie current, Asbie prev) {
        AsccSummaryRecord currentAscc = context.currentBieDocument.getCcDocument().getAscc(current.getBasedAsccManifestId());
        AsccSummaryRecord prevAscc = context.prevBieDocument.getCcDocument().getAscc(prev.getBasedAsccManifestId());
        return currentAscc.guid().equals(prevAscc.guid());
    }

    private boolean compareBcc(BieTrackContext context, Bbie current, Bbie prev) {
        BccSummaryRecord currentBcc = context.currentBieDocument.getCcDocument().getBcc(current.getBasedBccManifestId());
        BccSummaryRecord prevBcc = context.prevBieDocument.getCcDocument().getBcc(prev.getBasedBccManifestId());
        return currentBcc.guid().equals(prevBcc.guid());
    }

}
