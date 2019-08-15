var maxIndex;
var moduleNames = ['allergy', 'daily', 'hourly', 'maps', 'news', 'run', 'video'];
var actions = ['cl', 'vi'];
var tileNames = [];
var allTiles = [
    'radar map'
    ,'radar small'
    ,'precip start large'
    ,'precip end large'
    ,'precip start small'
    ,'precip end small'
    ,'tomorrow\'s forecast large'
    ,'weekend forecast large'
    ,'daily forecast large'
    ,'hourly forecast large'
    ,'breaking news video small'
    ,'breaking news video large'
    ,'lightning small'
    ,'t-storm now small'
    ,'winter storm now small'
    ,'snow accumulation small'
    ,'winter storm forecast small'
    ,'t-storm soon small'
    ,'precip intensity'
    ,'cold and flu small'
    ,'gorun small'
    ,'feels like small'
    ,'boat and beach small'
    ,'tomorrow\'s forecast small'
    ,'weekend forecast small'
    ,'sunset\/sunrise small'
    ,'hourly forecast small'
    ,'daily forecast small'
    ,'top video small'
    ,'top video large'
    ,'severe alerts table'
    ,'editorial calendar large'
    ,'alert small'
    ,'alert large'
    ,'road conditions small'
    ,'radar extra large'
    ,'video extra large'
];
//Load Cache
var windows;
var sessions;
if (cache === undefined || cache.sessions === undefined) sessions = [];
else sessions = fromCache(cache.sessions);
if (cache.system === undefined) cacheFreeSize = 1024;
else cacheFreeSize = cache.system.cacheFreeSize;
//Add new events, query and generate results
sessions = eventsBySession(dropDuplicateEvents(events, 2 * 1000));
result = querySessions();
result = arrayToJson(result); //Stringify
if (cacheFreeSize < 800) cleanCache(); // Remove first element in arrays
cache.sessions = arrayToJson(sessions); //Stringify for Cache

function cleanCache() {
    sessions.launch.splice(0, 1);
    actions.forEach(function (action) {
        moduleNames.forEach(function (module) {
            sessions.modules[module][action].splice(0, 1);
        });
        tileNames.forEach(function (module) {
            sessions.tiles[module][action].splice(0, 1);
        });
    });
}

//Read from JSON
function fromCache(s) {
    try {
        // Read JSON, Arrays as objects
        obj = s;
        //Convert legacy Cache
        if (s !== undefined && s.launch === undefined) {
            obj.launch = Object.values(s.dateTime);
            obj.duration = [];
            for (i = 0; i <= obj.launch.length; i++) obj.duration.push(0)
        }
        else{
            obj.launch = Object.values(s.launch);
            obj.duration = Object.values(s.duration);
        }
        //Convert Arrays
        ["l", "r"].forEach(function (value) {
            obj.mtbs[value] = Object.values(s.mtbs[value])
        });
        for (key in s.tiles) tileNames.push(key);

        //Modules
        moduleNames.forEach(function (module) {
            if (obj[module] === undefined) {
                actions.forEach(function (action) {
                    obj.modules[module][action] = Object.values(s.modules[module][action]);
                });
            }
            else {            //Convert legacy Cache
                obj.modules[module] = [];
                if (obj[module].clicked !== undefined){
                    obj.modules[module]['cl'] = Object.values(s[module]['clicked']);
                    obj.modules[module]['vi'] = Object.values(s[module]['viewed']);
                }
                else {
                    obj.modules[module]['cl'] = Object.values(s[module]['cl']);
                    obj.modules[module]['vi'] = Object.values(s[module]['vi']);
                }
                delete obj[module];
                trace("Fallback: Reset");
            }
        });
        //Tiles
        tileNames.forEach(function (tile) {
            actions.forEach(function (action) {
                obj.tiles[tile][action] = Object.values(s.tiles[tile][action]);
            });
        });
        return obj;
    }catch (err) {
        trace("Error Loading Cache:" + err.name + ":" + err.message);
        trace("Fallback: Reset");
    }
}
function addTile(name) {
    tileNames.push(name);
    sessions.tiles[name] = [];
    actions.forEach(function (action) {
        sessions.tiles[name][action] = [];
        for (i = 0; i <= sessions.launch.length -1; i++) {
            sessions.tiles[name][action].push(0);
        }
    });
}

