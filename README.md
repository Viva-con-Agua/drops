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
After the default Play 2 App production deployment, the system requires the call of the route <code>/auth/init</code>. This call creates a default admin account using a configured Email and Password. Both can be changed inside the admin.conf.
Additionally the same can be done for crews. The route that should be used is <code>/crews/init/</code>.

ChangeLog
=========

## Version 0.7.0 (2016-12-19)
*  [[F] #8 - Different groups](https://repo.cses.informatik.hu-berlin.de/gitlab/sozmed/waves/issues/8)
*  [[F] #11 - Configurable geography](https://repo.cses.informatik.hu-berlin.de/gitlab/sozmed/waves/issues/11)
*  [[F] #7 - Different roles](https://repo.cses.informatik.hu-berlin.de/gitlab/sozmed/waves/issues/7)
*  [[F] #4 - Oauth 2 provider](https://repo.cses.informatik.hu-berlin.de/gitlab/sozmed/waves/issues/4)
*  [[F] #3 - Users login](https://repo.cses.informatik.hu-berlin.de/gitlab/sozmed/waves/issues/3)
*  [[F] #2 - Users are able to register themselves](https://repo.cses.informatik.hu-berlin.de/gitlab/sozmed/waves/issues/2)
*  [[F] #1 - Represent a user](https://repo.cses.informatik.hu-berlin.de/gitlab/sozmed/waves/issues/1)