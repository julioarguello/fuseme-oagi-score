import { Injectable, HttpException, HttpStatus } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { map } from 'rxjs/operators';
import { catchError, firstValueFrom } from 'rxjs';
import { HttpService } from '@nestjs/axios';
import { AxiosError } from 'axios';
import { Components, ComponentsWithChildren } from './components.dto';
import { Releases, Release } from './releases.dto';
import { plainToInstance } from 'class-transformer';
import { AxiosHelper } from '../common/gateway_api_helper';
import { CacheHelper } from '../common/cache_helper';
import { Libraries } from './libraries.dto';




@Injectable()
export class ComponentsService {

    constructor(
        private readonly httpService: HttpService, private readonly configService: ConfigService,
        private readonly axiosHelper: AxiosHelper, private readonly cacheHelper: CacheHelper
        ,) { }


    async getLibraries(): Promise<Libraries> {

        const librariesUrl = this.axiosHelper.getBackendUrl("gateway_external_api.libraries_backend_endpoint");
        const libraries = await firstValueFrom(this.httpService.get(librariesUrl)
            .pipe(map(response => {
                var libraryList = response.data.list;
                const librariesDto = plainToInstance(Libraries, { "libraries": libraryList },
                    { excludeExtraneousValues: true, exposeUnsetFields: true, enableImplicitConversion: true });
                if (librariesDto)
                    return librariesDto;
            }))
            .pipe(
                catchError((error: AxiosError) => {
                    this.axiosHelper.handleAxiosError(error);
                }),
            )
        );
        if (!libraries)
            throw new HttpException("No libraries found", HttpStatus.INTERNAL_SERVER_ERROR);
        return libraries;
    }

    async getReleases(libraryName: string): Promise<Releases> {

        const userId = this.configService.get("user_id");

        const releasesUrl = this.axiosHelper.getBackendUrl("gateway_external_api.releases_backend_endpoint");
        const releases = await firstValueFrom(this.httpService.get(releasesUrl,
            {
                params:
                {
                    userId: userId,
                    libraryName: libraryName,
                    excludeReleaseNums: "Working",
                    releaseStates: "Published",
                    orderBy: "releaseNum",
                    pageIndex: -1,
                    pageSize: -1
                }
            })
            .pipe(map(response => {
                response.data.library = libraryName;
                var releasesList: any[] = response.data.list;
                releasesList.forEach(release => {
                    release.lastUpdateTimestamp = release.lastUpdated.when;
                });

                const releasesDto = plainToInstance(Releases, { "releases": releasesList },
                    { excludeExtraneousValues: true, exposeUnsetFields: true, enableImplicitConversion: true });
                if (releasesDto)
                    return releasesDto;
            }))
            .pipe(
                catchError((error: AxiosError) => {
                    this.axiosHelper.handleAxiosError(error);
                }),
            )
        );
        if (!releases)
            throw new HttpException("No releases found", HttpStatus.INTERNAL_SERVER_ERROR);
        return releases;
    }


