import { readFile, writeFile, mkdir } from 'fs/promises';
import { join } from 'path';
import { Cache } from 'cache-manager';
import { CACHE_MANAGER } from "@nestjs/cache-manager";
import { Inject } from '@nestjs/common';


export class CacheHelper {


    constructor(@Inject(CACHE_MANAGER) private cacheManager: Cache) { }


    async getFromCache(libraryName: string, uuid: string, schemaType: string, releaseVersion: string): Promise<string | null> {

        const cacheFile = join(this.getCachePath(libraryName, releaseVersion, uuid, schemaType), uuid + '.' + schemaType)

        try {
            const schema = await readFile(cacheFile, 'utf8');
            console.log("cache hit " + cacheFile);
            return schema.toString();
        }
        catch (error) {
            console.log("cache miss " + cacheFile);
            return null;
        }

    }


    async writeToCache(libraryName: string, uuid: string, schema: string, schemaType: string, releaseVersion: string) {

        const cachePath = this.getCachePath(libraryName, releaseVersion, uuid, schemaType);
        const cacheFile = join(cachePath, uuid + '.' + schemaType);
        console.log("writing cache " + cacheFile);
        try {
            await mkdir(cachePath, { recursive: true });
        } catch (error: unknown) {
            if (error instanceof Error) {
                if ('code' in error && typeof error.code === 'string') {
                    if (error.code != 'EEXIST') {
                        console.error("Cache path could not be created:  " + error)
                        throw error;
                    }
                }
            }
        }

        try {
            await writeFile(cacheFile, schema, 'utf8');
            console.log("Schema written to cache " + cacheFile);

        } catch (error) {
            console.error("Schema cache failed:  " + error)
            throw error;
        }
    }

    getCachePath(libraryName: string, uuid: string, schemaType: string, releaseVersion: string): string {
        return join('cache', libraryName, schemaType, releaseVersion, uuid + '.' + schemaType);
    }


    async getFromInMemoryCache(localRoute: string): Promise<string | null> {
        console.log("retrieving from in memory cache " + localRoute);
        const cachePrefix = await this.getCachePrefix();
        const response = await this.cacheManager.get<string>(cachePrefix + ":/api" + localRoute);
        return response ?? null;
    }

    async addToInMemoryCache(localRoute: string, data: string) {
        console.log("adding to in memory cache " + localRoute);
        const cachePrefix = await this.getCachePrefix();
        await this.cacheManager.set<string>(cachePrefix + ":/api" + localRoute, data);
    }

    private async getCachePrefix(): Promise<string> {

        const defaultPrefix = 'keyv';
        const store = this.cacheManager.stores[0].store as any;

        if (store && store.options && store.options.prefix) {
            console.log(store.options.prefix);
            return store.options.prefix;
        }
        else
            return defaultPrefix;
    }

}