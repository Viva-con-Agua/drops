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

docker pull mariadb:latest
docker run --name drops-mariadb \
    -e MYSQL_ROOT_PASSWORD=admin \
    -e MYSQL_DATABASE=drops \
    -e MYSQL_USER=drops \
    -e MYSQL_PASSWORD=STRONG_PASSWORD \
    -d mariadb


```
(start.sh)

```sh
#!/bin/bash
docker stop drops-mongo
docker stop drops-mariadb
docker stop drops
```
(stop.sh)

```sh
#!/bin/bash
docker rm drops-mongo
docker rm drops-mariadb
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

OAuth 2 Handshake
-----------------
You can add a specific URL to the configuration that will be used for redirect after login. By default <code>routes.Application.index</code> is used, but you can add:

```
login.flow {
  ms.switch=true
  ms.url=/pool/
}
```
to your <code>application.conf</code>. <code>ms.url</code> can hold every valid URL (absolute and relative).

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
  "id": "4e899ba5-2897-4500-acdc-7ce998b033db",
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

Currently, there are two different versions of the webservice:
*  <code>1.0.0</code>: supports filtering by page, search and group and sorting
*  <code>1.1.0</code>: supports all features of version <code>1.0.0</code>. Additionally, 
this version supports the <code>all</code> filter: Returns all requested entities and ignores 
a given pagination filter. So, if <code>all</code> is true and no search or group filter is 
defined all saved entities will be returned.

All the mentioned above entry points are routes using the HTTP method <code>POST</code> and the body of these requests can contain a query JSON like the following example (Version 1.0.0):
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

Since version 1.1.0 the following addional parameter is allowed:
```json
{
    "filterBy" : {
        "all" : true
    }
}
```

