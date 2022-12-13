package com.middleware.zeus.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.SessionAttribute;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.RequestParameterBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.ParameterType;
import springfox.documentation.service.RequestParameter;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/22
 */
@Configuration
public class SwaggerConfig {

    private static final String DC_ID_KEY = "skyview-dc-id";
    private static final String ROLE_ID_LEY = "skyview-role-id";
    private static final String LANGUAGE_KEY = "skyview-language";
    private static final String PROJECT_ID_KEY = "skyview-project-id";
    private static final String TENANT_ID_KEY = "skyview-tenant-id";

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .ignoredParameterTypes(SessionAttribute.class)
                .select()
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts())
                .globalRequestParameters(requestParameters());
    }

    private List<RequestParameter> requestParameters() {
        List<RequestParameter> parameters = new ArrayList<>();
        // fixme 目前不需要这些，之后要是使用了再加回来
//        parameters.add(new RequestParameterBuilder()
//                .name(DC_ID_KEY)
//                .description("数据中心编号")
//                .required(false)
//                .in(ParameterType.HEADER)
//                .build());
//        parameters.add(new RequestParameterBuilder()
//                .name(LANGUAGE_KEY)
//                .description("语言")
//                .required(false)
//                .in(ParameterType.HEADER)
//                .build());
//        parameters.add(new RequestParameterBuilder()
//                .name(TENANT_ID_KEY)
//                .description("租户编号")
//                .required(false)
//                .in(ParameterType.HEADER)
//                .build());
//        parameters.add(new RequestParameterBuilder()
//                .name(PROJECT_ID_KEY)
//                .description("项目编号")
//                .required(false)
//                .in(ParameterType.HEADER)
//                .build());
//        parameters.add(new RequestParameterBuilder()
//                .name(ROLE_ID_LEY)
//                .description("角色编号")
//                .required(false)
//                .in(ParameterType.HEADER)
//                .build());
        return parameters;
    }

    private List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> schemes = new ArrayList<>();
        schemes.add(new ApiKey("Authorization", "Authorization", "header"));
        schemes.add(new ApiKey("userToken", "userToken", "header"));
        schemes.add(new ApiKey("authType", "authType", "header"));
        return schemes;
    }

    private List<SecurityContext> securityContexts() {
        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(SecurityContext.builder().securityReferences(securityReferences()).build());
        return securityContexts;
    }

    private List<SecurityReference> securityReferences() {
        List<SecurityReference> securityReferences = new ArrayList<>();
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[]{new AuthorizationScope("global", "accessEverything")};
        securityReferences.add(new SecurityReference("Authorization", authorizationScopes));
        securityReferences.add(new SecurityReference("userToken", authorizationScopes));
        securityReferences.add(new SecurityReference("authType", authorizationScopes));
        return securityReferences;
    }

    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("谐云 中间件平台 OpenAPI")
                .description("谐云 中间件平台 OpenAPI")
                .version("1.0.0")
                .build();
    }


}
