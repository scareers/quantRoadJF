// 简单Util库 封装必要功能
if (typeof Util == 'undefined') Util = {};
Util.isIE = (navigator.appName.indexOf("Microsoft", 0) != -1) ? true : false;
Util.getBodyHeight = function () {
    var Dim = Util.getBodyDim();
    return Dim.H;
};
Util.getBodyDim = function () {
    var Dim = {};
    // Try to get the "inner width" and "inner height"

    if (window.innerWidth) {
        Dim.W = window.innerWidth;
        Dim.H = window.innerHeight;
    } else if (document.documentElement && document.documentElement.clientWidth) {
        Dim.W = document.documentElement.clientWidth;
        Dim.H = document.documentElement.clientHeight;
    } else if (document.body) {
        Dim.W = document.body.clientWidth;
        Dim.H = document.body.clientHeight;
    }
    return Dim;
};
Util.getScrollTop = function () {
    var scrollPos;
    if (typeof window.pageYOffset != 'undefined') {
        scrollPos = window.pageYOffset;
    } else if (typeof document.compatMode != 'undefined' &&
        document.compatMode != 'BackCompat') {
        scrollPos = document.documentElement.scrollTop;
    } else if (typeof document.body != 'undefined') {
        scrollPos = document.body.scrollTop;
    }
    return scrollPos;
};
Util.randomString = function (string_length, first_alpha) {
    var alphachars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
    var chars = alphachars + "0123456789";
    var randomstring = '';
    for (var i = 0; i < string_length; i++) {

        if (first_alpha && i == 0) {
            var rnum = Math.floor(Math.random() * alphachars.length);
            randomstring += alphachars.substring(rnum, rnum + 1);
        } else {
            var rnum = Math.floor(Math.random() * chars.length);
            randomstring += chars.substring(rnum, rnum + 1);
        }
    }
    return randomstring;
};
Util.setCookie = function (name, value, expires, path, domain, secure) {
    var curCookie = name + "=" + escape(value) +
        ((expires) ? "; expires=" + expires.toGMTString() : "") +
        ((path) ? "; path=" + path : "") +
        ((domain) ? "; domain=" + domain : "") +
        ((secure) ? "; secure" : "");
    document.cookie = curCookie;
};
Util.getCookie = function (name) {
    var dc = document.cookie;
    var prefix = name + "=";
    var begin = dc.indexOf("; " + prefix);
    if (begin == -1) {
        begin = dc.indexOf(prefix);
        if (begin != 0) return null;
    } else
        begin += 2;
    var end = document.cookie.indexOf(";", begin);
    if (end == -1)
        end = dc.length;
    return unescape(dc.substring(begin + prefix.length, end));
};
Util.AddEvent = function (el, name, cb, useCapture) {
    if (el.addEventListener) {
        el.addEventListener(name, cb, useCapture);
    } else if (el.attachEvent) {
        el.attachEvent('on' + name, cb);
    }
};
Util.RemoveEvent = function (el, name, cb, useCapture) {
    if (el.removeEventListener) {
        el.removeEventListener(name, cb, useCapture);
    } else if (el.detachEvent) {
        el.detachEvent('on' + name, cb);
    }
};
Util.StrFmt = function () {
    var fmt = arguments[0], tS = fmt.split("%s"), rtn = new Array();
    for (var i = 0; i < tS.length; i++) {
        rtn.push(tS[i]);
        if (i + 1 < arguments.length && i != tS.length - 1)
            rtn.push(arguments[i + 1]);
    }
    return rtn.join('');
};


Function.prototype._Bind = function () {
    var __m = this, object = arguments[0], args = new Array();
    for (var i = 1; i < arguments.length; i++) {
        args.push(arguments[i]);
    }
    return function () {
        var c_args = [];
        for (var k = 0; k < arguments.length; k++) {
            c_args.push(arguments[k]);
        }
        return __m.apply(object, c_args.concat(args));
    }
};

Function.prototype._BindForEvent = function () {
    var __m = this, object = arguments[0], args = new Array();
    for (var i = 1; i < arguments.length; i++) {
        args.push(arguments[i]);
    }
    return function (event) {
        return __m.apply(object, [(event || window.event)].concat(args));
    }
};
//From DOJO lang/common.js
Util.Extend = function (obj, props) {
    var tobj = {};
    var robj = new Util.cloneObject(obj);

    for (var x in props) {
        // the "tobj" condition avoid copying properties in "props"
        // inherited from Object.prototype.  For example, if obj has a custom
        // toString() method, don't overwrite it with the toString() method
        // that props inherited from Object.protoype
        if (typeof tobj[x] == "undefined" || tobj[x] != props[x]) {
            robj[x] = props[x];
        }
    }
    // IE doesn't recognize custom toStrings in for..in
    if (Util.isIE && typeof props["toString"] == 'function' && props["toString"] != robj["toString"]) {
        robj.toString = props.toString;
    }
    return robj;
};

Util.cloneObject = function (what) {
    for (i in what) {
        this[i] = what[i];
    }
};
if (!Util) {
    Util = {};
}

Util.MergeObj = function (o1, o2) {
    var i, k;
    if (!o1) {
        o1 = {};
    }
    if (o2) {
        for (k in o2) {
            if (typeof (o2[k]) != 'undefined') {
                o1[k] = o2[k];
            }
        }
    }
    return o1;
};

Util._fcSearch = function (o, s) {
    if (s instanceof Array) {
        for (var x in s) {
            if (s[x].toString().toLowerCase() == o) {
                return 1;
            }
        }
    } else if (typeof (s) == 'string') {
        if (s.toLowerCase() == o) {
            return 1;
        }
    }
    return 0;
};

Util.FindChildren = function (o, t) {
    var i, a = [];
    if (!o || !o.childNodes) {
        return a;
    }
    for (i = 0; i < o.childNodes.length; i++) {
        if (o.childNodes[i].tagName && Util._fcSearch(o.childNodes[i].tagName.toLowerCase(), t)) {
            a.push(o.childNodes[i]);
        }
    }
    return a;
};

Util.ReplaceCss = function (n, o, c, t)
//n = Node, o = Oldcss, c = newCss, t = Text(while n = null)
{
    var i, s, a, f;
    f = 0;
    if (!n) {
        s = t ? t : '';
    } else {
        s = n.className ? n.className.toString() : '';
    }
    a = s.split(' ');
    if (!a.join) {
        a = [];
    }
    for (i = 0; i < a.length; i++) {
        if (a[i] == c) {
            f |= 1;
            //return;
        } else if (a[i] == o) {
            break;
        }
    }
    if (i < a.length) {
        if (c) {
            a[i] = c;
        } else {
            a.splice(i, 1);
        }
    } else {
        if (c && !(f & 1)) {
            a.push(c);
        }
    }
    t = a.join(' ');
    if (n) {
        n.className = t;
    }
    return t;
};

Util.AddComma = function (n) {
    var r = /(\d{1,3})(?=(\d{3})+(?:$|\.))/g;
    return n.replace(r, '$1,');
};

Util.StrTemplate = function () {
    var i;
    var s = arguments[0];
    for (i = 1; i < arguments.length; i++) {
        s = s.replace('{$' + i + '}', arguments[i]);
        s = s.replace('$' + i, arguments[i]);
    }
    return s;
};

Util.HideObj = function (o, a) {
    if (o) {
        if (a === 1 || a === true || a === undefined) {
            if (o.style.display != 'none') {
                o.style.display = 'none';
            }
        } else if (a === 0 || a === false) {
            if (o.style.display) {
                o.style.display = '';
            }
        } else if (a == -1) {
            o.style.display = (o.style.display == 'none') ? '' : 'none';
        }
    }
};

Util.SL_ArraySearch = function (s, a) {
    if (a instanceof Array) {
        for (var x in a) {
            if (a[x] == s) {
                return x;
            }
        }
    }
};
// 基于IO.Ajax (IO.Script可选) 封装好的服务器端对应服务调用
if (typeof IO == 'undefined') IO = {};
IO.SRV = function () {
    this.Init.apply(this, arguments);
};
/**
 * JSON格式传输数据
 * 接口调用模式
 * /path/to/api/gateway.php/srv.method
 *
 * post参数为 json格式 key -> value 配对格式串
 *
 *
 */
IO.SRV.prototype = {
    _xhr: null,
    _url: null,
    _cblist: [],
    _err_cb: null,
    Init: function (a_ServiceURL, a_ErrorCB, a_randomURL) {
        var l_opt = {};
        if (a_randomURL) l_opt.randomURL = true;
        if (a_ErrorCB) l_opt.err_cb = a_ErrorCB;
        this._err_cb = a_ErrorCB;
        this._xhr = new IO.Ajax(l_opt);
        this._url = a_ServiceURL;
    },
    Call: function (a_srvname, a_callback, a_arguments, req_sign) {
        var l_callurl = this._url + '/' + a_srvname;
        var l_poststr = '';

        var l_opt = {};
        if (typeof a_arguments == 'object') {
            for (var k in a_arguments) {
                var l_p = encodeURIComponent(a_arguments[k]);
                if (l_poststr == '') {
                    l_poststr = k + '=' + l_p;
                } else {
                    l_poststr += '&' + k + '=' + l_p;
                }
            }
            l_opt.postBody = l_poststr;
            //alert(req_sign + ' ' + l_poststr);
        }
        var l = this._cblist.length;
        this._cblist[l] = a_callback;
        if (req_sign) {
            this._xhr.get(l_callurl + (l_poststr ? ("?" + l_poststr) : ''), this._StdCallBack(this, l));
        } else {
            this._xhr.post(l_callurl, this._StdCallBack(this, l), l_opt);
        }
    },
    _StdCallBack: function (o, a_idx) {
        return function (rtn) {
            var l_cb = o._cblist[a_idx];
            delete o._cblist[a_idx];
            if (!rtn.responseText) {
                if (l_cb) l_cb();
                return;
            }

            try {
                eval("var v_l_data = " + rtn.responseText);

                /*
				 * var l_data = eval(rtn.responseText);
				 * 对于{t : "s"}这种JSON串会出错
				 */
            } catch (e) {
                //throw e;
                o._err_cb(4000, rtn.responseText);
                return;
            }

            if (v_l_data && v_l_data.__ERROR) {
                o._err_cb(8001, v_l_data.__ERROR, v_l_data.__ERRORNO);
            } else/* if(v_l_data)*/
            {
                if (l_cb) l_cb(v_l_data, rtn.responseText);
            }
            /*else{
				o._err_cb(8000);
			}*/
        }
    }
};
//基于Script的跨域SRV调用 Cross-site SRV
if (typeof IO == 'undefined') IO = {};
IO.XSRV2 = function () {
    this.Init.apply(this, arguments);
};
IO.XSRV2.CallbackList = [];
IO.XSRV2.prototype = {
    _url: null,
    _err_cb: null,
    _loader: null,
    Init: function (a_ServiceURL, a_ErrorCB, a_randomURL) {
        var l_opt = {};
        var o;
        this._err_cb = a_ErrorCB;
        this._url = a_ServiceURL;
        o = document.getElementsByTagName('head')[0];
        if (o) {
            this._loader = o;
        } else {
            this._loader = document.body;
        }
        this._randUrl = a_randomURL;
    },
    jshash: function (s) {
        var a, i, j, c, c0, c1, c2, r;
        var _s = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_$';
        var _r64 = function (s, b) {
            return ((s | (s << 6)) >>> (b % 6)) & 63;
        };
        a = [];
        c = [];
        for (i = 0; i < s.length; i++) {
            c0 = s.charCodeAt(i);
            if (c0 & ~255) {
                c0 = (c0 >> 8) ^ c0;
            }
            c.push(c0);
            if (c.length == 3 || i == s.length - 1) {
                while (c.length < 3) {
                    c.push(0);
                }
                a.push((c[0] >> 2) & 63);
                a.push(((c[1] >> 4) | (c[0] << 6)) & 63);
                a.push(((c[1] << 4) | (c[2] >> 2)) & 63);
                a.push(c[2] & 63);
                c = [];
            }
        }
        while (a.length < 16) {
            a.push(0);
        }
        r = 0;
        for (i = 0; i < a.length; i++) {
            r ^= (_r64(a[i] ^ (r | i), i) ^ _r64(i, r)) & 63;
        }
        for (i = 0; i < a.length; i++) {
            a[i] = (_r64((r | i & a[i]), r) ^ a[i]) & 63;
            r += a[i];
        }
        for (i = 16; i < a.length; i++) {
            a[i % 16] ^= (a[i] + (i >>> 4)) & 63;
        }
        for (i = 0; i < 16; i++) {
            a[i] = _s.substr(a[i], 1);
        }
        a = a.slice(0, 16).join('');
        return a;
    },
    Call: function (a_srvname, a_callback, a_arguments, a_cid) {
        var a = [];
        for (var k in a_arguments) {
            a.push(encodeURIComponent(k) + '=' + encodeURIComponent(a_arguments[k]));
        }
        if (this._randUrl) {
            a.push(encodeURIComponent(this._randUrl) + '=' + Math.floor(60466176 * Math.random()).toString(36));
        }
        a = a.join('&');
        var h, s, i;
        s = a_srvname + '?' + a;
        if (a_cid && a_cid !== true) {
            h = cid;
        } else {
            h = this.jshash(s);
            for (i = 0; IO.XSRV2.CallbackList[h]; i++) {
                h = this.jshash(s + '#@`' + i);
            }
        }
        var l_load_path = this._url + '/' +
            'IO.XSRV2.CallbackList[\'' + h + '\']' + '/' + a_srvname;
        if (a) {
            l_load_path += '?' + a;
        }
        var l_el = document.createElement("script");
        l_el.type = "text/javascript";
        l_el.src = l_load_path;
        IO.XSRV2.CallbackList[h] = function (a_data) {
            l_el.parentNode.removeChild(l_el);
            a_callback(a_data, h);
            delete IO.XSRV2.CallbackList[h];
        }
        this._loader.appendChild(l_el);
    }
};
// XHR Connections Pool
if (typeof IO == 'undefined') IO = {};
IO.Ajax = function () {
    this.Init.apply(this, arguments);
};

IO.Ajax.prototype = {
    _standbyXmlHttpArray: [],

    _pendingXmlHttpArray: [],
    Init: function (opts) {
        if (opts) {
            if (opts.nopool) this._nopool = true;
            if (opts.randomURL) this._randomURL = true;
            if (opts.err_cb) this._err_cb = opts.err_cb;
        }

    },
    get: function (url, callback, options) {
        if (typeof options == 'undefined') options = {};
        options.method = 'GET';
        this.request(url, callback, options);
    },
    post: function (url, callback, options) {
        if (typeof options == 'undefined') options = {};
        options.method = 'POST';
        this.request(url, callback, options);
    },
    request: function (url, callback, options) {
        var xhr = this.getXmlHttp();
        if (xhr) {
            if (!options) options = {};
            if (!options.method)
                options.method = 'GET';
            this._opt = options;
            this.registerXmlhttpCallback(xhr, callback);
            if (this._randomURL) {
                var tok = (url.indexOf('?') == -1) ? '?' : '&';
                url += tok + Math.round(Math.random() * 2147483648);
            }
            //	url += '?' + Math.round(Math.random() * 2147483648);
            //else
            //	url += '&' + Math.round(Math.random() * 2147483648);

            xhr.open(options.method, url, true);
            if (options.method = 'POST') {
                xhr.setRequestHeader('Content-type',
                    'application/x-www-form-urlencoded');
                xhr.send(options.postBody);
            } else xhr.send(null);

        } else {
            if (this._err_cb) this._err_cb(2000)
        }
    },
    registerXmlhttpCallback: function (xmlhttp, callback) {
        var c = this._pendingXmlHttpArray.length;
        this._pendingXmlHttpArray[c] = xmlhttp;
        xmlhttp.onreadystatechange = this._internalXmlhttpCallback(this, callback, c);
    },
    getXmlHttp: function () {
        if (this._nopool) {
            return this._createXmlHttp();
        }
        var xmlhttp = this._standbyXmlHttpArray.pop();

        if (!xmlhttp) {
            xmlhttp = this._createXmlHttp();
        }

        setTimeout(this._supplyXmlHttp(this), this._standbyXmlHttpArray.length * 20);

        return xmlhttp;
    },
    _supplyXmlHttp: function (obj) {
        return function () {
            try {
                while (obj._standbyXmlHttpArray.length < 5) {
                    obj._standbyXmlHttpArray.push(obj._createXmlHttp());
                }
            } catch (a) {
            }
        }
    },
    _createXmlHttp: function () {
        var xmlhttp = null;
        var b = "Msxml2.XMLHTTP";
        try {
            xmlhttp = new ActiveXObject(b);
        } catch (c) {
            try {
                b = "Microsoft.XMLHTTP";
                xmlhttp = new ActiveXObject(b);
            } catch (d) {
            }
        }
        if (!xmlhttp) {
            try {
                xmlhttp = new XMLHttpRequest;

            } catch (e) {

            }
        }
        return xmlhttp;
    },
    _internalXmlhttpCallback: function (obj, callback, index) {
        var a_cbable = false;
        return function () {
            var xmlhttp = obj._pendingXmlHttpArray[index];
            try {
                if (obj._opt.raw) {
                    a_cbable = true;
                } else {
                    if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
                        a_cbable = true;
                    }
                }
            } catch (d) {
                if (obj._err_cb) obj._err_cb(d);
            }
            if (a_cbable)
                callback(xmlhttp);

            if (xmlhttp.readyState == 4) {
                setTimeout(function () {
                        xmlhttp.onreadystatechange = obj._doNothing;
                        delete obj._pendingXmlHttpArray[index];
                    }
                    , 1000);
            }
        }
    },
    _doNothing: function () {
    }
}


