import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { ComponentsController } from './components/components.controller';
import { ComponentsService } from './components/components.service';
import { CacheModule, CacheInterceptor } from '@nestjs/cache-manager';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { APP_INTERCEPTOR, APP_GUARD } from '@nestjs/core';
import { HttpModule } from '@nestjs/axios';
import { ThrottlerModule } from '@nestjs/throttler';
import { AuthGuard } from './auth/auth.guard';
import { BieController } from './bie/bie.controller';
import { BieService } from './bie/bie.service';
import config from './config/config';
import { AxiosHelper } from './common/gateway_api_helper';
import { CacheHelper } from './common/cache_helper';
import { BieControllerDeprecated } from './bie/bie.controller_deprecated';


@Module({
  imports: [
    HttpModule
    , CacheModule.register(
      {
        isGlobal: false,
        ttl: 0,
      }
    )
    , ConfigModule.forRoot(
      {
        load: [config],
        isGlobal: true,
        cache: true,
        ignoreEnvFile: true
      }),
      ThrottlerModule.forRootAsync({
        imports: [ConfigModule],
        inject: [ConfigService],
        useFactory: (config: ConfigService) => [
          {
            ttl: Number(config.get('THROTTLE_TTL')),
            limit: Number(config.get('THROTTLE_LIMIT')),
          },
        ],
      }),
  ],
  controllers: [
    AppController,
    ComponentsController,
    BieController,
    BieControllerDeprecated
  ],
  providers: [
    AppService,
    ComponentsService,
    BieService,
    AxiosHelper,
    CacheHelper,
    {
      provide: APP_GUARD,
      useClass: AuthGuard,
    },
    {
      provide: APP_INTERCEPTOR,
      useClass: CacheInterceptor,
    },

  ],
})
export class AppModule { }
