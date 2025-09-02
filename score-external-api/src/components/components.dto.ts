import { ApiProperty } from '@nestjs/swagger';
import { Exclude, Expose, Type } from 'class-transformer';
import { ChildComponent } from './child_components.dto';
import { ValidateNested } from 'class-validator';

@Exclude()
export class Component {

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
    public lastUpdateUser: string;

    @ApiProperty()
    @Expose()
    public lastUpdateTimestamp: bigint;

    @ApiProperty()
    @Expose()
    public releaseNum: string;

    @ApiProperty()
    @Expose()
    public newComponent: boolean;

    @ApiProperty()
    @Expose()
    public nounBodVerbTag: string;

    @ApiProperty()
    @Expose()
    public sinceReleaseNum: string;

    @ApiProperty()
    @Expose()
    public updatedReleaseNum: string;

}

@Exclude()
export class Components {
    @Expose()
    @ApiProperty({ type: [Component] })
    @Type(() => Component)
    public components: Component[];
}

@Exclude()
export class ComponentWithChildren extends Component {

  
    @ApiProperty({ type: [ChildComponent] })
    @Expose()
    @ValidateNested({ each: true })
    @Type(() => ChildComponent) // Important: Tells class-transformer how to transform nested object
    public children: ChildComponent[];

}

@Exclude()
export class ComponentsWithChildren {
    @Expose()
    @ApiProperty({ type: [ComponentWithChildren] })
    @Type(() => ComponentWithChildren)
    public components: ComponentWithChildren[];
}