if (typeof (S_Finance) == 'undefined') {
    S_Finance = {};
}
//处理数据获取及回调处理操作(IO操作)
S_Finance.IO = function () {
    this.Init.apply(this, arguments);
};
S_Finance.IO.prototype = {
    _scriptCharset: 'gb2312',
    _randomURL: false,

    Init: function (opts, a_parent) {
        var o;
        if (opts) {
            if (opts.randomURL) this._randomURL = true;
            if (opts.scriptCharset) this._scriptCharset = opts.scriptCharset;
        }
        if (a_parent)
            this._parent = a_parent;
        else {
            o = document.getElementsByTagName('head')[0];
            this._parent = o ? o : document.body;
        }
    },
    load: function (url, idname, callback, callback_valname) {
        if (this._randomURL) {
            var tok = (url.indexOf('?') == -1) ? '?' : '&';
            url += tok + Math.round(Math.random() * 2147483648);
        }

        var s = this._createScriptTag();
        if (callback) {
            if (typeof callback == 'string' && callback_valname) {
                this._load_by_remote_cb(s, url, callback, callback_valname, idname);
            } else
                this._load_by_local_cb(s, url, callback, idname);
        } else this._load_by_code(s, url, idname);

    },
    _load_by_local_cb: function (s, url, cb, idname) {
        s.charset = this._scriptCharset;
        s.src = url;
        s.id = idname;

        this._parent.appendChild(s);
        if (s.addEventListener) {
            s.addEventListener('load', this._chck_s_state(s, cb), false);
        } else if (s.attachEvent) {
            s.attachEvent('onreadystatechange', this._chck_s_state(s, cb));
        }
    },
    _load_by_remote_cb: function (s, url, cb_name, cb_valname, idname) {
        var tok = (url.indexOf('?') == -1) ? '?' : '&';
        url += tok + cb_valname + '=' + cb_name;
        s.charset = this._scriptCharset;
        s.src = url;
        s.id = idname;
        this._parent.appendChild(s);
        this._clean_up_script_tag(s);
    },
    _load_by_code: function (s, url, idname) {
        s.charset = this._scriptCharset;
        s.src = url;
        s.id = idname;
        this._parent.appendChild(s);
        this._clean_up_script_tag(s);
    },
    _createScriptTag: function () {
        var s = document.createElement('script');
        s.type = 'text/javascript';

        return s;
    },
    _clean_up_script_tag: function (s) {
        if (s.addEventListener) {
            s.addEventListener('load', this._rmvScriptTag(s), false);
        } else if (s.attachEvent) {
            s.attachEvent('onreadystatechange', this._rmvScriptTag(s));
        }
    },
    _chck_s_state: function (s, cb) {
        return function () {
            if (s.readyState) {
                if (s.readyState != 'loading') {
                    cb(s);
                }
            } else {
                cb(s);
            }
        }
    },
    _rmvScriptTag: function (s) {
        return function () {
            if (s.readyState) {
                if (s.readyState == 'loaded' || s.readyState == 'complete') {
                    s.parentNode.removeChild(s);
                }
            } else {
                s.parentNode.removeChild(s);
            }
        }
    }
};
/*
*	个股行情数据获取类及回调处理
*	输入：个股代码
*	输出：无。在获取行情数据时候，直接通过回调函数来处理后续操作
*/
S_Finance.QuotesDataLight = function () {
    this.Init.apply(this, arguments);
};
S_Finance.QuotesDataLight.prototype =
    {
        CODEPERQUERY: 100, //每次最多查询的代码个数
        SCRIPTIDPRE_: '_s_qdl_', //SCRIPT标签ID前缀
        QUOTESURL: '//hq.sinajs.cn/',
        JSVARPRE: 'hq_str_',
        _scriptidseq: 0,
        _iTmr: 0,
        _oTmr: null,
        _fReq: null,
        _ofio: null,
        _dp: S_Finance.cnProcess,
        Init: function (a_codes, a_intv, a_callback, a_dataProcesser) {
            //初始化script标签id
            this._arrCodeList = [];
            this._arrCodeType = [];
            this.SCRIPTIDPRE = this.SCRIPTIDPRE_ + (new Date().getTime() % 46656).toString(36) + '_';
            this._scriptidseq = 0;
            //
            if (a_dataProcesser) {
                this._dp = a_dataProcesser;
            } else {
                this._dp = S_Finance.cnProcess;
            }
            this._ofio = new S_Finance.IO();
            this._arrCodeList = a_codes;
            this._fReq = a_callback;
            this.changeTimer(a_intv);
        },
        changeCodes: function (a_codes, a_types) {
            this._aResult = [];
            this._arrCodeList = a_codes;
            if (a_types) {
                this._arrCodeType = a_types;
            }
            this.changeTimer(this._iTmr);
        },
        changeTimer: function (a_intv)
            //删除原来的定时器，使用新的定时器。
        {
            if (this._oTmr) {
                clearInterval(this._oTmr);
                this._oTmr = null;
            }
            if (a_intv) {
                this._oTmr = setInterval(this._timerCallback._Bind(this), a_intv);
                this._iTmr = a_intv;
            }
        },
        start: function () {
            this.changeTimer(this._iTmr);
        },
        stop: function () {
            this.changeTimer(0);
        },
        _timerCallback: function () {
            return this.request();
        },
        _getQueryExp: function (i)
            //i = index of _arrCodeList & _arrCodeType
        {
            var d, q;
            d = (this._arrCodeType[i] && S_Finance[this._arrCodeType[i]]) ? S_Finance[this._arrCodeType[i]] : this._dp;
            //alert([i, this._arrCodeType[i], S_Finance[this._arrCodeType[i]]]);
            if (!(q = d.QueryExp)) {
                q = this._arrCodeList[i];
            } else {
                q = typeof (q) == 'function' ? q(this._arrCodeList[i]) : q.replace('$1', this._arrCodeList[i]);
            }
            return this._specialchars(q);
        },
        request: function (p) {
            var a, i, u, g, j, q, s;
            this._aResult = [];
            s = '';
            if (typeof (p) == 'undefined' && this._dp.ExtraPara) {
                p = this._dp.ExtraPara;
            }
            if (typeof (p) == 'string') {
                s = p;
            } else if (typeof (p) == 'object') {
                s = [];
                for (var x in p) {
                    s.push(encodeURIComponent(x) + '=' + encodeURIComponent(p[x]));
                }
                s = s.join('&');
            }
            g = Math.ceil(this._arrCodeList.length / this.CODEPERQUERY);
            for (i = 0; i < g; i++) {
                a = this._getCodeSector(i);
                for (j = 0; j < a.length; j++) {
                    a[j] = this._getQueryExp(i * this.CODEPERQUERY + j);
                }
                if (s) {
                    s += '&';
                }
                u = this.QUOTESURL + 'rn=' + Math.round(Math.random() * 60466176).toString(36) + '&' + s + 'list=' + a.join(',');
                this._ofio.load(u, this.SCRIPTIDPRE + (this._scriptidseq++), this._reqCallback._Bind(this, i));
            }
        },
        _getCodeSector: function (s)//sector id
        {
            var g = Math.ceil(this._arrCodeList.length / this.CODEPERQUERY);
            return this._arrCodeList.slice(s * this.CODEPERQUERY,
                (s == g - 1) ? this._arrCodeList.length : (s + 1) * this.CODEPERQUERY);
        },
        _reqCallback: function (a_useless, s) {
            var i, a, q, p, n;
            a = this._getCodeSector(s);
            for (i = 0; i < a.length; i++) {
                n = s * this.CODEPERQUERY + i;
                q = this._getQueryExp(n);
                p = (this._arrCodeType[n] && S_Finance[this._arrCodeType[n]]) ? S_Finance[this._arrCodeType[n]] : this._dp;
                if (q.indexOf(',') > 0) { //新添加英股
                    q = q.split(',');
                    var _symbol = a[i].toLowerCase();
                    this._aResult[n] = this._absconv(p.ABSlongVer, _symbol, (window[this.JSVARPRE + q[0]]), (window[this.JSVARPRE + q[1]]));
                } else {
                    this._aResult[n] = this._absconv(((q.substr(0, 2) == 's_') ? p.ABSshortVer : p.ABSlongVer), _symbol, (window[this.JSVARPRE + q]));
                }

                // this._aResult[n] = this._absconv(((q.substr(0, 2) == 's_') ? p.ABSshortVer : p.ABSlongVer), (window[this.JSVARPRE + q]));
                //alert([this._arrCodeType[n],this.JSVARPRE + q,window[this.JSVARPRE + q]]);
            }
            this._checkReady();
        },
        _absconv: function (f, s, d, e) {
            var r, a;
            r = {};
            if (f instanceof Array) {
                if (d && d.indexOf(',') >= 0) {
                    a = d.split(',');
                    for (i = 0; i < f.length; i++) {
                        if (f[i] == 'cname' && e) {
                            e = e.split(',');
                            r['cname'] = e[0] || e[1] || a[i];
                        } else {
                            r[f[i]] = a[i];
                        }
                    }
                } else {
                    for (i = 0; i < f.length; i++) {
                        r[f[i]] = '';
                    }
                }
            } else if (typeof (f) == 'function') {
                r = f(d);
            }
            return r;
        },
        _specialchars: function (q) {
            return q.replace(/\./g, '').replace(/&/g, '');
        },
        _checkReady: function (a_notcall) {
            var i;
            for (i = 0; i < this._arrCodeList.length; i += this.CODEPERQUERY) {
                if (typeof (this._aResult[i]) == 'undefined') {
                    return false;
                }
            }
            if (!a_notcall) {
                if (this._fReq) {
                    this._fReq(this._aResult);
                }
            }
            return true;
        }
    };
//名称,今开,昨收,最新,最高,最低,买入,卖出,成交量,成交额,买1量,买1价,...,日期,时间
//将数据处理为易于操作的结构
S_Finance.cnProcess = {
    ABSshortVer: ['name', 'latestdeal', 'updownvol', 'updownrange', 'dealvol', 'dealprice'],
    ABSlongVer: ['name', 'todayopen', 'yesterdayclose', 'latestdeal', 'highdeal', 'lowdeal',
        'buy', 'sell', 'dealvol', 'dealprice', 'buyvol1', 'buyprice1',
        'buyvol2', 'buyprice2', 'buyvol3', 'buyprice3', 'buyvol4', 'buyprice4',
        'buyvol5', 'buyprice5', 'sellvol1', 'sellprice1', 'sellvol2', 'sellprice2',
        'sellvol3', 'sellprice3', 'sellvol4', 'sellprice4', 'sellvol5', 'sellprice5',
        'quotesdate', 'quotestime']
};

//证券类型,证券拼音,最近年度摊薄每股收益(eps),最近4个季度摊薄每股收益之和,今年迄今为止的每股收益,每股净资产,过去5个交易日平均每分钟成交量,总股本 (万股),流通股本(万股),流通A股 (万股),流通B股 (万股),计价单位
S_Finance.corpProcess = {
    QueryExp: '$1_i',
    ABSlongVer: ['type', 'name_py', 'eps', 'q4_eps_sum', 'latest_eps', 'nta', 'lastfive',
        'total_capital', 'curr_capital', 'curr_a_capital', 'curr_b_capital', 'price_currency']
};

S_Finance.forexProcess = {
//time,buy_price,sell_price,last_close_price,margin,open_price,high_price,low_price,close_price,name
//时间,买入价,卖出价,昨收盘,点差,开盘价,最高价,最低价,最新价,名称
    QueryExp: '$1',
    ABSlongVer: ['time', 'buy_price', 'sell_price', 'last_close_price', 'margin',
        'open_price', 'high_price', 'low_price', 'close_price', 'name']
};

S_Finance.fundNetProcess = {
//zqjc,dwjz,ljjz,zrjz,jzzz,jzrq
//证券简称,单位净值,累积单位净值,前单位净值,净值日期,基金规模
    QueryExp: 'f_$1',
    ABSlongVer: ['name', 'dwjz', 'ljdwjz', 'zrjz', 'date', 'jjgm']
};

S_Finance.fundProcess = {
//name,time,nav_chg,pre_nav,last_nav
//名称,时间,涨跌幅,净值,历史净值
//name,time,pre_nav,last_nav,accu_nav,wfzs,nav_chg
//基金名称,更新时间,最新净值,昨日净值,累计净值,五分钟涨速,涨跌幅
    QueryExp: 'fu_$1',
    ABSshortVer: ['name', 'time', 'nav_chg', 'pre_nav', 'last_nav'],
    ABSlongVer: ['name', 'time', 'pre_nav', 'last_nav', 'accu_nav', 'wfzs', 'nav_chg']
};

S_Finance.futuresProcess = {
    QueryExp: 'nf_$1',
    //name,CurrentTime,OpenPrice,HighPrice,LowPrice,ClosePrice,BidPrice,AskPrice,NewPrice,CurrentAccountsPrice,LastAccountsPrice,BidVol,AskVol,TotalVol,DealVol
    //期货名称，现在交易时间，开盘价，最高价，最低价，收盘价，竞买价，竞卖价，最新价，动态结算价,昨日结算价,买量，卖量，持仓量，成交量 交易所 品种 日期
    ABSlongVer: ['name', 'ticktime', 'open', 'high', 'low', 'close', 'bid', 'ask', 'trade', 'settlement', 'prevsettlement',
        'bidvol', 'askvol', 'ticktime', 'volume', 'market', 'contract', 'tradedate']
};

//将数据处理为易于操作的结构
//名称，点位，涨跌额，涨跌幅，日期时间
S_Finance.globalProcess = {
    QueryExp: 'b_$1',
    ABSlongVer: ['name', 'price', 'change', 'changerate', 'time']
};

//将数据处理为易于操作的结构
S_Finance.hkProcess = {
    QueryExp: 'hk$1',
    ABSlongVer: ['engname', 'name', 'open', 'prevclose', 'high', 'low', 'lasttrade',
        'change', 'changeRate', 'buy', 'sell', 'currentvolume', 'volume', 'PE', 'Yield', 'high_52week', 'low_52week',
        'Update_Date', 'ticktime']
};

S_Finance.hkrtProcess = {
    QueryExp: 'rt_hk$1',
    ABSlongVer: ['engname', 'name', 'open', 'prevclose', 'high', 'low', 'lasttrade',
        'change', 'changeRate', 'buy', 'sell', 'currentvolume', 'volume', 'PE', 'Yield', 'high_52week', 'low_52week',
        'Update_Date', 'ticktime']
};


S_Finance.usProcess = {
//cname,price,chg,updtime,diff,open,hign,low,52whigh,52wlow,volume,avgvol,mktcap,eps,pe,fpe,beta,dividend,yield,shares,instown,newprice,newchg,newdiff,newustime,ustime,prevclose
//名称，价格，涨跌，……
//cname中文名,price当前价,chg涨跌幅,updtime更新时间,diff涨跌额,open开盘价,
//hign最高价,low最低价,52whigh52周最高,52wlow52周最低,volume成交量,avgvol平均成交量,
//mktcap市值,eps每股收益,pe市盈率,fpe预期市盈率,beta贝塔系数,dividend股息,
//yield收益率,shares总股本,instown机构持股率,newprice,newchg,newdiff,
//newustime,ustime
    QueryExp: function (x) {
        return 'gb_' + x.toLowerCase();
    },
    ABSlongVer: ['cname', 'price', 'chg', 'updtime', 'diff', 'open', 'high', 'low', '_52whigh',
        '_52wlow', 'volume', 'avgvol', 'mktcap', 'eps', 'pe', 'fpe', 'beta', 'dividend', 'yield',
        'shares', 'instown', 'newprice', 'newchg', 'newdiff', 'newustime', 'ustime', 'prevclose']
};

S_Finance.futuresIndexProcess = {
//今开盘,最高价,最低价, 最新价,成交量,成交金额,持仓量,今收盘,今结算,涨停板价,跌停板价,昨虚实度,今虚实度, 昨收盘,昨结算,昨持仓量,申买价一,申买量一,申买价二,申买量二, 申买价三,申买量三,申买价四,申买量四,申买价五,申买量五,申卖价一,申卖量一, 申卖价二,申卖量二,申卖价三,申卖量三,申卖价四,申卖量四,申卖价五,申卖量五,交易日,行情时间,行情时间毫秒
    QueryExp: 'nf_$1',
    ABSlongVer: ['open', 'high', 'low', 'trade', 'volume', 'amount', 'position', 'close', 'settlement', 'tradingboard',
        'dailylimit', 'prevactual', 'actual', 'prevclose', 'prevsettlement', 'prevposition', 'buyprice1', 'buyvol1',
        'buyprice2', 'buyvol2', 'buyprice3', 'buyvol3', 'buyprice4', 'buyvol4', 'buyprice5', 'buyvol5', 'sellprice1',
        'sellvol1', 'sellprice2', 'sellvol2', 'sellprice3', 'sellvol3', 'sellprice4', 'sellvol4', 'sellprice5',
        'sellvol5', 'date', 'ticktime', 'microsec']
};

S_Finance.futuresGlobalProcess = {
    QueryExp: 'hf_$1',
    ABSlongVer: ['last', 'pricechange', 'bid', 'ask', 'high', 'low', 'timeupdate', 'prev', 'open', 'totalvol',
        'bidsize', 'asksize', 'currentvol', 'dateupdate']
};

S_Finance.twIndexProcess = {
    QueryExp: 'twi_$1',
    ABSshortVer: ['trade_day', 'time', 'pri', 'preclose', 'ttlvol', 'ttlamt', 'name'],
    ABSlongVer: ['trade_day', 'time', 'pri', 'open', 'preclose', 'high', 'low',
        'ttlvol', 'vol', 'ttlamt', 'ttldealrec', 'index', 'ttlbuyrec', 'ttlsellrec',
        'ttlbuyvol', 'ttlsellvol', 'cntup', 'cntdown', 'cnteve', 'cntupstop', 'cntdownstop',
        'cntundeal', 'outmkt', 'inmkt', 'cntbuy', 'cntsell', 'cntbuystop', 'cntsellstop', 'chgpct',
        'name']
};

//代码，品种,显示名称，最新价，均价，昨日均价，开盘价，最高价，最低价，昨收价,买价，卖价，买入量，卖出量，总成交量，总成交额，更新时间，涨跌幅
S_Finance.goldProcess = {
    QueryExp: 'SGE_$1',
    ABSlongVer: ['symbol', 'name', 'webname', 'trade', 'settlement', 'prevsettlement', 'open', 'high', 'low', 'close',
        'bid', 'ask', 'bidvol', 'askvol', 'volume', 'amount', 'ticktime', 'changepercent']
};

S_Finance.pureProcess = {
    QueryExp: '$1',
    ABSlongVer: ['value']
};

S_Finance.futuresGoodsProcess = {
    //商品代码 商品名称 最新价 涨跌 买价 买量 卖价 卖量 成交量 今开盘 昨结算 最高价 最低价
    QueryExp: 'bohai_exchange_$1',
    ABSlongVer: ['symbol', 'name', 'trade', 'change', 'buyprice', 'buyvol', 'sellprice', 'sellVol', 'volume', 'todayopen', 'yesterdaysettlement', 'high', 'low']
};

S_Finance.futuresComProcess = {
    //商品代码 商品名称 最新价 涨跌 买价 买量 卖价 卖量 成交量 今开盘 昨结算 最高价 最低价
    QueryExp: 'kunming_exchange_$1',
    ABSlongVer: ['symbol', 'name', 'trade', 'change', 'buyprice', 'buyvol', 'sellprice', 'sellVol', 'volume', 'todayopen', 'yesterdaysettlement', 'high', 'low']
};

S_Finance.matterGoldProcess = {
    //id 品牌标识 品牌 品种标志 产品 价格 单位 纯度 手工费 涨跌 日期
    QueryExp: 'gold_matter_$1',
    ABSlongVer: ['id_code', 'symbol', 'brandname', 'productssymbol', 'productsname', 'price', 'units', 'purity', 'handworkprice', 'wave', 'tradedate']
};
// JavaScript Document

//stock list framework

//stock list
//using namespace S_SL;

//SINA_STOCKLIST_CONFIG

if (typeof $C == 'undefined') $C = function (t) {
    return document.createElement(t)
};
if (typeof $T == 'undefined') $T = function (t) {
    return document.createTextNode(t)
};

S_SL = function () {
    this.Init.apply(this, arguments);
};
S_SL.WAITMASK = {a: {}};

