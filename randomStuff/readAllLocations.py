import csv
import requests 
import json
from pprint import pprint
import re
import numbers
import requests
# script that reads the locations from the csv, and then outputs all the polylines to a file

def getCounty(requestInfo):
    counties = ["carlow", "cavan","clare","cork","donegal","dublin","galway","kerry","kildare","kilkenny","laois","leitrim","limerick","longford","louth","mayo","meath","monaghan","offaly","roscommon","sligo","tipperary","waterford","westmeath","wexford","wicklow"]
                        
    countyEndingLocation = json.dumps(requestInfo.text)
    countyEndingLocation = json.loads(countyEndingLocation)
    countyEndingLocation = json.dumps(json.loads(countyEndingLocation)['results'])
    
    users = json.loads(countyEndingLocation)
    for county in counties:
        for user in users:
            if county in user['formatted_address'].lower():
                return county


with open('/home/alexander11/Documents/Projects/android-app/randomStuff/alllocations.csv', 'r') as file:
    reader = csv.reader(file)
    counties = ["carlow", "cavan","clare","cork","donegal","dublin","galway","kerry","kildare","kilkenny","laois","leitrim","limerick","longford","louth","mayo","meath","monaghan","offaly","roscommon","sligo","tipperary","waterford","westmeath","wexford","wicklow"]
    count = 0
    file = open("sqlinserts","w")
    for row in reader:
        try:
            count+=1
            if count > 5:
                break
            myString = ','.join(str(x) for x in row)
            myList = re.findall("[-]?\d+\.\d{3,10}", myString)
            myCounty = "null"
            for county in counties:
                if county in myString.lower():
                    myCounty = county
            if myCounty != "null":
                startingPoint = myList[0][0:7] +','+myList[1][0:7]
                endingPoint = myList[2][0:7] +','+myList[3][0:7]

                # added this as some speedVan locations start in one county and end in the other county, so they will be added to both counties
                URLcountyStartingLocation="https://maps.googleapis.com/maps/api/geocode/json?address="+startingPoint+"&key=AIzaSyA0qW44Qlaqs4mDDEqXZdYGeG7pWh97yhw"
                finalCountyStartingLocation = getCounty(requests.get(url = URLcountyStartingLocation))
                if(finalCountyStartingLocation is None):
                    finalCountyStartingLocation=myCounty

                URLcountyEndingLocation="https://maps.googleapis.com/maps/api/geocode/json?address="+endingPoint+"&key=AIzaSyA0qW44Qlaqs4mDDEqXZdYGeG7pWh97yhw"
                finalCountyEndingLocation = getCounty(requests.get(url = URLcountyEndingLocation))
                if(finalCountyEndingLocation is None):
                    finalCountyEndingLocation=myCounty

                URL = "https://maps.googleapis.com/maps/api/directions/json?origin="+startingPoint+"&destination="+endingPoint+"&key=AIzaSyA0qW44Qlaqs4mDDEqXZdYGeG7pWh97yhw"
                r = requests.get(url = URL) 
                try:
                    polyline = json.dumps(r.text)
                    polyline = json.loads(polyline)
                    finalpolyline = json.dumps(json.loads(polyline)['routes'][0]['overview_polyline']["points"])
                    if (finalCountyStartingLocation == finalCountyEndingLocation):
                        insertString = 'INSERT into locationInfo(county,polyline) values ("'+finalCountyStartingLocation+'",'+finalpolyline+');'
                        file.write(insertString)
                        file.write('\n')
                    else:
                        insertString = 'INSERT into locationInfo(county,polyline) values ("'+finalCountyStartingLocation+'",'+finalpolyline+');'
                        file.write(insertString)
                        file.write('\n')

                        insertString = 'INSERT into locationInfo(county,polyline) values ("'+finalCountyEndingLocation+'",'+finalpolyline+');'
                        file.write(insertString)
                        file.write('\n')
                except:
                    print(row)
        except:
            print (row)
            print("error")