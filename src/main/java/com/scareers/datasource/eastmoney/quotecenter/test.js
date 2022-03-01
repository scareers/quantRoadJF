!function (e) {
    var t = {};

    function n(r) {
        if (t[r]) return t[r].exports;
        var o = t[r] = {i: r, l: !1, exports: {}};
        return e[r].call(o.exports, o, o.exports, n), o.l = !0, o.exports
    }

    n.m = e, n.c = t, n.d = function (e, t, r) {
        n.o(e, t) || Object.defineProperty(e, t, {configurable: !1, enumerable: !0, get: r})
    }, n.n = function (e) {
        var t = e && e.__esModule ? function () {
            return e["default"]
        } : function () {
            return e
        };
        return n.d(t, "a", t), t
    }, n.o = function (e, t) {
        return Object.prototype.hasOwnProperty.call(e, t)
    }, n.p = "", n(n.s = 64)
}([function (e, t, n) {
    "use strict";
    t.__esModule = !0, t.extend = s, t.indexOf = function (e, t) {
        for (var n = 0, r = e.length; n < r; n++) if (e[n] === t) return n;
        return -1
    }, t.escapeExpression = function (e) {
        if ("string" != typeof e) {
            if (e && e.toHTML) return e.toHTML();
            if (null == e) return "";
            if (!e) return e + "";
            e = "" + e
        }
        if (!i.test(e)) return e;
        return e.replace(o, a)
    }, t.isEmpty = function (e) {
        return !e && 0 !== e || !(!u(e) || 0 !== e.length)
    }, t.createFrame = function (e) {
        var t = s({}, e);
        return t._parent = e, t
    }, t.blockParams = function (e, t) {
        return e.path = t, e
    }, t.appendContextPath = function (e, t) {
        return (e ? e + "." : "") + t
    };
    var r = {"&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#x27;", "`": "&#x60;", "=": "&#x3D;"},
        o = /[&<>"'`=]/g, i = /[&<>"'`=]/;

    function a(e) {
        return r[e]
    }

    function s(e) {
        for (var t = 1; t < arguments.length; t++) for (var n in arguments[t]) Object.prototype.hasOwnProperty.call(arguments[t], n) && (e[n] = arguments[t][n]);
        return e
    }

    var c = Object.prototype.toString;
    t.toString = c;
    var l = function (e) {
        return "function" == typeof e
    };
    l(/x/) && (t.isFunction = l = function (e) {
        return "function" == typeof e && "[object Function]" === c.call(e)
    }), t.isFunction = l;
    var u = Array.isArray || function (e) {
        return !(!e || "object" != typeof e) && "[object Array]" === c.call(e)
    };
    t.isArray = u
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0;
    var r = ["description", "fileName", "lineNumber", "endLineNumber", "message", "name", "number", "stack"];

    function o(e, t) {
        var n = t && t.loc, i = undefined, a = undefined, s = undefined, c = undefined;
        n && (i = n.start.line, a = n.end.line, s = n.start.column, c = n.end.column, e += " - " + i + ":" + s);
        for (var l = Error.prototype.constructor.call(this, e), u = 0; u < r.length; u++) this[r[u]] = l[r[u]];
        Error.captureStackTrace && Error.captureStackTrace(this, o);
        try {
            n && (this.lineNumber = i, this.endLineNumber = a, Object.defineProperty ? (Object.defineProperty(this, "column", {
                value: s,
                enumerable: !0
            }), Object.defineProperty(this, "endColumn", {
                value: c,
                enumerable: !0
            })) : (this.column = s, this.endColumn = c))
        } catch (e) {
        }
    }

    o.prototype = new Error, t["default"] = o, e.exports = t["default"]
}, function (e, t, n) {
    e.exports = n(27)["default"]
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.sendHost = t.sendInterface = t.sendByScript = t.loadResource = t.getHq = t.sendJsonp = t.ajax = t.send = void 0;
    var r = n(24), o = n(6), i = n(15), a = n(8);
    n(25);
    window.location.origin;

    function s(e, t, n, r) {
        return void 0 === n && (n = "json"), void 0 === r && (r = 30), new Promise(function (n, r) {
            var o = new XMLHttpRequest, i = [];
            for (var a in t) i.push(a + "=" + encodeURIComponent(t[a]));
            var s = i.join("&");
            o.responseType = "json", o.onreadystatechange = function () {
                4 == o.readyState && o.status >= 200 && o.status < 400 && n(o.response)
            }, o.onerror = function (e) {
                console.error(e), r(e)
            }, o.ontimeout = function (e) {
                console.error("timeout", e), r(e)
            }, o.withCredentials = !0, o.open("POST", e), o.setRequestHeader("Content-Type", "application/x-www-form-urlencoded"), o.send(s)
        })
    }

    function c(e, t, n) {
        return void 0 === t && (t = "callback"), void 0 === n && (n = !0), o["default"](e, "", t, n)
    }

    t.send = function (e, t, n) {
        void 0 === n && (n = "/rank/interface/GetData.aspx");
        var o = "";
        "string" == typeof t ? o = t : (t = Object.assign({}, t), o = r.params(t));
        var a = {param: o, plat: "Web", path: e, env: 1, origin: undefined};
        return i.env.isDevelop ? (n = "//guba-test.eastmoney.com/interface/GetData.aspx", a.origin = window.location.origin) : (i.env.isTest, n = "/rank/interface/GetData.aspx"), s(n, a, "json", 5e3)
    }, t.ajax = s, t.sendJsonp = c, t.getHq = function (e, t) {
        var n = (void 0 === t ? {fields: ""} : t).fields, r = a.getHQSecIdByMutiCode(e), o = r.indexOf(",") >= 0,
            i = "";
        return n || (n = o ? "f1,f2,f3,f4,f12,f13,f14,f152,f15,f16" : "f43,f170,f57,f107,f58"), i = o ? "https://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&np=3&ut=a79f54e3d4c8d44e494efb8f748db291&invt=2&secids=" + r + "&fields=" + n : "https://push2.eastmoney.com/api/qt/stock/get?fltt=2&ut=a79f54e3d4c8d44e494efb8f748db291&invt=2&secid=" + r + "&fields=" + n, new Promise(function (e, t) {
            c(i, "cb").then(function (t) {
                e(t)
            })
        })
    };
    var l = {}, u = {};

    function d(e) {
        var t = l[e];
        if (t) for (var n in t) {
            (0, t[n])()
        }
    }

    t.loadResource = function (e, t, n) {
        if (void 0 === n && (n = !1), l[e] || (l[e] = [], u[e] = 0), l[e].push(t), !0 === n) {
            if (1 == u[e]) return;
            if (2 == u[e]) return void d(e)
        }
        u[e] = 1;
        var r = e.lastIndexOf(".") + 1, o = e.length, i = e.substring(r, o), a = null;

        function s(t) {
            u[e] = 2, setTimeout(function () {
                console.log("execute:" + e), !0 !== n ? t && t() : d(e)
            }, 0)
        }

        "js" == i ? ((a = document.createElement("script")).setAttribute("type", "text/javascript"), a.setAttribute("src", e), document.getElementsByTagName("body")[0].appendChild(a), document.all ? a.onreadystatechange = function () {
            "loaded" != a.readyState && "complete" != a.readyState || s(t)
        } : a.onload = function () {
            setTimeout(function () {
                s(t)
            }, 300)
        }) : "css" == i && ((a = document.createElement("link")).setAttribute("rel", "stylesheet"), a.setAttribute("type", "text/css"), a.setAttribute("href", e), document.getElementsByTagName("head")[0].appendChild(a), t && t())
    }, t.sendByScript = function (e, t, n, r, o) {
        var i = new Date,
            a = i.getFullYear() + "_" + (i.getMonth() + 1) + "_" + i.getDate() + "_" + i.getHours() + "_" + i.getMinutes();
        e = e, t && (e = e + "?" + t), e = e.indexOf("?") < 0 ? e + "?v=" + a : e + "&v=" + a, $.ajax({
            type: "GET",
            url: e,
            dataType: "script",
            data: {},
            cache: !0,
            success: function (e) {
                n(e)
            },
            error: function (e) {
                r(e)
            }
        })
    }, t.sendInterface = function (e, t) {
        var n = "/" + e;
        return t = t || {}, i.env.isDevelop && (n = "//guba-test.eastmoney.com" + n, t.origin = window.location.origin), s(n, t)
    }, t.sendHost = function () {
        var e = "//gbcdn.dfcfw.com/rank/", t = "//guba.eastmoney.com/rank/";
        return i.env.isRelease || (e = "//gbcdn-test.dfcfw.com/rank/", t = "//guba-test.eastmoney.com/rank/", location.hostname.indexOf("guba-testd") >= 0 && (e = "//gubacdnimgtest.dfcfw.com/rank/", t = "//guba-testd.eastmoney.com/rank/")), {
            gbcdnHost: e,
            gbHost: t
        }
    }
}, function (e, t) {
    var n;
    n = function () {
        return this
    }();
    try {
        n = n || Function("return this")() || (0, eval)("this")
    } catch (e) {
        "object" == typeof window && (n = window)
    }
    e.exports = n
}, function (e, t, n) {
    "use strict";

    function r(e) {
        if (e) {
            e = e.replace(/href= target='_blank'/gi, 'href="javascript:;"');
            var t = ["微笑", "大笑", "鼓掌", "为什么", "哭", "怒", "滴汗", "俏皮", "傲", "好困惑", "兴奋", "加油", "困顿", "想一下", "撇嘴", "色", "发呆", "得意", "害羞羞", "大哭", "呲牙", "惊讶", "囧", "抓狂", "偷笑", "愉快", "憨笑", "晕", "再见", "坏笑", "左哼哼", "右哼哼", "哈欠", "委屈", "快哭了", "亲", "可怜", "口罩", "笑哭", "惊吓", "哼", "捂脸", "奸笑", "吃瓜", "旺柴", "围观", "摊手", "爱心", "献花", "福", "拜神", "胜利", "赞", "握手", "抱拳", "勾引", "拳头", "OKOK", "强壮", "毛估估", "亏大了", "赚大了", "牛", " ", "成交", "财力", "护城河", "复盘", "买入", "卖出", "满仓", "空仓", "抄底", "看多", "看空", "加仓", "减仓", "上涨", "下跌", "财神", "火箭", "龙头", "韭菜", "面", "泡沫", "惨!关灯吃面"],
                n = new RegExp("\\[.+?\\]", "ig");
            return e = e.replace(n, function (e) {
                for (var n = 0; n < t.length; n++) if ("[" + t[n] + "]" == e) return '<img class="emot" title="' + t[n] + '" src="http://gbfek.dfcfw.com/face/emot_default/emot' + (n + 1) + '.png" alt="' + e + '">';
                return e
            })
        }
        return ""
    }

    function o(e) {
        return setTimeout(function () {
            var t = e.innerHTML;
            var n = e.clientHeight;
            e.scrollHeight - 10 > n ? function n() {
                t = t.substring(0, t.length - 4), e.innerHTML = t + "...", e.scrollHeight - 10 > e.clientHeight ? n() : e.innerHTML = t + "..."
            }() : e.innerHTML = t
        }, 0), !1
    }

    Object.defineProperty(t, "__esModule", {value: !0}), t.fixedQuote = t.formatQuoteZDF = t.cutCommentTexts = t.cutTexts = t.cutText = t.showface = t.CtoH = t.getQueryString = void 0, t.getQueryString = function (e) {
        var t = new RegExp("(^|&)" + e + "=([^&]*)(&|$)"), n = window.location.search.substr(1).match(t);
        return null != n ? unescape(n[2]) : null
    }, t.CtoH = function (e) {
        for (var t = "", n = 0; n < e.length; n++) 12288 != e.charCodeAt(n) ? e.charCodeAt(n) > 65280 && e.charCodeAt(n) < 65375 ? t += String.fromCharCode(e.charCodeAt(n) - 65248) : t += String.fromCharCode(e.charCodeAt(n)) : t += String.fromCharCode(e.charCodeAt(n) - 12256);
        return t
    }, t.showface = r, t.cutText = o, t.cutTexts = function (e) {
        return setTimeout(function () {
            document.querySelectorAll(e).forEach(function (e) {
                o(e)
            })
        }, 0), !1
    }, t.cutCommentTexts = function (e) {
        return setTimeout(function () {
            document.querySelectorAll(e).forEach(function (e) {
                o(e)
            })
        }, 0), setTimeout(function () {
            document.querySelectorAll(e).forEach(function (e) {
                var t, n;
                e.innerHTML = r((t = e.innerHTML, (n = /\[at=(.*?)\](.*?)\[\/at\]/gim).test(t) ? t.replace(n, '<a class="at_user" href="http://mguba.eastmoney.com/mguba/user/$1">$2</a>') : t))
            })
        }, 100), !1
    }, t.formatQuoteZDF = function (e) {
        return e.toString().indexOf("%") >= 0 ? e.toFixed(2) : e < 0 ? e.toFixed(2) + "%" : e.toString().indexOf("-") >= 0 ? e : e.toFixed(2) + "%"
    }, t.fixedQuote = function (e) {
        return e < 0 ? e.toFixed(2) : e.toString().indexOf("-") >= 0 ? e : e.toFixed(2)
    }
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.loadScript = t.jsonpGet = void 0;
    var r, o = "qa_wap_jsonpCB";

    function i(e, t, n, r) {
        return void 0 === n && (n = "callback"), void 0 === r && (r = !0), new Promise(function (i, a) {
            var s, c = Date.parse((new Date).toString()) + (1e3 * Math.random() >> 0) + "", l = o + c;

            function u(e) {
                e.parentNode.removeChild(e)
            }

            t ? (s = e + "?" + t, s += n + "=" + l) : s = e + "&" + n + "=" + l, window[l] = function (e) {
                window["jsonpData_" + l] = e, delete window[l]
            };
            var d = document.createElement("script");
            d.type = "text/javascript", d.src = r ? encodeURI(s) : s, d.id = l, d.onload = function (e) {
                u(e.target), i(window["jsonpData_" + l]), delete window["jsonpData_" + l]
            }, d.onerror = function (e) {
                u(e.target);
                var t = "Your jsonp request to " + e.target.src + " is fail, please check your url or params again.";
                a(t)
            }, document.body.appendChild(d)
        })
    }

    t.jsonpGet = i, function (e) {
        e[e.LOADING = 0] = "LOADING", e[e.LOADED = 1] = "LOADED"
    }(r || (r = {}));
    var a = new Map;
    t.loadScript = function e(t, n, o) {
        void 0 === o && (o = !1);
        var i = encodeURI(t);
        if (a.get(i) != r.LOADED) if (o && "undefined" == typeof window.jQuery) e("//gbfek.dfcfw.com/libs/jquery/1.8.3/jquery.min.js", function () {
            e(t, n)
        }); else {
            a.set(t, r.LOADING);
            var s = document.createElement("script");
            s.type = "text/javascript", s.src = i, s.onload = function (e) {
                a.set(i, r.LOADED), n()
            }, s.onerror = function (e) {
                a["delete"](e.target.src), e.target.src
            }, document.body.appendChild(s)
        } else n()
    }, t["default"] = i
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.deleteCookie = t.getCookie = t.getsec = t.setCookie = void 0;
    var r = n(5);

    function o(e) {
        var t = Number(e.substring(1, e.length)), n = e.substring(0, 1);
        return "s" == n ? 1e3 * t : "h" == n ? 60 * t * 60 * 1e3 : "d" == n ? 24 * t * 60 * 60 * 1e3 : 6048e5
    }

    t.setCookie = function (e, t, n) {
        void 0 === n && (n = "");
        var i = new Date, a = encodeURIComponent(r.CtoH(t));
        if (n) {
            var s = o(n);
            i.setTime(i.getTime() + s)
        } else i.setTime(i.getTime() + 6048e5);
        document.cookie = e + "=" + a + "; expires=" + i.toUTCString() + "; path=/"
    }, t.getsec = o, t.getCookie = function (e) {
        var t = ("; " + document.cookie).split("; " + e + "="), n = "";
        if (2 == t.length || 3 == t.length) return n = t.pop().split(";").shift(), decodeURIComponent(n)
    }, t.deleteCookie = function (e) {
        var t = new Date;
        t.setTime(t.getTime() + -864e5), document.cookie = e + "=; expires=" + t.toUTCString() + "; path=/"
    }
}, function (e, t, n) {
    "use strict";

    function r(e) {
        if ("105" == e.substring(0, 3) || "106" == e.substring(0, 3) || "107" == e.substring(0, 3)) return e;
        if ("NASDAQ" == e.substring(0, 6) || "nasdaq" == e.substring(0, 6)) return "105." + e.substring(7, 999);
        if ("NYSE" == e.substring(0, 4) || "nyse" == e.substring(0, 4)) return "106." + e.substring(5, 999);
        if ("AMEX" == e.substring(0, 4) || "amex" == e.substring(0, 4)) return "107." + e.substring(5, 999);
        if ("HK" == e.substring(0, 2) || "hk" == e.substring(0, 2)) return "116." + e.substring(3, 999);
        var t = e.substring(0, 1), n = e.substring(0, 2), r = e.substring(0, 3);
        return "5" == t || "6" == t || "9" == t ? "1." + e : 0 == e.toLowerCase().indexOf("sh") ? "1." + e.substring(2, e.length) : 0 == e.toLowerCase().indexOf("sz") ? "0." + e.substring(2, e.length) : "bk" == n.toLowerCase() ? "90." + e : "000003" == e || "000300" == e ? "1." + e : "009" == r || "126" == r || "110" == r ? "1." + e : "0." + e
    }

    Object.defineProperty(t, "__esModule", {value: !0}), t.getSuffixByCode = t.getSuffixByMarket = t.getStockType = t.putMarketBeforeCode = t.getHSAB = t.getHQSecIdByCode = t.getHQSecIdByMutiCode = t.getMarketTypeByCode = void 0, t.getMarketTypeByCode = function (e) {
        if (0 == e.toLowerCase().indexOf("hk")) return "5";
        if (0 == e.toLowerCase().indexOf("us")) return "7";
        if (0 == e.toLowerCase().indexOf("sh")) return "1";
        if (0 == e.toLowerCase().indexOf("sz")) return "2";
        var t = "1", n = e.substring(0, 1), r = e.substring(0, 3);
        return "5" == n || "6" == n || "9" == n || "009" == r || "126" == r || "110" == r || (t = "2"), t
    }, t.getHQSecIdByMutiCode = function (e) {
        for (var t = [], n = 0, o = e.split(","); n < o.length; n++) {
            var i = r(o[n]);
            t.push(i)
        }
        return t.join(",")
    }, t.getHQSecIdByCode = r, t.getHSAB = function (e, t) {
        if (1 == e) {
            if (2 == t || 23 == t) return "沪A";
            if (3 == t) return "沪B"
        } else if (0 == e) {
            if (6 == t || 13 == t || 80 == t) return "深A";
            if (7 == t) return "深B"
        }
    }, t.putMarketBeforeCode = function (e) {
        if (e.indexOf(".") > 0) {
            var t = e.split(".");
            return t[1] + t[0]
        }
        return e
    }, t.getStockType = function () {
        var e = window, t = e.Category, n = e.Market;
        return 200 == e.Category && 102 == e.Type ? "kcbUnMarked" : "100" != t && "101" != t || "100" != n && "101" != n ? "100" != t && "101" != t || "102" != n ? "100" != t || "103" != n && "104" != n && "105" != n ? "100" == t && "106" == n ? "hk" : "102" == t || "201" == t || "103" == t || "200" == t || "206" == t || "107" == t || "205" == t ? "kcb" == e.code ? "zt_kcb" : "zs" : "108" == t ? "zq" : "204" == t || "106" == t ? "qq" : "105" == t ? "jj" : "110" == t ? "hkjj" : "111" == t ? "gdlc" : "100" != t || "120" != n && "130" != n && "131" != n && "132" != n && "133" != n && "134" != n ? void 0 : "ljs" : "us" : "sb" : "hs"
    }, t.getSuffixByMarket = function (e) {
        return "100" == e ? "sh" : "101" == e ? "sz" : ""
    }, t.getSuffixByCode = function (e, t) {
        if (0 == e.toLowerCase().indexOf("hk") || 116 == t) return "hk";
        if (0 == e.toLowerCase().indexOf("us") || 105 == t || 106 == t || 107 == t) return "us";
        if (0 == e.toLowerCase().indexOf("sh")) return "sh";
        if (0 == e.toLowerCase().indexOf("sz")) return "sz";
        var n = "sh", r = e.substring(0, 1), o = e.substring(0, 3);
        return "5" == r || "6" == r || "9" == r || "009" == o || "126" == o || "110" == o || (n = "sz"), n
    }
}, function (e, t, n) {
    "use strict";

    function r(e) {
        return e && e.__esModule ? e : {"default": e}
    }

    t.__esModule = !0, t.HandlebarsEnvironment = u;
    var o = n(0), i = r(n(1)), a = n(10), s = n(35), c = r(n(11)), l = n(12);
    t.VERSION = "4.7.7";
    t.COMPILER_REVISION = 8;
    t.LAST_COMPATIBLE_COMPILER_REVISION = 7;
    t.REVISION_CHANGES = {
        1: "<= 1.0.rc.2",
        2: "== 1.0.0-rc.3",
        3: "== 1.0.0-rc.4",
        4: "== 1.x.x",
        5: "== 2.0.0-alpha.x",
        6: ">= 2.0.0-beta.1",
        7: ">= 4.0.0 <4.3.0",
        8: ">= 4.3.0"
    };

    function u(e, t, n) {
        this.helpers = e || {}, this.partials = t || {}, this.decorators = n || {}, a.registerDefaultHelpers(this), s.registerDefaultDecorators(this)
    }

    u.prototype = {
        constructor: u, logger: c["default"], log: c["default"].log, registerHelper: function (e, t) {
            if ("[object Object]" === o.toString.call(e)) {
                if (t) throw new i["default"]("Arg not supported with multiple helpers");
                o.extend(this.helpers, e)
            } else this.helpers[e] = t
        }, unregisterHelper: function (e) {
            delete this.helpers[e]
        }, registerPartial: function (e, t) {
            if ("[object Object]" === o.toString.call(e)) o.extend(this.partials, e); else {
                if (void 0 === t) throw new i["default"]('Attempting to register a partial called "' + e + '" as undefined');
                this.partials[e] = t
            }
        }, unregisterPartial: function (e) {
            delete this.partials[e]
        }, registerDecorator: function (e, t) {
            if ("[object Object]" === o.toString.call(e)) {
                if (t) throw new i["default"]("Arg not supported with multiple decorators");
                o.extend(this.decorators, e)
            } else this.decorators[e] = t
        }, unregisterDecorator: function (e) {
            delete this.decorators[e]
        }, resetLoggedPropertyAccesses: function () {
            l.resetLoggedProperties()
        }
    };
    var d = c["default"].log;
    t.log = d, t.createFrame = o.createFrame, t.logger = c["default"]
}, function (e, t, n) {
    "use strict";

    function r(e) {
        return e && e.__esModule ? e : {"default": e}
    }

    t.__esModule = !0, t.registerDefaultHelpers = function (e) {
        o["default"](e), i["default"](e), a["default"](e), s["default"](e), c["default"](e), l["default"](e), u["default"](e)
    }, t.moveHelperToHooks = function (e, t, n) {
        e.helpers[t] && (e.hooks[t] = e.helpers[t], n || delete e.helpers[t])
    };
    var o = r(n(28)), i = r(n(29)), a = r(n(30)), s = r(n(31)), c = r(n(32)), l = r(n(33)), u = r(n(34))
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0;
    var r = n(0), o = {
        methodMap: ["debug", "info", "warn", "error"], level: "info", lookupLevel: function (e) {
            if ("string" == typeof e) {
                var t = r.indexOf(o.methodMap, e.toLowerCase());
                e = t >= 0 ? t : parseInt(e, 10)
            }
            return e
        }, log: function (e) {
            if (e = o.lookupLevel(e), "undefined" != typeof console && o.lookupLevel(o.level) <= e) {
                var t = o.methodMap[e];
                console[t] || (t = "log");
                for (var n = arguments.length, r = Array(n > 1 ? n - 1 : 0), i = 1; i < n; i++) r[i - 1] = arguments[i];
                console[t].apply(console, r)
            }
        }
    };
    t["default"] = o, e.exports = t["default"]
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0, t.createProtoAccessControl = function (e) {
        var t = Object.create(null);
        t.constructor = !1, t.__defineGetter__ = !1, t.__defineSetter__ = !1, t.__lookupGetter__ = !1;
        var n = Object.create(null);
        return n.__proto__ = !1, {
            properties: {
                whitelist: r.createNewLookupObject(n, e.allowedProtoProperties),
                defaultValue: e.allowProtoPropertiesByDefault
            },
            methods: {
                whitelist: r.createNewLookupObject(t, e.allowedProtoMethods),
                defaultValue: e.allowProtoMethodsByDefault
            }
        }
    }, t.resultIsAllowed = function (e, t, n) {
        return a("function" == typeof e ? t.methods : t.properties, n)
    }, t.resetLoggedProperties = function () {
        Object.keys(i).forEach(function (e) {
            delete i[e]
        })
    };
    var r = n(37), o = function (e) {
        if (e && e.__esModule) return e;
        var t = {};
        if (null != e) for (var n in e) Object.prototype.hasOwnProperty.call(e, n) && (t[n] = e[n]);
        return t["default"] = e, t
    }(n(11)), i = Object.create(null);

    function a(e, t) {
        return e.whitelist[t] !== undefined ? !0 === e.whitelist[t] : e.defaultValue !== undefined ? e.defaultValue : (function (e) {
            !0 !== i[e] && (i[e] = !0, o.log("error", 'Handlebars: Access has been denied to resolve the property "' + e + '" because it is not an "own property" of its parent.\nYou can add a runtime option to disable the check or this warning:\nSee https://handlebarsjs.com/api-reference/runtime-options.html#options-to-control-prototype-access for details'))
        }(t), !1)
    }
}, function (e, t) {
    var n = {
        get: function (e) {
            var t, n = new RegExp("(^| )" + e + "=([^;]*)(;|$)");
            return (t = document.cookie.match(n)) ? decodeURIComponent(t[2]) : null
        }, set: function (e, t, n, r) {
            r = r || ".eastmoney.com";
            var o = new Date;
            if (n) {
                var i = this.getsec(n);
                o.setTime(o.getTime() + 1 * i), document.cookie = e + "=" + encodeURIComponent(t) + ";domain=" + r + ";path=/;expires=" + o.toGMTString()
            } else document.cookie = e + "=" + encodeURIComponent(t) + ";domain=" + r + ";path=/"
        }, getsec: function (e) {
            var t = 1 * e.substring(1, e.length), n = e.substring(0, 1);
            return "s" == n ? 1e3 * t : "h" == n ? 60 * t * 60 * 1e3 : "d" == n ? 24 * t * 60 * 60 * 1e3 : -1
        }, removeCookie: function (e, t) {
            return this.get(e) !== undefined && (this.set(e, "", "-1"), !this.get(e))
        }
    };
    e.exports = {get: n.get, set: n.set, getsec: n.getsec, removeCookie: n.removeCookie}
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.user = void 0;
    var r = n(7), o = null, i = function () {
        function e() {
        }

        return e.prototype.getUser = function () {
            var e = r.getCookie("pi");
            if (e) {
                var t = e.split(";");
                o = {id: t[0], name: t[1], nickname: decodeURI(t[2])}
            } else o = null;
            return o
        }, e.prototype.isLogin = function () {
            return null != this.getUser()
        }, e.prototype.reload = function () {
            this.getUser()
        }, e.prototype.logOut = function () {
            r.deleteCookie("dcuser_pubs"), r.deleteCookie("dcuser_keys");
            var e = new Date;
            document.cookie = "dcusername=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuserinfo=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcusermingchen=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuserpass=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuserpubinfo=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuserpubs=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuserkeys=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuser_name=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuser_info=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuser_mingchen=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuser_pass=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuser_pubinfo=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuser_pubs=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "dcuser_keys=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "puser_pname=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "puser_pinfo=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "pi=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "ct=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "ut=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), document.cookie = "uidal=;path=/;domain=eastmoney.com;expires=" + e.toGMTString(), window.location.reload()
        }, e
    }();
    t.user = new i
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.env = void 0;
    var r = !1, o = !1, i = !1;
    window.location.host.indexOf(":9") >= 0 || window.location.host.indexOf(":8") >= 0 || window.location.host.indexOf("guangguang") >= 0 ? o = !0 : window.location.host.indexOf("-test") >= 0 || window.location.host.indexOf("test") >= 0 ? i = !0 : r = !0, t.env = {
        isRelease: r,
        isDevelop: o,
        isTest: i
    }
}, , function (e, t, n) {
    (function (r) {
        var o;
        "NodeList" in window && !NodeList.prototype.forEach && (console.info("polyfill for IE11"), NodeList.prototype.forEach = function (e, t) {
            t = t || window;
            for (var n = 0; n < this.length; n++) e.call(t, this[n], n, this)
        }), function (e) {
            function n(e) {
                var t = [], n = [], r = [], o = Object.is || function (e, t) {
                    return e === t ? 0 !== e || 1 / e == 1 / t : e != e && t != t
                }, i = function (e) {
                    if (e != e || 0 === e) for (var t = this.length; t-- && !o(this[t], e);) ; else t = [].indexOf.call(this, e);
                    return t
                }, s = function (e, o) {
                    var a = i.call(n, e);
                    a > -1 ? (t[a][1] = o, r[a] = o) : (t.push([e, o]), n.push(e), r.push(o))
                };
                if (Array.isArray(e)) e.forEach(function (e) {
                    if (2 !== e.length) throw new TypeError("Invalid iterable passed to Map constructor");
                    s(e[0], e[1])
                }); else if (e !== undefined) throw new TypeError("Invalid Map");
                return Object.create(a, {
                    items: {
                        value: function () {
                            return [].slice.call(t)
                        }
                    }, keys: {
                        value: function () {
                            return [].slice.call(n)
                        }
                    }, values: {
                        value: function () {
                            return [].slice.call(r)
                        }
                    }, has: {
                        value: function (e) {
                            return i.call(n, e) > -1
                        }
                    }, get: {
                        value: function (e) {
                            var t = i.call(n, e);
                            return t > -1 ? r[t] : undefined
                        }
                    }, set: {value: s}, size: {
                        get: function () {
                            return t.length
                        }
                    }, clear: {
                        value: function () {
                            n.length = r.length = t.length = 0
                        }
                    }, "delete": {
                        value: function (e) {
                            var o = i.call(n, e);
                            return o > -1 && (n.splice(o, 1), r.splice(o, 1), t.splice(o, 1), !0)
                        }
                    }, forEach: {
                        value: function (e) {
                            if ("function" != typeof e) throw new TypeError("Invalid callback function given to forEach");

                            function t() {
                                try {
                                    return n.next()
                                } catch (e) {
                                    return undefined
                                }
                            }

                            for (var n = this.iterator(), r = t(), o = t(); r !== undefined;) e.apply(arguments[1], [r[1], r[0], this]), r = o, o = t()
                        }
                    }, iterator: {
                        value: function () {
                            return new function (e, t) {
                                var n = 0;
                                return Object.create({}, {
                                    next: {
                                        value: function () {
                                            if (n < e.items().length) switch (t) {
                                                case"keys":
                                                    return e.keys()[n++];
                                                case"values":
                                                    return e.values()[n++];
                                                case"keys+values":
                                                    return [].slice.call(e.items()[n++]);
                                                default:
                                                    throw new TypeError("Invalid iterator type")
                                            }
                                            throw new Error("Stop Iteration")
                                        }
                                    }, iterator: {
                                        value: function () {
                                            return this
                                        }
                                    }, toString: {
                                        value: function () {
                                            return "[object Map Iterator]"
                                        }
                                    }
                                })
                            }(this, "keys+values")
                        }
                    }, toString: {
                        value: function () {
                            return "[Object Map]"
                        }
                    }
                })
            }

            var o = "undefined" == e, i = o ? this : r, a = (e = o ? {} : t, n.prototype);
            n.prototype = a = n(), i.Map = e.Map = i.Map || n
        }.call(this, typeof t), function (e) {
            "use strict";
            if (!e.WeakMap) {
                var t = Object.prototype.hasOwnProperty, n = function (e, t, n) {
                    Object.defineProperty ? Object.defineProperty(e, t, {
                        configurable: !0,
                        writable: !0,
                        value: n
                    }) : e[t] = n
                };
                e.WeakMap = function () {
                    function e() {
                        if (void 0 === this) throw new TypeError("Constructor WeakMap requires 'new'");
                        if (n(this, "_id", "_WeakMap" + "_" + i() + "." + i()), arguments.length > 0) throw new TypeError("WeakMap iterable is not supported")
                    }

                    function o(e, n) {
                        if (!r(e) || !t.call(e, "_id")) throw new TypeError(n + " method called on incompatible receiver " + typeof e)
                    }

                    function i() {
                        return Math.random().toString().substring(2)
                    }

                    return n(e.prototype, "delete", function (e) {
                        if (o(this, "delete"), !r(e)) return !1;
                        var t = e[this._id];
                        return !(!t || t[0] !== e) && (delete e[this._id], !0)
                    }), n(e.prototype, "get", function (e) {
                        if (o(this, "get"), r(e)) {
                            var t = e[this._id];
                            return t && t[0] === e ? t[1] : void 0
                        }
                    }), n(e.prototype, "has", function (e) {
                        if (o(this, "has"), !r(e)) return !1;
                        var t = e[this._id];
                        return !(!t || t[0] !== e)
                    }), n(e.prototype, "set", function (e, t) {
                        if (o(this, "set"), !r(e)) throw new TypeError("Invalid value used as weak map key");
                        var i = e[this._id];
                        return i && i[0] === e ? (i[1] = t, this) : (n(e, this._id, [e, t]), this)
                    }), n(e, "_polyfill", !0), e
                }()
            }

            function r(e) {
                return Object(e) === e
            }
        }("undefined" != typeof self ? self : "undefined" != typeof window ? window : void 0 !== r ? r : this), function (e) {
            "use strict";
            e.Set || (console.log("not have Set"), e.Set = function () {
                var e = {
                    "[object Array]": !0,
                    "[object Arguments]": !0,
                    "[object HTMLCollection]": !0,
                    "[object NodeList]": !0
                }, t = Object.prototype.hasOwnProperty, n = Object.prototype.toString;

                function r(e, n) {
                    return t.call(e, n)
                }

                var o = Object.defineProperty && Object.defineProperties;

                function i(e, t, n, r, i) {
                    o ? Object.defineProperty(e, t, {enumerable: r, configurable: !1, writable: i, value: n}) : e[t] = n
                }

                var a = !1;

                function s(e, t) {
                    a = !0, e.size = t, a = !1
                }

                function c(t) {
                    var r, s, c = 0;
                    if (i(this, "baseType", "Set", !1, !1), i(this, "_data", {}, !1, !0), o ? Object.defineProperty(this, "size", {
                        enumerable: !0,
                        configurable: !1,
                        get: function () {
                            return c
                        },
                        set: function (e) {
                            if (!a) throw new Error("Can't set size property on Set object.");
                            c = e
                        }
                    }) : this.size = 0, t !== undefined && null !== t) if ("object" == typeof (r = t) && (s = n.call(r), !0 === e[s] || "number" == typeof r.length && r.length >= 0 && (0 === r.length || "object" == typeof r[0] && r[0].nodeType > 0))) for (var l = 0; l < t.length; l++) this.add(t[l]); else (t instanceof Set || "Set" === t.baseType) && t.forEach(function (e) {
                        this.add(e)
                    }, this)
                }

                var l = 0, u = "obj_", d = "__objectPolyFillID",
                    f = {string: !0, boolean: !0, number: !0, undefined: !0};

                function p(e, t) {
                    var r, i = typeof e;
                    if (f[i]) return i.substr(0, 3) + "_" + e;
                    if (null === e) return "nul_null";
                    if ("object" === i || "function" === i) return e[d] ? e[d] : t ? (r = u + l++, "[object Object]" === n.call(e) && o ? Object.defineProperty(e, d, {
                        enumerable: !1,
                        configurable: !1,
                        writable: !1,
                        value: r
                    }) : e[d] = r, r) : null;
                    throw new Error("Unsupported type for Set.add()")
                }

                function h(e, t, n) {
                    var o = 0, i = e.length;
                    this.next = function () {
                        for (var a, s, c = {}; ;) {
                            if (o < i) {
                                if (c.done = !1, s = e[o++], (a = t[s]) === undefined && !r(t, s)) continue;
                                "keys" === n ? c.value = a : "entries" === n && (c.value = [a, a])
                            } else e = null, t = null, c.done = !0;
                            return c
                        }
                    }
                }

                function m(e) {
                    var t = [];
                    for (var n in e) r(e, n) && t.push(n);
                    return t
                }

                return c.prototype = {
                    add: function (e) {
                        var t = p(e, !0);
                        return r(this._data, t) || (this._data[t] = e, s(this, this.size + 1)), this
                    }, clear: function () {
                        this._data = {}, s(this, 0)
                    }, "delete": function (e) {
                        var t = p(e, !1);
                        return !(null === t || !r(this._data, t)) && (delete this._data[t], s(this, this.size - 1), !0)
                    }, remove: function (e) {
                        return this["delete"](e)
                    }, forEach: function (e) {
                        if ("function" == typeof e) for (var t, n, r = arguments[1], o = this.keys(); (t = o.next()) && !t.done;) n = t.value, e.call(r, n, n, this)
                    }, has: function (e) {
                        var n = p(e, !1);
                        return null !== n && t.call(this._data, n)
                    }, values: function () {
                        return this.keys()
                    }, keys: function () {
                        return new h(m(this._data), this._data, "keys")
                    }, entries: function () {
                        return new h(m(this._data), this._data, "entries")
                    }
                }, c.prototype.constructor = c, c
            }())
        }("undefined" != typeof self ? self : "undefined" != typeof window ? window : void 0 !== r ? r : this), function (t, r) {
            "use strict";
            n(18).amd ? (o = function () {
                return r(t)
            }.call("exports", n, "exports", e)) === undefined || (e.exports = o) : r(t)
        }(window, function (e) {
            "use strict";
            var t, n, r, o, i, a, s, c, l, u = document.createElement("_"), d = "DOMAttrModified";

            function f() {
                var e, a = {};
                for (n = this.attributes, r = 0, s = n.length; r < s; r += 1) t = n[r], (c = t.name.match(i)) && (a[(e = c[1], e.replace(o, function (e, t) {
                    return t.toUpperCase()
                }))] = t.value);
                return a
            }

            function p() {
                a ? u.removeEventListener(d, p, !1) : u.detachEvent("on" + d, p), l = !0
            }

            u.dataset === undefined && (o = /\-([a-z])/gi, i = /^data\-(.+)/, a = !!document.addEventListener, l = !1, a ? u.addEventListener(d, p, !1) : u.attachEvent("on" + d, p), u.setAttribute("foo", "bar"), Object.defineProperty(e.Element.prototype, "dataset", {
                get: l ? function () {
                    return this._datasetCache || (this._datasetCache = f.call(this)), this._datasetCache
                } : f
            }), l && a && document.addEventListener(d, function (e) {
                delete e.target._datasetCache
            }, !1))
        }), "document" in window.self && ("classList" in document.createElement("_") && (!document.createElementNS || "classList" in document.createElementNS("http://www.w3.org/2000/svg", "g")) || function (e) {
            "use strict";
            if ("Element" in e) {
                var t = e.Element.prototype, n = Object, r = String.prototype.trim || function () {
                    return this.replace(/^\s+|\s+$/g, "")
                }, o = Array.prototype.indexOf || function (e) {
                    for (var t = 0, n = this.length; t < n; t++) if (t in this && this[t] === e) return t;
                    return -1
                }, i = function (e, t) {
                    this.name = e, this.code = DOMException[e], this.message = t
                }, a = function (e, t) {
                    if ("" === t) throw new i("SYNTAX_ERR", "An invalid or illegal string was specified");
                    if (/\s/.test(t)) throw new i("INVALID_CHARACTER_ERR", "String contains an invalid character");
                    return o.call(e, t)
                }, s = function (e) {
                    for (var t = r.call(e.getAttribute("class") || ""), n = t ? t.split(/\s+/) : [], o = 0, i = n.length; o < i; o++) this.push(n[o]);
                    this._updateClassName = function () {
                        e.setAttribute("class", this.toString())
                    }
                }, c = s.prototype = [], l = function () {
                    return new s(this)
                };
                if (i.prototype = Error.prototype, c.item = function (e) {
                    return this[e] || null
                }, c.contains = function (e) {
                    return -1 !== a(this, e += "")
                }, c.add = function () {
                    var e, t = arguments, n = 0, r = t.length, o = !1;
                    do {
                        e = t[n] + "", -1 === a(this, e) && (this.push(e), o = !0)
                    } while (++n < r);
                    o && this._updateClassName()
                }, c.remove = function () {
                    var e, t, n = arguments, r = 0, o = n.length, i = !1;
                    do {
                        for (e = n[r] + "", t = a(this, e); -1 !== t;) this.splice(t, 1), i = !0, t = a(this, e)
                    } while (++r < o);
                    i && this._updateClassName()
                }, c.toggle = function (e, t) {
                    e += "";
                    var n = this.contains(e), r = n ? !0 !== t && "remove" : !1 !== t && "add";
                    return r && this[r](e), !0 === t || !1 === t ? t : !n
                }, c.toString = function () {
                    return this.join(" ")
                }, n.defineProperty) {
                    var u = {get: l, enumerable: !0, configurable: !0};
                    try {
                        n.defineProperty(t, "classList", u)
                    } catch (e) {
                        e.number !== undefined && -2146823252 !== e.number || (u.enumerable = !1, n.defineProperty(t, "classList", u))
                    }
                } else n.prototype.__defineGetter__ && t.__defineGetter__("classList", l)
            }
        }(window.self), function () {
            "use strict";
            var e = document.createElement("_");
            if (e.classList.add("c1", "c2"), !e.classList.contains("c2")) {
                var t = function (e) {
                    var t = DOMTokenList.prototype[e];
                    DOMTokenList.prototype[e] = function (e) {
                        var n, r = arguments.length;
                        for (n = 0; n < r; n++) e = arguments[n], t.call(this, e)
                    }
                };
                t("add"), t("remove")
            }
            if (e.classList.toggle("c3", !1), e.classList.contains("c3")) {
                var n = DOMTokenList.prototype.toggle;
                DOMTokenList.prototype.toggle = function (e, t) {
                    return 1 in arguments && !this.contains(e) == !t ? t : n.call(this, e)
                }
            }
            e = null
        }())
    }).call(t, n(4))
}, function (e, t) {
    e.exports = function () {
        throw new Error("define cannot be used indirect")
    }
}, function (e, t) {
}, function (e, t) {
}, function (e, t) {
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.MakeHeader = void 0, n(23);
    var r = n(14), o = n(3), i = n(7), a = n(26), s = function () {
        function e() {
            this.query = function (e) {
                return document.querySelector(e)
            }, this.queryAll = function (e) {
                return document.querySelectorAll(e)
            }, this.host = "//i.eastmoney.com/", this.init()
        }

        return e.prototype.init = function () {
            this.query("#topnavper").innerHTML = a(), this.bind(), this.refreshstatus(), this.popmsgInit(), i.setCookie("rankpromt", "1", "d5")
        }, e.prototype.bind = function () {
            Array.prototype.forEach.call(this.queryAll(".user_info"), function (e) {
                var t = e.querySelector(".slide_down");
                e.addEventListener("mouseenter", function () {
                    var n, r;
                    null === (n = e.querySelector(".trg")) || void 0 === n || n.classList.remove("icon_down_s"), null === (r = e.querySelector(".trg")) || void 0 === r || r.classList.add("icon_up_s"), t.style.display = "block"
                }), e.addEventListener("mouseleave", function () {
                    var n, r;
                    null === (n = e.querySelector(".trg")) || void 0 === n || n.classList.remove("icon_up_s"), null === (r = e.querySelector(".trg")) || void 0 === r || r.classList.add("icon_down_s"), t.style.display = "none"
                })
            })
        }, e.prototype.refreshstatus = function () {
            if (r.user.isLogin()) this.query("#header-login").style.display = "none", this.query("#top-user").style.display = "block", this.query(".header_username .user_name").innerHTML = '<a class="top_name" href="' + this.host + r.user.getUser().id + '">' + r.user.getUser().nickname + "</a>", this.query(".iguba .user_name").innerHTML = '<a class="top_name bartype"  href="' + this.host + r.user.getUser().id + '">我的股吧</a>', this.query("#log-out").onclick = function () {
                r.user.logOut()
            }; else {
                this.query("#header-login").style.display = "block", this.query("#top-user").style.display = "none";
                var e = "https://passport2.eastmoney.com/pub/login?backurl=" + encodeURIComponent(window.location.href);
                this.query("#header_btn_login").setAttribute("href", e)
            }
        }, e.prototype.popmsgInit = function () {
            var e = this;
            if (!r.user.isLogin()) return !1;
            var t = function () {
                e.popmsgGet(function (t) {
                    for (var n, r = 0, o = 0; o < 3; o++) {
                        switch (o) {
                            case 0:
                                n = t.Reply;
                                break;
                            case 1:
                                n = t.RefMe;
                                break;
                            case 2:
                                n = t.Follower
                        }
                        r = e.makeMsg(r, n, o)
                    }
                    r > 0 && r <= 99 ? (e.query(".user_info .my_msg i").innerHTML = r, e.query(".user_info .my_msg i").classList.add("new_msg_tip")) : r > 99 && 0 == e.hasClass("my_msg", "icon_more_msg") && (e.query(".user_info .my_msg i").innerHTML = "", e.query(".user_info .my_msg i").classList.remove("new_msg_tip"), e.query(".user_info .my_msg i").classList.add("icon_more_msg"))
                })
            };
            setTimeout(function () {
                !function e() {
                    t(), setTimeout(function () {
                        e()
                    }, 3e4)
                }()
            }, 1e4)
        }, e.prototype.makeMsg = function (e, t, n) {
            return t > 0 && t <= 99 ? (Array.prototype.forEach.call(this.queryAll(".topnavdownulmsgul em"), function (e, r) {
                r == n && (e.innerHTML = t, e.classList.add("msg_num"))
            }), e += t) : t > 99 ? (0 == this.hasClass("topnavdownulmsgul", "icon_more_msg") && Array.prototype.forEach.call(this.queryAll(".topnavdownulmsgul em"), function (e, t) {
                t == n && (e.innerHTML = "", e.classList.remove("msg_num"), e.classList.add("icon_more_msg"))
            }), e += t) : 0 == t && Array.prototype.forEach.call(this.queryAll(".topnavdownulmsgul em"), function (e, t) {
                t == n && (e.innerHTML = "", e.classList.remove("msg_num"))
            }), e
        }, e.prototype.popmsgGet = function (e) {
            o.send("message/api/UserMessage/UserMessageCount", {}).then(function (t) {
                e && e(t)
            })["catch"](function (e) {
                console.log(e)
            })
        }, e.prototype.hasClass = function (e, t) {
            var n = e.split(/\s+/);
            for (var r in n) if (n[r] == t) return !0;
            return !1
        }, e
    }();
    t.MakeHeader = s
}, function (e, t) {
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.isFromBaidualaddin = t.loadImg = t.photoZoom = t.share = t.addShareJS = t.setRemUnit = t.formatTime = t.throttle = t.params = void 0;
    var r = n(3);

    function o() {
        return new Promise(function (e, t) {
            setTimeout(function () {
                r.loadResource("//gbfek.dfcfw.com/libs/jquery/1.8.3/jquery.min.js", function () {
                    r.loadResource("//gbfek.dfcfw.com/libs/share/share.js", function () {
                        window.$.fn.share ? e() : t("share.js has failed to load.")
                    }, !0), r.loadResource("//gbfek.dfcfw.com/libs/share/share2.css", function () {
                    }, !0)
                }, !0)
            }, 100)
        })
    }

    t.params = function (e) {
        return Object.keys(e).map(function (t) {
            return t + "=" + encodeURIComponent(e[t])
        }).join("&")
    }, t.throttle = function (e, t, n) {
    }, t.formatTime = function (e) {
        var t = new Date, n = t.getFullYear(), r = t.getMonth() + 1, o = t.getDate(),
            i = new RegExp(/(.+?)-(.+?)-(.+?) (.+?):(.+?):(.+?)/, "img"),
            a = e.replace(i, "$1,$2,$3,$4,$5,$6").split(",");
        return n == Number(a[0]) ? r == Number(a[1]) && o == Number(a[2]) ? "今天 " + a[3] + ":" + a[5] : a[1] + "-" + a[2] + " " + a[3] + ":" + a[5] : a[0] + "-" + a[1] + "-" + a[2] + " " + a[3] + ":" + a[5]
    }, t.setRemUnit = function () {
        var e = document.documentElement, t = 100 * e.clientWidth / 375;
        e.style.fontSize = t + "px"
    }, t.addShareJS = o, t.share = function (e, t, n, r, i) {
        void 0 === i && (i = ["wechat", "moment", "qzone", "weibo"]), o().then(function () {
            (0, window.$)(r).share({
                channels: i,
                pageShare: !0,
                shareData: {
                    title: e,
                    desc: t,
                    link: n,
                    from: "东方财富网移动版",
                    imgUrl: "https://gbfek.dfcfw.com/project/gubawap/img/wechatlogo2.png",
                    fail: function (e) {
                        alert(JSON.stringify(e))
                    }
                },
                custom: {qzone: {desc: ""}}
            })
        })
    }, t.photoZoom = function (e, t, n) {
        void 0 === t && (t = !0), void 0 === n && (n = ""), r.loadResource("//gbfek.dfcfw.com/project/gubawap/photoZoom/photoZoom.js", function () {
            window.photoZoom && new (0, window.photoZoom)(e, t, n).init()
        }, !0)
    }, t.loadImg = function (e) {
        var t = new Image;
        return t.src = e, t
    }, t.isFromBaidualaddin = function () {
        return window.location.href.toLowerCase().indexOf("from=baidualaddin") > 0
    }
}, function (e, t) {
    var n, r, o = o || function (e, t) {
        var n = {}, r = n.lib = {}, o = r.Base = function () {
            function e() {
            }

            return {
                extend: function (t) {
                    e.prototype = this;
                    var n = new e;
                    return t && n.mixIn(t), n.$super = this, n
                }, create: function () {
                    var e = this.extend();
                    return e.init.apply(e, arguments), e
                }, init: function () {
                }, mixIn: function (e) {
                    for (var t in e) e.hasOwnProperty(t) && (this[t] = e[t]);
                    e.hasOwnProperty("toString") && (this.toString = e.toString)
                }, clone: function () {
                    return this.$super.extend(this)
                }
            }
        }(), i = r.WordArray = o.extend({
            init: function (e, t) {
                e = this.words = e || [], this.sigBytes = void 0 != t ? t : 4 * e.length
            }, toString: function (e) {
                return (e || s).stringify(this)
            }, concat: function (e) {
                var t = this.words, n = e.words, r = this.sigBytes;
                e = e.sigBytes;
                if (this.clamp(), r % 4) for (var o = 0; o < e; o++) t[r + o >>> 2] |= (n[o >>> 2] >>> 24 - o % 4 * 8 & 255) << 24 - (r + o) % 4 * 8; else if (65535 < n.length) for (o = 0; o < e; o += 4) t[r + o >>> 2] = n[o >>> 2]; else t.push.apply(t, n);
                return this.sigBytes += e, this
            }, clamp: function () {
                var t = this.words, n = this.sigBytes;
                t[n >>> 2] &= 4294967295 << 32 - n % 4 * 8, t.length = e.ceil(n / 4)
            }, clone: function () {
                var e = o.clone.call(this);
                return e.words = this.words.slice(0), e
            }, random: function (t) {
                for (var n = [], r = 0; r < t; r += 4) n.push(4294967296 * e.random() | 0);
                return i.create(n, t)
            }
        }), a = n.enc = {}, s = a.Hex = {
            stringify: function (e) {
                for (var t = e.words, n = (e = e.sigBytes, []), r = 0; r < e; r++) {
                    var o = t[r >>> 2] >>> 24 - r % 4 * 8 & 255;
                    n.push((o >>> 4).toString(16)), n.push((15 & o).toString(16))
                }
                return n.join("")
            }, parse: function (e) {
                for (var t = e.length, n = [], r = 0; r < t; r += 2) n[r >>> 3] |= parseInt(e.substr(r, 2), 16) << 24 - r % 8 * 4;
                return i.create(n, t / 2)
            }
        }, c = a.Latin1 = {
            stringify: function (e) {
                for (var t = e.words, n = (e = e.sigBytes, []), r = 0; r < e; r++) n.push(String.fromCharCode(t[r >>> 2] >>> 24 - r % 4 * 8 & 255));
                return n.join("")
            }, parse: function (e) {
                for (var t = e.length, n = [], r = 0; r < t; r++) n[r >>> 2] |= (255 & e.charCodeAt(r)) << 24 - r % 4 * 8;
                return i.create(n, t)
            }
        }, l = a.Utf8 = {
            stringify: function (e) {
                try {
                    return decodeURIComponent(escape(c.stringify(e)))
                } catch (e) {
                    throw Error("Malformed UTF-8 data")
                }
            }, parse: function (e) {
                return c.parse(unescape(encodeURIComponent(e)))
            }
        }, u = r.BufferedBlockAlgorithm = o.extend({
            reset: function () {
                this._data = i.create(), this._nDataBytes = 0
            }, _append: function (e) {
                "string" == typeof e && (e = l.parse(e)), this._data.concat(e), this._nDataBytes += e.sigBytes
            }, _process: function (t) {
                var n = this._data, r = n.words, o = n.sigBytes, a = this.blockSize, s = o / (4 * a);
                t = (s = t ? e.ceil(s) : e.max((0 | s) - this._minBufferSize, 0)) * a, o = e.min(4 * t, o);
                if (t) {
                    for (var c = 0; c < t; c += a) this._doProcessBlock(r, c);
                    c = r.splice(0, t), n.sigBytes -= o
                }
                return i.create(c, o)
            }, clone: function () {
                var e = o.clone.call(this);
                return e._data = this._data.clone(), e
            }, _minBufferSize: 0
        });
        r.Hasher = u.extend({
            init: function () {
                this.reset()
            }, reset: function () {
                u.reset.call(this), this._doReset()
            }, update: function (e) {
                return this._append(e), this._process(), this
            }, finalize: function (e) {
                return e && this._append(e), this._doFinalize(), this._hash
            }, clone: function () {
                var e = u.clone.call(this);
                return e._hash = this._hash.clone(), e
            }, blockSize: 16, _createHelper: function (e) {
                return function (t, n) {
                    return e.create(n).finalize(t)
                }
            }, _createHmacHelper: function (e) {
                return function (t, n) {
                    return d.HMAC.create(e, n).finalize(t)
                }
            }
        });
        var d = n.algo = {};
        return n
    }(Math);
    r = (n = o).lib.WordArray, n.enc.Base64 = {
        stringify: function (e) {
            var t = e.words, n = e.sigBytes, r = this._map;
            e.clamp(), e = [];
            for (var o = 0; o < n; o += 3) for (var i = (t[o >>> 2] >>> 24 - o % 4 * 8 & 255) << 16 | (t[o + 1 >>> 2] >>> 24 - (o + 1) % 4 * 8 & 255) << 8 | t[o + 2 >>> 2] >>> 24 - (o + 2) % 4 * 8 & 255, a = 0; 4 > a && o + .75 * a < n; a++) e.push(r.charAt(i >>> 6 * (3 - a) & 63));
            if (t = r.charAt(64)) for (; e.length % 4;) e.push(t);
            return e.join("")
        }, parse: function (e) {
            var t = (e = e.replace(/\s/g, "")).length, n = this._map;
            (o = n.charAt(64)) && -1 != (o = e.indexOf(o)) && (t = o);
            for (var o = [], i = 0, a = 0; a < t; a++) if (a % 4) {
                var s = n.indexOf(e.charAt(a - 1)) << a % 4 * 2, c = n.indexOf(e.charAt(a)) >>> 6 - a % 4 * 2;
                o[i >>> 2] |= (s | c) << 24 - i % 4 * 8, i++
            }
            return r.create(o, i)
        }, _map: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    }, function (e) {
        function t(e, t, n, r, o, i, a) {
            return ((e = e + (t & n | ~t & r) + o + a) << i | e >>> 32 - i) + t
        }

        function n(e, t, n, r, o, i, a) {
            return ((e = e + (t & r | n & ~r) + o + a) << i | e >>> 32 - i) + t
        }

        function r(e, t, n, r, o, i, a) {
            return ((e = e + (t ^ n ^ r) + o + a) << i | e >>> 32 - i) + t
        }

        function i(e, t, n, r, o, i, a) {
            return ((e = e + (n ^ (t | ~r)) + o + a) << i | e >>> 32 - i) + t
        }

        var a = o, s = (c = a.lib).WordArray, c = c.Hasher, l = a.algo, u = [];
        !function () {
            for (var t = 0; 64 > t; t++) u[t] = 4294967296 * e.abs(e.sin(t + 1)) | 0
        }(), l = l.M = c.extend({
            _doReset: function () {
                this._hash = s.create([1732584193, 4023233417, 2562383102, 271733878])
            }, _doProcessBlock: function (e, o) {
                for (var a = 0; 16 > a; a++) {
                    var s = e[c = o + a];
                    e[c] = 16711935 & (s << 8 | s >>> 24) | 4278255360 & (s << 24 | s >>> 8)
                }
                s = (c = this._hash.words)[0];
                var c, l = c[1], d = c[2], f = c[3];
                for (a = 0; 64 > a; a += 4) 16 > a ? l = t(l, d = t(d, f = t(f, s = t(s, l, d, f, e[o + a], 7, u[a]), l, d, e[o + a + 1], 12, u[a + 1]), s, l, e[o + a + 2], 17, u[a + 2]), f, s, e[o + a + 3], 22, u[a + 3]) : 32 > a ? l = n(l, d = n(d, f = n(f, s = n(s, l, d, f, e[o + (a + 1) % 16], 5, u[a]), l, d, e[o + (a + 6) % 16], 9, u[a + 1]), s, l, e[o + (a + 11) % 16], 14, u[a + 2]), f, s, e[o + a % 16], 20, u[a + 3]) : 48 > a ? l = r(l, d = r(d, f = r(f, s = r(s, l, d, f, e[o + (3 * a + 5) % 16], 4, u[a]), l, d, e[o + (3 * a + 8) % 16], 11, u[a + 1]), s, l, e[o + (3 * a + 11) % 16], 16, u[a + 2]), f, s, e[o + (3 * a + 14) % 16], 23, u[a + 3]) : l = i(l, d = i(d, f = i(f, s = i(s, l, d, f, e[o + 3 * a % 16], 6, u[a]), l, d, e[o + (3 * a + 7) % 16], 10, u[a + 1]), s, l, e[o + (3 * a + 14) % 16], 15, u[a + 2]), f, s, e[o + (3 * a + 5) % 16], 21, u[a + 3]);
                c[0] = c[0] + s | 0, c[1] = c[1] + l | 0, c[2] = c[2] + d | 0, c[3] = c[3] + f | 0
            }, _doFinalize: function () {
                var e = this._data, t = e.words, n = 8 * this._nDataBytes, r = 8 * e.sigBytes;
                for (t[r >>> 5] |= 128 << 24 - r % 32, t[14 + (r + 64 >>> 9 << 4)] = 16711935 & (n << 8 | n >>> 24) | 4278255360 & (n << 24 | n >>> 8), e.sigBytes = 4 * (t.length + 1), this._process(), e = this._hash.words, t = 0; 4 > t; t++) n = e[t], e[t] = 16711935 & (n << 8 | n >>> 24) | 4278255360 & (n << 24 | n >>> 8)
            }
        }), a.M = c._createHelper(l), a.HmacMD5 = c._createHmacHelper(l)
    }(Math), window.CJS = o, function () {
        var e, t = o, n = (e = t.lib).Base, r = e.WordArray, i = (e = t.algo).EvpKDF = n.extend({
            cfg: n.extend({keySize: 4, hasher: e.MD5, iterations: 1}),
            init: function (e) {
                this.cfg = this.cfg.extend(e)
            },
            compute: function (e, t) {
                for (var n = (s = this.cfg).hasher.create(), o = r.create(), i = o.words, a = s.keySize, s = s.iterations; i.length < a;) {
                    c && n.update(c);
                    var c = n.update(e).finalize(t);
                    n.reset();
                    for (var l = 1; l < s; l++) c = n.finalize(c), n.reset();
                    o.concat(c)
                }
                return o.sigBytes = 4 * a, o
            }
        });
        t.EvpKDF = function (e, t, n) {
            return i.create(n).compute(e, t)
        }
    }();
    var i = o.M("getUtilsFromFile"), a = CJS.enc.Utf8.parse(i);
    o.lib.Cipher || function (e) {
        var t = (h = o).lib, n = t.Base, r = t.WordArray, i = t.BufferedBlockAlgorithm, a = h.enc.Base64,
            s = h.algo.EvpKDF, c = t.Cipher = i.extend({
                cfg: n.extend(), createEncryptor: function (e, t) {
                    return this.create(this._ENC_XFORM_MODE, e, t)
                }, createDecryptor: function (e, t) {
                    return this.create(this._DEC_XFORM_MODE, e, t)
                }, init: function (e, t, n) {
                    this.cfg = this.cfg.extend(n), this._xformMode = e, this._key = t, this.reset()
                }, reset: function () {
                    i.reset.call(this), this._doReset()
                }, process: function (e) {
                    return this._append(e), this._process()
                }, finalize: function (e) {
                    return e && this._append(e), this._doFinalize()
                }, keySize: 4, ivSize: 4, _ENC_XFORM_MODE: 1, _DEC_XFORM_MODE: 2, _createHelper: function (e) {
                    return {
                        e: function (t, n, r) {
                            return ("string" == typeof n ? m : p).encrypt(e, t, n, r)
                        }, d: function (t, n, r) {
                            return ("string" == typeof n ? m : p).d(e, t, n, r)
                        }
                    }
                }
            });
        t.StreamCipher = c.extend({
            _doFinalize: function () {
                return this._process(!0)
            }, blockSize: 1
        });
        var l = h.mode = {}, u = t.BlockCipherMode = n.extend({
            createEncryptor: function (e, t) {
                return this.Encryptor.create(e, t)
            }, createDecryptor: function (e, t) {
                return this.Decryptor.create(e, t)
            }, init: function (e, t) {
                this._cipher = e, this._iv = t
            }
        }), d = (l = l.CBC = function () {
            function t(t, n, r) {
                var o = this._iv;
                o ? this._iv = e : o = this._prevBlock;
                for (var i = 0; i < r; i++) t[n + i] ^= o[i]
            }

            var n = u.extend();
            return n.Encryptor = n.extend({
                processBlock: function (e, n) {
                    var r = this._cipher, o = r.blockSize;
                    t.call(this, e, n, o), r.encryptBlock(e, n), this._prevBlock = e.slice(n, n + o)
                }
            }), n.Decryptor = n.extend({
                processBlock: function (e, n) {
                    var r = this._cipher, o = r.blockSize, i = e.slice(n, n + o);
                    r.decryptBlock(e, n), t.call(this, e, n, o), this._prevBlock = i
                }
            }), n
        }(), (h.pad = {}).Pkcs7 = {
            pad: function (e, t) {
                for (var n, o = (n = (n = 4 * t) - e.sigBytes % n) << 24 | n << 16 | n << 8 | n, i = [], a = 0; a < n; a += 4) i.push(o);
                n = r.create(i, n), e.concat(n)
            }, unpad: function (e) {
                e.sigBytes -= 255 & e.words[e.sigBytes - 1 >>> 2]
            }
        });
        t.BlockCipher = c.extend({
            cfg: c.cfg.extend({mode: l, padding: d}), reset: function () {
                c.reset.call(this);
                var e = (t = this.cfg).iv, t = t.mode;
                if (this._xformMode == this._ENC_XFORM_MODE) var n = t.createEncryptor; else n = t.createDecryptor, this._minBufferSize = 1;
                this._mode = n.call(t, this, e && e.words)
            }, _doProcessBlock: function (e, t) {
                this._mode.processBlock(e, t)
            }, _doFinalize: function () {
                var e = this.cfg.padding;
                if (this._xformMode == this._ENC_XFORM_MODE) {
                    e.pad(this._data, this.blockSize);
                    var t = this._process(!0)
                } else t = this._process(!0), e.unpad(t);
                return t
            }, blockSize: 4
        });
        var f = t.CipherParams = n.extend({
            init: function (e) {
                this.mixIn(e)
            }, toString: function (e) {
                return (e || this.formatter).stringify(this)
            }
        }), p = (l = (h.format = {}).OpenSSL = {
            stringify: function (e) {
                var t = e.ciphertext;
                return (t = ((e = e.salt) ? r.create([1398893684, 1701076831]).concat(e).concat(t) : t).toString(a)).replace(/(.{64})/g, "$1\n")
            }, parse: function (e) {
                var t = (e = a.parse(e)).words;
                if (1398893684 == t[0] && 1701076831 == t[1]) {
                    var n = r.create(t.slice(2, 4));
                    t.splice(0, 4), e.sigBytes -= 16
                }
                return f.create({ciphertext: e, salt: n})
            }
        }, t.SerializableCipher = n.extend({
            cfg: n.extend({format: l}), e: function (e, t, n, r) {
                r = this.cfg.extend(r), t = (o = e.createEncryptor(n, r)).finalize(t);
                var o = o.cfg;
                return f.create({
                    ciphertext: t,
                    key: n,
                    iv: o.iv,
                    algorithm: e,
                    mode: o.mode,
                    padding: o.padding,
                    blockSize: e.blockSize,
                    formatter: r.format
                })
            }, d: function (e, t, n, r) {
                return r = this.cfg.extend(r), t = this._parse(t, r.format), e.createDecryptor(n, r).finalize(t.ciphertext)
            }, _parse: function (e, t) {
                return "string" == typeof e ? t.parse(e) : e
            }
        })), h = (h.kdf = {}).OpenSSL = {
            compute: function (e, t, n, o) {
                return o || (o = r.random(8)), e = s.create({keySize: t + n}).compute(e, o), n = r.create(e.words.slice(t), 4 * n), e.sigBytes = 4 * t, f.create({
                    key: e,
                    iv: n,
                    salt: o
                })
            }
        }, m = t.PasswordBasedCipher = p.extend({
            cfg: p.cfg.extend({kdf: h}), e: function (e, t, n, r) {
                return n = (r = this.cfg.extend(r)).kdf.compute(n, e.keySize, e.ivSize), r.iv = n.iv, (e = p.encrypt.call(this, e, t, n.key, r)).mixIn(n), e
            }, d: function (e, t, n, r) {
                return r = this.cfg.extend(r), t = this._parse(t, r.format), n = r.kdf.compute(n, e.keySize, e.ivSize, t.salt), r.iv = n.iv, p.decrypt.call(this, e, t, n.key, r)
            }
        })
    }();
    var s = o.enc.Utf8.parse("getClassFromFile");
    !function () {
        var e = o, t = e.lib.BlockCipher, n = e.algo, r = [], i = [], a = [], s = [], c = [], l = [], u = [], d = [],
            f = [], p = [];
        !function () {
            for (var e = [], t = 0; 256 > t; t++) e[t] = 128 > t ? t << 1 : t << 1 ^ 283;
            var n = 0, o = 0;
            for (t = 0; 256 > t; t++) {
                var h = (h = o ^ o << 1 ^ o << 2 ^ o << 3 ^ o << 4) >>> 8 ^ 255 & h ^ 99;
                r[n] = h, i[h] = n;
                var m = e[n], g = e[m], v = e[g], y = 257 * e[h] ^ 16843008 * h;
                a[n] = y << 24 | y >>> 8, s[n] = y << 16 | y >>> 16, c[n] = y << 8 | y >>> 24, l[n] = y, y = 16843009 * v ^ 65537 * g ^ 257 * m ^ 16843008 * n, u[h] = y << 24 | y >>> 8, d[h] = y << 16 | y >>> 16, f[h] = y << 8 | y >>> 24, p[h] = y, n ? (n = m ^ e[e[e[v ^ m]]], o ^= e[e[o]]) : n = o = 1
            }
        }(), window.Crypto = null, CJS.mode.ECB = CJS.mode.CBC, CJS.pad.ZERO = CJS.pad.Pkcs7;
        var h = [0, 1, 2, 4, 8, 16, 32, 64, 128, 27, 54];
        n = n.AlocalStorage = t.extend({
            _doReset: function () {
                for (var e = (n = this._key).words, t = n.sigBytes / 4, n = 4 * ((this._nRounds = t + 6) + 1), o = this._keySchedule = [], i = 0; i < n; i++) if (i < t) o[i] = e[i]; else {
                    var a = o[i - 1];
                    i % t ? 6 < t && 4 == i % t && (a = r[a >>> 24] << 24 | r[a >>> 16 & 255] << 16 | r[a >>> 8 & 255] << 8 | r[255 & a]) : (a = r[(a = a << 8 | a >>> 24) >>> 24] << 24 | r[a >>> 16 & 255] << 16 | r[a >>> 8 & 255] << 8 | r[255 & a], a ^= h[i / t | 0] << 24), o[i] = o[i - t] ^ a
                }
                for (e = this._invKeySchedule = [], t = 0; t < n; t++) i = n - t, a = t % 4 ? o[i] : o[i - 4], e[t] = 4 > t || 4 >= i ? a : u[r[a >>> 24]] ^ d[r[a >>> 16 & 255]] ^ f[r[a >>> 8 & 255]] ^ p[r[255 & a]]
            }, encryptBlock: function (e, t) {
                this._doCryptBlock(e, t, this._keySchedule, a, s, c, l, r)
            }, decryptBlock: function (e, t) {
                var n = e[t + 1];
                e[t + 1] = e[t + 3], e[t + 3] = n, this._doCryptBlock(e, t, this._invKeySchedule, u, d, f, p, i), n = e[t + 1], e[t + 1] = e[t + 3], e[t + 3] = n
            }, _doCryptBlock: function (e, t, n, r, o, i, a, s) {
                for (var c = this._nRounds, l = e[t] ^ n[0], u = e[t + 1] ^ n[1], d = e[t + 2] ^ n[2], f = e[t + 3] ^ n[3], p = 4, h = 1; h < c; h++) {
                    var m = r[l >>> 24] ^ o[u >>> 16 & 255] ^ i[d >>> 8 & 255] ^ a[255 & f] ^ n[p++],
                        g = r[u >>> 24] ^ o[d >>> 16 & 255] ^ i[f >>> 8 & 255] ^ a[255 & l] ^ n[p++],
                        v = r[d >>> 24] ^ o[f >>> 16 & 255] ^ i[l >>> 8 & 255] ^ a[255 & u] ^ n[p++];
                    f = r[f >>> 24] ^ o[l >>> 16 & 255] ^ i[u >>> 8 & 255] ^ a[255 & d] ^ n[p++], l = m, u = g, d = v
                }
                m = (s[l >>> 24] << 24 | s[u >>> 16 & 255] << 16 | s[d >>> 8 & 255] << 8 | s[255 & f]) ^ n[p++], g = (s[u >>> 24] << 24 | s[d >>> 16 & 255] << 16 | s[f >>> 8 & 255] << 8 | s[255 & l]) ^ n[p++], v = (s[d >>> 24] << 24 | s[f >>> 16 & 255] << 16 | s[l >>> 8 & 255] << 8 | s[255 & u]) ^ n[p++], f = (s[f >>> 24] << 24 | s[l >>> 16 & 255] << 16 | s[u >>> 8 & 255] << 8 | s[255 & d]) ^ n[p++], e[t] = m, e[t + 1] = g, e[t + 2] = v, e[t + 3] = f
            }, keySize: 8
        });
        e.AlocalStorage = t._createHelper(n)
    }(), o.pad.ZeroPadding = {
        pad: function (e, t) {
            var n = 4 * t;
            e.clamp(), e.sigBytes += n - (e.sigBytes % n || n)
        }, unpad: function (e) {
            for (var t = e.words, n = e.sigBytes - 1; !(t[n >>> 2] >>> 24 - n % 4 * 8 & 255);) n--;
            e.sigBytes = n + 1
        }
    }, window.d_key = "wijrKSCUiQuGbrwsgyEMyIx7Uogmfe85", window.d_iv = "ho6KJIIz9WV7nozZl5fVnG7MtDUcSUB1",
        window.d = function (e) {
            return CJS.AlocalStorage.d(e, a, {
                iv: s,
                mode: o.mode.CBC,
                padding: o.pad.Pkcs7
            }).toString(CJS.enc.Utf8).toString()
        }
}, function (e, t, n) {
    var r = n(2);
    e.exports = (r["default"] || r).template({
        compiler: [8, ">= 4.3.0"], main: function (e, t, n, r, o) {
            return '\t<div id="top-user"> \r\n\t\t<li class="user_info header_username">\r\n\t\t\t<em class="user_name"></em>\r\n\t\t\t<em class="trg icon_down_s"></em>\r\n\t\t\t<ul class="slide_down">\r\n\t\t\t\t<li><a href="https://passport2.eastmoney.com/pub/basicinfo" target="_blank">个人设置</a></li>\r\n\t\t\t\t<li><a href="https://passport2.eastmoney.com/pub/changepassword" target="_blank">修改密码</a></li>\r\n\t\t\t\t<li><a id="set_pry" href="//i.eastmoney.com/privacy" target="_blank">隐私设置</a></li>\r\n\t\t\t\t<li><a id="set_msg" href="//i.eastmoney.com/information" target="_blank">消息设置</a></li>\r\n\t\t\t\t<li><a href="//i.eastmoney.com/qianbao.html" target="_blank">我的钱包</a></li>\r\n\t\t\t\t<li id="log-out"><a href="javascript:;" target="_self">退出</a></li>\r\n\t\t\t</ul>\r\n\t\t</li>\r\n\t\t<li class="user_info iguba" >\r\n\t\t\t<em class="user_name"></em>\r\n\t\t\t<em class="trg icon_down_s"></em>\r\n\t\t\t<ul class="slide_down">\r\n\t\t\t\t<li><a id="home" href="//i.eastmoney.com/">我关注的股</a></li>\r\n\t\t\t\t<li><a id="myfollper" href="//i.eastmoney.com/following">我关注的人</a></li>\r\n\t\t\t\t<li><a id="myart" href="//i.eastmoney.com/myarts">我的发言</a></li>\r\n\t\t\t\t<li><a id="myfav" href="//i.eastmoney.com/collection\r\n">我的收藏</a></li>\r\n\t\t\t</ul>\r\n\t\t</li>\r\n\t\t<li class="user_info">\r\n\t\t\t<em class="top_name my_msg">我的消息<i></i></em>\r\n\t\t\t<em class="trg icon_down_s"></em>\r\n\t\t\t<ul class="slide_down topnavdownulmsgul">\r\n\t\t\t\t<li><a id="replyme" href="//i.eastmoney.com/replyme">&nbsp;&nbsp;查看新回复<em></em></a></li>\r\n\t\t\t\t<li><a id="atmereply" href="//i.eastmoney.com/atme_zhutie">&nbsp;&nbsp;查看新@我的<em></em></a></li>\r\n\t\t\t\t<li><a id="myfans" href="//i.eastmoney.com/fans">&nbsp;&nbsp;查看新粉丝<em></em></a></li>\r\n\t\t\t\t<li style="display: none;"><a id="my_wdmsg" href="//i.eastmoney.com/myinfo">&nbsp;&nbsp;查看问答消息<em></em></a></li>\r\n\t\t\t</ul>\r\n\t\t</li>\r\n\t</div>\r\n\t\r\n\t<li id="header-login" >\r\n\t\t<span>您好，欢迎来股吧！ </span>\r\n\t\t<a id="header_btn_login" href="javascript:;" target="_self">\r\n\t\t\t<strong>登录/注册</strong>\r\n\t\t</a>\r\n\t\t\r\n\t</li>\r\n'
        }, useData: !0
    })
}, function (e, t, n) {
    "use strict";

    function r(e) {
        return e && e.__esModule ? e : {"default": e}
    }

    function o(e) {
        if (e && e.__esModule) return e;
        var t = {};
        if (null != e) for (var n in e) Object.prototype.hasOwnProperty.call(e, n) && (t[n] = e[n]);
        return t["default"] = e, t
    }

    t.__esModule = !0;
    var i = o(n(9)), a = r(n(38)), s = r(n(1)), c = o(n(0)), l = o(n(39)), u = r(n(41));

    function d() {
        var e = new i.HandlebarsEnvironment;
        return c.extend(e, i), e.SafeString = a["default"], e.Exception = s["default"], e.Utils = c, e.escapeExpression = c.escapeExpression, e.VM = l, e.template = function (t) {
            return l.template(t, e)
        }, e
    }

    var f = d();
    f.create = d, u["default"](f), f["default"] = f, t["default"] = f, e.exports = t["default"]
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0;
    var r = n(0);
    t["default"] = function (e) {
        e.registerHelper("blockHelperMissing", function (t, n) {
            var o = n.inverse, i = n.fn;
            if (!0 === t) return i(this);
            if (!1 === t || null == t) return o(this);
            if (r.isArray(t)) return t.length > 0 ? (n.ids && (n.ids = [n.name]), e.helpers.each(t, n)) : o(this);
            if (n.data && n.ids) {
                var a = r.createFrame(n.data);
                a.contextPath = r.appendContextPath(n.data.contextPath, n.name), n = {data: a}
            }
            return i(t, n)
        })
    }, e.exports = t["default"]
}, function (e, t, n) {
    "use strict";
    (function (r) {
        t.__esModule = !0;
        var o, i = n(0), a = n(1), s = (o = a) && o.__esModule ? o : {"default": o};
        t["default"] = function (e) {
            e.registerHelper("each", function (e, t) {
                if (!t) throw new s["default"]("Must pass iterator to #each");
                var n, o = t.fn, a = t.inverse, c = 0, l = "", u = undefined, d = undefined;

                function f(t, n, r) {
                    u && (u.key = t, u.index = n, u.first = 0 === n, u.last = !!r, d && (u.contextPath = d + t)), l += o(e[t], {
                        data: u,
                        blockParams: i.blockParams([e[t], t], [d + t, null])
                    })
                }

                if (t.data && t.ids && (d = i.appendContextPath(t.data.contextPath, t.ids[0]) + "."), i.isFunction(e) && (e = e.call(this)), t.data && (u = i.createFrame(t.data)), e && "object" == typeof e) if (i.isArray(e)) for (var p = e.length; c < p; c++) c in e && f(c, c, c === e.length - 1); else if (r.Symbol && e[r.Symbol.iterator]) {
                    for (var h = [], m = e[r.Symbol.iterator](), g = m.next(); !g.done; g = m.next()) h.push(g.value);
                    for (p = (e = h).length; c < p; c++) f(c, c, c === e.length - 1)
                } else n = undefined, Object.keys(e).forEach(function (e) {
                    n !== undefined && f(n, c - 1), n = e, c++
                }), n !== undefined && f(n, c - 1, !0);
                return 0 === c && (l = a(this)), l
            })
        }, e.exports = t["default"]
    }).call(t, n(4))
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0;
    var r, o = n(1), i = (r = o) && r.__esModule ? r : {"default": r};
    t["default"] = function (e) {
        e.registerHelper("helperMissing", function () {
            if (1 === arguments.length) return undefined;
            throw new i["default"]('Missing helper: "' + arguments[arguments.length - 1].name + '"')
        })
    }, e.exports = t["default"]
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0;
    var r, o = n(0), i = n(1), a = (r = i) && r.__esModule ? r : {"default": r};
    t["default"] = function (e) {
        e.registerHelper("if", function (e, t) {
            if (2 != arguments.length) throw new a["default"]("#if requires exactly one argument");
            return o.isFunction(e) && (e = e.call(this)), !t.hash.includeZero && !e || o.isEmpty(e) ? t.inverse(this) : t.fn(this)
        }), e.registerHelper("unless", function (t, n) {
            if (2 != arguments.length) throw new a["default"]("#unless requires exactly one argument");
            return e.helpers["if"].call(this, t, {fn: n.inverse, inverse: n.fn, hash: n.hash})
        })
    }, e.exports = t["default"]
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0, t["default"] = function (e) {
        e.registerHelper("log", function () {
            for (var t = [undefined], n = arguments[arguments.length - 1], r = 0; r < arguments.length - 1; r++) t.push(arguments[r]);
            var o = 1;
            null != n.hash.level ? o = n.hash.level : n.data && null != n.data.level && (o = n.data.level), t[0] = o, e.log.apply(e, t)
        })
    }, e.exports = t["default"]
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0, t["default"] = function (e) {
        e.registerHelper("lookup", function (e, t, n) {
            return e ? n.lookupProperty(e, t) : e
        })
    }, e.exports = t["default"]
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0;
    var r, o = n(0), i = n(1), a = (r = i) && r.__esModule ? r : {"default": r};
    t["default"] = function (e) {
        e.registerHelper("with", function (e, t) {
            if (2 != arguments.length) throw new a["default"]("#with requires exactly one argument");
            o.isFunction(e) && (e = e.call(this));
            var n = t.fn;
            if (o.isEmpty(e)) return t.inverse(this);
            var r = t.data;
            return t.data && t.ids && ((r = o.createFrame(t.data)).contextPath = o.appendContextPath(t.data.contextPath, t.ids[0])), n(e, {
                data: r,
                blockParams: o.blockParams([e], [r && r.contextPath])
            })
        })
    }, e.exports = t["default"]
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0, t.registerDefaultDecorators = function (e) {
        i["default"](e)
    };
    var r, o = n(36), i = (r = o) && r.__esModule ? r : {"default": r}
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0;
    var r = n(0);
    t["default"] = function (e) {
        e.registerDecorator("inline", function (e, t, n, o) {
            var i = e;
            return t.partials || (t.partials = {}, i = function (o, i) {
                var a = n.partials;
                n.partials = r.extend({}, a, t.partials);
                var s = e(o, i);
                return n.partials = a, s
            }), t.partials[o.args[0]] = o.fn, i
        })
    }, e.exports = t["default"]
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0, t.createNewLookupObject = function () {
        for (var e = arguments.length, t = Array(e), n = 0; n < e; n++) t[n] = arguments[n];
        return r.extend.apply(undefined, [Object.create(null)].concat(t))
    };
    var r = n(0)
}, function (e, t, n) {
    "use strict";

    function r(e) {
        this.string = e
    }

    t.__esModule = !0, r.prototype.toString = r.prototype.toHTML = function () {
        return "" + this.string
    }, t["default"] = r, e.exports = t["default"]
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0, t.checkRevision = function (e) {
        var t = e && e[0] || 1, n = s.COMPILER_REVISION;
        if (t >= s.LAST_COMPATIBLE_COMPILER_REVISION && t <= s.COMPILER_REVISION) return;
        if (t < s.LAST_COMPATIBLE_COMPILER_REVISION) {
            var r = s.REVISION_CHANGES[n], o = s.REVISION_CHANGES[t];
            throw new a["default"]("Template was precompiled with an older version of Handlebars than the current runtime. Please update your precompiler to a newer version (" + r + ") or downgrade your runtime to an older version (" + o + ").")
        }
        throw new a["default"]("Template was precompiled with a newer version of Handlebars than the current runtime. Please update your runtime to a newer version (" + e[1] + ").")
    }, t.template = function (e, t) {
        if (!t) throw new a["default"]("No environment passed to template");
        if (!e || !e.main) throw new a["default"]("Unknown template object: " + typeof e);
        e.main.decorator = e.main_d, t.VM.checkRevision(e.compiler);
        var n = e.compiler && 7 === e.compiler[0];
        var r = {
            strict: function (e, t, n) {
                if (!(e && t in e)) throw new a["default"]('"' + t + '" not defined in ' + e, {loc: n});
                return r.lookupProperty(e, t)
            }, lookupProperty: function (e, t) {
                var n = e[t];
                return null == n ? n : Object.prototype.hasOwnProperty.call(e, t) ? n : u.resultIsAllowed(n, r.protoAccessControl, t) ? n : undefined
            }, lookup: function (e, t) {
                for (var n = e.length, o = 0; o < n; o++) {
                    var i = e[o] && r.lookupProperty(e[o], t);
                    if (null != i) return e[o][t]
                }
            }, lambda: function (e, t) {
                return "function" == typeof e ? e.call(t) : e
            }, escapeExpression: o.escapeExpression, invokePartial: function (n, r, i) {
                i.hash && (r = o.extend({}, r, i.hash), i.ids && (i.ids[0] = !0));
                n = t.VM.resolvePartial.call(this, n, r, i);
                var s = o.extend({}, i, {hooks: this.hooks, protoAccessControl: this.protoAccessControl}),
                    c = t.VM.invokePartial.call(this, n, r, s);
                null == c && t.compile && (i.partials[i.name] = t.compile(n, e.compilerOptions, t), c = i.partials[i.name](r, s));
                if (null != c) {
                    if (i.indent) {
                        for (var l = c.split("\n"), u = 0, d = l.length; u < d && (l[u] || u + 1 !== d); u++) l[u] = i.indent + l[u];
                        c = l.join("\n")
                    }
                    return c
                }
                throw new a["default"]("The partial " + i.name + " could not be compiled when running in runtime-only mode")
            }, fn: function (t) {
                var n = e[t];
                return n.decorator = e[t + "_d"], n
            }, programs: [], program: function (e, t, n, r, o) {
                var i = this.programs[e], a = this.fn(e);
                return t || o || r || n ? i = d(this, e, a, t, n, r, o) : i || (i = this.programs[e] = d(this, e, a)), i
            }, data: function (e, t) {
                for (; e && t--;) e = e._parent;
                return e
            }, mergeIfNeeded: function (e, t) {
                var n = e || t;
                return e && t && e !== t && (n = o.extend({}, t, e)), n
            }, nullContext: Object.seal({}), noop: t.VM.noop, compilerInfo: e.compiler
        };

        function i(t) {
            var n = arguments.length <= 1 || arguments[1] === undefined ? {} : arguments[1], o = n.data;
            i._setup(n), !n.partial && e.useData && (o = function (e, t) {
                t && "root" in t || ((t = t ? s.createFrame(t) : {}).root = e);
                return t
            }(t, o));
            var a = undefined, c = e.useBlockParams ? [] : undefined;

            function l(t) {
                return "" + e.main(r, t, r.helpers, r.partials, o, c, a)
            }

            return e.useDepths && (a = n.depths ? t != n.depths[0] ? [t].concat(n.depths) : n.depths : [t]), (l = p(e.main, l, r, n.depths || [], o, c))(t, n)
        }

        return i.isTop = !0, i._setup = function (i) {
            if (i.partial) r.protoAccessControl = i.protoAccessControl, r.helpers = i.helpers, r.partials = i.partials, r.decorators = i.decorators, r.hooks = i.hooks; else {
                var a = o.extend({}, t.helpers, i.helpers);
                !function (e, t) {
                    Object.keys(e).forEach(function (n) {
                        var r = e[n];
                        e[n] = function (e, t) {
                            var n = t.lookupProperty;
                            return l.wrapHelper(e, function (e) {
                                return o.extend({lookupProperty: n}, e)
                            })
                        }(r, t)
                    })
                }(a, r), r.helpers = a, e.usePartial && (r.partials = r.mergeIfNeeded(i.partials, t.partials)), (e.usePartial || e.useDecorators) && (r.decorators = o.extend({}, t.decorators, i.decorators)), r.hooks = {}, r.protoAccessControl = u.createProtoAccessControl(i);
                var s = i.allowCallsToHelperMissing || n;
                c.moveHelperToHooks(r, "helperMissing", s), c.moveHelperToHooks(r, "blockHelperMissing", s)
            }
        }, i._child = function (t, n, o, i) {
            if (e.useBlockParams && !o) throw new a["default"]("must pass block params");
            if (e.useDepths && !i) throw new a["default"]("must pass parent depths");
            return d(r, t, e[t], n, 0, o, i)
        }, i
    }, t.wrapProgram = d, t.resolvePartial = function (e, t, n) {
        e ? e.call || n.name || (n.name = e, e = n.partials[e]) : e = "@partial-block" === n.name ? n.data["partial-block"] : n.partials[n.name];
        return e
    }, t.invokePartial = function (e, t, n) {
        var r = n.data && n.data["partial-block"];
        n.partial = !0, n.ids && (n.data.contextPath = n.ids[0] || n.data.contextPath);
        var i = undefined;
        n.fn && n.fn !== f && function () {
            n.data = s.createFrame(n.data);
            var e = n.fn;
            i = n.data["partial-block"] = function (t) {
                var n = arguments.length <= 1 || arguments[1] === undefined ? {} : arguments[1];
                return n.data = s.createFrame(n.data), n.data["partial-block"] = r, e(t, n)
            }, e.partials && (n.partials = o.extend({}, n.partials, e.partials))
        }();
        e === undefined && i && (e = i);
        if (e === undefined) throw new a["default"]("The partial " + n.name + " could not be found");
        if (e instanceof Function) return e(t, n)
    }, t.noop = f;
    var r, o = function (e) {
        if (e && e.__esModule) return e;
        var t = {};
        if (null != e) for (var n in e) Object.prototype.hasOwnProperty.call(e, n) && (t[n] = e[n]);
        return t["default"] = e, t
    }(n(0)), i = n(1), a = (r = i) && r.__esModule ? r : {"default": r}, s = n(9), c = n(10), l = n(40), u = n(12);

    function d(e, t, n, r, o, i, a) {
        function s(t) {
            var o = arguments.length <= 1 || arguments[1] === undefined ? {} : arguments[1], s = a;
            return !a || t == a[0] || t === e.nullContext && null === a[0] || (s = [t].concat(a)), n(e, t, e.helpers, e.partials, o.data || r, i && [o.blockParams].concat(i), s)
        }

        return (s = p(n, s, e, a, r, i)).program = t, s.depth = a ? a.length : 0, s.blockParams = o || 0, s
    }

    function f() {
        return ""
    }

    function p(e, t, n, r, i, a) {
        if (e.decorator) {
            var s = {};
            t = e.decorator(t, s, n, r && r[0], i, a, r), o.extend(t, s)
        }
        return t
    }
}, function (e, t, n) {
    "use strict";
    t.__esModule = !0, t.wrapHelper = function (e, t) {
        if ("function" != typeof e) return e;
        return function () {
            var n = arguments[arguments.length - 1];
            return arguments[arguments.length - 1] = t(n), e.apply(this, arguments)
        }
    }
}, function (e, t, n) {
    "use strict";
    (function (n) {
        t.__esModule = !0, t["default"] = function (e) {
            var t = void 0 !== n ? n : window, r = t.Handlebars;
            e.noConflict = function () {
                return t.Handlebars === e && (t.Handlebars = r), e
            }
        }, e.exports = t["default"]
    }).call(t, n(4))
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.searchHeader = void 0, n(43);
    var r = n(6), o = n(44), i = n(45), a = function () {
        function e() {
            this.query = function (e) {
                return document.querySelector(e)
            }, this.queryAll = function (e) {
                return document.querySelectorAll(e)
            }, this.init()
        }

        return e.prototype.init = function () {
            this.query("#searchHeader").innerHTML = o(), this.bindSearch(), i.launchIn(".historybox")
        }, e.prototype.bindSearch = function () {
            var e = this;
            r.loadScript("//emcharts.dfcfw.com/suggest/stocksuggest2017.min.js", function () {
                var t, n = new suggest2017({
                    inputid: "heat_search",
                    offset: {left: 0, top: 0},
                    width: 350,
                    gubacount: 5,
                    modules: ["stock"],
                    moveinput: "Code",
                    gubatable: !0,
                    showstocklink: !1,
                    showblank: !1,
                    placeholder: "请输入资产代码/名称/简拼",
                    filter: {securitytype: "1,2,25,19,7,20", status: 1},
                    onConfirmStock: function (t) {
                        console.log(t);
                        var r = t.stock.Code, o = t.stock.Name, i = t.stock.MktNum, a = t.stock.JYS;
                        return 0 != i && 1 != i && (r = a + "_" + r), e.query("#heat_search").value = r, e.query("#heat_search").setAttribute("code", r), e.query("#heat_search").setAttribute("name", o), window.open("/rank/stock?code=" + r, "_blank"), n.hide(), !1
                    }
                });
                (t = window.jQuery)("#heat_search").change(function () {
                    var e = t(".suggest2017:visible").length > 0, n = t(".suggest2017 .sg2017table tr").length > 0;
                    if (t(".suggest2017 .sg2017nof").length, n && e) {
                        var r = t(".suggest2017 .sg2017table tr:first").data("stockdata");
                        t("#heat_search").val(r.Code), t("#heat_search").attr("code", r.Code), t("#heat_search").attr("name", r.Name)
                    } else t("#heat_search").attr("code", 0), t("#heat_search").attr("name", 0)
                }), t("#search_btn").click(function () {
                    var e = t("#heat_search").attr("code");
                    0 != e && e ? window.open("/rank/stock?code=" + e, "_blank") : alert("请输入正确的资产代码")
                })
            }, !0)
        }, e
    }();
    t.searchHeader = a
}, function (e, t) {
}, function (e, t, n) {
    var r = n(2);
    e.exports = (r["default"] || r).template({
        compiler: [8, ">= 4.3.0"], main: function (e, t, n, r, o) {
            return '<div class="search_box cl">\r\n\t<div class="page_title fl"></div>\r\n\t<div class="search_wrap fl">\r\n\t\t<form class="search_form">\r\n\t\t\t<input id="heat_search"/>\r\n\t\t\t<div id="search_btn"><b class="icon icon_search"></b></div>\r\n\t\t</form>\r\n\t</div>\r\n\t<div class="historybox fr"></div>\r\n</div>'
        }, useData: !0
    })
}, function (e, t, n) {
    n(46);
    var r = n(47), o = n(48);
    e.exports = {
        launchIn: function (e) {
            !function (e) {
                var t = {stockItem: [], moreStockItem: []}, n = o().stockData;
                if (n.stockItem.length < 1) return !1;
                t.stockItem = n.stockItem.slice(0, 3), t.hasMoreHistory = n.stockItem.length > 3 ? "show" : "hide", "show" == t.hasMoreHistory && (t.moreStockItem = n.stockItem.slice(3, 6)), $(e).html(r(t))
            }(e)
        }
    }
}, function (e, t) {
}, function (e, t, n) {
    var r = n(2);
    e.exports = (r["default"] || r).template({
        1: function (e, t, n, r, o) {
            var i, a, s = null != t ? t : e.nullContext || {}, c = e.hooks.helperMissing,
                l = e.lookupProperty || function (e, t) {
                    return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
                };
            return '      <li class="his_item"><a href="' + e.escapeExpression("function" == typeof (a = null != (a = l(n, "barhref") || (null != t ? l(t, "barhref") : t)) ? a : c) ? a.call(s, {
                name: "barhref",
                hash: {},
                data: o,
                loc: {start: {line: 5, column: 36}, end: {line: 5, column: 47}}
            }) : a) + '">' + (null != (i = "function" == typeof (a = null != (a = l(n, "stockName") || (null != t ? l(t, "stockName") : t)) ? a : c) ? a.call(s, {
                name: "stockName",
                hash: {},
                data: o,
                loc: {start: {line: 5, column: 49}, end: {line: 5, column: 64}}
            }) : a) ? i : "") + "</a></li>\r\n"
        }, 3: function (e, t, n, r, o) {
            var i, a, s = null != t ? t : e.nullContext || {}, c = e.hooks.helperMissing,
                l = e.lookupProperty || function (e, t) {
                    return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
                };
            return '        <li><a href="' + e.escapeExpression("function" == typeof (a = null != (a = l(n, "barhref") || (null != t ? l(t, "barhref") : t)) ? a : c) ? a.call(s, {
                name: "barhref",
                hash: {},
                data: o,
                loc: {start: {line: 12, column: 21}, end: {line: 12, column: 32}}
            }) : a) + '">' + (null != (i = "function" == typeof (a = null != (a = l(n, "stockName") || (null != t ? l(t, "stockName") : t)) ? a : c) ? a.call(s, {
                name: "stockName",
                hash: {},
                data: o,
                loc: {start: {line: 12, column: 34}, end: {line: 12, column: 49}}
            }) : a) ? i : "") + "</a></li>\r\n"
        }, compiler: [8, ">= 4.3.0"], main: function (e, t, n, r, o) {
            var i, a, s = null != t ? t : e.nullContext || {}, c = e.lookupProperty || function (e, t) {
                return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
            };
            return '<div id="history_box">\r\n  最近访问：\r\n  <ul class="his_items">\r\n' + (null != (i = c(n, "each").call(s, null != t ? c(t, "stockItem") : t, {
                name: "each",
                hash: {},
                fn: e.program(1, o, 0),
                inverse: e.noop,
                data: o,
                loc: {start: {line: 4, column: 4}, end: {line: 6, column: 13}}
            })) ? i : "") + '  </ul>\r\n  <div class="more ' + e.escapeExpression("function" == typeof (a = null != (a = c(n, "hasMoreHistory") || (null != t ? c(t, "hasMoreHistory") : t)) ? a : e.hooks.helperMissing) ? a.call(s, {
                name: "hasMoreHistory",
                hash: {},
                data: o,
                loc: {start: {line: 8, column: 19}, end: {line: 8, column: 37}}
            }) : a) + '">\r\n    更多>\r\n    <ul class="more_bar_list">\r\n' + (null != (i = c(n, "each").call(s, null != t ? c(t, "moreStockItem") : t, {
                name: "each",
                hash: {},
                fn: e.program(3, o, 0),
                inverse: e.noop,
                data: o,
                loc: {start: {line: 11, column: 6}, end: {line: 13, column: 15}}
            })) ? i : "") + "    </ul>\r\n  </div>\r\n</div>\r\n"
        }, useData: !0
    })
}, function (e, t, n) {
    var r = n(49);
    e.exports = function (e) {
        var t = "/list";
        e && e.host && (t = e.host);
        for (var n = r.storage.getItem("stock_history") ? r.storage.getItem("stock_history").split("/") : [], o = {stockItem: []}, i = 0; i < n.length; i++) try {
            var a = {}, s = JSON.parse(n[i].replace(/、/g, "/")).stockname + "吧", c = JSON.parse(n[i]).barcode;
            if (!c || !s) continue;
            a.stockName = s, a.barCode = c, a.barhref = t + "," + c + ".html", o[s] = c, o.stockItem.push(a)
        } catch (e) {
            console.log(e)
        }
        return {stockData: o, historyItem: n}
    }
}, function (e, t, n) {
    var r = n(50), o = n(13), i = window.helper = {storage: r, cookie: o};
    e.exports = i
}, function (e, t, n) {
    var r = n(13);
    e.exports = {
        setItem: function (e, t, n) {
            if ("undefined" != typeof localStorage) return localStorage.setItem(e, t);
            var o = window.location.hostname;
            return r.set(e, t, n, o)
        }, getItem: function (e) {
            return "undefined" != typeof localStorage ? localStorage.getItem(e) : r.get(e)
        }, delItem: function (e, t) {
            return "undefined" != typeof localStorage ? localStorage.removeItem(e) : r.removeCookie(e, t)
        }
    }
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.ChartHistorySmall = void 0;
    var r = window.echarts, o = function () {
        return function (e, t) {
            var n;
            this.chart = r.init(e);
            var o = [];
            t.forEach(function (e) {
                o.push([e.CALCTIME, e.RANK])
            }), (n = {
                xAxis: {type: "time", show: !1},
                yAxis: {type: "value", axisPointer: {lineStyle: {type: "solid"}}, show: !1, inverse: !0},
                grid: {top: 0, bottom: 0},
                series: [{data: o, type: "line", symbolSize: 1, showSymbol: o.length < 3}]
            }) && this.chart.setOption(n)
        }
    }();
    t.ChartHistorySmall = o
}, function (e, t, n) {
    "use strict";
    var r, o = this && this.__extends || (r = function (e, t) {
        return (r = Object.setPrototypeOf || {__proto__: []} instanceof Array && function (e, t) {
            e.__proto__ = t
        } || function (e, t) {
            for (var n in t) t.hasOwnProperty(n) && (e[n] = t[n])
        })(e, t)
    }, function (e, t) {
        function n() {
            this.constructor = e
        }

        r(e, t), e.prototype = null === t ? Object.create(t) : (n.prototype = t.prototype, new n)
    });
    Object.defineProperty(t, "__esModule", {value: !0}), t.TipPop = void 0;
    var i = function (e) {
        function t(t) {
            var n = this;
            return console.log(t, "options"), (n = e.call(this) || this).title = t.title, n.content = t.content, n.addClass = t.addOwnClass, n.show(), n
        }

        return o(t, e), t
    }(n(53).Popup);
    t.TipPop = i
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.Popup = void 0;
    var r = n(54);
    n(55);
    var o = function () {
        function e() {
            var e = this;
            this.root = document.createElement("div"), this.root.innerHTML = '<div class="popbody">\n                                    <div class="popheader">\n                                        <span class="title"></span>\n                                        <span class="popclose">\n                                            <i class="closeicon"></i>\n                                        </span>\n                                        </div><div class="popcont">\n                                    </div>\n                                <div>', this.root.classList.add("popbox"), this.mask = document.createElement("div"), this.mask.classList.add("popmask"), document.querySelector("body").appendChild(this.mask), document.querySelector("body").appendChild(this.root), setTimeout(function () {
                e.root.querySelector(".popclose").onclick = function () {
                    e.remove()
                }
            }, 10)
        }

        return Object.defineProperty(e.prototype, "title", {
            set: function (e) {
                this.root.querySelector(".title").innerHTML = e
            }, enumerable: !1, configurable: !0
        }), Object.defineProperty(e.prototype, "content", {
            set: function (e) {
                this.root.querySelector(".popcont").innerHTML = e
            }, enumerable: !1, configurable: !0
        }), Object.defineProperty(e.prototype, "addClass", {
            set: function (e) {
                this.root.classList.add(e)
            }, enumerable: !1, configurable: !0
        }), e.prototype.show = function () {
            this.mask.style.display = "block", this.root.style.display = "block", this.root.querySelector(".popbody").classList.remove("pophide"), this.root.querySelector(".popbody").classList.add("popshow")
        }, e.prototype.hide = function () {
            var e = this;
            this.root.querySelector(".popbody").classList.remove("popshow"), this.root.querySelector(".popbody").classList.add("pophide"), setTimeout(function () {
                e.root.style.display = "none", e.mask.style.display = "none"
            }, 100)
        }, e.prototype.remove = function () {
            var e = this;
            this.root.querySelector(".popbody").classList.remove("popshow"), this.root.querySelector(".popbody").classList.add("pophide"), setTimeout(function () {
                r.testIEVerison() ? (e.root.removeNode(!0), e.mask.removeNode(!0)) : (e.root.remove(), e.mask.remove())
            }, 100)
        }, e
    }();
    t.Popup = o
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.getBroswer = t.testIEVerison = void 0, t.testIEVerison = function () {
        var e = navigator.userAgent.toLowerCase(), t = e.indexOf("msie") > -1, n = e.match(/rv:([\d.]+)\) like gecko/),
            r = e.match(/msie ([\d.]+)/);
        return t || n ? n ? n[1] : r[1] : null
    }, t.getBroswer = function () {
        var e, t = {}, n = navigator.userAgent.toLowerCase();
        return (e = n.match(/edge\/([\d.]+)/)) ? t.edge = e[1] : (e = n.match(/rv:([\d.]+)\) like gecko/)) ? t.ie = e[1] : (e = n.match(/msie ([\d.]+)/)) ? t.ie = e[1] : (e = n.match(/firefox\/([\d.]+)/)) ? t.firefox = e[1] : (e = n.match(/chrome\/([\d.]+)/)) ? t.chrome = e[1] : (e = n.match(/opera.([\d.]+)/)) ? t.opera = e[1] : (e = n.match(/version\/([\d.]+).*safari/)) && (t.safari = e[1]), t.edge ? {
            broswer: "Edge",
            version: t.edge
        } : t.ie ? {broswer: "IE", version: t.ie} : t.firefox ? {
            broswer: "Firefox",
            version: t.firefox
        } : t.chrome ? {broswer: "Chrome", version: t.chrome} : t.opera ? {
            broswer: "Opera",
            version: t.opera
        } : t.safari ? {broswer: "Safari", version: t.safari} : {broswer: "", version: "0"}
    }
}, function (e, t) {
}, function (e, t, n) {
    function r() {
        $("body").append('<div id="sharewrap"></div>'), $.getScript("//emres.dfcfw.com/common/emshare/js/share.js", function () {
            new emshare2020({title: document.title, link: window.location.href, dom: "#sharewrap", qrwidth: 205})
        })
    }

    n(57), e.exports = {
        launchIn: function (e) {
            r()
        }
    }
}, function (e, t) {
}, function (e, t, n) {
    n(59), e.exports = {
        launchIn: function (e) {
            $("body").append('<div id="backbox"><div id="feedback" onclick="window.open(\'//corp.eastmoney.com/Lianxi_liuyan.asp\')">意见反馈</div><div id="backtop" onclick="window.scroll(0,0)"><em class="icon icon_backtotop"></em></div></div>'), $(window).scroll(function () {
                $(window).scrollTop() > $(window).height() / 2 ? $("#backtop").show() : $("#backtop").hide()
            })
        }
    }
}, function (e, t) {
}, , , , , function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), n(17), n(19), n(20), n(21);
    var r = n(22), o = n(42), i = n(65), a = n(56), s = n(58);
    new r.MakeHeader, new o.searchHeader, new i.rankTable, a.launchIn(""), s.launchIn("")
}, function (module, exports, __webpack_require__) {
    "use strict";
    Object.defineProperty(exports, "__esModule", {value: !0}), exports.rankTable = void 0, __webpack_require__(66);
    var net_1 = __webpack_require__(3), rankTableHbs = __webpack_require__(67), popitemHbs = __webpack_require__(68),
        topicHbs = __webpack_require__(69), chartHistorySmall_1 = __webpack_require__(51),
        pager_1 = __webpack_require__(70), tip_1 = __webpack_require__(52), net = __webpack_require__(3),
        text_1 = __webpack_require__(5), rankTable = function () {
            function rankTable() {
                this.query = function (e) {
                    return document.querySelector(e)
                }, this.queryAll = function (e) {
                    return document.querySelectorAll(e)
                }, this.p = 1, this.sort = 0, this.marketType = 0, this.ps = 20, this.sendApprove = !0, this.init()
            }

            return rankTable.prototype.init = function () {
                var e = this;
                this.query("title").innerHTML = "东方财富个股人气榜top100", this.query("head").insertAdjacentHTML("afterbegin", '<meta name="keywords" content="个股人气榜,股票人气榜,个股人气,个股排名,人气排名,个股热度,实时人气,人气变动,粉丝特征">'), this.query("head").insertAdjacentHTML("afterbegin", '<meta name="description" content="东方财富个股人气榜--通过个股人气、历史趋势、粉丝特征、关注股等模块，为股友提供一种以用户行为来分析股票的功能">'), this.query("#rankCont").innerHTML = rankTableHbs(), "uptab" == text_1.getQueryString("tab") ? (this.p = 1, this.sort = 1, this.query(".ranktit.hotrank").classList.remove("active"), this.query(".ranktit.rankup").classList.add("active")) : text_1.getQueryString("tab"), "hk" == text_1.getQueryString("market") ? (this.marketType = 1, this.queryAll(".market_li").forEach(function (e) {
                    e.classList.remove("on")
                }), this.query(".market_li.hk_stock").classList.add("on")) : "us" == text_1.getQueryString("market") && (this.marketType = 2, this.queryAll(".market_li").forEach(function (e) {
                    e.classList.remove("on")
                }), this.query(".market_li.us_stock").classList.add("on")), this.getData(), this.sendApprove = !1, this.query(".ranktit.hotrank").addEventListener("click", function (t) {
                    e.p = 1, e.sort = 0, e.getData(), e.query(".ranktit.rankup").classList.remove("active"), e.query(".ranktit.hotrank").classList.add("active"), e.sendApprove = !1
                }), this.query(".ranktit.rankup").addEventListener("click", function () {
                    e.p = 1, e.sort = 1, e.getData(), e.query(".ranktit.hotrank").classList.remove("active"), e.query(".ranktit.rankup").classList.add("active"), e.sendApprove = !1
                }), this.queryAll(".market_li").forEach(function (t, n) {
                    t.addEventListener("click", function () {
                        var n = t.getAttribute("market");
                        e.marketType = n, e.getData(), e.queryAll(".market_li").forEach(function (e) {
                            e.classList.remove("on")
                        }), t.classList.add("on"), e.sendApprove = !1
                    })
                }), this.query(".refresh").addEventListener("click", function (t) {
                    e.p = 1, e.getData(), e.sendApprove = !1
                })
            }, rankTable.prototype.getData = function () {
                var _this = this;
                this.sendApprove && (this.query(".tablebox").innerHTML = '<div class="loading"></div>', net.sendByScript(net.sendHost().gbcdnHost + "popularityList.js?type=" + this.marketType + "&sort=" + this.sort + "&page=" + this.p, "", function (data) {
                    var scriptData = window.popularityList;
                    scriptData && (scriptData = window.d(scriptData), scriptData = eval(scriptData), scriptData.length > 0 && _this.fixData(scriptData)), _this.sendApprove = !0
                }, function (e) {
                    console.log(e), _this.sendApprove = !0
                }))
            }, rankTable.prototype.fixData = function (e) {
                var t = this, n = [];
                e.forEach(function (e, r) {
                    if (0 == t.sort) switch (e.rankNumber) {
                        case 1:
                            e.top_rank = "icon_rank1", e.rankNumber = "";
                            break;
                        case 2:
                            e.top_rank = "icon_rank2", e.rankNumber = "";
                            break;
                        case 3:
                            e.top_rank = "icon_rank3", e.rankNumber = ""
                    }
                    e.changeNumber > 0 ? e.changetr = "rankup" : e.changeNumber < 0 ? e.changetr = "rankdown" : e.changetr = "", 0 == e.newFans && 0 == e.ironsFans ? e.showbar = "none" : (e.leftb_width = Math.round(e.newFans / 100 * 150), e.rightb_width = Math.round(e.ironsFans / 100 * 150), e.barsplit_left = Math.round(e.newFans / 100 * 150) - 3), e.gubaCode = e.code, e.showCode = e.code, 1 == t.marketType && (e.gubaCode = "hk" + e.code.substring(e.code.indexOf("_") + 1, e.code.length), e.showCode = e.code.substring(e.code.indexOf("_") + 1, e.code.length)), 2 == t.marketType && (e.gubaCode = "us" + e.code.substring(e.code.indexOf("_") + 1, e.code.length), e.gubaCode.indexOf("_") > -1 && (e.gubaCode = e.gubaCode.replace("_", ".")), e.showCode = e.code.substring(e.code.indexOf("_") + 1, e.code.length)), n.push(e.code), e.strHistory = JSON.stringify(e.history)
                });
                var r = !0, o = !0;
                0 != this.marketType && (r = !1), 0 != this.sort && (o = !1);
                var i = {isStockHS: r, dataList: e, isPopList: o};
                this.bind(i, n)
            }, rankTable.prototype.bind = function (e, t) {
                this.query(".tablebox").innerHTML = popitemHbs(e), window.scrollTo({top: 0, behavior: "smooth"});
                var n = this.queryAll(".chart_line");
                Array.prototype.forEach.call(n, function (e) {
                    try {
                        var t = JSON.parse(e.getAttribute("data-strdata"));
                        new chartHistorySmall_1.ChartHistorySmall(e, t)
                    } catch (e) {
                        console.log(e)
                    }
                });
                var r = this.query(".rank_table");
                r.style.width = r.offsetWidth + "px", 1 == this.marketType ? r.classList.add("hktable") : 2 == this.marketType && r.classList.add("ustable"), this.gethqData(t), this.makePager(e);
                var o = e.dataList[0].exactTime;
                this.query(".updata_time").innerHTML = o.substring(0, o.length - 3), this.getTopic(t.join(","))
            }, rankTable.prototype.getTopic = function (e) {
                var t = this;
                net.sendByScript(net.sendHost().gbcdnHost + "interface/GetData.js?needClick=1&codes=" + e + "&path=newtopic/api/Topic/GubaCodeHotTopicNewRead&cb=topicList", "", function (n) {
                    var r = window.topicList;
                    if (r && r.length > 0 && r[0].re) {
                        var o = r[0].re;
                        e.split(",").forEach(function (e) {
                            if (o[e].length > 0) {
                                var n = o[e][0];
                                t.query(".item_" + e + " .stock_name_" + e).insertAdjacentHTML("afterend", topicHbs(n)), t.query(".item_" + e).classList.add("hastopic")
                            }
                        })
                    }
                }, function (e) {
                    console.log(e)
                })
            }, rankTable.prototype.gethqData = function (e) {
                var t = this;
                net_1.getHq(e.join(",")).then(function (e) {
                    var n;
                    (null === (n = null === e || void 0 === e ? void 0 : e.data) || void 0 === n ? void 0 : n.diff) && e.data.diff.forEach(function (e) {
                        var n = "grey";
                        switch (e.f4 > 0 ? n = "red" : e.f4 < 0 && (n = "green"), e.domCode = e.f12, e.f13) {
                            case 105:
                                e.domCode = "NASDAQ_" + e.f12;
                                break;
                            case 106:
                                e.domCode = "NYSE_" + e.f12;
                                break;
                            case 107:
                                e.domCode = "AMEX_" + e.f12;
                                break;
                            case 116:
                                e.domCode = "HK_" + e.f12
                        }
                        Array.prototype.forEach.call(t.queryAll(".quote_" + e.domCode), function (t) {
                            t.setAttribute("href", "//quote.eastmoney.com/unify/r/" + e.f13 + "." + e.f12)
                        }), Array.prototype.forEach.call(t.queryAll(".price_" + e.domCode), function (t) {
                            t.innerHTML = e.f2, t.classList.add(n)
                        }), Array.prototype.forEach.call(t.queryAll(".zde_" + e.domCode), function (t) {
                            t.innerHTML = e.f4, t.classList.add(n)
                        }), Array.prototype.forEach.call(t.queryAll(".zdf_" + e.domCode), function (t) {
                            t.innerHTML = "-" == e.f3 ? "" + e.f3 : e.f3 + "%", t.classList.add(n)
                        }), Array.prototype.forEach.call(t.queryAll(".stock_name_" + e.domCode), function (t) {
                            t.innerHTML = e.f14, t.setAttribute("title", e.f14)
                        }), Array.prototype.forEach.call(t.queryAll(".zgj_" + e.domCode), function (t) {
                            t.innerHTML = e.f15
                        }), Array.prototype.forEach.call(t.queryAll(".zdj_" + e.domCode), function (t) {
                            t.innerHTML = e.f16
                        })
                    })
                })
            }, rankTable.prototype.makePager = function (e) {
                var t = this, n = new pager_1.Pager({
                    sum: 100,
                    pageCount: this.ps,
                    thisPage: this.p,
                    firstpage_hide_first: !1,
                    needIndex: !1,
                    needJumpLink: !0,
                    linkBind: function (e) {
                        e.setAttribute("href", "javascript:;"), e.setAttribute("target", "_self")
                    }
                });
                this.query(".pager").innerHTML = "", this.query(".pager").appendChild(n.getPagerElems()), this.query(".jump_launch").onclick = function (e) {
                    var n = t.query(".jump_input").value.trim(), r = n.match(/\D/g),
                        o = Number(t.query(".sumpage").innerHTML), i = !1;
                    n && !r && Number(n) <= Number(o) && Number(n) > 0 && (i = !0), i ? (t.p = n, t.getData(), t.sendApprove = !1) : new tip_1.TipPop({
                        title: "提示",
                        content: "请输入有效的页码"
                    })
                }, this.queryAll(".pager a").forEach(function (e) {
                    e.onclick = function (e) {
                        var n = e.currentTarget.classList.contains("on"), r = e.currentTarget.getAttribute("data-page");
                        t.p = r, t.sendApprove && !n && (t.getData(), t.sendApprove = !1)
                    }
                })
            }, rankTable
        }();
    exports.rankTable = rankTable
}, function (e, t) {
}, function (e, t, n) {
    var r = n(2);
    e.exports = (r["default"] || r).template({
        compiler: [8, ">= 4.3.0"], main: function (e, t, n, r, o) {
            return '<div class="table_cont">\r\n\t<div class="box_header cl">\r\n\t\t<div class="tit_right fl">\r\n\t\t\t<span class="ranktit hotrank active"><b class="icon icon_hotrank"></b> 人气榜</span>\r\n\t\t\t<span class="ranktit rankup"><b class="icon icon_rank_up"></b> 飙升榜</span>\r\n\t\t</div>\r\n\t\t<div class="tit_left fr">\r\n\t\t\t<span>更新时间：<span class="updata_time"></span></span>\r\n\t\t\t<span class="refresh"><b class="icon icon_refresh"></b> 刷新</span>\r\n\t\t</div>\r\n\t</div>\r\n\t<ul class="market_tab cl">\r\n\t\t<li class="market_li a_stock on" market=0>A股市场</li>\r\n\t\t<li class="market_li hk_stock" market=1>港股市场</li>\r\n\t\t<li class="market_li us_stock" market=2>美股市场</li>\r\n\t</ul>\r\n\t<div class="tablebox">\r\n\t\t\r\n\t</div>\r\n</div>\r\n\r\n\r\n<div class="pager"></div>'
        }, useData: !0
    })
}, function (e, t, n) {
    var r = n(2);
    e.exports = (r["default"] || r).template({
        1: function (e, t, n, r, o) {
            return "                <td><div>当前排名</div></td>\r\n                <td><div>排名较昨日变动</div></td>\r\n"
        }, 3: function (e, t, n, r, o) {
            return "                <td><div>排名较昨日变动</div></td>\r\n                <td><div>当前排名</div></td>\r\n"
        }, 5: function (e, t, n, r, o) {
            return "                <td><div>新晋粉丝</div></td>\r\n                <td><div>铁杆粉丝</div></td>\r\n"
        }, 7: function (e, t, n, r, o) {
            return "                <td><div>最高价</div></td>\r\n                <td><div>最低价</div></td>\r\n"
        }, 9: function (e, t, n, r, o, i, a) {
            var s, c, l = null != t ? t : e.nullContext || {}, u = e.hooks.helperMissing, d = "function",
                f = e.escapeExpression, p = e.lookupProperty || function (e, t) {
                    return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
                };
            return '            <tr class="item_' + f(typeof (c = null != (c = p(n, "code") || (null != t ? p(t, "code") : t)) ? c : u) === d ? c.call(l, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 29, column: 28}, end: {line: 29, column: 36}}
            }) : c) + '">\r\n' + (null != (s = p(n, "if").call(l, null != a[1] ? p(a[1], "isPopList") : a[1], {
                name: "if",
                hash: {},
                fn: e.program(10, o, 0, i, a),
                inverse: e.program(12, o, 0, i, a),
                data: o,
                loc: {start: {line: 30, column: 16}, end: {line: 36, column: 23}}
            })) ? s : "") + '                <td><a class="chart_line" data-strdata="' + f(typeof (c = null != (c = p(n, "strHistory") || (null != t ? p(t, "strHistory") : t)) ? c : u) === d ? c.call(l, {
                name: "strHistory",
                hash: {},
                data: o,
                loc: {start: {line: 37, column: 56}, end: {line: 37, column: 70}}
            }) : c) + '" href="/rank/stock?code=' + f(typeof (c = null != (c = p(n, "code") || (null != t ? p(t, "code") : t)) ? c : u) === d ? c.call(l, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 37, column: 95}, end: {line: 37, column: 103}}
            }) : c) + '"></a></td>    \r\n                <td><div class="stocktd"><a class="sec_code quote_' + f(typeof (c = null != (c = p(n, "code") || (null != t ? p(t, "code") : t)) ? c : u) === d ? c.call(l, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 38, column: 68}, end: {line: 38, column: 76}}
            }) : c) + '" href="javascript:;">' + f(typeof (c = null != (c = p(n, "showCode") || (null != t ? p(t, "showCode") : t)) ? c : u) === d ? c.call(l, {
                name: "showCode",
                hash: {},
                data: o,
                loc: {start: {line: 38, column: 98}, end: {line: 38, column: 110}}
            }) : c) + '</a></div></td>    \r\n                <td class="nametd"><div class="nametd_box"><a class="stock_name_' + f(typeof (c = null != (c = p(n, "code") || (null != t ? p(t, "code") : t)) ? c : u) === d ? c.call(l, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 39, column: 80}, end: {line: 39, column: 88}}
            }) : c) + '" href="/rank/stock?code=' + f(typeof (c = null != (c = p(n, "code") || (null != t ? p(t, "code") : t)) ? c : u) === d ? c.call(l, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 39, column: 113}, end: {line: 39, column: 121}}
            }) : c) + '">' + f(typeof (c = null != (c = p(n, "name") || (null != t ? p(t, "name") : t)) ? c : u) === d ? c.call(l, {
                name: "name",
                hash: {},
                data: o,
                loc: {start: {line: 39, column: 123}, end: {line: 39, column: 131}}
            }) : c) + '</a></div></td>    \r\n                <td class="relative"><div><a class="rank_detail" href="/rank/stock?code=' + f(typeof (c = null != (c = p(n, "code") || (null != t ? p(t, "code") : t)) ? c : u) === d ? c.call(l, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 40, column: 88}, end: {line: 40, column: 96}}
            }) : c) + '">排名详情</a><a href="//guba.eastmoney.com/list,' + f(typeof (c = null != (c = p(n, "gubaCode") || (null != t ? p(t, "gubaCode") : t)) ? c : u) === d ? c.call(l, {
                name: "gubaCode",
                hash: {},
                data: o,
                loc: {start: {line: 40, column: 141}, end: {line: 40, column: 153}}
            }) : c) + '.html">股吧</a></div></td>    \r\n                <td><div class="price_' + f(typeof (c = null != (c = p(n, "code") || (null != t ? p(t, "code") : t)) ? c : u) === d ? c.call(l, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 41, column: 38}, end: {line: 41, column: 46}}
            }) : c) + '">--</div></td>    \r\n                <td><div class="zde_' + f(typeof (c = null != (c = p(n, "code") || (null != t ? p(t, "code") : t)) ? c : u) === d ? c.call(l, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 42, column: 36}, end: {line: 42, column: 44}}
            }) : c) + '">--</div></td>    \r\n                <td><div class="zdf_' + f(typeof (c = null != (c = p(n, "code") || (null != t ? p(t, "code") : t)) ? c : u) === d ? c.call(l, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 43, column: 36}, end: {line: 43, column: 44}}
            }) : c) + '">--</div></td>   \r\n' + (null != (s = p(n, "if").call(l, null != a[1] ? p(a[1], "isStockHS") : a[1], {
                name: "if",
                hash: {},
                fn: e.program(14, o, 0, i, a),
                inverse: e.program(16, o, 0, i, a),
                data: o,
                loc: {start: {line: 44, column: 16}, end: {line: 58, column: 23}}
            })) ? s : "") + "            </tr>\r\n"
        }, 10: function (e, t, n, r, o) {
            var i, a = null != t ? t : e.nullContext || {}, s = e.hooks.helperMissing, c = e.escapeExpression,
                l = e.lookupProperty || function (e, t) {
                    return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
                };
            return '                    <td class="ranktd bold"><div>' + c("function" == typeof (i = null != (i = l(n, "rankNumber") || (null != t ? l(t, "rankNumber") : t)) ? i : s) ? i.call(a, {
                name: "rankNumber",
                hash: {},
                data: o,
                loc: {start: {line: 31, column: 49}, end: {line: 31, column: 63}}
            }) : i) + '<b class="ranktop icon ' + c("function" == typeof (i = null != (i = l(n, "top_rank") || (null != t ? l(t, "top_rank") : t)) ? i : s) ? i.call(a, {
                name: "top_rank",
                hash: {},
                data: o,
                loc: {start: {line: 31, column: 86}, end: {line: 31, column: 98}}
            }) : i) + '"></b></div></td>    \r\n                    <td class="rankchange"><div><b class="changeicon icon icon_' + c("function" == typeof (i = null != (i = l(n, "changetr") || (null != t ? l(t, "changetr") : t)) ? i : s) ? i.call(a, {
                name: "changetr",
                hash: {},
                data: o,
                loc: {start: {line: 32, column: 79}, end: {line: 32, column: 91}}
            }) : i) + '"></b>' + c("function" == typeof (i = null != (i = l(n, "changeNumber") || (null != t ? l(t, "changeNumber") : t)) ? i : s) ? i.call(a, {
                name: "changeNumber",
                hash: {},
                data: o,
                loc: {start: {line: 32, column: 97}, end: {line: 32, column: 113}}
            }) : i) + "</div></td>    \r\n"
        }, 12: function (e, t, n, r, o) {
            var i, a = null != t ? t : e.nullContext || {}, s = e.hooks.helperMissing, c = e.escapeExpression,
                l = e.lookupProperty || function (e, t) {
                    return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
                };
            return '                    <td class="rankchange bold"><div><b class="changeicon icon icon_' + c("function" == typeof (i = null != (i = l(n, "changetr") || (null != t ? l(t, "changetr") : t)) ? i : s) ? i.call(a, {
                name: "changetr",
                hash: {},
                data: o,
                loc: {start: {line: 34, column: 84}, end: {line: 34, column: 96}}
            }) : i) + '"></b>' + c("function" == typeof (i = null != (i = l(n, "changeNumber") || (null != t ? l(t, "changeNumber") : t)) ? i : s) ? i.call(a, {
                name: "changeNumber",
                hash: {},
                data: o,
                loc: {start: {line: 34, column: 102}, end: {line: 34, column: 118}}
            }) : i) + '</div></td>    \r\n                    <td class="ranktd"><div>' + c("function" == typeof (i = null != (i = l(n, "rankNumber") || (null != t ? l(t, "rankNumber") : t)) ? i : s) ? i.call(a, {
                name: "rankNumber",
                hash: {},
                data: o,
                loc: {start: {line: 35, column: 44}, end: {line: 35, column: 58}}
            }) : i) + '<b class="ranktop icon ' + c("function" == typeof (i = null != (i = l(n, "top_rank") || (null != t ? l(t, "top_rank") : t)) ? i : s) ? i.call(a, {
                name: "top_rank",
                hash: {},
                data: o,
                loc: {start: {line: 35, column: 81}, end: {line: 35, column: 93}}
            }) : i) + '"></b></div></td>    \r\n'
        }, 14: function (e, t, n, r, o) {
            var i, a = null != t ? t : e.nullContext || {}, s = e.hooks.helperMissing, c = "function",
                l = e.escapeExpression, u = e.lookupProperty || function (e, t) {
                    return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
                };
            return '                    <td class="fans" colspan="2">\r\n                        <div class="bars" style="display:' + l(typeof (i = null != (i = u(n, "showbar") || (null != t ? u(t, "showbar") : t)) ? i : s) === c ? i.call(a, {
                name: "showbar",
                hash: {},
                data: o,
                loc: {start: {line: 46, column: 57}, end: {line: 46, column: 68}}
            }) : i) + '">\r\n                            <span class="left_percent">' + l(typeof (i = null != (i = u(n, "newFans") || (null != t ? u(t, "newFans") : t)) ? i : s) === c ? i.call(a, {
                name: "newFans",
                hash: {},
                data: o,
                loc: {start: {line: 47, column: 55}, end: {line: 47, column: 66}}
            }) : i) + '%</span> \t\r\n                            <span class="right_percent">' + l(typeof (i = null != (i = u(n, "ironsFans") || (null != t ? u(t, "ironsFans") : t)) ? i : s) === c ? i.call(a, {
                name: "ironsFans",
                hash: {},
                data: o,
                loc: {start: {line: 48, column: 56}, end: {line: 48, column: 69}}
            }) : i) + '%</span> \t\r\n                            <div class="leftbar" style="width:' + l(typeof (i = null != (i = u(n, "leftb_width") || (null != t ? u(t, "leftb_width") : t)) ? i : s) === c ? i.call(a, {
                name: "leftb_width",
                hash: {},
                data: o,
                loc: {start: {line: 49, column: 62}, end: {line: 49, column: 77}}
            }) : i) + 'px"></div>\r\n                            <div class="rightbar" style="width:' + l(typeof (i = null != (i = u(n, "rightb_width") || (null != t ? u(t, "rightb_width") : t)) ? i : s) === c ? i.call(a, {
                name: "rightb_width",
                hash: {},
                data: o,
                loc: {start: {line: 50, column: 63}, end: {line: 50, column: 79}}
            }) : i) + 'px"></div>\r\n                            <div class="barsplit" style="left:' + l(typeof (i = null != (i = u(n, "barsplit_left") || (null != t ? u(t, "barsplit_left") : t)) ? i : s) === c ? i.call(a, {
                name: "barsplit_left",
                hash: {},
                data: o,
                loc: {start: {line: 51, column: 62}, end: {line: 51, column: 79}}
            }) : i) + 'px"></div>\r\n                        </div>\r\n                    </td>  \r\n'
        }, 16: function (e, t, n, r, o) {
            var i, a = null != t ? t : e.nullContext || {}, s = e.hooks.helperMissing, c = e.escapeExpression,
                l = e.lookupProperty || function (e, t) {
                    return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
                };
            return '                    <td><div class="zgj_' + c("function" == typeof (i = null != (i = l(n, "code") || (null != t ? l(t, "code") : t)) ? i : s) ? i.call(a, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 55, column: 40}, end: {line: 55, column: 48}}
            }) : i) + '">--</div></td>    \r\n                    <td><div class="zdj_' + c("function" == typeof (i = null != (i = l(n, "code") || (null != t ? l(t, "code") : t)) ? i : s) ? i.call(a, {
                name: "code",
                hash: {},
                data: o,
                loc: {start: {line: 56, column: 40}, end: {line: 56, column: 48}}
            }) : i) + '">--</div></td>   \r\n                </td>  \r\n'
        }, compiler: [8, ">= 4.3.0"], main: function (e, t, n, r, o, i, a) {
            var s, c = null != t ? t : e.nullContext || {}, l = e.lookupProperty || function (e, t) {
                return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
            };
            return '<table class="rank_table">\r\n    <thead class="tabhead">\r\n        <tr>\r\n' + (null != (s = l(n, "if").call(c, null != t ? l(t, "isPopList") : t, {
                name: "if",
                hash: {},
                fn: e.program(1, o, 0, i, a),
                inverse: e.program(3, o, 0, i, a),
                data: o,
                loc: {start: {line: 4, column: 12}, end: {line: 10, column: 19}}
            })) ? s : "") + "            <td><div>历史趋势</div></td>\r\n            <td><div>代码</div></td>\r\n            <td><div>资产名称</div></td>\r\n            <td><div>相关</div></td>\r\n            <td><div>最新价</div></td>\r\n            <td><div>涨跌额</div></td>\r\n            <td><div>涨跌幅</div></td>\r\n" + (null != (s = l(n, "if").call(c, null != t ? l(t, "isStockHS") : t, {
                name: "if",
                hash: {},
                fn: e.program(5, o, 0, i, a),
                inverse: e.program(7, o, 0, i, a),
                data: o,
                loc: {start: {line: 18, column: 12}, end: {line: 24, column: 19}}
            })) ? s : "") + '        </tr>\r\n    </thead>\r\n    <tbody class="stock_tbody"> \r\n' + (null != (s = l(n, "each").call(c, null != t ? l(t, "dataList") : t, {
                name: "each",
                hash: {},
                fn: e.program(9, o, 0, i, a),
                inverse: e.noop,
                data: o,
                loc: {start: {line: 28, column: 8}, end: {line: 60, column: 17}}
            })) ? s : "") + "    </tbody>\r\n</table>"
        }, useData: !0, useDepths: !0
    })
}, function (e, t, n) {
    var r = n(2);
    e.exports = (r["default"] || r).template({
        1: function (e, t, n, r, o) {
            var i, a = e.lookupProperty || function (e, t) {
                return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
            };
            return '                <ul class="topiclist">\r\n' + (null != (i = a(n, "each").call(null != t ? t : e.nullContext || {}, null != t ? a(t, "essen_postinfo") : t, {
                name: "each",
                hash: {},
                fn: e.program(2, o, 0),
                inverse: e.noop,
                data: o,
                loc: {start: {line: 16, column: 20}, end: {line: 18, column: 29}}
            })) ? i : "") + "                </ul>        \r\n"
        }, 2: function (e, t, n, r, o) {
            var i, a = null != t ? t : e.nullContext || {}, s = e.hooks.helperMissing, c = e.escapeExpression,
                l = e.lookupProperty || function (e, t) {
                    return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
                };
            return '                        <li class="topicli"><em class="doticon"></em><a href=' + c("function" == typeof (i = null != (i = l(n, "gubaUrl") || (null != t ? l(t, "gubaUrl") : t)) ? i : s) ? i.call(a, {
                name: "gubaUrl",
                hash: {},
                data: o,
                loc: {start: {line: 17, column: 77}, end: {line: 17, column: 88}}
            }) : i) + ">" + c("function" == typeof (i = null != (i = l(n, "title") || (null != t ? l(t, "title") : t)) ? i : s) ? i.call(a, {
                name: "title",
                hash: {},
                data: o,
                loc: {start: {line: 17, column: 89}, end: {line: 17, column: 98}}
            }) : i) + "</a></li>\r\n"
        }, compiler: [8, ">= 4.3.0"], main: function (e, t, n, r, o) {
            var i, a, s = null != t ? t : e.nullContext || {}, c = e.hooks.helperMissing, l = "function",
                u = e.escapeExpression, d = e.lookupProperty || function (e, t) {
                    return Object.prototype.hasOwnProperty.call(e, t) ? e[t] : undefined
                };
            return '<div class="topics">\r\n        <span class="icon icon_topic"></span>\r\n        <div class="topicitem cl">\r\n            <div class="topic_header cl">\r\n                <img src=' + u(typeof (a = null != (a = d(n, "pic") || (null != t ? d(t, "pic") : t)) ? a : c) === l ? a.call(s, {
                name: "pic",
                hash: {},
                data: o,
                loc: {start: {line: 5, column: 25}, end: {line: 5, column: 32}}
            }) : a) + ' alt="" class="topicimg fl">\r\n                <div class="topic_intr fl">\r\n                    <div class="topic_tit"><a href="//gubatopic.eastmoney.com/topic_v3.html?htid=' + u(typeof (a = null != (a = d(n, "htid") || (null != t ? d(t, "htid") : t)) ? a : c) === l ? a.call(s, {
                name: "htid",
                hash: {},
                data: o,
                loc: {start: {line: 7, column: 97}, end: {line: 7, column: 105}}
            }) : a) + '"> #<span class="topictit">' + u(typeof (a = null != (a = d(n, "name") || (null != t ? d(t, "name") : t)) ? a : c) === l ? a.call(s, {
                name: "name",
                hash: {},
                data: o,
                loc: {start: {line: 7, column: 132}, end: {line: 7, column: 140}}
            }) : a) + '</span>#</a></div>\r\n                    <div class="topic_info">\r\n                        <span class="dissc">讨论数：<span class="disnum">' + u(typeof (a = null != (a = d(n, "participantCount") || (null != t ? d(t, "participantCount") : t)) ? a : c) === l ? a.call(s, {
                name: "participantCount",
                hash: {},
                data: o,
                loc: {start: {line: 9, column: 69}, end: {line: 9, column: 89}}
            }) : a) + '</span></span>\r\n                        <span class="views">浏览：<span class="disnum">' + u(typeof (a = null != (a = d(n, "clickCount") || (null != t ? d(t, "clickCount") : t)) ? a : c) === l ? a.call(s, {
                name: "clickCount",
                hash: {},
                data: o,
                loc: {start: {line: 10, column: 68}, end: {line: 10, column: 82}}
            }) : a) + "</span></span>\r\n                    </div>\r\n                </div>\r\n            </div>\r\n" + (null != (i = d(n, "if").call(s, null != t ? d(t, "essen_postinfo") : t, {
                name: "if",
                hash: {},
                fn: e.program(1, o, 0),
                inverse: e.noop,
                data: o,
                loc: {start: {line: 14, column: 12}, end: {line: 20, column: 19}}
            })) ? i : "") + "        </div>\r\n</div>"
        }, useData: !0
    })
}, function (e, t, n) {
    "use strict";
    Object.defineProperty(t, "__esModule", {value: !0}), t.Pager = void 0, n(71);
    var r = function () {
        function e(e) {
            this.options = {
                sum: 0,
                pageCount: 0,
                thisPage: 1,
                needLast: !0,
                needSum: !0,
                needJumpLink: 0,
                needIndex: 1,
                pagerType: "",
                linkBind: function () {
                }
            }, this.options = Object.assign(this.options, e)
        }

        return e.prototype.init = function (e) {
            document.querySelector(e).appendChild(this.getPagerElems())
        }, e.prototype.getPagerElems = function () {
            var e = this, t = parseInt(this.options.sum), n = parseInt(this.options.pageCount),
                r = parseInt(this.options.thisPage);
            if (n > t) return null;
            var o = Math.ceil(t / n), i = [];
            if (i.push("<span>"), (1 != r || this.options.needIndex) && i.push('<a class="first_page" data-page="1">首页</a>'), r > 1 && i.push('<a data-page="' + (r - 1) + '">上一页</a>'), r > 6 && o - 5 > r) for (var a = r - 5; a <= r + 5; a++) i.push('<a data-page="' + a + '" ' + (r == a ? 'class="on"' : "") + ">" + a + "</a>"); else if (r <= 6) for (a = 1; a <= 11 && (i.push('<a data-page="' + a + '" ' + (r == a ? 'class="on"' : "") + ">" + a + "</a>"), a != o); a++) ; else {
                var s = 1;
                a = void 0;
                for (o - 11 > 0 && (s = o - 11), a = s; a <= o; a++) i.push('<a data-page="' + a + '" ' + (r == a ? 'class="on"' : "") + ">" + a + "</a>")
            }
            o > r && i.push('<a data-page="' + (r + 1) + '">下一页</a>'), this.options.needLast && i.push('<a class="last_page" data-page="' + o + '">尾页</a>'), this.options.needSum && i.push(' 共<span class="sumpage">' + o + "</span>页</span>"), this.options.needJumpLink && i.push('<span class="jump_page"> <input class="jump_input"> </span><span class="jump_launch">跳转</span>');
            var c = i.join(""), l = document.createElement("span");
            l.innerHTML = c;
            var u = l.querySelectorAll("a");
            return Array.prototype.forEach.call(u, function (t) {
                e.options.linkBind(t)
            }), l
        }, e
    }();
    t.Pager = r
}, function (e, t) {
}]);