S_SL.prototype =
    {
        VERSION: [1, 3, 14], //huangxu@2011-03-16
        //NOTICE: 版本控制并没有被强制使用
        _ARROWS: ['↑', '↓', '　', '　'],
        _IDPREFIX: 's_sl_id_',
        _CSSIGNORE: 's_sl_ignore',
        Init: function (base, divroot, objcfg, callback, pagediv, cpmf, sslcfg/*, ext*/)//
        {
            var t, i, o, config;
            //this._oEXT = ext ? ext : {};
            this._cp = null;
            this._srv = '';
            //服务对象
            this._sID = 'sl_sid_' + base + '_' + Math.floor(60466176 * Math.random()).toString(36);
            this._oDivRoot = null;
            this._oDivPage = null;
            this._oPage = {_aEvents: [], itemCount: -1};
            //翻页控件
            this._oTmr = {cnt: 0};
            //计时器相关
            this._aCodes = [];
            this._cntCol = 0;
            this._aEvents = [];
            this._aHead = [];
            this._aCells = [];
            this._aRows = [];
            this._aData = [];
            this._aExtData = {d: {}, f: {}};
            this._oCfg = this.MergeObj(this.MergeObj({}, window.SINA_STOCKLIST_CONFIG), sslcfg);
            //modify: user-defined config will be merged with the global one since ver 1.3.1
            config = this.MergeObj({}, objcfg);
            for (var x in config) {
                if (x.substr(0, 5) == 'EXFP_') {
                    this._oCfg[x.substr(5)] = config[x];
                    delete config[x];
                }
            }
            //modify: the frame now accepts frame-used params in cp-config params, with the suffix 'EXFP_'.
            //since ver 1.3.3
            this._initCP(base, config, cpmf);
            if (!this._cp)
            //ERROR! ERROR! ERROR!
            {
                return;
            }
            this._oCBCol = (typeof (callback) != 'object') ? {pc: callback} : callback;
            //modify: the callback param now may be an object, while use 'pc' to figure out param-change-event.
            //since ver 1.3.3
            //divroot can be a DOM-object or an ID
            this._oDivRoot = typeof (divroot) == 'string' ? document.getElementById(divroot) : divroot;
            if (t = this._searchBuiltTable()) {
                this._buildTable(t);
            } else {
                this._oDivRoot.appendChild(this._buildTable());
            }
            if (pagediv == '') {
                pagediv = [];
                //In previous version, this means not to create a page div.
                //After this update, it should be an empty array to create no page div.
            }
            if (!(pagediv instanceof Array)) {
                pagediv = [pagediv];
            }
            this._oDivPage = [];
            for (i = 0; i < pagediv.length; i++) {
                if (typeof (pagediv[i]) != 'function') {
                    o = document.getElementById(pagediv[i]);
                    if (!o) {
                        o = $C('div');
                        o.className = this._cp.css.pagediv;
                        this._oDivRoot.appendChild(o);
                    }
                } else {
                    o = pagediv[i];
                }
                this._oDivPage.push(o);
            }
            this._addEvent(window, 'scroll', this._scroll._BindForEvent(this));
            this._sortcss();
            this._getStockCount();
            this.SetReqAction('init');
            this._getDataFromService();
            this.ResetTimer();
            //this._createPageBtn();
        },
        _scroll: function () {
            this._makeWMScroll();
        },
        _searchBuiltTable: function () {
            var a = Util.FindChildren(this._oDivRoot, 'table');
            if (a && a.length) {
                return a[0];
            }
            return null;
        },
        GetDataAtOnce: function () {
            return this._getDataFromService();
        },
        ResetTimer: function () {
            this.EnableTimer(false);
            this.EnableTimer(true);
        },
        ClearEvent: function (x) {
            var i, o;
            for (o in this._aEvents) {
                if (!x || this._aEvents[o].o == x) {
                    Util.RemoveEvent(this._aEvents[o].o, this._aEvents[o].a, this._aEvents[o].f);
                }
            }
            this._aEvents = [];
        },
        RemoveMe: function () {
            this._extCallBack(this._cp.RemoveMe, [], this._cp);
            this._clearWaitMask();
            this.ClearEvent();
            this._clearPageBtn();
            while (this._oDivRoot.firstChild) {
                this._oDivRoot.removeChild(this._oDivRoot.firstChild);
            }
            if (this._oTmr.oTmr) {
                clearInterval(this._oTmr.oTmr);
            }
        },
        SetLength: function (l) {
            var p;
            l = parseInt(l);
            if (l > 0) {
                p = Math.floor((this._cp.svcParam.num * (this._cp.svcParam.page - 1)) / l) + 1;
                this._cp.svcParam.num = l;
                this._buildTable(l);
                this._sortcss();
                this.SetReqAction('setlen');
                this._cp.svcParam.page = p;
                if (!this._bLightMode && this._aCodes.length) {
                    this._aCodes = [];
                }
                this._getDataFromService();
                this._redrawPage();
            }
        },
        _addEvent: function (o, a, f) {
            this._aEvents.push({o: o, a: a, f: f});
            Util.AddEvent(o, a, f);
        },
        _createTitleTD: function (f)//f = field id
        {
            var a, td;
            td = $C((this._cp.aFields[f].s & 16) ? 'th' : 'td');
            if (this._cp.aFields[f].s & 2) {
                td.appendChild($T(this._cp.aFields[f].t));
            } else {
                a = $C('a');
                a.appendChild($T(this._cp.aFields[f].t));
                a.setAttribute('href', 'javascript:void(0);');
                this._addEvent(a, 'click', this._sort._BindForEvent(this, f));
                td.appendChild(a);
            }
            return td;
        },
        _highlight: function (ev, l, t) {
            if (typeof (this._iHighlight) != 'undefined') {
                this.ReplaceCss(this._getRow(l), this._cp.css.highlight, '');
            }
            if (t) {
                this.ReplaceCss(this._getRow(l), '', this._cp.css.highlight);
                this._iHighlight = l;
            }
        },
        _buildTable: function (a_len)
            //a_len: two-way auto param
            //       it means the length of expected table, while having type of int and this._oTable defined;
            //       it means the built table existing on the page, while having type of DOM and this._oTable undefined
        {
            var o, t, tb, tr, td, a, i, j, th, trg, tdg, a_cg, n, m;
            var a_st, a_en;
            var a_bOldTable, a_bNewTd, a_bNewTr;
            if (this._oTable) {
                a_st = this._aRows.length;
                a_en = a_len;
                t = this._oTable;
                tb = this._oTbody;
                if (a_en < this._aRows.length) {
                    j = this._searchField(this._cp.svcParam.sort);
                    for (i = a_en; i < this._aRows.length; i++) {
                        Util.HideObj(this._aRows[i], 1);
                        this.ReplaceCss(this._aCells[i][j], this._cp.css.sortasc, '');
                        this.ReplaceCss(this._aCells[i][j], this._cp.css.sortdesc, '');
                    }
                }
            } else {
                a_st = -1;
                a_en = this._cp.svcParam.num;
                if (a_len && a_len.nodeType) {
                    t = a_len;
                    a_bOldTable = 1;
                    //for (i = 0; i < t.childNodes.length; i++)
                    //{
                    //	if (!t.childNodes[i].tagName ||
                    //		(t.childNodes[i].tagName.toLowerCase() != 'thead' && t.childNodes[i].tagName.toLowerCase() != 'tbody'))
                    //	{
                    //		t.removeChild(t.childNodes[i]);
                    //		i--;
                    //	}
                    //}
                    th = Util.FindChildren(t, 'thead');
                    if (th && th[0]) {
                        th = th[0];
                    } else {
                        th = $C('thead');
                        //t.appendChild(th);
                    }
                    tb = Util.FindChildren(t, 'tbody');
                    if (tb && tb[0]) {
                        tb = tb[0];
                        a_en = (Util.FindChildren(tb, 'tr')).length;
                        if (a_en < this._cp.svcParam.num) {
                            a_en = this._cp.svcParam.num;
                        }
                    } else {
                        tb = $C('tbody');
                        //t.appendChild(tb);
                    }
                    this._aRows = Util.FindChildren(tb, 'tr');
                } else {
                    t = $C('table');
                    th = $C('thead');
                    tb = $C('tbody');
                }
            }
            if (a_bOldTable || this._cp.tdGroups)
            //The re-use of old table, the td-group and the colmap can not work with any other options.
            {
                delete this._oCfg.aColMap;
            }
            this._oThead = th;
            for (i = a_st; i < a_en; i++) {
                a_bNewTr = 1;
                if (a_bOldTable) {
                    if (i == -1) {
                        while (th.firstChild) {
                            th.removeChild(th.firstChild);
                        }
                        //tr = $C('tr');
                    } else {
                        tr = this._aRows[i];
                        if (!tr) {
                            tr = $C('tr');
                        } else {
                            this._aCells[i] = Util.FindChildren(tr, ['td', 'th']);
                            a_bNewTr = 0;
                        }
                        if (this._cp.aRowCss && this._cp.aRowCss.length) {
                            this.ReplaceCss(tr, '', this._cp.aRowCss[i % this._cp.aRowCss.length]);
                        }
                        Util.HideObj(tr, i >= this._cp.svcParam.num ? 1 : 0);
                        //tr.style.display = (i >= this._cp.svcParam.num) ? 'none' : '';
                    }
                }
                if (a_bNewTr || !a_bOldTable) {
                    tr = $C('tr');
                    if (i >= 0) {
                        Util.HideObj(tr, 1);
                        //tr.style.display = 'none';
                        this._aCells[i] = [];
                        this._aRows[i] = (tr);
                    }
                    if (this._cp.aRowCss && this._cp.aRowCss.length) {
                        this.ReplaceCss(tr, '', this._cp.aRowCss[i % this._cp.aRowCss.length]);
                    }
                }
                if (this._cp.tdGroups) {
                    trg = $C('tr');
                    a_cg = 0;
                }
                //for (j = 0; j < this._cp.aFields.length; j++)
                for (j = 0; j < this._colmap(); j++) {
                    m = this._colmap(j);
                    a_bNewTd = 1;
                    if (i == -1) {
                        td = this._createTitleTD(m);
                        this._aHead[j] = td;
                    } else {
                        if (!this._aCells[i][j]) {
                            td = $C((this._cp.aFields[m].s & 16) ? 'th' : 'td');
                            this._aCells[i][j] = td;
                        } else {
                            a_bNewTd = 0;
                        }
                    }
                    if (this._cp.aFields[m].c) {
                        td.className = this._cp.aFields[m].c;
                    }
                    if (a_bNewTd) {
                        if (trg && i == -1) {
                            n = this._cp.tdGroups[a_cg];
                            if (n && n.s == j) {
                                tdg = $C('th');
                                tdg.className = 'th_grouptitle ' + (n.c ? n.c : '');
                                try {
                                    tdg.appendChild(n.t);
                                } catch (ex) {
                                    tdg.appendChild($T(n.t));
                                }
                                tdg.colSpan = n.e - n.s + 1;
                                tdg.setAttribute('colSpan', n.e - n.s + 1);
                                trg.appendChild(tdg);
                            }
                            if (n && n.e >= j && n.s <= j) {
                                tr.appendChild(td);
                            } else {
                                td.rowSpan = 2;
                                td.setAttribute('rowSpan', 2);
                                trg.appendChild(td);
                            }
                            if (n && n.e == j) {
                                a_cg++;
                            }
                        } else {
                            tr.appendChild(td);
                        }
                    }
                }
                if (i >= 0) {
                    //tr.setAttribute('id', this._IDPREFIX + 'tr_' + i);
                    if (this._cp.css.highlight) {
                        this._addEvent(tr, 'mouseover', this._highlight._BindForEvent(this, i, 1));
                        this._addEvent(tr, 'mouseout', this._highlight._BindForEvent(this, i, 0));
                    }
                    if (a_bNewTr) {
                        tb.appendChild(tr);
                    }
                } else {
                    if (trg) {
                        th.appendChild(trg);
                    }
                    th.appendChild(tr);
                }
            }
            if (a_st < 0) {
                t.appendChild(th);
                t.appendChild(tb);
                t.border = '0';
                this._oTbody = tb;
            }
            this._cntCol = this._cp.aFields.length;
            return this._oTable = t;
        },
        _createPageLink: function (t, f, c, a_force)
            //Text, Event, Css
        {
            var a;
            if (f || a_force) {
                a = $C('a');
                a.appendChild($T(t));
                a.className = c;
                a.setAttribute('href', 'javascript:void(0);');
                this._oPage._aEvents.push({o: a, f: f});
                Util.AddEvent(a, 'click', f);
            } else {
                a = $C('span');
                a.appendChild($T(t));
                a.className = c;
                this.ReplaceCss(a, '', this._cp.css.pagedisabled);
            }
            return a;
        },
        _clearPageBtn: function () {
            var i, o;
            if (!this._oDivPage || !this._oDivPage.length) {
                return;
            }
            for (i = 0; i < this._oPage._aEvents.length; i++) {
                Util.RemoveEvent(this._oPage._aEvents[i].o, 'click', this._oPage._aEvents[i].f);
            }
            this._oPage._aEvents = [];
            for (i = 0; i < this._oDivPage.length; i++) {
                o = this._oDivPage[i];
                while (o.firstChild) {
                    o.removeChild(o.firstChild);
                }
            }
        },
        _createPageBtn: function (p) {
            var a, m, i, j, o;
            var p0, p1, cp1;
            if (!this._oDivPage || !this._oDivPage.length) {
                return;
            }
            if (!p) {
                p = this._cp.svcParam.page;
            }
            p = parseInt(p);
            m = Math.ceil(this._oPage.itemCount / this._cp.svcParam.num);//max page
            if (m < 0) {
                m = 0;
            }
            p0 = typeof (this._cp.pagetagl) == 'number' ? this._cp.pagetagl : (p - Math.floor(this._cp.pagetags / 2));
            p1 = typeof (this._cp.pagetagr) == 'number' ? this._cp.pagetagr : (p + Math.floor(this._cp.pagetags / 2));
            cp1 = this._cp.css.pageone ? this._cp.css.pageone : '';
            //p1 = p + Math.floor(this._cp.pagetags / 2);
            if (p0 < 1) {
                p0 = 1;
            }
            if (p1 > m) {
                p1 = m;
            }
            for (j = 0; j < this._oDivPage.length; j++) {
                if (typeof (this._oDivPage[j]) == 'function') {
                    this._oDivPage[j](m, p || 1, this._pageAct._Bind(this), this._oCfg);
                    //max page, current page, page action, config
                    //p may become nan when on the first page
                } else {
                    if (m <= 1 && !this._oCfg.bAlwaysShowPage) {
                        Util.HideObj(this._oDivPage[j], 1);
                        //this._oDivPage[j].style.display = 'none';
                        continue;
                    } else {
                        Util.HideObj(this._oDivPage[j], 0);
                        this._oDivPage[j].style.display = '';
                    }
                    o = this._oDivPage[j];
                    o.appendChild(this._createPageLink('上一页', p > 1 ? this._pageAct._BindForEvent(this, p - 1) : null, cp1));
                    for (i = p0; i <= p1; i++) {
                        if (i == p0 && p0 != 1) {
                            o.appendChild(this._createPageLink('1', this._pageAct._BindForEvent(this, 1), ''));
                            if (i - 1 > 1) {
                                o.appendChild($T('...'));
                            }
                        }
                        o.appendChild(this._createPageLink(i, i == p ? null : this._pageAct._BindForEvent(this, i), i == p ? 'pagecurr' : ''), 1);
                        if (i == p1 && p1 != m) {
                            if (i + 1 < m) {
                                o.appendChild($T('...'));
                            }
                            o.appendChild(this._createPageLink(m, this._pageAct._BindForEvent(this, m), ''));
                        }
                    }
                    o.appendChild(this._createPageLink('下一页', p < m ? this._pageAct._BindForEvent(this, p + 1) : null, cp1));
                }
            }
        },
        _pageAct: function (ev, p) {
            if (typeof (p) == 'undefined' && !isNaN(ev)) {
                p = parseInt(ev);
            }
            this._cp.svcParam.page = p;
            if (arguments[0]) {
                this.SetReqAction('page');
                this._getDataFromService();
            }
            this._redrawPage();
        },
        _redrawPage: function () {
            this._clearPageBtn();
            this._createPageBtn();
        },
        _searchField: function (f) {
            var i;
            for (i = 0; i < this._cp.aFields.length; i++) {
                if (this._cp.aFields[i].f == f) {
                    return i;
                }
            }
            return -1;
        },
        _sortcss: function (c)
            //c means clean
        {
            var p;
            //p = this._searchField(this._cp.svcParam.sort);
            p = this._colmap(this._cp.svcParam.sort);
            if (p >= 0) {
                if (c) {
                    this.ColCss(p, this._cp.css.sortasc, '');
                    this.ColCss(p, this._cp.css.sortdesc, '');
                } else {
                    this.ColCss(p, '', this._cp.css[this._cp.svcParam.asc ? 'sortasc' : 'sortdesc']);
                }
            }
        },
        _sort: function (ev, l) {
            var a_sortfield = this._cp.aFields[l].f;
            if ((this._cp.aFields[l].s & 2)) {
                return;
            }
            this._sortcss(1);
            if (a_sortfield == this._cp.svcParam.sort) {
                if (this._b3SSort && (!this._cp.svcParam.asc == !!(this._cp.aFields[l].s & 32))) {
                    this._cp.svcParam.sort = this._s_cpsort;
                    this._cp.svcParam.asc = this._i_cpasc;
                } else {
                    this._cp.svcParam.asc ^= 1;
                }
            } else {
                this._cp.svcParam.asc = (this._cp.aFields[l].s & 32) ? 1 : 0;
                this._cp.svcParam.sort = a_sortfield;
            }
            this._sortcss();
            this.SetReqAction('sort');
            this._getDataFromService();
        },
        ColCss: function (l, c, o)//coLomn, newCss, Oldcss
        {
            var i, r;
            for (i = -1; i < this._cp.svcParam.num; i++) {
                r = i < 0 ? (this._aHead[l]) : (this._aCells[i][l]);
                this.ReplaceCss(r, c, o);
            }
        },
        _getDataLight: function (f) {
            this._cp.GetDataLight(this._svcCallbackLight._Bind(this));
        },
        _getStockCount: function () {
            this._cp.GetListLength(this.SetStockCnt._Bind(this));
        },
        UpdateCount: function () {
            return this._getStockCount();
        },
        SetStockCnt: function (o) {
            o = o || {};
            if (isNaN(parseInt(o))) {
                o = o.total_num || 0;
            } else {
                o = parseInt(o);
            }
            if (this._oPage.itemCount != o) {
                this._oPage.itemCount = o;
                if (this._oPage.itemCount == 0 && !this._oCfg.bAllowEmptyData && !this._oDivEDTip) {
                    this._hideTable(1);
                    this._oDivEDTip = $C('div');
                    this._oDivEDTip.className = 'divEmptyData';
                    this._oDivEDTip.appendChild($T('没有符合条件的内容'));
                    this._oDivRoot.appendChild(this._oDivEDTip);
                }
                this._redrawPage();
            }
        },
        _getDataFromService: function (f)//f = from, 1 means from timer
        {
            var a, i, o, j;
            if (!f)
            //manual call
            {
                this.ParamChange();
                this._clearWaitMask();
                if (this._oCfg.iWaitMask) {
                    this._oTmrMsgBox = setTimeout(this._addWaitMask._Bind(this), this._oCfg.iWaitMask);
                }
            }
            if (this._bLightMode)
            //If it is running in light mode, it won't have to get a new list, as it is almost the same.
            {
                a = [];
                for (i = 0; i < this._aCodes.length; i++) {
                    for (j = 0; j < this._aListData.length; j++) {
                        if (this._aListData[j][this._cp.indexField] == this._aCodes[i]) {
                            a[i] = this._aListData[j];
                            break;
                        }
                    }
                }
                this._svcCallback(a);
            } else {
                this._oTmr.cnt = 0;
                this._cp.GetData(this._svcCallback._Bind(this));
            }
        },
        _addWaitMask: function () {
            var o, f;
            delete this._oTmrMsgBox;
            //if (!Util.MsgBox) return;
            S_SL.WAITMASK.a[this._sID] = 1;
            S_SL.WAITMASK.i = 1;
            if (!S_SL.WAITMASK.C) {
                S_SL.WAITMASK.C = $C('div');
                S_SL.WAITMASK.D = $C('div');
                S_SL.WAITMASK.C.style.cssText = 'z-index: 30000; position: absolute; margin: 0px; ' +
                    'background: black; filter: Alpha(opacity=0); opacity: 0; ';
                S_SL.WAITMASK.D.style.cssText = 'width: 220px; height: 70px' +
                    'px; z-index: 31000; position: absolute; ' +
                    'border-top: 1px #cccccc solid; border-left: 1px #cccccc solid; ' +
                    'border-bottom: 2px #666666 solid; border-right: 2px #666666 solid;';
                Util.AddEvent(S_SL.WAITMASK.C, 'click', this.StopPropagation);
                Util.AddEvent(S_SL.WAITMASK.C, 'mouseup', this.StopPropagation);
                o = $C('img');
                o.setAttribute('src', '//www.sinaimg.cn/cj/money/images/progress2.gif');
                o.setAttribute('alt', '读取中');
                o.setAttribute('border', 0);
                S_SL.WAITMASK.D.appendChild(o);
                document.body.appendChild(S_SL.WAITMASK.D);
                document.body.appendChild(S_SL.WAITMASK.C);
            }
            //this._addEvent(this._oDivCover, 'mouseover', this.Cover._BindForEvent(this));
            this._makeWMScroll();
            this._oTmrMsgBoxC = setTimeout(this._clearWaitMask._Bind(this), 15000);
            //this._oMsgBox = new Util.MsgBox(o, '');
            //this._oMsgBox.SetMask(3);
            //this._oMsgBox.SetSize(220, 70);
            //this._oMsgBox.Show('STOCK_LIST_LOADING');
            //this._oTmrMsgBoxC = setTimeout(this._clearWaitMask._Bind(this), 15000);
        },
        _makeWMScroll: function (e) {
            var s, t;
            if (!S_SL.WAITMASK.i) {
                return;
            }
            s = Util.getBodyDim();
            t = Util.getScrollTop();
            S_SL.WAITMASK.C.style.top = t + 'px';
            S_SL.WAITMASK.C.style.width = (s.W - 30) + 'px';
            S_SL.WAITMASK.C.style.height = s.H + 'px';
            S_SL.WAITMASK.D.style.left = (s.W - 220) / 2 + 'px';
            S_SL.WAITMASK.D.style.top = ((s.H - 70) / 2 + t) + 'px';
            //alert('showwait!');
            Util.HideObj(S_SL.WAITMASK.C, 0);
            Util.HideObj(S_SL.WAITMASK.D, 0);
            //S_SL.WAITMASK.C.style.display = '';
            //S_SL.WAITMASK.D.style.display = '';
        },
        _clearWaitMask: function () {
            var o, i, c;
            if (this._oTmrMsgBoxC) {
                clearTimeout(this._oTmrMsgBoxC);
                delete this._oTmrMsgBoxC;
            }
            delete S_SL.WAITMASK.a[this._sID];
            c = 0;
            for (var x in S_SL.WAITMASK.a) {
                c++;
            }
            S_SL.WAITMASK.i = c;
            if (c == 0 /*&& S_SL.WAITMASK.C*/) {
                //alert('hidewait!');
                Util.HideObj(S_SL.WAITMASK.C, 1);
                Util.HideObj(S_SL.WAITMASK.D, 1);
                //S_SL.WAITMASK.C.style.display = 'none';
                //S_SL.WAITMASK.D.style.display = 'none';
            }
            //if (this._oMsgBox)
            //{
            //	this._oMsgBox.CloseDialog();
            //	delete this._oMsgBox;
            //}
            if (this._oTmrMsgBox) {
                clearTimeout(this._oTmrMsgBox);
                delete this._oTmrMsgBox;
            }
        },
        StopPropagation: function (ev) {
            if (ev.stopPropagation) {
                ev.stopPropagation();
            }
            ev.cancelBubble = true;
        },
        _svcCallback: function (o) {
            return this._gotDataSvc(o, 0);
        },
        _svcCallbackLight: function (o) {
            //this.ResetTimer();
            var i, x;
            if (!o) {
                o = [];
            }
            x = [];
            for (i = 0; i < o.length; i++) {
                x[i] = this.MergeObj({}, o[i]);
                delete x[i][this._cp.indexField];
            }
            return this._gotDataSvc(x, 1);
        },
        MergeObj: Util.MergeObj,
        _dataFull: function (o) {
            var k, i;
            if (!o) {
                return true;
                //when there is no data, perhaps 'TRUE' is better
            }
            for (i = 0; i < this._cp.aFields.length; i++) {
                if (typeof (o[this._cp.aFields[i].f]) == 'undefined') {
                    return false;
                }
            }
            return true;
        },
        _gotDataSvc: function (o, l)//l = islight
        {
            var i, c, p, j, b, a, r, tmp, m;
            c = 0;
            b = o.length > this._cp.svcParam.num ? this._cp.svcParam.num * (this._cp.svcParam.page - 1) : 0;
            if (!l && o[0] && (this._bLightMode = !this._dataFull(o[0])))
            //the data service can not supply enough data.
            //we'll have to read the light data to get the full data,
            //and sort the data.
            {
                this._aLightData = [];
                for (i = 0; i < o.length; i++) {
                    this._aLightData[i] = o[i];
                    this._aCodes[i] = o[i][this._cp.indexField];
                }
                if (!this._aInitCodes) {
                    this._aInitCodes = this._aCodes.slice(0, this._aCodes.length);
                    ;
                }
                this._cp.CodesChange(this._aCodes);
                this._getDataLight(1);
                return;
            } else if (l && this._aLightData) {
                for (i = 0; i < o.length; i++) {
                    this.MergeObj(o[i], this._aLightData[i]);
                }
                p = this._searchField(this._cp.svcParam.sort);
                if (p >= 0) {
                    switch (this._cp.aFields[p].d) {
                        case -2:
                        case -3:
                            this.iSortMode = 2;
                            for (i = 0; i < o.length; i++) {
                                o[i][this._cp.svcParam.sort] = typeof (o[i][this._cp.svcParam.sort]) == 'string' ?
                                    o[i][this._cp.svcParam.sort].toString() : '';
                            }
                            break;
                        default:
                            this.iSortMode = 1;
                            for (i = 0; i < o.length; i++) {
                                if (typeof (o[i][this._cp.svcParam.sort]) == 'string' && o[i][this._cp.svcParam.sort] != '' &&
                                    !isNaN(o[i][this._cp.svcParam.sort])) {
                                    o[i][this._cp.svcParam.sort] = parseFloat(o[i][this._cp.svcParam.sort]);
                                }
                            }
                        //end switch
                    }
                    o.sort(this._sortCallback._Bind(this));
                    for (i = 0; i < o.length; i++) {
                        this._aCodes[i] = o[i][this._cp.indexField];
                    }
                } else if (this._aInitCodes) {
                    this._aCodes = this._aInitCodes.slice(0, this._aInitCodes.length);
                    p = {};
                    for (i = 0; i < this._aCodes.length; i++) {
                        p['smb_' + this._aCodes[i]] = i;
                    }
                    r = [];
                    for (i = 0; i < o.length; i++) {
                        r[p['smb_' + o[i][this._cp.indexField]]] = o[i];
                    }
                    o = r;
                }
                this._cp.CodesChange(this._aCodes);
            }
            //var debug_date = new Date();
            m = 0;
            for (i = b; i < this._cp.svcParam.num + b; i++) {
                r = o.length > this._cp.svcParam.num ? o[i] : o[i % this._cp.svcParam.num];
                if (r) {
                    m++;
                    this._sPrev = this._aCodes[i];
                    if (r[this._cp.indexField] && this._aCodes[i] != r[this._cp.indexField]) {
                        this._aCodes[i] = r[this._cp.indexField];
                        //顺序不能错！
                        c++;
                    }
                    if ((o.length > this._cp.svcParam.num && this._iCurrPage != this._cp.svcParam.page)) {
                        delete this._aData[i];
                    }
                    if (this._aData[i] && (typeof (r[this._cp.indexField]) == 'undefined' ||
                        this._aData[i][this._cp.indexField] == r[this._cp.indexField])) {
                        for (j = 0; j < this._cp.aFields.length; j++) {
                            if (this._cp.aFields[j].s & 64) {
                                continue;
                            }
                            tmp = this._cp.aFields[j].f;
                            if ((l && (this._cp.aFields[j].s & 512) && !this._bLightMode) ||
                                (typeof (r[tmp]) != 'undefined' && (r[tmp] == this._aData[i][tmp] /*||
							(r[j].nodeType && r[j].innerHTML == this._aData[i][j].innerHTML)*/)
                                    && !this._cp.aFields[j].u)) {
                                delete r[tmp];
                            }
                        }
                    }
                    this._replaceRow(r, i, this._aData[i]);
                    this._aData[i] = this.MergeObj(this._aData[i], r);
                } else {
                    if (this._aCodes.length > i) {
                        this._aCodes = this._aCodes.slice(0, i);
                    }
                    //delete this._aCodes[i];
                    this._replaceRow(undefined, i, this._aData[i]);
                }
            }
            //alert(new Date().getTime() - debug_date.getTime());
            this._iCurrPage = this._cp.svcParam.page;
            if (l && this._aLightData) {
                this._aListData = this._aLightData;
                this._aLightData = undefined;
            }
            this._sPrev = null;
            if (c) {
                this._cp.CodesChange(this._aCodes);
            }
            this._clearWaitMask();
            this._extCallBack(/*this._oEXT.fValidRow*/ this._oCBCol.vr, [m]);
        },
        _getRow: function (l) {
            return this._aRows[l];
        },
        _getText: function (o) {
            var i, s;
            if (!o.tagName && o.data) {
                return o.data;
            } else if (o.childNodes) {
                s = [];
                for (i = 0; i < o.childNodes.length; i++) {
                    //if (this.ReplaceCss(null, this._CSSIGNORE, '', o.childNodes[i].className) == o.childNodes[i].className)
                    if (o.childNodes[i].className && o.childNodes[i].className.indexOf(this._CSSIGNORE) < 0) {
                        s.push(this._getText(o.childNodes[i]));
                    } else if (!o.childNodes[i].tagName && o.childNodes[i].data) {
                        s.push(o.childNodes[i].data);
                    }
                }
                return s.join('');
            } else {
                return '';
            }
        },
        _replaceRow: function (o, l, s)//l = lineid, s = saveddata
        {
            var r, c, i, f, v, a, td, d, p, tmp, u, m;
            if (typeof (s) == 'undefined') {
                s = {};
            }
            r = this._getRow(l % this._cp.svcParam.num);
            if (!r) {
                return;
            }
            this._extCallBack(this._cp.OnRowRemove, [o, s, l, r], this._cp);
            //since v.1.3.4: CP will get a notice when a row is removing, so it will be able to recycle events if necessary.
            if (!o) {
                //if (r.style.display != 'none')
                //{
                //	r.style.display = 'none';
                //}
                Util.HideObj(r, 1);
                return;
            }
            if (this._oDivEDTip) {
                this._oDivEDTip.parentNode.removeChild(this._oDivEDTip);
                delete this._oDivEDTip;
                this._hideTable(0);
                //Util.HideObj(this._oTable, 0);
                //this._oTable.style.display = '';
            }
            //if (r.style.display)
            //{
            //	r.style.display = '';
            //alert(r.style.display);
            //}
            Util.HideObj(r, 0);
            //v = this._cp.aFields[this._cp.colorfield] ? o[this._cp.aFields[this._cp.colorfield].f] : undefined;
            v = o[this._cp.colorfield];
            if (typeof (v) != 'undefined') {
                v = parseFloat(v);
                c = r.className;
                c = this.ReplaceCss(null, this._cp.css.up, '', c);
                c = this.ReplaceCss(null, this._cp.css.down, '', c);
                c = this.ReplaceCss(null, this._cp.css.draw, '', c);
                c = this.ReplaceCss(null, this._cp.css.other, '', c);
                if (isNaN(v)) {
                    c = this.ReplaceCss(null, '', this._cp.css.other, c);
                } else {
                    if (v > 0) {
                        c = this.ReplaceCss(null, '', this._cp.css.up, c);
                    } else if (v < 0) {
                        c = this.ReplaceCss(null, '', this._cp.css.down, c);
                    } else {
                        c = this.ReplaceCss(null, '', this._cp.css.draw, c);
                    }
                }
                if (c != r.className) {
                    r.className = c;
                }
                //c releases
            }
            td = this._aCells[l % this._cp.svcParam.num];
            for (i = 0; i < td.length; i++) {
                c = td[i];
                //v = o[this._cp.aFields[i].f];
                m = this._colmap(i);
                v = o[this._cp.aFields[m].f];
                if (typeof (v) == 'undefined') {
                    continue;
                }
                if (v == '' && !(this._cp.aFields[m].s & 256) && (!this._bLightMode) &&
                    (!o[this._cp.indexField] || (this._sPrev == o[this._cp.indexField])))
                //remove bad data ...
                {
                    continue;
                }
                if (typeof (v) == 'string' && v == '' && !(this._cp.aFields[m].s & 256)) {
                    v = '--';
                }
                p = null;
                if ((this._cp.aFields[m].s & 64) && (!this._aLightData) && (!o[this._cp.indexField] ||
                    (this._sPrev == o[this._cp.indexField]))) {
                    p = this._aData[l][this._cp.aFields[m].f];
                }
                while (c.firstChild) {
                    c.removeChild(c.firstChild);
                }
                if (v && v.nodeType) {
                    c.appendChild(v);
                } else if (this._cp.aFields[m].s & 128) {
                    c.innerHTML = v;
                } else {
                    //v is the shown value while d is the real value
                    if (typeof (v) == 'number') {
                        if (isNaN(v)) {
                            v = 0;
                        }
                    }
                    d = v;
                    if (this._cp.aFields[m].d >= 0 && !isNaN(v)) {
                        v = parseFloat(v);
                        if (this._cp.aFields[m].o) {
                            v *= Math.pow(10, this._cp.aFields[m].o);
                        }
                        v = v.toFixed(this._cp.aFields[m].d);
                        if (this._cp.aFields[m].s & 8) {
                            v = this.AddComma(v);
                        }
                        if ((this._cp.aFields[m].s & 4) && parseFloat(d) > 0) {
                            v = '+' + v;
                        }
                    }
                    if (a = this._cp.aFields[m].p) {
                        v = a.replace('$1', v);
                    }
                    if (this._cp.aFields[m].s & 64) {
                        if (!isNaN(parseFloat(p))) {
                            p = this._compare(d, p, 1);
                            p = [1, 2, 0][p + 1];
                        } else {
                            p = 3;
                        }
                        v = this._ARROWS[p] + v.toString();
                        //v = $C('span');
                        //v.className = this._CSSIGNORE;
                        //v.appendChild($T(this._ARROWS[p]));
                        //c.insertBefore(v, c.firstChild);
                    }
                    if (tmp = this._extCallBack(this._cp[this._cp.aFields[m].u], [v, o, s, l, r], this._cp))
                    //if (typeof(this._cp[this._cp.aFields[m].u]) == 'function'
                    //	&& (tmp = this._cp[this._cp.aFields[m].u](v, o, s, l, r)))
                    {
                        c.appendChild(tmp);
                    } else if (this._cp.aFields[m].s & 1) {
                        a = $C('a');
                        u = this._cp.detailPage;
                        if (typeof (this._cp.detailPage) == 'function') {
                            if (this._aCodes[l].indexOf('fx_s') != -1) {
                                u = this._cp.detailPage(this._aCodes[l].toUpperCase());
                            } else {
                                u = this._cp.detailPage(this._aCodes[l]);
                            }

                        } else {
                            if (this._aCodes[l].indexOf('fx_s') != -1) {
                                u = Util.StrTemplate(this._cp.detailPage, this._aCodes[l].toUpperCase());
                            } else {
                                u = Util.StrTemplate(this._cp.detailPage, this._aCodes[l]);
                            }

                        }
                        // 针对外汇修改名字字段
                        if (u.indexOf('FX_S') != -1) {
                            u = u.replace('FX_S', '');
                        }
                        a.setAttribute('href', u);
                        if (!this._oCfg.bUseCurrentWindow) {
                            a.setAttribute('target', '_blank');
                        }
                        if (v.indexOf('fx_s') != -1) {
                            v = v.replace('fx_s', '').toUpperCase();
                        }
                        a.appendChild($T(v));
                        c.appendChild(a);
                        //IE Bug:
                        //If we set the href of an '<a>' node after appending a textnode including character '@',
                        //    the innerHTML will be the href rather than the textnode.
                        //The bugs occurs in IE6 and IE7, while fixed in IE8.
                        /*a = [];
					a.push('<a href="');
					a.push(Util.StrTemplate(this._cp.detailPage, this._aCodes[l]));
					a.push('" target="blank">');
					a.push(v);
					a.push('</a>');
					c.innerHTML = a.join('');*/
                    } else {
                        c.appendChild($T(v));
                        //c.innerHTML = v;
                    }
                }
            }
        },
        _compare: function (i1, i2, m)
            //item1, item2, mode
            //mode = 0 default
            //mode = 1 float
            //mode = 2 string
        {
            var x1, x2;
            try {
                if (i1 && i1.nodeType) {
                    i1 = this._getText(i1);
                }
                if (i2 && i2.nodeType) {
                    i2 = this._getText(i2);
                }
                switch (m) {
                    case 1:
                        i1 = parseFloat(i1);
                        i2 = parseFloat(i2);
                        if (isNaN(i1)) {
                            i1 = 0;
                        }
                        if (isNaN(i2)) {
                            i2 = 0;
                        }
                        break;
                    case 2:
                        i1 = i1 ? i1.toString() : '';
                        i2 = i2 ? i2.toString() : '';
                        break;
                }
                /*if (i1 == i2)
			{
				return 0;
			}
			else */
                if (i1 < i2) {
                    return -1;
                } else if (i1 > i2) {
                    return 1;
                }
                //alert([i1,i2,this._cp.svcParam.sort]);
                return 0;
            } catch (ex) {
                return 0;
            }
        },
        _sortCallback: function (o1, o2) {
            //ignore mode..
            //please check mode (d: -1 or -2) if nesseracy
            return -this._compare(o1[this._cp.svcParam.sort], o2[this._cp.svcParam.sort]
                , this.iSortMode
            ) * (0.5 - this._cp.svcParam.asc);
        },
        _timerCallback: function () {
            this._oTmr.cnt++;
            if (this._oTmr.disabled) {
                return;
            }
            if (!this._bLightMode && this._oTmr.cnt >= this._cp.tmrCntFull) {
                this._oTmr.cnt = 0;
                this.SetReqAction('auto');
                this._getDataFromService(1);
            } else {
                this._getDataLight();
            }
        },
        EnableTimer: function (e) //e = enabled
        {
            if (e && !this._oTmr.oTmr) {
                this._oTmr.cnt = 0;
                if (this._cp.tmrInt) {
                    this._oTmr.oTmr = setInterval(this._timerCallback._Bind(this), this._cp.tmrInt);
                }
            } else if (!e && this._oTmr.oTmr) {
                clearInterval(this._oTmr.oTmr);
                delete this._oTmr.oTmr;
            }
            this._extCallBack(this._cp.EnableTimer, [e], this._cp);
        },
        GetCodes: function () {
            return this._aCodes;
        },
        ReplaceCss: Util.ReplaceCss,
        AddComma: Util.AddComma,
        _initCP: function (b, p, f)
            //b = Base & p = Param
        {
            var s;
            try {
                this._cp = new window['S_SL_' + b.toUpperCase()](this, p);
            } catch (ex) {
                return;
            }
            this._extCallBack(f, [this._cp]);
            //if (f && typeof(f) == 'function')
            //{
            //	f(this._cp);
            //}
            /*if (typeof(this._cp.colorfield) == 'string' && isNaN(this._cp.colorfield))
		{
			this._cp.colorfield = this._searchField(this._cp.colorfield);
		}*/
            if (!isNaN(this._cp.colorfield)) {
                this._cp.colorfield = this._cp.aFields[this._cp.colorfield].f;
            }
            this._cp__defaultParam = this.MergeObj({}, this._cp.svcParam);
            s = p.sort;
            if (!s) {
                s = this._cp__defaultParam.sort;
            }
            this._s_cpsort = s;
            this._b3SSort = (this._searchField(s) < 0);
            //debug
            //this._b3SSort = 1;
            s = p.asc;
            if (typeof (s) == 'undefined') {
                s = this._cp__defaultParam.asc;
            }
            this._i_cpasc = s;
            this._paramFilter(p);
            this._extCallBack(this._cp.Init2, [f], this._cp);
            //if (typeof(this._cp.Init2) == 'function')
            //{
            //	this._cp.Init2(f);
            //}
        },
        _paramFilter: function (p) {
            var i, k;
            if (!p) {
                p = {};
            }
            for (k in this._cp.svcParam) {
                if (typeof (p[k]) != 'undefined') {
                    if (typeof (this._cp.svcParam[k]) == 'number') {
                        this._cp.svcParam[k] = parseFloat(p[k]);
                    } else {
                        this._cp.svcParam[k] = p[k];
                    }
                }
            }
        },
        ParamChange: function () {
            this._extCallBack(this._oCBCol.pc, [this._outputParam()]);
        },
        _outputParam: function () {
            var o, i;
            if (!this._cp.outputParam) {
                return this._cp.svcParam;
            }
            o = {};
            for (i = 0; i < this._cp.outputParam.length; i++) {
                if (typeof (this._cp.svcParam[this._cp.outputParam[i]]) != 'undefined' &&
                    this._cp.svcParam[this._cp.outputParam[i]] != this._cp__defaultParam[this._cp.outputParam[i]]) {
                    o[this._cp.outputParam[i]] = this._cp.svcParam[this._cp.outputParam[i]];
                }
            }
            return o;
        },
        ErrHandler: function (eid, info) {
            if (!this._oCfg.bAllowErrorData && !this._oDivEDTip) {
                this._hideTable(1);
                this._oDivEDTip = $C('div');
                this._oDivEDTip.className = 'divErrorData';
                this._oDivEDTip.appendChild($T('读取内容失败！请检查网络状况并刷新页面。'));
                this._oDivRoot.appendChild(this._oDivEDTip);
                this._clearWaitMask();
            }
        },
        _hideTable: function (a) {
            var i;
            Util.HideObj(this._oTable, a);
            //this._oTable.style.display = 'none';
            for (i = 0; i < this._oDivPage.length; i++) {
                if (typeof (this._oDivPage[i]) == 'function') {
                    this._oDivPage[i](0, 0, function () {
                    }, this._oCfg);
                } else {
                    Util.HideObj(this._oDivPage[i], a);
                }
                //this._oDivPage[i].style.display = 'none';
            }
        },
        SRVProvider: function (a_err, a_service, a_type, a_ext) {
            if (!a_err) {
                a_err = this.ErrHandler._Bind(this);
            }
            if (typeof (this._oCfg.iSRVType) != 'undefined' && typeof (a_type) == 'undefined') {
                a_type = this._oCfg.iSRVType;
            }
            this._sSrvCid = 'js_sl_' + Math.floor(60466176 * Math.random()).toString(36);
            try {
                switch (a_type) {
                    case 1: //xSRV
                        if (typeof (a_ext) == 'undefined') {
                            a_ext = this._oDivRoot;
                        }
                        if (!a_service) {
                            a_service = this._oCfg.sSRVPath ? this._oCfg.sSRVPath : '//vip.stock.finance.sina.com.cn/quotes_service/api/jsonp_v2.php';
                        }
                        this._srv = new IO.XSRV(a_service, a_err, a_ext);
                        this._iSrvType = 1;
                        break;
                    case 3: //xSRV2
                        if (!a_service) {
                            a_service = this._oCfg.sSRVPath ? this._oCfg.sSRVPath : '//vip.stock.finance.sina.com.cn/quotes_service/api/jsonp_v2.php';
                        }
                        this._srv = new IO.XSRV2(a_service, a_err);
                        this._iSrvType = 3;
                        break;
                    case 10: //英股
                        if (!a_service) {
                            a_service = this._oCfg.sSRVPath ? this._oCfg.sSRVPath : '//quotes.sina.cn/hq/api/jsonp.php';
                        }
                        this._srv = new IO.XSRV2(a_service, a_err);
                        this._iSrvType = 10;
                        break;
                    default: //srv
                        if (!a_service) {
                            a_service = this._oCfg.sSRVPath ? this._oCfg.sSRVPath : '/quotes_service/api/json_v2.php';
                        }
                        if (typeof (a_ext) == 'undefined') {
                            a_ext = false;
                        }
                        this._srv = new IO.SRV(a_service, a_err, a_ext);
                        this._iSrvType = 2;
                        break;
                }
            } catch (ex) {
                return null;
            }
            return this._srv;
        },
        SRVCall: function (a_svc, a_callback, a_param, a_ext, o_svc) {
            var a, o;
            if (!this._srv && !o_svc) {
                return;
            }
            o = o_svc ? o_svc : this._srv;
            if (typeof (a_ext) == 'undefined') {
                switch (this._iSrvType) {
                    case 1:
                        a_ext = [];
                        break;
                    case 2:
                    case 3:
                    default:
                        a_ext = [true];
                        break;
                }
            } else if (!(a_ext instanceof Array)) {
                a_ext = [a_ext];
            }
            o.Call.apply(this._srv, [a_svc, a_callback, a_param].concat(a_ext));
        },
        SetReqAction: function (a) {
            if (this._oCfg.bEnableSRA) {
                this._cp.svcParam._s_r_a = a;
            }
            this._aExtData.d.SRA = a;
        },
        ExtFuncStruct: function () {
            return this._aExtData.f;
        },
        ExtDataStruct: function () {
            return this._aExtData.d;
        },
        ExtFuncCall: function (f, p, o) {
            return this._extCallBack(this._aExtData.f[f], p, o);
        },
        _extCallBack: function (f, p, o) {
            if (typeof (f) == 'function') {
                return f.apply(o, p ? p : []);
            }
        },
        _colmap: function (f) {
            var x, m = this._oCfg.aColMap;
            var a = m instanceof Array;
            switch (typeof (f)) {
                case 'undefined':
                    return a ? m.length : this._cp.aFields.length;
                //undefined: return the length of mapped columns.
                case 'number':
                    return a ? this._searchField(m[f]) : f;
                //number: return the original index by the mapped one.
                case 'string':
                    if (a) {
                        x = Util.SL_ArraySearch(f, m);
                        return x == undefined ? -1 : x;
                    } else {
                        return this._searchField(f);
                    }
                //string: return the position in map.
            }
        }
    };

