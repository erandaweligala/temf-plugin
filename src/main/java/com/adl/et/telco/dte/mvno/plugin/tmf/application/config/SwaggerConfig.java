/*
  Copyrights 2020 Axiata Digital Labs Pvt Ltd.
  All Rights Reserved.
  <p>
  These material are unpublished, proprietary, confidential source
  code of Axiata Digital Labs Pvt Ltd (ADL) and constitute a TRADE
  SECRET of ADL.
  <p>
  ADL retains all title to and intellectual property rights in these
  materials.
 */
package com.adl.et.telco.dte.mvno.plugin.tmf.application.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;



@Configuration
public class SwaggerConfig {

    @Value("${app.host}")
    private String host;


    @Bean
    public OpenAPI springShopOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.info(apiV3Info());
        Server server = new Server();
        server.setUrl(host);
        openAPI.setServers(Collections.singletonList(server));
        return openAPI;
    }

    private Info apiV3Info() {
        return new Info().title("Swagger Title")
                .description("Swagger Description")
                .version("Swagger Version")
                .contact(new Contact()
                        .name("Admin")
                        .url("http://www.axiatadigitallabs.com")
                        .email("adl@axiatadigitallabs.com"));
    }

//    @Bean
//    public Docket api(TmfPluginConfiguration configuration) {
//
//        String host = configuration.getPort() == null ? configuration.getHost() : configuration.getHost() + ":" + configuration.getPort();
//
//        return new Docket(DocumentationType.SWAGGER_2)
//                .protocols(Stream.of(configuration.getProtocol()).collect(Collectors.toSet()))
//                .host(host)
//                .select().apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
//                .paths(regex("/.*"))
//                .build()
//                .apiInfo(apiInfo(configuration.getSwagger())).pathProvider(new BasePathAwareRelativePathProvider(configuration.getContext().getRelative()));
//    }

//    private ApiInfo apiInfo(TmfPluginConfiguration.SwaggerConfiguration configuration) {
//        return new ApiInfo(
//                configuration.getTitle(),
//                configuration.getDescription(),
//                configuration.getVersion(),
//                "",
//                new Contact("Admin", "http://www.axiatadigitallabs.com", "adl@axiatadigitallabs.com"),
//                "License of API", "API license URL", Collections.emptyList());
//    }

//    class BasePathAwareRelativePathProvider extends AbstractPathProvider {
//        private String swaggerCall;
//
//        public BasePathAwareRelativePathProvider(String swaggerCall) {
//            this.swaggerCall = swaggerCall;
//        }
//
//        @Override
//        protected String applicationPath() {
//            return swaggerCall;
//        }
//
//        @Override
//        protected String getDocumentationPath() {
//            return swaggerCall;
//        }
//
//        @Override
//        public String getOperationPath(String operationPath) {
//            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath("/");
//            return Paths.removeAdjacentForwardSlashes(
//                    uriComponentsBuilder.path(operationPath.replaceFirst(swaggerCall, "")).build().toString());
//        }
//    }
}

