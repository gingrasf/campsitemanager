# Campsite Manager

Campsite manager is a REST API that manage reservation for a single campsite with the following constraints:

* The campsite will be free for all.
* The campsite can be reserved for max 3 days.
* The campsite can be reserved minimum 1 day(s) ahead of arrival and up to 1 month in advance.
* Reservations can be cancelled anytime.
* We assume the check-in & check-out time is 12:00 AM

### Assumptions

* Making a reservation starting in exactly one month (today + 1 month) is allowed. That's the last possible date to start a reservation
* When we update a reservation, we can only change its date. Changing the owner of a reservation is not allowed.
 

### Things to consider for making this application production ready

#### Embeded MongoDb

For the sake of simplicity an in-memory embeded mongo db instance was use as a persistence layer. This obviously is 
not production ready as it can't be scaled nor will actually persist the data when the server is restarted. To make it 
production ready we could easily swap the in-memory mongo db by a real mongo db culsters. To help with CI/CD I would use 
Kubernetes/Docker to manage/deploy thoses. A mongo db cluster would indeed work, but to have things simpler I would advocate 
using a cloud database like AWS DynamoDB instead of Mongo to avoid having to maintains the mongo instance (version upgrade, etc) 
and make scalability super simple. Another option could be a Redis instance.

#### REST API Scalability and reliability

To have full scalability to handle a huge number of request, we would need to have multiple docker instances for the application behind
and ELB. Note that given the simplicity of the app this is most-likely not needed. We still want at least two instances though for reliability.
 
#### Security and authentication

This application has no security or authentication. We would need to add some before making it production ready. 

##### Metrics and logging

This application has no metrics or logging. Some would need to be added before it is considered production ready. Both are
very easy to add using SLF4J and spring boot metrics. 

### How to start the Campsite Manager application
Campsite Manager is a simple spring boot app, as such It can be [launched in multiple ways](https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-running-your-application.html)

A simple one is through the maven plugin:

```
mvn spring-boot:run
```

*Note that the first time it's start it can take a bit of time since it needs to download files for the embeded mongo*

### How to use the application

Once the application is running, it can be used as a REST API. A way to do that is to use a tool like [Postman](https://www.getpostman.com/) and to import the
[collection for this API](postman/Campsite Manager.postman_collection.json)

[Swagger](https://swagger.io/) is also available at http://localhost:8080/swagger-ui.html but I didn't fine tune it.


### Dev environment

This projet uses [lombok](https://projectlombok.org/) so you need to make sure it is properly installed.