//javascript:alert(window.TreeMenu._loader.Load('BOHAI_FUTURE', 'tbl_wrap', {}));
//javascript:alert(window.TreeMenu._loader.Load('CN.CNTURNOVERR', 'tbl_wrap', {node: 'sh_a', sort: 'turnoverratio', asc: 0}));
//javascript:alert(window.TreeMenu._loader.Load('cn2', 'tbl_wrap', {node: 'sh_a', sort: 'turnoverratio', asc: 0, EXFP_aColMap : ['turnoverratio', 'symbol', 'name']}));
//javascript:alert(window.TreeMenu._loader.Load('cnl', 'tbl_wrap', {sort: 'symbol', asc: 1, codes: ['sz300001', 'sz300002', 'sz300003', 'sz300004', 'sz300005', 'sz300006', 'sz300007', 'sz300008', 'sz300009', 'sz300010', 'sz300011', 'sz300012', 'sz300013', 'sz300014', 'sz300015', 'sz300016', 'sz300017', 'sz300018', 'sz300019', 'sz300020', 'sz300021', 'sz300022', 'sz300023', 'sz300024', 'sz300025', 'sz300026', 'sz300027', 'sz300028'], EXFP_aColMap : ['symbol', 'name', 'trade', 'changepercent', 'open', 'turnoverratio', 'per', 'nmc']}));
//javascript:alert(window.TreeMenu._loader.Load('GOLD', 'tbl_wrap', {}));


// JavaScript Document

//stock list loader


S_SLLDR = function () {
    this.Init.apply(this, arguments);
};

S_SLLDR.prototype = {
    _oPrevObj: null,
    _sDivRoot: null,
    Init: function (sslcfg) {
        this._oCfg = sslcfg ? sslcfg : (window.SINA_STOCKLIST_CONFIG ? window.SINA_STOCKLIST_CONFIG : {});
    },
    Recycle: function () {
        if (this._oPrevObj) {
            this._oPrevObj.RemoveMe();
        }
        if (this._oDivRoot = (this._sDivRoot)) {
            while (this._oDivRoot.firstChild) {
                this._oDivRoot.removeChild(this._oDivRoot.firstChild);
            }
        }
    },
    Load: function (a_base, a_divid, a_config, a_callback, a_pagediv, a_sslcfg)
        //In order to copy LESS code, it will be possible to modify an existing class using a function.
        //The function and the class are passed by param 'base' being split by dot.
    {
        var o, f, b;
        if (this._oPrevObj) {
            this.Recycle();
        }
        if (!a_divid) {
            a_divid = this._sDivRoot;
        }
        if (!a_base) {
            return;
        }
        o = a_base.split('.');
        b = o[0];
        f = o[1] ? o[1] : '';
        if (typeof (window['S_SL_' + b.toUpperCase()]) != 'function') {
            return;
        }
        this._sDivRoot = (a_divid && a_divid.nodeType) ? a_divid : document.getElementById(a_divid);
        if (this._oDivRoot = this._sDivRoot) {
            while (!this._oCfg.bKeepDiv && this._oDivRoot.firstChild) {
                this._oDivRoot.removeChild(this._oDivRoot.firstChild);
            }
        }
        this._oPrevObj = new S_SL(b, a_divid, a_config, a_callback, a_pagediv,
            window['S_SL_CF_' + f.toUpperCase()], a_sslcfg);
        return this._oPrevObj;
    },
    EnableTimer: function (e)//enable
    {
        return this._oPrevObj ? this._oPrevObj.EnableTimer(e) : undefined;
    },
    ResetTimer: function () {
        return this._oPrevObj ? this._oPrevObj.ResetTimer() : undefined;
    },
    GetDataAtOnce: function () {
        return this._oPrevObj ? this._oPrevObj.GetDataAtOnce() : undefind;
    },
    SetLength: function (l) {
        return this._oPrevObj ? this._oPrevObj.SetLength(l) : undefined;
    },
    SetReqAction: function (a) {
        return this._oPrevObj ? this._oPrevObj.SetReqAction(a) : undefined;
    },
    UpdateCount: function () {
        return this._oPrevObj ? this._oPrevObj.UpdateCount() : undefined;
    }
};

