'''
Created on Mar 7, 2021

@author: uhl
'''
from datetime import datetime
from pytz import timezone

timezoneName = 'Europe/Berlin'  # to come from Kostal  scb:time/Timezone setting
timepoint = datetime(2021, 3, 7, 23, 20, 0)  # 2021-03-07T10:07:00 without timezone spec ("naive")
tz=timezone(timezoneName)
localizedTimepoint = tz.localize(timepoint)
print(localizedTimepoint)
propertyId = "Battery:TimeControl:Conf"+localizedTimepoint.strftime("%a")
print(propertyId)
# Idea: obtain dayString from property, then update one or more digits and write back
dayString = '000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000'
digit = int(localizedTimepoint.hour*4) + int(localizedTimepoint.minute/15)
print(digit)
newValue = '1'
newDayString = dayString[0:digit]+newValue+dayString[digit+1:]
print(newDayString, len(newDayString))