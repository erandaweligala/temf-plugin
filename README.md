# TMF Plugin
### TMF plugin for TMF services

#### Requirements

- Java 11 or above
- Spring Boot 2.7.x (the plugin uses the `javax.*` servlet and validation namespaces)

#### Enabling plugin

1. Include dependency in `pom.xml`
```xml
<dependency>
    <groupId>com.adl.et.telco.dte.mvno</groupId>
    <artifactId>tmf-plugin</artifactId>
    <version>{{tmf-plugin-version}}</version>
</dependency>
```
2. Enable using `@TmfPlugin` annotation
```java
@SpringBootApplication
@TmfPlugin
public class Application {

}
```
3. Create `TmfUrlConfig` bean to configure the URL context for resources
```java
@Bean
public TmfUrlConfig urlConfig() {

    return new TmfUrlConfig()
            .addConfig(ProductOrder.class, Constants.UrlConstants.PRODUCT_ORDER_RESOURCE)
            .addConfig(CancelProductOrder.class, Constants.UrlConstants.CANCEL_PRODUCT_ORDER_RESOURCE);
}
```
4. Add `application.yml` configurations
```yaml
app:
  host: "localhost"
  protocol: "http"
  port: "8088"
  context:
    absolute: /tmf-api/productOrderingManagement/v4
    relative: /tmf-api/productOrderingManagement/v4
  swagger:
    title: Product Ordering Management
    description: "TMF API Reference : TMF 622 - Product Ordering Management"
    version: 4.0.0
```

#### API documentation

The plugin ships `springdoc-openapi-ui`, so a service that enables `@TmfPlugin` exposes:

| Endpoint | Purpose |
| --- | --- |
| `/swagger-ui.html` | Swagger UI. Open this one — it redirects to `/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config` |
| `/swagger-ui/index.html` | UI resource. Opening it without the `configUrl` query parameter shows *Failed to load remote configuration* |
| `/v3/api-docs` | OpenAPI 3 document as JSON |
| `/v3/api-docs.yaml` | OpenAPI 3 document as YAML |
| `/v3/api-docs/swagger-config` | UI bootstrap configuration fetched by swagger-ui |

These paths are relative to the service root, not to `app.context.absolute`. With
`server.port: 8088` the UI is at `http://localhost:8088/swagger-ui.html`.

The `Info` block is taken from `app.swagger.*`. The documented server URL is built as
`app.protocol://app.host:app.port`, with `app.context.relative` appended when it differs from
`app.context.absolute` — that is the case when a gateway routes to the service on its own prefix.
Paths in the document already contain `app.context.absolute`, because that is what the
controllers are mapped on.

Do not add `springfox` to a service that uses the plugin. Springfox serves a different, older UI
and both libraries register resource handlers for the documentation paths.