//debug
//window._o_s_slldr = new S_SLLDR();

// javascript document
// work with stock_list.js

S_SL_ADR = function () {
    this.Init.apply(this, arguments);
};

S_SL_ADR.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'adr_hk'};
        this.css = {
            up: 'green', down: 'red', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'page', 'num', 'asc'];
        this.colorfield = 3;
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 1;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 48},
            {f: 'chname', t: '中文名称', d: -2, s: 50},
            {f: 'last', t: '最新价', d: 2, s: 64, c: 'colorize'},
            {f: 'chg', t: '涨跌额', d: 3, s: 4, c: 'colorize'},
            {f: 'pchg', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'prevclose', t: '昨收', d: 2, s: 0, c: 'colorize'},
            {f: 'open', t: '今开', d: 2, s: 0, c: 'colorize'},
            {f: 'high', t: '最高', d: 2, s: 0, c: 'colorize'},
            {f: 'low', t: '最低', d: 2, s: 0, c: 'colorize'},
            {f: 'volume', t: '成交量', d: 0, s: 8, c: 'colorize'},
            {f: 'amount', t: '成交额', d: 0, s: 8, c: 'colorize'},
            //{f:'high52', t:'52周最高', d:2, s:0, c:'colorize'},
            //{f:'low52', t:'52周最低', d:2, s:0, c:'colorize'},
            {f: 'vwap', t: '加权均价', d: 2, s: 0, c: 'colorize'}
            //,{f:'trade_time', t:'行情时间', d:-3, s:0}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1';
        this._aCodes = [];
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getADRData', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(o);
    },
    GetDataLight: function (cb) {
        cb([]);
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getADRCount', cb, this.svcParam);
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    }
};

// javascript document
// work with stock_list.js

S_SL_ANH = function () {
    this.Init.apply(this, arguments);
};

S_SL_ANH.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'hrap', asc: 0, node: 'aplush', X_optimeid: ''};
        this.css = {
            up: 'anh_up', down: 'anh_down', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'page', 'num', 'asc'];
        //this.colorfield = 4;
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 999999999;
        this.tmrCntFull = 10;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        //u: user_function, (public name required)
        this.aFields = [
            {f: 'name', t: '股票简称', d: -2, s: 51},
            {f: 'symbol', t: 'A股代码', d: -2, s: 49},
            {f: 'hsymbol', t: 'H股代码', d: -2, s: 48, u: 'HKLink'},
            {f: 'lasttrade', t: 'A股价格(RMB)', d: 2, s: 64, u: 'SetColor_cn'},
            {f: 'changepercent', t: 'A股涨跌幅', d: 3, s: 4, p: '$1%', u: 'SetColor_cn'},
            {f: 'hhlasttrade', t: 'H股价格(HKD)', d: 3, s: 64, u: 'SetColor_hk'},
            {f: 'hrlasttrade', t: 'H股价格(RMB)', d: 3, s: 0, u: 'SetColor_hk'},
            {f: 'hchangepercent', t: 'H股涨跌幅', d: 3, s: 4, p: '$1%', u: 'SetColor_hk'},
            {f: 'hrap', t: '比价H:A', d: 2, s: 0, p: '$1%'},
            {f: 'volume', t: 'A股成交量', d: 0, s: 8},
            {f: 'hvolume', t: 'H股成交量', d: 0, s: 8}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this));
        }
        this._srv = this._frame.SRVProvider();
    },
    A2H: function (a) {
        return this._arrA2H[a];
    },
    GetData: function (cb) {
        var s = Util.MergeObj({}, this.svcParam);
        delete s.X_optimeid;
        this._cbSVC = cb;
        if (typeof (cb) == 'function') {
            this._cbSVC = cb;
        }
        if (!this._arrA2H) {
            this._frame.SRVCall('Market_Center.getANHData', this._codeInit._Bind(this), s);
        } else {
            setTimeout(this._gotData._Bind(this), 10);
        }
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._svcChgCb) {
            this._svcChgCb(this._outputParam());
        }
    },
    _gotData: function () {
        var i, a;
        a = [];
        for (var x in this._arrA2H) {
            a.push({symbol: x, hsymbol: this._arrA2H[x]});
        }
        this._cbSVC(a);
    },
    GetDataLight: function (cb) {
        if (typeof (cb) == 'function') {
            this._cbQDL = cb;
        }
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getANHCount', cb,
            {node: this.svcParam.node});
    },
    _codeInit: function (o) {
        var i, a;
        a = [];
        this._arrA2H = {};
        if (!o) {
            o = [];
        }
        for (i = 0; i < o.length; i++) {
            this._arrA2H[o[i].a] = o[i].h;
        }
        for (var x in this._arrA2H) {
            a.push(x);
        }
        this.CodesChange(a);
        if (this._cbSVC) {
            this._gotData();
        }
    },
    CodesChange: function (c) {
        var i, a, t;
        this._aCodes = c;
        a = ['HKDCNY'];
        t = ['forexProcess'];
        for (i = 0; i < c.length; i++) {
            a.push(c[i]);
            t.push('cnProcess');
            a.push(this._arrA2H[c[i]]);
            t.push('hkrtProcess');
        }
        if (this._oQDL) {
            this._oQDL.changeCodes(a, t);
        }
    },
    _qdlCallback: function (a) {
        var i, r, j, t = '';
        this._fHKDCNY = parseFloat(a[0].close_price);
        //s=[];for(var x in a[0])s.push(x+':'+a[0][x]);alert(s);
        r = [];
        for (i = 1; i < a.length; i += 2) {
            r[(i - 1) / 2] = this._qdlObjConv(a[i], a[i + 1], this._aCodes[(i - 1) / 2]);
            j = [a[i + 1].Update_Date, a[i + 1].ticktime].join(' ');
            if (j > t) {
                t = j;
            }
        }
        i = this.svcParam.X_optimeid;
        if (typeof (i) == 'function') {
            i(t);
        } else {
            if ($(i)) {
                $(i).innerHTML = t;
            }
        }
        this._cbQDL(r);
    },
    _qdlObjConv: function (oa, oh, c) {
        var r;
        r = {
            name: oa.name, /*symbol : c, */hsymbol: this._arrA2H[c], lasttrade: parseFloat(oa.latestdeal),
            changepercent: parseFloat(oa.latestdeal) ? ((oa.latestdeal / oa.yesterdayclose) * 100 - 100) : 0,
            hhlasttrade: parseFloat(oh.lasttrade), hrlasttrade: parseFloat(oh.lasttrade) * this._fHKDCNY,
            hchangepercent: parseFloat(oh.lasttrade) ? ((oh.lasttrade / oh.prevclose) * 100 - 100) : 0,
            volume: oa.dealvol / 100, hvolume: oh.volume
        };
        r.hrap = (r.hrlasttrade ? r.hrlasttrade : oh.prevclose) / (r.lasttrade ? r.lasttrade : oa.yesterdayclose) * 100;
        return r;
    },
    SetColor_cn: function (v, o, s, l) {
        var c;
        c = parseFloat(typeof (o.changepercent) == 'undefined' ? s.changepercent : o.changepercent);
        c = (c > 0) ? ('anh_cn_up') : ((c < 0) ? ('anh_cn_down') : (''));
        return this._setColor_cb(v, c);
    },
    SetColor_hk: function (v, o, s, l) {
        var c;
        c = parseFloat(typeof (o.hchangepercent) == 'undefined' ? s.hchangepercent : o.hchangepercent);
        c = (c > 0) ? ('anh_hk_up') : ((c < 0) ? ('anh_hk_down') : (''));
        return this._setColor_cb(v, c);
    },
    _setColor_cb: function (v, c) {
        var o, n;
        o = document.createElement('span');
        o.appendChild(document.createTextNode(v));
        o.className = c;
        return o;
    },
    HKLink: function (v) {
        var a;
        a = document.createElement('a');
        a.setAttribute('href', 'http://stock.finance.sina.com.cn/hkstock/quotes/' + v + '.html');
        a.setAttribute('target', '_blank');
        a.appendChild(document.createTextNode(v));
        return a;
    }
};

// javascript document
// work with stock_list.js

S_SL_ANH_DELAY = function () {
    this.Init.apply(this, arguments);
};

S_SL_ANH_DELAY.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'hrap', asc: 0, node: 'aplush'};
        this.css = {
            up: 'anh_up', down: 'anh_down', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'page', 'num', 'asc'];
        //this.colorfield = 4;
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 6000;
        this.tmrCntFull = 6;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        //u: user_function, (public name required)
        this.aFields = [
            {f: 'name', t: '股票简称', d: -2, s: 51},
            {f: 'symbol', t: 'A股代码', d: -2, s: 49},
            {f: 'hsymbol', t: 'H股代码', d: -2, s: 48, u: 'HKLink'},
            {f: 'lasttrade', t: 'A股价格(RMB)', d: 2, s: 64, u: 'SetColor_cn'},
            {f: 'changepercent', t: 'A股涨跌幅', d: 3, s: 4, p: '$1%', u: 'SetColor_cn'},
            {f: 'hhlasttrade', t: 'H股价格(HKD)', d: 3, s: 64, u: 'SetColor_hk'},
            {f: 'hrlasttrade', t: 'H股价格(RMB)', d: 3, s: 0, u: 'SetColor_hk'},
            {f: 'hchangepercent', t: 'H股涨跌幅', d: 3, s: 4, p: '$1%', u: 'SetColor_hk'},
            {f: 'hrap', t: '比价H:A', d: 2, s: 0, p: '$1%'},
            {f: 'volume', t: 'A股成交量', d: 0, s: 8},
            {f: 'hvolume', t: 'H股成交量', d: 0, s: 8}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this));
        }
        this._srv = this._frame.SRVProvider();
    },
    A2H: function (a) {
        return this._arrA2H[a];
    },
    GetData: function (cb) {
        if (typeof (cb) == 'function') {
            this._cbSVC = cb;
        }
        if (!this._arrA2H) {
            this._frame.SRVCall('Market_Center.getANHData', this._codeInit._Bind(this), this.svcParam);
        } else {
            setTimeout(this._gotData._Bind(this), 10);
        }
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._svcChgCb) {
            this._svcChgCb(this._outputParam());
        }
    },
    _gotData: function () {
        var i, a;
        a = [];
        for (var x in this._arrA2H) {
            a.push({symbol: x, hsymbol: this._arrA2H[x]});
        }
        this._cbSVC(a);
    },
    GetDataLight: function (cb) {
        if (typeof (cb) == 'function') {
            this._cbQDL = cb;
        }
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getANHCount', cb,
            {node: this.svcParam.node});
    },
    _codeInit: function (o) {
        var i, a;
        a = [];
        this._arrA2H = {};
        if (!o) {
            o = [];
        }
        for (i = 0; i < o.length; i++) {
            this._arrA2H[o[i].a] = o[i].h;
        }
        for (var x in this._arrA2H) {
            a.push(x);
        }
        this.CodesChange(a);
        if (this._cbSVC) {
            this._gotData();
        }
    },
    CodesChange: function (c) {
        var i, a, t;
        this._aCodes = c;
        a = ['HKDCNY'];
        t = ['forexProcess'];
        for (i = 0; i < c.length; i++) {
            a.push(c[i]);
            t.push('cnProcess');
            a.push(this._arrA2H[c[i]]);
            t.push('hkProcess');
        }
        if (this._oQDL) {
            this._oQDL.changeCodes(a, t);
        }
    },
    _qdlCallback: function (a) {
        var i, r;
        this._fHKDCNY = parseFloat(a[0].close_price);
        //s=[];for(var x in a[0])s.push(x+':'+a[0][x]);alert(s);
        r = [];
        for (i = 1; i < a.length; i += 2) {
            r[(i - 1) / 2] = this._qdlObjConv(a[i], a[i + 1], this._aCodes[(i - 1) / 2]);
        }
        this._cbQDL(r);
    },
    _qdlObjConv: function (oa, oh, c) {
        var r;
        r = {
            name: oa.name, /*symbol : c, */hsymbol: this._arrA2H[c], lasttrade: parseFloat(oa.latestdeal),
            changepercent: parseFloat(oa.latestdeal) ? ((oa.latestdeal / oa.yesterdayclose) * 100 - 100) : 0,
            hhlasttrade: parseFloat(oh.lasttrade), hrlasttrade: parseFloat(oh.lasttrade) * this._fHKDCNY,
            hchangepercent: parseFloat(oh.lasttrade) ? ((oh.lasttrade / oh.prevclose) * 100 - 100) : 0,
            volume: oa.dealvol / 100, hvolume: oh.volume
        };
        r.hrap = (r.hrlasttrade ? r.hrlasttrade : oh.prevclose) / (r.lasttrade ? r.lasttrade : oa.yesterdayclose) * 100;
        return r;
    },
    SetColor_cn: function (v, o, s, l) {
        var c;
        c = parseFloat(typeof (o.changepercent) == 'undefined' ? s.changepercent : o.changepercent);
        c = (c > 0) ? ('anh_cn_up') : ((c < 0) ? ('anh_cn_down') : (''));
        return this._setColor_cb(v, c);
    },
    SetColor_hk: function (v, o, s, l) {
        var c;
        c = parseFloat(typeof (o.hchangepercent) == 'undefined' ? s.hchangepercent : o.hchangepercent);
        c = (c > 0) ? ('anh_hk_up') : ((c < 0) ? ('anh_hk_down') : (''));
        return this._setColor_cb(v, c);
    },
    _setColor_cb: function (v, c) {
        var o, n;
        o = document.createElement('span');
        o.appendChild(document.createTextNode(v));
        o.className = c;
        return o;
    },
    HKLink: function (v) {
        var a;
        a = document.createElement('a');
        a.setAttribute('href', 'http://stock.finance.sina.com.cn/hkstock/quotes/' + v + '.html');
        a.setAttribute('target', '_blank');
        a.appendChild(document.createTextNode(v));
        return a;
    }
};

// javascript document
// work with stock_list.js

S_SL_BOND = function () {
    this.Init.apply(this, arguments);
};

S_SL_BOND.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'hs_z'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 'changepercent';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 4000;
        this.tmrCntFull = 50;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'trade', t: '最新价', d: 2, s: 64, c: 'colorize'},
            {f: 'pricechange', t: '涨跌额', d: 2, s: 4, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'buy', t: '买入', d: 2, c: 'colorize'},
            {f: 'sell', t: '卖出', d: 2, c: 'colorize'},
            {f: 'settlement', t: '昨收', d: 2, c: 'colorize'},
            {f: 'open', t: '今开', d: 2, c: 'colorize'},
            {f: 'high', t: '最高', d: 2, c: 'colorize'},
            {f: 'low', t: '最低', d: 2, c: 'colorize'},
            {f: 'volume', t: '成交量/手', d: 0, s: 8},
            {f: 'amount', t: '成交额/万', d: 2, s: 8},
            {f: 'bar', t: '股吧', s: 18}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this));
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getHQNodeDataSimple', this._gotData._Bind(this), this.svcParam);
        if (this._svcChgCb) {
            this._svcChgCb(this._outputParam());
        }
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    _openstatus: function (t) {
        var x = t ? parseInt(t.replace(/\:/g, ''), 10) : 0;
        return (x >= 92500 && x < 150500);
    },
    _genStockLink: function (code) {
        var a, r, i;
        r = [];
        a = document.createElement('a');
        a.setAttribute('href', 'http://vip.stock.finance.sina.com.cn/portfolio/hqjia.php?symbol=' + code);
        a.setAttribute('target', '_blank');
        a.setAttribute('title', '加入我的自选');
        i = document.createElement('img');
        i.setAttribute('src', 'http://www.sinaimg.cn/cj/subject/2009/0618/images/addto_p.gif');
        i.setAttribute('alt', '关注');
        a.appendChild(i);
        r[0] = a;
        a = document.createElement('a');
        a.setAttribute('href', 'http://guba.sina.com.cn/bar.php?name=' + code);
        a.setAttribute('target', '_blank');
        a.setAttribute('title', '进入股票吧');
        i = document.createElement('img');
        i.setAttribute('src', 'http://www.sinaimg.cn/cj/subject/2009/0618/images/linkto_ba.gif');
        i.setAttribute('alt', '股吧');
        a.appendChild(i);
        r[1] = a;
        return r;
    },
    _dataPrepare: function (o, l) {
        var i, v, j, a, am, r, c, d;
        if (!o) {
            o = [];
        }
        a = ['per', 'pb'];
        am = ['volume'/*, 'amount', 'mktcap', 'nmc'*/];
        if (!l) {
            this._hqOpen = 0;
        }
        for (i = 0; i < o.length; i++) {
            if (!l) {
                this._hqOpen |= this._openstatus(o[i].ticktime);
            }
            c = o[i][this.indexField] ? o[i][this.indexField] : this._aCodes[i];
            r = this._genStockLink(c);
            for (j in a) {
                if (o[i][a[j]] && parseFloat(o[i][a[j]] < 0)) {
                    o[i][a[j]] = 'N/a';
                }
            }
            for (j in am) {
                if (o[i][am[j]] && !isNaN(parseFloat(o[i][am[j]]))) {
                    d = 1;
                    if (l && (c.substr(0, 4) == 'sh58' || c.substr(0, 4) == 'sh00')) {
                    } else if (l && (c.substr(0, 3) == 'sh1' || (c.substr(0, 4) >= 'sz10' && c.substr(0, 4) <= 'sz12') || c.substr(0, 3) == 'sh0')) {
                        d = 10;
                    } else {
                        d = 100;
                    }
                    o[i][am[j]] = parseFloat(o[i][am[j]]) / d;
                }
            }
            if (o[i] && !isNaN(parseFloat(o[i].amount))) {
                /*if (l && (c.substr(0, 3) == 'sh6' || c.substr(0, 3) == 'sh9' || c.substr(0, 4) == 'sz00' || c.substr(0, 4) == 'sz20'))
				{
					o[i].amount = parseFloat(o[i].amount) * 100;
				}*/
                o[i].amount = parseFloat(o[i].amount) / 10000;
            }
            o[i].zxg = r[0];
            o[i].bar = r[1];
            if (parseFloat(o[i].trade) == 0) {
                o[i].trade = '--';
            }
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._hqOpen && this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getHQNodeStockCountSimple', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        if (!a) {
            a = [];
        }
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlObjConv(a[i]);
        }
        this._cbQDL(this._dataPrepare(a, 1));
    },
    _qdlObjConv: function (o) {
        var s, d, i;
        s = ['todayopen', 'yesterdayclose', 'highdeal', 'lowdeal', 'latestdeal', 'dealvol', 'dealprice'];
        d = ['open', 'settlement', 'high', 'low', 'trade', 'volume', 'amount'];
        for (i = 0; i < s.length; i++) {
            if (typeof (o[s[i]]) != 'undefined') {
                o[d[i]] = o[s[i]];
                o[s[i]] = undefined;
            }
        }
        if (typeof (o.pricechange) == 'undefined') {
            o.pricechange = o.trade - o.settlement;
        }
        if (typeof (o.changepercent) == 'undefined') {
            o.changepercent = (o.pricechange / o.settlement) * 100;
        }
        if (o.trade == 0) {
            o.pricechange = o.changepercent = 0;
        }
        return o;
    }
};

S_SL_CF_CNZXG = function (o) {
    var i, x, a;
    x = {f: 'zxg', t: '收藏', s: 18};
    o.aFields.splice(o.aFields.length - 1, 0, x);
    a = ['trade', 'pricechange', 'changepercent', 'buy', 'sell', 'settlement', 'high', 'low', 'open'];
    for (i = 0; i < o.aFields.length; i++) {
        for (var y in a) {
            if (o.aFields[i].f == a[y]) {
                o.aFields[i].d = 3;
                break;
            }
        }
    }
};

S_SL_CF_FUNDEX = function (o) {
    var a = ['trade', 'pricechange', 'changepercent', 'buy', 'sell', 'settlement', 'high', 'low', 'open'];
    o.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1&country=fund';
    for (var x in o.aFields) {
        for (var y in a) {
            if (o.aFields[x].f == a[y]) {
                o.aFields[x].d = 3;
            }
        }
        if (o.aFields[x].f == 'bar') {
            o.aFields[x].t = '基金吧';
        }
    }
};

S_SL_CF_CNINDEX = function (o) {
    var i, a = ['buy', 'sell'];
    for (i = 0; i < o.aFields.length; i++) {
        for (var y in a) {
            if (o.aFields[i].f == a[y]) {
                o.aFields.splice(i, 1);
                i--;
                break;
            }
        }
    }
};

S_SL_CF_BOND = function (o) {
    var i;
    for (i = 0; i < o.aFields.length; i++) {
        if (o.aFields[i].s && o.aFields[i].f != 'symbol' && o.aFields[i].f != 'name') {
            o.aFields[i].s &= 0xfffffffe;
        }
    }
};


// javascript document
// work with stock_list.js

S_SL_CN = function () {
    this.Init.apply(this, arguments);
};

