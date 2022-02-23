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

# The interval duration at which the inverter can control the battery;
# for the Kostal Plenticore this is 15 minutes
INTERVAL=timedelta(minutes=15)
# The interval at which the wallbox power is polled
WALLBOX_POLLING_INTERVAL=timedelta(minutes=1)
# Pick your time zone:
TZ=timezone('Europe/Berlin')
# The name of the executable for reading and setting values in the Kostal inverter,
# without the need to provide a password, requiring
# the user executing this script to be member of the "kostal" group.
KOSTAL_RESTAPI='kostal-RESTAPI'
WEEKDAYS=['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

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
        return "Interval["+str(self.timepoint)+", start="+str(self.getStart())+", end="+str(self.getEnd())+", blocked="+str(self.blocked)+", originalState="+str(self.originalState)+"]"

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
        """Tells whether the current point in time is at or after the interval's end plus the polling interval"""
        return self.getEnd() + WALLBOX_POLLING_INTERVAL <= datetime.now(TZ)

    def contains(self, aTimePoint):
        """Start is inclusive, end is exclusive"""
        return self.getStart() <= aTimePoint and self.getEnd() > aTimePoint

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
        """Uses the kostal-RESTAPI executable, expected to be in the PATH, to
           obtain the current battery time controls as a map, with the property
           names as returned by getBatteryTimeControlPropertyForDay as the keys
           and strings with digits 0, 1, or 2 for each INTERVAL during that day"""
        return json.loads(subprocess.check_output([KOSTAL_RESTAPI, '-ReadBatteryTimeControl', '1']).decode(sys.stdout.encoding))

    def getBatteryTimeControlPropertyValue(self):
        """Fetches the current values from the inverter and returns the single digit as
           int value for this interval"""
        return self.getBatteryTimeControlPropertyValueForTimePoint(self.getBatteryTimeControlMapFromInverter())

    def setBatteryTimeControlPropertyValue(self, newValue):
        """For this interval updates the digit in the weekday digit string for self.timepoint
           to the newValue in the inverter"""
        subprocess.check_output([KOSTAL_RESTAPI, '-SetBatteryTimeControlJson',
                   json.dumps(self.getUpdatedTimeControls(
                       self.getBatteryTimeControlMapFromInverter(), newValue))]).decode(sys.stdout.encoding)

    def unblock(self):
        print("Unblocking "+str(self))
        self.setBatteryTimeControlPropertyValue(0)
        self.blocked = False

    def block(self):
        print("Blocking "+str(self))
        self.setBatteryTimeControlPropertyValue(2)
        self.blocked = True

    def updateBlockedFromInverter(self):
        self.blocked = self.getBatteryTimeControlPropertyValue() == 2

    def revert(self):
        print("Reverting "+str(self))
        self.setBatteryTimeControlPropertyValue(self.originalState)
        self.blocked = self.originalState == 2

class Json:
    def toMap(self, interval):
        return { 'timepoint': int(interval.timepoint.timestamp()),
                 'blocked': 'true' if interval.blocked else 'false',
                 'originalState': interval.originalState }

    def toJson(self, interval):
        map=self.toMap(interval)
        return json.dumps(map)

    def fromJson(self, intervalJson):
        map=json.loads(intervalJson)
        return self.fromMap(map)

    def fromMap(self, map):
        return Interval(TZ.localize(datetime.fromtimestamp(map['timepoint'])), map['blocked'], map['originalState'])

    def toJsonArray(self, intervals):
        intervalMaps=[]
        for interval in intervals:
            intervalMaps.append(self.toMap(interval))
        return json.dumps(intervalMaps)

    def fromJsonArray(self, intervalsJson):
        intervalMaps=json.loads(intervalsJson)
        intervals=[]
        for intervalMap in intervalMaps:
            intervals.append(self.fromMap(intervalMap))
        return intervals

class Store:
    """Maintains a set of Interval objects and keeps it in sync with a FILE store.
       Intervals can be looked up and created
       based on time points. Block and unblock operations for intervals are provided which will
       keep the persistent state in sync with the set of Interval objects held by this object.
       If an Interval object obtained through this object are modified without going
       through this object's methods, the persistent store will not be updated."""
    FILE='/var/cache/kostal/blocked-intervals.json'

    def __init__(self):
        self.load()

    def __str__(self):
        return ",".join( map( str, self.intervals ) )

    def load(self):
        try:
            with open(Store.FILE, 'r') as infile:
                self.intervals = Json().fromJsonArray(infile.read())
        except IOError:
            print("File "+str(Store.FILE)+" not readable. Starting with empty store.")
            self.intervals = []

    def store(self):
        with open(Store.FILE, 'w') as outfile:
            outfile.write(Json().toJsonArray(self.intervals))

    def revertAndRemoveExpiredIntervals(self):
        remainingIntervals = []
        for interval in self.intervals:
            if interval.isExpired():
                print(""+str(interval)+" has expired; reverting.")
                interval.revert()
            else:
                remainingIntervals.append(interval)
        self.intervals = remainingIntervals
        self.store()

    def getIntervalForTimePoint(self, timepoint):
        for interval in self.intervals:
            if interval.contains(timepoint):
                return interval
        return None

    def getOrCreateIntervalForTimePoint(self, timepoint):
        interval = self.getIntervalForTimePoint(timepoint)
        if interval == None:
            interval = Interval(timepoint)
            print("Didn't find interval for "+str(timepoint)+" in store. Created "+str(interval))
            self.intervals.append(interval)
        return interval

    def block(self, interval):
        interval.block()
        self.store()

    def blockCurrent(self):
        """Obtains the interval for now and now+2*WALLBOX_POLLING_INTERVAL
           and blocks both of them."""
        now = datetime.now(TZ)
        intervalForNow = self.getOrCreateIntervalForTimePoint(now)
        if not intervalForNow.blocked:
            self.block(intervalForNow)
        intervalForNextPoll=self.getOrCreateIntervalForTimePoint(now+2*WALLBOX_POLLING_INTERVAL)
        if intervalForNextPoll != intervalForNow and not intervalForNextPoll.blocked:
            self.block(intervalForNextPoll)

    def revertAndRemoveAllIntervals(self):
        for interval in self.intervals:
            interval.revert()
        self.intervals = []
        self.store()

if __name__ == "__main__":
    if len(sys.argv) <= 1:
        print("Usage: "+sys.argv[0]+""" [ block | revert ]

If the sub-command 'block' is chosen, the current interval, and if it expires in less
than the polling interval """+str(WALLBOX_POLLING_INTERVAL)+""", also the next interval will be blocked; furthermore,
all intervals already expired will be reverted to their original state and will
be removed from the set of known intervals.

If the sub-command 'revert' is chosen, all intervals on record will be reverted to
their original state and will be removed from the set of known intervals.

For the store of managed intervals see """+Store.FILE)
    else:
        store=Store()
        print("Interval Store: "+str(store))
        if sys.argv[1] == "block":
            print("Blocking current and perhaps next interval")
            store.blockCurrent()
            store.revertAndRemoveExpiredIntervals()
        elif sys.argv[1] == "revert":
            print("Reverting all intervals")
            store.revertAndRemoveAllIntervals()
