import { ApiProperty } from '@nestjs/swagger';
import { Exclude, Expose, Type } from 'class-transformer';

@Exclude()
export class Release {

    @ApiProperty()
    @Expose()
    public type: string;

    @ApiProperty()
    @Exclude()
    public manifestId: bigint;

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
    public lastUpdateTimestamp: bigint;

    @ApiProperty()
    @Expose()
    public releaseNum: string;

    @ApiProperty()
    @Exclude()
    public releaseId: bigint;

    @ApiProperty()
    @Expose()
    public newComponent: boolean;

    @ApiProperty()
    @Expose()
    public tagList: string[];


}

@Exclude()
export class Releases {
    @Expose()
    @ApiProperty({ type: [Release] })
    @Type(() => Release)
    public releases: Release[];

}