S_SL_CN.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'sh_a'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 'pricechange';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 4000;
        this.tmrCntFull = 50;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'trade', t: '最新价', d: 2, s: 64, c: 'colorize'},
            {f: 'pricechange', t: '涨跌额', d: 2, s: 4, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'buy', t: '买入', d: 2, c: 'colorize'},
            {f: 'sell', t: '卖出', d: 2, c: 'colorize'},
            {f: 'settlement', t: '昨收', d: 2, c: 'colorize'},
            {f: 'open', t: '今开', d: 2, c: 'colorize'},
            {f: 'high', t: '最高', d: 2, c: 'colorize'},
            {f: 'low', t: '最低', d: 2, c: 'colorize'},
            {f: 'volume', t: '成交量/手', d: 0, s: 8},
            {f: 'amount', t: '成交额/万', d: 2, s: 8},
            //{f:'per', t:'市盈利率', d:3},
            //{f:'pb', t:'市净率', d:3},
            //{f:'mktcap', t:'市值', d:2, s:8},
            //{f:'nmc', t:'流通市值', d:2, s:8},
            //{f:'turnoverratio', t:'换手率', d:2, p:'$1%'},
            {f: 'zxg', t: '收藏', s: 18},
            {f: 'bar', t: '股吧', s: 18}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this));
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getHQNodeData', this._gotData._Bind(this), this.svcParam);
    },
    _openstatus: function (t) {
        var x = t ? parseInt(t.replace(/\:/g, ''), 10) : 0;
        return (x >= 92500 && x < 150500);
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    _genStockLink: function (code) {
        var a, r, i;
        r = [];
        a = document.createElement('a');
        a.setAttribute('href', 'http://vip.stock.finance.sina.com.cn/portfolio/hqjia.php?symbol=' + code);
        a.setAttribute('target', '_blank');
        a.setAttribute('title', '加入我的自选');
        i = document.createElement('img');
        i.setAttribute('src', 'http://www.sinaimg.cn/cj/subject/2009/0618/images/addto_p.gif');
        i.setAttribute('alt', '收藏');
        a.appendChild(i);
        r[0] = a;
        a = document.createElement('a');
        a.setAttribute('href', 'http://guba.sina.com.cn/bar.php?name=' + code);
        a.setAttribute('target', '_blank');
        a.setAttribute('title', '进入股票吧');
        i = document.createElement('img');
        i.setAttribute('src', 'http://www.sinaimg.cn/cj/subject/2009/0618/images/linkto_ba.gif');
        i.setAttribute('alt', '股吧');
        a.appendChild(i);
        r[1] = a;
        return r;
    },
    _dataPrepare: function (o, l) {
        var i, v, j, a, am, r, c;
        a = ['per', 'pb'];
        am = ['volume'/*, 'amount', 'mktcap', 'nmc'*/];
        if (!o) {
            o = [];
        }
        if (!l) {
            this._hqOpen = 0;
        }
        for (i = 0; i < o.length; i++) {
            if (!l) {
                this._hqOpen |= this._openstatus(o[i].ticktime);
            }
            for (j in a) {
                if (o[i][a[j]] && parseFloat(o[i][a[j]] < 0)) {
                    o[i][a[j]] = 'N/a';
                }
            }
            for (j in am) {
                if (o[i][am[j]] && !isNaN(parseFloat(o[i][am[j]]))) {
                    o[i][am[j]] = parseFloat(o[i][am[j]]) / 100;
                }
            }
            if (o[i] && !isNaN(parseFloat(o[i].amount))) {
                o[i].amount = parseFloat(o[i].amount) / 10000;
            }
            c = o[i][this.indexField] ? o[i][this.indexField] : this._aCodes[i];
            r = this._genStockLink(c);
            o[i].zxg = r[0];
            o[i].bar = r[1];
            if (parseFloat(o[i].trade) == 0) {
                o[i].trade = '--';
            }
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._hqOpen && this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getHQNodeStockCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        if (!a) {
            a = [];
        }
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlObjConv(a[i]);
        }
        this._cbQDL(this._dataPrepare(a, 1));
    },
    _qdlObjConv: function (o) {
        var s, d, i;
        s = ['todayopen', 'yesterdayclose', 'highdeal', 'lowdeal', 'latestdeal', 'dealvol', 'dealprice'];
        d = ['open', 'settlement', 'high', 'low', 'trade', 'volume', 'amount'];
        for (i = 0; i < s.length; i++) {
            if (typeof (o[s[i]]) != 'undefined') {
                o[d[i]] = o[s[i]];
                delete o[s[i]];
            }
        }
        if (typeof (o.pricechange) == 'undefined') {
            o.pricechange = o.trade - o.settlement;
        }
        if (typeof (o.changepercent) == 'undefined') {
            o.changepercent = (o.pricechange / o.settlement) * 100;
        }
        if (o.trade == 0) {
            o.pricechange = o.changepercent = 0;
        }
        return o;
    }
};

S_SL_CF_CN_SH_B = function (o) {
    var a = ['trade', 'pricechange', 'changepercent', 'buy', 'sell', 'settlement', 'high', 'low', 'open'];
    for (var x in o.aFields) {
        for (var y in a) {
            if (o.aFields[x].f == a[y]) {
                o.aFields[x].d = 3;
            }
        }
    }
};

S_SL_CF_CNTURNOVERR = function (o) {
//{f:'turnoverratio', t:'换手率', d:2, p:'$1%'},
    var i, a = ['buy', 'sell'];
    for (i = 0; i < o.aFields.length; i++) {
        for (var y in a) {
            if (o.aFields[i].f == a[y]) {
                o.aFields.splice(i, 1);
                i--;
                break;
            }
        }
    }
    i = o.aFields.length - 2;
    o.aFields.splice(i, 0, {f: 'turnoverratio', t: '换手率', d: 2, p: '$1%'});
};

// javascript document
// work with stock_list.js

S_Finance.cnmrProcess =
    {
        QueryExp: '',
        ExtraPara: {format: 'text'},
        ABSlongVer: function (d) {
            return d;
        },
        p_NodeTable:
            {
                stock_sh_up_5min_20: {asc: 0},
                stock_sz_up_5min_20: {asc: 0},
                stock_hs_up_5min_20: {asc: 0},
                stock_shw_up_5min_20: {asc: 0},
                stock_szw_up_5min_20: {asc: 0},
                stock_hsw_up_5min_20: {asc: 0},
                stock_sh_down_5min_20: {asc: 1},
                stock_sz_down_5min_20: {asc: 1},
                stock_hs_down_5min_20: {asc: 1},
                stock_shw_down_5min_20: {asc: 1},
                stock_szw_down_5min_20: {asc: 1},
                stock_hsw_down_5min_20: {asc: 1}
            }
    }

S_SL_CNMR = function () {
    this.Init.apply(this, arguments);
};

S_SL_CNMR.prototype = {
    Init: function (f, p) {
        var o, n;
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'mr_percent', asc: 1, base: 'cnmr', node: 'stock_sh_up_5min_20'};
        this.css = {
            up: 'red', down: 'green', draw: '', other: '', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node'];
        this.colorfield = 'pricechange';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 12000;
        this.tmrCntFull = 1;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 51},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'trade', t: '最新价', d: 2, s: 66, c: 'colorize'},
            {f: 'pricechange', t: '涨跌额', d: 2, s: 6, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 3, s: 6, c: 'colorize', p: '$1%'},
            {f: 'mr_percent', t: '5分钟涨速', d: 2, s: 6, c: 'colorize', p: '$1%'},
            {f: 'settlement', t: '昨收', d: 2, s: 2, c: 'colorize'},
            {f: 'open', t: '今开', d: 2, s: 2, c: 'colorize'},
            {f: 'high', t: '最高', d: 2, s: 2, c: 'colorize'},
            {f: 'low', t: '最低', d: 2, s: 2, c: 'colorize'},
            {f: 'volume', t: '成交量/手', d: 0, s: 10},
            {f: 'amount', t: '成交额/万', d: 2, s: 10}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1';
    },
    Init2: function () {
        var o;
        o = S_Finance.cnmrProcess.p_NodeTable[this.svcParam.node];
        if (!o) {
            return; //error
        }
        this.svcParam = this._frame.MergeObj(this.svcParam, o);
        this._oQDLs = new S_Finance.QuotesDataLight([], 0, this._qdlsCB._Bind(this), S_Finance.cnmrProcess);
        this._oQDL = new S_Finance.QuotesDataLight([], 0, this._gotData._Bind(this));
        this._oQDLs.changeCodes([this.svcParam.node]);
        this._oQDLs.JSVARPRE = '';
    },
    _qdlsCB: function (o) {
        var i, m, a, l, c;
        m = o[0];
        a = [];
        c = [];
        if (!m) {
            m = [];
        }
        for (i = 0; i < m.length; i++) {
            c[i] = m[i].symbol;
            a[i] = {symbol: c[i], mr_percent: m[i].percent};
        }
        this._aMRData = a;
        this._aCode = c;
        this._oQDL.changeCodes(c);
        if (m.length) {
            this._oQDL.request();
        } else {
            this._gotData([]);
        }
    },
    GetData: function (cb) {
        if (this._oQDL) {
            this._cbSVC = cb;
            //不支持并发且回调不同的情况，实践中没有这种情况
            this._oQDLs.request();
        }
    },
    _gotData: function (o) {
        var r, i;
        r = [];
        for (i = 0; i < o.length; i++) {
            r[i] = this._frame.MergeObj(this._qdlObjConv(o[i]), this._aMRData[i]);
        }
        this._frame.SetStockCnt(r.length);//new function in ver 1.3.1
        this._cbSVC(r);
    },
    GetDataLight: function (cb) {
    },
    GetListLength: function (cb) {
        //return cb(this.svcParam.num);
    },
    CodesChange: function (c) {
    },
    _qdlObjConv: function (o) {
        var s, d, i;
        var v, j, a, am, r;
        a = ['per', 'pb'];
        s = ['todayopen', 'yesterdayclose', 'highdeal', 'lowdeal', 'latestdeal', 'dealvol', 'dealprice'];
        d = ['open', 'settlement', 'high', 'low', 'trade', 'volume', 'amount'];
        for (i = 0; i < s.length; i++) {
            if (typeof (o[s[i]]) != 'undefined') {
                o[d[i]] = o[s[i]];
                delete o[s[i]];
            }
        }
        if (typeof (o.pricechange) == 'undefined') {
            o.pricechange = o.trade - o.settlement;
        }
        if (typeof (o.changepercent) == 'undefined') {
            o.changepercent = (o.pricechange / o.settlement) * 100;
        }
        if (o.trade == 0) {
            o.pricechange = o.changepercent = 0;
            o.recentprice = o.settlement;
        } else {
            o.recentprice = o.trade;
        }
        for (j in a) {
            if (o[a[j]] && parseFloat(o[a[j]] < 0)) {
                o[a[j]] = 'N/a';
            }
        }
        o.volume = parseFloat(o.volume) / 100;
        o.amount = parseFloat(o.amount) / 10000;
        return o;
    }
};

//javascript:alert(TreeMenu._loader.Load('cnmr', 'tbl_wrap', {node: 'stock_sz_up_5min_20'}));// javascript document
// work with stock_list.js

S_SL_CREFUND = function () {
    this.Init.apply(this, arguments);
};

S_SL_CREFUND.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'crefund'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'page', 'num', 'asc'];
        this.colorfield = 'nav_chg';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 6000;
        this.tmrCntFull = 1;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'trade', t: '最新价', d: 3, s: 64, c: 'colorize'},
            //{f:'pricechange', t:'涨跌额', d:3, s:4, c:'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 3, s: 4, c: 'colorize', u: 'addperc'},
            //{f:'pre_nav', t:'最新估值', d:4, s:64, c:'colorize'},
            {f: 'last_nav', t: '最新公布净值', d: 4, c: 'colorize'},
            {f: 'accu_nav', t: '累计净值', d: 4, c: 'colorize'},
            {f: 'zrjz', t: '前单位净值', d: 4, c: 'colorize'},
            {f: 'nav_chg', t: '净值增长率', d: 4, s: 4, c: 'colorize', p: '$1%'},
            {f: 'date', t: '公布日期', d: -3},
            {f: 'jjgm', t: '基金规模', d: 3, c: 'colorize'}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1&country=fund';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.fundProcess);
        }
        this._fio = new S_Finance.IO();
        this._fio.load('//finance.sina.com.cn/iframe/286/20091218/26.js', '', this._gotList._Bind(this));
        this._srv = this._frame.SRVProvider();
    },
    _gotList: function () {
        var a, i, s, r, h, o;
        a = CREFUND.split(',');
        r = [];
        h = {};
        for (i = 0; i < a.length; i++) {
            s = a[i].split('|');
            if (s.length < 2 || !s[0] || s[0].length != 6) {
                continue;
            }
            o = ({symbol: s[0], c_type: s[1], enable_exc: parseInt(s[2]), market: s[3]});
            r.push(o);
            h[s[0]] = o;
        }
        this._hList = h;
        return this._aList = r;
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        setTimeout(this._gotData._Bind(this), 10);
        //不支持并发且回调不同的情况，实践中没有这种情况
    },
    _gotData: function () {
        if (this._aList) {
            this._cbSVC(this._aList);
            if (this._cbLen) {
                this._cbLen(this._aList.length);
                delete this._cbLen;
            }
        } else {
            setTimeout(this._gotData._Bind(this), 100);
        }
    },
    _dataPrepare: function (o, l) {
        var i, v, j, a, am, r, c;
        if (!o) {
            o = [];
        }
        if (l) {
            return o;
        }
        for (i = 0; i < o.length; i++) {
            c = o[i][this.indexField] ? o[i][this.indexField] : this._aCodes[i];
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        if (this._aList) {
            return cb(this._aList.length);
        } else {
            this._cbLen = cb;
        }
    },
    CodesChange: function (c) {
        var a, t, i, o;
        this._aCodes = c;
        a = [];
        t = [];
        for (i = 0; i < c.length; i++) {
            a.push(c[i]);
            t.push('fundNetProcess');
            //a.push(c[i]);
            //t.push('fundProcess');
            if (this._hList[c[i]].enable_exc) {
                a.push(this._hList[c[i]].market + c[i]);
                t.push('cnProcess');
            }
        }
        if (this._oQDL) {
            this._oQDL.changeCodes(a, t);
        }
    },
    _qdlCallback: function (d) {
        var a, t, i, o, r, c, j, h;
        r = [];
        c = this._aCodes;
        j = 0;
        for (i = 0; i < c.length; i++) {
            h = this._hList[c[i]];
            o = {
                symbol: c[i], name: d[j].name, trade: '--', changepercent: '--', last_nav: d[j].dwjz,
                accu_nav: d[j].ljdwjz, zrjz: d[j].zrjz, date: d[j].date,
                nav_chg: (d[j].dwjz - d[j].zrjz) / d[j].zrjz * 100, jjgm: d[j].jjgm
            };
            if (h.enable_exc && d[j + 1] && d[j + 1].latestdeal) {
                o.trade = d[j + 1].latestdeal;
                o.changepercent = (o.trade - d[j + 1].yesterdayclose) / d[j + 1].yesterdayclose * 100;
            } else {
                o.trade = o.changepercent = '--';
            }
            r.push(o);
            j += h.enable_exc ? 2 : 1;
        }
        this._cbQDL(r);
    },
    addperc: function (v, o, s, l, r) {
        var t;
        if (v != '--' && v != -100) {
            t = v + '%';
            Util.ReplaceCss(r, this.css.down, '');
            Util.ReplaceCss(r, this.css.up, '');
            Util.ReplaceCss(r, '', v > 0 ? this.css.up : (v < 0 ? this.css.down : ''));
        } else {
            t = '--';
        }
        return document.createTextNode(t);
    }
};
// javascript document
// work with stock_list.js

S_SL_FOREX = function () {
    this.Init.apply(this, arguments);
};

S_SL_FOREX.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'jbhl_forex'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'asc', 'node'];
        this.colorfield = 'changepercent';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 6;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'close_price', t: '最新价', d: 4, s: 64, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 4, s: 4, c: 'colorize', p: '$1%'},
            {f: 'pricechange', t: '涨跌额', d: 4, s: 4, c: 'colorize'},
            {f: 'open_price', t: '开盘价', d: 4, s: 64, c: 'colorize'},
            {f: 'high_price', t: '最高价', d: 4, s: 64, c: 'colorize'},
            {f: 'low_price', t: '最低价', d: 4, s: 64, c: 'colorize'},
            {f: 'margin', t: '振幅', d: 0, s: 0, c: 'colorize'},
            {f: 'last_close_price', t: '昨收价', d: 4, s: 64, c: 'colorize'},
            {f: 'buy_price', t: '买入价', d: 4, s: 64, c: 'colorize'},
            {f: 'sell_price', t: '卖出价', d: 4, s: 64, c: 'colorize'}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://finance.sina.com.cn/money/forex/hq/$1.shtml';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.forexProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getNameList', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(o);
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getNameCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        if (!a) {
            a = [];
        }
        for (i = 0; i < a.length; i++) {
            if (a[i].close_price && a[i].last_close_price) {
                a[i].pricechange = a[i].close_price - a[i].last_close_price;
                a[i].changepercent = a[i].pricechange / a[i].last_close_price * 100;
            }
            a[i].name = a[i].name.replace(/\u5373\u671f\u6c47\u7387/g, '');
        }
        this._cbQDL(a);
    }
};

// javascript document
// work with stock_list.js

S_SL_FUND = function () {
    this.Init.apply(this, arguments);
};

S_SL_FUND.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'jjycjz'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'page', 'num', 'asc'];
        this.colorfield = 5;
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 5;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'pre_nav', t: '最新估值', d: 4, s: 64, c: 'colorize'},
            {f: 'last_nav', t: '最新公布净值', d: 4, c: 'colorize'},
            {f: 'accu_nav', t: '累计净值', d: 4, c: 'colorize'},
            {f: 'nav_chg', t: '涨跌幅', d: 4, s: 4, c: 'colorize', p: '$1%'},
            //{f:'zxg', t:'关注', s:146},
            {f: 'bar', t: '基金吧', s: 146}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1&country=fund';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.fundProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getFundPrevData', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    _genStockLink: function (code) {
        var a, r, i;
        r = [];
        /*if (Util.isIE)
		{
			a = [];
			a.push('<a href="http://vip.stock.finance.sina.com.cn/myfund/fund_quick_jia.php?symbol=');
			a.push(code);
			a.push('" target="_blank" title="加入我的自选"><img src="http://www.sinaimg.cn/cj/subject/2009/0618/images/addto_p.gif" alt="关注" /></a>');
			r[0] = a.join('');
			a = [];
			a.push('<a href="http://guba.sina.com.cn/bar.php?name=of');
			a.push(code);
			a.push('" target="_blank" title="进入基金吧"><img src="http://www.sinaimg.cn/cj/subject/2009/0618/images/linkto_ba.gif" alt="基金吧" /></a>');
			r[1] = a.join('');
		}
		else*/
        {
            /*a = document.createElement('a');
			a.setAttribute('href', 'http://vip.stock.finance.sina.com.cn/myfund/fund_quick_jia.php?symbol=' + code);
			a.setAttribute('target', '_blank');
			a.setAttribute('title', '加入我的自选');
			i = document.createElement('img');
			i.setAttribute('src', 'http://www.sinaimg.cn/cj/subject/2009/0618/images/addto_p.gif');
			i.setAttribute('alt', '关注');
			a.appendChild(i);
			r[0] = a;*/
            a = document.createElement('a');
            a.setAttribute('href', 'http://guba.sina.com.cn/bar.php?name=of' + code);
            a.setAttribute('target', '_blank');
            a.setAttribute('title', '进入基金吧');
            i = document.createElement('img');
            i.setAttribute('src', '//www.sinaimg.cn/cj/subject/2009/0618/images/linkto_ba.gif');
            i.setAttribute('alt', '基金吧');
            a.appendChild(i);
            r[1] = a;
        }
        return r;
    },
    _dataPrepare: function (o, l) {
        var i, v, j, a, am, r, c;
        if (!o) {
            o = [];
        }
        if (l) {
            return o;
        }
        for (i = 0; i < o.length; i++) {
            c = o[i][this.indexField] ? o[i][this.indexField] : this._aCodes[i];
            r = this._genStockLink(c);
            //o[i].zxg = r[0];
            o[i].bar = r[1];
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getFundPrevCount', cb, this.svcParam);
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        this._cbQDL(this._dataPrepare(a), 1);
    }
};
// javascript document
// work with stock_list.js

S_SL_FUNDMONEY = function () {
    this.Init.apply(this, arguments);
};

S_SL_FUNDMONEY.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'moeny_fund'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 3;
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 600000;
        this.tmrCntFull = 5;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'dwjz', t: '每万份基金单位收益', d: 3, c: 'colorize'},
            {f: 'ljdwjz', t: '7日年化收益率', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'date', t: '公布日期', d: -3},
            {f: 'jjgm', t: '基金规模', d: 3, c: 'colorize'}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1&country=fund';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.fundNetProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getNameList', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(o);
    },
    _dataPrepare: function (o, l) {
        var i, v, j, a, am, r, c;
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getNameCount', cb, this.svcParam);
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        this._cbQDL(this._dataPrepare(a), 1);
    }
};

// javascript document
// work with stock_list.js

S_SL_FUNDNET = function () {
    this.Init.apply(this, arguments);
};

S_SL_FUNDNET.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'open_fund'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 5;
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 600000;
        this.tmrCntFull = 5;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM 256=允许空值
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'dwjz', t: '最新公布净值', d: 3, c: 'colorize'},
            {f: 'ljdwjz', t: '累计净值', d: 3, c: 'colorize'},
            {f: 'zrjz', t: '前单位净值', d: 3, c: 'colorize'},
            {f: 'jzzz', t: '净值增长率', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'date', t: '公布日期', d: -3},
            {f: 'jjgm', t: '基金规模', d: 3, c: 'colorize'}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1&country=fund';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.fundNetProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getFundNetData', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    _dataPrepare: function (o, l) {
        var i, v, j, a, am, r, c;
        if (!o) {
            o = [];
        }
        for (i = 0; i < o.length; i++) {
            o[i].jzzz = (o[i].zrjz && o[i].dwjz) ? (((o[i].dwjz / o[i].zrjz) - 1) * 100) : 0;
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getFundNetCount', cb, this.svcParam);
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        this._cbQDL(this._dataPrepare(a), 1);
    }
};

// javascript document
// work with stock_list.js

S_SL_FUTURES = function () {
    this.Init.apply(this, arguments);
};

