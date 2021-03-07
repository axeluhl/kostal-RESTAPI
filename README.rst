kostal-RESTAPI
==========



Introduction
------------

This library provides a pure Python interface to access Kostal Inverters via currently undocumented RESTAPIs


Tested  with Python version 3.5, 3.6 and 3.8.




Features
~~~~~~~~

* Read Events from Kostal Inverter
* Read DC, AC, Battery - and Statistics data 
* Write Battery MinSOC, MinHomeComsumption and DynamicSoc Parameters
* Write Shadow Management Parameters 
* Write Switched Output Parameters
* Accessible via commandline options


Tested with 
~~~~~~~~~~~~~~~~

* Raspberry & Windows
* Kostal Plenticore Plus 10 with connected BYD 6.4





Installation
------------
Clone / Download repo and use kostal-RESTAPI.py 


Getting started
---------------

To use ``kostal-RESTAPI`` in a project take a look at the __main__ section in kostal-RESTAPI.py how to include it in your environment
You may also run the script without any parameters to understand the command line options

If you have a session token, you may use, e.g., something like

        curl -X PUT -H 'Content-Type: application/json' -H 'Authorization: Session fb6cd3cc2840a1b95bbb06c767995a3917ca0a76803405495228c1823871e7e2' "http://kostal.axeluhl.de/api/v1/settings" --data-raw '[{"moduleid":"devices:local","settings":[{"id":"Battery:TimeControl:Enable","value":"1"}, {"id":"Battery:TimeControl:ConfMon","value":"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"}]}]'

to block battery charging on Mondays (replace 0 by 1 in those 15min sections on Monday where you would not like
the battery to get charged).

The script show-writeable-settings.sh can help discover properties that may be updated. Note, however,
that based on permissions not all properties that the API lists as "readwrite" can actually be updated
by a corresponding PUT request.


Further reading:
----------------

https://libraries.io/npm/iobroker.plenticore

Disclaimer
----------

.. Warning::

   Please note that any incorrect or careless usage of this module as well as
   errors in the implementation may harm your Inverter !

   Therefore, the author does not provide any guarantee or warranty concerning
   to correctness, functionality or performance and does not accept any liability
   for damage caused by this module, examples or mentioned information.

   **Thus, use it on your own risk!**


License
-------

Distributed under the terms of the `GNU General Public License v3 <https://www.gnu.org/licenses/gpl-3.0.en.html>`_.
