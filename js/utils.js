MAX_ENTERIES_COUNT = 200;
enteriesArr = Array.apply(undefined,Array(MAX_ENTERIES_COUNT));
nextIndex = 0;

function trace(message) {
    if (typeof message === "string" || message instanceof String) {
        enteriesArr[nextIndex] = message;
        if (++nextIndex >= MAX_ENTERIES_COUNT) {
            nextIndex = 0;
        }
    }
}

__getTrace = function() {

    var outArr = [];

    for (var i = nextIndex ; i < enteriesArr.length ; i++) {
        var msg = enteriesArr[i];
        if (msg != undefined) {
            outArr.push(msg);
            enteriesArr[i] = undefined;
        }
    }

    for (var j = 0 ; j < nextIndex ; j++) {
        var msg = enteriesArr[j];
        if (msg != undefined) {
            outArr.push(msg);
            enteriesArr[j] = undefined;
        }
    }

    nextIndex = 0;
    return outArr;
}

// days between date2 to date1
function daysBetween (date1,date2) {

    var one_day=1000*60*60*24;
    var date1_ms = date1.getTime();
    var date2_ms = date2.getTime();
    var difference_ms = date2_ms - date1_ms;
    return Math.round(difference_ms/one_day);
}

//
function roundEventDateByDay(event) {
	var d = new Date(event.dateTime);
    var eventDay = new Date(Date.UTC(d.getFullYear(),d.getMonth(),d.getDate())).toJSON();
    return eventDay;
}

function countByDate(event,keyToCount,dateTable) {

    var eventDay = roundEventDateByDay(event);

    if (dateTable[eventDay] === undefined) {
		dateTable[eventDay] = {};
    }

    var dayTable = cache.dateTable[eventDay];

    if (dayTable[keyToCount] === undefined) {
		dayTable[keyToCount] = 1;
	} else {
		dayTable[keyToCount] += 1;
	}

    dateTable[eventDay] = dayTable;
}

function sumAndCleanLastDays(lastDaysToSum,dateTable) {

    var now = new Date();
    var daysArr = Object.keys(dateTable);
    var daysToRemove = [];
    var output = {};

    for (var day in daysArr) {
        var oneDay = daysArr[day];
        if (daysBetween(new Date(oneDay),now) < lastDaysToSum) {
            var oneDayVideoSum = dateTable[oneDay];
            var oneDayVideoSumArr = Object.keys(oneDayVideoSum);
            for (var c in oneDayVideoSumArr) {
                var cat = oneDayVideoSumArr[c];

                if (output[cat] === undefined) {
                    output[cat] = oneDayVideoSum[cat];
                } else {
                    output[cat] += oneDayVideoSum[cat];
                }
            }
        } else {
            daysToRemove.push(oneDay);
        }
    }

    for (var i in daysToRemove) {
        delete dateTable[daysToRemove[i]];
    }

    return output;
}

//reset the stream after x days from start date
function resetStreamAfterDays(startDate,resetAfterDaysNum) {

    var now = new Date();
    var daysDelta = daysBetween(startDate,now);

    trace("Reset Days After:" + resetAfterDaysNum  + "Days delta:" + daysDelta);

    if (daysDelta >= resetAfterDaysNum) {
        trace("reset stream");
        result = {};
        cache  = {};
    }
}

// create the buckets
function createBucketPerSessions(buckets, allEvents){
    trace("createBucketPerSessions");
    for (var e in allEvents) {
        try{
            if (allEvents[e].name === "app-launch"){
                trace("event app launch");
                buckets.push({
                    date: allEvents[e].dateTime,
                    eventcount: 0
                });
            }
        }
        catch(err){
            trace(err.name + ":" + err.message);
        }
    }
    return buckets
}

