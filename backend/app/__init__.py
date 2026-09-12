"""GPHub application package."""

import truststore

# This project is an application, not a reusable library: inject the Windows
# trust store before modules import requests/urllib3.
truststore.inject_into_ssl()