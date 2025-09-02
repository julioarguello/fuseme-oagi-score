import { ApiProperty } from '@nestjs/swagger';
import { Exclude, Expose, Type } from 'class-transformer';

@Exclude()
export class Library {

    @ApiProperty()
    @Expose()
    public type: string;

    @ApiProperty()
    @Expose()
    public name: string;

    @ApiProperty()
    @Expose()
    public organization: string;

    @ApiProperty()
    @Expose()
    public description: string;

    @ApiProperty()
    @Expose()
    public owner: string;

    @ApiProperty()
    @Exclude()
    public state: string;


}

@Exclude()
export class Libraries {
    @Expose()
    @ApiProperty({ type: [Library] })
    @Type(() => Library)
    public libraries: Library[];
}