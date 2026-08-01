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

