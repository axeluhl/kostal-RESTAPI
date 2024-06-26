#!/usr/bin/env python3
# -*- coding: utf-8 -*-

#  kostal-RESTAPI - Read and write access to Kostal Inverter using /dev/api access
#  Copyright (C) 2020  Kilian Knoll -->kilian.knoll@gmx.de
#  
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
#
#  Please note that any incorrect or careless usage of this module as well as errors in the implementation can damage your Smart Energy Meter!
#  Therefore, the author does not provide any guarantee or warranty concerning to correctness, functionality or performance and does not accept any liability for damage caused by this module, examples or mentioned information.
#  Thus, use it at your own risk!
#
#
#  Purpose: 
#           Set Values for the Kostal Inverter using the undocumented REST API
#           Read  Events of the Inverter
#           Read all key Live data - as well as statistics information 
#           
#           The main routine contains a couple of sample scenario´s - e.g:
#                changing the MinSOC of the battery
#                reading the events
#                reading various other information 
#           
#  Based on :
# https://stackoverflow.com/questions/59053539/api-call-portation-from-java-to-python-kostal-plenticore-inverter
#
# Version 1.0
#
# Update  June 14 2020
# Version 1.0.1
# Added ability to change shadow management parameters
# Version 1.0.2
# Added ability to change Switched output parameters
# Version 1.0.3
# Added ability to operate via commandline interface to either set - or query parameters
# For Options use python3 KostalRestapi.py -h 
# Version 1.0.3
# Added Command Line Option to write Battery Device Type -WriteBatteryDeviceType
# Version 1.04
# Bugfix in LogMeOut function 
#
# 
# Tested with:
# python 3.6.2 (windows)
# python 3.5.3 (raspbian)
# python 3.8   (windows)
#
# Nothing configurable beyond this point

import sys
import random
import string
import base64
import json
import requests
import hashlib
import os
import hmac
import time
import argparse
import traceback
from datetime import timedelta
from datetime import datetime
from pytz import timezone

from Cryptodome.Cipher import AES  #windows

import binascii
import pprint
from collections import OrderedDict

WEEKDAYS=['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
INTERVAL=timedelta(minutes=15)
TZ=timezone('Europe/Berlin')
USER_TYPE = "user"
AUTH_START = "/auth/start"
AUTH_FINISH = "/auth/finish"
AUTH_CREATE_SESSION = "/auth/create_session"
ME = "/auth/me"
pp = pprint.PrettyPrinter(indent=4)

def LogMeIn(BASE_URL, PASSWD):
    def randomString(stringLength):
        letters = string.ascii_letters
        return ''.join(random.choice(letters) for i in range(stringLength))
    
    u = randomString(12)
    u = base64.b64encode(u.encode('utf-8')).decode('utf-8')
    
    step1 = {
    "username": USER_TYPE,
    "nonce": u
    }
    step1 = json.dumps(step1)
    
    url = BASE_URL + AUTH_START
    headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
    response = requests.post(url, data=step1, headers=headers)
    response = json.loads(response.text)
    i = response['nonce']
    e = response['transactionId']
    o = response['rounds']
    a = response['salt']
    bitSalt = base64.b64decode(a)
    
    def getPBKDF2Hash(password, bytedSalt, rounds):
        return hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), bytedSalt, rounds)
    
    r = getPBKDF2Hash(PASSWD,bitSalt,o)
    s = hmac.new(r, "Client Key".encode('utf-8'), hashlib.sha256).digest()
    c = hmac.new(r, "Server Key".encode('utf-8'), hashlib.sha256).digest()
    _ = hashlib.sha256(s).digest()
    d = "n=user,r="+u+",r="+i+",s="+a+",i="+str(o)+",c=biws,r="+i
    g = hmac.new(_, d.encode('utf-8'), hashlib.sha256).digest()
    p = hmac.new(c, d.encode('utf-8'), hashlib.sha256).digest()
    f = bytes(a ^ b for (a, b) in zip(s, g))
    #print ("Authentication - Parameter proof / f", f)
    proof = base64.b64encode(f).decode('utf-8')
    
    step2 = {
    "transactionId": e,
    "proof": proof
    }
    step2 = json.dumps(step2)
    
    url = BASE_URL + AUTH_FINISH
    headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
    response = requests.post(url, data=step2, headers=headers)
    response = json.loads(response.text)
    #print ("my response ist..", response)
    token = response['token']
    signature = response['signature']
    
    y = hmac.new(_, "Session Key".encode('utf-8'), hashlib.sha256)
    y.update(d.encode('utf-8'))
    y.update(s)
    P = y.digest()
    protocol_key = P
    t = os.urandom(16)
    
    e2 = AES.new(protocol_key,AES.MODE_GCM,t)
    e2, authtag = e2.encrypt_and_digest(token.encode('utf-8'))
    
    step3 = {
    "transactionId": e,
    "iv": base64.b64encode(t).decode('utf-8'),
    "tag": base64.b64encode(authtag).decode("utf-8"),
    "payload": base64.b64encode(e2).decode('utf-8')
    }
    step3 = json.dumps(step3)
    
    headers = { 'Content-type': 'application/json', 'Accept': 'application/json' }
    url = BASE_URL + AUTH_CREATE_SESSION
    response = requests.post(url, data=step3, headers=headers)
    response = json.loads(response.text)
    sessionId = response['sessionId']
    
    #create a new header with the new Session-ID for all further requests
    headers = { 'Content-type': 'application/json', 'Accept': 'application/json', 'authorization': "Session " + sessionId }
    url = BASE_URL + ME
    #print ("My ME URL ist ", url)
    response = requests.get(url = url, headers = headers)
    response = json.loads(response.text)
    authOK = response['authenticated']
    #print ("Authenticated ? ")
    #pp.pprint (response)
    if not authOK:
        print("authorization NOT OK")
        sys.exit()
    return headers
    

