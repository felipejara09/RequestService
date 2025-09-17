package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.RegisterLoanApplicationRequest;
import co.com.bancolombia.api.dto.RegisterLoanApplicationResponse;
import co.com.bancolombia.api.dto.docs.ErrorResponse;
import co.com.bancolombia.api.dto.docs.PageResponseLoanApplicationSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/solicitud",
                    produces = MediaType.APPLICATION_JSON_VALUE,
                    method = RequestMethod.POST,
                    beanClass = Handler.class,
                    beanMethod = "register",
                    operation = @Operation(
                            operationId = "registerLoanApplication",
                            summary = "Crear solicitud de préstamo",
                            description = "Crea una solicitud de préstamo verificando identidad y reglas de negocio.",
                            security = { @SecurityRequirement(name = "bearerAuth") },
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = RegisterLoanApplicationRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Creado",
                                            content = @Content(schema = @Schema(implementation = RegisterLoanApplicationResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Bad Request",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(responseCode = "401", description = "Unauthorized",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(responseCode = "403", description = "Forbidden",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitud",
                    produces = MediaType.APPLICATION_JSON_VALUE,
                    method = RequestMethod.GET,
                    beanClass = Handler.class,
                    beanMethod = "list",
                    operation = @Operation(
                            operationId = "listManualReview",
                            summary = "Listar solicitudes para revisión manual",
                            description = "Retorna solicitudes en estados de revisión manual, paginadas y con filtro f.",
                            security = { @SecurityRequirement(name = "bearerAuth") },
                            parameters = {
                                    @Parameter(name = "page", in = ParameterIn.QUERY,
                                            description = "Página (0..N)", schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
                                    @Parameter(name = "size", in = ParameterIn.QUERY,
                                            description = "Tamaño de página (1..100)", schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100")),
                                    @Parameter(name = "f", in = ParameterIn.QUERY,
                                            description = "Filtro de texto (email o identificación)", schema = @Schema(type = "string"))
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "OK",
                                            content = @Content(schema = @Schema(implementation = PageResponseLoanApplicationSummary.class))),
                                    @ApiResponse(responseCode = "401", description = "Unauthorized",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(responseCode = "403", description = "Forbidden",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routes(Handler handler) {
        return RouterFunctions.route()
                .POST("/api/v1/solicitud", handler::register)
                .GET("/api/v1/solicitud", handler::list)
                .build();
    }
}
