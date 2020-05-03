import csv
import requests 
import json
from pprint import pprint
import re
import numbers
import requests
# script that reads the locations from the csv, and then outputs all the polylines to a file
with open('alllocations.csv', 'r') as file:
    reader = csv.reader(file)
    counties = ["carlow", "cavan","clare","cork","donegal","dublin","galway","kerry","kildare","kilkenny","laois","leitrim","limerick","longford","louth","mayo","meath","monaghan","offaly","roscommon","sligo","tipperary","waterford","westmeath","wexford","wicklow"]
    count = 0
    file = open("locations.txt","w")
    for row in reader:
        try:
            # print (row)
            count+=1
            # if count > 5:
            #     break
            myString = ','.join(str(x) for x in row)
            myList = re.findall("[-]?\d+\.\d{3,10}", myString)
            myCounty = "null"
            for county in counties:
                if county in myString.lower():
                    myCounty = county
            if myCounty != "null":
                startingPoint = myList[0] +','+myList[1]
                endingPoint = myList[2] +','+myList[3]
                URL = "https://maps.googleapis.com/maps/api/directions/json?origin="+startingPoint+"&destination="+endingPoint+"&key=AIzaSyA0qW44Qlaqs4mDDEqXZdYGeG7pWh97yhw"
                r = requests.get(url = URL) 
                try:
                    polyline = json.dumps(r.text)
                    polyline = json.loads(polyline)
                    finalpolyline = json.dumps(json.loads(polyline)['routes'][0]['overview_polyline']["points"])
                    insertString = 'INSERT into locationInfo(county,polyline) values ("'+myCounty+'",'+finalpolyline+');'
                    file.write(insertString)
                    file.write('\n')
                except:
                    print(row)

        except:
            print("error")
        