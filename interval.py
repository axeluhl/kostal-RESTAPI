#!/usr/bin/env python3

import sys
import string
import subprocess
import json
import time
import argparse
import traceback
from datetime import timedelta
from datetime import datetime
from pytz import timezone
from Cryptodome.Cipher import AES  #windows

WEEKDAYS=['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
INTERVAL=timedelta(minutes=15)
TZ=timezone('Europe/Berlin')
# The name of the executable for reading and setting values in the Kostal inverter,
# without the need to provide a password, requiring
# the user executing this script to be member of the "kostal" group.
KOSTAL_RESTAPI='kostal-RESTAPI'

class Interval:
    def __init__(self, timepoint=datetime.now(TZ), blocked=None, originalState=None):
        """Construct an interval for the timepoint, blockedness and original state specified;
           The originalState can be 0, 1, 2, or null if not known.
           self.blocked is a boolean value."""
        self.timepoint = timepoint
        if self.timepoint != None and originalState == None:
            self.originalState = self.getBatteryTimeControlPropertyValue()
        else:
            self.originalState = originalState
        if blocked == None:
            self.blocked = True if self.originalState == 2 else False
        else:
            self.blocked = blocked

    def __str__(self):
        return "Interval["+str(self.timepoint)+", blocked="+str(self.blocked)+", originalState="+str(self.originalState)+"]"

    def getWeekdayNumber(self):
        return self.timepoint.weekday()

    def getWeekdayPropertyName(self):
        return self.getBatteryTimeControlPropertyForDayNumber(self.getWeekdayNumber())

    def getStartOfDay(self):
        return datetime(self.timepoint.year, self.timepoint.month, self.timepoint.day, tzinfo=self.timepoint.tzinfo)

    def getDurationSinceStartOfDay(self):
        return self.timepoint - self.getStartOfDay()

    def getSlot(self):
        """Returns the 0-based index into the digits string for this interval's day of week"""
        return self.getDurationSinceStartOfDay() // INTERVAL

    def getStart(self):
        """Returns the start time point of this interval"""
        return self.getStartOfDay() + INTERVAL * self.getSlot()

    def getEnd(self):
        """Returns the start time point of this interval"""
        return self.getStartOfDay() + INTERVAL * (self.getSlot() + 1)

    def isExpired(self):
        """Tells whether getEnd() is after the current point in time"""
        return self.getEnd() < datetime.now(TZ)

    def readTimeControlsAsMap(self):
        """Uses the kostal-RESTAPI executable, expected to be in the PATH, to
           obtain the current battery time controls as a map, with the property
           names as returned by getBatteryTimeControlPropertyForDay as the keys
           and strings with digits 0, 1, or 2 for each INTERVAL during that day"""
        return json.loads(subprocess.check_output(['kostal-RESTAPI', '-ReadBatteryTimeControl', '1'])
                   .decode(sys.stdout.encoding))

    def getBatteryTimeControlPropertyName(self):
        """Obtains the property name for the battery time control map for the day of self.timepoint"""
        weekdayNumber = self.timepoint.weekday()
        return self.getBatteryTimeControlPropertyForDayNumber(weekdayNumber)

    def getBatteryTimeControlPropertyForDayNumber(self, dayNumberStartingWithZero):
        return self.getBatteryTimeControlPropertyForDay(WEEKDAYS[dayNumberStartingWithZero])

    def getBatteryTimeControlPropertyForDay(self, dayNameFromWEEKDAYS):
        return 'Battery:TimeControl:Conf'+dayNameFromWEEKDAYS

    def getBatteryTimeControlPropertyValueForDayOfTimePoint(self, batteryTimeControlValuesAsMap):
        return batteryTimeControlValuesAsMap[self.getBatteryTimeControlPropertyName()]

    def getBatteryTimeControlPropertyValueForTimePoint(self, batteryTimeControlValuesAsMap):
        digitsStringForDay = self.getBatteryTimeControlPropertyValueForDayOfTimePoint(batteryTimeControlValuesAsMap)
        slot = self.getSlot()
        return int(digitsStringForDay[slot])

    def getUpdatedTimeControls(self, batteryTimeControlValuesAsMap, newValue):
        weekdayPropertyName = self.getBatteryTimeControlPropertyName()
        slot = self.getSlot()
        batteryTimeControlValuesAsMap[weekdayPropertyName] = batteryTimeControlValuesAsMap[weekdayPropertyName][:slot] + \
                                                             str(newValue) + \
                                                             batteryTimeControlValuesAsMap[weekdayPropertyName][slot+1:]
        return batteryTimeControlValuesAsMap

    def getBatteryTimeControlMapFromInverter(self):
        return json.loads(subprocess.check_output([KOSTAL_RESTAPI, '-ReadBatteryTimeControl', '1']).decode(sys.stdout.encoding))

    def getBatteryTimeControlPropertyValue(self):
        """Fetches the current values from the inverter and returns the single digit as
           int value for this interval"""
        return self.getBatteryTimeControlPropertyValueForTimePoint(self.getBatteryTimeControlMapFromInverter())

    def setBatteryTimeControlPropertyValue(self, newValue):
        """For this interval updates the digit in the weekday digit string for self.timepoint
           to the newValue in the inverter"""
        return json.loads(subprocess.check_output([KOSTAL_RESTAPI, '-SetBatteryTimeControlJson',
                   json.dumps(self.getUpdatedTimeControls(
                       self.getBatteryTimeControlMapFromInverter(), newValue))]).decode(sys.stdout.encoding))

    def unblock(self):
        self.setBatteryTimeControlPropertyValue(0)
        self.blocked = False

    def block(self):
        self.setBatteryTimeControlPropertyValue(2)
        self.blocked = True

    def updateBlockedFromInverter(self):
        self.blocked = self.getBatteryTimeControlPropertyValue() == 2

    def reset(self):
        self.setBatteryTimeControlPropertyValue(self.originalState)
        self.blocked = self.originalState == 2

class Json:
  def toJson(self, interval):
    map={ 'timepoint': int(interval.timepoint.timestamp()),
          'blocked': 'true' if interval.blocked else 'false',
          'originalState': interval.originalState }
    return json.dumps(map)
    
  def fromJson(self, intervalJson):
    map=json.loads(intervalJson)
    return Interval(datetime.fromtimestamp(map['timepoint']), map['blocked'], map['originalState'])