S_SL_FUTURES.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'hj_qh'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 'changepercent';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 6000;
        this.tmrCntFull = 15;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 49},
            {f: 'trade', t: '最新价', d: 0, s: 64, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 2, s: 4, c: 'colorize', p: '$1%'}, //
            {f: 'close', t: '昨收', d: 0, c: 'colorize'},
            {f: 'open', t: '今开', d: 0, c: 'colorize'},
            {f: 'high', t: '最高', d: 0, c: 'colorize'},
            {f: 'low', t: '最低', d: 0, c: 'colorize'},
            {f: 'bid', t: '买入', d: 0, c: 'colorize'},
            {f: 'ask', t: '卖出', d: 0, c: 'colorize'},
            {f: 'settlement', t: '动态结算', d: 0, c: 'colorsize'},
            {f: 'prevsettlement', t: '昨日结算', d: 0, c: 'colorsize'},
            {f: 'bidvol', t: '买量', d: 0},
            {f: 'askvol', t: '卖量', d: 0},
            {f: 'volume', t: '成交量', d: 0, s: 8},
            {f: 'position', t: '持仓量', d: 0, s: 8}
            //{f:'bar', t:'股吧', s:18}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://finance.sina.com.cn/money/future/quote.html?$1';
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.futuresProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getHQFuturesData', this._gotData._Bind(this), this.svcParam);
        if (this._svcChgCb) {
            this._svcChgCb(this._outputParam());
        }
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    _genStockLink: function (code) {
        var a, r, i;
        a = document.createElement('a');
        a.setAttribute('href', 'http://guba.sina.com.cn/bar.php?name=' + code);
        a.setAttribute('target', '_blank');
        a.setAttribute('title', '进入股票吧');
        i = document.createElement('img');
        i.setAttribute('src', 'http://www.sinaimg.cn/cj/subject/2009/0618/images/linkto_ba.gif');
        i.setAttribute('alt', '股吧');
        a.appendChild(i);
        return a;
    },
    _dataPrepare: function (o) {
        var i, v, j, a, am, r, c;
        if (!o) {
            o = [];
        }
        for (i = 0; i < o.length; i++) {
            c = o[i][this.indexField] ? o[i][this.indexField] : this._aCodes[i];
            r = this._genStockLink(c);
            o[i].bar = r;
            if (typeof (o[i].changepercent) == 'undefined') {
                o[i].changepercent = parseFloat(o[i].trade) ? ((o[i].trade / o[i].prevsettlement - 1) * 100) : 0;
            } else {
                o[i].changepercent = (!isNaN(o[i].changepercent)) ? (parseFloat(o[i].changepercent) * 100) : 0;
            }
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getHQFuturesCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        if (!a) {
            a = [];
        }
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlObjConv(a[i]);
        }
        this._cbQDL(this._dataPrepare(a));
    },
    _qdlObjConv: function (o) {
        var re = /(\d{1,2})(?=(\d{2})+(?:$|\.))/g;
        if (o.ticktime) {
            o.ticktime.replace(re, "$1:");
        }
        return o;
    }
};


S_SL_CF_FU_DIG2 = function (o) {
    var a = ['trade', 'close', 'open', 'high', 'low', 'bid', 'ask', 'settlement', 'prevsettlement'];
    for (var x in o.aFields) {
        for (var y in a) {
            if (o.aFields[x].f == a[y]) {
                o.aFields[x].d = 2;
            }
        }
    }
};


// javascript document
// work with stock_list.js
S_SL_FUTURESCOM = function () {
    this.Init.apply(this, arguments);
};
S_SL_FUTURESCOM.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: arguments[1].node};
        this.css = {
            up: 'red', down: 'green', draw: '', other: '', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'asc', 'node'];
        this.colorfield = 'change';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 6;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 48},
            {f: 'name', t: '名称', d: -2, s: 50},
            {f: 'trade', t: '最新价', d: -1, s: 64, c: 'colorize'},
            {f: 'change', t: '涨跌', d: -1, s: 4, c: 'colorize'},
            {f: 'buyprice', t: '买价', d: -1, s: 64, c: 'colorize'},
            {f: 'buyvol', t: '买量', d: -1, s: 64, c: 'colorize'},
            {f: 'sellprice', t: '卖价', d: -1, s: 64, c: 'colorize'},
            {f: 'sellVol', t: '卖量', d: -1, s: 64, c: 'colorize'},
            {f: 'volume', t: '成交量', d: -1, s: 64, c: 'colorize'},
            {f: 'todayopen', t: '今开盘', d: -1, s: 64, c: 'colorize'},
            {f: 'yesterdaysettlement', t: '昨结算', d: -1, s: 64, c: 'colorize'},
            {f: 'high', t: '最高价', d: -1, s: 64, c: 'colorize'},
            {f: 'low', t: '最低价', d: -1, s: 64, c: 'colorize'}
        ];

        this.indexField = 'symbol';
        //this.detailPage = 'http://finance.sina.com.cn/money/forex/hq/$1.shtml';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.futuresComProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getFuturesSymbols', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(o);
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getFuturesCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        if (!a) {
            a = [];
        }
        this._cbQDL(a);
    }
};

// javascript document
// work with stock_list.js

S_SL_FUTURESGLOBAL = function () {
    this.Init.apply(this, arguments);
};

S_SL_FUTURESGLOBAL.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'qqqh_qh'};
        this.css = {
            up: 'global_up', down: 'global_down', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'page', 'num', 'asc'];
        this.colorfield = 'pricechange';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 6000;
        this.tmrCntFull = 6;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'pricechange', t: '涨跌额', d: 3, s: 4, c: 'colorize'},
            {f: 'last', t: '最新价', d: 3, s: 64, c: 'colorize'},
            {f: 'prev', t: '昨收', d: 3, c: 'colorize'},
            {f: 'open', t: '今开', d: 3, c: 'colorize'},
            {f: 'high', t: '最高', d: 3, c: 'colorize'},
            {f: 'low', t: '最低', d: 3, c: 'colorize'},
            {f: 'bid', t: '买入', d: 3, c: 'colorize'},
            {f: 'ask', t: '卖出', d: 3, c: 'colorize'},
            {f: 'bidsize', t: '买量', d: 0},
            {f: 'asksize', t: '卖量', d: 0},
            {f: 'totalvol', t: '持仓量', d: 0, s: 8}
        ];
        this.indexField = 'symbol';
        // this.detailPage = 'http://finance.sina.com.cn/money/future/quote_hf.html?$1';
        this.detailPage = 'https://finance.sina.com.cn/futures/quotes/$1.shtml';
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.futuresGlobalProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getFuturesGlobalData', this._gotData._Bind(this), this.svcParam);
        if (this._svcChgCb) {
            this._svcChgCb(this._outputParam());
        }
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    _dataPrepare: function (o) {
        var i, v, j, a, am, r, c;
        if (!o) {
            o = [];
        }
        for (i = 0; i < o.length; i++) {
            o[i].pricechange = o[i].last ? (o[i].last - o[i].prev) : 0;
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getFuturesGlobalCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        if (!a) {
            a = [];
        }
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlObjConv(a[i]);
        }
        this._cbQDL(this._dataPrepare(a));
    },
    _qdlObjConv: function (o) {
        var re = /(\d{1,2})(?=(\d{2})+(?:$|\.))/g;
        if (o.ticktime) {
            o.ticktime.replace(re, "$1:");
        }
        return o;
    }
};

// javascript document
// work with stock_list.js

S_SL_FUTURESGOODS = function () {
    this.Init.apply(this, arguments);
};

S_SL_FUTURESGOODS.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'bohai_qh'};
        this.css = {
            up: 'red', down: 'green', draw: '', other: '', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'asc', 'node'];
        this.colorfield = 'change';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 6;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 48},
            {f: 'name', t: '名称', d: -2, s: 50},
            {f: 'trade', t: '最新价', d: -1, s: 64, c: 'colorize'},
            {f: 'change', t: '涨跌', d: -1, s: 4, c: 'colorize'},
            {f: 'buyprice', t: '买价', d: -1, s: 64, c: 'colorize'},
            {f: 'buyvol', t: '买量', d: -1, s: 64, c: 'colorize'},
            {f: 'sellprice', t: '卖价', d: -1, s: 64, c: 'colorize'},
            {f: 'sellVol', t: '卖量', d: -1, s: 64, c: 'colorize'},
            {f: 'volume', t: '成交量', d: -1, s: 64, c: 'colorize'},
            {f: 'todayopen', t: '今开盘', d: -1, s: 64, c: 'colorize'},
            {f: 'yesterdaysettlement', t: '昨结算', d: -1, s: 64, c: 'colorize'},
            {f: 'high', t: '最高价', d: -1, s: 64, c: 'colorize'},
            {f: 'low', t: '最低价', d: -1, s: 64, c: 'colorize'}
        ];

        this.indexField = 'symbol';
        //this.detailPage = 'http://finance.sina.com.cn/money/forex/hq/$1.shtml';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.futuresGoodsProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getBohaiSymbols', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(o);
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getBohaiCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        if (!a) {
            a = [];
        }
        this._cbQDL(a);
    }
};

// javascript document
// work with stock_list.js

S_SL_FUTURESINDEX = function () {
    this.Init.apply(this, arguments);
};

S_SL_FUTURESINDEX.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'qz_qh'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 'pricechange';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 6000;
        this.tmrCntFull = 5;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'trade', t: '最新价', d: 1, s: 64, c: 'colorize'},
            {f: 'pricechange', t: '涨跌额', d: 1, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 2, c: 'colorize', p: '$1%'},
            {f: 'prevclose', t: '昨收', d: 1, c: 'colorize'},
            {f: 'open', t: '今开', d: 1, c: 'colorize'},
            {f: 'high', t: '最高', d: 1, c: 'colorize'},
            {f: 'low', t: '最低', d: 1, c: 'colorize'},
            //{f:'settlement', t:'今结算', d:1, c:'colorsize'},
            {f: 'prevsettlement', t: '昨结算', d: 1, c: 'colorsize'},
            {f: 'volume', t: '成交量', d: 0, s: 8},
            {f: 'position', t: '持仓量', d: 0, s: 8}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://finance.sina.com.cn/futures/quotes/$1.shtml';
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.futuresIndexProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getNameList', this._gotData._Bind(this), this.svcParam);
        if (this._svcChgCb) {
            this._svcChgCb(this._outputParam());
        }
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    _genStockLink: function (code) {
        var a, r, i;
        a = document.createElement('a');
        a.setAttribute('href', 'http://guba.sina.com.cn/bar.php?name=' + code);
        a.setAttribute('target', '_blank');
        a.setAttribute('title', '进入股票吧');
        i = document.createElement('img');
        i.setAttribute('src', '//www.sinaimg.cn/cj/subject/2009/0618/images/linkto_ba.gif');
        i.setAttribute('alt', '股吧');
        a.appendChild(i);
        return a;
    },
    _dataPrepare: function (o) {
        var i, v, j, a, am, r, c;
        if (!o) {
            o = [];
        }
        for (i = 0; i < o.length; i++) {
            c = o[i][this.indexField] ? o[i][this.indexField] : this._aCodes[i];
            r = this._genStockLink(c);
            o[i].bar = r;
            if (o[i].symbol) {
                o[i].name = o[i].symbol.replace('IF', '期指');
            }
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getNameCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        if (!a) {
            a = [];
        }
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlObjConv(a[i]);
        }
        this._cbQDL(this._dataPrepare(a));
    },
    _qdlObjConv: function (o) {
        var re = /(\d{1,2})(?=(\d{2})+(?:$|\.))/g;
        if (o.ticktime) {
            o.ticktime.replace(re, "$1:");
        }
        if (typeof (o.pricechange) == 'undefined') {
            o.pricechange = o.trade - o.prevsettlement;
        }
        if (typeof (o.changepercent) == 'undefined') {
            o.changepercent = (o.pricechange / o.prevsettlement) * 100;
        }
        if (o.trade == 0) {
            o.pricechange = o.changepercent = 0;
        }
        //if (o.volume)
        {
            //	o.volume = o.volume / 10000;
            //	o.amount = o.amount / 10000;
        }
        return o;
    }
};

// javascript document
// work with stock_list.js

S_SL_GLOBAL = function () {
    this.Init.apply(this, arguments);
};

S_SL_GLOBAL.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: '_', asc: 1, node: 'asia_hqgz'};
        this.css = {
            up: 'global_up', down: 'global_down', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 4;
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 6000;
        this.tmrCntFull = 6;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'area', t: '国家或地区', d: -2, s: 48},
            {f: 'symbol', t: '代码', d: -2, s: 48},
            {f: 'name', t: '名称', d: -2, s: 50},
            {f: 'price', t: '最新价', d: 2, s: 64, c: 'colorize'},
            {f: 'change', t: '涨跌额', d: 2, s: 4, c: 'colorize'},
            {f: 'changerate', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'time', t: '行情时间', s: 2, d: -3}
        ];
        this.indexField = 'symbol';
        this.detailPage = '';
        this.cssPrefix = 's_sl_global_css_';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.globalProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getGlobalIndex', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(o);
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getGlobalIndexCount', cb, this.svcParam);
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        this._cbQDL(a);
    }
};

// javascript document
// work with stock_list.js

S_SL_GOLD = function () {
    this.Init.apply(this, arguments);
};

S_SL_GOLD.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'webname', asc: 1, node: 'sge_gold'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'asc', 'node', 'page'];
        this.colorfield = 'changepercent';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 6;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'webname', t: '品种', d: -2, s: 48, u: 'NameLink'},
            {f: 'name', t: '名称', d: -2, s: 50},
            {f: 'trade', t: '最新价', d: 2, s: 64, c: 'colorize'},
            {f: 'open', t: '开盘价', d: 2, c: 'colorize'},
            {f: 'high', t: '最高价', d: 2, c: 'colorize'},
            {f: 'low', t: '最低价', d: 2, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 2, s: 4, c: 'colorize', p: '$1'},
            {f: 'close', t: '昨收价', d: 2, c: 'colorize'},
            {f: 'volume', t: '总成交量', d: 2, s: 8},
            {f: 'amount', t: '总成交额', d: 2, s: 8},
            {f: 'ticktime', t: '更新时间', s: 16, d: -3},
            {f: 'lsjy', t: '历史交易', s: 2}
        ];
        this.indexField = 'symbol';
        this.detailPage = '';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.goldProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getNameList', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        var i, a;
        this._cbSVC(o);
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getNameCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i, o, name;
        if (!a) {
            a = [];
        }
        for (i = 0; i < a.length; i++) {
            o = $C('a');
            o.appendChild($T('查看'));
            o.target = '_blank';
            o.href = 'http://vip.stock.finance.sina.com.cn/q/view/vGold_History.php?breed=' + a[i].symbol;
            a[i].lsjy = o;

            if (a[i].symbol == "AUTD" || a[i].symbol == "AGTD") {
                name = $C('a');
                name.appendChild($T(a[i].name));
                name.target = '_blank';
                name.href = "http://finance.sina.com.cn/money/gold/" + a[i].symbol + "/quote.shtml";
                a[i].name = name;
            }
        }

        this._cbQDL(a);
    },
    NameLink: function (v, o) {
        var a;
        if (o.symbol == "AUTD" || o.symbol == "AGTD") {
            a = $C('a');
            a.appendChild($T(v));
            a.target = '_blank';
            a.href = "http://finance.sina.com.cn/money/gold/" + o.symbol + "/quote.shtml";
            return a;
        } else {
            return $T(v);
        }
    }
};

// javascript document
// work with stock_list.js

S_SL_HK = function () {
    this.Init.apply(this, arguments);
};

S_SL_HK.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'qbgg_hk', X_optimeid: ''};
        this.css = {
            up: 'global_up', down: 'global_down', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 'pricechange';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 999999999;
        this.tmrCntFull = 10;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 563},
            //{f:'engname', t:'英文名称', d:-2, s:51},
            {f: 'lasttrade', t: '最新价', d: 3, s: 64, c: 'colorize'},
            {f: 'pricechange', t: '涨跌额', d: 3, s: 4, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            //{f:'buy', t:'买入', d:3, c:'colorize'},
            {f: 'sell', t: '成交价', d: 3, c: 'colorize'},
            {f: 'prevclose', t: '昨收', d: 3, s: 512, c: 'colorize'},
            {f: 'open', t: '今开', d: 3, c: 'colorize'},
            {f: 'high', t: '最高', d: 3, c: 'colorize'},
            {f: 'low', t: '最低', d: 3, c: 'colorize'},
            {f: 'volume', t: '成交量/万', d: 2, s: 8},
            {f: 'amount', t: '成交额/万', d: 2, s: 8},
            //{f:'jjtmfsshq', t:'经济通免费实时行情', d:-2, s:18},
            //{f:'zxg', t:'关注', s:18},
            {f: 'bar', t: '股吧', s: 18}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://stock.finance.sina.com.cn/hkstock/quotes/$1.html';
        this.cssPrefix = 's_sl_hk_css_';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.hkrtProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        var s = Util.MergeObj({}, this.svcParam);
        delete s.X_optimeid;
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getRTHKStockData', this._gotData._Bind(this), s);
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    _genStockLink: function (code) {
        var a, r, i;
        r = [];
        /*a = document.createElement('a');
		a.setAttribute('href', 'http://vip.stock.finance.sina.com.cn/portfolio/hqjia.php?symbol=' + code);
		a.setAttribute('target', '_blank');
		a.setAttribute('title', '加入我的自选');
		i = document.createElement('img');
		i.setAttribute('src', 'http://www.sinaimg.cn/cj/subject/2009/0618/images/addto_p.gif');
		i.setAttribute('alt', '关注');
		a.appendChild(i);
		r[0] = a;*/
        var c = parseInt(code, 10).toString();
        while (c.length < 4) {
            c = '0' + c;
        }
        a = document.createElement('a');
        a.setAttribute('href', 'http://guba.sina.com.cn/bar.php?name=' + c + 'HK');
        a.setAttribute('target', '_blank');
        a.setAttribute('title', '进入股票吧');
        i = document.createElement('img');
        i.setAttribute('src', '//www.sinaimg.cn/cj/subject/2009/0618/images/linkto_ba.gif');
        i.setAttribute('alt', '股吧');
        a.appendChild(i);
        r[0] = a;
        return r;
    },
    _dataPrepare: function (o, l) {
        var i, v, j, a, am, r, c, t;
        t = '';
        if (!o) {
            o = [];
        }
        for (i = 0; i < o.length; i++) {
            if (l && !parseFloat(o[i].open) && !parseFloat(o[i].volume)) {
                o[i] = {};
                continue;
            }
            j = [o[i].Update_Date, o[i].ticktime].join(' ');
            if (j > t) {
                t = j;
            }
            c = o[i][this.indexField] ? o[i][this.indexField] : this._aCodes[i];
            r = this._genStockLink(c);
            //o[i].zxg = r[0];
            o[i].bar = r[0];
            if (o[i].volume) {
                o[i].volume /= 10000;
            }
            if (o[i].amount) {
                o[i].amount /= 10000;
            }
            //r = document.createElement('a');
            //v = document.createElement('img');
            //v.src = 'http://www.sinaimg.cn/cj/subject/2009/0618/images/btn_hk_rt.gif';
            //r.appendChild(v);
            //r.target = '_blank';
            //r.href = 'http://biz.finance.sina.com.cn/hk/hk_rt_rd.php?code=' + c;
            //o[i].jjtmfsshq = r;
            //if (o[i].lasttrade == 0)
            //{
            //	o[i].lasttrade = '停牌';
            //}
        }
        i = this.svcParam.X_optimeid;
        if (typeof (i) == 'function') {
            i(t);
        } else {
            if ($(i)) {
                $(i).innerHTML = t;
            }
        }
        return o;
    },
    GetDataLight: function (cb) {
        /*this._cbQDL = cb;
		//不支持并发且回调不同的情况，实践中没有这种情况
		if (this._oQDL)
		{
		//	this._oQDL.request();
		}*/
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getRTHKStockCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlObjConv(a[i]);
        }
        this._cbQDL(this._dataPrepare(a, 1));
    },
    _qdlObjConv: function (o) {
        var s, d, i;
        return o;
    }
};

// javascript document
// work with stock_list.js

S_SL_HKINDEX = function () {
    this.Init.apply(this, arguments);
};

S_SL_HKINDEX.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'zs_hk'};
        this.css = {
            up: 'global_up', down: 'global_down', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'page', 'num', 'asc'];
        this.colorfield = 3;
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 5;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 51},
            {f: 'lasttrade', t: '最新价', d: 2, s: 64, c: 'colorize'},
            {f: 'pricechange', t: '涨跌额', d: 2, s: 4, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'prevclose', t: '昨收', d: 2, c: 'colorize'},
            {f: 'open', t: '今开', d: 2, c: 'colorize'},
            {f: 'high', t: '最高', d: 2, c: 'colorize'},
            {f: 'low', t: '最低', d: 2, c: 'colorize'}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://stock.finance.sina.com.cn/hkstock/quotes/$1.html';
        this.cssPrefix = 's_sl_hkindex_css_';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.hkProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getNameList', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    _dataPrepare: function (o) {
        var i, v, j, a, am, r, c;
        if (!o) {
            o = [];
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getNameCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlConv(a[i]);
        }
        this._cbQDL(this._dataPrepare(a));
    },
    _qdlConv: function (o) {
        o.pricechange = o.lasttrade - o.prevclose;
        o.changepercent = o.pricechange / o.prevclose * 100;
        if (!o.lasttrade) {
            o.pricechange = o.changepercent = 0;
        }
        return o;
    }
};

// javascript document
// work with stock_list.js

S_SL_HK_DELAY = function () {
    this.Init.apply(this, arguments);
};

