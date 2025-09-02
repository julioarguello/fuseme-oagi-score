import { Controller, ParseUUIDPipe, UseGuards } from '@nestjs/common';
import { Get } from '@nestjs/common';
import { BieService } from './bie.service';
import { Param, Query } from '@nestjs/common';
import { CacheTTL } from '@nestjs/cache-manager';
import { AuthGuard } from 'src/auth/auth.guard';
import { ApiOperation, ApiResponse, ApiTags, ApiQuery, ApiBearerAuth, ApiUnauthorizedResponse, ApiOkResponse, ApiForbiddenResponse, ApiProduces } from '@nestjs/swagger';
import { BIEs } from './bie.dto';
import { ComponentsService } from 'src/components/components.service';


@ApiTags('BIE Deprecated')
@Controller('api/bie')
@UseGuards(AuthGuard)
export class BieControllerDeprecated {

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
    summary: 'DEPRECATED - use business-information-entities endpoint. Return metadata for every Business Information Entity (BIE) with optional filtering parameters.'
      + ' Only Production BIEs are returned.', deprecated: true
  })
  @ApiBearerAuth()
  @CacheTTL(60000)
  findAll(
    @Query('library') library: string,
    @Query('den') den: string,
    @Query('businessContext') businessContext: string,
    @Query('branch') branch: string,
  ) {

    return this.bieService.getAllBieMetadata(library ?? this.componentsService.getDefaultLibrary(), branch, businessContext, den, 'Production');
  }



  @Get('uuid/:uuid')
  @ApiOperation({
    summary: 'DEPRECATED - use business-information-entities/uuid endpoint. Return BIE Schema for the given UUID -'
      + ' currently XSD is the only schema type supported. Only Production BIEs are returned.', deprecated: true
  })
  @ApiProduces('application/xml')
  @ApiResponse({ status: 200, description: 'OK' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  @ApiBearerAuth()
  @ApiUnauthorizedResponse({ description: 'Unauthorized' })
  findBie(
    @Param('uuid') uuid: string
  ) {
    return this.bieService.getBieSchema(uuid, 'Production');
  }

}