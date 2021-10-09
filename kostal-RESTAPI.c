#include <stdio.h>
#include <unistd.h>

int main(int argc, char** args) {
  printf("argc: %i\n", argc);
  char* scriptPath = "kostal-RESTAPI.py";
  char* catSecretArgs[argc+5];
  catSecretArgs[0] = scriptPath;
  catSecretArgs[1] = "--baseurl";
  catSecretArgs[2] = "http://kostal.example.com";
  catSecretArgs[3] = "--password";
  catSecretArgs[4] = "mysupersecretpassword"
  for (int i=1; i<argc; i++) {
    catSecretArgs[4+i] = args[i];
  }
  catSecretArgs[argc+4] = (char*) NULL;
  if (execv(scriptPath, catSecretArgs) < 0) {
    printf("Error\n");
  }
}
