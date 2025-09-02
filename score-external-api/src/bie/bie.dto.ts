import { ApiProperty } from '@nestjs/swagger';
import { Exclude, Expose, Type } from 'class-transformer';

@Exclude()
export class Bie {

    @ApiProperty()
    @Expose()
    public type: string;

    @ApiProperty()
    @Expose()
    public manifestId: bigint;

    @ApiProperty()
    @Expose()
    public topLevelAsbiepId: bigint;

    @ApiProperty()
    @Expose({ name: 'guid' })
    public uuid: string;

    @ApiProperty()
    @Expose()
    public den: string;

    @ApiProperty()
    @Expose()
    public definition: string;

    @ApiProperty()
    @Expose()
    public name: string;

    @ApiProperty()
    @Expose()
    public oagisComponentType: string;

    @ApiProperty()
    @Expose()
    public dtType: string;

    @ApiProperty()
    @Expose()
    public owner: string;

    @ApiProperty()
    @Expose()
    public state: string;

    @ApiProperty()
    @Expose()
    public deprecated: boolean;

    @ApiProperty()
    @Expose()
    public lastUpdateUser: string;

    @ApiProperty()
    @Expose()
    public lastUpdateTimestamp: bigint;

    @ApiProperty()
    @Expose({ name: 'branch' })
    public branchCreatedWith: string;

    @ApiProperty()
    @Expose()
    public newComponent: boolean;

    @ApiProperty()
    @Expose()
    public tagList: string[];

    @ApiProperty()
    @Expose()
    public sinceReleaseNum: string;

    @ApiProperty()
    @Expose()
    public updatedReleaseNum: string;

    @ApiProperty()
    @Expose()
    public componentTag: string;

    @ApiProperty()
    @Expose()
    public componentDefinition: string;

    @ApiProperty()
    @Expose()
    public componentState: string;

    @ApiProperty()
    @Expose()
    public componentUuid: string;

    @ApiProperty()
    @Expose()
    public fromNewComponent: boolean;

    @ApiProperty()
    @Expose()
    public componentSinceReleaseNum: string;

    @ApiProperty()
    @Expose()
    public componentLastChangedReleaseNum: string;

    @ApiProperty()
    @Expose()
    public basedTopLevelAsbiepId: bigint;

}

@Exclude()
export class BIEs {
    @Expose()
    @ApiProperty({ type: [Bie] })
    @Type(() => Bie)
    public businessInformationEntities: Bie[];
}

@Exclude()
export class BieWithChildren extends Bie {

    @ApiProperty()
    @Expose()
    @ApiProperty({ type: [Bie] })
    public childBusinessInformationEntities: Bie[];
}

@Exclude()
export class BiesWithChildren {
    @Expose()
    @ApiProperty({ type: [BieWithChildren] })
    @Type(() => BieWithChildren)
    public businessInformationEntities: BieWithChildren[];
}