    async getLatestRelease(libraryName: string): Promise<Release> {

        const releases = await this.getReleases(libraryName);

        const releaseList = releases.releases;

        if (!releaseList || releaseList.length == 0) {
            throw new HttpException('No releases found', HttpStatus.NO_CONTENT);
        }

        const latestRelease = releaseList.reduce((max, current) => {
            return (current.lastUpdateTimestamp > max.lastUpdateTimestamp) ? current : max;
        }, releaseList[0]);

        if (!latestRelease) {
            throw new HttpException("Could not get latest release", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else {
            return Promise.resolve(latestRelease);
        }
    }


    async getStandaloneComponent(libraryName: string, uuid: string, schemaType = 'xsd', releaseVersion?: string) {

        const schemaUrlXsd = this.axiosHelper.getBackendUrl("gateway_external_api.components_schema_backend_endpoint");

        if (schemaType != 'xsd') { //&& schemaType!='json') { //TODO:  JSON
            throw new HttpException("Unsupported schema type", HttpStatus.BAD_REQUEST);
        }

        const releaseNum = releaseVersion ?? (await this.getLatestRelease(libraryName)).releaseNum;
        console.log(releaseNum);

        var cachedSchema = await this.cacheHelper.getFromCache(libraryName, uuid, schemaType, releaseNum);
        if (cachedSchema) {
            console.log("returning cached schema" + cachedSchema);
            return cachedSchema;
        }
        else {

            //const manifestId = await this.getManifestId(uuid, releaseNum, 'ASCCP');
            const standaloneSchema =
                this.httpService.get
                    (schemaUrlXsd,
                        {
                            headers:
                            {
                                Accept: "application/xml",
                                "Content-Type": "application/xml"
                            },
                            params:
                            {
                                libraryName: libraryName,
                                releaseVersion: releaseNum,
                                guid: uuid
                                //asccpManifestIdList: manifestId
                            }
                        }
                    )
                    .pipe(map(response => {
                        let standalone = response.data;
                        this.cacheHelper.writeToCache(libraryName, uuid, standalone.toString(), schemaType, releaseNum);
                        return standalone.toString();
                    }))
                    .pipe(
                        catchError((error: AxiosError) => {
                            this.axiosHelper.handleAxiosError(error);
                        }),
                    )
                ;
            if (!standaloneSchema)
                throw new HttpException('UUID not found or is not ASCCP', HttpStatus.NOT_FOUND);
            return standaloneSchema;

        }
    }

    async getAsccpComponents(libraryName: string, releaseVersion: string): Promise<Components> {

        const asccpRoute = '/components?library=' + libraryName + '&releaseVersion=' + releaseVersion + '&types=asccp';
        const componentsCache = await this.cacheHelper.getFromInMemoryCache(asccpRoute);
        if (componentsCache) {
            try {
                return JSON.parse(componentsCache) as Components;
            }
            catch (error) {
                console.error("could not parse in memory components " + error);
                throw error;
            }
        }
        else {
            const components = await this.getAllComponentsMetadata(libraryName, false, undefined, releaseVersion, undefined, undefined);
            if (components) {
                await this.cacheHelper.addToInMemoryCache(asccpRoute, JSON.stringify(components));
                return components;
            }

            return new Components();
        }
    }

    async getAllComponentsMetadata(libraryName: string, withChildren: boolean, tags?: string, releaseVersion?: string, componentTypes?: string, componentDen?: string): Promise<Components> {

        const userId = this.configService.get("user_id");

        const metadataUrl = this.axiosHelper.getBackendUrl('gateway_external_api.components_backend_endpoint');
        const childrenUrl = this.axiosHelper.getBackendUrl('gateway_external_api.components_children_backend_endpoint');

        console.log("retrieving component metadata from " + metadataUrl);
        let axiosConfig = {
            params:
            {
                userId: userId,
                libraryName: libraryName,
                releaseVersion: releaseVersion ?? (await this.getLatestRelease(libraryName)).releaseNum,
                types: componentTypes,
                asccpTypes: "Default",
                pageSize: -1,
                pageIndex: -1,
                orderBy: "den",
                tags: tags,
                den: componentDen
            },
            validateStatus: function (status: number) {
                return status == 200; // Resolve only if the status code is 200
            }
        };

        var components =
            await firstValueFrom(
                this.httpService.get(metadataUrl, axiosConfig)
                    .pipe(map(async response => {
                        var componentsList: any[] = response.data.list;
                        componentsList.forEach(component => {
                            component.owner = component.owner.loginId;
                            component.definition = component.definition.content;
                            const tagList: any[] = component.tagList;
                            tagList.forEach(tag => {
                                if (tag.name === 'Noun' || tag.name == 'BOD' || tag.name == 'Verb') {
                                    component.nounBodVerbTag = component.tagList[0].name;
                                }
                            })

                        });

                        if (withChildren) {
                            const componentParentChildren =
                                await firstValueFrom(
                                    this.httpService.get(childrenUrl, axiosConfig)
                                        .pipe(map(response => {
                                            const parentChildComponents = response.data.list;
                                            return parentChildComponents;
                                        }))

                                        .pipe(
                                            catchError((error: AxiosError) => {
                                                this.axiosHelper.handleAxiosError(error);
                                            }),
                                        )
                                )
                                ;

                            const groupedChildren: Record<string, string[]> = {};
                            for (const rel of componentParentChildren) {
                                if (!groupedChildren[rel.parentGuid]) {
                                    groupedChildren[rel.parentGuid] = [];
                                }
                                groupedChildren[rel.parentGuid].push(rel);
                            }

                            const updatedComponentsList = componentsList.map((parent) => ({
                                ...parent,
                                children: componentParentChildren.filter((rel: any) => rel.parentGuid === parent.guid),
                            }));
                            componentsList = updatedComponentsList;

                        }

                        const componentsDto = plainToInstance(withChildren ? ComponentsWithChildren : Components, { "components": componentsList },
                            { excludeExtraneousValues: true, exposeUnsetFields: true, enableImplicitConversion: true });
                        return componentsDto;
                    }))

                    .pipe(
                        catchError((error: AxiosError) => {
                            this.axiosHelper.handleAxiosError(error);
                        }),
                    )
            )
            ;

        if (!components)
            return new Components();
        return components;

    }

    getDefaultLibrary() {
        return this.configService.get<string>('default_library') ?? 'connectSpec';
    }

}