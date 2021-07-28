# ring-openapi-validator

Validate ring requests and responses against Swagger v2 / OpenAPI v3
specifications. Uses [Atlassian's Swagger Request
Validator](https://bitbucket.org/atlassian/swagger-request-validator/src/master/)
to do the actual validation.

## Dependency coordinates

    [nl.jomco/ring-openapi-validator "0.1.1"]

## Usage

      (require '[nl.jomco.ring-openapi-validator :as validator])
      
      (def validator (validator/openapi-validator "path/to/spec.json"))
      
      (when-let [issues (validator/validate-interaction validator
                                                        ring-request ring-response)]
        (doseq [issue issues]
          (prn issue)))

## Documentation

API documentation is available inline and at [cljdoc](https://cljdoc.org/d/nl.jomco/ring-openapi-validator/CURRENT).

## License

Copyright © 2020 - 2021 Joost Diepenmaat

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
