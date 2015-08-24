# Service Registry Broker

This is a sample of a [Cloud Foundry Service Broker] (https://docs.cloudfoundry.org/services/api.html) exposing service registry functionality to applications running on Cloud Foundry. 

Information about backend services can be registered and then made available to other apps that bind to the exposed service instances on CF. Registration configuration of a service on the registry can include multiple plans and associated credentials per plan (like bronze plan will allow only 5 connections to a dev instance endpoint, while gold will allow 50 connections to a prod instance endpoint). The data about the services, plans and credentials is persisted in a Database (by default uses MySQL service binding when deployed on CF). 

Once a service is created based on the plan, any client app bound to the underlying service would information about the associated endpoint and any credentials information in the form of a VCAP_SERVICES env variable when pushed to CF. The client/consumer app needs to parse the VCAP_SERVICES env variable or use the appropriate spring cloud connectors to use the endpoint configuration and invoke the related service.

The Service Broker does not create any new set of service instances, or spin off new apps or containers or vms. The backend services are expected to be up and running at some endpoint as specified in the credentials. The Service Registry Broker also does not manage or monitor the health or lifecycle of underlying services and only acts as a bridging layer to provide information about the service to any consuming application, unlike Eureka or other services.

The Service Registry exposes a REST api interface to create/read/update/delete services, plans and credentials.

# Steps to deploy the service-registry-broker:

* Deploy the backend or any test/simulation service. A sample simulation service is available at [document-service] (https://github.com/cf-platform-eng/document-service)
* Edit the input.sql under src/main/resources folder to populate some prebuilt services and associated plans, credentials, endpoints etc.
* Run maven to build.
* Push the app to CF using manifest.yml. Edit the manifest to bind to a MySQL Service instance.
* Register the app as a service broker (this requires admin privileges on the CF instance) against Cloud Foundry.
* Expose the services/plans within a specific org or publicly accessible.
* Create the service based on the plan.
* Deploy the client app that would bind to the service and consume the service.
A sample client app is available on github at [sample-doc-retrieve-gateway client] (https://github.com/cf-platform-eng/sample-doc-retrieve-gateway/)

Sample:
```
# Edit the domain endpoint within input.sql file 
# Change the reference to correct CF App Domain
# Push the service registry broker app to CF.

# Then enable service-access for the service against the org/space or for everyone.
cf enable-service-access PolicyInterface

# Create a service based on a service defn and plan
cf create-service EDMSRetreiveInterface basic EDMSRetreiveInterface-basic

# Now push a sample client app that can bind to the newly created service
# cf bind-service sample-registry-client EDMSRetreiveInterface-basic
```

# Updating the catalog

After a new service has been registered or after updates on the service registry, update the catalog with the Cloud Foundry controller using update-service-broker api call.

```
cf update-service-broker service-registry-broker testuser somethingsecure http://service-registry-broker.xyz.com/
```

# Using the Service Registry REST interface
* List Services
To list services, use GET against /services
Example:
```
curl -v -u <user>:<password> http://service-registry-uri/services 
```
To list a specific service, use GET against /services/<ServiceName>
Example:
```
curl -v -u <user>:<password> http://service-registry-uri/services/MyService
```

* Bulk insert a set of services with nested plans
```
curl -v -u <user>:<password> http://service-registry-uri/services -d @<JsonPayloadFile>.json -H "Content-type: application/json" -X POST
```
Example:
```
curl -v -u testuser:testuser http://test-service-registry.xyz.com/services/ -d @./add-services.json -H "Content-type: application/json" -X POST
```

* To add a new plan under an existing service:
To create a new plan, use PUT operation against /services/<ServiceName>/plans
```
curl -v -u <user>:<password> http://service-registry-uri/services/<ServiceId>/plans/<NewPlanId> -d @<JsonPayloadFile>.json -H "Content-type: application/json" -X PUT
```
Example:
```
# Creates a new plan under service with name **`test-service`**  
curl -v -u testuser:testuser http://test-service-registry.xyz.com/services/test-service/plans -d @./add-plan.json -H "Content-type: application/json" -X PUT
```

* List Plans
To list plans, use GET against /services/<ServiceName>/plans
Example:
```
curl -v -u <user>:<password> http://service-registry-uri/services/MyService/plans
```

To list a specific plan detail, use GET against /services/<ServiceName>/plans/<PlanName>
Example:
```
curl -v -u <user>:<password> http://service-registry-uri/services/MyService/plans/MyPlan
```

* To associate a credential to an existing plan (within a service):
```
curl -v -u <user>:<password> http://service-registry-uri/services/<ServiceName>/plans/<PlanName>/creds -d @<JsonPayloadFile>.json -H "Content-type: application/json" -X PUT
```
Example: 
```
# To create a new credentials for plan **`test-plan`** under service with name **`test-service`**  
curl -v -u testuser:testuser http://test-service-registry.xyz.com/services/test-service/plans/test-plan/creds -d @./add-cred.json -H "Content-type: application/json" -X PUT
```

* To delete a resource, use DELETE option against the resource
Example:
```
# Delete a service
curl -v -u testuser:testuser http://test-service-registry.xyz.com/services/test-service -X DELETE

# Delete a plan
# This wont delete the underlying credential as it may be in use...
curl -v -u testuser:testuser http://test-service-registry.xyz.com/services/test-service/plans/test-plan -X DELETE

# Delete credential associated with a plan
curl -v -u testuser:testuser http://test-service-registry.xyz.com/services/test-service/plans/test-plan/creds  -X DELETE
```

# Notes

* To lock down access to the broker using a specific set of credentials, define following properties via the application.properties 
```
   security.user.name=<UserName>
   security.user.password=<UserPassword>
```

or the cf env during app push (or via manifest.yml)

```
   cf set-env <appName> SECURITY_USER_NAME <UserName>
   cf set-env <appName> SECURITIY_USER_PASSWORD <UserPassword>
```

* There can be multiple set of services, each with unlimited plans.

* Each plan in any service would be associated with one and only credentials row.
The Plan can be space or env specific and allow the service instance to use the set of credentials associated with the plan.

* Administrator can open up or lock down access to the plans using orgs and spaces.
Refer to [Service Plans Access control] (https://docs.cloudfoundry.org/services/access-control.html#enable-access)

* The Hibernate jpa can delete the table data and update the schemas each time the app instance is started with the update option (check application.properties).
To avoid loss of data, remove the auto creation from application.properties and manually create the tables using the service-registry.ddl file.
Or go with `update` auto option and then once the tables are created with right data set, switch off the auto table creation to `none`
Either make the change in the application.properties or via env variable passed via manifest.yml

* Credentials
Atleast one attribute needs to be passed in during creation of credentials : **`uri`**. username, password and other attributes can be null.
There can be any number of additional attributes (like username, password, certFile, certLocation, certType, otherTags...) that can be passed in during creation of the credentials entry. Check the add-creds.json file for some sample input. These would be then passed along to the apps consuming the instantiated services.
