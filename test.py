'''
Created on Mar 7, 2021

@author: uhl
'''
from datetime import datetime
from datetime import time
from datetime import timedelta
from pytz import timezone

timezoneName = 'Europe/Berlin'  # to come from Kostal  scb:time/Timezone setting
timepoint = datetime(2021, 3, 7, 23, 20, 0)  # 2021-03-07T10:07:00 without timezone spec ("naive")
tz=timezone(timezoneName)
localizedTimepoint = tz.localize(timepoint)
localizedNow = datetime.now(tz)
print(localizedTimepoint)
print(localizedNow)
fifteenMinutes=timedelta(minutes=15)
print('15min: '+str(fifteenMinutes))
totalDuration=timedelta(hours=4)
intervals=totalDuration//fifteenMinutes
print('Intervals: '+str(intervals))
intervalStart=localizedNow
for i in range(0, intervals):
  propertyId = "Battery:TimeControl:Conf"+intervalStart.strftime("%a")
  startOfDay = datetime(intervalStart.year, intervalStart.month, intervalStart.day, tzinfo=intervalStart.tzinfo)
  durationSinceStartOfDay = intervalStart - startOfDay
  slot = durationSinceStartOfDay // fifteenMinutes
  print('Interval: '+str(intervalStart)+', '+propertyId+', durationSinceStartOfDay '+str(durationSinceStartOfDay)+', slot: '+str(slot))
  intervalStart = intervalStart + fifteenMinutes
# Idea: obtain dayString from property, then update one or more digits and write back
dayString = '000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000'
digit = int(localizedTimepoint.hour*4) + int(localizedTimepoint.minute/15)
print(digit)
newValue = '1'
newDayString = dayString[0:digit]+newValue+dayString[digit+1:]
print(newDayString, len(newDayString))
