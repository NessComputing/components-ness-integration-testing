Ness Integration Testing Helper
===============================

What does it do?
----------------

Provides various test harnesses that make spinning up external parts of the system easier.
Currently helps mostly with Postgres, Jetty, Jersey, and JMX.


What should it NOT be used for?
-------------------------------

These classes should *NEVER* be in your compile scope.  They must remain in test scope.

How do I use it?
----------------

Some good examples are in 

* flag/flag-server/...FlagIntegrationTest.java
