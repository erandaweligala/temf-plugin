# Change Log

## [2.0] - 2023-07-09
### Added
- filter option

## [2.1] - 2023-07-13
### Added
- modify filter option

## [2.2] - 2023-09-11
### Added
- change BaseResourceService.Class to generate collection name according to the catalogId which comes in servlet request

## [3.0] - 2023-10.13
### Added
- only added filter part

## [3.1] - 2023-10.25
### Added
- added pagination

## [3.2] - 2023-10.25
### Added
- added pagination new
## [3.3] - 2023-11.02
### Added
- added regex search new
## [3.4] - 2023-11.05
### Added
- result order sort by DESC 
## [3.5] - 2023-11.05
### Added
- catalog filter added header swagger 

## [3.6] - 2023-11.29
### Added
- implement tenant support



## [4.0] - 2024-11.4
### Added
- implement tenant support update
- remove catalog filter added header swagger 

## [4.1] - 2024-11.4
### Added
- implement tenant support for event notify

## [5.1] - 2023-11.29
### added
- date filter change modified
## [5.2.2] - 2023-11.29
### added
- date filter change modified
## [5.2.7] - 2023-09.15
### added
- ignore modify filters specific reason

## [5.2.10]
### Changed
- build and runtime baseline moved down to Java 11 (bytecode target 11) on Spring Boot 2.7.18 and
  Spring Cloud 2021.0.9
- servlet and validation imports moved from `jakarta.*` back to `javax.*`
- HTTP client pool built with Apache HttpClient 4.5 instead of HttpClient 5, which is what
  `HttpComponentsClientHttpRequestFactory` supports on Spring 5
- `HttpResponse.status` is a `HttpStatus` again, `HttpStatusCode` only exists in Spring 6
- springdoc switched to `springdoc-openapi-ui` 1.7.0 and the embedded Mongo test dependency to the
  `spring27x` variant

### Fixed
- filter values are matched with their type, so equality filters on numeric, boolean and date
  fields no longer return an empty result
- comparison filters accept numbers larger than an int (for example epoch milliseconds) instead
  of failing the request
- comparison filters accept negative numbers
- date filters accept `yyyy-MM-dd` without quotes and offsets other than `Z`
- `&&` and `||` can be combined in one filter expression; previously such an expression matched
  every document in the collection
- an unparseable filter expression or an invalid regex is rejected with 400 instead of matching
  every document or failing with 500
- `offset` and `limit` can be used independently, are validated, and no longer produce an
  invalid `$limit` stage; `limit` alone previously returned the whole collection
- the `addOperations` extension point is applied again, to both the result and the count query
- tenant collection prefix is resolved from the `tenantId` header instead of `catalogId`
- error responses no longer fail when the error dispatch carries no exception


## [5.2.11] - 2026-08-01
### Added
- the query endpoint documents its filters: they are a catch-all `Map` request parameter, which
  springdoc cannot derive parameter names from, so the operation now declares a free form
  `filters` object that swagger-ui explodes into individual query parameters

### Fixed
- a paged query carries the request context onto the shared query executor, so the tenant and
  catalog headers stay readable while the results and the count run in parallel; without it the
  request failed with `IllegalStateException: No thread-bound request found`
- `LoggingAdvice` only matches handlers in `com.adl..*`, so it no longer wraps the handlers of
  libraries on the classpath; springdoc's `/v3/api-docs/swagger-config` was answered with a 500
  because the advice cast its `Map` return value to `ResponseEntity`
- a handler that returns its body instead of a `ResponseEntity` is logged with status 200 rather
  than failing with a `ClassCastException`
- the OpenAPI server URL is a real URL (`app.protocol://app.host:app.port`) instead of the bare
  `app.host` value, which swagger-ui resolved against the UI page so "Try it out" never reached
  the service; `app.context.relative` is appended when a gateway routes on a different prefix
- the API documentation title, description and version come from `app.swagger.*` instead of the
  hardcoded "Swagger Title" placeholders