>
<code>all</code> is a boolean parameter and it's also optional. If this parameter is set to true a possibly given <code>page</code> filter will be ignored.
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
*  <code>redirectUri</code>: a URL of your service pointing to an action that 
consumes the generated authorization code (e.g. if 
<code>``https://example.com/oauth/code/<generated_code>``</code> points to such an action, 
the <code>redirectUri</code> would be <code>``https://example.com/oauth/code/``</code>)

>
*Info:* Don't forget possible special signs at the end of the <code>redirectUri</code>
(e.g. <code>``/``</code> or <code>``?code=``</code>), because Drops simply concatinates the given URI
and the generated code.
>

Additionally, it should be obvious that you have to know the URL of the Drops Service 
you want to connect to.

Implementation
--------------
Implementing the handshake is very simple and consists of three steps:

(1)  Implement an URL path pointing to an action of your service that redirects 
(<code>HTTP 303</code> or <code>HTTP 302</code>) to: 
```
<drops_url>/oauth2/code/get?client_id=<client_id>&response_type=code&state=<state>&redirect_uri=<redirect_uri>
``` 
The variables <code>``<drops_url>``</code>, <code>``<client_id>``</code> and <code>``<redirect_uri>``</code> 
have been defined during preparation phase. <code>``state``</code> can be used to save the current state of the OAuth2 
client across redirects. Additionally, you can add the query paramater <code>``ajax``</code> indicating if the current 
redirects was issued by an ajax request. In that case no login screen will be shown, but a JSON encoded error message is 
returned.

(2)  The action handling the <code>redirectUri</code> has to be implemented. 
This action will be accessed by an HTTP redirect initiated by the Drops service.
It receives a code by a query parameter or inside the URL path and uses this code
to receive an OAuth 2 <code>AccessToken</code>. For this purpose it calls the 
Drops service directly using the webservice endpoint <code>``<drops_url>/oauth2/access_token``</code>
and the following query parameter: 
  *  <code>``grant_type=authorization_code``</code>
  *  <code>``client_id=<client_id>``</code>
  *  <code>``code=<received_code>``</code>
  *  <code>``redirect_uri=<redirectUri>``</code>

(3)  The responded <code>AccessToken</code> can be used to request the users profile,
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

## Version 0.36.65 (2021-5-5)
* [[B] #4 - UserDAO does not execute inserts as a transaction](https://github.com/SOTETO/drops/issues/4)

## Version 0.36.61 (2019-4-16)

* [[B] #340 - Bug: Nats heap space error](https://github.com/Viva-con-Agua/drops/issues/340)
* [[I] #322 - Users address](https://github.com/Viva-con-Agua/drops/issues/322)
* [[B] #339 - Bug: PasswordInfoDao](https://github.com/Viva-con-Agua/drops/issues/339)
* [[I] #329 - Remove MongoDB](https://github.com/Viva-con-Agua/drops/issues/329)
* [[I] #330 - Remove unused views](https://github.com/Viva-con-Agua/drops/issues/330)
* [[B] #333 - Delete failed](https://github.com/Viva-con-Agua/drops/issues/333)
* [[I] #324 - Export users crew to Pool 1](https://github.com/Viva-con-Agua/drops/issues/324)
* [[B] #319 - Case sensitivity not working for import](https://github.com/Viva-con-Agua/drops/issues/319)
* [[I] #318 - Email for login should not be case sensitive](https://github.com/Viva-con-Agua/drops/issues/318)
* [[B] #317 - Failure on UUID requests for Pool1 API](https://github.com/Viva-con-Agua/drops/issues/317)
* [[B] #315 - Failure on Pool1-API Response interpretation](https://github.com/Viva-con-Agua/drops/issues/315)
* [[B] #313 - Failure on Pool1 configuration](https://github.com/Viva-con-Agua/drops/issues/313)
* [[B] #312 - Pool 1 Service is called in controller and UserService](https://github.com/Viva-con-Agua/drops/issues/312)
* [[F] #259 - New email](https://github.com/Viva-con-Agua/drops/issues/259)
* [[F] #254 - WebApp action for user delete](https://github.com/Viva-con-Agua/drops/issues/254)
* [[F] #6 - Delete their own account](https://github.com/Viva-con-Agua/drops/issues/6)
* [[B] #310 - SignUp does not allow optional values](https://github.com/Viva-con-Agua/drops/issues/310)
* [[F] #306 - Update Pool1 API calls](https://github.com/Viva-con-Agua/drops/issues/306)
* [[B] #305 - Stolen Access Right](https://github.com/Viva-con-Agua/drops/issues/305)
* [[F] #303 - Role selection](https://github.com/Viva-con-Agua/drops/issues/303)
* [[I] #301 - Add route to receive user from WebApp](https://github.com/Viva-con-Agua/drops/issues/301)
* [[B] #299 - Cities are not deleted on crew update](https://github.com/Viva-con-Agua/drops/issues/299)
* [[I] #298 - WebApp-REST interface for crew selection](https://github.com/Viva-con-Agua/drops/issues/298)
* [[B] #297 - Crews view is outdated](https://github.com/Viva-con-Agua/drops/issues/297)
* [[F] #82 - Extend profile image CRUD](https://github.com/Viva-con-Agua/drops/issues/82)
* [[B] #294 - Sign Up not possible](https://github.com/Viva-con-Agua/drops/issues/294)
* [[B] #292 - Error on reset password](https://github.com/Viva-con-Agua/drops/issues/292)
* [[B] #291 - Error on calling users widget](https://github.com/Viva-con-Agua/drops/issues/291)
* [[I] #290 - Non-JSON endpoint sign up mail link](https://github.com/Viva-con-Agua/drops/issues/290)
* [[B] #289 - Asynchronous insert and find](https://github.com/Viva-con-Agua/drops/issues/289)
* [[B] #288 - Error on supporter roles (SignUp)](https://github.com/Viva-con-Agua/drops/issues/288)
* [[B] #287 - Status codes on Error](https://github.com/Viva-con-Agua/drops/issues/287)
* [[F] #284 - Extended Role-based Access Control](https://github.com/Viva-con-Agua/drops/issues/284)
* [[W] #226 - Group of users](https://github.com/Viva-con-Agua/drops/issues/226)
* [[W] #225 - Small inline user](https://github.com/Viva-con-Agua/drops/issues/225)
* [[W] #224 - Autocomplete Widget](https://github.com/Viva-con-Agua/drops/issues/224)
* [[F] #264 - CRUD Crews WebApp Controller](https://github.com/Viva-con-Agua/drops/issues/264)
* [[I] #245 - Frontend Controller SignUp](https://github.com/Viva-con-Agua/drops/issues/245)
* [[I] #244 - FrontendController SignIn](https://github.com/Viva-con-Agua/drops/issues/244)
* [[I] #241 - OAuth2 redirect to HTML during Ajax request](https://github.com/Viva-con-Agua/drops/issues/241)
* [[F] #227 - Impressum: Action for handling Impressum](https://github.com/Viva-con-Agua/drops/issues/227)
* [[I] #221 - Pool1 OES trouble](https://github.com/Viva-con-Agua/drops/issues/221)
* [[F] #93 - Add generic filter for Crews and Users](https://github.com/Viva-con-Agua/drops/issues/93)
* [[F] #32 -  Organizations are configurable](https://github.com/Viva-con-Agua/drops/issues/32)
* [[I] #222 - Allow simple filter OES](https://github.com/Viva-con-Agua/drops/issues/222)
* [[B] #215 - OAuth 2 Provider does not implements query params](https://github.com/Viva-con-Agua/drops/issues/215)
* [[B] #217 - Missing template on BadRequest](https://github.com/Viva-con-Agua/drops/issues/217)
* [[B] #213 - OAuth Database bug](https://github.com/Viva-con-Agua/drops/issues/213)
* [[F] #208 - Add MariadbPool1UserDao](https://github.com/Viva-con-Agua/drops/issues/208)
* [[F] #91 - handler for dispenser templates](https://github.com/Viva-con-Agua/drops/issues/91)
* [[I] #123 - Split Task and AccessRight Model in Database and Business Models](https://github.com/Viva-con-Agua/drops/issues/123)
* [[I] #114 - Mongo to MariaDB](https://github.com/Viva-con-Agua/drops/issues/114)
* [[B] #205 - Redirect Problems in a running Session](https://github.com/Viva-con-Agua/drops/issues/205)
* [[F] #202 - OES Create Update Delete](https://github.com/Viva-con-Agua/drops/issues/202)
* [[F] #107 - User import from external tools](https://github.com/Viva-con-Agua/drops/issues/107)
* [[F] #38 - logout event](https://github.com/Viva-con-Agua/drops/issues/38)
* [[B] #186 - the oauth handshake contains the client_secret](https://github.com/Viva-con-Agua/drops/issues/186)
* [[I] #183 - Accessibility of views bases on Pool 1 connection](https://github.com/Viva-con-Agua/drops/issues/183)
* [[F] #180 - Allow webservice secrets](https://github.com/Viva-con-Agua/drops/issues/180)
* [[F] #106 - Send new registered user to Pool 1](https://github.com/Viva-con-Agua/drops/issues/106)

## Version 0.19.14 (2017-12-14)
* [[I] #124 - Use singular labels for rest urls](https://github.com/Viva-con-Agua/drops/issues/124)
* [[I] #120 - False Translations](https://github.com/Viva-con-Agua/drops/issues/120)
* [[I] #117 - Translation](https://github.com/Viva-con-Agua/drops/issues/117)
* [[F] #102 - CRUD for Webservice](https://github.com/Viva-con-Agua/drops/issues/102)
* [[I] #111 - Default redirect page after login](https://github.com/Viva-con-Agua/drops/issues/111)
* [[F] #73 - RESTful interface to request access rights](https://github.com/Viva-con-Agua/drops/issues/73)
* [[B] #98 - Comments run into error on publish](https://github.com/Viva-con-Agua/drops/issues/98)
* [[I] #69 - Associate Access Rights to Microservices](https://github.com/Viva-con-Agua/drops/issues/69)
* [[F] #71 - Relations between users and tasks](https://github.com/Viva-con-Agua/drops/issues/71)
* [[F] #70 - Relations between Tasks and Access Rights](https://github.com/Viva-con-Agua/drops/issues/70)
* [[F] #47 - Task Object](https://github.com/Viva-con-Agua/drops/issues/47)
* [[B] #85 - Preselection of crew does not work](https://github.com/Viva-con-Agua/drops/issues/85)
* [[F] #12 - Extra profile image](https://github.com/Viva-con-Agua/drops/issues/12)
* [[F] #5 - Users profile](https://github.com/Viva-con-Agua/drops/issues/5)
* [[F] #35 - Remove active flag](https://github.com/Viva-con-Agua/drops/issues/35)
* [[F] #22 - ReDesign](https://github.com/Viva-con-Agua/drops/issues/22)
* [[F] #55 - Add relational database support for task managment](https://github.com/Viva-con-Agua/drops/issues/55)
* [[B] #61 - Clients are not authorized to get an Access Token](https://github.com/Viva-con-Agua/drops/issues/61)
* [[I] #60 - Remove uneeded attribute OAuthClient](https://github.com/Viva-con-Agua/drops/issues/60)
* [[I] #59 - Remove OAuthClient Secret](https://github.com/Viva-con-Agua/drops/issues/59)

## Version 0.9.4 (2017-01-26)
*  [[I] #26 - Crews WS: IDs](https://github.com/Viva-con-Agua/drops/issues/26)
*  [[I] #24 - WS: Support requesting all crews](https://github.com/Viva-con-Agua/drops/issues/24)
*  [[I] #25 - Versioning for webservice](https://github.com/Viva-con-Agua/drops/issues/25)
*  [[I] #18 - Multiple search conditions](https://github.com/Viva-con-Agua/drops/issues/18)
*  [[F] #16 - Test Data](https://github.com/Viva-con-Agua/drops/issues/16)
*  [[F] #9 - Supporter Webservice](https://github.com/Viva-con-Agua/drops/issues/9)
*  [[F] #8 - Different groups](https://github.com/Viva-con-Agua/drops/issues/8)
*  [[F] #10 - Configurable geography](https://github.com/Viva-con-Agua/drops/issues/10)
*  [[F] #7 - Different roles](https://github.com/Viva-con-Agua/drops/issues/7)
*  [[F] #4 - Oauth 2 provider](https://github.com/Viva-con-Agua/drops/issues/4)
*  [[F] #3 - Users login](https://github.com/Viva-con-Agua/drops/issues/3)
*  [[F] #2 - Users are able to register themselves](https://github.com/Viva-con-Agua/drops/issues/2)
*  [[F] #1 - Represent a user](https://github.com/Viva-con-Agua/drops/issues/1)
