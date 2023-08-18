# DO NOT EDIT THIS FILE - See: https://eclipse.dev/jetty/documentation/

[description]
Enables Graceful processing of requests

[tags]
server

[depend]
server

[xml]
etc/jetty-graceful.xml

[ini-template]

## If the Graceful shutdown should wait for async requests as well as the currently dispatched ones.
# jetty.statistics.gracefulShutdownWaitsForRequests=true
