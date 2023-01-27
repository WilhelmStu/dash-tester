# Dash client tester

A testing suite for the Adaptive Media Streaming course WS2022/23. Selenium and Mitmproxy are used to test a given
Dash-Client for its buffer size and HTTP response times, based on predefined network conditions.
The specific HTTP server which was used for the tests, can be found
on [Docker Hub](https://hub.docker.com/r/wilhelmstu/adaptive-media-streaming).

For this tool to work the [Mitmproxy](https://mitmproxy.org/) needs to be installed and
its [certificate](https://docs.mitmproxy.org/stable/overview-getting-started/) configured. Additionally, the Chrome
browser is required for the simulation of network conditions.