function eventsBySession(allEvents) {
    //Add missing arrays
    if (sessions.launch === undefined) {sessions.launch = []; sessions.duration = []; sessions.modules =[]; sessions.tiles = []}
    moduleNames.forEach(function (module) {
        if (sessions.modules[module] === undefined ) {
            sessions.modules[module] = [];
            actions.forEach(function (action) {
                sessions.modules[module][action] = new Array(sessions.launch.length);
            });
        }
    });
    if (sessions.mtbs === undefined) sessions.mtbs = {l: [],r: [],median: -99};
    if (sessions.totalLaunches === undefined) sessions.totalLaunches = 0;
    i = 0;
    var tc = false;
    allEvents.forEach(function (event) {
        try {
            currentET = event.name.toLowerCase();
            maxIndex = sessions.launch.length - 1;
            if (event.name === "app-launch") {
                if (maxIndex > 0) {
                    timeSinceLast = Math.round((event.dateTime - sessions.launch[maxIndex]) / (60 * 1000));
                    if (timeSinceLast > 0) { // more than 30 sec ago
                        sessions.totalLaunches++;
                        sessions.mtbs = streamMedian(timeSinceLast, sessions.mtbs);
                    }
                }
                else sessions.totalLaunches++;
                i++;
                //Modules
                moduleNames.forEach(function (module) {
                    actions.forEach(function (action) {
                        sessions.modules[module][action].push(0)
                    });
                });
                //Tiles
                tileNames.forEach(function (tile) {
                    actions.forEach(function (action) {
                        sessions.tiles[tile][action].push(0)
                    });
                });
                sessions.launch.push(event.dateTime);
                sessions.duration.push(0)
            }
            else if (i > 0) {
                if (event.eventData !== undefined &&
                    event.eventData.name !== undefined ) {
                    dName = event.eventData.name.toLowerCase().replace("go-run", "run");
                    if (currentET === "detail-viewed") {//has module been clicked?
                        if (tc) tc = false;
                        else {currentInfo = sessions.modules[dName].cl[maxIndex]++};
                    }
                    else if (currentET === "module-viewed" && moduleNames.indexOf(dName) != -1) {//Has module been viewed?
                        sessions.modules[dName.replace("lifestyle", "allergy")].vi[maxIndex]++;
                        if (dName === 'lifestyle') sessions.modules['run'].vi[maxIndex]++; //missing
                    }
                    else if (currentET === "tile-clicked") {//has module been clicked?
                        if (tileNames.indexOf(dName) === -1) addTile(dName);
                        currentInfo = sessions.tiles[dName].cl[maxIndex]++;
                        tc = true;
                    }
                    else if (currentET === "tile-viewed") {//Has tile been viewed?
                        if (tileNames.indexOf(dName) === -1) addTile(dName);
                        sessions.tiles[dName].vi[maxIndex]++;
                    }
                }
                else if (event.name === "app-exit") {
                    sessions.duration[maxIndex] = event.dateTime - sessions.launch[maxIndex]; // duration in millisec
                    i--;
                    tc = false;
                }
            }
        } catch (err) { trace("eventsBySession:" + err.name + ":" + err.message);}
    });
    return sessions;
}

function sinceLast(action, module) {
    //hours & sessions since
    for (i = maxIndex; i >= 0; i--) {
        if (sessions.modules[module] !== undefined && sessions.modules[module][action][i] !== 0) {
            var index = i;
            break;
        }
    }
    rS = result.since[module];
    if (sessions.modules[module][action][maxIndex] <= 0) {
        if (index !== undefined) {
            rS[action + "Time"] = Math.round(Math.abs((
                sessions.launch[maxIndex] - sessions.launch[index]) / (60 * 1000)));
            rS[action + "Sess"] = maxIndex - index + 1;
        }
    } else {
        rS[action + "Time"] = 0;
        rS[action + "Sess"] = 1;
    }
    return result;
}

