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
      if (elt === null) return null;
      if (elt === undefined) return null;
      if (typeof elt === 'function') return null;
      if (typeof elt === 'string') return elt;
      var prefix = "__" + (typeof elt) + "__";
      if (typeof elt === 'object') return prefix + JSON.stringify(elt);
      return prefix + elt.toString();
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

  window.WonderPushInAppSDK = new Proxy({}, new function() {

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
        // Array & Object results start with __array__ or __object__
        if (typeof result === 'string') {
          var match = result.match(/^__(array|object)__/);
          if (match) {
            try {
              result = JSON.parse(result.substring(match[0].length));
            } catch (e) {
              console.error(e);
              // Return result as-is.
            }
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
        return undefined;
      }
      return function() {
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
    // Register event listeners on data-wonderpush-button-label elements
    document.querySelectorAll('*[data-wonderpush-button-label]').forEach(function(elt) {
      var buttonLabel = elt.dataset.wonderpushButtonLabel;
      elt.addEventListener('click', function() {
        window._wpiam.trackClick(buttonLabel);
      });
    });
    // Register event listeners on data-wonderpush-dismiss elements
    document.querySelectorAll('*[data-wonderpush-dismiss]').forEach(function(elt) {
      elt.addEventListener('click', function() {
        window._wpiam.dismiss();
      });
    });
  }
  if (document.readyState === "complete") {
    onload();
  } else {
    window.addEventListener("load", onload);
  }
})();
