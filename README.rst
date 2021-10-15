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

Create a user group, e.g., "kostal" and add those users to it that you'd like to have
access to the interver.

Copy kostal-RESTAPI.h.template to kostal-RESTAPI.h and change ownership to a privileged account.
Make the file readable for the privileged account only and "su" into that user then. Compile
using
        gcc -o kostal-RESTAPI kostal-RESTAPI.c
        chgrp kostal kostal-RESTAPI
        chmod 710 kostal-RESTAPI
The chmod command makes the file non-readable for other users but your privileged user
and still allows members of the "kostal" group to execute it. This way, you grant members
of that "kostal" group access to your inverter. Make sure this is what you want.

Getting started
---------------

Change mode/ownerships for kostal-RESTAPI.h.template to a privileged used, then set your base URL and password,
then copy to kostal-RESTAPI.h with the same privileged permissions. Afterwards, as the privileged user,
compile using

        gcc -o kostal-RESTAPI kostal-RESTAPI.c

with the privileged user, resulting in an executable "kostal-RESTAPI". chmod 711 kostal-RESTAPI to make it executable
but not readable by all other users. This then allows you to run something like this, without password entry:

        ./kostal-RESTAPI -ReadString1Data 1

        {'I': 2.92435e-05, 'P': -0.1151212305, 'U': 8.2680463791}

Note that anyone with exection permission on the resulting kostal-RESTAPI file has access to the inverter
using the baked-in password.

To use ``kostal-RESTAPI`` in a project take a look at the __main__ section in kostal-RESTAPI.py how to include it in your environment
You may also run the script without any parameters to understand the command line options

If you have a session token, you may use, e.g., something like

        curl -X PUT -H 'Content-Type: application/json' -H 'Authorization: Session fb6cd3cc2840a1b95bbb06c767995a3917ca0a76803405495228c1823871e7e2' "http://kostal.axeluhl.de/api/v1/settings" --data-raw '[{"moduleid":"devices:local","settings":[{"id":"Battery:TimeControl:Enable","value":"1"}, {"id":"Battery:TimeControl:ConfMon","value":"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"}]}]'

to block battery charging on Mondays (replace 0 by 1 in those 15min sections on Monday where you would not like
the battery to get charged).

If you'd like to find out the current charge/discharge limitations, try

        curl -d '[{"moduleid":"devices:local","settingids":["Battery:TimeControl:ConfMon","Battery:TimeControl:ConfTue","Battery:TimeControl:ConfWed","Battery:TimeControl:ConfThu","Battery:TimeControl:ConfFri","Battery:TimeControl:ConfSat","Battery:TimeControl:ConfSun"]}]' -X POST -H 'Content-Type: application/json' -H 'Authorization: Session 138a29f5fbbced442c959a279566e1ddee5c27adccedadf333e6603a0b2a143d' http://kostal.axeluhl.de/api/v1/settings

The script show-writeable-settings.sh can help discover properties that may be updated. Note, however,
that based on permissions not all properties that the API lists as "readwrite" can actually be updated
by a corresponding PUT request.

To assemble the settings ID for a particular date to control battery loading for that day, use a Python
expression like this:

        import datetime
        print ("Battery:TimeControl:Conf"+datetime.date(2021, 3, 8).strftime("%a"))

This assumes en_US locale. Try this:

        import locale
        locale.setlocale(locale.LC_ALL, 'en_US')

New options -TimeControlEnable and -TimeControlConf[Day] with [Day] being one of Mon, Tue, Wed, Thu, Fri, Sat, or Sun
are now supported. TimeControlEnable accepts 0 and 1 as values; the TimeControlConf options receive strings
of form 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
with 0, 1, or 2 for each digit with 0 meaning no limit, 1 meaning charge blocked, 2 meaning discharge blocked.

Example usage:

  ./kostal-RESTAPI.py --baseurl http://kostal.example.com --password 'my-secret-password' -TimeControlConfMon 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

Setting battery time control for a time period starting now:

  ./kostal-RESTAPI.py --baseurl http://kostal.example.com --password 'my-secret-password' -SetBatteryTimeControl 60 2

blocks battery discharge (value 2) for the next 60 minutes, starting now.

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
