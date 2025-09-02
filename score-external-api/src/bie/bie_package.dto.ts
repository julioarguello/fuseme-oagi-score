import { ApiProperty } from '@nestjs/swagger';
import { Exclude, Expose, Type } from 'class-transformer';
import { Bie, BIEs, BieWithChildren } from './bie.dto';

@Exclude()
export class BiePackage {

    @ApiProperty()
    @Expose({ name: 'name' })
    public packageName: string;

    @ApiProperty()
    @Expose()
    public versionId: string;

    @ApiProperty()
    @Expose()
    public versionName: string;

    @ApiProperty()
    @Expose()
    public description: string;

    @ApiProperty()
    @Expose()
    public biePackageId: bigint;

}

@Exclude()
export class BiePackages {
    @Expose()
    @ApiProperty({ type: [BiePackage] })
    @Type(() => BiePackage)
    public biePackages: BiePackage[];
}

@Exclude()
export class BiePackageWithBies extends BiePackage {

    @ApiProperty({ type: [Bie] })
    @Expose()
    public businessInformationEntities: BIEs;
}

@Exclude()
export class BiePackageWithBiesWithChildren extends BiePackage {

    @ApiProperty({ type: [BieWithChildren] })
    @Expose()
    public BIEs: BieWithChildren[];
}