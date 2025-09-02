import { Injectable, HttpException, HttpStatus, ServiceUnavailableException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { map } from 'rxjs/operators';
import { catchError, firstValueFrom } from 'rxjs';
import { HttpService } from '@nestjs/axios';
import { AxiosError } from 'axios';
import { ComponentsService } from 'src/components/components.service';
import { AxiosHelper } from '../common/gateway_api_helper';
import { Bie, BIEs, BiesWithChildren, BieWithChildren } from './bie.dto';
import { plainToInstance } from 'class-transformer';
import { CacheHelper } from 'src/common/cache_helper';
import { BiePackages, BiePackageWithBies, BiePackageWithBiesWithChildren } from './bie_package.dto';


@Injectable()
export class BieService {

    constructor(
        private readonly httpService: HttpService, private readonly configService: ConfigService,
        private readonly componentService: ComponentsService, private readonly axiosHelper: AxiosHelper, private readonly cacheHelper: CacheHelper
    ) { }



    private async mergeComponentInfo(bieMetadata: BIEs, libraryName: string): Promise<BIEs> {

        if (bieMetadata?.businessInformationEntities) {
            let bies = bieMetadata.businessInformationEntities;
            for (var i = 0; i < bies.length; i++) {
                const components = (await this.componentService.getAsccpComponents(libraryName, bies[i].branchCreatedWith)).components;
                for (var j = 0; j < components.length; j++) {
                    if (bies[i].den == components[j].den) {
                        bies[i].componentTag = components[j].nounBodVerbTag;
                        bies[i].componentDefinition = components[j].definition;
                        bies[i].componentState = components[j].state;
                        bies[i].componentUuid = components[j].uuid;
                        bies[i].componentState = components[j].state;
                        bies[i].fromNewComponent = components[j].newComponent;
                        bies[i].componentSinceReleaseNum = components[j].sinceReleaseNum;
                        bies[i].componentLastChangedReleaseNum = components[j].updatedReleaseNum;
                        break;
                    }
                }
            }
        }
        return bieMetadata;
    }


    async getBieSchema(libraryName: string, uuid: string, schemaType = 'xsd', state?: string) {

        const schemaUrlXsd = this.axiosHelper.getBackendUrl("gateway_external_api.bie_generate_backend_endpoint");

        if (schemaType != 'xsd') { //&& schemaType!='json') { //TODO:  JSON
            throw new HttpException("Unsupported schema type", HttpStatus.BAD_REQUEST);
        }

        const standalone = await
            this.httpService.get(schemaUrlXsd,
                {
                    params:
                        { guid: uuid }
                }
            )
                .pipe(map(response => {
                    return response.data;
                }))
                .pipe(
                    catchError((error: AxiosError) => {
                        this.axiosHelper.handleAxiosError(error);
                    }),
                )
            ;



        return standalone;
    }

    async getAllBieMetadataWithChildren(libraryName: string, releaseVersions?: string, businessContexts?: string, den?: string, states?: string): Promise<BiesWithChildren> {
        const allBies = await this.getAllBieMetadata(libraryName, releaseVersions, businessContexts, den, states);
        return this.addBieChildren(allBies);
    }

    private async addBieChildren(bies: BIEs): Promise<BiesWithChildren> {

        let biesWithChildren: BieWithChildren[] = [];

        for (const parentBie of bies.businessInformationEntities) {
            let bieWithChildren: BieWithChildren = JSON.parse(JSON.stringify(parentBie));
            let childBies: Bie[] = [];
            for (const childBie of bies.businessInformationEntities) {
                if (parentBie.topLevelAsbiepId === childBie.basedTopLevelAsbiepId) {
                    childBies.push(childBie);
                }
            }
            bieWithChildren.childBusinessInformationEntities = childBies;
            biesWithChildren.push(bieWithChildren);
        }

        let biesWithChildrenDto: BiesWithChildren = new BiesWithChildren();
        biesWithChildrenDto.businessInformationEntities = biesWithChildren;

        return biesWithChildrenDto;
    }


    async getAllBieMetadata(libraryName: string, releaseVersions?: string, businessContexts?: string, den?: string, states?: string): Promise<BIEs> {

        const metadataUrl = this.axiosHelper.getBackendUrl("gateway_external_api.bie_backend_endpoint")

        console.log("retrieving bie metadata");

        let axiosConfig = {
            params:
            {
                libraryName: libraryName,
                pageSize: -1,
                pageIndex: -1,
                businessContext: businessContexts,
                den: den,
                states: states,
                releaseVersions: releaseVersions
            }
            ,
            validateStatus: function (status: number) {
                return status == 200; // Resolve only if the status code is 200
            }
        };

        const bies =
            await firstValueFrom(
                this.httpService.get
                    (metadataUrl, axiosConfig)
                    .pipe(map(response => {
                        var bieList: any[] = response.data.list;
                        bieList.forEach(bie => {
                            bie.basedTopLevelAsbiepId = bie.based?.topLevelAsbiepId;
                            bie.owner = bie.owner.loginId;
                            bie.branch = bie.release.releaseNum;
                        });

                        const biesDto = plainToInstance(BIEs, { businessInformationEntities: bieList },
                            { excludeExtraneousValues: true, exposeUnsetFields: true, enableImplicitConversion: true });

                        const mergedBies = this.mergeComponentInfo(biesDto, libraryName);

                        return mergedBies;

                    }
                    ))
                    .pipe(
                        catchError((error: AxiosError) => {
                            this.axiosHelper.handleAxiosError(error);
                        }),
                    )
            )
            ;


        return bies;
    }


    async getBiePackageMetadataWithBiesWithChildren(libraryName: string, packageName: string, packageVersionId: string, packageState?: string,): Promise<BiePackageWithBiesWithChildren> {

        const packageWithBies = await this.getBiePackageMetadataWithBies(libraryName, packageName, packageVersionId, packageState);
        let packageWithBiesWithChildren: BiePackageWithBiesWithChildren = JSON.parse(JSON.stringify(packageWithBies));
        const bies = packageWithBies.businessInformationEntities;

        if (bies) {
            const biesWithChildren = await this.addBieChildren(bies);
            packageWithBiesWithChildren.BIEs = biesWithChildren.businessInformationEntities;
            return packageWithBiesWithChildren;
        }
        else {
            return packageWithBiesWithChildren;
        }

    }


    async getBiePackageMetadataWithBies(libraryName: string, packageName: string, packageVersionId: string, packageState?: string): Promise<BiePackageWithBies> {

        const packageBiesUrl = this.axiosHelper.getBackendUrl('gateway_external_api.bie_packages_bies_backend_endpoint');

        let axiosConfig = {
            params:
            {
                libraryName: libraryName,
                biePackageName: packageName,
                biePackageVersionId: packageVersionId,
                states: packageState,
                pageSize: -1,
                pageIndex: -1,
            }
            ,
            validateStatus: function (status: number) {
                return status == 200;
            }
        }

        const biePackageBies =
            await firstValueFrom(this.httpService.get
                (packageBiesUrl, axiosConfig)
                .pipe(map(response => {
                    return response.data.list;
                }
                ))
                .pipe(
                    catchError((error: AxiosError) => {
                        this.axiosHelper.handleAxiosError(error);
                    }),
                )
            );

        const biePackages = await this.getBiePackagesMetadata(libraryName, packageState);
        const biePackage = biePackages.biePackages.find(biePackage => { 
            biePackage.packageName === packageName && biePackage.versionId === packageVersionId });
        if (biePackage) {
            let biePackageWithBies: BiePackageWithBies = JSON.parse(JSON.stringify(biePackage));
            biePackageWithBies.businessInformationEntities = biePackageBies;
            const packageWithBiesDto = plainToInstance(BiePackageWithBies, biePackageWithBies
                , { excludeExtraneousValues: true, exposeUnsetFields: true, enableImplicitConversion: true });

            return packageWithBiesDto;
        }

        throw new HttpException("package not found", HttpStatus.NOT_FOUND);
    }


    async getBiePackagesMetadata(libraryName: string, packageState?: string): Promise<BiePackages> {

        const packageUrl = this.axiosHelper.getBackendUrl('gateway_external_api.bie_packages_backend_endpoint');

        console.log("retrieving bie package metadata");

        let axiosConfig = {
            params:
            {
                libraryName: libraryName,
                pageSize: -1,
                pageIndex: -1,
                states: packageState
            }
            ,
            validateStatus: function (status: number) {
                return status == 200; // Resolve only if the status code is 200
            }
        };

        const biePackages =
            await firstValueFrom(
                this.httpService.get
                    (packageUrl, axiosConfig)
                    .pipe(map(response => {
                        const packages = plainToInstance(BiePackages,
                            { biePackages: response.data.list },
                            { excludeExtraneousValues: true, exposeUnsetFields: true, enableImplicitConversion: true });
                        return packages;
                    }
                    ))
                    .pipe(
                        catchError((error: AxiosError) => {
                            this.axiosHelper.handleAxiosError(error);
                        }),
                    )
            );

        return biePackages;
    }

}