function querySessions() {
    try {
        var result = prepareResultArray();
        maxIndex = sessions.launch.length - 1;
        if (sessions.launch[maxIndex] !== undefined) {
            result.sessions = sessions.totalLaunches;
            result.dt = new Date().getTime();
            result.mtbs = sessions.mtbs.median;
            actions.forEach(function (action) {
                moduleNames.forEach(function (module) {
                    result = sinceLast(action, module); //Since Last
                    result["s1"][action] += sessions.modules[module][action][maxIndex]; //Aggregated
                });
            });
            windows = {
                's1': 1, "s5": Math.min(maxIndex + 1, 5),
                "d14": indexforDaysWindow(14), "d28": indexforDaysWindow(28)
            };
            for (var key in windows) {
                window = [windows[key]][0];
                if (key === 'd28' || key === 'd14') {
                    result[key].daysCount = uniqueDates(sessions.launch.slice(-window)).length; // unique days
                    d = sessions.duration.slice(-window).filter(function (value) {
                        return value !== 0
                    });
                    sum = d.reduce(add, 0);
                    result[key].avgDur = Math.round(sum / d.length); //average duration by window
                    result[key].sessCount = sessions.launch.slice(-window).length; //unique sessions
                }
                //actions by module and window
                for (var i = 0, l = moduleNames.length; i < l; i++) aggModules(moduleNames[i], key);
                if (key === 'd28') {
                    tileNames.forEach(function (tile) {
                        aggTiles(tile, key)
                    })
                }
            }
        }
        else {} //first session
        return result;
    }catch (err) {trace("Airlock QuerySession:" + err.name + ":" + err.message);}
}

function aggTiles(tile, key) {
    tileCache = sessions.tiles[tile];
    id = allTiles.indexOf(tile);
    if (id != -1){
        id = "t" + id.toString();
        res = result[key].t[id];
        objCTR = [];
        actions.forEach(function (action) {
            obj = tileCache[action].slice(-window);
            res[action] = obj.reduce(add, 0); //count action
            objCTR[action] = tileCache[action].filter(function (value) {return value !== 0}).length //count sessions
        });
        res['r'] = objCTR.vi > 0 ? Math.round(objCTR.cl / Math.max(objCTR.vi, 1) * 100): -99; //Share of Sessions clicked
    }
}

function aggModules(module, key) {
    res = result[key][module];
    dt = sessions.launch.slice(-window);
    actions.forEach(function (action) {
        obj = sessions.modules[module][action].slice(-window);
        res[action] = obj.reduce(add, 0); //count action
        if (key !== "s1") {
            obsWith = obj.filter(function (value) {return value !== 0}).length;
            res[action + "Sess"] = obsWith; // Sessions with action
            res[action + "Share"] = Math.round(obsWith / Math.max(window, 1) * 100); //Share of Sessions with action
            for (i = 0; i <= obj.length - 1; i++) { //max and min Hour
                hour = new Date(dt[i]).getHours();
                if (obj[i] > 0 && hour > res[action + 'maxHr']) res[action + 'maxHr'] = hour; // max hour of action
                if (obj[i] > 0 && (hour < res[action + 'minHr'] ||
                        res[action + 'minHr'] === -99)) res[action + 'minHr'] = hour; // max hour of action
            }
        }
    });
}

function indexforDaysWindow(days) {
    for (i = 0; i <= maxIndex + 1; i++) {
        if (sessions.launch[i] > sessions.launch[maxIndex]
            - 60 * 60 * 1000 * 24 * days) return maxIndex + 1 - i ;
    }
    if (i === undefined) return maxIndex ;
}

function prepareResultArray() {
    result = [];
    result.since = [];
    moduleNames.forEach(function (module) {
        result.since[module] = {
            clTime: -99,
            clSess: -99,
            viTime: -99,
            viSess: -99
        }
    });
    ['s1','s5', 'd14', 'd28'].forEach(function (key) {
        result[key] = [];
        moduleNames.forEach(function (module) {
            if (key !== "s1") {
                result[key][module] = {
                    cl: 0,
                    clSess: 0,
                    clShare: 0,
                    clmaxHr: -99,
                    clminHr: -99,
                    vimaxHr: -99,
                    viminHr: -99,
                    vi: 0,
                    viShare: -99
                };
            }
            else {
                result[key].cl = 0;
                result[key].vi = 0;
                result[key][module] = {
                    cl: 0,
                    vi: 0
                }
            }
        });
        if (key === "d28") {
            result[key].t = {};

            tileNames.forEach(function (tile) {
                id = allTiles.indexOf(tile);
                if (id != -1) {
                    id = "t" + id.toString();
                    result[key].t[id] = [];
                }
            });
        }
    });
    result.tzOffset = new Date().getTimezoneOffset();
    return result;
}