def LogMeOut( myheader, MybaseURL):
    try:
        LogoutURL = "/auth/logout"
        LogoutURL = MybaseURL + LogoutURL
        myresponse = requests.post(LogoutURL, headers= myheader)
        
    except Exception as ERRonLogout:
        print ("was not able to logout", ERRonLogout)
        sys.exit()
        
#We receive non well formed JSON input and need to massage it ... -START:            
def cleandata(inputstring):
    badcharacters =["'"] 
    for i in badcharacters:
        inputstring = inputstring.replace(i,'"')
        inputstring= inputstring.replace('False','"False"')
        inputstring= inputstring.replace('True','"True"')  
    return inputstring
         
class kostal_writeablesettings (object): 
    def __init__ (self):
        self.KostalwriteableSettings= {}

    # Function to read values from the Inverter
    def readvalue(self, idsArray):
        self.mypayload_moduleID =  '[{"moduleid": "devices:local",'
        self.mypayload_settings = '"settingids"'+':['
        for id in idsArray:
          self.mypayload_settings = self.mypayload_settings + '"'+id+'",'
        self.mypayload_settings = self.mypayload_settings[0:-1]
        self.mypayload_settings = self.mypayload_settings+']}]'
        self.mypayload = self.mypayload_moduleID + self.mypayload_settings
        self.settingsurl = BASE_URL + "/settings"
        try:
            self.response = requests.post(url = self.settingsurl, data = self.mypayload, headers = headers)
            self.JsonResponse = self.response.json()
            self.HtmlReturn = str(self.response)
            self.HtmlOK = "200"
            if (self.HtmlReturn.find(self.HtmlOK)):
                return self.JsonResponse
            else:
                print ("Something went wrong")
                print (self.HtmlReturn)
                return null
        except Exception as Bad:
            print ("Kostal-RESTAPI ran into error", Bad)

    def readtimecontrols(self):
        resultSettings=self.readTimeControlsAsMap()
        stringsPerDay=[]
        for day in WEEKDAYS:
            stringsPerDay=stringsPerDay+[resultSettings[self.getBatteryTimeControlPropertyForDay(day)]]
        return stringsPerDay

    def getBatteryTimeControlPropertyForDay(self, dayNameFromWEEKDAYS):
      return 'Battery:TimeControl:Conf'+dayNameFromWEEKDAYS

    def getBatteryTimeControlPropertyForDayNumber(self, dayNumberStartingWithZero):
      return self.getBatteryTimeControlPropertyForDay(WEEKDAYS[dayNumberStartingWithZero])

    def readTimeControlsAsMap(self):
        settings=[]
        for day in WEEKDAYS:
          settings=settings+[self.getBatteryTimeControlPropertyForDay(day)]
        jsonBatteryTimeControlSettings=mykostalsettings.readvalue(settings)
        resultSettings=jsonBatteryTimeControlSettings[0]['settings']
        result={}
        for resultSetting in resultSettings:
          result[resultSetting['id']]=resultSetting['value']
        return result

    def getUpdatedTimeControls(self, durationFromNowInMinutes, value):
        existingTimeControls = self.readTimeControlsAsMap()
        localizedNow = datetime.now(TZ)
        intervals=max(1, abs(durationFromNowInMinutes//INTERVAL))
        intervalStart=localizedNow
        for i in range(0, intervals):
          weekdayNumber = intervalStart.weekday()
          weekdayPropertyName = self.getBatteryTimeControlPropertyForDayNumber(weekdayNumber)
          startOfDay = datetime(intervalStart.year, intervalStart.month, intervalStart.day, tzinfo=intervalStart.tzinfo)
          durationSinceStartOfDay = intervalStart - startOfDay
          slot = durationSinceStartOfDay // INTERVAL
          existingTimeControls[weekdayPropertyName] = existingTimeControls[weekdayPropertyName][:slot]+str(value)+existingTimeControls[weekdayPropertyName][slot+1:]
          intervalStart = intervalStart + INTERVAL*(durationFromNowInMinutes/abs(durationFromNowInMinutes))
        return existingTimeControls

    # Function to write values to the Inverter  
    def writevalue(self,ID,value):
      self.writevalues({ID: value})

    def writevalues(self,idsToValues):
    
        """
        Supported IDs and Values
        ID                                      Values          
        Battery:DynamicSoc:Enable               0 or 1
        Battery:MinHomeComsumption              50 or higher [Unit = W]
        Battery:MinSoc                          5 up to 100 [Unit in Percent, with 5% steps: e.g. 35]
        """
        
        self.mypayload_settings = '"settings":['
        for id in idsToValues:
            self.mypayload_settings = self.mypayload_settings + '{"value":"'+idsToValues[id]+'","id":"'+id+'"},'
        self.mypayload_settings = self.mypayload_settings[:len(self.mypayload_settings)-1] + ']'
        self.mypayload_moduleID =  '"moduleid": "devices:local",'
        self.mypayload = '[{' + self.mypayload_moduleID + self.mypayload_settings + '}]'
        self.settingsurl = BASE_URL + "/settings"
        try:
            self.response = requests.put(url = self.settingsurl, data = self.mypayload, headers = headers)
            self.HtmlReturn = str(self.response)
            self.HtmlOK = "200"
            if (self.HtmlReturn.find(self.HtmlOK)):
                print ("Successfully changed parameters", file=sys.stderr)
                print(json.dumps(idsToValues))
            else:
                print ("Something went wrong", file=sys.stderr)
                print (self.HtmlReturn, file=sys.stderr)
        except Exception as Bad:
            print ("Kostal-RESTAPI ran into error", Bad)
           
       
    #No input required - once authenticated will query the Events of the Inverter
    #It will return two values:
    #   Status 0 - for success -1 for errors trying to access the  events list
    #   myevents will contain all events that are of type error and active if Status is 0 else myevents will contain error message
    def getEvents(self):
        Status = 0
        self.eventsurl = BASE_URL + "/events/latest"
        myEvents =[]
        try:
            self.response = requests.get(url = self.eventsurl, headers = headers, stream=False)
            self.HtmlReturn = str(self.response)
            self.JsonResponse = self.response.json()
            self.HtmlOK = "200"
            if (self.HtmlReturn.find(self.HtmlOK)):
                print("I am reading events...")
                myresponse = str(self.JsonResponse)
                
                #We receive non well formed JSON input and need to massage it ... -START:
                myresponse = str(self.JsonResponse)
                myresponse = cleandata(myresponse)             
                self.eventsdict = json.loads(myresponse)
                #print (self.eventsdict)
                """
                Here is what we may get (example):
                [   OrderedDict([   ('code', 5014),
                    ('is_active', 'False'),
                    ('end_time', '2020-05-05T08:16:09'),
                    ('start_time', '2020-05-05T08:15:06'),
                    ('group', 'Information'),
                    ('long_description', 'None'),
                    ('description', 'None'),
                    ('category', 'info')]),
                OrderedDict([   ('code', 6260),
                    ('is_active', 'False'),
                    ('end_time', '2019-12-07T07:43:54'),
                    ('start_time', '2019-12-07T07:36:47'),
                    ('group', 'Battery fault'),
                    (   'long_description',
                        'Contact your installer or call our hotline.\n'),
                    (   'description',
                        'Contact your installer or call our hotline.\n'),
                    ('category', 'error')])]                
                """
                LengthDict= len(self.eventsdict)
                i = 0
                if (LengthDict >0):
                    while i < LengthDict:
                        if (("error" in self.eventsdict[i]['category']) and not ("False" in self.eventsdict[i]['is_active'])):
                            print (self.eventsdict[1]['is_active'])
                            myEvents.append(self.eventsdict[i])
                        i = i+1
                else:               #We have no  messages at all coming from the Inverter
                    pass
            else:
                print ("ran into severe error in getEvents routine Error accessing Events")
                print (self.HtmlReturn)
                myEvents.append(self.HtmlReturn)
                Status = -1     
        except Exception as Bad:
            print ("ran into severe error in getEvents routine - message is :", Bad)
            myEvents.append(Bad)
            Status = -1
        return Status, myEvents    
               
        
    #No input required - once authenticated will query the Processdata of the inverter
    #It will return two values:
    #   Status 0 - for success -1 for errors trying to access the Inverter
    #   MyProcessDict will contain a dictionary of  all LiveData that are available
    def getLiveData(self,RequestURL):
        Status = 0
        """
        self.eventsurl = BASE_URL + "/processdata/devices:local"   # The standard stuff
        self.eventsurl = BASE_URL + "/processdata/devices:local:ac"
        self.eventsurl = BASE_URL + "/processdata/devices:local:battery"        #Everything from Battery  
        self.eventsurl = BASE_URL + "/processdata/devices:local:powermeter"     #Everything from Smartmeter
        #self.eventsurl = BASE_URL + "/processdata/devices:scb:event:"          #Get nothing .. 
        self.eventsurl = BASE_URL + "/processdata/scb:statistic:EnergyFlow"     #All the statistics
        #self.eventsurl = BASE_URL + "/processdata/devices:local:pv"            #Get nothing
        #self.eventsurl = BASE_URL + "/settings/devices:local"                  #Get a "not authorized"
        """
        self.LivdataURL = BASE_URL +RequestURL 
       
        MyProcessDict ={}
        try:
            self.response = requests.get(url = self.LivdataURL, headers = headers, stream=False)
            self.HtmlReturn = str(self.response)
            self.JsonResponse = self.response.json()
            self.HtmlOK = "200"
            
            if (self.HtmlReturn.find(self.HtmlOK)):
                myresponse = str(self.JsonResponse)
                myresponse = cleandata(myresponse)             
                self.livedatatdict = json.loads(myresponse)
                LengthDict= len(self.livedatatdict)
                i = 0
                if (LengthDict >0):                  
                    MyProcessdataids = []
                    # FIXME has the output format changed? processdata is a nested array
                    MyProcessdataids.append(self.livedatatdict[0]['processdata'])
                    for elem in MyProcessdataids:
                        for subele in elem:
                            # FIXME and here we don't find sub-element IDs
                            MyProcessDict[subele['id']]= subele["value"]
                else:               #We have no  messages at all coming from the Inverter
                    pass                

        except Exception as Bad:
            print ("ran into severe error in getData routine - message is :", Bad)
            traceback.print_exc()
            MyProcessDict['ERROR']=Bad
            Status = -1
        return Status, MyProcessDict    

if __name__ == "__main__":
    try:
        my_parser = argparse.ArgumentParser()
        my_parser.add_argument('--baseurl', required=True,
                            help='The base URL for your inverter, without the /api/v1 suffix')
        my_parser.add_argument('--password', required=True,
                            help='The password for your "'+USER_TYPE+'"')
        my_parser.add_argument('--nargs', nargs='+',
                            help='You may specify more than one parameter on the commandline e.g.: python Kostal-RESTAPI.py  -StableTime 8 -RunTime 11 -PowerThreshold 3333')
        my_parser.add_argument('-TimeControlEnable',
                            action='store',
                            type = int,
                            choices=[0,1],
                            help='Enable/disable the controlling of battery state by time 0=inactive, 1=active')
        for day in WEEKDAYS:
                my_parser.add_argument('-TimeControlConf'+day,
                            action='store',
                            help='Control battery state for 15min intervals on '+day+'; 0=no limitation; 1=battery charge blocked; 2=battery discharge blocked; example: 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000')
        my_parser.add_argument('-DynamicSoc',
                            action='store',
                            type = int,
                            choices=[0,1],
                            help='Set the Dynamic State of Charge 0=inactive, 1=active')
        my_parser.add_argument('-SetMinSoc',
                            action='store',
                            type = int,
                            choices=[5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95],
                            help='Set your Battery minimum State of Charge - valid values in increments of 5 ranging from 5 to 95')

        my_parser.add_argument('-MinHomeComsumption',
                            action='store',
                            type = int,
                            #choices=[5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95],
                            help='Set your Battery minimum mome consumption [w]. Allowed range : integer values  from 50 through 10000')
                            
        my_parser.add_argument('-SmartBatteryControl',
                            action='store',
                            type = int,
                            choices=[0,1],
                            help='Set your Smart Battery Control . Allowed values 0=off, 1=on')                            

        my_parser.add_argument('-Strategy',
                            action='store',
                            type = int,
                            choices=[1,2],
                            help='Set your Smart Battery Strategy . Allowed values 1=Automatic, 2=Automatic Economic')
                            #This is a little weird. The actual RestAPI allows setting it to 0,1,2 - but I had not seen any difference between 0 and 1
                            
        my_parser.add_argument('-ShadowMgmt',
                            action='store',
                            type = int,
                            choices=[0,1,2,3],
                            help='Sets Shadow Management Parameters : valid values: 0=shadow management disabled, 1= shadowmanagement active for string 1 only, 2= shadowmanagement active for string 2 only, 3= shadow management active for string 1 and 2 ')
        my_parser.add_argument('-ConfigurationFlags',
                            action='store',
                            type = int,
                            choices=[1,2,5,6,9,10,13,14],
                            help='Sets Digital Output Parameters : - -valid values [int] 1 = Self-consumption control, 2= Dynamic Self-consumption control & Function 1, 3&4= Deactivated, 5=Self-consumption control & Function 2, 6=Dynamic Self-consumption control & Function 2, 7,8= Deactivated, 9= Self-consumption control & Function 1 & Other Options Leave Switch Output Activated, 10 = Dynamic Self-consumption control & Function 1 & Other Options Leave Switch Output Activated, 11,12= Deactivated, 13=Self-consumption control & Function 2 & Other Options Leave Switch Output Activated, 14= Dynamic Self-consumption control & Function 2 & Other Options Leave Switch Output Activated ')   
        my_parser.add_argument('-PowerThreshold',
                            action='store',
                            type = int,
                            #choices=range(1,999000),
                            help='Sets the Power Limit [W] of the Switched output for Function 1. Allowed range : integer values from 1 through 999000') 
        my_parser.add_argument('-StableTime',
                           action='store',
                           type = int,
                           #choices=range(1,720),
                           help='Sets the StableTime in [minutes] the inverter must exceed the set "power limit" -see also PowerThreshold  before the consumer is switched on. Allowed range : integer values  from 1 through 720') 
        my_parser.add_argument('-RunTime',
                           action='store',
                           type = int,
                           #choices=range(1,1440),
                           help='Sets the RunTime in [minutes] the inverter switches on the consumer. Allowed range : integer values  from 1 through 1440') 
        my_parser.add_argument('-MaxNoOfSwitchingCyclesPerDay',
                           action='store',
                           type = int,
                           #choices=range(1,999),
                           help='Sets how often the inverter switches on the consumer. Allowed range : integer values  from 1 through 999') 
        my_parser.add_argument('-OnPowerThreshold',
                           action='store',
                           type = int,
                           #choices=range(1,999),
                           help='Sets the Power on Threshold [W] (Activation Limit) for "Function 2". Allowed range : integer values  from 10 through 1000000') 
        my_parser.add_argument('-OffPowerThreshold',
                           action='store',
                           type = int,
                           #choices=range(1,999),
                           help='Sets the Power off Threshold [W] (Deactivation Limit) for "Function 2". Allowed range : integer values  from 10 through 1000000') 

        my_parser.add_argument('-ReadBatteryTimeControl',
                           action='store',
                           type = int,
                           choices=[1],
                           help='Reads battery time control data from inverter: Set to 1 if you would like to...')

        my_parser.add_argument('-SetBatteryTimeControl',
                           action='store',
                           nargs = 2,
                           type = int,
                           help='<minutes> <value>; Sets battery control for the next so many <minutes> to value <value>; value=0 means no limits; value=1 means charging blocked; value=2 means discharging blocked')

        my_parser.add_argument('-SetBatteryTimeControlJson',
                           action='store',
                           nargs = 1,
                           help='Sets battery control based on a JSON document that follows the format of the output of -ReadBatteryTimeControl')

        my_parser.add_argument('-ReadLiveData',
                           action='store',
                           type = int,
                           choices=[1],
                           help='Reads Live Data from Inverter: Set to 1 if you would like to...')                           

        my_parser.add_argument('-ReadACData',
                           action='store',
                           type = int,
                           choices=[1],
                           help='Reads AC Data from Inverter: Set to 1 if you would like to...')  

        my_parser.add_argument('-ReadBatteryData',
                           action='store',
                           type = int,
                           choices=[1],
                           help='Reads Battery Data from Inverter: Set to 1 if you would like to...')  

        my_parser.add_argument('-WriteBatteryDeviceType',
                           action='store',
                           type = int,
                           choices=[0,2,4],
                           help='Writes which Battery Device to activate 0=None, ,2=Piko Battery Li, 4=BYD')  
                           
        my_parser.add_argument('-ReadPowerMeterData',
                           action='store',
                           type = int,
                           choices=[1],
                           help='Reads Power Meter Data from Inverter: Set to 1 if you would like to...')  

        my_parser.add_argument('-ReadStatisticsData',
                           action='store',
                           type = int,
                           choices=[1],
                           help='Reads Statistics Data from Inverter: Set to 1 if you would like to...')  

        my_parser.add_argument('-ReadString1Data',
                           action='store',
                           type = int,
                           choices=[1],
                           help='Reads String 1 Data from Inverter: Set to 1 if you would like to...')  

        my_parser.add_argument('-ReadString2Data',
                           action='store',
                           type = int,
                           choices=[1],
                           help='Reads String 2 Data  from Inverter: Set to 1 if you would like to...')  

        #args = my_parser.parse_args()
        args = vars(my_parser.parse_args())
        BASE_URL = args['baseurl']+"/api/v1"
        PASSWD = args['password']
        #print ("Length of what we have ",len(args))
        CommandlineInput = 0
        for i in args:
            #print ("mein i ist ",i)
            #print ("mein args i ist",args[i]) 
        
            if (str(args[i]) != 'None'):
                #print ("Found", i , args[i])
                CommandlineInput = 1        
                #print ("CommandlineInput is ", CommandlineInput , " So will obeye what was specified on the commandline")
                
            
        #xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
        # COMMANDLINE ONLY
        #xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
        if (CommandlineInput == 1):
            # do commandline stuff only
            headers = LogMeIn(BASE_URL, PASSWD)              
            mykostalsettings = kostal_writeablesettings()
            #
            # All Parameters
            if (str(args['TimeControlEnable']) != 'None'):
                mykostalsettings.KostalwriteableSettings['Battery:TimeControl:Enable'] = args['TimeControlEnable']
                mykostalsettings.writevalue('Battery:TimeControl:Enable',mykostalsettings.KostalwriteableSettings['Battery:TimeControl:Enable'])

            for day in WEEKDAYS:
              if (str(args['TimeControlConf'+day]) != 'None'):
                mykostalsettings.KostalwriteableSettings[mykostalsettings.getBatteryTimeControlPropertyForDay(day)] = args['TimeControlConf'+day]
                mykostalsettings.writevalue(mykostalsettings.getBatteryTimeControlPropertyForDay(day),mykostalsettings.KostalwriteableSettings[mykostalsettings.getBatteryTimeControlPropertyForDay(day)])
            
            if (str(args['DynamicSoc']) != 'None'):
                mykostalsettings.KostalwriteableSettings['Battery:DynamicSoc:Enable'] = args['DynamicSoc']
                mykostalsettings.writevalue('Battery:DynamicSoc:Enable',mykostalsettings.KostalwriteableSettings['Battery:DynamicSoc:Enable'])
            
            if (str(args['SetMinSoc']) != 'None'):
                mykostalsettings.KostalwriteableSettings['Battery:MinSoc'] = args['SetMinSoc']
                mykostalsettings.writevalue('Battery:MinSoc',mykostalsettings.KostalwriteableSettings['Battery:MinSoc'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['Battery:MinSoc'])

            if (str(args['MinHomeComsumption']) != 'None'):
                mykostalsettings.KostalwriteableSettings['Battery:MinHomeComsumption'] = args['MinHomeComsumption']
                mykostalsettings.writevalue('Battery:MinHomeComsumption',mykostalsettings.KostalwriteableSettings['Battery:MinHomeComsumption'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['Battery:MinSoc'])               
                
            if (str(args['SmartBatteryControl']) != 'None'):
                mykostalsettings.KostalwriteableSettings['Battery:SmartBatteryControl:Enable'] = args['SmartBatteryControl']
                mykostalsettings.writevalue('Battery:SmartBatteryControl:Enable',mykostalsettings.KostalwriteableSettings['Battery:SmartBatteryControl:Enable'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['Battery:MinSoc'])                

            if (str(args['Strategy']) != 'None'):
                mykostalsettings.KostalwriteableSettings['Battery:Strategy'] = args['Strategy']
                mykostalsettings.writevalue('Battery:Strategy',mykostalsettings.KostalwriteableSettings['Battery:Strategy'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['Battery:MinSoc']) 
            
            if (str(args['ShadowMgmt']) != 'None'):
                mykostalsettings.KostalwriteableSettings['Generator:ShadowMgmt:Enable'] = args['ShadowMgmt']
                mykostalsettings.writevalue('Generator:ShadowMgmt:Enable',mykostalsettings.KostalwriteableSettings['Generator:ShadowMgmt:Enable'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['Generator:ShadowMgmt:Enable'])                
                        
            if (str(args['ConfigurationFlags']) != 'None'):
                mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:ConfigurationFlags'] = args['ConfigurationFlags']
                mykostalsettings.writevalue('DigitalOutputs:Customer:ConfigurationFlags', mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:ConfigurationFlags'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:ConfigurationFlags'] )             

            if (str(args['PowerThreshold']) != 'None'):
                mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:PowerThreshold'] = args['PowerThreshold']
                mykostalsettings.writevalue('DigitalOutputs:Customer:TimeMode:PowerThreshold',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:PowerThreshold'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:PowerThreshold'] )             

            if (str(args['StableTime']) != 'None'):
                mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:StableTime'] = args['StableTime']
                mykostalsettings.writevalue('DigitalOutputs:Customer:TimeMode:StableTime',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:StableTime'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:StableTime'] )             

            if (str(args['RunTime']) != 'None'):
                mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:RunTime'] = args['RunTime']
                mykostalsettings.writevalue('DigitalOutputs:Customer:TimeMode:RunTime',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:RunTime'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:RunTime'] ) 

            if (str(args['MaxNoOfSwitchingCyclesPerDay']) != 'None'):
                mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:MaxNoOfSwitchingCyclesPerDay'] = args['MaxNoOfSwitchingCyclesPerDay']
                mykostalsettings.writevalue('DigitalOutputs:Customer:TimeMode:MaxNoOfSwitchingCyclesPerDay',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:MaxNoOfSwitchingCyclesPerDay'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:MaxNoOfSwitchingCyclesPerDay'] )                 

            if (str(args['OnPowerThreshold']) != 'None'):
                mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:PowerMode:OnPowerThreshold'] = args['OnPowerThreshold']
                mykostalsettings.writevalue('DigitalOutputs:Customer:PowerMode:OnPowerThreshold',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:PowerMode:OnPowerThreshold'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:PowerMode:OnPowerThreshold'] )         
 
            if (str(args['OffPowerThreshold']) != 'None'):
                mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:PowerMode:OffPowerThreshold'] = args['OffPowerThreshold']
                mykostalsettings.writevalue('DigitalOutputs:Customer:PowerMode:OffPowerThreshold',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:PowerMode:OffPowerThreshold'])
                #print ("I hope I wrote something...",mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:PowerMode:OffPowerThreshold'] )   

            if (str(args['SetBatteryTimeControl']) != 'None'):
                updatedtimecontrols=mykostalsettings.getUpdatedTimeControls(
                            timedelta(minutes=args['SetBatteryTimeControl'][0]),
                            args['SetBatteryTimeControl'][1])
                weekdaysToValues={}
                mykostalsettings.KostalwriteableSettings = updatedtimecontrols
                mykostalsettings.writevalues(mykostalsettings.KostalwriteableSettings)

            if (str(args['SetBatteryTimeControlJson']) != 'None'):
                mykostalsettings.KostalwriteableSettings = json.loads(args['SetBatteryTimeControlJson'][0])
                mykostalsettings.writevalues(mykostalsettings.KostalwriteableSettings)

            if (str(args['ReadBatteryTimeControl']) != 'None'):
                timecontrols=mykostalsettings.readTimeControlsAsMap()
                print(json.dumps(timecontrols))
                #print('['+', '.join('"{0}"'.format(w) for w in timecontrols)+']')

            if (str(args['ReadLiveData']) != 'None'):
                StandardLiveView = "/processdata/devices:local"              # The standard stuff
                MyLiveDataStatus, MyLiveData = mykostalsettings.getLiveData(StandardLiveView)
                pp.pprint(MyLiveData) 

            if (str(args['ReadACData']) != 'None'):
                ACView =  "/processdata/devices:local:ac" 
                MyACDataStatus, MyACLiveData = mykostalsettings.getLiveData(ACView)
                pp.pprint(MyACLiveData)                 

            if (str(args['ReadBatteryData']) != 'None'):
                BatteryView = "/processdata/devices:local:battery"           #Everything from Battery 
                MyBatteryStatus, MyBatteryLiveData = mykostalsettings.getLiveData(BatteryView)
                pp.pprint(MyBatteryLiveData)  
                
            if (str(args['WriteBatteryDeviceType']) != 'None'):                   
                mykostalsettings.KostalwriteableSettings['Battery:Type'] = args['WriteBatteryDeviceType']
                mykostalsettings.writevalue('Battery:Type',mykostalsettings.KostalwriteableSettings['Battery:Type'])
                #print ("I hope I wrote something...", mykostalsettings.KostalwriteableSettings['Battery:Type'] )                

            if (str(args['ReadPowerMeterData']) != 'None'):
                PowerMeterView = "/processdata/devices:local:powermeter"     #Everything from Smartmeter
                MyPowerMeterStatus, MyPowerMeterLiveData = mykostalsettings.getLiveData(PowerMeterView)
                pp.pprint(MyPowerMeterLiveData)                 

            if (str(args['ReadStatisticsData']) != 'None'):
                StatisticsView = "/processdata/scb:statistic:EnergyFlow"     #All the Statistics stuff
                MyStaticsDataStatus, MyStatisticsLiveData = mykostalsettings.getLiveData(StatisticsView)
                pp.pprint(MyStatisticsLiveData)                 

            if (str(args['ReadString1Data']) != 'None'):
                Stringview1 = "/processdata/devices:local:pv1"
                MyString1DataStatus, MyString1LiveData = mykostalsettings.getLiveData(Stringview1)
                pp.pprint(MyString1LiveData)                 

            if (str(args['ReadString2Data']) != 'None'):
                Stringview2 = "/processdata/devices:local:pv2"
                MyString2DataStatus, MyString2LiveData = mykostalsettings.getLiveData(Stringview2)
                pp.pprint(MyString2LiveData) 

            LogMeOut (headers,BASE_URL)
        #xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
        # Example mode of operations..
        #xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx            
        else:           #do normal stuff
            #do normal stuff
            print ("----------------------------------------------------------")    
            print("start log on sequence")
            headers = LogMeIn(BASE_URL, PASSWD)        
            print ("----------------------------------------------------------")        
            mykostalsettings = kostal_writeablesettings()
            
            """
            # This is the current list of tested capabilities - where we change Parameters on the Inverter
            """
            print ("Actively changing Parameters :")
            pp.pprint (mykostalsettings.KostalwriteableSettings.items())                                    #Dictionary initially has no values set
            #Setting Values:
            #DynamicSoc - valid values: 0 (disabled), 1 (enabled)
            mykostalsettings.KostalwriteableSettings['Battery:DynamicSoc:Enable'] = 0
            #MinHomeComsumption - valid values: min 50 [w]
            mykostalsettings.KostalwriteableSettings['Battery:MinHomeComsumption'] = 50
            #MinSoc - valid values: range from 5 to 100, stepsize =5
            myMinSOC= int(10.0)
            mykostalsettings.KostalwriteableSettings['Battery:MinSoc'] = myMinSOC
            #ShadowMgmt - valid values: 0=shadow management disabled, 1= shadowmanagement active for string 1 only, 2= shadowmanagement active for string 2 only, 3= shadow management active for string 1 and 2
            #Values can be set up to value of 7 -but higher values greater than 4 - unclear what they do
            mykostalsettings.KostalwriteableSettings['Generator:ShadowMgmt:Enable'] = 3
            #
            #
            #Parameters for Digital Output - START:
            #
            #DigitalOutputs:Customer:ConfigurationFlags - -valid values [int] 1 = Self-consumption control, 2= Dynamic Self-consumption control & Function 1, 3&4= Deactivated, 5=Self-consumption control & Function 2, 6=Dynamic Self-consumption control & Function 2, 7,8= Deactivated, 9= Self-consumption control & Function 1 & Other Options Leave Switch Output Activated, 10 = Dynamic Self-consumption control & Function 1 & Other Options Leave Switch Output Activated, 11,12= Deactivated, 13=Self-consumption control & Function 2 & Other Options Leave Switch Output Activated, 14= Dynamic Self-consumption control & Function 2 & Other Options Leave Switch Output Activated
            mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:ConfigurationFlags'] = 14
            #
            # For Function 1:
            #'DigitalOutputs:Customer:TimeMode:PowerThreshold' [W]
            mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:PowerThreshold']=1112
            #'DigitalOutputs:Customer:TimeMode:StableTime' [min]
            mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:StableTime']= 4
            #'DigitalOutputs:Customer:TimeMode:RunTime' [min]
            mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:RunTime']= 3
            #'DigitalOutputs:Customer:TimeMode:MaxNoOfSwitchingCyclesPerDay' [int]
            mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:MaxNoOfSwitchingCyclesPerDay']= 2
            # For Function 2: 
            # DigitalOutputs:Customer:PowerMode:OnPowerThreshold - Activation Limit - valid values in [w]
            mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:PowerMode:OnPowerThreshold'] = 1533
            # DigitalOutputs:Customer:PowerMode:OffPowerThreshold -Deactiviation Limit - valid values in [w]
            mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:PowerMode:OffPowerThreshold'] = 1111
            #For Other Options: 
            #DigitalOutputs:Customer:DelayTime -valid values in [s]
            mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:DelayTime']= 11        
            #
            #Parameters for Digital Output - END
            #
            #Applying values:
            mykostalsettings.writevalue('Battery:DynamicSoc:Enable',mykostalsettings.KostalwriteableSettings['Battery:DynamicSoc:Enable'])
            mykostalsettings.writevalue('Battery:MinHomeComsumption',mykostalsettings.KostalwriteableSettings['Battery:MinHomeComsumption'])
            mykostalsettings.writevalue('Battery:MinSoc',mykostalsettings.KostalwriteableSettings['Battery:MinSoc'])
            mykostalsettings.writevalue('Generator:ShadowMgmt:Enable',mykostalsettings.KostalwriteableSettings['Generator:ShadowMgmt:Enable'])
            #
            mykostalsettings.writevalue('DigitalOutputs:Customer:ConfigurationFlags', mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:ConfigurationFlags'])
            mykostalsettings.writevalue('DigitalOutputs:Customer:DelayTime',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:DelayTime'])
            mykostalsettings.writevalue('DigitalOutputs:Customer:TimeMode:PowerThreshold',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:PowerThreshold'])
            mykostalsettings.writevalue('DigitalOutputs:Customer:TimeMode:StableTime',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:StableTime'])
            mykostalsettings.writevalue('DigitalOutputs:Customer:TimeMode:RunTime',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:RunTime'])
            mykostalsettings.writevalue('DigitalOutputs:Customer:TimeMode:MaxNoOfSwitchingCyclesPerDay',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:TimeMode:MaxNoOfSwitchingCyclesPerDay'])
            mykostalsettings.writevalue('DigitalOutputs:Customer:PowerMode:OnPowerThreshold',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:PowerMode:OnPowerThreshold'])
            mykostalsettings.writevalue('DigitalOutputs:Customer:PowerMode:OffPowerThreshold',mykostalsettings.KostalwriteableSettings['DigitalOutputs:Customer:PowerMode:OffPowerThreshold'])        
            #Show the updated values of the dictionary:
            pp.pprint (mykostalsettings.KostalwriteableSettings.items())
            
            #Alternatively, you could directly call the function and pass values too:
            #mykostalsettings.writevalue("Battery:MinSoc", 50)
            """
            # End section where we change parameters
            """
    
            
            
            #This section shows how to pull the Events from the Inverter
            print ("----------------------------------------------------------")        
            print ("Start accessing the event log information")
            MyEventStatus, myCurrentErrorEvents = mykostalsettings.getEvents()
            if ((MyEventStatus == 0) and (len (myCurrentErrorEvents) <1)):
                print("Everything is all right - no active error events from Inverter")
            else:
                print ("We have the following error events")
                pp.pprint(myCurrentErrorEvents)
            if (MyEventStatus == -1):
                print("We have trouble accessing the event information of the inverter")
                pp.pprint(myCurrentErrorEvents)         
            #
                
            #This section shows how to pull various Live Data and Statistics data  from the inverter  
            StandardLiveView = "/processdata/devices:local"              # The standard stuff
            ACView =  "/processdata/devices:local:ac"                               
            BatteryView = "/processdata/devices:local:battery"           #Everything from Battery  
            PowerMeterView = "/processdata/devices:local:powermeter"     #Everything from Smartmeter
            StatisticsView = "/processdata/scb:statistic:EnergyFlow"     #All the Statistics stuff 
            Stringview1 = "/processdata/devices:local:pv1"
            Stringview2 = "/processdata/devices:local:pv2"
            ProberView = "/processdata/devices:prober"                   #Get nothing
            
            #self.eventsurl =  "/processdata/devices:scb:event:"         #Get nothing .. 
            #
            #Just to prove we can loop over the values too, you can change the while loop to run multiple times
            starttime= time.time()
            i = 0
            while (i <1):                                           #Running the query once...
                MyLiveDataStatus, MyLiveData = mykostalsettings.getLiveData(StandardLiveView)
                MyACDataStatus, MyACLiveData = mykostalsettings.getLiveData(ACView)
                MyBatteryStatus, MyBatteryLiveData =  mykostalsettings.getLiveData(BatteryView)
                MyPowerMeterStatus, MyPowerMeterLiveData = mykostalsettings.getLiveData(PowerMeterView)
                MyStaticsDataStatus, MyStatisticsLiveData = mykostalsettings.getLiveData(StatisticsView)
                MyString1DataStatus, MyString1LiveData = mykostalsettings.getLiveData(Stringview1)
                MyString2DataStatus, MyString2LiveData = mykostalsettings.getLiveData(Stringview2)
                ProberDataStataus, ProberViewData = mykostalsettings.getLiveData(ProberView)
                
                print("-----------------------------------------")
                pp.pprint(MyLiveData)
                print("-----------------------------------------")
                pp.pprint(MyACLiveData)
                print("-----------------------------------------")
                pp.pprint(MyBatteryLiveData)
                print("-----------------------------------------")
                pp.pprint(MyPowerMeterLiveData)
                print("-----------------------------------------")
                pp.pprint(MyStatisticsLiveData)
                print("-----------------------------------------")
                pp.pprint(MyString1LiveData)
                pp.pprint(MyString2LiveData)
                #pp.pprint (ProberViewData)  HomeOwn_P
                print ("----------------------------------------------------------")
                print ("DC Power Generation (from String Values)                             :", round((MyString1LiveData['P']+MyString2LiveData['P']),0))    
                print ("A single value from MyLiveData - Home Battery Generation             :", MyLiveData['HomeBat_P'])
                print ("A single value from MyLiveData - Home Power Consumption from Battery :", MyBatteryLiveData['P'])
                print ("A single value from MyLiveData - Home Grid consumption               :", MyLiveData['HomeGrid_P'])
                print ("A single value from MyLiveData - PV Power Consumption                :", MyLiveData['HomePv_P'])
                print ("A single value from MyPowerMeterLiveData - PV Power to /from Grid    :", round(MyPowerMeterLiveData['P']),0)
                print ("A single value from MyBatteryLiveData -Cycle Counts                  :", MyBatteryLiveData['Cycles'])
                print ("A single value from MyBatteryLiveData -'WorkCapacity'                :", round(MyBatteryLiveData['WorkCapacity'],0))
                print ("A single value from MyStatisticsLiveData - Yearly Yield              :", round(MyStatisticsLiveData['Statistic:Yield:Year'],0))
                print ("----------------------------------------------------------")
                
                i = i +1
            endtime = time.time()
            Elapsedtime = endtime-starttime
            print ("My elapsed time was", round(Elapsedtime,1)," seconds - I ran the loop for ", i," time(s)")
            
            LogMeOut (headers,BASE_URL)
       #     print ("I logged out")
    except Exception as Badmain:
        print ("Ran into error executing Main kostal-RESTAPI Routine :", Badmain)

