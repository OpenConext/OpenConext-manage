OPTIONS /pdp/api/protected/policies
GET /pdp/api/protected/policies
GET /pdp/api/protected/policies/" + id
PUT, POST /pdp/api/protected/policies
DELETE /pdp/api/protected/policies/" + id
GET /pdp/api/protected/revisions/" + id
GET /pdp/api/protected/attributes/


Make custom controller in Manage, to be used by new simple implementation in Dashboard. Switch by different property.
Export in PDP which command line -> JSON
Bulk export from PdP to Manage -> programmatically -> add PdP datasource
Bulk import done

Migration
Er zit veel complexe logica om het JSON-formaat in de PdP-GUI naar het XACML formaat te transformeren. 