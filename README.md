# TMF Plugin
### TMF plugin for TMF services

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