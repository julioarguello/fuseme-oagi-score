import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { ConfigService } from '@nestjs/config';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger'


async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  const config = new DocumentBuilder()
    .setTitle('connectCenter (Score) RESTful API')
    .setDescription('RESTful API for accessing components of connectCenter libraries, including connectSpec(OAGIS), as well as profiled Business Information Entity (BIE) descriptions and schemas from connectCenter (also known as Score).'
      + ' The API is meant to serve as a read-only integration mechanism with other systems and not meant to manipulate data within connectCenter.'
      + ' The API uses an OAuth2 Resource Server for authentication and each endpoint requires a valid JWT (JSON Web Token) to be included as bearer token in the header.')
    .setVersion(process.env.npm_package_version??'unknown')
    .addBearerAuth()
    .build()
  const document = SwaggerModule.createDocument(app, config)
  SwaggerModule.setup('api', app, document)

  const configService: ConfigService = app.get(ConfigService);
  await app.listen(configService.get<string>('api_port')??3000);
}
bootstrap();
