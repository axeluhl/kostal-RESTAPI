#include <stdio.h>
#include <unistd.h>

#include "kostal-RESTAPI.h"

/*
 This is a little C wrapper for the kostal-RESTAPI.py script, expected to come in the same delivery as this
 file. The problem with the Python script is that it requires two mandatory parameters, one of which being
 the password for the Kostal inverter. This password is not to be published in any file readable to the
 general public or not even to any user on a host where it's run.

 Unix/Linux operating systems offer a concept called "setuid" which allows a user to mark an executable
 with a corresponding permission which causes the process executing it to assume the identify of the
 owner user (and/or group). This works only for binary executables, not for scripts for which the
 actual executable is the script processing shell (such as /bin/bash). So, a binary executable is
 needed that contains the password secret but is readable only for a privileged user, such as "root".
 The executable will acquire the privileged user's permissions.
 */
int main(int argc, char** args) {
  char* scriptPath = "kostal-RESTAPI.py";
  char* catSecretArgs[argc+5];
  catSecretArgs[0] = scriptPath;
  catSecretArgs[1] = "--baseurl";
  catSecretArgs[2] = BASEURL;
  catSecretArgs[3] = "--password";
  catSecretArgs[4] = PASSWORD;
  for (int i=1; i<argc; i++) {
    catSecretArgs[4+i] = args[i];
  }
  catSecretArgs[argc+4] = (char*) NULL;
  if (execv(scriptPath, catSecretArgs) < 0) {
    printf("Error\n");
  }
}
