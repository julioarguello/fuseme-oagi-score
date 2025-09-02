import { Controller, ParseUUIDPipe, UseGuards } from '@nestjs/common';
import { Get } from '@nestjs/common';
import { BieService } from './bie.service';
import { Param, Query } from '@nestjs/common';
import { CacheTTL } from '@nestjs/cache-manager';
import { AuthGuard } from 'src/auth/auth.guard';
import { ApiOperation, ApiResponse, ApiTags, ApiQuery, ApiBearerAuth, ApiUnauthorizedResponse, ApiOkResponse, ApiForbiddenResponse, ApiProduces } from '@nestjs/swagger';
import { BiePackages, BiePackageWithBies, BiePackageWithBiesWithChildren } from './bie_package.dto';
import { BIEs, BiesWithChildren } from './bie.dto';
import { ComponentsService } from 'src/components/components.service';
import { Throttle } from '@nestjs/throttler';


@ApiTags('Business Information Entities (BIE)')
@Controller('api/business-information-entities')
@UseGuards(AuthGuard)
export class BieController {

  constructor(
    private readonly bieService: BieService,
    private readonly componentsService: ComponentsService
  ) { }


  @Get()
  @ApiQuery({ name: 'library', required: false, description: 'Library to return BIEs for - if not specified it will be the default set by the deployment environment variable or to connectSpec (OAGIS) if no environment variable set.', example: 'connectSpec' })
  @ApiQuery({ name: 'den', required: false, description: 'Filter for BIEs with a DEN (Dictionary Entry Name) that contains the search string (case insensitive)', example: 'BOM' })
  @ApiQuery({ name: 'businessContext', required: false, description: 'Filter by a business context', example: 'HR' })
  @ApiQuery({ name: 'branch', required: false, description: 'Comma-separated list of branches of library.  If no library specified, the default is connectSpec (OAGIS).', example: '10.12.1' })
  @ApiProduces()
  @ApiOkResponse({ description: 'OK', type: BIEs })
  @ApiUnauthorizedResponse({ description: 'Unauthorized' })
  @ApiOperation({
    summary: 'Return metadata for every Business Information Entity (BIE) with optional filtering parameters.'
      + ' Only Production BIEs are returned.'
  })
  @ApiBearerAuth()
  @CacheTTL(60000)
  findAllBies(
    @Query('library') library: string,
    @Query('den') den: string,
    @Query('businessContext') businessContext: string,
    @Query('branch') branch: string,
  ) {

    return this.bieService.getAllBieMetadata(library ?? this.componentsService.getDefaultLibrary(), branch, businessContext, den, 'Production');
  }



  @Get('children')
  @ApiQuery({ name: 'library', required: false, description: 'Library to return BIEs for - if not specified it will be the default set by the deployment environment variable or to connectSpec (OAGIS) if no environment variable set.', example: 'connectSpec' })
  @ApiQuery({ name: 'den', required: false, description: 'Filter for BIEs with a DEN (Dictionary Entry Name) that contains the search string (case insensitive)', example: 'BOM' })
  @ApiQuery({ name: 'businessContext', required: false, description: 'Filter by a business context', example: 'HR' })
  @ApiQuery({ name: 'branch', required: false, description: 'Comma-separated list of branches of library.  If no library specified, the default is connectSpec (OAGIS).', example: '10.12.1' })
  @ApiProduces()
  @ApiOkResponse({ description: 'OK', type: BiesWithChildren })
  @ApiUnauthorizedResponse({ description: 'Unauthorized' })
  @ApiOperation({
    summary: 'Return metadata for every Business Information Entity (BIE) with optional filtering parameters.'
      + ' Only Production BIEs are returned.'
  })
  @ApiBearerAuth()
  @CacheTTL(60000)
  findAllWithChildren(
    @Query('library') library: string,
    @Query('den') den: string,
    @Query('businessContext') businessContext: string,
    @Query('branch') branch: string,
  ) {

    return this.bieService.getAllBieMetadataWithChildren(library ?? this.componentsService.getDefaultLibrary(), branch, businessContext, den, 'Production');
  }


  @Get('uuid/:uuid')
  @Throttle({ default: { limit: 10, ttl: 20000 } })
  @ApiOperation({
    summary: 'Return BIE Schema for the given UUID -'
      + ' currently XSD is the only schema type supported. Only Production BIEs are returned.'
  })
  @ApiProduces('application/xml')
  @ApiResponse({ status: 200, description: 'OK' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  @ApiBearerAuth()
  @ApiUnauthorizedResponse({ description: 'Unauthorized' })
  @CacheTTL(60000)
  findBusinessInformationEntitySchema(
    @Param('uuid') uuid: string
  ) {
    return this.bieService.getBieSchema(uuid, 'Production');
  }



  @Get('packages')
  @ApiQuery({ name: 'library', required: false, description: 'Library to return BIE packages for - if not specified it will be the default set by the deployment environment variable or to connectSpec (OAGIS) if no environment variable set.', example: 'connectSpec' })
  @ApiQuery({ name: 'versionId', required: false, description: 'Filter by version ID', example: 'v1.0.1' })
  @ApiQuery({ name: 'packageName', required: false, description: 'Filter by version name', example: 'Common Data Model BIE Package Release' })
  @ApiProduces()
  @ApiOkResponse({ description: 'OK', type: BiePackages })
  @ApiUnauthorizedResponse({ description: 'Unauthorized' })
  @ApiOperation({ summary: 'BIE package metadata.  Only BIE Packages in Production state are returned.' })
  @ApiBearerAuth()
  @CacheTTL(60000)
  findPackages(
    @Query('library') library: string,
    /*
    @Query('versionId') versionId: string,
    @Query('packageName') packageName: string,
    */
  ) {
    return this.bieService.getBiePackagesMetadata(library ?? this.componentsService.getDefaultLibrary(), 'Production');
  }

}