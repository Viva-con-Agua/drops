Drops
=====

This is a user management component for a micro-component application, supporting [Viva con Agua](https://www.vivaconagua.org) in organizing volunteering activities. The component is based on the [Play framework](https://www.playframework.com/), [Silhouette](http://silhouette.mohiva.com/) and the [dwPlayDemo](https://www.ibm.com/developerworks/apps/download/index.jsp?contentid=1020131&filename=dwPlayDemo.zip&method=http&locale=), implementing a basic user management. The authentication will be explained by [Pablo Pedemonte from IBM](http://www.ibm.com/developerworks/library/wa-playful-web-dev-1-trs-bluemix/index.html). It provides:

  * Email-based user sign up flow.
  * Email-based password reset flow.
  * Authentication using credentials (email + password).
  * Account linking: link a social provider profile to your credentials profile.
  * OAuth1 Twitter authentication (not turned on by default).
  * Anonymous or authenticated access to home page.
  * Profile information for authenticated users.

Install
=======
Drops can be deployed using Docker. The `drops.informatik.hu-berlin.de` 
config file (in `conf/` directory) contains all configuration information needed 
to run the service on production. Additionally, the following BASH scripts 
should be used:

```sh
#!/bin/bash
docker pull mongo
docker pull cses/drops:0.9.0
```
(install.sh)

```sh
#!/bin/bash
docker run --name drops-mongo --restart=unless-stopped -d mongo

docker run --name drops --link drops-mongo:mongo --restart=unless-stopped -v $(pwd)/drops.informatik.hu-berlin.de.p12:/certs.p12 -h drops.informatik.hu-berlin.de -p 443:9443 cses/drops:0.9.0 \
    -Dconfig.resource=drops.informatik.hu-berlin.de.conf \
    -Dhttps.port=9443 \
    -Dhttps.address=drops.informatik.hu-berlin.de \
    -Dhttp.address=drops.informatik.hu-berlin.de \
    -Dhttp.port=disabled \
    -Dplay.server.https.keyStore.path=/certs.p12 \
    -Dplay.server.https.keyStore.type=PKCS12 \
    -Dplay.server.https.keyStore.password=61aca05eb0eb07c4c0c53f35b7edf3e1 \
    -Dmongodb.uri=mongodb://mongo/drops \
    -J-Xms128M -J-Xmx512m -J-server \
    > server-output 2>&1 &
```
(start.sh)

```sh
#!/bin/bash
docker stop drops-mongo
docker stop drops
```
(stop.sh)

```sh
#!/bin/bash
docker rm drops-mongo
docker rm drops
```
(remove.sh)

>
*Notice:* 
All server generated output will be written to the `server-output` file.
This is also needed to confirm users since the production server also uses a mock
mail server.
>

After the default Play 2 App production deployment, the system requires the call of the route <code>/auth/init</code>. This call creates a default admin account using a configured Email and Password. Both can be changed inside the admin.conf.
Additionally the same can be done for crews. The route that should be used is <code>/crews/init</code>.

Dummy Data
==========
Using an admin account test users can be generated. For generating the following 
route has to be called: <code>/users/init/:count/:countSpecialRoles</code>, 
where <code>:count</code> has to be replaced by the count of new users that 
should be generated and <code>:countSpecialRoles</code> by the number of users 
with special roles (next to "Supporter").

>
Currently, the generation does not uses bulk inserts, so an insert operation 
will be executed for each test user. As a consequence, the system needs a lot of 
time.
>

Webservice
==========

Results
-------
The Drops service implements a webservice for requesting users and crews. Users will be described by the following JSON that is also returned to a valid request:
```json
{
    "id": "f2329fe0-c94b-4b33-a039-296c1a7dcba6",
    "profiles": [
      {
        "loginInfo": {
          "providerID": "credentials",
          "providerKey": "test@test.com"
        },
        "primary": true,
        "confirmed": true,
        "email": "test@test.com",
        "supporter": {
          "firstName": "Tester",
          "lastName": "Tester",
          "fullName": "Tester Tester",
          "mobilePhone": "0000/0000000",
          "placeOfResidence": "Hamburg",
          "birthday": 315529200000,
          "sex": "male",
          "crew": {
            "crew": {
              "name": "Berlin",
              "country": "DE",
              "cities": [
                "Berlin"
              ]
            },
            "active": true
          },
          "pillars": [
            {
              "pillar": "operation"
            },
            {
              "pillar": "finance"
            }
          ]
        }
      }
    ],
    "roles": [
      {
        "role": "supporter"
      }
    ]
  }
```
A user consists of an ID, multiple profiles and multiple roles. Drops implements different ways to create a virtual representation for a user. So he or she could
use default credentials or an existing Google or Facebook account. Also the users could connect their Drops accounts to Google or Facebook Accounts. In order to 
establish which profile should be used, the <code>primary</code> flag marks the so called profile. Additionally, it is possible to detect if an user has confirmed 
his or her account using the confirmation mail after the sign up (<code>confirmed</code> flag).

Next to the master data associated to the <code>Supporter</code>, there are two information specific to Viva con Agua:
*  Supporters can have a Crew
*  Supporters can have a selection of the four Viva con Agua pillars (<code>operation</code> - dt. "Aktionen", <code>finance</code>, <code>education</code> and <code>network</code>)

Currently, four different roles are implemented: <code>supporter</code> (default role), <code>volunteerManager</code>, <code>employee</code> and <code>admin</code>.
*Supporters* are all volunteering people of Viva con Agua, while a *volunteer manager* is a supporter that coordinates a crew. So, these roles are connected and one
crew can have multiple *volunteer managers*. An *employee* is not volunteering, but works for Viva con Agua and coordinates all entities inside the social system
(means crews and supporter). 
Contrary to the other roles the *admin* is more technical. Users holding this role are able to access all possible configurations of the system.

Also the webservice will describe a crew by the following JSON:

```json
{
  "name": "Berlin",
  "country": "DE",
  "cities": [
    "Berlin"
  ]
}
```

A crew has a name, a country code (described using the 2-Alpha codes of the [ISO 3166-1](https://en.wikipedia.org/wiki/ISO_3166-1)) and a set of cities. Maybe there
are regions with a lot of small cities or towns and a working infrastructure, where multiple volunteers in different cities join to one crew.

Access
------
There are three entry points implemented:
*  <code>/rest/users</code>: Returns a JSON containing a list of requested users
*  <code>/rest/users/:id</code>: Returns a JSON containing the user identified by the given ID
*  <code>/rest/crews</code>: Returns a JSON containing a list of requested crews 

There are three query parameter those have to be defined:
*  <code>client_id</code>: Your service has to be registered at the Drops service. Use the registered ID.
*  <code>client_secret</code>: Your service has to be registered at the Drops service. Use the registered Secret.
*  <code>version</code> or <code>v</code>: The version of the service that your request assumes. It is an optional
parameter. If nothing is defined, the latest version will be used.

All these entry points are routes using the HTTP method <code>POST</code> and the body of these requests can contain a query JSON like the following example:
```json
{
    "filterBy" : {
        "page" :  {
            "lastId": "f2329fe0-c94b-4b33-a039-296c1a7dcba6",
            "countsPerPage": 100
        },
        "search" : [
            {
                "keyword" : "Berlin",
                "fields": ["profiles.supporter.crew.crew.name"]
            },
            {
                "keyword" : "Test",
                "fields": ["profiles.supporter.firstName", "profiles.supporter.lastName"]
            }
        ],
        "groups" : [
            {
                "groupName" : "supporter",
                "area" : { "name" : "role" }
            },
            {
                "groupName" : "finance",
                "area" : { "name" : "pillar" }
            }	
        ]
    },
    "sortBy" : [
        {
            "field": "profiles.supporter.firstName",
            "dir" : "asc"
        }
    ]
}
```
>
If the crews webservice is requested, the <code>groups</code> filter will be ignored. Additionally, the <code>lastId</code> inside the <code>page</code> filter can be used for the crews name.
>

The <code>filterBy</code> JSON block reduces the resulting set. It is possible to reduce the set using a pagination (<code>lastId</code> is optional and if given the object with this ID won't be returned),
a search query (Regex based - it found every object that contains the given keyword as a substring of it's value inside one of the given fields) and groups (returns each object that is part of all given groups).

Using the <code>sortBy</code> list of fields the results can be ordered. The sorting criteria will be applied in the given order.

>
*Note*: Every <code>POST</code> request to an entry points has to set the HTTP header <code>Content-Type: application/json</code>
>

OAuth2 based session handshake
==============================
Drops allows other services to intiate an OAuth 2 handshake. The service implements
an OAuth 2 server and allows <code>authorization_code</code> and <code>password</code>
based authentication. Assuming that all other services are part of the official 
infrastructure, Drops implements a small addition to the OAuth 2 standard: The 
<code>authorization_code</code> authentication does not require additional permission
by the user. 

Basically, your service will make an HTTP redirect to the Drops service, so Drops
can check if there exists a session for the redirected client. If there exists no
session until now, Drops will show a login screen and the can access using his or 
her default credentials. Otherwise Drops will generate an authorization code (based
on the assumption that all registered services are part of the official infrastructure. 
So, you can trust these services) and redirects back to your service. Now, your 
service is able to request an access token using a webservice provided by Drops
and to request the users profile by another webservice that is also provided by
Drops and requires a valid access token as parameter.

Preparation
-----------
Your service has to be registered in Drops. For this purpose you have to send a 
mail to the Drops-Service administrator containing the following information:
*  <code>client_id</code>: e.g. the name of your service
*  <code>client_secret</code>: a safe random string (it should have at least 12 signs)
*  <code>codeRedirectUri</code>: a URL of your service pointing to an action that 
consumes the generated authorization code (e.g. if 
<code>``https://example.com/oauth/code/<generated_code>``</code> points to such an action, 
the <code>codeRedirectUri</code> would be <code>``https://example.com/oauth/code/``</code>)

>
*Info:* Don't forget possible special signs at the end of the <code>codeRedirectUri</code>
(e.g. <code>``/``</code> or <code>``?code=``</code>), because Drops simply concatinates the given URI
and the generated code.
>

Additionally, it should be obvious that you have to know the URL of the Drops Service 
you want to connect to.

Implementation
--------------
Implementing the handshake is very simple and consists of three steps:

1.  Implement an URL path pointing to an action of your service that mades a
Redirect (<code>HTTP 303</code> or <code>HTTP 302</code>) to 
<code>``<drops_url>/oauth2/code/get/<client_id>/client_secret>``</code>. The 
variables <code>``<drops_url>``</code>, <code>``<client_id>``</code> and 
<code>``<client_secret>``</code> have been defined during preparation phase.

2.  The action handling the <code>codeRedirectUri</code> has to be implemented. 
This action will be accessed by an HTTP redirect initiated by the Drops service.
It receives a code by a query parameter or inside the URL path and uses this code
to receive an OAuth 2 <code>AccessToken</code>. For this purpose it calls the 
Drops service directly using the webservice endpoint <code>``<drops_url>/oauth2/access_token``</code>
and the following query parameter: 
  *  <code>grant_type=authorization_code</code>
  *  <code>``client_id=<client_id>``</code>
  *  <code>``client_secret=<client_secret>``</code>
  *  <code>``code=<received_code>``</code>

3.  The responded <code>AccessToken</code> can be used to request the users profile,
by requesting another webservice supplied by the Drops service:
  *  endpoint: <code>``<drops_url>/oauth2/rest/profile``</code>
  *  query string: <code>``access_token=<access_token>``</code>

An error-free response of the request for an access token will be a JSON:
```json
{
    "token_type" : "some_string",
    "access_token" : "a random string",
    "expires_in" : a long,
    "refresh_token" : "a random string"
}
```
The key <code>access_token</code> of such an response should be used as value of
the variable <code>access_token</code> used in step 3.

An error-free response of the request for a profile will be a JSON describing a 
user as shown before. 

Using this information your service is able to initiate a session for the user 
and your service.


ChangeLog
=========

## Version 0.9.2 (2017-01-13)
*  [[I] #26 - Versioning for webservice](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/26)
*  [[I] #19 - Multiple search conditions](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/19)
*  [[F] #17 - Test Data](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/17)
*  [[F] #9 - Supporter Webservice](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/9)
*  [[F] #8 - Different groups](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/8)
*  [[F] #11 - Configurable geography](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/11)
*  [[F] #7 - Different roles](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/7)
*  [[F] #4 - Oauth 2 provider](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/4)
*  [[F] #3 - Users login](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/3)
*  [[F] #2 - Users are able to register themselves](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/2)
*  [[F] #1 - Represent a user](https://repo.cses.informatik.hu-berlin.de/gitlab/sell/drops/issues/1)