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
    def __init__(self):
        """Construct an interval for the current time"""
        self(datetime.now(TZ)

    def __init__(self, timepoint):
        """Construct an interval for the timepoint specified, unblocked"""
        self.__init__(self, timepoint, blocked=false, originalState=null)

    def __init__(self, timepoint, blocked, originalState):
        """Construct an interval for the timepoint, blockedness and original state specified"""
        self.timepoint = timepoint
        self.blocked = blocked
        self.originalState = originalState

    def getWeekdayNumber(self):
        return self.timepoint.weekday()

    def getWeekdayPropertyName(self):
        return self.getBatteryTimeControlPropertyForDayNumber(self.getWeekdayNumber())

    def getSlot(self):
        """Returns the 0-based index into the digits string for this interval's day of week"""
        startOfDay = datetime(self.timepoint.year, self.timepoint.month, self.timepoint.day, tzinfo=self.timepoint.tzinfo)
        durationSinceStartOfDay = self.timepoint - startOfDay
        return durationSinceStartOfDay // INTERVAL

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

if __name__ == "__main__":

