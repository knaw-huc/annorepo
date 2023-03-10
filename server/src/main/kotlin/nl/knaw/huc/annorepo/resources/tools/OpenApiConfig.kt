package nl.knaw.huc.annorepo.resources.tools

import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import nl.knaw.huc.annorepo.api.ARConst

@SecurityScheme(
    name = ARConst.SECURITY_SCHEME_NAME,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    `in` = SecuritySchemeIn.HEADER,
)
@OpenAPIDefinition(
    externalDocs = ExternalDocumentation(
        description = "API Usage",
        url = "https://knaw-huc.github.io/annorepo/docs/api-usage.html"
    )
)
class OpenApiConfig