// sort the events after the buckets
function sortEventsInBuckets(sessionBuckets, allEvents, eventName, eventDataName)
{
    trace("sortEventsInBuckets");
    var sortedEvents = sessionBuckets;
    for (var i in allEvents) {
        try{
            currentEventName = allEvents[i].name;
            try{
                if (allEvents[i].eventData.name === undefined){
                    currentEventDataName = null;
                }else{
                    currentEventDataName = allEvents[i].eventData.name;
                }
            }catch(err){
                trace("missing eventData.Name :" + err.name + ":" + err.message);
                currentEventDataName = null;
            }
            if ((eventName === null || eventName === undefined || currentEventName === eventName) &&
                (currentEventDataName === null || eventDataName === null || eventDataName === undefined
                || currentEventDataName === eventDataName)){
                trace(eventName + " : " + eventDataName);
                eventDate = allEvents[i].dateTime;
                for (var k = sortedEvents.length -1 ; k >= 0; k--){
                   if (sortedEvents[k].date < eventDate){
                       trace("add to bucket number " + i);
                       sortedEvents[k].eventcount = sortedEvents[k].eventcount +1;
                       break;
                  }
              }
            }
        }
        catch(err){
            trace(err.name + ":" + err.message);
        }
    }
    return sortedEvents;
}

// cleanup
function removeExtraBuckets(bucketArray, maxBuckets){
    trace("removeExtraBuckets: bucketArray.length = " + bucketArray.length + " maxBucket = " + maxBuckets);
    var extraSessions = bucketArray.length - maxBuckets;
    if (extraSessions > 0){
        bucketArray.splice(0, extraSessions);
    }
    trace("after splice sessions.length = " + bucketArray.length);
    return bucketArray;
}

//count the events
function countEventsInBuckets (buckets){
trace("countEventsInBuckets");
    var counter = 0;
    for (var j in buckets){
        if (buckets[j] !== undefined ){
            trace("events in session " + j + " = " + buckets[j].eventcount);
            counter = counter +  buckets[j].eventcount;
        }
        else{
            trace("buckets[j] = undefined");
        }
    }
    trace("eventCount = " + counter);
    return counter;
}


MAX_ENTERIES_COUNT = 200;
enteriesArr = Array.apply(undefined,Array(MAX_ENTERIES_COUNT));
nextIndex = 0;

function trace(message) {
    if (typeof message === "string" || message instanceof String) {
        enteriesArr[nextIndex] = message;
        if (++nextIndex >= MAX_ENTERIES_COUNT) {
            nextIndex = 0;
        }
    }
}

__getTrace = function() {

    var outArr = [];

    for (var i = nextIndex ; i < enteriesArr.length ; i++) {
        var msg = enteriesArr[i];
        if (msg != undefined) {
            outArr.push(msg);
            enteriesArr[i] = undefined;
        }
    }

    for (var j = 0 ; j < nextIndex ; j++) {
        var msg = enteriesArr[j];
        if (msg != undefined) {
            outArr.push(msg);
            enteriesArr[j] = undefined;
        }
    }

    nextIndex = 0;
    return outArr;
}

