(function() {
  if (typeof window === 'undefined') return;

  /**
   * Turns an array-like object into an array of serialized strings, suitable for the native layer
   * @param args
   * @returns {unknown[]}
   */
  var convertArguments = function(args) {
    args = Array.from(args);
    return args.map(function(elt) {
        return JSON.stringify({value: elt});
    });
  };

  /**
   * Flattens (max 2-level deep) the provided array
   * @param array
   * @returns array
   */
  var flatten = function(array) {
    return array.reduce(function(acc, cur) {
      if (Array.isArray(cur)) cur.forEach(function(elt) { acc.push(elt); });
      else acc.push(cur);
      return acc;
    }, []);
  };

  window.WonderPushInAppSDK = window.WonderPushPopupSDK = new Proxy({}, new function() {

    /**
     * Calls the provided func of the native layer:
     * - handles argument serialization
     * - handles result deserialization
     * - handles errors
     * @param func
     * @param args
     * @returns {Promise<any>}
     */
    var callNativeLayer = function(func, args) {
      try {
        var result = func.apply(window._wpiam, convertArguments(args));
        if (result === null || result === undefined) {
            return Promise.resolve();
        }
        if (typeof result === 'string') {
            try {
              var decoded = JSON.parse(result);
              if ("result" in decoded) return Promise.resolve(decoded.result);
              if ("error" in decoded) return Promise.reject(new Error(decoded.error));
            } catch (e) {
              console.error(e);
              // Return result as-is.
            }
        }
        return Promise.resolve(result);
      } catch (e) {
        return Promise.reject(e);
      }
    };

    /**
     * The main Proxy method
     * @type {any}
     */
    this.get = function(target, prop, receiver) {
      if (this.hasOwnProperty(prop)) return this[prop];
      var func = window._wpiam[prop];
      if (!func) {
        return function() { return Promise.reject(new Error('Unknown method '+prop)); };
      }
      return function() {
        // Delay dismiss
        if (prop === 'dismiss') {
            return new Promise(function (res, rej) {
                setTimeout(function() {
                    callNativeLayer(func, arguments).then(res, rej);
                }, 10);
            });
        }
        return callNativeLayer(func, arguments);
      };
    }.bind(this);

    /**
     * Takes tags as arguments:
     * - an array as first argument
     * - a variable argument signature with multiple strings
     * @returns {Promise<void>}
     */
    this.addTag = function() {
      // Flatten to accept variable arguments or an array of tags as first arg
      // always calling the native layer with a flat array of strings
      var tags = flatten(Array.from(arguments));
      return callNativeLayer(window._wpiam.addTag, [tags]);
    };

    /**
     * Takes tags as arguments:
     * - an array as first argument
     * - a variable argument signature with multiple strings
     * @returns {Promise<void>}
     */
    this.removeTag = function() {
      // Flatten to accept variable arguments or an array of tags as first arg.
      // always calling the native layer with a flat array of strings
      var tags = flatten(Array.from(arguments));
      return callNativeLayer(window._wpiam.removeTag, [tags]);
    };

  });

  // Executed when the window is loaded
  var onload = function() {
    // Register event listeners on data-wonderpush-* elements
    var keys = [ // Order matters: we try to dismiss last
      "wonderpushCallMethod",
      "wonderpushButtonLabel",
      "wonderpushRemoveAllTags", // remove tags before adding them
      "wonderpushRemoveTag",
      "wonderpushAddTag",
      "wonderpushUnsubscribeFromNotifications", // unsubscribe before subscribe
      "wonderpushSubscribeToNotifications",
      "wonderpushTrackEvent",
      "wonderpushTriggerLocationPrompt",
      "wonderpushOpenAppRating",
      "wonderpushOpenDeepLink", // move somewhere else last
      "wonderpushOpenExternalUrl",
      "wonderpushDismiss",
    ];
    document.querySelectorAll('*').forEach(function(elt) {
      if (!elt.dataset) return;
      keys.forEach(function (key) {
        if (!(key in elt.dataset)) return;
        var val = elt.dataset[key];
        var fn;
        switch (key) {
          case "wonderpushCallMethod":
            fn = function () {
              window.WonderPushPopupSDK.callMethod(val);
            };
            break;
          case "wonderpushAddTag":
            fn = function () {
              window.WonderPushPopupSDK.addTag(val);
            };
            break;
          case "wonderpushButtonLabel":
            fn = function () {
              window.WonderPushPopupSDK.trackClick(val);
            };
            break;
          case "wonderpushDismiss":
            fn = function () {
              window.WonderPushPopupSDK.dismiss();
            };
            break;
          case "wonderpushOpenDeepLink":
            fn = function () {
              window.WonderPushPopupSDK.openDeepLink(val);
            };
            break;
          case "wonderpushOpenExternalUrl":
            fn = function () {
              window.WonderPushPopupSDK.openExternalUrl(val);
            };
            break;
          case "wonderpushRemoveAllTags":
            fn = function () {
              window.WonderPushPopupSDK.removeAllTags();
            };
            break;
          case "wonderpushRemoveTag":
            fn = function () {
              window.WonderPushPopupSDK.removeTag(val);
            };
            break;
          case "wonderpushSubscribeToNotifications":
            fn = function () {
              window.WonderPushPopupSDK.subscribeToNotifications();
            };
            break;
          case "wonderpushTrackEvent":
            fn = function () {
              window.WonderPushPopupSDK.trackEvent(val);
            };
            break;
          case "wonderpushTriggerLocationPrompt":
            fn = function () {
              window.WonderPushPopupSDK.triggerLocationPrompt();
            };
            break;
          case "wonderpushOpenAppRating":
            fn = function () {
              window.WonderPushPopupSDK.openAppRating();
            };
            break;
          case "wonderpushUnsubscribeFromNotifications":
            fn = function () {
              window.WonderPushPopupSDK.unsubscribeFromNotifications();
            };
            break;
        }
        if (fn) {
          elt.addEventListener('click', fn);
        }
      });
    });
  }
  if (document.readyState === "complete") {
    onload();
  } else {
    window.addEventListener("load", onload);
  }
})();