S_SL_HK_DELAY.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'qbgg_hk'};
        this.css = {
            up: 'global_up', down: 'global_down', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 'pricechange';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 10;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'name', t: '名称', d: -2, s: 563},
            //{f:'engname', t:'英文名称', d:-2, s:51},
            {f: 'lasttrade', t: '最新价', d: 3, s: 64, c: 'colorize'},
            {f: 'pricechange', t: '涨跌额', d: 3, s: 4, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            // {f:'buy', t:'买入', d:3, c:'colorize'},
            // {f:'sell', t:'卖出', d:3, c:'colorize'},
            {f: 'prevclose', t: '昨收', d: 3, s: 512, c: 'colorize'},
            {f: 'open', t: '今开', d: 3, c: 'colorize'},
            {f: 'high', t: '最高', d: 3, c: 'colorize'},
            {f: 'low', t: '最低', d: 3, c: 'colorize'},
            {f: 'volume', t: '成交量/万', d: 2, s: 8},
            {f: 'amount', t: '成交额/万', d: 2, s: 8},
            //{f:'jjtmfsshq', t:'经济通免费实时行情', d:-2, s:18},
            //{f:'zxg', t:'关注', s:18},
            {f: 'bar', t: '股吧', s: 18}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://stock.finance.sina.com.cn/hkstock/quotes/$1.html';
        this.cssPrefix = 's_sl_hk_css_';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.hkProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getHKStockData', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    _genStockLink: function (code) {
        var a, r, i;
        r = [];
        /*a = document.createElement('a');
		a.setAttribute('href', 'http://vip.stock.finance.sina.com.cn/portfolio/hqjia.php?symbol=' + code);
		a.setAttribute('target', '_blank');
		a.setAttribute('title', '加入我的自选');
		i = document.createElement('img');
		i.setAttribute('src', 'http://www.sinaimg.cn/cj/subject/2009/0618/images/addto_p.gif');
		i.setAttribute('alt', '关注');
		a.appendChild(i);
		r[0] = a;*/
        var c = parseInt(code, 10).toString();
        while (c.length < 4) {
            c = '0' + c;
        }
        a = document.createElement('a');
        a.setAttribute('href', 'http://guba.sina.com.cn/bar.php?name=' + c + 'HK');
        a.setAttribute('target', '_blank');
        a.setAttribute('title', '进入股票吧');
        i = document.createElement('img');
        i.setAttribute('src', '//www.sinaimg.cn/cj/subject/2009/0618/images/linkto_ba.gif');
        i.setAttribute('alt', '股吧');
        a.appendChild(i);
        r[0] = a;
        return r;
    },
    _dataPrepare: function (o, l) {
        var i, v, j, a, am, r, c;
        for (i = 0; i < o.length; i++) {
            if (l && !parseFloat(o[i].open) && !parseFloat(o[i].volume)) {
                o[i] = {};
                continue;
            }
            c = o[i][this.indexField] ? o[i][this.indexField] : this._aCodes[i];
            r = this._genStockLink(c);
            //o[i].zxg = r[0];
            o[i].bar = r[0];
            if (o[i].volume) {
                o[i].volume /= 10000;
            }
            if (o[i].amount) {
                o[i].amount /= 10000;
            }
            r = document.createElement('a');
            v = document.createElement('img');
            v.src = '//www.sinaimg.cn/cj/subject/2009/0618/images/btn_hk_rt.gif';
            r.appendChild(v);
            r.target = '_blank';
            r.href = 'http://biz.finance.sina.com.cn/hk/hk_rt_rd.php?code=' + c;
            o[i].jjtmfsshq = r;
            //if (o[i].lasttrade == 0)
            //{
            //	o[i].lasttrade = '停牌';
            //}
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getHKStockCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlObjConv(a[i]);
        }
        this._cbQDL(this._dataPrepare(a, 1));
    },
    _qdlObjConv: function (o) {
        var s, d, i;
        return o;
    }
};

// javascript document
// work with stock_list.js

S_SL_MATTERGOLD = function () {
    this.Init.apply(this, arguments);
};

S_SL_MATTERGOLD.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'tradedate', asc: 0, node: 'sw_gold'};
        this.css = {
            up: 'red', down: 'green', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'asc', 'node', 'page'];
        this.colorfield = 'wave';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 6000000;
        this.tmrCntFull = 6;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'brandname', t: '品牌', d: -2, s: 16},
            {f: 'productsname', t: '产品', d: -2, s: 16},
            {f: 'price', t: '价格(元/克)', d: 2, s: 16},
            {f: 'purity', t: '纯度', d: 2, s: 16},
            {f: 'wave', t: '涨跌', s: 16, d: 2, c: 'colorize', u: 'setColor'},
            {f: 'tradedate', t: '日期', d: -2, s: 16},
            {f: 'lsjy', t: '历史交易', s: 18}
        ];
        this.indexField = 'symbol';
        this.detailPage = '';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.matterGoldProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getNameList', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        var i, a;
        this._cbSVC(o);
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getNameCount', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i, o, name, webname;
        if (!a) {
            a = [];
        }
        for (i = 0; i < a.length; i++) {
            o = $C('a');
            o.appendChild($T('查看'));
            o.target = '_blank';
            o.href = 'http://money.finance.sina.com.cn/q/view/vGold_Matter_History.php?code=' + a[i].id_code;
            a[i].lsjy = o;
        }
        this._cbQDL(a);
    },
    setColor: function (v) {
        if (v == 0)
            v = "--";
        else if (v > 0)
            v = "涨";
        else if (v < 0)
            v = "跌";
        return $T(v);
    }
};

// javascript document
// work with stock_list.js

S_SL_US = function () {
    this.Init.apply(this, arguments);
};

S_SL_US.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'chg', asc: 0, node: 'china_us'};
        this.css = {
            up: 'global_up', down: 'global_down', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr', pageone: 'pageone'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 'diff';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 6;
        //this.tdGroups = [{t: 'now', s: 2, e: 3, c: ''}, {t:'max', s: 6, e: 9, c: 'test'}];
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'cname', t: '名称', d: -2, s: 51},
            {f: 'price', t: '最新价', d: 2, s: 64, c: 'colorize'},
            {f: 'diff', t: '涨跌额', d: 2, s: 4, c: 'colorize'},
            {f: 'chg', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'open', t: '开盘价', d: 3, c: 'colorize'},
            {f: 'high', t: '最高价', d: 3, c: 'colorize'},
            {f: 'low', t: '最低价', d: 3, c: 'colorize'},
            {f: '_52whigh', t: '52周最高价', d: 3, c: 'colorize'},
            {f: '_52wlow', t: '52周最低价', d: 3, c: 'colorize'},
            {f: 'volume', t: '成交量', s: 8, d: 0}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?strict=1&country=us&q=$1';
        this.cssPrefix = 's_sl_us_css_';
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.usProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getUSList', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        this._cbSVC(o);
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getUSCount', cb, {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        this._cbQDL(a);
    }
};

// javascript document
// work with stock_list.js

S_SL_US2 = function () {
    this.Init.apply(this, arguments);
};

S_SL_US2.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: 'symbol', asc: 1, node: 'china_us'};
        this.css = {
            up: 'global_up', down: 'global_down', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['node', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 'diff';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 5000;
        this.tmrCntFull = 40;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'cname', t: '名称', d: -2, s: 146},
            {f: 'price', t: '最新价', d: 2, s: 64, c: 'colorize'},
            {f: 'diff', t: '涨跌额', d: 2, s: 4, c: 'colorize'},
            {f: 'chg', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'open', t: '开盘价', d: 3, c: 'colorize'},
            {f: 'high', t: '最高价', d: 3, c: 'colorize'},
            {f: 'low', t: '最低价', d: 3, c: 'colorize'},
            {f: '_52whigh', t: '最高', d: 3, c: 'colorize'},
            {f: '_52wlow', t: '最低', d: 3, c: 'colorize'},
            {f: 'volume', t: '成交量', s: 8, d: 0},
            {f: 'exchange', t: '交易所', s: 16, d: -2}
        ];
        this.tdGroups = [{s: 8, e: 9, t: '52周区间'}];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?strict=1&country=us&q=$1';
        this.cssPrefix = 's_sl_us_css_';
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.usProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('Market_Center.getUS2List', this._gotData._Bind(this), this.svcParam);
        if (this._svcChgCb) {
            this._svcChgCb(this._outputParam());
        }
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    Wrap_cn: function (s, b) {
        var i, j, l, c1, c2;
        c1 = c2 = 0;
        j = 0;
        l = 0;
        for (i = 0; i < s.length; i++) {
            j += (s.charCodeAt(i) & 0xff80) ? 2 : 1;
            if (j > b - 2 && !c1) {
                c1 = i - 1;
            }
            if (j > b && !c2) {
                c2 = i - 1;
                break;
            }
            l = j;
        }
        if (!c2) {
            return s;
        } else {
            return s.substr(0, c1) + '...';
        }
    },
    _dataPrepare: function (o, l) {
        var i, v, j, a, am, r, c, d;
        if (!o) {
            o = [];
        }
        for (i = 0; i < o.length; i++) {
            if (l) {
                delete o[i].cname;
            } else {
                a = document.createElement('a');
                a.setAttribute('href', Util.StrTemplate(this.detailPage, o[i][this.indexField]));
                a.appendChild(document.createTextNode(this.Wrap_cn(o[i].cname, 16)));
                a.setAttribute('title', o[i].cname);
                a.setAttribute('alt', o[i].cname);
                a.setAttribute('target', '_blank');
                o[i].cname = a;
            }
        }
        return o;
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('Market_Center.getUS2Count', cb,
            {node: this.svcParam.node});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        this._cbQDL(this._dataPrepare(a, 1));
    }
};

// javascript document
// work with stock_list.js

S_SL_WAR = function () {
    this.Init.apply(this, arguments);
};

S_SL_WAR.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {
            stock_symbol: '', 'value': '', 'class': 'a', publisher: '', page: 1,
            num: 20, sort: 'chgrate', asc: 0, node: 'hk_warrant_rgz', X_optimeid: ''
        };
        this.css = {
            up: 'green', down: 'red', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'page', 'num', 'asc', 'node'];
        this.colorfield = 3;
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 999999999;
        this.tmrCntFull = 1;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'warrant_name', t: '名称', d: -2, s: 51},
            {f: 'lasttrade', t: '最新价', d: 3, s: 64, c: 'colorize'},
            {f: 'chgrate', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'volume', t: '成交量/万', d: 2, s: 8, c: 'colorize'},
            {f: 'amount', t: '成交额/万', d: 2, s: 8, c: 'colorize'},
            {f: 'callputflag', t: '形式', d: -2},
            {f: 'strikeprice', t: '行使价', d: -2},
            {f: 'maturitydate', t: '到期日', d: -2},
            //{f:'publish_date', t:'上市日', d:-2},
            {f: 'publisher', t: '发行人', d: -2, s: 18},
            {f: 'stock_name', t: '相关股票', d: -2, s: 18}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1&country=hk';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.hkProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this.svcParam.node) {
            var c = {
                hk_warrant_rgz: 'a',
                hk_warrant_nxz: 'b',
                hk_warrant_rengu: '0',
                hk_warrant_rengou: '1',
                hk_warrant_xz: '2',
                hk_warrant_nz: '3'
            };
            this.svcParam['class'] = c[this.svcParam.node];
        }
        this._frame.SRVCall('HK_RtQuotes.getWarrant', this._gotData._Bind(this), this.svcParam);
    },
    _dataPrepare: function (o) {
        var i, a, t;
        if (!o) {
            o = [];
        }
        t = '';
        for (i = 0; i < o.length; i++) {
            if (o[i].ticktime > t) {
                t = o[i].ticktime;
            }
            a = document.createElement('a');
            a.appendChild(document.createTextNode(o[i]['stock_name']));
            a.setAttribute('target', '_blank');
            a.setAttribute('href', 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?country=hk&q=' + o[i]['stock_symbol']);
            o[i]['stock_name'] = a;
            o[i].volume /= 10000;
            o[i].amount /= 10000;
            //a = document.createElement('a');
            //a.appendChild(document.createTextNode(o[i]['publisher']));
            //a.setAttribute('target', '_blank');
            //a.setAttribute('href', '?publisher=' + o[i]['publisher']);
            //o[i]['publisher'] = a;
        }
        i = this.svcParam.X_optimeid;
        if (typeof (i) == 'function') {
            i(t);
        } else {
            if ($(i)) {
                $(i).innerHTML = t;
            }
        }
        return o;
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    _qdlCallback: function (a) {
        var i;
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlObjConv(a[i]);
        }
        this._cbQDL(this._dataPrepare(a));
    },
    _qdlObjConv: function (o) {
        var s, d, i;
        s = ['changeRate'];
        d = ['chgrate'];
        for (i = 0; i < s.length; i++) {
            if (typeof (o[s[i]]) != 'undefined') {
                o[d[i]] = o[s[i]];
                delete o[s[i]];
            }
        }
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    GetListLength: function (cb) {
        if (this.svcParam.node) {
            var c = {
                hk_warrant_rgz: 'a',
                hk_warrant_nxz: 'b',
                hk_warrant_rengu: '0',
                hk_warrant_rengou: '1',
                hk_warrant_xz: '2',
                hk_warrant_nz: '3'
            };
            this.svcParam['class'] = c[this.svcParam.node];
        }
        this._frame.SRVCall('HK_RtQuotes.getWarrantCount', cb,
            this.svcParam);
    }
};

//callputflag
S_SL_CF_WARSIMPLE = function (o) {
    var i;
    for (i = 0; i < o.aFields.length; i++) {
        if (o.aFields[i].f == 'callputflag') {
            o.aFields.splice(i, 1);
            break;
        }
    }
};


// javascript document
// work with stock_list.js

S_SL_WAR_DELAY = function () {
    this.Init.apply(this, arguments);
};

S_SL_WAR_DELAY.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {
            stock_symbol: '', 'value': '', 'class': 'a', publisher: '', page: 1,
            num: 20, sort: 'chgrate', asc: 0, node: 'hk_warrant_rgz'
        };
        this.css = {
            up: 'green', down: 'red', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr'
        };
        this.outputParam = ['sort', 'page', 'num', 'asc', 'node'];
        this.colorfield = 3;
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 1;
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'warrant_name', t: '名称', d: -2, s: 51},
            {f: 'lasttrade', t: '最新价', d: 3, s: 64, c: 'colorize'},
            {f: 'chgrate', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'volume', t: '成交量/万', d: 2, s: 8, c: 'colorize'},
            {f: 'amount', t: '成交额/万', d: 2, s: 8, c: 'colorize'},
            {f: 'callputflag', t: '形式', d: -2},
            {f: 'strikeprice', t: '行使价', d: -2},
            {f: 'maturitydate', t: '到期日', d: -2},
            //{f:'publish_date', t:'上市日', d:-2},
            {f: 'publisher', t: '发行人', d: -2, s: 18},
            {f: 'stock_name', t: '相关股票', d: -2, s: 18}
        ];
        this.indexField = 'symbol';
        this.detailPage = 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?q=$1&country=hk';
        this._aCodes = [];
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.hkProcess);
        }
        this._srv = this._frame.SRVProvider();
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this.svcParam.node) {
            var c = {
                hk_warrant_rgz: 'a',
                hk_warrant_nxz: 'b',
                hk_warrant_rengu: '0',
                hk_warrant_rengou: '1',
                hk_warrant_xz: '2',
                hk_warrant_nz: '3'
            };
            this.svcParam['class'] = c[this.svcParam.node];
        }
        this._frame.SRVCall('HK_DelayQuotes.getWarrant', this._gotData._Bind(this), this.svcParam);
    },
    _dataPrepare: function (o) {
        var i, a;
        if (!o) {
            o = [];
        }
        for (i = 0; i < o.length; i++) {
            a = document.createElement('a');
            a.appendChild(document.createTextNode(o[i]['stock_name']));
            a.setAttribute('target', '_blank');
            a.setAttribute('href', 'http://biz.finance.sina.com.cn/suggest/lookup_n.php?country=hk&q=' + o[i]['stock_symbol']);
            o[i]['stock_name'] = a;
            o[i].volume /= 10000;
            o[i].amount /= 10000;
            //a = document.createElement('a');
            //a.appendChild(document.createTextNode(o[i]['publisher']));
            //a.setAttribute('target', '_blank');
            //a.setAttribute('href', '?publisher=' + o[i]['publisher']);
            //o[i]['publisher'] = a;
        }
        return o;
    },
    _gotData: function (o) {
        this._cbSVC(this._dataPrepare(o));
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    _qdlCallback: function (a) {
        var i;
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlObjConv(a[i]);
        }
        this._cbQDL(this._dataPrepare(a));
    },
    _qdlObjConv: function (o) {
        var s, d, i;
        s = ['changeRate'];
        d = ['chgrate'];
        for (i = 0; i < s.length; i++) {
            if (typeof (o[s[i]]) != 'undefined') {
                o[d[i]] = o[s[i]];
                delete o[s[i]];
            }
        }
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    GetListLength: function (cb) {
        if (this.svcParam.node) {
            var c = {
                hk_warrant_rgz: 'a',
                hk_warrant_nxz: 'b',
                hk_warrant_rengu: '0',
                hk_warrant_rengou: '1',
                hk_warrant_xz: '2',
                hk_warrant_nz: '3'
            };
            this.svcParam['class'] = c[this.svcParam.node];
        }
        this._frame.SRVCall('HK_DelayQuotes.getWarrantCount', cb,
            this.svcParam);
    }
};

//callputflag
S_SL_CF_WARSIMPLE = function (o) {
    var i;
    for (i = 0; i < o.aFields.length; i++) {
        if (o.aFields[i].f == 'callputflag') {
            o.aFields.splice(i, 1);
            break;
        }
    }
};


//英股
S_Finance.ukProcess = {
    QueryExp: function (x) {
        x = x.toLowerCase();

        function parseUkStock(str_) {
            return str_.replace(/[^0-9a-z_]/gi, function (str) {
                return '$' + str.charCodeAt(0).toString(16);
            });
        }

        var _newStockCode = parseUkStock(x);
        return 'lse_' + _newStockCode + ',lse_' + _newStockCode + '_i';
    },
    ABSlongVer: ['cname', 'price', 'high', 'open', 'low', 'prevclose', 'volume', 'totalPrice', 'time']
};

S_SL_UK = function () {
    this.Init.apply(this, arguments);
};

S_SL_UK.prototype = {
    Init: function (f) {
        this._frame = f;
        this.svcParam = {page: 1, num: 20, sort: '-', asc: 0, type: 'lse_star'};
        this.css = {
            up: 'global_up', down: 'global_down', highlight: 'highlight',
            sortasc: 'sort_down', sortdesc: 'sort_up',
            pagediv: 'pages', pagedisabled: 'pagedisabled', pagecurr: 'pagecurr', pageone: 'pageone'
        };
        this.outputParam = ['type', 'sort', 'page', 'num', 'asc'];
        this.colorfield = 'chg';
        this.pagetags = 5;
        //无前缀的css
        this.tmrInt = 60000;
        this.tmrCntFull = 6;
        //this.tdGroups = [{t: 'now', s: 2, e: 3, c: ''}, {t:'max', s: 6, e: 9, c: 'test'}];
        //列表：f=字段名，t=显示名/字段标题，d=小数位数（-1不起作用，-2表示字符）
        //s: 1=显示名称连接 2=不允许排序 4=强制+号 8=千位逗号 16=使用th 32=默认递增 64=涨跌箭头
        //   128=innerHTML/DOM
        //c: custom_css
        //p: template
        this.aFields = [
            {f: 'symbol', t: '代码', d: -2, s: 49},
            {f: 'cname', t: '名称', d: -2, s: 563, c: 'm200'},
            {f: 'price', t: '最新价', d: 2, s: 64, c: 'colorize'},
            {f: 'chg', t: '涨跌额', d: 2, s: 4, c: 'colorize'},
            {f: 'changepercent', t: '涨跌幅', d: 3, s: 4, c: 'colorize', p: '$1%'},
            {f: 'volume', t: '成交量', s: 8, d: 0},
            {f: 'totalPrice', t: '成交额', s: 8, d: 2}
        ];
        this.indexField = 'symbol';
        this.detailPage = '//quotes.sina.com.cn/lse/pc/quotes.php?symbol=$1';
        this.cssPrefix = 's_sl_us_css_';
        if (S_Finance.QuotesDataLight) {
            this._oQDL = new S_Finance.QuotesDataLight([], 0, this._qdlCallback._Bind(this), S_Finance.ukProcess);
        }
        this._srv = this._frame.SRVProvider('', '', 10);
    },
    GetData: function (cb) {
        this._cbSVC = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        this._frame.SRVCall('LseService.lsePcHq', this._gotData._Bind(this), this.svcParam);
    },
    _gotData: function (o) {
        var _o = [];
        var _data = o && o.data;
        if (_data && _data.length) {
            for (var i = 0; i < _data.length; i++) {
                _data[i] && _o.push({symbol: _data[i].toUpperCase()})
            }
            this._cbSVC(_o);
        }
    },
    GetDataLight: function (cb) {
        this._cbQDL = cb;
        //不支持并发且回调不同的情况，实践中没有这种情况
        if (this._oQDL) {
            this._oQDL.request();
        }
    },
    GetListLength: function (cb) {
        this._frame.SRVCall('LseService.lsePcHqNum', cb, {type: this.svcParam.type});
    },
    CodesChange: function (c) {
        this._aCodes = c;
        if (this._oQDL) {
            this._oQDL.changeCodes(c);
        }
    },
    _qdlCallback: function (a) {
        var i;
        if (!a) {
            a = [];
        }
        for (i = 0; i < a.length; i++) {
            a[i] = this._qdlObjConv(a[i]);
        }
        this._cbQDL(a);
    },
    _ps: function (d, b, e) {
        return +d;
        e = e || ''
        b = b || 2
        d = Number(d)
        if (isNaN(d)) {
            return '-'
        }
        var c = Math.abs(d)
        if (c < 10000) {
            return d.toFixed(b) + e
        }
        if (c < 100000000) {
            return (d / 10000).toFixed(b) + '\u4E07' + e
        }
        return (d / 100000000).toFixed(b) + '\u4EBF' + e
    },
    _qdlObjConv: function (o) {
        if (o.price == 0) {
            o.price = o.prevclose;
        }
        if (typeof (o.chg) == 'undefined') {
            o.chg = o.price - o.prevclose;
        }
        if (typeof (o.changepercent) == 'undefined') {
            o.changepercent = (o.chg / o.prevclose) * 100;
        }
        o.totalPrice = this._ps(o.totalPrice);
        o.volume = this._ps(o.volume);
        return o;
    }
};


// https://n.sinaimg.cn/finance/c30320b4/20200107/hqzx20190729.js