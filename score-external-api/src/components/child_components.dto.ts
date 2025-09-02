import { ApiProperty } from '@nestjs/swagger';
import { Exclude, Expose, Type } from 'class-transformer';

@Exclude()
export class ChildComponent {

    @ApiProperty()
    @Expose({ name: 'parentGuid' })
    public parentUuid: string;

    @ApiProperty()
    @Expose({ name: 'childGuid' })
    public childUuid: string;

    @ApiProperty()
    @Expose()
    public childDen: string;

    @ApiProperty()
    @Expose()
    public childPropertyTerm: string;

    @ApiProperty()
    @Expose()
    public type: string;

    @ApiProperty()
    @Expose()
    public childDataType: string;

    @ApiProperty()
    @Expose()
    public cardinalityMin: string;

    @ApiProperty()
    @Expose()
    public cardinalityMax: string;
}

/*
@Exclude()
export class ChildComponents {
    @Expose()
    @ApiProperty()
    @Type(() => ChildComponent)
    public children: ChildComponent[];
}
*/