#!/usr/bin/env python3

import sys
import string
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

class Interval:
    def __init__(self, timepoint=datetime.now(TZ), blocked=False, originalState=None):
        """Construct an interval for the timepoint, blockedness and original state specified;
           The originalState can be 0, 1, 2, or null if not known.
           self.blocked is a boolean value."""
        self.timepoint = timepoint
        self.blocked = blocked
        self.originalState = originalState

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

    def getBatteryTimeControlPropertyForDay(self, dayNameFromWEEKDAYS):
        return 'Battery:TimeControl:Conf'+dayNameFromWEEKDAYS

    def getBatteryTimeControlPropertyForDayNumber(self, dayNumberStartingWithZero):
        return self.getBatteryTimeControlPropertyForDay(WEEKDAYS[dayNumberStartingWithZero])

class Json:
  def toJson(self, interval):
    map={ 'timepoint': int(interval.timepoint.timestamp()),
          'blocked': 'true' if interval.blocked else 'false',
          'originalState': interval.originalState }
    return json.dumps(map)
    
  def fromJson(self, intervalJson):
    map=json.loads(intervalJson)
    return Interval(datetime.fromtimestamp(map['timepoint']), map['blocked'], map['originalState'])
