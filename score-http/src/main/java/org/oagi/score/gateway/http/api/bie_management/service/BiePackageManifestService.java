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
import org.oagi.score.gateway.http.api.cc_management.model.acc.AccSummaryRecord;
import org.oagi.score.gateway.http.api.cc_management.model.ascc.AsccSummaryRecord;
import org.oagi.score.gateway.http.api.cc_management.model.asccp.AsccpSummaryRecord;
import org.oagi.score.gateway.http.api.cc_management.model.bcc.BccSummaryRecord;
import org.oagi.score.gateway.http.api.cc_management.model.bccp.BccpSummaryRecord;
import org.oagi.score.gateway.http.api.cc_management.model.dt.DtSummaryRecord;
import org.oagi.score.gateway.http.api.cc_management.model.dt_sc.DtScSummaryRecord;
import org.oagi.score.gateway.http.api.cc_management.repository.AccQueryRepository;
import org.oagi.score.gateway.http.api.cc_management.repository.AsccpQueryRepository;
import org.oagi.score.gateway.http.api.cc_management.repository.BccpQueryRepository;
import org.oagi.score.gateway.http.api.cc_management.repository.DtQueryRepository;
import org.oagi.score.gateway.http.api.code_list_management.model.CodeListManifestId;
import org.oagi.score.gateway.http.api.code_list_management.model.CodeListSummaryRecord;
import org.oagi.score.gateway.http.api.library_management.model.LibraryDetailsRecord;
import org.oagi.score.gateway.http.api.library_management.model.LibraryId;
import org.oagi.score.gateway.http.api.library_management.repository.LibraryQueryRepository;
import org.oagi.score.gateway.http.api.release_management.model.ReleaseDetailsRecord;
import org.oagi.score.gateway.http.api.release_management.model.ReleaseId;
import org.oagi.score.gateway.http.api.release_management.model.ReleaseSummaryRecord;
import org.oagi.score.gateway.http.api.release_management.repository.ReleaseQueryRepository;
import org.oagi.score.gateway.http.api.xbt_management.model.XbtManifestId;
import org.oagi.score.gateway.http.api.xbt_management.model.XbtSummaryRecord;
import org.oagi.score.gateway.http.common.model.Guid;
import org.oagi.score.gateway.http.common.model.ScoreUser;
import org.oagi.score.gateway.http.common.repository.jooq.RepositoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    public BiePackageManifestResponse getBiePackageManifest(ScoreUser requester,
                                                            BiePackageId biePackageId) {

        BiePackageQueryRepository biePackageQueryRepository = repositoryFactory.biePackageQueryRepository(requester);
        BiePackageSummaryRecord currentPackage = biePackageQueryRepository.getBiePackageSummary(biePackageId);

        TopLevelAsbiepQueryRepository topLevelAsbiepQueryRepository = repositoryFactory.topLevelAsbiepQueryRepository(requester);

        Collection<BieManifestSummary> newBiesFromPriorPackageVersion = new ArrayList<>();
        Collection<BieManifestSummary> removedBiesFromPriorPackageVersion = new ArrayList<>();
        Collection<BieManifestSummary> changedBiesFromPriorPackageVersion = new ArrayList<>();
        Collection<BieManifestSummary> deprecatedBiesFromPriorPackageVersion = new ArrayList<>();
        Map<LibraryId, Collection<ReleaseSummaryRecord>> releaseMapByLibraryId = new HashMap<>();

        BiePackageSummaryRecord prevPackage =
                (currentPackage.prevBiePackageId() != null) ?
                        biePackageQueryRepository.getBiePackageSummary(currentPackage.prevBiePackageId()) : null;

        List<BieManifestEntry> bieManifestEntryList = new ArrayList<>();
        List<TopLevelAsbiepSummaryRecord> currentTopLevelAsbiepList =
                biePackageQueryRepository.getTopLevelAsbiepIdListInBiePackage(currentPackage.biePackageId())
                        .stream().map(e -> topLevelAsbiepQueryRepository.getTopLevelAsbiepSummary(e))
                        .collect(Collectors.toList());
        List<TopLevelAsbiepSummaryRecord> prevTopLevelAsbiepList = (prevPackage != null) ?
                biePackageQueryRepository.getTopLevelAsbiepIdListInBiePackage(prevPackage.biePackageId())
                        .stream().map(e -> topLevelAsbiepQueryRepository.getTopLevelAsbiepSummary(e))
                        .collect(Collectors.toList()) : Collections.emptyList();

        for (TopLevelAsbiepSummaryRecord currentTopLevelAsbiep : currentTopLevelAsbiepList) {

            releaseMapByLibraryId.putIfAbsent(currentTopLevelAsbiep.library().libraryId(), new ArrayList<>());
            releaseMapByLibraryId.get(currentTopLevelAsbiep.library().libraryId()).add(currentTopLevelAsbiep.release());

            BieManifestEntry bieManifestEntry = null;
            for (TopLevelAsbiepSummaryRecord prevTopLevelAsbiep : prevTopLevelAsbiepList) {
                BieTrackContext context = diff(requester, currentTopLevelAsbiep, prevTopLevelAsbiep);
                if (context != null) {
                    bieManifestEntry = new BieManifestEntry(
                            new BieManifest(
                                    currentTopLevelAsbiep.guid(),
                                    currentTopLevelAsbiep.version(),
                                    currentTopLevelAsbiep.den(),
                                    currentTopLevelAsbiep.displayName()
                            ),
                            prevTopLevelAsbiep.guid(),
                            prevTopLevelAsbiep.version(),
                            true,
                            context.added,
                            context.removed,
                            context.valueDomainChanged,
                            context.deprecated
                    );
                    break;
                }
            }

            if (bieManifestEntry != null) {
                if (!bieManifestEntry.addedElementsFromPriorPackageVersion().isEmpty() ||
                        !bieManifestEntry.removedElementsFromPriorPackageVersion().isEmpty() ||
                        !bieManifestEntry.valueDomainChangeFromPriorPackageVersion().isEmpty() ||
                        !bieManifestEntry.deprecatedElementsFromPriorPackageVersion().isEmpty()) {
                    changedBiesFromPriorPackageVersion.add(new BieManifestSummary(
                            currentTopLevelAsbiep.guid(),
                            currentTopLevelAsbiep.den(),
                            currentTopLevelAsbiep.displayName()
                    ));
                }
            } else {
                newBiesFromPriorPackageVersion.add(new BieManifestSummary(
                        currentTopLevelAsbiep.guid(),
                        currentTopLevelAsbiep.den(),
                        currentTopLevelAsbiep.displayName()
                ));
                bieManifestEntry = new BieManifestEntry(
                        new BieManifest(
                                currentTopLevelAsbiep.guid(),
                                currentTopLevelAsbiep.version(),
                                currentTopLevelAsbiep.den(),
                                currentTopLevelAsbiep.displayName()
                        ),
                        null,
                        null,
                        false,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                );
            }

            bieManifestEntryList.add(bieManifestEntry);
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
                removedBiesFromPriorPackageVersion.add(new BieManifestSummary(
                        prevTopLevelAsbiep.guid(),
                        prevTopLevelAsbiep.den(),
                        prevTopLevelAsbiep.displayName()
                ));
            }
        }

        LibraryQueryRepository libraryQueryRepository = repositoryFactory.libraryQueryRepository(requester);
        Collection<LibraryCompatibility> libraryCompatibilityCollection = new ArrayList<>();
        for (LibraryId libraryId : releaseMapByLibraryId.keySet()) {
            LibraryDetailsRecord libraryDetailsRecord = libraryQueryRepository.getLibraryDetails(libraryId);
            Collection<ReleaseSummaryRecord> releases = releaseMapByLibraryId.get(libraryId);
            ReleaseSummaryRecord latestReleasesInCollection = findLatestReleasesIn(requester, libraryId, releases);
            libraryCompatibilityCollection.add(new LibraryCompatibility(
                    libraryDetailsRecord.name(),
                    latestReleasesInCollection.releaseNum()
            ));
        }

        BiePackageManifest biePackageMetadata = new BiePackageManifest(
                currentPackage.name(),
                currentPackage.versionId(),
                currentPackage.versionName(),
                (prevPackage != null) ? prevPackage.versionId() : null,
                newBiesFromPriorPackageVersion,
                removedBiesFromPriorPackageVersion,
                changedBiesFromPriorPackageVersion,
                deprecatedBiesFromPriorPackageVersion,
                libraryCompatibilityCollection,
                bieManifestEntryList);
        BiePackageManifestResponse biePackageManifest = new BiePackageManifestResponse(biePackageMetadata);

        return biePackageManifest;
    }

    private ReleaseSummaryRecord findLatestReleasesIn(ScoreUser requester,
                                                      LibraryId libraryId,
                                                      Collection<ReleaseSummaryRecord> releases) {
        if (releases == null || releases.isEmpty()) {
            return null;
        }

        ReleaseQueryRepository releaseQueryRepository = repositoryFactory.releaseQueryRepository(requester);
        List<ReleaseDetailsRecord> releaseDetailsRecords =
                releaseQueryRepository.getReleaseSummaryList(libraryId).stream()
                        .map(e -> releaseQueryRepository.getReleaseDetails(e.releaseId()))
                        .collect(Collectors.toList());
        Map<ReleaseId, ReleaseDetailsRecord> releaseInDatabaseMap = releaseDetailsRecords.stream()
                .collect(Collectors.toMap(ReleaseDetailsRecord::releaseId, Function.identity()));
        ReleaseDetailsRecord currentRelease = releaseDetailsRecords.stream()
                .filter(e -> e.next() == null).findFirst().orElse(null);

        Map<ReleaseId, ReleaseSummaryRecord> releaseInCollectionMap = releases.stream()
                .collect(Collectors.toMap(ReleaseSummaryRecord::releaseId, Function.identity()));

        while (currentRelease != null) {
            if (releaseInCollectionMap.containsKey(currentRelease.releaseId())) {
                return releaseInCollectionMap.get(currentRelease.releaseId());
            }

            if (currentRelease.prev() != null) {
                currentRelease = releaseInDatabaseMap.get(currentRelease.prev().releaseId());
            } else {
                break;
            }
        }

        throw new IllegalStateException("No latest release in the provided collection.");
    }

    private class BieTrackContext {

        ScoreUser requester;

        BieDocument currentBieDocument;
        BieDocument prevBieDocument;

        Collection<BieElementChange> added = new ArrayList<>();
        Collection<BieElementChange> removed = new ArrayList<>();
        Collection<BieElementChange> valueDomainChanged = new ArrayList<>();
        Collection<BieElementChange> deprecated = new ArrayList<>();

        public BieTrackContext(ScoreUser requester,
                               BieDocument currentBieDocument, BieDocument prevBieDocument) {
            this.requester = requester;

            this.currentBieDocument = currentBieDocument;
            this.prevBieDocument = prevBieDocument;
        }
    }

    public BieTrackContext diff(ScoreUser requester,
                                TopLevelAsbiepSummaryRecord currentTopLevelAsbiep,
                                TopLevelAsbiepSummaryRecord prevTopLevelAsbiep) {

        BieDocument current = bieReadService.getBieDocument(requester, currentTopLevelAsbiep.topLevelAsbiepId());
        BieDocument previous = bieReadService.getBieDocument(requester, prevTopLevelAsbiep.topLevelAsbiepId());

        BieTrackContext context = new BieTrackContext(requester, current, previous);

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

            context.valueDomainChanged.add(newBieElementChange(context, currentBbie));
        }

        if (currentBbie.isDeprecated() && !prevBbie.isDeprecated()) {
            context.deprecated.add(newBieElementChange(context, currentBbie));
        }

        List<BbieSc> currentScs = context.currentBieDocument.getBbieScList(currentBbie);
        List<BbieSc> prevScs = context.prevBieDocument.getBbieScList(prevBbie);

        for (BbieSc currentSc : currentScs) {
            boolean matched = false;

            for (BbieSc prevSc : prevScs) {
                if (currentSc.getGuid().equals(prevSc.getGuid())) {
                    if (!hasSameValueDomain(context, currentSc, prevSc,
                            BbieSc::getXbtManifestId, BbieSc::getCodeListManifestId, BbieSc::getAgencyIdListManifestId)) {

                        context.valueDomainChanged.add(newBieElementChange(context, currentSc));
                    }

                    if (currentSc.isDeprecated() && !prevSc.isDeprecated()) {
                        context.deprecated.add(newBieElementChange(context, currentBbie));
                    }

                    matched = true;
                    break;
                }
            }

            if (!matched) context.added.add(newBieElementChange(context, currentSc));
        }

        for (BbieSc prevSc : prevScs) {
            boolean matched = currentScs.stream()
                    .anyMatch(currentSc -> currentSc.getGuid().equals(prevSc.getGuid()));
            if (!matched) context.removed.add(newBieElementChange(context, prevSc));
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

            if (!matched) {
                if (current.isAsbie()) {
                    context.added.add(newBieElementChange(context, (Asbie) current));
                } else {
                    context.added.add(newBieElementChange(context, (Bbie) current));
                }
            }
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

            if (!matched)
                if (prev.isAsbie()) {
                    context.removed.add(newBieElementChange(context, (Asbie) prev));
                } else {
                    context.removed.add(newBieElementChange(context, (Bbie) prev));
                }
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

    private BieElementChange newBieElementChange(BieTrackContext context, Asbie asbie) {
        AccQueryRepository accQueryRepository = repositoryFactory.accQueryRepository(context.requester);
        AsccSummaryRecord basedAscc = accQueryRepository.getAsccSummary(asbie.getBasedAsccManifestId());

        AccSummaryRecord fromAcc = accQueryRepository.getAccSummary(basedAscc.fromAccManifestId());

        AsccpQueryRepository asccpQueryRepository = repositoryFactory.asccpQueryRepository(context.requester);
        AsccpSummaryRecord toAsccp = asccpQueryRepository.getAsccpSummary(basedAscc.toAsccpManifestId());

        return new BieElementChange(
                toAsccp.propertyTerm(),
                fromAcc.objectClassTerm());
    }

    private BieElementChange newBieElementChange(BieTrackContext context, Bbie bbie) {
        AccQueryRepository accQueryRepository = repositoryFactory.accQueryRepository(context.requester);
        BccSummaryRecord basedBcc = accQueryRepository.getBccSummary(bbie.getBasedBccManifestId());

        AccSummaryRecord fromAcc = accQueryRepository.getAccSummary(basedBcc.fromAccManifestId());

        BccpQueryRepository bccpQueryRepository = repositoryFactory.bccpQueryRepository(context.requester);
        BccpSummaryRecord toBccp = bccpQueryRepository.getBccpSummary(basedBcc.toBccpManifestId());

        return new BieElementChange(
                toBccp.propertyTerm(),
                fromAcc.objectClassTerm());
    }

    private BieElementChange newBieElementChange(BieTrackContext context, BbieSc bbieSc) {
        DtQueryRepository dtQueryRepository = repositoryFactory.dtQueryRepository(context.requester);
        DtScSummaryRecord dtSc = dtQueryRepository.getDtScSummary(bbieSc.getBasedDtScManifestId());
        DtSummaryRecord dt = dtQueryRepository.getDtSummary(dtSc.ownerDtManifestId());

        return new BieElementChange(
                dtSc.den(),
                dt.den().replaceAll(". Type", ""));
    }

}
