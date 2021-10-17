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
Clone / Download repo and use kostal-RESTAPI.py. If you'd like to have a more convenient yet secure way that doesn't require you to enter your password for each invocation without having to place the password in a file readable to the users whom you'd like to grant permission to control your inverter, read on.

Create a user group, e.g., "kostal" and add those users to it that you'd like to have
access to the interver.

Copy kostal-RESTAPI.h.template to kostal-RESTAPI.h and change ownership to a privileged account.
Make the file readable for the privileged account only and "su" into that user then. Compile
using

        gcc -o kostal-RESTAPI kostal-RESTAPI.c; chgrp kostal kostal-RESTAPI; chmod 710 kostal-RESTAPI

The chmod command makes the file non-readable for other users but your privileged user
and still allows members of the "kostal" group to execute it. This way, you grant members
of that "kostal" group access to your inverter. Make sure this is what you want.

If you don't even like the idea of having the password appear in between the ``gcc`` call finishing and the ``chgrp`` and ``chmod`` commands running, do all of this in a directory readable only to the privileged user.

Getting started
---------------

        ./kostal-RESTAPI -ReadString1Data 1

        {'I': 2.92435e-05, 'P': -0.1151212305, 'U': 8.2680463791}

Note that anyone with exection permission on the resulting kostal-RESTAPI file has access to the inverter
using the baked-in password.

Example usage of the Python script, requiring URL and password:

  ./kostal-RESTAPI.py --baseurl http://kostal.example.com --password 'my-secret-password' -TimeControlConfMon 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

Setting battery time control for a time period starting now:

  ./kostal-RESTAPI.py --baseurl http://kostal.example.com --password 'my-secret-password' -SetBatteryTimeControl 60 2
  
Or in the short form:

  ./kostal-RESTAPI -SetBatteryTimeControl 60 2

blocks battery discharge (value 2) for the next 60 minutes, starting now.

There are a few convenience scripts:

 - kostal-setBatteryTimeControl: invokes ``./kostal-RESTAPI -SetBatteryTimeControl $1 $2`` and waits for $1 minutes, then resets the battery time control settings to their values at the time of invoking this script
 - kostal-noDischarge: short for ``kostal-setBatteryTimeControl $1 2``
 - kostal-setBatteryTimeControlForAll: shorthand for ``./kostal-RESTAPI -SetBatteryTimeControl 10080 $1`` which sets all values for the entire week to $1

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