var weekday = ["Sunday","Monday","Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
function getDayOfWeek(timeStamp){
     date = new Date(timeStamp);
     return weekday[date.getDay()];
}

function getDateFromTimeStamp(timeStamp){
    date = new Date(timeStamp);
    var year = date.getFullYear();
    var month = date.getMonth();
    var day = date.getDate();
    return day + "." + month + "." + year;
}

function daysBetween (timeStamp1, timeStamp2){
     oneDay = 60*60*1000*24;
     try{
        return Math.floor(Math.abs(timeStamp2-timeStamp1)/oneDay) +1;
     }
    catch(err){
        return 1;
        //trace("cant calculate days between" + err.name + ":" + err.message);
    }
}

// needed for array sort of numbers (used in ML streams)
function sortNumber(a,b) {
    return a - b;
}
//
function roundEventDateByDay(event) {
	var d = new Date(event.dateTime);
    var eventDay = new Date(Date.UTC(d.getFullYear(),d.getMonth(),d.getDate())).toJSON();
    return eventDay;
}

function countByDate(event,keyToCount,dateTable) {

    var eventDay = roundEventDateByDay(event);

    if (dateTable[eventDay] === undefined) {
		dateTable[eventDay] = {};
    }

    var dayTable = cache.dateTable[eventDay];

    if (dayTable[keyToCount] === undefined) {
		dayTable[keyToCount] = 1;
	} else {
		dayTable[keyToCount] += 1;
	}

    dateTable[eventDay] = dayTable;
}

function sumAndCleanLastDays(lastDaysToSum,dateTable) {

    var now = new Date();
    var daysArr = Object.keys(dateTable);
    var daysToRemove = [];
    var output = {};

    for (var day in daysArr) {
        var oneDay = daysArr[day];
        if (daysBetween(new Date(oneDay),now) < lastDaysToSum) {
            var oneDayVideoSum = dateTable[oneDay];
            var oneDayVideoSumArr = Object.keys(oneDayVideoSum);
            for (var c in oneDayVideoSumArr) {
                var cat = oneDayVideoSumArr[c];

                if (output[cat] === undefined) {
                    output[cat] = oneDayVideoSum[cat];
                } else {
                    output[cat] += oneDayVideoSum[cat];
                }
            }
        } else {
            daysToRemove.push(oneDay);
        }
    }

    for (var i in daysToRemove) {
        delete dateTable[dayToRemove[i]];
    }

    return output;
}

//reset the stream after x days from start date
function resetStreamAfterDays(startDate,resetAfterDaysNum) {

    var now = new Date();
    var daysDelta = daysBetween(startDate,now);

   // trace("Reset Days After:" + resetAfterDaysNum  + "Days delta:" + daysDelta);

    if (daysDelta >= resetAfterDaysNum) {
      //  trace("reset stream");
        result = {};
        cache  = {};
    }
}

// create the buckets
function createBucketPerSessions(buckets, allEvents){
   // trace("createBucketPerSessions");
    for (var e in allEvents) {
        try{
            if (allEvents[e].name === "app-launch"){
               // trace("event app launch");
                buckets.push({
                    date: allEvents[e].dateTime,
                    eventcount: 0
                });
            }
        }
        catch(err){
         //   trace(err.name + ":" + err.message);
        }
    }
    return buckets
}

// sort the events after the buckets
function sortEventsInBuckets(sessionBuckets, allEvents, eventName, eventDataName)
{
   // trace("sortEventsInBuckets");
    var sortedEvents = sessionBuckets;
    for (var i in allEvents) {
        try{
            currentEventName = allEvents[i].name;
            try{
                if (allEvents[i].eventData.name === undefined){
                    currentEventDataName = null;
                }else{
                    currentEventDataName = allEvents[i].eventData.name;
                }
            }catch(err){
               // trace("missing eventData.Name :" + err.name + ":" + err.message);
                currentEventDataName = null;
            }
            if ((eventName === null || eventName === undefined || currentEventName === eventName) &&
                (currentEventDataName === null || eventDataName === null || eventDataName === undefined
                || currentEventDataName === eventDataName)){
               // trace(eventName + " : " + eventDataName);
                eventDate = allEvents[i].dateTime;
                for (var k = sortedEvents.length -1 ; k >= 0; k--){
                   if (sortedEvents[k].date < eventDate){
                      // trace("add to bucket number " + i);
                       sortedEvents[k].eventcount = sortedEvents[k].eventcount +1;
                       break;
                  }
              }
            }
        }
        catch(err){
           // trace(err.name + ":" + err.message);
        }
    }
    return sortedEvents;
}

// cleanup
function removeExtraBuckets(bucketArray, maxBuckets){
  //  trace("removeExtraBuckets: bucketArray.length = " + bucketArray.length + " maxBucket = " + maxBuckets);
    var extraSessions = bucketArray.length - maxBuckets;
    if (extraSessions > 0){
        bucketArray.splice(0, extraSessions);
    }
  //  trace("after splice sessions.length = " + bucketArray.length);
    return bucketArray;
}

//count the events
function countEventsInBuckets (buckets){
//trace("countEventsInBuckets");
    var counter = 0;
    for (var j in buckets){
        if (buckets[j] !== undefined ){
         //   trace("events in session " + j + " = " + buckets[j].eventcount);
            counter = counter +  buckets[j].eventcount;
        }
        else{
           // trace("buckets[j] = undefined");
        }
    }
   // trace("eventCount = " + counter);
    return counter;
}

function streamMedian(value, medianObj) {
    r = medianObj.r; l = medianObj.l;
    lenR = r.length; lenL = l.length;
    median = lenL > lenR ? l[0] : median = lenR > lenL ? r[0] : Math.round((r[0] + l[0]) / 2);
    if (lenR === 0 & lenL ===0 ) {r[0] = value; lenR++}
    else {
        if (value < median) {
            for (i = 0; i < lenL; i++) if (value > l[i]) break;
            l.splice(i, 0, value);
            lenL++;
        }
        else if (value >= median) {
            for (i = 0; i < lenR; i++) if (value < r[i]) break;
            r.splice(i, 0, value);
            lenR++;
        }
    }
    if (lenL > lenR) {median = l[0]; if (lenL - lenR > 1) r.unshift(l.splice(0, 1)[0]);}
    else if(lenR > lenL) {median = r[0]; if (lenR - lenL > 1) l.unshift(r.splice(0, 1)[0]);}
    else median = Math.round((r[0] + l[0]) / 2);
    if (false) {medianObj.l.pop(); medianObj.r.pop();}
    medianObj.median = median;
    return medianObj;
}
function add(a, b) {
    return a + b;
}
function arrayToJson(array){
    var thisEleObj = {};
    if(typeof array == "object"){
        for(var i in array){
            thisEleObj[i] = arrayToJson(array[i]);
        }
    }else {
        thisEleObj = array;
    }
    return thisEleObj;
}

function uniqueDates(data) {
    canonicalize = function (x) {
        return x;
    }
    var lookup = {};

    return data.filter(function (date) {
        var serialised = canonicalize(new Date(date).setHours(0, 0, 0, 0));

        if (lookup.hasOwnProperty(serialised)) {
            return false;
        } else {
            lookup[serialised] = true;
            return true;
        }
    })
}
function streamMedian(value, medianObj) {
    r = medianObj.r; l = medianObj.l;
    lenR = r.length; lenL = l.length;
    median = lenL > lenR ? l[0] : median = lenR > lenL ? r[0] : Math.round((r[0] + l[0]) / 2);
    if (lenR === 0 & lenL ===0 ) {r[0] = value; lenR++}
    else {
        if (value < median) {
            for (i = 0; i < lenL; i++) if (value > l[i]) break;
            l.splice(i, 0, value);
            lenL++;
        }
        else if (value >= median) {
            for (i = 0; i < lenR; i++) if (value < r[i]) break;
            r.splice(i, 0, value);
            lenR++;
        }
    }
    if (lenL > lenR) {median = l[0]; if (lenL - lenR > 1) r.unshift(l.splice(0, 1)[0]);}
    else if(lenR > lenL) {median = r[0]; if (lenR - lenL > 1) l.unshift(r.splice(0, 1)[0]);}
    else median = Math.round((r[0] + l[0]) / 2);
    if (false) {medianObj.l.pop(); medianObj.r.pop();}
    medianObj.median = median;
    return medianObj;
}
function add(a, b) {
    return a + b;
}
function arrayToJson(array){
    var thisEleObj = {};
    if(typeof array == "object"){
        for(var i in array){
            thisEleObj[i] = arrayToJson(array[i]);
        }
    }else {
        thisEleObj = array;
    }
    return thisEleObj;
}
function dropDuplicateEvents(events, milisec){
    lastEvent = {};
    for (i = 0; i <= events.length-1; i++) {
        if (events[i].name === lastEvent.name &&
            Math.abs(events[i].dateTime - lastEvent.dateTime) < milisec &&
            events[i].eventData !== undefined && lastEvent.eventData !== undefined &&
            events[i].eventData.name === lastEvent.eventData.name
        ) events.splice(i, 1);
        else lastEvent = events[i]
    }
    return events
}
function uniqueDates(data) {
    canonicalize = function (x) {
        return x;
    };
    var lookup = {};

    return data.filter(function (date) {
        var serialised = canonicalize(new Date(date).setHours(0, 0, 0, 0));

        if (lookup.hasOwnProperty(serialised)) {
            return false;
        } else {
            lookup[serialised] = true;
            return true;
        }
    })
}
